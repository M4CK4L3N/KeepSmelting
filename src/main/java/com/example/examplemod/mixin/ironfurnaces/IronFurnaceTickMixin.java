package com.example.examplemod.mixin.ironfurnaces;

import com.example.examplemod.KeepSmelting;
import com.example.examplemod.KeepSmeltingConfig;
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
 * Factory triggers neighbor generator catchup BEFORE pulling RF,
 * so offline-generated RF is available.
 * Uses targets string + Pseudo -- safe when ironfurnaces absent.
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
        // Reset timer if time mode changed since last tick (in-memory avoids NBT save/load lag)
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
            elapsed = now - last; // already in ticks
        } else {
            elapsed = (now - last) / 50L; // ms → ticks
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
    //  Furnace
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

        // Counter — survives autoIO flushes unlike output slot diff
        long totalItems = 0;

        while (remaining > 0 && burnTime > 0) {
            // If input empty → try autoIO to pull from chest
            if (input.isEmpty()) {
                tile.furnaceBurnTime = Math.max(0, burnTime);
                tile.cookTime = Math.max(0, cookTime);
                tile.setChanged();
                acc.invokeAutoIO();
                input = tile.inventory.get(0);
                KeepSmelting.LOGGER.info("[Furnace IO] input empty → autoIO, got={}", input.getCount());
                if (input.isEmpty()) break;
            }
            int inputCount = input.getCount();

            long applyTick = Math.min(remaining, Math.min(burnTime,
                    totalCookTime - cookTime + (long) (inputCount - 1) * totalCookTime));
            cookTime += (int) applyTick;
            burnTime -= (int) applyTick;
            remaining -= applyTick;

            while (cookTime >= totalCookTime && !input.isEmpty()) {
                cookTime -= totalCookTime;

                // Output full → try autoIO to flush to chest
                if (!output.isEmpty() && output.getCount() >= maxStack) {
                    tile.furnaceBurnTime = Math.max(0, burnTime);
                    tile.cookTime = Math.max(0, cookTime);
                    tile.setChanged();
                    acc.invokeAutoIO();
                    output = tile.inventory.get(2);
                    KeepSmelting.LOGGER.info("[Furnace IO] output full → autoIO, now={}", output.getCount());
                    if (!output.isEmpty() && output.getCount() >= maxStack) break; // chest full too
                }

                input.shrink(1);
                inputCount--;

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

            if (burnTime <= 0 && remaining > 0) {
                // If fuel empty → try autoIO to pull fuel from chest
                if (fuel.isEmpty()) {
                    tile.furnaceBurnTime = 0;
                    tile.cookTime = cookTime;
                    tile.setChanged();
                    acc.invokeAutoIO();
                    fuel = tile.inventory.get(1);
                    KeepSmelting.LOGGER.info("[Furnace IO] fuel empty → autoIO, got={}", fuel.getCount());
                }
                if (!fuel.isEmpty()) {
                    int fb = ForgeHooks.getBurnTime(fuel, RecipeType.SMELTING);
                    if (fb > 0) {
                        burnTime = fb;
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
    //  Factory
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

        // Step 0: Force generator catchup on neighbor generators FIRST
        // so their offline-generated RF is available for factory to pull
        processNeighborGenerators(tile, elapsed, level, pos);

        // Step 1: pull ALL RF from neighbor generators (freshly generated)
        int pulledRf = pullAllRFFromNeighborGenerators(tile, level, pos);

        // Step 2: total available RF = own + pulled
        int rfConsumed = 0;
        boolean anyWorked = false;

        // Step 3: batch-process each slot
        for (int t = 0; t < elapsed; t++) {
            boolean tickWorked = false;

            for (int i = 0; i < 6; i++) {
                int slot = FACTORY_INPUT[i];
                ItemStack input = tile.inventory.get(slot);

                int outputSlot = slot + 6;
                ItemStack output = tile.inventory.get(outputSlot);

                // Output full → flush to chest
                if (!output.isEmpty() && output.getCount() >= output.getMaxStackSize()) {
                    tile.setChanged();
                    acc.invokeAutoFactoryIO();
                    output = tile.inventory.get(outputSlot);
                    KeepSmelting.LOGGER.info("[Factory IO] slot{} output full → autoFactoryIO, now={}", i, output.getCount());
                    if (!output.isEmpty() && output.getCount() >= output.getMaxStackSize()) continue;
                }

                if (input.isEmpty()) {
                    tile.setChanged();
                    acc.invokeAutoFactoryIO();
                    input = tile.inventory.get(slot);
                    KeepSmelting.LOGGER.info("[Factory IO] slot{} input empty → autoFactoryIO, got={}", i, input.getCount());
                    if (input.isEmpty()) continue;
                }

                int totalCookTime = acc.invokeGetFactoryCookTime(slot);
                if (totalCookTime <= 0) continue;

                int cookTotal = tile.factoryTotalCookTime[i];
                if (cookTotal <= 0) cookTotal = totalCookTime;

                Optional<? extends net.minecraft.world.item.crafting.AbstractCookingRecipe> recipeOpt = acc.invokeGetRecipeFactory(slot, input);
                if (recipeOpt.isEmpty()) continue;
                net.minecraft.world.item.crafting.AbstractCookingRecipe recipe = recipeOpt.get();

                if (!acc.invokeCanFactorySmelt(recipe, slot)) continue;

                // RF per item = recipe base cookingTime * 20
                int recipeCookTime = recipe.getCookingTime();
                int energyRecipe = recipeCookTime * 20;
                ItemStack augment = tile.inventory.get(4);
                if (!augment.isEmpty()) {
                    String augName = augment.getItem().getClass().getName();
                    if (augName.contains("ItemAugmentSpeed")) {
                        energyRecipe *= 2;
                    } else if (augName.contains("ItemAugmentFuel")) {
                        energyRecipe /= 2;
                    }
                }
                int energyPerTick = Math.max(1, energyRecipe / Math.max(1, cookTotal));

                // Gate: need FULL item RF cost to START a new item (mirrors original ironfurnaces)
                if (tile.getEnergy() < energyRecipe && tile.factoryCookTime[i] <= 0) continue;

                // Drain RF per tick (same formula as original ironfurnaces: energy / cookTotal per tick)
                int rfPerTick = Math.max(1, (int)((double)energyRecipe / (double)cookTotal));
                if (tile.getEnergy() < rfPerTick) continue; // Not enough RF even for 1 tick - freeze slot

                tile.setEnergy(tile.getEnergy() - rfPerTick);
                tile.usedRF[i] += rfPerTick;
                rfConsumed += rfPerTick;

                tile.factoryCookTime[i]++;
                tickWorked = true;
                anyWorked = true;

                if (tile.factoryCookTime[i] >= cookTotal) {
                    tile.factoryCookTime[i] = 0;
                    // Deduct remaining RF cost (mirrors original ironfurnaces logic)
                    if (tile.usedRF[i] < (double)energyRecipe) {
                        double diff = (double)energyRecipe - tile.usedRF[i];
                        int actualDrain = Math.min(tile.getEnergy(), (int)diff);
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
            String itemStr = finalOutput > 0 ? String.format("smelted: §a%d", finalOutput) : "smelted: 0";
            String rfStr = String.format("rf: -%d (pull=%d, %dRF/item)", rfConsumed, pulledRf, rfPerItem);
            Component msg = Component.literal(
                    String.format("§7[§6KeepSmelting§7] §e[Factory] §f%s §7| §e%d§7t§r | %s §7| %s",
                            pos.toShortString(), elapsed,
                            itemStr, rfStr));
            sendToNearbyPlayers((ServerLevel) level, pos, msg);
        }
    }

    // ══════════════════════════════════════════════
    //  Generator
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
                    KeepSmelting.LOGGER.info("[Generator IO] fuel empty → autoIOGenerator, got={}", fuel.getCount());
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
    //  Neighbor generator pre-processing
    // ══════════════════════════════════════════════

    /**
     * Scans NSEW neighbors for generator-mode furnaces and runs their
     * catchup FIRST, so factory can pull freshly-generated RF.
     * Marks them so their own tick skips catchup (no double-process).
     */
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
                // Run generator catchup now so RF is generated
                applyGeneratorCatchup(genTile, elapsed, level, neighbor);
                mixinSelf.keepsmelting$catchupDone = true;
            }
        }
    }

    // ══════════════════════════════════════════════
    //  RF pull: factory takes ALL RF from neighbor generators
    // ══════════════════════════════════════════════

    /**
     * Scans 4-directional neighbors (NSEW) for generator-mode furnaces.
     * Only pulls RF if generator side facing factory is set to Output(2) or I/O(3).
     * Respects furnaceSettings per-side config.
     */
    @Unique
    private static int pullAllRFFromNeighborGenerators(BlockIronFurnaceTileBase tile, Level level,
                                                       BlockPos pos) {
        int totalPulled = 0;
        for (Direction dir : Direction.Plane.HORIZONTAL) {
            BlockPos neighbor = pos.relative(dir);
            if (!level.isLoaded(neighbor)) continue;
            BlockEntity be = level.getBlockEntity(neighbor);
            if (be instanceof BlockIronFurnaceTileBase neighborTile && neighborTile.isGenerator()) {
                // Check generator's side facing this factory: must be Output(2) or I/O(3)
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
