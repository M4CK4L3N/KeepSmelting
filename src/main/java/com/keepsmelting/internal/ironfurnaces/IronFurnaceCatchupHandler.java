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
            FurnaceMode.apply(ift, elapsed, level, pos);
        } else if (ift.isFactory()) {
            applyFactoryWithNeighbors(ift, elapsed, level, pos);
        } else if (ift.isGenerator()) {
            GeneratorMode.apply(ift, elapsed, level, pos);
        }
    }

    /** Factory + генераторы: подсчёт → симуляция → применение (один проход). */
    private void applyFactoryWithNeighbors(BlockIronFurnaceTileBase factoryTile,
                                           long elapsed, Level level, BlockPos pos) {
        // Ищем соседний генератор
        BlockIronFurnaceTileBase genTile = findNeighborGenerator(factoryTile, level, pos);
        if (genTile == null) {
            // Нет генератора — просто завод
            FactoryMode.apply(factoryTile, elapsed, level, pos);
            return;
        }

        // === PHASE 1: COUNT ===
        int totalFuel = CatchupSimulation.countGeneratorFuel(genTile, level);
        long burnTicksPerFuel = CatchupSimulation.getBurnTicksPerFuel(genTile);
        int rfPerTick = CatchupSimulation.getGeneratorRfPerTick(genTile);
        int totalSmeltable = CatchupSimulation.countFactoryInputs(factoryTile, level);
        int outputSpace = CatchupSimulation.countFactoryOutputSpace(factoryTile, level);
        CatchupSimulation.FactorySmeltParams params =
                CatchupSimulation.computeFactoryParams(factoryTile);

        // === PHASE 2: SIMULATE ===
        CatchupSimulation.SimulationResult result = CatchupSimulation.simulate(
                totalFuel, burnTicksPerFuel, rfPerTick,
                totalSmeltable, outputSpace,
                params.maxRfPerItem, params.totalRfPerTick, params.maxCookTime,
                genTile.getCapacity(), genTile.getEnergy(),
                factoryTile.getCapacity(), factoryTile.getEnergy(),
                elapsed);

        // === PHASE 3: APPLY ===
        CatchupSimulation.applyResult(genTile, factoryTile, level, pos, result);
    }

    /** Находит первый соседний генератор, который отдаёт RF на завод. */
    private static BlockIronFurnaceTileBase findNeighborGenerator(
            BlockIronFurnaceTileBase factoryTile, Level level, BlockPos pos) {
        for (net.minecraft.core.Direction dir : net.minecraft.core.Direction.Plane.HORIZONTAL) {
            BlockPos neighbor = pos.relative(dir);
            if (!level.isLoaded(neighbor)) continue;
            BlockEntity be = level.getBlockEntity(neighbor);
            if (be instanceof BlockIronFurnaceTileBase genTile && genTile.isGenerator()) {
                // Проверяем, что отдаёт RF на завод
                net.minecraft.core.Direction genSide = dir.getOpposite();
                int setting = genTile.furnaceSettings.get(genSide.ordinal());
                if (setting == 2 || setting == 3) return genTile;
            }
        }
        return null;
    }
}
