package com.example.examplemod.mixin.ironfurnaces;

import com.example.examplemod.KeepSmelting;
import com.example.examplemod.KeepSmeltingConfig;
import ironfurnaces.items.augments.ItemAugmentFuel;
import ironfurnaces.items.augments.ItemAugmentSpeed;
import ironfurnaces.tileentity.furnaces.BlockIronFurnaceTileBase;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.common.ForgeHooks;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;
import java.util.Optional;

/**
 * Catchup mixin for Iron Furnaces BlockIronFurnaceTileBase.
 * Supports 3 modes: furnace, factory, generator.
 * Fully respects all augment slots:
 *   Slot 3 (Red):   recipe type — normal / Blasting / Smoking
 *   Slot 4 (Green): efficiency — normal / Speed / Fuel
 *   Slot 5 (Blue):  mode — Furnace / Factory / Generator
 */
@Pseudo
@Mixin(targets = "ironfurnaces.tileentity.furnaces.BlockIronFurnaceTileBase")
public abstract class IronFurnaceTickMixin {

    @Unique
    private static final String TAG_LAST_TIME = "keepsmelting_lastRealTime";

    @Unique
    private long keepsmelting$lastRealTime;

    @Unique
    private boolean keepsmelting$catchupDone;

    @Unique
    private boolean keepsmelting$ioGuard;

    @Unique
    private String keepsmelting$activeTimeMode;

    @Unique
    private static final int[] FACTORY_INPUT = new int[]{7, 8, 9, 10, 11, 12};

    @Inject(method = "saveAdditional", at = @At("TAIL"))
    private void onSave(CompoundTag tag, CallbackInfo ci) {
        tag.putLong(TAG_LAST_TIME, this.keepsmelting$lastRealTime);
        tag.putString("keepsmelting_timeMode", KeepSmeltingConfig.COMMON.timeMode.get().name());
    }

    @Inject(method = "load", at = @At("TAIL"))
    private void onLoad(CompoundTag tag, CallbackInfo ci) {
        String savedMode = tag.getString("keepsmelting_timeMode");
        String currentMode = KeepSmeltingConfig.COMMON.timeMode.get().name();
        if (!savedMode.isEmpty() && savedMode.equals(currentMode)) {
            this.keepsmelting$lastRealTime = tag.getLong(TAG_LAST_TIME);
        } else {
            this.keepsmelting$lastRealTime = 0L;
        }
    }

    @Inject(method = "tick", at = @At("HEAD"), remap = false)
    private static void onTick(Level level, BlockPos pos, BlockState state,
                               BlockIronFurnaceTileBase tile, CallbackInfo ci) {
        if (level.isClientSide) return;
        if (!KeepSmeltingConfig.COMMON.catchupEnabled.get()) return;

        IronFurnaceTickMixin self = (IronFurnaceTickMixin) (Object) tile;
        String currentMode = KeepSmeltingConfig.COMMON.timeMode.get().name();
        if (self.keepsmelting$activeTimeMode != null && !self.keepsmelting$activeTimeMode.equals(currentMode)) {
            self.keepsmelting$lastRealTime = 0L;
        }
        self.keepsmelting$activeTimeMode = currentMode;

        long now;
        long last = self.keepsmelting$lastRealTime;
        if (KeepSmeltingConfig.COMMON.timeMode.get() == KeepSmeltingConfig.TimeMode.GAMETIME) {
            now = level.getGameTime();
        } else {
            now = System.currentTimeMillis();
        }
        self.keepsmelting$lastRealTime = now;
        long elapsed;
        if (KeepSmeltingConfig.COMMON.timeMode.get() == KeepSmeltingConfig.TimeMode.GAMETIME) {
            elapsed = now - last;
        } else {
            elapsed = (now - last) / 50L;
        }
        if (last == 0) return;
        int minDelta = KeepSmeltingConfig.COMMON.minDeltaThreshold.get();
        if (elapsed < minDelta) return;
        long max = KeepSmeltingConfig.COMMON.maxCatchupTicks.get();
        elapsed = Math.min(elapsed, max);
        if (elapsed <= 0) return;

        if (tile.isFurnace()) {
            if (self.keepsmelting$ioGuard) return;
            self.keepsmelting$ioGuard = true;
            try {
                ((IronFurnaceAccessor) tile).invokeCheckRecipeType();
                applyFurnaceCatchup(tile, elapsed, level, pos);
            } finally {
                self.keepsmelting$ioGuard = false;
            }
        } else if (tile.isFactory()) {
            if (self.keepsmelting$ioGuard) return;
            self.keepsmelting$ioGuard = true;
            try {
                ((IronFurnaceAccessor) tile).invokeCheckRecipeType();
                applyFactoryCatchup(tile, elapsed, level, pos);
            } finally {
                self.keepsmelting$ioGuard = false;
            }
        } else if (tile.isGenerator()) {
            if (self.keepsmelting$catchupDone) {
                self.keepsmelting$catchupDone = false;
                return;
            }
            if (self.keepsmelting$ioGuard) return;
            self.keepsmelting$ioGuard = true;
            try {
                applyGeneratorCatchup(tile, elapsed, level, pos);
            } finally {
                self.keepsmelting$ioGuard = false;
            }
        }
    }

