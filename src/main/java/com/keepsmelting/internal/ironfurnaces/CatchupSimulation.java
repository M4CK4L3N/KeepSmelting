package com.keepsmelting.internal.ironfurnaces;

import com.keepsmelting.internal.ironfurnaces.SimulationData.FactorySmeltParams;
import com.keepsmelting.internal.ironfurnaces.SimulationData.NetworkResources;
import com.keepsmelting.internal.ironfurnaces.SimulationData.SimulationResult;
import com.keepsmelting.mixin.ironfurnaces.IronFurnaceAccessor;
import ironfurnaces.items.augments.ItemAugmentFuel;
import ironfurnaces.items.augments.ItemAugmentSpeed;
import ironfurnaces.tileentity.furnaces.BlockIronFurnaceTileBase;
import net.minecraft.core.BlockPos;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import com.keepsmelting.internal.catchup.AbstractCatchupHandler;
import java.util.Optional;

/**
 * Симуляция Factory + Generator без потерь RF.
 * Phase 1: подсчёт ресурсов (без сайд-эффектов).
 * Phase 2: математика узкого места.
 * Phase 3: применение (с сайд-эффектами).
 */
public class CatchupSimulation {

    /**
     * Возвращает массив входных слотов завода в зависимости от tier'а.
     * Tier 0 (Iron): слоты 9, 10 (2 слота)
     * Tier 1 (Gold): слоты 8, 9, 10, 11 (4 слота)
     * Остальные: слоты 7, 8, 9, 10, 11, 12 (6 слотов)
     */
    public static int[] getFactoryInputSlots(BlockIronFurnaceTileBase factoryTile) {
        int tier = factoryTile.getTier();
        if (tier == 0) return new int[]{9, 10};
        if (tier == 1) return new int[]{8, 9, 10, 11};
        return new int[]{7, 8, 9, 10, 11, 12};
    }

    // ========== PHASE 1: COUNT ==========

    /** Сколько RF/тик даёт генератор. */
    public static int getGeneratorRfPerTick(BlockIronFurnaceTileBase genTile) {
        return Math.max(1, genTile.getGeneration());
    }

    /** Сколько тиков одна единица топлива даёт в генераторе. */
    public static long getBurnTicksPerFuel(BlockIronFurnaceTileBase genTile) {
        return FurnaceFuelHandler.getBurnTicksPerFuel(genTile);
    }

    /** Подсчитывает всё топливо генератора (слот + соседние контейнеры по всем сторонам). */
    public static int countGeneratorFuel(BlockIronFurnaceTileBase genTile, Level level) {
        return FurnaceFuelHandler.countGeneratorFuel(genTile, level);
    }

