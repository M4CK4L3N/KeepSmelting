/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.world.level.block.entity.BlockEntity
 *  net.minecraft.world.level.block.entity.BlockEntityType
 */
package mod.gottsch.forge.everfurnace.api;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.BooleanSupplier;
import java.util.function.IntSupplier;
import java.util.function.LongSupplier;
import java.util.function.Predicate;
import mod.gottsch.forge.everfurnace.api.CookingCatchupHandler;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;

public final class EverFurnaceApi {
    private static final Map<BlockEntityType<?>, CookingCatchupHandler> HANDLERS = new LinkedHashMap();
    private static final List<Map.Entry<Predicate<BlockEntity>, CookingCatchupHandler>> FALLBACKS = new ArrayList<Map.Entry<Predicate<BlockEntity>, CookingCatchupHandler>>();
    private static BooleanSupplier catchupEnabled = () -> true;
    private static LongSupplier maxCatchupTicks = () -> 24000L;
    private static IntSupplier minDeltaThreshold = () -> 20;

    private EverFurnaceApi() {
    }

    public static <T extends BlockEntity> void registerHandler(BlockEntityType<T> type, CookingCatchupHandler handler) {
        HANDLERS.put(type, handler);
    }

    public static void registerFallback(Predicate<BlockEntity> predicate, CookingCatchupHandler handler) {
        FALLBACKS.add(Map.entry(predicate, handler));
    }

    public static Optional<CookingCatchupHandler> findHandler(BlockEntity be) {
        CookingCatchupHandler exact = HANDLERS.get(be.m_58903_());
        if (exact != null) {
            return Optional.of(exact);
        }
        for (Map.Entry<Predicate<BlockEntity>, CookingCatchupHandler> fallback : FALLBACKS) {
            if (!fallback.getKey().test(be)) continue;
            return Optional.of(fallback.getValue());
        }
        return Optional.empty();
    }

    public static void bindConfig(BooleanSupplier enabled, LongSupplier maxTicks, IntSupplier minDelta) {
        catchupEnabled = enabled;
        maxCatchupTicks = maxTicks;
        minDeltaThreshold = minDelta;
    }

    public static boolean isCatchupEnabled() {
        return catchupEnabled.getAsBoolean();
    }

    public static long getMaxCatchupTicks() {
        return maxCatchupTicks.getAsLong();
    }

    public static int getMinDeltaThreshold() {
        return minDeltaThreshold.getAsInt();
    }
}