    // ══════════════════════════════════════════════
    //  Furnace mode
    //  recipeType honoured, Speed halts cook+halves burn,
    //  Fuel extends cook+multiplies burn
    // ══════════════════════════════════════════════

    @Unique
    private static void applyFurnaceCatchup(BlockIronFurnaceTileBase tile, long elapsed,
                                            Level level, BlockPos pos) {
        if (tile.furnaceBurnTime <= 0 || tile.totalCookTime <= 0) return;

        int cookTimeBefore = tile.cookTime;
        int burnTimeBefore = tile.furnaceBurnTime;
        int fuelBefore = tile.inventory.get(1).getCount();

        int maxStack = tile.getMaxStackSize();
        long remaining = elapsed;
        int cookTime = tile.cookTime;
        int burnTime = tile.furnaceBurnTime;
        int totalCookTime = tile.totalCookTime;
        ItemStack fuel = tile.inventory.get(1);
        ItemStack output = tile.inventory.get(2);
        ItemStack input = tile.inventory.get(0);

        IronFurnaceAccessor acc = (IronFurnaceAccessor) tile;
        long totalItems = 0;

        // Detect green augment on slot 4 for burn time formula
        ItemStack greenAug = tile.inventory.get(4);
        boolean hasSpeed = !greenAug.isEmpty() && greenAug.getItem() instanceof ItemAugmentSpeed;
        boolean hasFuel = !greenAug.isEmpty() && greenAug.getItem() instanceof ItemAugmentFuel;

        while (remaining > 0 && burnTime > 0) {
            // Sync totalCookTime with current augments (matches original tick behaviour)
            int newTotal = tile.getCookTime();
            if (newTotal > 0) totalCookTime = newTotal;

            // Pull input from chest if empty
            if (input.isEmpty()) {
                tile.furnaceBurnTime = Math.max(0, burnTime);
                tile.cookTime = Math.max(0, cookTime);
                tile.setChanged();
                acc.invokeAutoIO();
                input = tile.inventory.get(0);
                if (input.isEmpty()) break;
            }

            long applyTick = Math.min(remaining, Math.min(burnTime,
                    totalCookTime - cookTime + (long) (input.getCount() - 1) * totalCookTime));
            cookTime += (int) applyTick;
            burnTime -= (int) applyTick;
            remaining -= applyTick;

            while (cookTime >= totalCookTime && !input.isEmpty()) {
                cookTime -= totalCookTime;

                // Flush output to chest if full
                if (!output.isEmpty() && output.getCount() >= maxStack) {
                    tile.furnaceBurnTime = Math.max(0, burnTime);
                    tile.cookTime = Math.max(0, cookTime);
                    tile.setChanged();
                    acc.invokeAutoIO();
                    output = tile.inventory.get(2);
                    if (!output.isEmpty() && output.getCount() >= maxStack) break;
                }

                input.shrink(1);

                Optional<?> recipeOpt = acc.invokeGetRecipe(
                        input.isEmpty() ? ItemStack.EMPTY : input);
                if (recipeOpt.isPresent()) {
                    Recipe<?> recipe = (Recipe<?>) recipeOpt.get();
                    ItemStack result = recipe.getResultItem(level.registryAccess());
                    if (output.isEmpty()) {
                        tile.inventory.set(2, result.copy());
                        output = tile.inventory.get(2);
                    } else if (ItemStack.isSameItemSameTags(output, result)) {
                        output.grow(result.getCount());
                    }
                }
                totalItems++;
                if (input.getCount() <= 0) {
                    tile.inventory.set(0, ItemStack.EMPTY);
                    input = ItemStack.EMPTY;
                }
            }

            // Burn new fuel if current exhausted and input exists
            if (burnTime <= 0 && remaining > 0) {
                if (fuel.isEmpty()) {
                    tile.furnaceBurnTime = 0;
                    tile.cookTime = cookTime;
                    tile.setChanged();
                    acc.invokeAutoIO();
                    fuel = tile.inventory.get(1);
                }
                if (!fuel.isEmpty()) {
                    // Use tile.recipeType (set by red augment — normal/Smoking/Blasting)
                    RecipeType<?> rt = tile.recipeType;
                    int baseBurn = ForgeHooks.getBurnTime(fuel, rt);
                    if (baseBurn > 0) {
                        // Mirror original Iron Furnaces burn time formula:
                        //   no augment: baseBurn * cookTime / 200
                        //   Speed:      baseBurn * cookTime / 200 / 2
                        //   Fuel:       baseBurn * cookTime / 200 * 2
                        int adjustedCook = Math.max(1, totalCookTime);
                        int burn = baseBurn * adjustedCook / 200;
                        if (hasSpeed) burn /= 2;
                        if (hasFuel) burn *= 2;
                        burnTime = Math.max(1, burn);
                        fuel.shrink(1);
                        if (fuel.getCount() <= 0) {
                            tile.inventory.set(1, fuel.getCraftingRemainingItem());
                        }
                    } else break;
                } else break;
            }
        }
        tile.furnaceBurnTime = Math.max(0, burnTime);
        tile.cookTime = Math.max(0, cookTime);
        tile.setChanged();

        sendChatDebug(level, pos, "Furnace", elapsed,
                fuelBefore - tile.inventory.get(1).getCount(),
                (int) totalItems,
                tile.cookTime - cookTimeBefore,
                burnTimeBefore - tile.furnaceBurnTime,
                tile.furnaceBurnTime > 0);
    }