    /** Подсчитывает ингредиенты завода (правильные слоты под tier + контейнер сверху). */
    public static int countFactoryInputs(BlockIronFurnaceTileBase factoryTile, Level level) {
        int[] inputSlots = getFactoryInputSlots(factoryTile);
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
        int maxRfPerItem = 0;
        int totalRfPerTick = 0;
        int maxCookTime = 0;

        ItemStack greenAug = factoryTile.inventory.get(4);
        boolean hasSpeed = !greenAug.isEmpty() && greenAug.getItem() instanceof ItemAugmentSpeed;
        boolean hasFuel = !greenAug.isEmpty() && greenAug.getItem() instanceof ItemAugmentFuel;

        IronFurnaceAccessor acc = (IronFurnaceAccessor) factoryTile;

        for (int slot : new int[]{7, 8, 9, 10, 11, 12}) {
            ItemStack input = factoryTile.inventory.get(slot);
            if (input.isEmpty()) continue;

            // Получаем рецепт — нужно для base recipe cooking time
            Optional<? extends net.minecraft.world.item.crafting.AbstractCookingRecipe> recipeOpt =
                    acc.invokeGetRecipeFactory(slot, input);
            if (recipeOpt.isEmpty()) continue;

            // Оригинальный Iron Furnaces tick() считает:
            //   rfPerItem = recipe.getCookingTime() * 20
            //   if (hasSpeed) rfPerItem *= 2;
            //   if (hasFuel) rfPerItem /= 2;
            int baseRecipeTime = recipeOpt.get().getCookingTime();
            int rfPerItem = baseRecipeTime * 20;
            if (hasSpeed) rfPerItem *= 2;
            if (hasFuel) rfPerItem /= 2;

            int cookTime = acc.invokeGetFactoryCookTime(slot);
            if (cookTime <= 0) continue;

            maxRfPerItem = Math.max(maxRfPerItem, rfPerItem);
            // RF per tick = rfPerItem / cookTime (как в оригинальном tick(): energy / factoryTotalCookTime)
            totalRfPerTick += Math.max(1, rfPerItem / Math.max(1, cookTime));
            maxCookTime = Math.max(maxCookTime, cookTime);
        }

        FactorySmeltParams p = new FactorySmeltParams();
        p.maxRfPerItem = Math.max(1, maxRfPerItem);
        p.totalRfPerTick = Math.max(1, totalRfPerTick);
        p.maxCookTime = Math.max(1, maxCookTime);
        return p;
    }

    /** Агрегирует ресурсы из сети печей. */
    public static NetworkResources aggregateNetwork(FurnaceNetwork network, Level level) {
        NetworkResources nr = new NetworkResources();

        // Генераторы
        long totalBurnSum = 0;
        long totalGenRfPerTick = 0;
        for (BlockIronFurnaceTileBase gen : network.generators) {
            nr.totalFuel += countGeneratorFuel(gen, level);
            nr.totalGenCapacity += gen.getCapacity();
            nr.totalGenCurrentRf += gen.getEnergy();
            int rfPerTick = getGeneratorRfPerTick(gen);
            nr.totalRfPerTick += rfPerTick;
            long burnTicks = getBurnTicksPerFuel(gen);
            totalBurnSum += burnTicks * rfPerTick; // взвешенное по RF/tick
            totalGenRfPerTick += rfPerTick;
        }
        // Средневзвешенное кол-во burn ticks на единицу топлива
        nr.totalAvgBurnTicksPerFuel = totalGenRfPerTick > 0
                ? (int) (totalBurnSum / totalGenRfPerTick)
                : 1200;

        // Заводы
        for (BlockIronFurnaceTileBase factory : network.factories) {
            nr.totalSmeltableItems += countFactoryInputs(factory, level);
            nr.totalOutputSpace += countFactoryOutputSpace(factory, level);
            nr.totalFactoryCapacity += factory.getCapacity();
            nr.totalFactoryCurrentRf += factory.getEnergy();

            FactorySmeltParams params = computeFactoryParams(factory);
            nr.totalRfPerTickConsumption += params.totalRfPerTick;
            nr.maxRfPerItem = Math.max(nr.maxRfPerItem, params.maxRfPerItem);
            nr.maxCookTime = Math.max(nr.maxCookTime, params.maxCookTime);
        }

        // Запоминаем сеть для распределения
        nr.network = network;
        return nr;
    }

