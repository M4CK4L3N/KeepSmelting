package com.keepsmelting.internal.catchup;

import com.keepsmelting.internal.catchup.PipelineData.*;

/**
 * Phase 2: Pipeline simulation — computes throughput bottleneck
 * based on elapsed ticks, fuel, and output space.
 */
public class PipelineSimulator {

    private PipelineSimulator() {}

    /**
     * Вычисляет, сколько предметов можно переплавить за {@code elapsed} тиков
     * с учётом pipeline, топлива и места на выходе.
     */
    public static SimulationResult simulate(
            Pipeline pipeline,
            long elapsed,
            int cookingProgressBefore,
            int cookTotal,
            int litDuration,
            int litTime
    ) {
        if (cookTotal <= 0 || litDuration <= 0) {
            return new SimulationResult(0, 0, 0);
        }

        // 1. Пропускная способность каждого узла цепочки
        long inputTP = pipeline.inputThroughput(elapsed);
        long fuelTP = pipeline.fuelThroughput(elapsed);
        long outputTP = pipeline.outputThroughput(elapsed);
        long furnaceTP = elapsed / cookTotal;

        // 2. Bottleneck цепочки (без топлива)
        long chainBottleneck = min(inputTP, furnaceTP, outputTP);

        // 3. Топливный лимит
        long maxFuelArrivals = Math.min(pipeline.fuelItemTotal(), fuelTP);
        long totalBurnTicks = maxFuelArrivals * litDuration + (litTime > 0 ? litTime : 0);
        long actualTicks = Math.min(elapsed, totalBurnTicks);

        long maxByFuel = 0;
        if (cookTotal > 0) {
            int progressNeeded = cookTotal - cookingProgressBefore;
            if (actualTicks >= progressNeeded) {
                long leftover = actualTicks - progressNeeded;
                maxByFuel = 1 + (leftover / cookTotal);
            }
        }

        // 4. Финальный лимит = минимум из всех ограничений
        long itemsToCook = min(
                chainBottleneck,
                maxByFuel,
                pipeline.inputItemTotal(),
                pipeline.outputSlotSpace()
        );

        // 5. Расход топлива
        int fi = (int) Math.min(itemsToCook, Integer.MAX_VALUE);
        int fuelConsumed = 0;
        if (fi > 0) {
            long ticksNeeded = (long) fi * cookTotal - cookingProgressBefore;
            if (ticksNeeded < 0) ticksNeeded = 0;
            fuelConsumed = (int) Math.min(pipeline.fuelItemTotal(),
                    (int) Math.ceil((double) ticksNeeded / litDuration));
        }

        return new SimulationResult(fi, fuelConsumed, fi);
    }

    private static long min(long... values) {
        long m = Long.MAX_VALUE;
        for (long v : values) {
            if (v < m) m = v;
        }
        return m;
    }
}