    // ══════════════════════════════════════════════
    //  Factory mode
    //  Proper instanceof checks, Speed doubles RF,
    //  Fuel halves RF cost
    // ══════════════════════════════════════════════

    @Unique
    private static void applyFactoryCatchup(BlockIronFurnaceTileBase tile, long elapsed,
                                            Level level, BlockPos pos) {
        IronFurnaceAccessor acc = (IronFurnaceAccessor) tile;

        int energyBefore = tile.getEnergy();
        int[] cookBefore = new int[6];
        for (int i = 0; i < 6; i++) cookBefore[i] = tile.factoryCookTime[i];
        int[] outputBefore = new int[6];
        for (int i = 0; i < 6; i++) {
            outputBefore[i] = tile.inventory.get(FACTORY_INPUT[i] + 6).getCount();
        }

        // Detect green augment on slot 4
        ItemStack greenAug = tile.inventory.get(4);
        boolean hasSpeed = !greenAug.isEmpty() && greenAug.getItem() instanceof ItemAugmentSpeed;
        boolean hasFuel = !greenAug.isEmpty() && greenAug.getItem() instanceof ItemAugmentFuel;

        // Force generator catchup on neighbors FIRST
        processNeighborGenerators(tile, elapsed, level, pos);

        // Pull all RF from neighbor generators
        int pulledRf = pullAllRFFromNeighborGenerators(tile, level, pos);

        int rfConsumed = 0;
        boolean anyWorked = false;

        for (int t = 0; t < elapsed; t++) {
            boolean tickWorked = false;

            for (int i = 0; i < 6; i++) {
                int slot = FACTORY_INPUT[i];
                ItemStack input = tile.inventory.get(slot);

                int outputSlot = slot + 6;
                ItemStack output = tile.inventory.get(outputSlot);

                // Flush output to chest
                if (!output.isEmpty() && output.getCount() >= output.getMaxStackSize()) {
                    tile.setChanged();
                    acc.invokeAutoFactoryIO();
                    output = tile.inventory.get(outputSlot);
                    if (!output.isEmpty() && output.getCount() >= output.getMaxStackSize()) continue;
                }

                // Pull input from chest
                if (input.isEmpty()) {
                    tile.setChanged();
                    acc.invokeAutoFactoryIO();
                    input = tile.inventory.get(slot);
                    if (input.isEmpty()) continue;
                }

                int totalCookTime = acc.invokeGetFactoryCookTime(slot);
                if (totalCookTime <= 0) continue;

                int cookTotal = tile.factoryTotalCookTime[i];
                if (cookTotal <= 0) cookTotal = totalCookTime;

                Optional<? extends net.minecraft.world.item.crafting.AbstractCookingRecipe> recipeOpt =
                        acc.invokeGetRecipeFactory(slot, input);
                if (recipeOpt.isEmpty()) continue;
                net.minecraft.world.item.crafting.AbstractCookingRecipe recipe = recipeOpt.get();

                if (!acc.invokeCanFactorySmelt(recipe, slot)) continue;

                // RF per item = recipe base cookingTime * 20
                // Speed: ×2, Fuel: /2 (mirrors original BlockIronFurnaceTileBase line 809-810)
                int recipeCookTime = recipe.getCookingTime();
                int energyRecipe = recipeCookTime * 20;
                if (hasSpeed) energyRecipe *= 2;
                if (hasFuel) energyRecipe /= 2;

                // Gate: need FULL item RF cost to START new item
                if (tile.getEnergy() < energyRecipe && tile.factoryCookTime[i] <= 0) continue;

                int rfPerTick = Math.max(1, energyRecipe / Math.max(1, cookTotal));
                if (tile.getEnergy() < rfPerTick) continue;

                tile.setEnergy(tile.getEnergy() - rfPerTick);
                tile.usedRF[i] += rfPerTick;
                rfConsumed += rfPerTick;

                tile.factoryCookTime[i]++;
                tickWorked = true;
                anyWorked = true;

                if (tile.factoryCookTime[i] >= cookTotal) {
                    tile.factoryCookTime[i] = 0;
                    if (tile.usedRF[i] < (double) energyRecipe) {
                        double diff = (double) energyRecipe - tile.usedRF[i];
                        int actualDrain = Math.min(tile.getEnergy(), (int) diff);
                        tile.setEnergy(tile.getEnergy() - actualDrain);
                        rfConsumed += actualDrain;
                    }
                    tile.usedRF[i] = 0.0;
                    tile.factoryTotalCookTime[i] = acc.invokeGetFactoryCookTime(slot);
                    acc.invokeFactorySmelt(recipe, slot);
                    tile.setChanged();
                }
            }

            if (!tickWorked) break;
        }

        if (anyWorked) {
            tile.setChanged();
            int finalOutput = calcTotalOutput(tile, outputBefore, 6);
            int rfPerItem = rfConsumed > 0 && finalOutput > 0 ? rfConsumed / finalOutput : 0;
            int itemsStarted = 0;
            for (int i = 0; i < 6; i++) {
                if (tile.inventory.get(FACTORY_INPUT[i]).isEmpty()) continue;
                if (tile.factoryCookTime[i] > 0 || cookBefore[i] > 0) itemsStarted++;
            }
            String itemStr = finalOutput > 0
                    ? String.format("smelted: §a%d", finalOutput)
                    : "smelted: 0";
            String rfStr = String.format("rf: -%d (pull=%d, %dRF/item)",
                    rfConsumed, pulledRf, rfPerItem);
            Component msg = Component.literal(
                    String.format("§7[§6KeepSmelting§7] §e[Factory] §f%s §7| §e%d§7t§r | %s §7| %s",
                            pos.toShortString(), elapsed,
                            itemStr, rfStr));
            sendToNearbyPlayers((ServerLevel) level, pos, msg);
        }
    }