    /** Симулирует всю сеть за 1 проход. */
    public static SimulationResult simulateNetwork(NetworkResources nr, long elapsedTicks) {
        SimulationResult r = new SimulationResult();

        com.keepsmelting.KeepSmelting.LOGGER.info(
                "[Simulate] === Network {} gen={} fact={} elapsed={} ===",
                nr.network.size(), nr.network.generators.size(), nr.network.factories.size(), elapsedTicks);
        com.keepsmelting.KeepSmelting.LOGGER.info(
                "[Simulate] Gen: fuel={} rf/tick={} cap={} curRF={}",
                nr.totalFuel, nr.totalRfPerTick, nr.totalGenCapacity, nr.totalGenCurrentRf);
        com.keepsmelting.KeepSmelting.LOGGER.info(
                "[Simulate] Factory: items={} outSpace={} rf/item={} rf/tick={} cookTime={} cap={} curRF={}",
                nr.totalSmeltableItems, nr.totalOutputSpace, nr.maxRfPerItem,
                nr.totalRfPerTickConsumption, nr.maxCookTime,
                nr.totalFactoryCapacity, nr.totalFactoryCurrentRf);

        // Сколько RF можем сгенерировать из топлива
        long maxRfFromFuel = 0;
        if (nr.totalRfPerTick > 0 && nr.totalFuel > 0) {
            long maxBurnTicks = Math.min((long) nr.totalFuel * nr.totalAvgBurnTicksPerFuel, elapsedTicks);
            maxRfFromFuel = maxBurnTicks * nr.totalRfPerTick;
        }
        com.keepsmelting.KeepSmelting.LOGGER.info("[Simulate] maxRfFromFuel={}", maxRfFromFuel);

        // Сколько RF можем потребить (ингредиенты + место на выходе)
        int actualSmeltable = Math.min(nr.totalSmeltableItems, nr.totalOutputSpace);
        long maxRfToConsume = 0;
        if (nr.totalRfPerTickConsumption > 0 && actualSmeltable > 0) {
            long ticksToSmeltAll = (long) actualSmeltable * nr.maxCookTime;
            maxRfToConsume = Math.min(ticksToSmeltAll, elapsedTicks) * nr.totalRfPerTickConsumption;
        }
        com.keepsmelting.KeepSmelting.LOGGER.info("[Simulate] actualSmeltable={} maxRfToConsume={}", actualSmeltable, maxRfToConsume);

        // Сколько RF можем сохранить
        int storageAvailable = Math.max(0, nr.getTotalCapacity() - nr.getTotalCurrentRf());
        com.keepsmelting.KeepSmelting.LOGGER.info("[Simulate] storageAvailable={} (cap={} cur={})",
                storageAvailable, nr.getTotalCapacity(), nr.getTotalCurrentRf());

        // RF из генераторов (топливо + накопленное) — то, что можем распределить
        long rfFromGenPool = maxRfFromFuel + nr.totalGenCurrentRf;
        // RF уже внутри заводов — не требует распределения, но доступно для плавки
        long rfFactoryAlreadyThere = nr.totalFactoryCurrentRf;
        // Общий бюджет RF на плавку
        long totalRfForSmelting = rfFromGenPool + rfFactoryAlreadyThere;
        long effectiveRf = Math.min(totalRfForSmelting, maxRfToConsume);
        com.keepsmelting.KeepSmelting.LOGGER.info("[Simulate] effectiveRf={}", effectiveRf);
        if (effectiveRf <= 0 && maxRfFromFuel <= 0 && maxRfToConsume <= 0) return r;

        // Сколько топлива сжечь (реальное, не всё)
        if (maxRfFromFuel > 0 && nr.totalFuel > 0 && effectiveRf > 0) {
            long neededBurnTicks = effectiveRf / Math.max(1, nr.totalRfPerTick);
            r.fuelToBurn = (int) Math.ceil((double) neededBurnTicks / nr.totalAvgBurnTicksPerFuel);
            // Не больше чем есть
            r.fuelToBurn = Math.min(r.fuelToBurn, nr.totalFuel);
        }
        com.keepsmelting.KeepSmelting.LOGGER.info("[Simulate] fuelToBurn={}", r.fuelToBurn);

        // Сколько предметов расплавить
        if (maxRfToConsume > 0 && nr.maxRfPerItem > 0) {
            r.itemsToSmelt = (int) Math.min(actualSmeltable, effectiveRf / nr.maxRfPerItem);
            r.rfForFactory = r.itemsToSmelt * nr.maxRfPerItem;
        }
        com.keepsmelting.KeepSmelting.LOGGER.info("[Simulate] itemsToSmelt={} rfForFactory={}", r.itemsToSmelt, r.rfForFactory);

        // Остаток RF — распределить
        long remainingRf = effectiveRf - r.rfForFactory;
        if (remainingRf > 0) {
            int genStorageSpace = Math.max(0, nr.totalGenCapacity - nr.totalGenCurrentRf);
            if (genStorageSpace > 0) {
                r.rfForGenerators = (int) Math.min(genStorageSpace, remainingRf);
                remainingRf -= r.rfForGenerators;
            }
            int factoryStorageSpace = Math.max(0, nr.totalFactoryCapacity - nr.totalFactoryCurrentRf);
            if (factoryStorageSpace > 0) {
                r.rfForFactoryStorage = (int) Math.min(factoryStorageSpace, remainingRf);
            }
        }

        com.keepsmelting.KeepSmelting.LOGGER.info("[Simulate] remainingRf={} genSpace={} factSpace={}",
                effectiveRf - r.rfForFactory,
                Math.max(0, nr.totalGenCapacity - nr.totalGenCurrentRf),
                Math.max(0, nr.totalFactoryCapacity - nr.totalFactoryCurrentRf));

        r.effectiveTicks = elapsedTicks;
        return r;
    }

