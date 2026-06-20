package com.keepsmelting.internal.ironfurnaces.data;

import com.keepsmelting.internal.ironfurnaces.collect.FurnaceNetwork;

/**
 * DTO классов для симуляции печей Iron Furnaces.
 */
public class SimulationData {

    private SimulationData() {}

    /** Агрегированные ресурсы сети. */
    public static class NetworkResources {
        public FurnaceNetwork network;

        public int totalFuel;
        public int totalRfPerTick;
        public int totalGenCapacity;
        public int totalGenCurrentRf;
        public int totalAvgBurnTicksPerFuel = 1200;

        public int totalSmeltableItems;
        public int totalOutputSpace;
        public int totalRfPerTickConsumption;
        public int maxRfPerItem = 1;
        public int maxCookTime = 1;
        public int totalFactoryCapacity;
        public int totalFactoryCurrentRf;

        public int getTotalCapacity() { return totalGenCapacity + totalFactoryCapacity; }
        public int getTotalCurrentRf() { return totalGenCurrentRf + totalFactoryCurrentRf; }
    }

    /** Результат симуляции. */
    public static class SimulationResult {
        public int fuelToBurn;
        public int itemsToSmelt;
        public int rfForFactory;
        public int rfForGenerators;
        public int rfForFactoryStorage;
        public long effectiveTicks;
    }

    /** Параметры плавки завода. */
    public static class FactorySmeltParams {
        public int maxRfPerItem;
        public int totalRfPerTick;
        public int maxCookTime;
    }
}