    // ══════════════════════════════════════════════
    //  Generator mode
    //  Already correct — getGeneratorBurn() and
    //  getGeneration() handle Speed/Fuel internally
    // ══════════════════════════════════════════════

    @Unique
    private static void applyGeneratorCatchup(BlockIronFurnaceTileBase tile, long elapsed,
                                              Level level, BlockPos pos) {
        IronFurnaceAccessor acc = (IronFurnaceAccessor) tile;

        int energyBefore = tile.getEnergy();
        int fuelBefore = tile.inventory.get(6).getCount();

        for (int t = 0; t < elapsed; t++) {
            if (tile.getEnergy() >= tile.getCapacity()) break;

            if (tile.generatorBurn <= 0.0) {
                ItemStack fuel = tile.inventory.get(6);
                if (fuel.isEmpty()) {
                    acc.invokeAutoIOGenerator();
                    fuel = tile.inventory.get(6);
                    if (fuel.isEmpty()) break;
                }

                tile.generatorBurn = tile.getGeneratorBurn();
                tile.generatorRecentRecipeRF = (int) tile.generatorBurn;

                if (fuel.hasCraftingRemainingItem()) {
                    tile.inventory.set(6, fuel.getCraftingRemainingItem());
                } else {
                    fuel.shrink(1);
                    if (fuel.isEmpty()) {
                        tile.inventory.set(6, fuel.getCraftingRemainingItem());
                    }
                }
                tile.setChanged();
            }

            if (tile.generatorBurn > 0.0) {
                int gen = tile.getGeneration();
                tile.gottenRF += gen;
                tile.setEnergy(tile.getEnergy() + gen);

                double max = tile.generatorRecentRecipeRF * 20.0;
                if (tile.generatorBurn - gen / 20.0 <= 0.0) {
                    if (tile.gottenRF + gen > max && tile.gottenRF + gen < tile.getCapacity()) {
                        int diff = (int) (tile.gottenRF + gen - max);
                        tile.setEnergy(tile.getEnergy() + gen);
                        tile.removeEnergy(diff);
                    }
                    if (tile.gottenRF + gen < max) {
                        int diff = (int) (max - tile.gottenRF + gen);
                        tile.setEnergy(tile.getEnergy() + gen);
                        tile.setEnergy(tile.getEnergy() + diff);
                    }
                    tile.gottenRF = 0.0;
                }

                tile.generatorBurn -= gen / 20.0;
                if (tile.generatorBurn <= 0.0) {
                    acc.invokeAutoIOGenerator();
                    tile.generatorBurn = 0.0;
                }
                tile.setChanged();
            }
        }

        if (tile.generatorBurn <= 0.0) {
            tile.generatorBurn = 0.0;
        }

        sendChatDebug(level, pos, "Generator", elapsed,
                fuelBefore - tile.inventory.get(6).getCount(),
                tile.getEnergy() - energyBefore,
                0, 0, tile.generatorBurn > 0.0);
    }