    /** Распределяет результат симуляции по сети. */
    public static void distributeToNetwork(NetworkResources nr, SimulationResult r, Level level) {
        com.keepsmelting.KeepSmelting.LOGGER.info(
                "[Distribute] fuel={} items={} rfGen={} rfFact={} rfStore={}",
                r.fuelToBurn, r.itemsToSmelt, r.rfForGenerators, r.rfForFactory, r.rfForFactoryStorage);

        if (r.fuelToBurn <= 0 && r.itemsToSmelt <= 0
                && r.rfForGenerators <= 0 && r.rfForFactoryStorage <= 0) return;

        // === 1. Сжечь топливо в генераторах (с распределением остатка) ===
        if (r.fuelToBurn > 0 && !nr.network.generators.isEmpty()) {
            int totalGen = nr.network.generators.size();
            int baseShare = r.fuelToBurn / totalGen;
            int remainder = r.fuelToBurn - baseShare * totalGen;
            for (int i = 0; i < totalGen; i++) {
                int share = baseShare + (i < remainder ? 1 : 0);
                if (share > 0) {
                    FurnaceFuelHandler.burnFuelIn(nr.network.generators.get(i), share, level);
                }
            }
        }

        // === 2. Дать RF заводам (бюджет на плавку) ===
        if (r.rfForFactory > 0 && !nr.network.factories.isEmpty()) {
            int totalFact = nr.network.factories.size();
            int baseRf = r.rfForFactory / totalFact;
            int rfRemainder = r.rfForFactory - baseRf * totalFact;
            for (int i = 0; i < totalFact; i++) {
                int rfShare = baseRf + (i < rfRemainder ? 1 : 0);
                nr.network.factories.get(i).setEnergy(nr.network.factories.get(i).getEnergy() + rfShare);
                nr.network.factories.get(i).setChanged();
            }
        }

        // === 3. Расплавить предметы (с распределением остатка) ===
        if (r.itemsToSmelt > 0 && !nr.network.factories.isEmpty()) {
            int totalFact = nr.network.factories.size();
            int baseItems = r.itemsToSmelt / totalFact;
            int itemsRemainder = r.itemsToSmelt - baseItems * totalFact;
            for (int i = 0; i < totalFact; i++) {
                int share = baseItems + (i < itemsRemainder ? 1 : 0);
                if (share > 0) {
                    applyFactorySmelt(nr.network.factories.get(i), level, share, 0);
                }
            }
        }

        // === 4. Распределить RF генераторам (с распределением остатка) ===
        if (r.rfForGenerators > 0 && !nr.network.generators.isEmpty()) {
            int totalGen = nr.network.generators.size();
            int baseRf = r.rfForGenerators / totalGen;
            int rfRemainder = r.rfForGenerators - baseRf * totalGen;
            for (int i = 0; i < totalGen; i++) {
                int share = baseRf + (i < rfRemainder ? 1 : 0);
                BlockIronFurnaceTileBase gen = nr.network.generators.get(i);
                int space = gen.getCapacity() - gen.getEnergy();
                if (space > 0 && share > 0) {
                    int toAdd = Math.min(space, share);
                    gen.setEnergy(gen.getEnergy() + toAdd);
                    gen.setChanged();
                }
            }
        }

        // === 5. Распределить оставшийся RF заводам (с распределением остатка) ===
        if (r.rfForFactoryStorage > 0 && !nr.network.factories.isEmpty()) {
            int totalFact = nr.network.factories.size();
            int baseRf = r.rfForFactoryStorage / totalFact;
            int rfRemainder = r.rfForFactoryStorage - baseRf * totalFact;
            for (int i = 0; i < totalFact; i++) {
                int share = baseRf + (i < rfRemainder ? 1 : 0);
                BlockIronFurnaceTileBase factory = nr.network.factories.get(i);
                int space = factory.getCapacity() - factory.getEnergy();
                if (space > 0 && share > 0) {
                    factory.setEnergy(factory.getEnergy() + Math.min(space, share));
                    factory.setChanged();
                }
            }
        }

        // Пометить обработанные печи
        for (BlockIronFurnaceTileBase gen : nr.network.generators) {
            CatchupDedup.mark(gen.getBlockPos());
        }
        for (BlockIronFurnaceTileBase fact : nr.network.factories) {
            CatchupDedup.mark(fact.getBlockPos());
        }
        // Пометить триггер (factoryTile, который инициировал симуляцию)
        if (!nr.network.factories.isEmpty()) {
            CatchupDedup.mark(nr.network.factories.get(0).getBlockPos());
        }
    }

