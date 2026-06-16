package com.example.examplemod.mixin;

import com.example.examplemod.KeepSmelting;
import com.example.examplemod.KeepSmeltingConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.AbstractFurnaceBlock;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.common.ForgeHooks;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Optional;

/**
 * Core mixin — intercepts AbstractFurnaceBlockEntity.serverTick.
 * <p>
 * Tracks real-world time (System.currentTimeMillis) so catchup works
 * across chunk unload, game exit, and menu pause.
 * <p>
 * Logic mirrors EverFurnace's FurnaceCatchupHandler:
 *   • burn fuel ticks for the elapsed period
 *   • advance cooking progress
 *   • produce finished items
 *   • update block state when fuel runs out
 * <p>
 * Optional hopper I/O (config vanillaHopperIO) pulls input/fuel from
 * adjacent containers and pushes output below, enabling offline hopper
 * furnace chains.
 */
@Mixin(AbstractFurnaceBlockEntity.class)
public abstract class FurnaceTickMixin {

    @Unique
    private static final String LAST_REAL_TIME_TAG = "keepsmelting_lastRealTime";
    @Unique
    private static final String NBT_VERSION_TAG = "keepsmelting_version";
    @Unique
    private static final int CURRENT_NBT_VERSION = 1;

    @Unique
    private long keepsmelting$lastRealTime;

    @Unique
    private String keepsmelting$activeTimeMode;

    // ──────────────────────────────────────────────
    //  NBT save / load
    // ──────────────────────────────────────────────

    @Inject(method = "saveAdditional", at = @At("TAIL"))
    private void onSave(net.minecraft.nbt.CompoundTag tag, CallbackInfo ci) {
        tag.putInt(NBT_VERSION_TAG, CURRENT_NBT_VERSION);
        tag.putLong(LAST_REAL_TIME_TAG, this.keepsmelting$lastRealTime);
        tag.putString("keepsmelting_timeMode", KeepSmeltingConfig.COMMON.timeMode.get().name());
    }

    @Inject(method = "load", at = @At("TAIL"))
    private void onLoad(net.minecraft.nbt.CompoundTag tag, CallbackInfo ci) {
        String savedMode = tag.getString("keepsmelting_timeMode");
        String currentMode = KeepSmeltingConfig.COMMON.timeMode.get().name();
        if (!savedMode.isEmpty() && savedMode.equals(currentMode)) {
            this.keepsmelting$lastRealTime = tag.getLong(LAST_REAL_TIME_TAG);
        } else {
            this.keepsmelting$lastRealTime = 0L;
        }
    }

    // ──────────────────────────────────────────────
    //  serverTick interception
    // ──────────────────────────────────────────────

    @Inject(method = "serverTick", at = @At("HEAD"))
    private static void onTick(Level world, BlockPos pos, BlockState state,
                               AbstractFurnaceBlockEntity blockEntity, CallbackInfo ci) {
        if (world.isClientSide) return;

        if (!KeepSmeltingConfig.COMMON.catchupEnabled.get()) return;

        FurnaceTickMixin self = (FurnaceTickMixin) (Object) blockEntity;

        String currentMode = KeepSmeltingConfig.COMMON.timeMode.get().name();
        if (self.keepsmelting$activeTimeMode != null && !self.keepsmelting$activeTimeMode.equals(currentMode)) {
            self.keepsmelting$lastRealTime = 0L;
        }
        self.keepsmelting$activeTimeMode = currentMode;

        long lastReal = self.keepsmelting$lastRealTime;
        long elapsedTicks;
        if (KeepSmeltingConfig.COMMON.timeMode.get() == KeepSmeltingConfig.TimeMode.GAMETIME) {
            self.keepsmelting$lastRealTime = world.getGameTime();
            elapsedTicks = self.keepsmelting$lastRealTime - lastReal;
        } else {
            self.keepsmelting$lastRealTime = System.currentTimeMillis();
            elapsedTicks = (self.keepsmelting$lastRealTime - lastReal) / 50L;
        }

        if (lastReal == 0) return;

        int minDelta = KeepSmeltingConfig.COMMON.minDeltaThreshold.get();
        if (elapsedTicks < minDelta) return;

        long maxTicks = KeepSmeltingConfig.COMMON.maxCatchupTicks.get();
        elapsedTicks = Math.min(elapsedTicks, maxTicks);
        if (elapsedTicks <= 0) return;

        applyCatchup(blockEntity, elapsedTicks, (ServerLevel) world, pos);
    }

