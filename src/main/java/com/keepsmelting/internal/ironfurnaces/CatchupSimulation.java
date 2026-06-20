package com.keepsmelting.internal.ironfurnaces;

import com.keepsmelting.mixin.ironfurnaces.IronFurnaceAccessor;
import ironfurnaces.items.augments.ItemAugmentFuel;
import ironfurnaces.items.augments.ItemAugmentSpeed;
import ironfurnaces.tileentity.furnaces.BlockIronFurnaceTileBase;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.common.ForgeHooks;

import com.keepsmelting.internal.catchup.AbstractCatchupHandler;
import java.util.Optional;

/**
 * Симуляция Factory + Generator без потерь RF.
 * Phase 1: подсчёт ресурсов (без сайд-эффектов).
 * Phase 2: математика узкого места.
 * Phase 3: применение (с сайд-эффектами).
 */
public class CatchupSimulation {

    // ========== PHASE 1: COUNT ==========

    /** Сколько RF/тик даёт генератор. */
    public static int getGeneratorRfPerTick(BlockIronFurnaceTileBase genTile) {
        return Math.max(1, genTile.getGeneration());
    }

    /** Сколько тиков одна единица топлива даёт в генераторе. */
    public static long getBurnTicksPerFuel(BlockIronFurnaceTileBase genTile) {
        int gen = Math.max(1, genTile.getGeneration());
        return (long) Math.ceil(genTile.getGeneratorBurn() * 20.0 / gen);
    }

    /** Подсчитывает всё топливо генератора (слот + соседние контейнеры). */
    public static int countGeneratorFuel(BlockIronFurnaceTileBase genTile, Level level) {
        int total = genTile.inventory.get(6).getCount();
        for (Direction dir : Direction.Plane.HORIZONTAL) {
            BlockPos neighbor = genTile.getBlockPos().relative(dir);
            if (!level.isLoaded(neighbor)) continue;
            BlockEntity be = level.getBlockEntity(neighbor);
            if (!(be instanceof Container container)) continue;
            for (int i = 0; i < container.getContainerSize(); i++) {
                ItemStack stack = container.getItem(i);
                if (stack.isEmpty()) continue;
                if (ForgeHooks.getBurnTime(stack, null) > 0) {
                    total += stack.getCount();
                }
            }
        }
        return total;
    }

    /** Подсчитывает ингредиенты завода (6 слотов + контейнер сверху). */
    public static int countFactoryInputs(BlockIronFurnaceTileBase factoryTile, Level level) {
        int[] inputSlots = new int[]{7, 8, 9, 10, 11, 12};
        int total = 0;
        for (int slot : inputSlots) {
            total += factoryTile.inventory.get(slot).getCount();
        }
        BlockPos above = factoryTile.getBlockPos().above();
        if (level.isLoaded(above) && level.getBlockEntity(above) instanceof Container container) {
            for (int i = 0; i < container.getContainerSize(); i++) {
                total += container.getItem(i).getCount();
            }
        }
        return total;
    }

    /** Подсчитывает свободное место на выходе завода. */
    public static int countFactoryOutputSpace(BlockIronFurnaceTileBase factoryTile, Level level) {
        int[] outputSlots = new int[]{13, 14, 15, 16, 17, 18};
        int space = 0;
        for (int slot : outputSlots) {
            ItemStack stack = factoryTile.inventory.get(slot);
            space += stack.isEmpty() ? 64 : stack.getMaxStackSize() - stack.getCount();
        }
        BlockPos below = factoryTile.getBlockPos().below();
        if (level.isLoaded(below) && level.getBlockEntity(below) instanceof Container container) {
            for (int i = 0; i < container.getContainerSize(); i++) {
                ItemStack stack = container.getItem(i);
                space += stack.isEmpty() ? 64 : stack.getMaxStackSize() - stack.getCount();
            }
        }
        return space;
    }

