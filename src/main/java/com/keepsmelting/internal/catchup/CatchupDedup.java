package com.keepsmelting.internal.catchup;

import net.minecraft.core.BlockPos;
import java.util.HashSet;
import java.util.Set;

/**
 * Дедупликация обработки печей в рамках одного игрового тика.
 */
public class CatchupDedup {

    private static final Set<BlockPos> processedThisTick = new HashSet<>();
    private static long lastTickCleared = -1;

    public static void mark(BlockPos pos) {
        if (pos != null) {
            processedThisTick.add(pos);
        }
    }

    public static boolean isProcessed(BlockPos pos) {
        return pos != null && processedThisTick.contains(pos);
    }

    public static boolean checkNewTick(long gameTime) {
        if (gameTime != lastTickCleared) {
            processedThisTick.clear();
            com.keepsmelting.api.catchup.AbstractCatchupHandler.clearDebugDedup();
            lastTickCleared = gameTime;
            return true;
        }
        return false;
    }
}