    // ──────────────────────────────────────────────
    //  Catchup — loop with optional hopper IO
    // ──────────────────────────────────────────────

    @Unique
    private static void applyCatchup(AbstractFurnaceBlockEntity furnace, long deltaTime,
                                     ServerLevel level, BlockPos pos) {
        IFurnaceAccessor acc = (IFurnaceAccessor) furnace;

        if (!acc.callIsLit()) {
            KeepSmelting.LOGGER.info("[HopperIO] furnace {} not lit, skip catchup", pos.toShortString());
            return;
        }

        boolean hopperIO = KeepSmeltingConfig.COMMON.vanillaHopperIO.get();

        // snapshots for debug
        NonNullList<ItemStack> items = acc.getItems();
        int outputBefore = items.get(2).getCount();
        int fuelBefore = items.get(1).getCount();
        int progressBefore = acc.getCookingProgress();

        // real item counter — survives pushToBelow flushing output slot
        int totalItemsCooked = 0;
        long remaining = deltaTime;

        while (remaining > 0) {
            items = acc.getItems();
            ItemStack input = items.get(0);
            ItemStack output = items.get(2);
            ItemStack fuel = items.get(1);

            // ── Hopper I/O: push output below (always when not empty) ──
            if (hopperIO && !output.isEmpty()) {
                pushToBelow(furnace, level, pos);
                output = items.get(2);
            }

            // ── Hopper I/O: pull input from above ──
            if (hopperIO && input.isEmpty()) {
                pullFromAbove(furnace, level, pos);
                input = items.get(0);
            }

            // ── Hopper I/O: push output below again (after possible input pull) ──
            if (hopperIO && !output.isEmpty()) {
                pushToBelow(furnace, level, pos);
                output = items.get(2);
            }

            if (input.isEmpty()) {
                if (hopperIO && KeepSmeltingConfig.COMMON.debugMode.get() != KeepSmeltingConfig.DebugMode.OFF) {
                    KeepSmelting.LOGGER.info("[HopperIO] {} input still empty after pullAbove, break loop", pos.toShortString());
                }
                break;
            }
            if (!output.isEmpty() && output.getCount() >= furnace.getMaxStackSize()) {
                if (hopperIO && KeepSmeltingConfig.COMMON.debugMode.get() != KeepSmeltingConfig.DebugMode.OFF) {
                    KeepSmelting.LOGGER.info("[HopperIO] {} output still full after pushBelow, break loop", pos.toShortString());
                }
                break;
            }

            Recipe<?> recipe = findRecipe(furnace, level);
            if (recipe == null) {
                if (hopperIO && KeepSmeltingConfig.COMMON.debugMode.get() != KeepSmeltingConfig.DebugMode.OFF) {
                    KeepSmelting.LOGGER.info("[HopperIO] {} no recipe for input, break", pos.toShortString());
                }
                break;
            }

            if (!acc.callCanBurn(level.registryAccess(), recipe, items, furnace.getMaxStackSize())) {
                if (hopperIO && KeepSmeltingConfig.COMMON.debugMode.get() != KeepSmeltingConfig.DebugMode.OFF) {
                    KeepSmelting.LOGGER.info("[HopperIO] {} callCanBurn false, break", pos.toShortString());
                }
                break;
            }

            int cookTotal = acc.getCookingTotalTime();
            if (cookTotal <= 0) break;

            int litDur = acc.getLitDuration();
            if (litDur <= 0) break;

            // snapshot input count before cooking
            int inputCountBefore = items.get(0).getCount();

            // max ticks available — handle empty fuel (last ember still burning)
            long fuelTicks;
            if (fuel.isEmpty()) {
                fuelTicks = acc.getLitTime();
            } else {
                fuelTicks = (long) (fuel.getCount() - 1) * litDur + acc.getLitTime();
            }
            // max ticks needed to smelt all input
            long cookTicksNeeded = (long) (inputCountBefore - 1) * cookTotal
                    + (cookTotal - acc.getCookingProgress());

            long applyTicks = Math.min(remaining, Math.min(fuelTicks, cookTicksNeeded));
            if (applyTicks <= 0) break;

            // 1) burn fuel
            applyFuelTime(acc, fuel, applyTicks, litDur);

            // 2) smelt items
            int cookedBefore = totalItemsCooked;
            applyCookTime(level, furnace, acc, recipe, input, applyTicks, cookTotal);

            // track items produced by checking how many input items disappeared
            int inputConsumed = inputCountBefore - items.get(0).getCount();
            if (inputConsumed > 0) {
                totalItemsCooked += inputConsumed;
            } else {
                // fallback: count by output slot change (works without pushToBelow)
                int outputNow = items.get(2).getCount();
                totalItemsCooked += outputNow - (outputBefore + (totalItemsCooked - cookedBefore));
            }

            remaining -= applyTicks;
            furnace.setChanged();

            // toggle lit state if fuel exhausted
            if (!acc.callIsLit()) {
                BlockState bs = level.getBlockState(pos);
                if (bs.hasProperty(AbstractFurnaceBlock.LIT)) {
                    level.setBlock(pos, bs.setValue(AbstractFurnaceBlock.LIT, false), 3);
                }
                furnace.setChanged();
            }

            // ── Hopper I/O after batch: push output, pull fuel ──
            if (hopperIO) {
                pushToBelow(furnace, level, pos);

                // try reload fuel from side hoppers if empty
                if (!acc.callIsLit() || acc.getLitTime() <= 0) {
                    KeepSmelting.LOGGER.info("[HopperIO] {} fuel ran out, try pullFuelFromSides", pos.toShortString());
                    pullFuelFromSides(furnace, level, pos);
                    items = acc.getItems();
                    fuel = items.get(1);
                    if (!fuel.isEmpty()) {
                        int burnTime = ForgeHooks.getBurnTime(fuel, RecipeType.SMELTING);
                        KeepSmelting.LOGGER.info("[HopperIO] {} pullFuelFromSides got {} (burnTime={})", pos.toShortString(), fuel.getCount(), burnTime);
                        if (burnTime > 0) {
                            acc.setLitTime(burnTime);
                            fuel.shrink(1);
                            if (fuel.getCount() <= 0) {
                                items.set(1, fuel.getCraftingRemainingItem());
                            }
                            BlockState bs = level.getBlockState(pos);
                            if (!bs.getValue(AbstractFurnaceBlock.LIT)) {
                                level.setBlock(pos, bs.setValue(AbstractFurnaceBlock.LIT, true), 3);
                            }
                        }
                    } else {
                        KeepSmelting.LOGGER.info("[HopperIO] {} pullFuelFromSides found nothing", pos.toShortString());
                    }
                }
            }

            // guard: no fuel and no way to get more
            if (!acc.callIsLit()) {
                if (!hopperIO) break;
                items = acc.getItems();
                fuel = items.get(1);
                if (fuel.isEmpty() || ForgeHooks.getBurnTime(fuel, RecipeType.SMELTING) <= 0) break;
            }
        }

        // ── debug output — only when something actually happened ──
        if (KeepSmeltingConfig.COMMON.debugMode.get() != KeepSmeltingConfig.DebugMode.OFF) {
            items = acc.getItems();
            int outputDelta = items.get(2).getCount() - outputBefore;
            int fuelDelta = fuelBefore - items.get(1).getCount();
            int progressDelta = acc.getCookingProgress() - progressBefore;
            boolean lit = acc.callIsLit();

            if (outputDelta == 0 && fuelDelta == 0 && progressDelta == 0 && totalItemsCooked == 0) return;

            String extra = "";
            if (hopperIO && totalItemsCooked > 0) {
                extra = String.format(" §7(§a%d§7 actual items)", totalItemsCooked);
            }
            Component msg = Component.literal(
                    String.format("§7[§6KeepSmelting§7] §f%s §7| §e%d§7t | §a+%ditem §c-%dfuel §7lit=%s%s",
                            pos.toShortString(), deltaTime,
                            outputDelta,
                            fuelDelta,
                            lit,
                            extra));
            if (KeepSmeltingConfig.COMMON.debugMode.get() == KeepSmeltingConfig.DebugMode.LOG) {
                KeepSmelting.LOGGER.info(msg.getString());
            } else {
                sendToNearbyPlayers(level, pos, msg);
            }
        }
    }

