package com.keepsmelting.internal.ironfurnaces;

import com.keepsmelting.internal.catchup.AbstractCatchupHandler;
import com.keepsmelting.internal.ironfurnaces.SimulationData.FactorySmeltParams;
import com.keepsmelting.internal.ironfurnaces.SimulationData.NetworkResources;
import com.keepsmelting.internal.ironfurnaces.SimulationData.SimulationResult;
import ironfurnaces.tileentity.furnaces.BlockIronFurnaceTileBase;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

public class IronFurnaceCatchupHandler extends AbstractCatchupHandler {

    public static final IronFurnaceCatchupHandler INSTANCE = new IronFurnaceCatchupHandler();

    @Override
    public void applyCatchup(BlockEntity tile, long elapsed, Level level, BlockPos pos) {
        BlockIronFurnaceTileBase ift = (BlockIronFurnaceTileBase) tile;
        if (ift.isFurnace()) {
            FurnaceMode.apply(ift, elapsed, level, pos);
        } else if (ift.isFactory()) {
            applyFactoryWithNeighbors(ift, elapsed, level, pos);
        } else if (ift.isGenerator()) {
            applyGenerator(ift, elapsed, level, pos);
        }
    }

    /** Generator в соло — симуляция. */
    private void applyGenerator(BlockIronFurnaceTileBase genTile,
                                long elapsed, Level level, BlockPos pos) {
        int totalFuel = CatchupSimulation.countGeneratorFuel(genTile, level);
        long burnTicksPerFuel = CatchupSimulation.getBurnTicksPerFuel(genTile);
        int rfPerTick = CatchupSimulation.getGeneratorRfPerTick(genTile);

        SimulationResult result = CatchupSimulation.simulateGeneratorOnly(
                totalFuel, burnTicksPerFuel, rfPerTick,
                genTile.getCapacity(), genTile.getEnergy(), elapsed);

        CatchupSimulation.applyGeneratorOnly(genTile, level, result);
    }

    /** Factory + вся сеть генераторов/заводов — сетевая симуляция. */
    private void applyFactoryWithNeighbors(BlockIronFurnaceTileBase factoryTile,
                                           long elapsed, Level level, BlockPos pos) {
        // Discovery: найти все связанные печи
        FurnaceNetwork network = new FurnaceNetwork();
        network.discover(factoryTile, level);

        if (!network.hasGenerators() && !network.hasFactories()) {
            // Нет сети — просто завод
            int totalSmeltable = CatchupSimulation.countFactoryInputs(factoryTile, level);
            int outputSpace = CatchupSimulation.countFactoryOutputSpace(factoryTile, level);
            FactorySmeltParams params =
                    CatchupSimulation.computeFactoryParams(factoryTile);

            SimulationResult result = CatchupSimulation.simulateFactoryOnly(
                    totalSmeltable, outputSpace,
                    params.maxRfPerItem, params.totalRfPerTick, params.maxCookTime,
                    factoryTile.getEnergy(), elapsed);

            CatchupSimulation.applyFactoryOnly(factoryTile, level, result);
            return;
        }

        // Aggregate: суммировать ресурсы всей сети
        NetworkResources nr =
                CatchupSimulation.aggregateNetwork(network, level);

        // Simulate: 1 проход
        SimulationResult result =
                CatchupSimulation.simulateNetwork(nr, elapsed);

        // Distribute: применить ко всем печам в сети
        CatchupSimulation.distributeToNetwork(nr, result, level);
    }
}
