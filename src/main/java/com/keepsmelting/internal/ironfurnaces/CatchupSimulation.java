package com.keepsmelting.internal.ironfurnaces;

import com.keepsmelting.internal.ironfurnaces.SimulationData.FactorySmeltParams;
import com.keepsmelting.internal.ironfurnaces.SimulationData.NetworkResources;
import com.keepsmelting.internal.ironfurnaces.SimulationData.SimulationResult;
import ironfurnaces.tileentity.furnaces.BlockIronFurnaceTileBase;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

/**
 * Фасад для симуляции Iron Furnaces.
 * Делегирует Phase 1 → NetworkDataCollector, Phase 2 → SimulationEngine, Phase 3 → SimulationApplicator.
 */
public class CatchupSimulation {

    private CatchupSimulation() {}

    // ========== PHASE 1: COUNT (делегировано NetworkDataCollector) ==========

    public static int[] getFactoryInputSlots(BlockIronFurnaceTileBase factoryTile) {
        return NetworkDataCollector.getFactoryInputSlots(factoryTile);
    }

    public static int getGeneratorRfPerTick(BlockIronFurnaceTileBase genTile) {
        return NetworkDataCollector.getGeneratorRfPerTick(genTile);
    }

    public static long getBurnTicksPerFuel(BlockIronFurnaceTileBase genTile) {
        return NetworkDataCollector.getBurnTicksPerFuel(genTile);
    }

    public static int countGeneratorFuel(BlockIronFurnaceTileBase genTile, Level level) {
        return NetworkDataCollector.countGeneratorFuel(genTile, level);
    }

    public static int countFactoryInputs(BlockIronFurnaceTileBase factoryTile, Level level) {
        return NetworkDataCollector.countFactoryInputs(factoryTile, level);
    }

    public static int countFactoryOutputSpace(BlockIronFurnaceTileBase factoryTile, Level level) {
        return NetworkDataCollector.countFactoryOutputSpace(factoryTile, level);
    }

    public static FactorySmeltParams computeFactoryParams(BlockIronFurnaceTileBase factoryTile) {
        return NetworkDataCollector.computeFactoryParams(factoryTile);
    }

    public static NetworkResources aggregateNetwork(FurnaceNetwork network, Level level) {
        return NetworkDataCollector.aggregateNetwork(network, level);
    }

    // ========== PHASE 2: SIMULATE (делегировано SimulationEngine) ==========

    public static SimulationResult simulateNetwork(NetworkResources nr, long elapsedTicks) {
        return SimulationEngine.simulateNetwork(nr, elapsedTicks);
    }

    public static SimulationResult simulateGeneratorOnly(
            int totalFuelItems, long burnTicksPerFuel, int rfPerTick,
            int capacity, int currentRf, long elapsedTicks) {
        return SimulationEngine.simulateGeneratorOnly(totalFuelItems, burnTicksPerFuel, rfPerTick,
                capacity, currentRf, elapsedTicks);
    }

    public static SimulationResult simulateFactoryOnly(
            int totalSmeltableItems, int outputSpace, int maxRfPerItem,
            int totalRfPerTickConsumption, int maxCookTime,
            int factoryCurrentRf, long elapsedTicks) {
        return SimulationEngine.simulateFactoryOnly(totalSmeltableItems, outputSpace, maxRfPerItem,
                totalRfPerTickConsumption, maxCookTime, factoryCurrentRf, elapsedTicks);
    }

    public static SimulationResult simulate(
            int totalFuelItems, long burnTicksPerFuel, int rfPerTick,
            int totalSmeltableItems, int outputSpace, int maxRfPerItem,
            int totalRfPerTickConsumption, int maxCookTime,
            int generatorCapacity, int generatorCurrentRf,
            int factoryCapacity, int factoryCurrentRf,
            long elapsedTicks) {
        return SimulationEngine.simulate(totalFuelItems, burnTicksPerFuel, rfPerTick,
                totalSmeltableItems, outputSpace, maxRfPerItem,
                totalRfPerTickConsumption, maxCookTime,
                generatorCapacity, generatorCurrentRf,
                factoryCapacity, factoryCurrentRf, elapsedTicks);
    }

    // ========== PHASE 3: APPLY (делегировано SimulationApplicator) ==========

    public static void distributeToNetwork(NetworkResources nr, SimulationResult r, Level level) {
        SimulationApplicator.distributeToNetwork(nr, r, level);
    }

    public static void applyGeneratorOnly(
            BlockIronFurnaceTileBase genTile, Level level, SimulationResult simResult) {
        SimulationApplicator.applyGeneratorOnly(genTile, level, simResult);
    }

    public static void applyFactoryOnly(
            BlockIronFurnaceTileBase factoryTile, Level level, SimulationResult simResult) {
        SimulationApplicator.applyFactoryOnly(factoryTile, level, simResult);
    }

    public static void applyResult(
            BlockIronFurnaceTileBase genTile, BlockIronFurnaceTileBase factoryTile,
            Level level, BlockPos pos, SimulationResult simResult) {
        SimulationApplicator.applyResult(genTile, factoryTile, level, pos, simResult);
    }
}
