package com.keepsmelting.internal.catchup;

import com.keepsmelting.internal.catchup.PipelineData.*;
import com.keepsmelting.mixin.IFurnaceAccessor;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.block.AbstractFurnaceBlock;
import net.minecraft.world.level.block.HopperBlock;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.HopperBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.ForgeHooks;

/**
 * Phase 3: Pipeline application — distributes items according to simulation result.
 */
public class PipelineApplicator {

    private PipelineApplicator() {}

    /**
     * Применяет результат симуляции к реальным BlockEntity:
     * вычитает топливо, вычитает вход, кладёт результат на выход.
     */
    @SuppressWarnings("deprecation")
    public static void apply(
            AbstractFurnaceBlockEntity furnace,
            IFurnaceAccessor acc,
            ServerLevel level,
            BlockPos pos,
            SimulationResult result,
            Recipe<?> recipe,
            int cookTotal,
            int litDuration,
            int cookingProgressBefore,
            long elapsed
    ) {
        var items = acc.getItems();

        // ====================================================================
        // 1. ПРЕДВАРИТЕЛЬНО: перемещаем предметы из источников в воронки
        // ====================================================================
        fillHoppersFromSources(level, pos);

        // ====================================================================
        // 2. ТОПЛИВО: печь → боковые контейнеры
        // ====================================================================
        int fuelRemaining = result.fuelConsumed();
        if (fuelRemaining > 0 && !items.get(1).isEmpty()) {
            int take = Math.min(fuelRemaining, items.get(1).getCount());
            items.get(1).shrink(take);
            fuelRemaining -= take;
        }
        if (fuelRemaining > 0) {
            consumeExternalFuel(level, pos, fuelRemaining);
        }

        // ====================================================================
        // 3. ВХОД: печь → контейнеры сверху
        // ====================================================================
        int inputRemaining = result.inputConsumed();
        if (inputRemaining > 0 && !items.get(0).isEmpty()) {
            int take = Math.min(inputRemaining, items.get(0).getCount());
            items.get(0).shrink(take);
            inputRemaining -= take;
        }
        if (inputRemaining > 0) {
            consumeExternalInput(furnace, level, pos, inputRemaining);
        }

        // ====================================================================
        // 4. ВЫХОД: печь → контейнеры снизу
        // ====================================================================
        if (result.itemsToCook() > 0) {
            ItemStack resultItem = recipe.getResultItem(level.registryAccess());
            if (resultItem.isEmpty()) {
                resultItem = new ItemStack(net.minecraft.world.item.Items.IRON_INGOT);
            }

            int maxStack = furnace.getMaxStackSize();
            int remaining = result.itemsToCook();

            // Сначала в слот 2 печи
            ItemStack output = items.get(2);
            if (output.isEmpty()) {
                int place = Math.min(remaining, maxStack);
                items.set(2, new ItemStack(resultItem.getItem(), place));
                remaining -= place;
            } else if (ItemStack.isSameItemSameTags(output, resultItem)) {
                int space = maxStack - output.getCount();
                if (space > 0) {
                    int place = Math.min(remaining, space);
                    output.grow(place);
                    remaining -= place;
                }
            }

            // Остаток — сразу в контейнер снизу
            if (remaining > 0) {
                pushOverflowToBelow(level, pos, resultItem, remaining);
            }
        }

        // ====================================================================
        // 5. ПРОГРЕСС
        // ====================================================================
        long actualTicks = Math.min(elapsed,
                (long) result.fuelConsumed() * litDuration + (acc.getLitTime() > 0 ? acc.getLitTime() : 0));

        if (result.itemsToCook() > 0) {
            long leftover = actualTicks - ((long) result.itemsToCook() * cookTotal - cookingProgressBefore);
            if (leftover < 0) leftover = 0;
            acc.setCookingProgress((int) Math.min(leftover, cookTotal - 1));
        } else if (actualTicks < cookTotal - cookingProgressBefore) {
            acc.setCookingProgress(cookingProgressBefore + (int) actualTicks);
        }

        // ====================================================================
        // 6. LIT STATUS
        // ====================================================================
        if (result.fuelConsumed() > 0) {
            acc.setLitTime(litDuration);
            BlockState bs = level.getBlockState(pos);
            if (bs.hasProperty(AbstractFurnaceBlock.LIT) && !bs.getValue(AbstractFurnaceBlock.LIT)) {
                level.setBlock(pos, bs.setValue(AbstractFurnaceBlock.LIT, true), 3);
            }
        } else if (result.itemsToCook() <= 0) {
            acc.setLitTime(0);
            BlockState bs = level.getBlockState(pos);
            if (bs.hasProperty(AbstractFurnaceBlock.LIT)) {
                level.setBlock(pos, bs.setValue(AbstractFurnaceBlock.LIT, false), 3);
            }
        }

        furnace.setChanged();
    }