    // ══════════════════════════════════════════════
    //  Hopper I/O helpers
    // ══════════════════════════════════════════════

    /**
     * Simulate hopper: pull 1 smeltable item from container above furnace input slot.
     */
    @Unique
    private static void pullFromAbove(AbstractFurnaceBlockEntity furnace, ServerLevel level, BlockPos pos) {
        BlockPos above = pos.above();
        KeepSmelting.LOGGER.info("[pullFromAbove] {} check above {}", pos.toShortString(), above.toShortString());
        if (!level.isLoaded(above)) {
            KeepSmelting.LOGGER.info("[pullFromAbove] {} above not loaded", pos.toShortString());
            return;
        }
        BlockEntity be = level.getBlockEntity(above);
        if (be == null) {
            KeepSmelting.LOGGER.info("[pullFromAbove] {} no block entity above", pos.toShortString());
            return;
        }
        if (!(be instanceof Container source)) {
            KeepSmelting.LOGGER.info("[pullFromAbove] {} not Container: {}", pos.toShortString(), be.getClass().getName());
            return;
        }

        ItemStack current = furnace.getItem(0);
        KeepSmelting.LOGGER.info("[pullFromAbove] {} above size={} current={}", pos.toShortString(), source.getContainerSize(), current);

        for (int i = 0; i < source.getContainerSize(); i++) {
            ItemStack src = source.getItem(i);
            if (src.isEmpty()) {
                KeepSmelting.LOGGER.info("[pullFromAbove] {} slot{} empty", pos.toShortString(), i);
                continue;
            }
            String itemName = src.getHoverName().getString();
            boolean smeltable = isSmeltable(furnace, src);
            KeepSmelting.LOGGER.info("[pullFromAbove] {} slot{} item={} count={} smeltable={}", pos.toShortString(), i, itemName, src.getCount(), smeltable);
            if (!smeltable) continue;

            ItemStack extracted = source.removeItem(i, 1);
            KeepSmelting.LOGGER.info("[pullFromAbove] {} extracted={}", pos.toShortString(), extracted);
            if (extracted.isEmpty()) continue;

            if (current.isEmpty()) {
                furnace.setItem(0, extracted);
                KeepSmelting.LOGGER.info("[pullFromAbove] {} placed in input", pos.toShortString());
            } else if (ItemStack.isSameItemSameTags(current, extracted)
                    && current.getCount() < current.getMaxStackSize()) {
                current.grow(1);
                furnace.setItem(0, current);
                KeepSmelting.LOGGER.info("[pullFromAbove] {} stacked input now {}", pos.toShortString(), current.getCount());
            } else {
                source.setItem(i, extracted);
                KeepSmelting.LOGGER.info("[pullFromAbove] {} put back, slot mismatch", pos.toShortString());
                continue;
            }
            source.setChanged();
            furnace.setChanged();
            return;
        }
        KeepSmelting.LOGGER.info("[pullFromAbove] {} no suitable item found in {}slots", pos.toShortString(), source.getContainerSize());
    }