    // ========== PHASE 2: SIMULATE ==========

    /** Симуляция Generator в соло (без завода рядом). */
    public static SimulationResult simulateGeneratorOnly(
            int totalFuelItems, long burnTicksPerFuel, int rfPerTick,
            int capacity, int currentRf,
            long elapsedTicks) {
        SimulationResult r = new SimulationResult();
        // Сколько RF можем сгенерировать за elapsed (ограничено топливом)
        long maxBurnTicks = Math.min((long) totalFuelItems * burnTicksPerFuel, elapsedTicks);
        long maxRfFromFuel = maxBurnTicks * rfPerTick;
        // Сколько RF можем сохранить (capacity)
        int storageAvailable = Math.max(0, capacity - currentRf);
        // Узкое место — min(генерация, хранение)
        long effectiveRf = Math.min(maxRfFromFuel, storageAvailable);
        if (effectiveRf <= 0) return r;
        long effectiveBurnTicks = effectiveRf / Math.max(1, rfPerTick);
        r.effectiveTicks = Math.min(elapsedTicks, effectiveBurnTicks);
        r.fuelToBurn = (int) Math.ceil((double) r.effectiveTicks / burnTicksPerFuel);
        r.rfForGenerators = (int) Math.min(storageAvailable, effectiveRf);
        return r;
    }

