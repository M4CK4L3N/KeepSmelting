package com.keepsmelting.internal.catchup;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;

import javax.annotation.Nullable;

/**
 * Data model for vanilla furnace hopper pipeline.
 *
 * @see PipelineDiscoverer
 * @see PipelineSimulator
 * @see PipelineApplicator
 */
public final class PipelineData {

    private PipelineData() {}

    /** Скорость воронки: 1 предмет за 8 тиков (HopperBlockEntity.MOVE_ITEM_SPEED). */
    public static final long HOPPER_TICKS_PER_ITEM = 8;

    /** Тип узла конвейера. */
    public enum NodeType {
        INPUT_SOURCE,    // Контейнер над печью/воронкой (руда/еда)
        INPUT_HOPPER,    // Воронка над печью (FACING=DOWN)
        FUEL_SOURCE,     // Контейнер сбоку/над боковой воронкой (топливо)
        FUEL_HOPPER,     // Воронка сбоку (FACING в сторону печи)
        FURNACE,         // Сама печь
        OUTPUT_HOPPER,   // Воронка под печью (выталкивает готовое)
        OUTPUT_DEST      // Контейнер под печью/воронкой (приёмник)
    }

    /**
     * Узел конвейера.
     *
     * @param type        тип узла
     * @param pos         позиция в мире
     * @param facing      направление (для воронок; {@code null} для контейнеров)
     * @param ticksPerItem сколько тиков на предмет (0 = безлимит)
     */
    public record PipelineNode(
            NodeType type,
            BlockPos pos,
            @Nullable Direction facing,
            long ticksPerItem
    ) {
        /** Пропускная способность узла за {@code elapsed} тиков. */
        public long throughput(long elapsed) {
            if (ticksPerItem <= 0) return Long.MAX_VALUE;
            return elapsed / ticksPerItem;
        }
    }

    /**
     * Конвейер — полная цепочка от источников до приёмников.
     *
     * @param nodes            все узлы
     * @param inputItemTotal   всего smeltable предметов на входе (печь + контейнеры)
     * @param fuelItemTotal    всего топлива (печь + контейнеры)
     * @param outputSlotSpace  свободное место на выходе (печь + контейнеры)
     * @param inputHopperCount есть ли воронка на входе (0 или 1)
     * @param fuelHopperCount  сколько боковых воронок с топливом (0-4)
     * @param outputHopperCount есть ли воронка на выходе (0 или 1)
     */
    public record Pipeline(
            java.util.List<PipelineNode> nodes,
            int inputItemTotal,
            int fuelItemTotal,
            int outputSlotSpace,
            int inputHopperCount,
            int fuelHopperCount,
            int outputHopperCount
    ) {
        /** Пропускная способность входа. */
        public long inputThroughput(long elapsed) {
            if (inputHopperCount == 0) return Long.MAX_VALUE;
            return elapsed / HOPPER_TICKS_PER_ITEM;
        }

        /** Пропускная способность топливных воронок (все параллельно). */
        public long fuelThroughput(long elapsed) {
            if (fuelHopperCount == 0) return Long.MAX_VALUE;
            return (long) fuelHopperCount * (elapsed / HOPPER_TICKS_PER_ITEM);
        }

        /** Пропускная способность выхода. */
        public long outputThroughput(long elapsed) {
            if (outputHopperCount == 0) return Long.MAX_VALUE;
            return elapsed / HOPPER_TICKS_PER_ITEM;
        }
    }

    /** Результат симуляции. */
    public record SimulationResult(
            int itemsToCook,
            int fuelConsumed,
            int inputConsumed
    ) {}
}
