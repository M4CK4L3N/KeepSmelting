package com.keepsmelting.api;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

/**
 * API для других модов.
 * Реализуй этот интерфейс и зарегистрируй через CatchupHandlerRegistry,
 * чтобы KeepSmelting поддерживал твою кастомную печку.
 */
public interface IFurnaceCatchupHandler {
    void applyCatchup(BlockEntity tile, long elapsed, Level level, BlockPos pos);
    void saveTime(BlockEntity tile, CompoundTag tag);
    void loadTime(BlockEntity tile, CompoundTag tag);
}