    /** Параметры плавки завода. */
    public static FactorySmeltParams computeFactoryParams(BlockIronFurnaceTileBase factoryTile) {
        ItemStack greenAug = factoryTile.inventory.get(4);
        boolean hasSpeed = !greenAug.isEmpty() && greenAug.getItem() instanceof ItemAugmentSpeed;
        boolean hasFuel = !greenAug.isEmpty() && greenAug.getItem() instanceof ItemAugmentFuel;

        int maxRfPerItem = 0;
        int totalRfPerTick = 0;
        int maxCookTime = 0;

        IronFurnaceAccessor acc = (IronFurnaceAccessor) factoryTile;
        int[] inputSlots = new int[]{7, 8, 9, 10, 11, 12};

        for (int i = 0; i < 6; i++) {
            int slot = inputSlots[i];
            ItemStack input = factoryTile.inventory.get(slot);
            if (input.isEmpty()) continue;

            int cookTime = acc.invokeGetFactoryCookTime(slot);
            if (cookTime <= 0) continue;

            int rfPerItem = cookTime * 20;
            if (hasSpeed) rfPerItem *= 2;
            if (hasFuel) rfPerItem /= 2;

            maxRfPerItem = Math.max(maxRfPerItem, rfPerItem);
            totalRfPerTick += Math.max(1, rfPerItem / Math.max(1, cookTime));
            maxCookTime = Math.max(maxCookTime, cookTime);
        }

        FactorySmeltParams p = new FactorySmeltParams();
        p.maxRfPerItem = Math.max(1, maxRfPerItem);
        p.totalRfPerTick = Math.max(1, totalRfPerTick);
        p.maxCookTime = Math.max(1, maxCookTime);
        return p;
    }

    // ========== PHASE 2: SIMULATE ==========

    /** Результат симуляции. */
    public static class SimulationResult {
        public int fuelToBurn;
        public int itemsToSmelt;
        public int rfForFactory;
        public int rfForGenerators;
        public int rfForFactoryStorage;
        public long effectiveTicks;
    }

    /** Симулирует цепочку без сайд-эффектов. */
    public static SimulationResult simulate(
            int totalFuelItems, long burnTicksPerFuel, int rfPerTick,
            int totalSmeltableItems, int outputSpace, int maxRfPerItem,
            int totalRfPerTickConsumption, int maxCookTime,
            int generatorCapacity, int generatorCurrentRf,
            int factoryCapacity, int factoryCurrentRf,
            long elapsedTicks) {

        SimulationResult r = new SimulationResult();

        // 1. Сколько RF можем сгенерировать
        long maxBurnTicks = Math.min((long) totalFuelItems * burnTicksPerFuel, elapsedTicks);
        long maxRfFromFuel = maxBurnTicks * rfPerTick;

        // 2. Сколько RF можем потребить (ингредиенты + место на выходе)
        int actualSmeltable = Math.min(totalSmeltableItems, outputSpace);
        long ticksToSmeltAll = (long) actualSmeltable * maxCookTime;
        long maxRfToConsume = Math.min(ticksToSmeltAll, elapsedTicks) * totalRfPerTickConsumption;

        // 3. Сколько RF можем сохранить
        int totalCapacity = generatorCapacity + factoryCapacity;
        int totalCurrentRf = generatorCurrentRf + factoryCurrentRf;
        int storageAvailable = Math.max(0, totalCapacity - totalCurrentRf);

        // 4. Узкое место
        long effectiveRf = Math.min(maxRfFromFuel, maxRfToConsume + storageAvailable);
        if (effectiveRf <= 0) return r;

        // 5. Сколько тиков
        long effectiveBurnTicks = effectiveRf / Math.max(1, rfPerTick);
        r.effectiveTicks = Math.min(elapsedTicks, effectiveBurnTicks);

        // 6. Сколько топлива сжечь
        r.fuelToBurn = (int) Math.ceil((double) r.effectiveTicks / burnTicksPerFuel);

        // 7. Сколько предметов расплавить
        int itemsPossible = (int) (effectiveRf / Math.max(1, maxRfPerItem));
        r.itemsToSmelt = Math.min(actualSmeltable, itemsPossible);
        r.rfForFactory = r.itemsToSmelt * maxRfPerItem;

        // 8. Остаток RF распределить
        long remainingRf = effectiveRf - r.rfForFactory;
        int genSpace = Math.max(0, generatorCapacity - generatorCurrentRf);
        if (genSpace > 0 && remainingRf > 0) {
            r.rfForGenerators = (int) Math.min(genSpace, remainingRf);
            remainingRf -= r.rfForGenerators;
        }
        int factorySpace = Math.max(0, factoryCapacity - factoryCurrentRf);
        if (factorySpace > 0 && remainingRf > 0) {
            r.rfForFactoryStorage = (int) Math.min(factorySpace, remainingRf);
        }

        return r;
    }