    /**
     * Simulate hopper: pull 1 burnable item from side containers (NSEW) into fuel slot.
     */
    @Unique
    private static void pullFuelFromSides(AbstractFurnaceBlockEntity furnace, ServerLevel level, BlockPos pos) {
        for (Direction dir : Direction.Plane.HORIZONTAL) {
            BlockPos neighbor = pos.relative(dir);
            if (!level.isLoaded(neighbor)) continue;
            BlockEntity be = level.getBlockEntity(neighbor);
            if (!(be instanceof Container source)) continue;

            ItemStack current = furnace.getItem(1);

            for (int i = 0; i < source.getContainerSize(); i++) {
                ItemStack src = source.getItem(i);
                if (src.isEmpty()) continue;
                if (ForgeHooks.getBurnTime(src, RecipeType.SMELTING) <= 0) continue;

                ItemStack extracted = source.removeItem(i, 1);
                if (extracted.isEmpty()) continue;

                if (current.isEmpty()) {
                    furnace.setItem(1, extracted);
                } else if (ItemStack.isSameItemSameTags(current, extracted)
                        && current.getCount() < current.getMaxStackSize()) {
                    current.grow(1);
                    furnace.setItem(1, current);
                } else {
                    source.setItem(i, extracted);
                    continue;
                }
                source.setChanged();
                furnace.setChanged();
                return;
            }
        }
    }

