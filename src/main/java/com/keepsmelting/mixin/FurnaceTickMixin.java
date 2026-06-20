package com.keepsmelting.mixin;

import com.keepsmelting.KeepSmeltingConfig;
import com.keepsmelting.api.CatchupHandlerRegistry;
import com.keepsmelting.api.IFurnaceCatchupHandler;
import com.keepsmelting.internal.catchup.VanillaCatchupHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AbstractFurnaceBlockEntity.class)
public abstract class FurnaceTickMixin {

    @Unique
    private long keepsmelting$lastRealTime;

    @Unique
    private String keepsmelting$activeTimeMode;

    /**
     * Флаг для предотвращения двойной обработки.
     * Iron Furnaces Tile наследует AbstractFurnaceBlockEntity,
     * поэтому ванильная mixin может сработать повторно на той же печи.
     * Устанавливается перед catchup, сбрасывается после.
     */
    @Unique
    private boolean keepsmelting$catchupProcessed;

    @Inject(method = "saveAdditional", at = @At("TAIL"))
    private void onSave(CompoundTag tag, CallbackInfo ci) {
        tag.putLong("keepsmelting_lastRealTime", this.keepsmelting$lastRealTime);
        tag.putString("keepsmelting_timeMode", KeepSmeltingConfig.COMMON.timeMode.get().name());
    }

    @Inject(method = "load", at = @At("TAIL"))
    private void onLoad(CompoundTag tag, CallbackInfo ci) {
        String savedMode = tag.getString("keepsmelting_timeMode");
        String currentMode = KeepSmeltingConfig.COMMON.timeMode.get().name();
        this.keepsmelting$lastRealTime = (!savedMode.isEmpty() && savedMode.equals(currentMode))
                ? tag.getLong("keepsmelting_lastRealTime") : 0L;
    }

    @Inject(method = "serverTick", at = @At("HEAD"))
    private static void onTick(Level world, BlockPos pos, BlockState state,
                               AbstractFurnaceBlockEntity furnace, CallbackInfo ci) {
        if (world.isClientSide) return;

        // Очищаем кэш дедупликации debug-сообщений каждый тик
        com.keepsmelting.api.catchup.AbstractCatchupHandler.clearDebugDedup();

        if (!KeepSmeltingConfig.COMMON.catchupEnabled.get()) return;

        FurnaceTickMixin self = (FurnaceTickMixin) (Object) furnace;

        // Предотвращаем двойную обработку: если Iron Furnaces mixin
        // уже обработала эту печь в этом тике, пропускаем
        if (self.keepsmelting$catchupProcessed) {
            self.keepsmelting$catchupProcessed = false;
            return;
        }

        // Check time mode change
        String currentMode = KeepSmeltingConfig.COMMON.timeMode.get().name();
        if (self.keepsmelting$activeTimeMode != null && !self.keepsmelting$activeTimeMode.equals(currentMode)) {
            self.keepsmelting$lastRealTime = 0L;
        }
        self.keepsmelting$activeTimeMode = currentMode;

        // Calculate elapsed ticks
        long now = KeepSmeltingConfig.COMMON.timeMode.get() == KeepSmeltingConfig.TimeMode.GAMETIME
                ? world.getGameTime() : System.currentTimeMillis();
        long last = self.keepsmelting$lastRealTime;
        self.keepsmelting$lastRealTime = now;
        if (last == 0) return;

        long elapsed = KeepSmeltingConfig.COMMON.timeMode.get() == KeepSmeltingConfig.TimeMode.GAMETIME
                ? now - last : (now - last) / 50L;
        if (elapsed < KeepSmeltingConfig.COMMON.minDeltaThreshold.get()) return;
        elapsed = Math.min(elapsed, KeepSmeltingConfig.COMMON.maxCatchupTicks.get());
        if (elapsed <= 0) return;

        // Устанавливаем флаг — если Iron Furnaces mixin вызовется
        // на этом же тике, она увидит флаг и пропустит обработку
        self.keepsmelting$catchupProcessed = true;

        // Delegate to handler
        IFurnaceCatchupHandler handler = CatchupHandlerRegistry.find(furnace.getClass());
        if (handler != null) {
            handler.applyCatchup(furnace, elapsed, world, pos);
        } else {
            VanillaCatchupHandler.INSTANCE.applyCatchup(furnace, elapsed, world, pos);
        }
    }
}
