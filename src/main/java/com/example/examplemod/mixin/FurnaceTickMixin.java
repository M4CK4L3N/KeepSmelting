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
 * Hopper I/O (always enabled) pulls input/fuel from adjacent containers
 * and pushes output below, enabling offline hopper furnace chains.
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

        if (!acc.callIsLit()) return;

        // snapshots for debug
        NonNullList<ItemStack> items = acc.getItems();
        int outputBefore = items.get(2).getCount();
        int fuelBefore = items.get(1).getCount();
        int progressBefore = acc.getCookingProgress();

        long remaining = deltaTime;

        // ── One-shot hopper I/O: push output, fill input, fill fuel ──
        {
            // How many hopper operations fire during deltaTime (8 ticks per item)
            int hopperOps = Math.max(1, (int)(deltaTime / 8));

            // 1) Push output below (up to hopperOps items)
            if (!items.get(2).isEmpty()) {
                pushToBelow(furnace, level, pos, hopperOps);
                items = acc.getItems();
            }
            // 2) Fill input slot from above hopper (1 item per 8 ticks)
            int space = furnace.getMaxStackSize() - items.get(0).getCount();
            if (space > 0) {
                fillInputFromAbove(furnace, level, pos, Math.min(hopperOps, space));
                items = acc.getItems();
            }
            // 3) Pre-pull fuel if empty (1 item per catchup)
            if (items.get(1).isEmpty() && acc.getLitTime() <= 0) {
                pullFuelFromSides(furnace, level, pos);
                items = acc.getItems();
                if (!items.get(1).isEmpty()) {
                    int bt = ForgeHooks.getBurnTime(items.get(1), RecipeType.SMELTING);
                    if (bt > 0) {
                        ItemStack f = items.get(1);
                        acc.setLitTime(bt);
                        f.shrink(1);
                        if (f.isEmpty()) items.set(1, f.getCraftingRemainingItem());
                        BlockState bs = level.getBlockState(pos);
                        if (!bs.getValue(AbstractFurnaceBlock.LIT)) {
                            level.setBlock(pos, bs.setValue(AbstractFurnaceBlock.LIT, true), 3);
                        }
                    }
                }
            }
        }

        ItemStack input = items.get(0);
        ItemStack output = items.get(2);
        ItemStack fuel = items.get(1);

        if (input.isEmpty()) return;
        if (!output.isEmpty() && output.getCount() >= furnace.getMaxStackSize()) return;

        Recipe<?> recipe = findRecipe(furnace, level);
        if (recipe == null) return;

        if (!acc.callCanBurn(level.registryAccess(), recipe, items, furnace.getMaxStackSize())) return;

        int cookTotal = acc.getCookingTotalTime();
        if (cookTotal <= 0) return;

        int litDur = acc.getLitDuration();
        if (litDur <= 0) return;

        // max ticks available from fuel
        long fuelTicks;
        if (fuel.isEmpty()) {
            fuelTicks = acc.getLitTime();
        } else {
            fuelTicks = (long) (fuel.getCount() - 1) * litDur + acc.getLitTime();
        }

        // ticks needed to smelt all current input (counting partial progress)
        long cookTicksNeeded = (long) (input.getCount() - 1) * cookTotal
                + (cookTotal - acc.getCookingProgress());

        long applyTicks = Math.min(remaining, Math.min(fuelTicks, cookTicksNeeded));
        if (applyTicks <= 0) return;

        int inputCountBefore = input.getCount();

        // Burn fuel
        applyFuelTime(acc, fuel, applyTicks, litDur);

        // Cook items
        applyCookTime(level, furnace, acc, recipe, input, applyTicks, cookTotal);

        // ── Count how many items were actually produced ──
        int itemsProduced = inputCountBefore - items.get(0).getCount();

        furnace.setChanged();

        // toggle lit state if fuel exhausted
        if (!acc.callIsLit()) {
            BlockState bs = level.getBlockState(pos);
            if (bs.hasProperty(AbstractFurnaceBlock.LIT)) {
                level.setBlock(pos, bs.setValue(AbstractFurnaceBlock.LIT, false), 3);
            }
            furnace.setChanged();
        }

        // ── Final push: flush output to below ──
        if (!acc.getItems().get(2).isEmpty()) {
            pushToBelow(furnace, level, pos, Integer.MAX_VALUE);
        }

        // ── debug output — only when something actually happened ──
        if (KeepSmeltingConfig.COMMON.debugMode.get() != KeepSmeltingConfig.DebugMode.OFF) {
            items = acc.getItems();
            boolean lit = acc.callIsLit();
            int progressDelta = acc.getCookingProgress() - progressBefore;

            if (progressDelta == 0 && itemsProduced == 0 && fuelBefore == items.get(1).getCount()) return;

            int fuelUsed = fuelBefore - items.get(1).getCount();
            String fuelStr = fuelUsed > 0 ? String.format("fuel: -%d", fuelUsed) : "fuel: 0";
            String itemStr = itemsProduced > 0 ? String.format("smelted: §a%d", itemsProduced) : "smelted: 0";

            Component msg = Component.literal(
                    String.format("§7[§6KeepSmelting§7] §f%s §7| §e%d§7t§r | %s §7| %s §7| §7lit=%s",
                            pos.toShortString(), deltaTime,
                            itemStr, fuelStr, lit));
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
     * Fill furnace input slot from container above. Pulls as many items as possible (up to maxStack).
     * Does nothing if slot is full. Only one pull per catchup (no loop).
     */
    @Unique
    private static void fillInputFromAbove(AbstractFurnaceBlockEntity furnace, ServerLevel level, BlockPos pos, int maxToPull) {
        BlockPos above = pos.above();
        if (!level.isLoaded(above)) return;
        BlockEntity be = level.getBlockEntity(above);
        if (!(be instanceof Container source)) return;

        ItemStack current = furnace.getItem(0);

        for (int i = 0; i < source.getContainerSize(); i++) {
            ItemStack src = source.getItem(i);
            if (src.isEmpty()) continue;
            if (!isSmeltable(furnace, src)) continue;

            int canTake = Math.min(src.getCount(), maxToPull);
            if (canTake <= 0) continue;

            if (current.isEmpty()) {
                // Place first item
                ItemStack taken = source.removeItem(i, Math.min(canTake, src.getMaxStackSize()));
                if (taken.isEmpty()) continue;
                furnace.setItem(0, taken);
                current = taken;
            } else if (ItemStack.isSameItemSameTags(current, src)) {
                int space = Math.min(furnace.getMaxStackSize(), current.getMaxStackSize()) - current.getCount();
                if (space <= 0) break;
                int toTake = Math.min(src.getCount(), Math.min(space, maxToPull));
                ItemStack taken = source.removeItem(i, toTake);
                if (taken.isEmpty()) continue;
                current.grow(taken.getCount());
                furnace.setItem(0, current);
            } else {
                continue; // wrong item type, try next slot
            }

            source.setChanged();
            furnace.setChanged();
            return;
        }
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
    private static void pushToBelow(AbstractFurnaceBlockEntity furnace, ServerLevel level, BlockPos pos, int maxItems) {
        BlockPos below = pos.below();
        if (!level.isLoaded(below)) return;
        BlockEntity be = level.getBlockEntity(below);
        if (!(be instanceof Container dest)) return;

        ItemStack output = furnace.getItem(2);
        if (output.isEmpty()) return;

        int maxStack = output.getMaxStackSize();
        int toTransfer = Math.min(output.getCount(), maxItems);
        if (toTransfer <= 0) return;

        if (dest instanceof WorldlyContainer wc) {
            for (int i : wc.getSlotsForFace(Direction.UP)) {
                ItemStack destStack = dest.getItem(i);
                int slotLimit = Math.min(maxStack, dest.getMaxStackSize());

                if (destStack.getCount() >= slotLimit) continue;

                if (destStack.isEmpty()) {
                    if (!wc.canPlaceItem(i, output)) continue;
                    int transfer = Math.min(toTransfer, slotLimit);
                    ItemStack put = output.copy();
                    put.setCount(transfer);
                    output.shrink(transfer);
                    toTransfer -= transfer;
                    dest.setItem(i, put);
                    if (output.isEmpty()) furnace.setItem(2, ItemStack.EMPTY);
                    dest.setChanged();
                    furnace.setChanged();
                    if (toTransfer <= 0) return;
                } else if (ItemStack.isSameItemSameTags(destStack, output)) {
                    int space = slotLimit - destStack.getCount();
                    int transfer = Math.min(toTransfer, space);
                    destStack.grow(transfer);
                    output.shrink(transfer);
                    toTransfer -= transfer;
                    if (output.isEmpty()) furnace.setItem(2, ItemStack.EMPTY);
                    dest.setChanged();
                    furnace.setChanged();
                    if (toTransfer <= 0) return;
                }
            }
        } else {
            for (int i = 0; i < dest.getContainerSize(); i++) {
                ItemStack destStack = dest.getItem(i);
                int slotLimit = Math.min(maxStack, dest.getMaxStackSize());

                if (destStack.getCount() >= slotLimit) continue;

                if (destStack.isEmpty()) {
                    int transfer = Math.min(toTransfer, slotLimit);
                    ItemStack put = output.copy();
                    put.setCount(transfer);
                    output.shrink(transfer);
                    toTransfer -= transfer;
                    dest.setItem(i, put);
                    if (output.isEmpty()) furnace.setItem(2, ItemStack.EMPTY);
                    dest.setChanged();
                    furnace.setChanged();
                    if (toTransfer <= 0) return;
                } else if (ItemStack.isSameItemSameTags(destStack, output)) {
                    int space = slotLimit - destStack.getCount();
                    int transfer = Math.min(toTransfer, space);
                    destStack.grow(transfer);
                    output.shrink(transfer);
                    toTransfer -= transfer;
                    if (output.isEmpty()) furnace.setItem(2, ItemStack.EMPTY);
                    dest.setChanged();
                    furnace.setChanged();
                    if (toTransfer <= 0) return;
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
