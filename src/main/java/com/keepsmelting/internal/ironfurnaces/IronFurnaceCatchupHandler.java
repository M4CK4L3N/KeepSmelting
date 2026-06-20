package com.keepsmelting.internal.ironfurnaces;

import com.keepsmelting.internal.catchup.AbstractCatchupHandler;
import com.keepsmelting.internal.ironfurnaces.data.SimulationData.FactorySmeltParams;
import com.keepsmelting.internal.ironfurnaces.data.SimulationData.NetworkResources;
import com.keepsmelting.internal.ironfurnaces.data.SimulationData.SimulationResult;
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

    private void applyFactoryWithNeighbors(BlockIronFurnaceTileBase factoryTile,
                                           long elapsed, Level level, BlockPos pos) {
        FurnaceNetwork network = new FurnaceNetwork();
        network.discover(factoryTile, level);

        if (!network.hasGenerators() && !network.hasFactories()) {
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

        NetworkResources nr =
                CatchupSimulation.aggregateNetwork(network, level);

        SimulationResult result =
                CatchupSimulation.simulateNetwork(nr, elapsed);

        CatchupSimulation.distributeToNetwork(nr, result, level);
    }
}