    // ========== PHASE 3: APPLY ==========

    /** Применяет результат симуляции. */
    public static void applyResult(
            BlockIronFurnaceTileBase genTile, BlockIronFurnaceTileBase factoryTile,
            Level level, BlockPos pos, SimulationResult simResult) {

        if (simResult.fuelToBurn <= 0 && simResult.itemsToSmelt <= 0
                && simResult.rfForGenerators <= 0 && simResult.rfForFactoryStorage <= 0) return;

        // === Сжечь топливо ===
        if (simResult.fuelToBurn > 0 && genTile != null) {
            int toBurn = simResult.fuelToBurn;
            ItemStack fuelStack = genTile.inventory.get(6);
            while (toBurn > 0) {
                if (fuelStack.isEmpty()) {
                    pullFuelFromNeighbors(genTile, level);
                    fuelStack = genTile.inventory.get(6);
                    if (fuelStack.isEmpty()) break;
                }
                int take = Math.min(toBurn, fuelStack.getCount());
                fuelStack.shrink(take);
                toBurn -= take;
                if (fuelStack.isEmpty()) {
                    genTile.inventory.set(6, fuelStack.getCraftingRemainingItem());
                }
                genTile.setChanged();
            }
        }

        // === Расплавить предметы ===
        if (simResult.itemsToSmelt > 0 && factoryTile != null) {
            applyFactorySmelt(factoryTile, level, simResult.itemsToSmelt, simResult.rfForFactory);
        }

        // === Распределить RF ===
        if (simResult.rfForGenerators > 0 && genTile != null) {
            int space = genTile.getCapacity() - genTile.getEnergy();
            if (space > 0) {
                genTile.setEnergy(genTile.getEnergy() + Math.min(space, simResult.rfForGenerators));
                genTile.setChanged();
            }
        }
        if (simResult.rfForFactoryStorage > 0 && factoryTile != null) {
            int space = factoryTile.getCapacity() - factoryTile.getEnergy();
            if (space > 0) {
                factoryTile.setEnergy(factoryTile.getEnergy() + Math.min(space, simResult.rfForFactoryStorage));
                factoryTile.setChanged();
            }
        }
    }

    /** Плавит N предметов в Factory режиме. */
    private static void applyFactorySmelt(
            BlockIronFurnaceTileBase factoryTile, Level level,
            int itemsToSmelt, int rfBudget) {

        IronFurnaceAccessor acc = (IronFurnaceAccessor) factoryTile;
        int[] inputSlots = new int[]{7, 8, 9, 10, 11, 12};
        int remaining = itemsToSmelt;
        int rfSpent = 0;

        while (remaining > 0) {
            boolean anyWorked = false;
            for (int i = 0; i < 6; i++) {
                int slot = inputSlots[i];
                ItemStack input = factoryTile.inventory.get(slot);
                if (input.isEmpty()) {
                    acc.invokeAutoFactoryIO();
                    input = factoryTile.inventory.get(slot);
                    if (input.isEmpty()) continue;
                }

                int outputSlot = slot + 6;
                ItemStack out = factoryTile.inventory.get(outputSlot);
                if (!out.isEmpty() && out.getCount() >= out.getMaxStackSize()) {
                    // Пытаемся вытолкнуть вниз
                    pushFactoryOutputBelow(factoryTile, level);
                    out = factoryTile.inventory.get(outputSlot);
                    if (!out.isEmpty() && out.getCount() >= out.getMaxStackSize()) continue;
                }

                // Получаем рецепт
                Optional<? extends net.minecraft.world.item.crafting.AbstractCookingRecipe> recipeOpt =
                        acc.invokeGetRecipeFactory(slot, input);
                if (recipeOpt.isEmpty()) continue;
                if (!acc.invokeCanFactorySmelt(recipeOpt.get(), slot)) continue;

                int rfPerItem = recipeOpt.get().getCookingTime() * 20;
                ItemStack greenAug = factoryTile.inventory.get(4);
                boolean hasSpeed = !greenAug.isEmpty() && greenAug.getItem() instanceof ItemAugmentSpeed;
                boolean hasFuel = !greenAug.isEmpty() && greenAug.getItem() instanceof ItemAugmentFuel;
                if (hasSpeed) rfPerItem *= 2;
                if (hasFuel) rfPerItem /= 2;

                if (factoryTile.getEnergy() < rfPerItem) continue;

                // Плавим
                input.shrink(1);
                if (input.isEmpty()) {
                    factoryTile.inventory.set(slot, ItemStack.EMPTY);
                }
                acc.invokeFactorySmelt(recipeOpt.get(), slot);

                int drain = Math.min(rfPerItem, factoryTile.getEnergy());
                factoryTile.setEnergy(factoryTile.getEnergy() - drain);
                rfSpent += drain;
                remaining--;
                anyWorked = true;
                factoryTile.setChanged();

                if (remaining <= 0) break;
            }
            if (!anyWorked) break;
        }

        // Дебаг
        AbstractCatchupHandler.sendChatDebug(level, factoryTile.getBlockPos(),
                "Factory", itemsToSmelt, 0,
                itemsToSmelt - remaining, 0, 0, true);
    }

