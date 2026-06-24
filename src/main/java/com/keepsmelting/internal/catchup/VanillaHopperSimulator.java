package com.keepsmelting.internal.catchup;

import com.keepsmelting.internal.catchup.PipelineData.*;
import com.keepsmelting.mixin.IFurnaceAccessor;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;

import javax.annotation.Nullable;

/**
 * Facade for vanilla furnace hopper pipeline.
 * <p>
 * Phase 1: {@link PipelineDiscoverer#discover(ServerLevel, BlockPos, AbstractFurnaceBlockEntity, Recipe)}
 * Phase 2: {@link PipelineSimulator#simulate(Pipeline, long, int, int, int, int)}
 * Phase 3: {@link PipelineApplicator#apply(AbstractFurnaceBlockEntity, IFurnaceAccessor, ServerLevel, BlockPos, SimulationResult, Recipe, int, int, int, long)}
 */
public class VanillaHopperSimulator {

    private VanillaHopperSimulator() {}

    /** Строит Pipeline, сканируя BlockEntity вокруг печи. */
    public static Pipeline discover(
            ServerLevel level,
            BlockPos furnacePos,
            AbstractFurnaceBlockEntity furnace,
            @Nullable Recipe<?> recipe
    ) {
        return PipelineDiscoverer.discover(level, furnacePos, furnace, recipe);
    }

    /** Вычисляет bottleneck throughput. */
    public static SimulationResult simulate(
            Pipeline pipeline,
            long elapsed,
            int cookingProgressBefore,
            int cookTotal,
            int litDuration,
            int litTime
    ) {
        return PipelineSimulator.simulate(pipeline, elapsed, cookingProgressBefore, cookTotal, litDuration, litTime);
    }

    /** Применяет результат симуляции к реальным BlockEntity. */
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
        PipelineApplicator.apply(furnace, acc, level, pos, result, recipe, cookTotal, litDuration, cookingProgressBefore, elapsed);
    }
}
