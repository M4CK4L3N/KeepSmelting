package com.keepsmelting.internal.ironfurnaces;

import com.keepsmelting.internal.catchup.AbstractCatchupHandler;
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
            applyFurnace(ift, elapsed, level, pos);
        } else if (ift.isFactory()) {
            applyFactoryWithNeighbors(ift, elapsed, level, pos);
        } else if (ift.isGenerator()) {
            applyGenerator(ift, elapsed, level, pos);
        }
    }

    /** Generator в соло — симуляция без потерь. */
    private void applyGenerator(BlockIronFurnaceTileBase genTile,
                                long elapsed, Level level, BlockPos pos) {
        // COUNT
        int totalFuel = CatchupSimulation.countGeneratorFuel(genTile, level);
        long burnTicksPerFuel = CatchupSimulation.getBurnTicksPerFuel(genTile);
        int rfPerTick = CatchupSimulation.getGeneratorRfPerTick(genTile);
        // SIMULATE
        CatchupSimulation.SimulationResult result = CatchupSimulation.simulateGeneratorOnly(
                totalFuel, burnTicksPerFuel, rfPerTick,
                genTile.getCapacity(), genTile.getEnergy(),
                elapsed);
        // APPLY
        CatchupSimulation.applyGeneratorOnly(genTile, level, result);
    }

    /** Furnace в соло — симуляция без потерь. */
    private void applyFurnace(BlockIronFurnaceTileBase ift,
                              long elapsed, Level level, BlockPos pos) {
        // Пока используем старую логику для Furnace (там нет RF, только предметы)
        FurnaceMode.apply(ift, elapsed, level, pos);
    }

    /** Factory + генераторы — симуляция без потерь. */
    private void applyFactoryWithNeighbors(BlockIronFurnaceTileBase factoryTile,
                                           long elapsed, Level level, BlockPos pos) {
        // Ищем соседний генератор
        BlockIronFurnaceTileBase genTile = findNeighborGenerator(factoryTile, level, pos);

        // COUNT
        int totalSmeltable = CatchupSimulation.countFactoryInputs(factoryTile, level);
        int outputSpace = CatchupSimulation.countFactoryOutputSpace(factoryTile, level);
        CatchupSimulation.FactorySmeltParams params =
                CatchupSimulation.computeFactoryParams(factoryTile);

        if (genTile == null) {
            // Factory без генератора
            CatchupSimulation.SimulationResult result = CatchupSimulation.simulateFactoryOnly(
                    totalSmeltable, outputSpace,
                    params.maxRfPerItem, params.totalRfPerTick, params.maxCookTime,
                    factoryTile.getEnergy(), elapsed);
            CatchupSimulation.applyFactoryOnly(factoryTile, level, result);
            return;
        }

        // Factory + Generator
        int totalFuel = CatchupSimulation.countGeneratorFuel(genTile, level);
        long burnTicksPerFuel = CatchupSimulation.getBurnTicksPerFuel(genTile);
        int rfPerTick = CatchupSimulation.getGeneratorRfPerTick(genTile);

        CatchupSimulation.SimulationResult result = CatchupSimulation.simulate(
                totalFuel, burnTicksPerFuel, rfPerTick,
                totalSmeltable, outputSpace,
                params.maxRfPerItem, params.totalRfPerTick, params.maxCookTime,
                genTile.getCapacity(), genTile.getEnergy(),
                factoryTile.getCapacity(), factoryTile.getEnergy(),
                elapsed);

        CatchupSimulation.applyResult(genTile, factoryTile, level, pos, result);
    }

    private static BlockIronFurnaceTileBase findNeighborGenerator(
            BlockIronFurnaceTileBase factoryTile, Level level, BlockPos pos) {
        for (net.minecraft.core.Direction dir : net.minecraft.core.Direction.Plane.HORIZONTAL) {
            BlockPos neighbor = pos.relative(dir);
            if (!level.isLoaded(neighbor)) continue;
            BlockEntity be = level.getBlockEntity(neighbor);
            if (be instanceof BlockIronFurnaceTileBase genTile && genTile.isGenerator()) {
                net.minecraft.core.Direction genSide = dir.getOpposite();
                int setting = genTile.furnaceSettings.get(genSide.ordinal());
                if (setting == 2 || setting == 3) return genTile;
            }
        }
        return null;
    }
}
