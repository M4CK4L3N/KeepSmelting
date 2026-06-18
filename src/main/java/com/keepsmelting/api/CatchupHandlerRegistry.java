package com.keepsmelting.api;

import net.minecraft.world.level.block.entity.BlockEntity;

import javax.annotation.Nullable;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Реестр хендлеров. Другие моды регистрируют свои печки:
 * <pre>
 * CatchupHandlerRegistry.register(MyFurnaceTile.class, new MyFurnaceHandler());
 * </pre>
 */
public class CatchupHandlerRegistry {
    private static final ConcurrentHashMap<Class<?>, IFurnaceCatchupHandler> HANDLERS = new ConcurrentHashMap<>();

    public static void register(Class<? extends BlockEntity> tileClass, IFurnaceCatchupHandler handler) {
        HANDLERS.put(tileClass, handler);
    }

    @Nullable
    public static IFurnaceCatchupHandler find(Class<?> tileClass) {
        IFurnaceCatchupHandler h = HANDLERS.get(tileClass);
        if (h != null) return h;
        Class<?> c = tileClass.getSuperclass();
        while (c != null && c != Object.class) {
            h = HANDLERS.get(c);
            if (h != null) return h;
            c = c.getSuperclass();
        }
        return null;
    }
}
