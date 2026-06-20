package com.keepsmelting.internal.ironfurnaces;

import net.minecraft.core.BlockPos;

import java.util.HashSet;
import java.util.Set;

/**
 * Дедупликация обработки печей в рамках одного игрового тика.
 * Нужна чтобы сеть печей (Factory + Generator) не обрабатывалась дважды:
 * один раз от Factory (через симуляцию) и ещё раз от Generator (через его mixin-тик).
 */
public class CatchupDedup {

    private static final Set<BlockPos> processedThisTick = new HashSet<>();
    private static long lastTickCleared = -1;

    /** Помечает позицию как обработанную. */
    public static void mark(BlockPos pos) {
        if (pos != null) {
            processedThisTick.add(pos);
        }
    }

    /** Проверяет, обработана ли позиция уже в этом тике. */
    public static boolean isProcessed(BlockPos pos) {
        return pos != null && processedThisTick.contains(pos);
    }

    /** Очищает кэш и возвращает true, если начался новый тик. */
    public static boolean checkNewTick(long gameTime) {
        if (gameTime != lastTickCleared) {
            processedThisTick.clear();
            com.keepsmelting.internal.catchup.AbstractCatchupHandler.clearDebugDedup();
            lastTickCleared = gameTime;
            return true;
        }
        return false;
    }
}