    // ========================================================================
    // ПРЕДВАРИТЕЛЬНОЕ ЗАПОЛНЕНИЕ ВОРОНОК ИЗ ИСТОЧНИКОВ
    // ========================================================================

    static void fillHoppersFromSources(ServerLevel level, BlockPos pos) {
        BlockPos above = pos.above();
        if (level.isLoaded(above)) {
            BlockEntity be = level.getBlockEntity(above);
            if (be instanceof HopperBlockEntity hopper) {
                BlockPos aboveHopper = above.above();
                if (level.isLoaded(aboveHopper)) {
                    BlockEntity src = level.getBlockEntity(aboveHopper);
                    if (src instanceof Container source) {
                        transferToHopper(hopper, source);
                    }
                }
            }
        }

        for (Direction dir : Direction.Plane.HORIZONTAL) {
            BlockPos neighbor = pos.relative(dir);
            if (!level.isLoaded(neighbor)) continue;
            BlockEntity be = level.getBlockEntity(neighbor);
            if (be instanceof HopperBlockEntity hopper) {
                Direction facing = level.getBlockState(neighbor).getValue(HopperBlock.FACING);
                if (facing == dir.getOpposite()) {
                    BlockPos aboveHopper = neighbor.above();
                    if (level.isLoaded(aboveHopper)) {
                        BlockEntity src = level.getBlockEntity(aboveHopper);
                        if (src instanceof Container source) {
                            transferToHopper(hopper, source);
                        }
                    }
                }
            }
        }
    }

    static void transferToHopper(HopperBlockEntity hopper, Container source) {
        for (int i = 0; i < source.getContainerSize(); i++) {
            ItemStack srcStack = source.getItem(i);
            if (srcStack.isEmpty()) continue;

            for (int j = 0; j < hopper.getContainerSize(); j++) {
                ItemStack destStack = hopper.getItem(j);
                int maxSlot = Math.min(srcStack.getMaxStackSize(), hopper.getMaxStackSize());

                if (destStack.isEmpty()) {
                    int transfer = Math.min(srcStack.getCount(), maxSlot);
                    hopper.setItem(j, new ItemStack(srcStack.getItem(), transfer));
                    srcStack.shrink(transfer);
                    if (srcStack.isEmpty()) source.setItem(i, ItemStack.EMPTY);
                    source.setChanged();
                    hopper.setChanged();
                    break;
                } else if (ItemStack.isSameItemSameTags(destStack, srcStack) && destStack.getCount() < maxSlot) {
                    int space = maxSlot - destStack.getCount();
                    int transfer = Math.min(space, srcStack.getCount());
                    destStack.grow(transfer);
                    srcStack.shrink(transfer);
                    if (srcStack.isEmpty()) source.setItem(i, ItemStack.EMPTY);
                    source.setChanged();
                    hopper.setChanged();
                    break;
                }
            }
        }
    }

    // ---- ПОТРЕБЛЕНИЕ ----

