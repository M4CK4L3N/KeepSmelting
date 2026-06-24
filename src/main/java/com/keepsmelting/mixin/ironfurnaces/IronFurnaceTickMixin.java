package com.keepsmelting.mixin.ironfurnaces;

import com.keepsmelting.KeepSmeltingConfig;
import com.keepsmelting.api.CatchupHandlerRegistry;
import com.keepsmelting.internal.catchup.CatchupDedup;
import ironfurnaces.tileentity.furnaces.BlockIronFurnaceTileBase;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets = "ironfurnaces.tileentity.furnaces.BlockIronFurnaceTileBase")
public abstract class IronFurnaceTickMixin {

    @Unique
    private long keepsmelting$lastRealTime;

    @Unique
    private String keepsmelting$activeTimeMode;

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

    @Inject(method = "tick", at = @At("HEAD"), remap = false)
    private static void onTick(Level level, BlockPos pos, BlockState state,
                               BlockIronFurnaceTileBase tile, CallbackInfo ci) {
        if (level.isClientSide) return;

        // Очищаем кэш дедупликации раз в тик
        CatchupDedup.checkNewTick(level.getGameTime());

        if (!KeepSmeltingConfig.COMMON.catchupEnabled.get()) return;

        // Дедупликация: если печь уже обработана сетью (от Factory), пропускаем
        if (CatchupDedup.isProcessed(pos)) return;
        CatchupDedup.mark(pos);

        IronFurnaceTickMixin self = (IronFurnaceTickMixin) (Object) tile;

        String currentMode = KeepSmeltingConfig.COMMON.timeMode.get().name();
        if (self.keepsmelting$activeTimeMode != null && !self.keepsmelting$activeTimeMode.equals(currentMode)) {
            self.keepsmelting$lastRealTime = 0L;
        }
        self.keepsmelting$activeTimeMode = currentMode;

        long now = KeepSmeltingConfig.COMMON.timeMode.get() == KeepSmeltingConfig.TimeMode.GAMETIME
                ? level.getGameTime() : System.currentTimeMillis();
        long last = self.keepsmelting$lastRealTime;
        self.keepsmelting$lastRealTime = now;
        if (last == 0) return;

        long elapsed = KeepSmeltingConfig.COMMON.timeMode.get() == KeepSmeltingConfig.TimeMode.GAMETIME
                ? now - last : (now - last) / 50L;
        if (elapsed < KeepSmeltingConfig.COMMON.minDeltaThreshold.get()) return;
        elapsed = Math.min(elapsed, KeepSmeltingConfig.COMMON.maxCatchupTicks.get());
        if (elapsed <= 0) return;

        CatchupHandlerRegistry.find(tile.getClass()).applyCatchup(tile, elapsed, level, pos);
    }
}