    /** Симуляция Factory без генератора. */
    public static SimulationResult simulateFactoryOnly(
            int totalSmeltableItems, int outputSpace, int maxRfPerItem,
            int totalRfPerTickConsumption, int maxCookTime,
            int factoryCurrentRf, long elapsedTicks) {
        SimulationResult r = new SimulationResult();
        int actualSmeltable = Math.min(totalSmeltableItems, outputSpace);
        if (actualSmeltable <= 0) return r;
        long ticksToSmeltAll = (long) actualSmeltable * maxCookTime;
        long maxTicksThatFit = Math.min(ticksToSmeltAll, elapsedTicks);
        // RF потребление ограничено текущим RF + временем
        long rfNeededMax = maxTicksThatFit * totalRfPerTickConsumption;
        long rfAvailable = factoryCurrentRf; // только то что есть, генератора нет
        long effectiveRf = Math.min(rfNeededMax, rfAvailable);
        if (effectiveRf <= 0) return r;
        int itemsPossible = (int) (effectiveRf / Math.max(1, maxRfPerItem));
        r.itemsToSmelt = Math.min(actualSmeltable, itemsPossible);
        r.rfForFactory = r.itemsToSmelt * maxRfPerItem;
        r.effectiveTicks = r.itemsToSmelt * maxCookTime;
        return r;
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

        // 1. Сколько RF есть всего: накопленное + можно сгенерировать
        long maxBurnTicks = Math.min((long) totalFuelItems * burnTicksPerFuel, elapsedTicks);
        long maxRfFromFuel = maxBurnTicks * rfPerTick;

        // RF из топлива + накопленное в генераторах — распределяемый пул
        long genPool = maxRfFromFuel + generatorCurrentRf;
        // RF внутри заводов — уже там, не требует распределения
        long totalRfAvailable = genPool + factoryCurrentRf;

        // 2. Сколько RF можем потребить (ингредиенты + место на выходе)
        int actualSmeltable = Math.min(totalSmeltableItems, outputSpace);
        long ticksToSmeltAll = (long) actualSmeltable * maxCookTime;
        long maxRfToConsume = Math.min(ticksToSmeltAll, elapsedTicks) * totalRfPerTickConsumption;

        // 3. Сколько RF можем сохранить (свободное место)
        int totalCapacity = generatorCapacity + factoryCapacity;
        int totalCurrentRf = generatorCurrentRf + factoryCurrentRf;
        int storageAvailable = Math.max(0, totalCapacity - totalCurrentRf);

        // 4. Узкое место: сколько RF можем потребить за elapsed время
        long effectiveRf = Math.min(totalRfAvailable, maxRfToConsume);
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

    /** Применяет результат симуляции Generator в соло. */
    public static void applyGeneratorOnly(
            BlockIronFurnaceTileBase genTile, Level level,
            SimulationResult simResult) {
        if (simResult.fuelToBurn <= 0 && simResult.rfForGenerators <= 0) return;
        // Сжечь топливо
        if (simResult.fuelToBurn > 0) {
            FurnaceFuelHandler.burnFuelIn(genTile, simResult.fuelToBurn, level);
        }
        // Залить RF
        if (simResult.rfForGenerators > 0) {
            int space = genTile.getCapacity() - genTile.getEnergy();
            if (space > 0) {
                genTile.setEnergy(genTile.getEnergy() + Math.min(space, simResult.rfForGenerators));
                genTile.setChanged();
            }
        }
        AbstractCatchupHandler.sendChatDebug(level, genTile.getBlockPos(),
                "Generator", simResult.effectiveTicks,
                simResult.fuelToBurn, simResult.rfForGenerators,
                0, 0, genTile.getEnergy() > 0);
    }

    /** Применяет результат симуляции Factory в соло. */
    public static void applyFactoryOnly(
            BlockIronFurnaceTileBase factoryTile, Level level,
            SimulationResult simResult) {
        if (simResult.itemsToSmelt <= 0 && simResult.rfForFactory <= 0) return;
        applyFactorySmelt(factoryTile, level, simResult.itemsToSmelt, simResult.rfForFactory);
    }

    /** Применяет результат симуляции Factory + Generator (1-to-1 связка). */
    public static void applyResult(
            BlockIronFurnaceTileBase genTile, BlockIronFurnaceTileBase factoryTile,
            Level level, BlockPos pos, SimulationResult simResult) {

        if (simResult.fuelToBurn <= 0 && simResult.itemsToSmelt <= 0
                && simResult.rfForGenerators <= 0 && simResult.rfForFactoryStorage <= 0) return;

        // === Сжечь топливо ===
        if (simResult.fuelToBurn > 0 && genTile != null) {
            FurnaceFuelHandler.burnFuelIn(genTile, simResult.fuelToBurn, level);
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
        // Количество входных слотов зависит от tier'а печи
        int[] inputSlots = getFactoryInputSlots(factoryTile);
        int slotCount = inputSlots.length;

        // Pre-fill: заполняем пустые слоты из бочек
        acc.invokeAutoFactoryIO();

        int remaining = itemsToSmelt;
        int rfSpent = 0;
        int smeltedCount = 0;
        int consecutiveFails = 0;

        while (remaining > 0) {
            boolean anyWorked = false;
            for (int i = 0; i < slotCount; i++) {
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
                    pushFactoryOutputBelow(factoryTile, level);
                    out = factoryTile.inventory.get(outputSlot);
                    if (!out.isEmpty() && out.getCount() >= out.getMaxStackSize()) continue;
                }

                // Сбрасываем cookTime и usedRF перед каждым новым предметом
                // (имитируем завершение предыдущего рецепта)
                factoryTile.usedRF[i] = 0.0;
                factoryTile.factoryCookTime[i] = 0;

                // Получаем рецепт
                Optional<? extends net.minecraft.world.item.crafting.AbstractCookingRecipe> recipeOpt =
                        acc.invokeGetRecipeFactory(slot, input);
                if (recipeOpt.isEmpty()) continue;

                int rfPerItem = recipeOpt.get().getCookingTime() * 20;
                ItemStack greenAug = factoryTile.inventory.get(4);
                boolean hasSpeed = !greenAug.isEmpty() && greenAug.getItem() instanceof ItemAugmentSpeed;
                boolean hasFuel = !greenAug.isEmpty() && greenAug.getItem() instanceof ItemAugmentFuel;
                if (hasSpeed) rfPerItem *= 2;
                if (hasFuel) rfPerItem /= 2;

                // Вместо invokeFactorySmelt — напрямую кладём результат
                // (избегаем проблем с внутренним состоянием Iron Furnaces: usedRF, cookTime)
                input.shrink(1);
                if (input.isEmpty()) {
                    factoryTile.inventory.set(slot, ItemStack.EMPTY);
                }

                // Получаем результат рецепта
                ItemStack result = recipeOpt.get().getResultItem(factoryTile.getLevel().registryAccess());
                if (!result.isEmpty()) {
                    ItemStack currentOut = factoryTile.inventory.get(outputSlot);
                    if (currentOut.isEmpty()) {
                        factoryTile.inventory.set(outputSlot, result.copy());
                    } else if (ItemStack.isSameItemSameTags(currentOut, result)
                            && currentOut.getCount() < currentOut.getMaxStackSize()) {
                        currentOut.grow(result.getCount());
                    }
                }

                int drain = Math.min(rfPerItem, factoryTile.getEnergy());
                factoryTile.setEnergy(factoryTile.getEnergy() - drain);
                rfSpent += drain;
                remaining--;
                smeltedCount++;
                anyWorked = true;
                consecutiveFails = 0;
                factoryTile.setChanged();

                if (remaining <= 0) break;
            }
            if (!anyWorked) {
                consecutiveFails++;
                if (consecutiveFails >= 3) break;
                // Пробуем ещё — может autoFactoryIO не успел
                acc.invokeAutoFactoryIO();
            }
        }

        // Дебаг (используем переданный rfBudget как показатель)
        AbstractCatchupHandler.sendChatDebug(level, factoryTile.getBlockPos(),
                "Factory", 0, 0,
                smeltedCount, 0, 0, true);
    }

    // ========== HOPPER HELPERS ==========

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
}