    static void consumeExternalFuel(ServerLevel level, BlockPos pos, int amount) {
        if (amount <= 0) return;
        for (Direction dir : Direction.Plane.HORIZONTAL) {
            if (amount <= 0) return;
            BlockPos n = pos.relative(dir);
            if (!level.isLoaded(n)) continue;
            BlockEntity be = level.getBlockEntity(n);

            if (be instanceof HopperBlockEntity hopper) {
                BlockPos above = n.above();
                if (level.isLoaded(above)) {
                    BlockEntity src = level.getBlockEntity(above);
                    if (src instanceof Container c) {
                        amount = takeBurnable(c, amount);
                    }
                }
                amount = takeBurnable(hopper, amount);
                continue;
            }

            if (be instanceof Container c) {
                amount = takeBurnable(c, amount);
            }
        }
    }

    static void consumeExternalInput(AbstractFurnaceBlockEntity furnace, ServerLevel level, BlockPos pos, int amount) {
        if (amount <= 0) return;
        BlockPos above = pos.above();
        if (!level.isLoaded(above)) return;
        BlockEntity be = level.getBlockEntity(above);

        if (be instanceof HopperBlockEntity hopper) {
            BlockPos aboveHopper = above.above();
            if (level.isLoaded(aboveHopper)) {
                BlockEntity src = level.getBlockEntity(aboveHopper);
                if (src instanceof Container c) {
                    amount = takeSmeltable(furnace, c, amount);
                }
            }
            takeSmeltable(furnace, hopper, amount);
            return;
        }

        if (be instanceof Container c) {
            takeSmeltable(furnace, c, amount);
        }
    }

    // ---- ВЫТАЛКИВАНИЕ НА ВЫХОД ----

    static void pushOverflowToBelow(ServerLevel level, BlockPos pos, ItemStack template, int count) {
        if (count <= 0 || template.isEmpty()) return;
        BlockPos below = pos.below();
        if (!level.isLoaded(below)) return;
        BlockEntity be = level.getBlockEntity(below);
        if (!(be instanceof Container dest)) return;

        ItemStack stack = new ItemStack(template.getItem(), count);
        for (int i = 0; i < dest.getContainerSize(); i++) {
            if (stack.isEmpty()) break;
            ItemStack destStack = dest.getItem(i);
            int slotLimit = Math.min(stack.getMaxStackSize(), dest.getMaxStackSize());
            if (destStack.isEmpty()) {
                int transfer = Math.min(stack.getCount(), slotLimit);
                dest.setItem(i, new ItemStack(stack.getItem(), transfer));
                stack.shrink(transfer);
                dest.setChanged();
            } else if (ItemStack.isSameItemSameTags(destStack, stack) && destStack.getCount() < slotLimit) {
                int space = slotLimit - destStack.getCount();
                int transfer = Math.min(stack.getCount(), space);
                destStack.grow(transfer);
                stack.shrink(transfer);
                dest.setChanged();
            }
        }
    }

    private static int takeBurnable(Container container, int amount) {
        if (amount <= 0) return 0;
        for (int i = 0; i < container.getContainerSize(); i++) {
            if (amount <= 0) break;
            ItemStack s = container.getItem(i);
            if (s.isEmpty()) continue;
            if (ForgeHooks.getBurnTime(s, RecipeType.SMELTING) <= 0) continue;
            int take = Math.min(amount, s.getCount());
            s.shrink(take);
            amount -= take;
        }
        return amount;
    }

    private static int takeSmeltable(AbstractFurnaceBlockEntity furnace, Container container, int amount) {
        if (amount <= 0) return 0;
        for (int i = 0; i < container.getContainerSize(); i++) {
            if (amount <= 0) break;
            ItemStack s = container.getItem(i);
            if (s.isEmpty()) continue;
            if (!VanillaHopperIO.isSmeltable(furnace, s)) continue;
            int take = Math.min(amount, s.getCount());
            s.shrink(take);
            amount -= take;
        }
        return amount;
    }
}