    /**
     * Simulate hopper: push output items into container below furnace.
     */
    @Unique
    private static void pushToBelow(AbstractFurnaceBlockEntity furnace, ServerLevel level, BlockPos pos) {
        BlockPos below = pos.below();
        if (!level.isLoaded(below)) return;
        BlockEntity be = level.getBlockEntity(below);
        if (!(be instanceof Container dest)) return;

        ItemStack output = furnace.getItem(2);
        if (output.isEmpty()) return;

        int maxStack = output.getMaxStackSize();

        if (dest instanceof WorldlyContainer wc) {
            for (int i : wc.getSlotsForFace(Direction.UP)) {
                ItemStack destStack = dest.getItem(i);
                int slotLimit = Math.min(maxStack, dest.getMaxStackSize());

                if (destStack.getCount() >= slotLimit) continue;

                if (destStack.isEmpty()) {
                    if (!wc.canPlaceItem(i, output)) continue;
                    int transfer = Math.min(output.getCount(), slotLimit);
                    ItemStack put = output.copy();
                    put.setCount(transfer);
                    output.shrink(transfer);
                    dest.setItem(i, put);
                    if (output.isEmpty()) furnace.setItem(2, ItemStack.EMPTY);
                    dest.setChanged();
                    furnace.setChanged();
                    return;
                } else if (ItemStack.isSameItemSameTags(destStack, output)) {
                    int space = slotLimit - destStack.getCount();
                    int transfer = Math.min(output.getCount(), space);
                    destStack.grow(transfer);
                    output.shrink(transfer);
                    if (output.isEmpty()) furnace.setItem(2, ItemStack.EMPTY);
                    dest.setChanged();
                    furnace.setChanged();
                    return;
                }
            }
        } else {
            for (int i = 0; i < dest.getContainerSize(); i++) {
                ItemStack destStack = dest.getItem(i);
                int slotLimit = Math.min(maxStack, dest.getMaxStackSize());

                if (destStack.getCount() >= slotLimit) continue;

                if (destStack.isEmpty()) {
                    int transfer = Math.min(output.getCount(), slotLimit);
                    ItemStack put = output.copy();
                    put.setCount(transfer);
                    output.shrink(transfer);
                    dest.setItem(i, put);
                    if (output.isEmpty()) furnace.setItem(2, ItemStack.EMPTY);
                    dest.setChanged();
                    furnace.setChanged();
                    return;
                } else if (ItemStack.isSameItemSameTags(destStack, output)) {
                    int space = slotLimit - destStack.getCount();
                    int transfer = Math.min(output.getCount(), space);
                    destStack.grow(transfer);
                    output.shrink(transfer);
                    if (output.isEmpty()) furnace.setItem(2, ItemStack.EMPTY);
                    dest.setChanged();
                    furnace.setChanged();
                    return;
                }
            }
        }
    }