    // ══════════════════════════════════════════════
    //  Neighbour generator pre-processing
    // ══════════════════════════════════════════════

    @Unique
    private static void processNeighborGenerators(BlockIronFurnaceTileBase tile, long elapsed,
                                                  Level level, BlockPos pos) {
        for (Direction dir : Direction.Plane.HORIZONTAL) {
            BlockPos neighbor = pos.relative(dir);
            if (!level.isLoaded(neighbor)) continue;
            BlockEntity be = level.getBlockEntity(neighbor);
            if (be instanceof BlockIronFurnaceTileBase genTile && genTile.isGenerator()) {
                IronFurnaceTickMixin mixinSelf = (IronFurnaceTickMixin) (Object) genTile;
                if (mixinSelf.keepsmelting$catchupDone) continue;
                applyGeneratorCatchup(genTile, elapsed, level, neighbor);
                mixinSelf.keepsmelting$catchupDone = true;
            }
        }
    }

    @Unique
    private static int pullAllRFFromNeighborGenerators(BlockIronFurnaceTileBase tile, Level level,
                                                       BlockPos pos) {
        int totalPulled = 0;
        for (Direction dir : Direction.Plane.HORIZONTAL) {
            BlockPos neighbor = pos.relative(dir);
            if (!level.isLoaded(neighbor)) continue;
            BlockEntity be = level.getBlockEntity(neighbor);
            if (be instanceof BlockIronFurnaceTileBase neighborTile && neighborTile.isGenerator()) {
                Direction genSide = dir.getOpposite();
                int setting = neighborTile.furnaceSettings.get(genSide.ordinal());
                if (setting != 2 && setting != 3) continue;
                int available = neighborTile.getEnergy();
                if (available <= 0) continue;
                neighborTile.removeEnergy(available);
                neighborTile.setChanged();
                tile.setEnergy(tile.getEnergy() + available);
                totalPulled += available;
            }
        }
        return totalPulled;
    }

