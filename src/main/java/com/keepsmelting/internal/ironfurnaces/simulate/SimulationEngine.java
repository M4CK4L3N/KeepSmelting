package com.keepsmelting.internal.ironfurnaces.simulate;

import com.keepsmelting.internal.ironfurnaces.data.SimulationData.NetworkResources;
import com.keepsmelting.internal.ironfurnaces.data.SimulationData.SimulationResult;

/**
 * Phase 2: чистая математика симуляции. Никаких сайд-эффектов.
 */
public class SimulationEngine {

    private SimulationEngine() {}

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

        long maxRfFromFuel = 0;
        if (nr.totalRfPerTick > 0 && nr.totalFuel > 0) {
            long maxBurnTicks = Math.min((long) nr.totalFuel * nr.totalAvgBurnTicksPerFuel, elapsedTicks);
            maxRfFromFuel = maxBurnTicks * nr.totalRfPerTick;
        }

        int actualSmeltable = Math.min(nr.totalSmeltableItems, nr.totalOutputSpace);
        long maxRfToConsume = 0;
        if (nr.totalRfPerTickConsumption > 0 && actualSmeltable > 0) {
            long ticksToSmeltAll = (long) actualSmeltable * nr.maxCookTime;
            maxRfToConsume = Math.min(ticksToSmeltAll, elapsedTicks) * nr.totalRfPerTickConsumption;
        }

        long rfFromGenPool = maxRfFromFuel + nr.totalGenCurrentRf;
        long rfFactoryAlreadyThere = nr.totalFactoryCurrentRf;
        long totalRfForSmelting = rfFromGenPool + rfFactoryAlreadyThere;
        long effectiveRf = Math.min(totalRfForSmelting, maxRfToConsume);

        if (effectiveRf <= 0 && maxRfFromFuel <= 0 && maxRfToConsume <= 0) return r;

        if (maxRfFromFuel > 0 && nr.totalFuel > 0 && effectiveRf > 0) {
            long neededBurnTicks = effectiveRf / Math.max(1, nr.totalRfPerTick);
            r.fuelToBurn = (int) Math.ceil((double) neededBurnTicks / nr.totalAvgBurnTicksPerFuel);
            r.fuelToBurn = Math.min(r.fuelToBurn, nr.totalFuel);
        }

        if (maxRfToConsume > 0 && nr.maxRfPerItem > 0) {
            r.itemsToSmelt = (int) Math.min(actualSmeltable, effectiveRf / nr.maxRfPerItem);
            r.rfForFactory = r.itemsToSmelt * nr.maxRfPerItem;
        }

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

        r.effectiveTicks = elapsedTicks;
        return r;
    }

    public static SimulationResult simulateGeneratorOnly(
            int totalFuelItems, long burnTicksPerFuel, int rfPerTick,
            int capacity, int currentRf, long elapsedTicks) {
        SimulationResult r = new SimulationResult();
        long maxBurnTicks = Math.min((long) totalFuelItems * burnTicksPerFuel, elapsedTicks);
        long maxRfFromFuel = maxBurnTicks * rfPerTick;
        int storageAvailable = Math.max(0, capacity - currentRf);
        long effectiveRf = Math.min(maxRfFromFuel, storageAvailable);
        if (effectiveRf <= 0) return r;
        long effectiveBurnTicks = effectiveRf / Math.max(1, rfPerTick);
        r.effectiveTicks = Math.min(elapsedTicks, effectiveBurnTicks);
        r.fuelToBurn = (int) Math.ceil((double) r.effectiveTicks / burnTicksPerFuel);
        r.rfForGenerators = (int) Math.min(storageAvailable, effectiveRf);
        return r;
    }

    public static SimulationResult simulateFactoryOnly(
            int totalSmeltableItems, int outputSpace, int maxRfPerItem,
            int totalRfPerTickConsumption, int maxCookTime,
            int factoryCurrentRf, long elapsedTicks) {
        SimulationResult r = new SimulationResult();
        int actualSmeltable = Math.min(totalSmeltableItems, outputSpace);
        if (actualSmeltable <= 0) return r;
        long ticksToSmeltAll = (long) actualSmeltable * maxCookTime;
        long maxTicksThatFit = Math.min(ticksToSmeltAll, elapsedTicks);
        long rfNeededMax = maxTicksThatFit * totalRfPerTickConsumption;
        long rfAvailable = factoryCurrentRf;
        long effectiveRf = Math.min(rfNeededMax, rfAvailable);
        if (effectiveRf <= 0) return r;
        int itemsPossible = (int) (effectiveRf / Math.max(1, maxRfPerItem));
        r.itemsToSmelt = Math.min(actualSmeltable, itemsPossible);
        r.rfForFactory = r.itemsToSmelt * maxRfPerItem;
        r.effectiveTicks = r.itemsToSmelt * maxCookTime;
        return r;
    }

    public static SimulationResult simulate(
            int totalFuelItems, long burnTicksPerFuel, int rfPerTick,
            int totalSmeltableItems, int outputSpace, int maxRfPerItem,
            int totalRfPerTickConsumption, int maxCookTime,
            int generatorCapacity, int generatorCurrentRf,
            int factoryCapacity, int factoryCurrentRf,
            long elapsedTicks) {

        SimulationResult r = new SimulationResult();

        long maxBurnTicks = Math.min((long) totalFuelItems * burnTicksPerFuel, elapsedTicks);
        long maxRfFromFuel = maxBurnTicks * rfPerTick;
        long genPool = maxRfFromFuel + generatorCurrentRf;
        long totalRfAvailable = genPool + factoryCurrentRf;

        int actualSmeltable = Math.min(totalSmeltableItems, outputSpace);
        long ticksToSmeltAll = (long) actualSmeltable * maxCookTime;
        long maxRfToConsume = Math.min(ticksToSmeltAll, elapsedTicks) * totalRfPerTickConsumption;

        long effectiveRf = Math.min(totalRfAvailable, maxRfToConsume);
        if (effectiveRf <= 0) return r;

        long effectiveBurnTicks = effectiveRf / Math.max(1, rfPerTick);
        r.effectiveTicks = Math.min(elapsedTicks, effectiveBurnTicks);
        r.fuelToBurn = (int) Math.ceil((double) r.effectiveTicks / burnTicksPerFuel);

        int itemsPossible = (int) (effectiveRf / Math.max(1, maxRfPerItem));
        r.itemsToSmelt = Math.min(actualSmeltable, itemsPossible);
        r.rfForFactory = r.itemsToSmelt * maxRfPerItem;

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
}