    @Unique
    private static boolean isSmeltable(AbstractFurnaceBlockEntity furnace, ItemStack stack) {
        if (stack.isEmpty()) {
            KeepSmelting.LOGGER.info("[isSmeltable] empty stack");
            return false;
        }
        Level level = furnace.getLevel();
        if (level == null) {
            KeepSmelting.LOGGER.info("[isSmeltable] level null");
            return false;
        }
        RecipeType<?> recipeType = recipeTypeFor(furnace);
        SimpleContainer testInv = new SimpleContainer(stack);
        boolean found = level.getRecipeManager().getRecipeFor((RecipeType) recipeType, testInv, level).isPresent();
        if (!found) {
            KeepSmelting.LOGGER.info("[isSmeltable] {} no recipe found for type {} (item={})",
                furnace.getBlockPos().toShortString(), recipeType, stack.getHoverName().getString());
        }
        return found;
    }

    // ──────────────────────────────────────────────
    //  Fuel burning (same algo as EverFurnace)
    // ──────────────────────────────────────────────

    @Unique
    private static void applyFuelTime(IFurnaceAccessor acc, ItemStack fuelStack,
                                      long ticks, int litDuration) {
        long remaining = ticks;
        int litTime = acc.getLitTime();

        if (remaining <= litTime) {
            acc.setLitTime(litTime - (int) remaining);
            return;
        }

        remaining -= litTime;

        // if no fuel left, burn what we can and stop
        if (fuelStack.isEmpty()) {
            acc.setLitTime(0);
            return;
        }

        int wholeItems = (int) Math.ceil((double) remaining / litDuration);
        wholeItems = Math.min(wholeItems, fuelStack.getCount());

        long ticksFromNew = (long) wholeItems * litDuration;
        long leftover = ticksFromNew - remaining;

        fuelStack.shrink(wholeItems);
        if (fuelStack.isEmpty()) {
            acc.setLitTime(0);
        } else {
            acc.setLitTime((int) leftover);
        }
    }

    // ──────────────────────────────────────────────
    //  Cooking application (same algo as EverFurnace)
    // ──────────────────────────────────────────────

    @Unique
    private static void applyCookTime(Level world, AbstractFurnaceBlockEntity furnace,
                                      IFurnaceAccessor acc, Recipe<?> recipe,
                                      ItemStack input, long ticks, int cookTotal) {
        int progress = acc.getCookingProgress();
        int neededForCurrent = cookTotal - progress;

        if (ticks < neededForCurrent) {
            acc.setCookingProgress(progress + (int) ticks);
            return;
        }

        long remaining = ticks - neededForCurrent;

        // finish current item
        acc.setCookingProgress(cookTotal);
        if (acc.callBurn(world.registryAccess(), recipe, acc.getItems(), furnace.getMaxStackSize())) {
            furnace.setRecipeUsed(recipe);
        }
        acc.setCookingProgress(0);

        // smelt additional full items
        if (!input.isEmpty() && cookTotal > 0) {
            long fullItems = remaining / cookTotal;
            int remainder = (int) (remaining % cookTotal);

            for (long i = 0; i < fullItems; i++) {
                if (!acc.callCanBurn(world.registryAccess(), recipe, acc.getItems(), furnace.getMaxStackSize()))
                    break;
                if (acc.callBurn(world.registryAccess(), recipe, acc.getItems(), furnace.getMaxStackSize())) {
                    furnace.setRecipeUsed(recipe);
                }
            }

            acc.setCookingProgress(remainder);
        }
    }

    // ──────────────────────────────────────────────
    //  Recipe lookup
    // ──────────────────────────────────────────────

    @Nullable
    @Unique
    @SuppressWarnings({"rawtypes", "unchecked"})
    private static Recipe<?> findRecipe(AbstractFurnaceBlockEntity furnace, Level level) {
        RecipeType recipeType = (RecipeType) recipeTypeFor(furnace);
        Optional opt = level.getRecipeManager().getRecipeFor(recipeType, (Container) furnace, level);
        return (Recipe<?>) opt.orElse(null);
    }

    @Unique
    private static RecipeType<?> recipeTypeFor(AbstractFurnaceBlockEntity furnace) {
        BlockEntityType<?> type = furnace.getType();
        if (type == BlockEntityType.SMOKER) {
            return RecipeType.SMOKING;
        } else if (type == BlockEntityType.BLAST_FURNACE) {
            return RecipeType.BLASTING;
        } else {
            return RecipeType.SMELTING;
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