    // ══════════════════════════════════════════════
    //  Helpers
    // ══════════════════════════════════════════════

    @Unique
    private static int calcTotalOutput(BlockIronFurnaceTileBase tile, int[] before, int slots) {
        int total = 0;
        for (int i = 0; i < slots; i++) {
            total += tile.inventory.get(FACTORY_INPUT[i] + 6).getCount() - before[i];
        }
        return total;
    }

    @Unique
    private static int calcTotalCookAdvance(BlockIronFurnaceTileBase tile, int[] before, int slots) {
        int total = 0;
        for (int i = 0; i < slots; i++) {
            total += tile.factoryCookTime[i] - before[i];
        }
        return total;
    }

    // ══════════════════════════════════════════════
    //  Chat Debug
    // ══════════════════════════════════════════════

    @Unique
    private static void sendChatDebug(Level level, BlockPos pos, String mode, long elapsed,
                                      int fuelDelta, int outputDelta, int cookDelta, int burnDelta, boolean lit) {
        KeepSmeltingConfig.DebugMode dm = KeepSmeltingConfig.COMMON.debugMode.get();
        if (dm == KeepSmeltingConfig.DebugMode.OFF) return;
        if (!(level instanceof ServerLevel serverLevel)) return;

        Component msg;
        if ("Generator".equals(mode)) {
            String rfStr = outputDelta > 0 ? String.format("rf: §a+%d", outputDelta) : "rf: 0";
            String fuelStr = fuelDelta > 0 ? String.format("fuel: -%d", fuelDelta) : "fuel: 0";
            msg = Component.literal(
                    String.format("§7[§6KeepSmelting§7] §e[Generator] §f%s §7| §e%d§7t§r | %s §7| %s §7| §7lit=%s",
                            pos.toShortString(), elapsed,
                            rfStr, fuelStr, lit));
        } else {
            String itemStr = outputDelta > 0 ? String.format("smelted: §a%d", outputDelta) : "smelted: 0";
            String fuelStr = fuelDelta > 0 ? String.format("fuel: -%d", fuelDelta) : "fuel: 0";
            msg = Component.literal(
                    String.format("§7[§6KeepSmelting§7] §e[%s] §f%s §7| §e%d§7t§r | %s §7| %s §7| §7lit=%s",
                            mode, pos.toShortString(), elapsed,
                            itemStr, fuelStr, lit));
        }

        if (dm == KeepSmeltingConfig.DebugMode.CHAT) {
            sendToNearbyPlayers(serverLevel, pos, msg);
        } else {
            KeepSmelting.LOGGER.info(msg.getString());
        }
    }

    @Unique
    private static void sendToNearbyPlayers(ServerLevel level, BlockPos pos, Component msg) {
        AABB box = new AABB(pos).inflate(16);
        List<ServerPlayer> players = level.getEntitiesOfClass(ServerPlayer.class, box);
        for (ServerPlayer p : players) {
            p.sendSystemMessage(msg);
        }
    }
}