    // ========== HOPPER HELPERS ==========

    private static void pullFuelFromNeighbors(BlockIronFurnaceTileBase genTile, Level level) {
        BlockPos pos = genTile.getBlockPos();
        for (Direction dir : Direction.Plane.HORIZONTAL) {
            BlockPos neighbor = pos.relative(dir);
            if (!level.isLoaded(neighbor)) continue;
            if (!(level.getBlockEntity(neighbor) instanceof Container container)) continue;

            for (int i = 0; i < container.getContainerSize(); i++) {
                ItemStack stack = container.getItem(i);
                if (stack.isEmpty()) continue;
                if (ForgeHooks.getBurnTime(stack, null) <= 0) continue;

                ItemStack taken = container.removeItem(i, 1);
                if (taken.isEmpty()) continue;

                ItemStack current = genTile.inventory.get(6);
                if (current.isEmpty()) {
                    genTile.inventory.set(6, taken);
                } else if (ItemStack.isSameItemSameTags(current, taken)
                        && current.getCount() < current.getMaxStackSize()) {
                    current.grow(1);
                    genTile.inventory.set(6, current);
                } else {
                    container.setItem(i, taken);
                    continue;
                }
                container.setChanged();
                genTile.setChanged();
                return;
            }
        }
    }

    private static void pushFactoryOutputBelow(BlockIronFurnaceTileBase factoryTile, Level level) {
        BlockPos below = factoryTile.getBlockPos().below();
        if (!level.isLoaded(below)) return;
        if (!(level.getBlockEntity(below) instanceof Container container)) return;

        int[] outputSlots = new int[]{13, 14, 15, 16, 17, 18};
        for (int slot : outputSlots) {
            ItemStack out = factoryTile.inventory.get(slot);
            if (out.isEmpty()) continue;

            for (int i = 0; i < container.getContainerSize(); i++) {
                ItemStack dest = container.getItem(i);
                if (dest.isEmpty()) {
                    ItemStack toPush = out.copy();
                    int toPushCount = Math.min(toPush.getCount(), toPush.getMaxStackSize());
                    toPush.setCount(toPushCount);
                    out.shrink(toPushCount);
                    container.setItem(i, toPush);
                    if (out.isEmpty()) factoryTile.inventory.set(slot, ItemStack.EMPTY);
                    container.setChanged();
                    factoryTile.setChanged();
                    return;
                } else if (ItemStack.isSameItemSameTags(out, dest)
                        && dest.getCount() < dest.getMaxStackSize()) {
                    int space = dest.getMaxStackSize() - dest.getCount();
                    int toMove = Math.min(out.getCount(), space);
                    out.shrink(toMove);
                    dest.grow(toMove);
                    if (out.isEmpty()) factoryTile.inventory.set(slot, ItemStack.EMPTY);
                    container.setChanged();
                    factoryTile.setChanged();
                    return;
                }
            }
        }
    }

    // ========== DTO ==========

    public static class FactorySmeltParams {
        public int maxRfPerItem;
        public int totalRfPerTick;
        public int maxCookTime;
    }
}
