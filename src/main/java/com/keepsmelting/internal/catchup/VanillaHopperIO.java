package com.keepsmelting.internal.catchup;

import com.keepsmelting.mixin.IFurnaceAccessor;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.common.ForgeHooks;

import javax.annotation.Nullable;
import java.util.Optional;

public class VanillaHopperIO {

    public static void fillInputFromAbove(AbstractFurnaceBlockEntity furnace, ServerLevel level, BlockPos pos, int maxToPull) {
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
                continue;
            }
            source.setChanged();
            furnace.setChanged();
            return;
        }
    }

    public static void pullFuelFromSides(AbstractFurnaceBlockEntity furnace, ServerLevel level, BlockPos pos) {
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

    public static void pushToBelow(AbstractFurnaceBlockEntity furnace, ServerLevel level, BlockPos pos, int maxItems) {
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

    public static boolean isSmeltable(AbstractFurnaceBlockEntity furnace, ItemStack stack) {
        if (stack.isEmpty()) return false;
        Level level = furnace.getLevel();
        if (level == null) return false;
        RecipeType<?> recipeType = recipeTypeFor(furnace);
        SimpleContainer testInv = new SimpleContainer(stack);
        return level.getRecipeManager().getRecipeFor((RecipeType) recipeType, testInv, level).isPresent();
    }

    public static void applyFuelTime(IFurnaceAccessor acc, ItemStack fuelStack, long ticks, int litDuration) {
        long remaining = ticks;
        int litTime = acc.getLitTime();
        if (remaining <= litTime) {
            acc.setLitTime(litTime - (int) remaining);
            return;
        }
        remaining -= litTime;
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

    public static void applyCookTime(Level world, AbstractFurnaceBlockEntity furnace,
                                     IFurnaceAccessor acc, Recipe<?> recipe,
                                     ItemStack input, long ticks, int cookTotal) {
        int progress = acc.getCookingProgress();
        int neededForCurrent = cookTotal - progress;
        if (ticks < neededForCurrent) {
            acc.setCookingProgress(progress + (int) ticks);
            return;
        }
        long remaining = ticks - neededForCurrent;
        acc.setCookingProgress(cookTotal);
        if (acc.callBurn(world.registryAccess(), recipe, acc.getItems(), furnace.getMaxStackSize())) {
            furnace.setRecipeUsed(recipe);
        }
        acc.setCookingProgress(0);
        if (!input.isEmpty() && cookTotal > 0) {
            long fullItems = remaining / cookTotal;
            int remainder = (int) (remaining % cookTotal);
            for (long i = 0; i < fullItems; i++) {
                if (!acc.callCanBurn(world.registryAccess(), recipe, acc.getItems(), furnace.getMaxStackSize())) break;
                if (acc.callBurn(world.registryAccess(), recipe, acc.getItems(), furnace.getMaxStackSize())) {
                    furnace.setRecipeUsed(recipe);
                }
            }
            acc.setCookingProgress(remainder);
        }
    }

    @Nullable
    @SuppressWarnings({"rawtypes", "unchecked"})
    public static Recipe<?> findRecipe(AbstractFurnaceBlockEntity furnace, Level level) {
        RecipeType recipeType = (RecipeType) recipeTypeFor(furnace);
        Optional opt = level.getRecipeManager().getRecipeFor(recipeType, (Container) furnace, level);
        return (Recipe<?>) opt.orElse(null);
    }

    public static RecipeType<?> recipeTypeFor(AbstractFurnaceBlockEntity furnace) {
        BlockEntityType<?> type = furnace.getType();
        if (type == BlockEntityType.SMOKER) {
            return RecipeType.SMOKING;
        } else if (type == BlockEntityType.BLAST_FURNACE) {
            return RecipeType.BLASTING;
        }
        return RecipeType.SMELTING;
    }
}
