/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.core.BlockPos
 *  net.minecraft.nbt.CompoundTag
 *  net.minecraft.server.level.ServerLevel
 *  net.minecraft.world.level.Level
 *  net.minecraft.world.level.block.entity.BlockEntity
 *  net.minecraft.world.level.block.entity.CampfireBlockEntity
 *  net.minecraft.world.level.block.state.BlockState
 *  org.spongepowered.asm.mixin.Mixin
 *  org.spongepowered.asm.mixin.Unique
 *  org.spongepowered.asm.mixin.injection.At
 *  org.spongepowered.asm.mixin.injection.Inject
 *  org.spongepowered.asm.mixin.injection.callback.CallbackInfo
 */
package mod.gottsch.forge.everfurnace.core.mixin;

import mod.gottsch.forge.everfurnace.api.EverFurnaceApi;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.CampfireBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value={CampfireBlockEntity.class})
public abstract class CampfireBlockEntityMixin {
    @Unique
    private static final String LAST_GAME_TIME_TAG = "everfurnace_lastGameTime";
    @Unique
    private static final String NBT_VERSION_TAG = "everfurnace_version";
    @Unique
    private static final int CURRENT_NBT_VERSION = 1;
    @Unique
    private long everFurnace_1_20_1$lastGameTime;

    @Inject(method={"saveAdditional"}, at={@At(value="TAIL")})
    private void everFurnace_1_20_1$onSave(CompoundTag tag, CallbackInfo ci) {
        tag.m_128405_(NBT_VERSION_TAG, 1);
        tag.m_128356_(LAST_GAME_TIME_TAG, this.everFurnace_1_20_1$lastGameTime);
    }

    @Inject(method={"load"}, at={@At(value="TAIL")})
    private void everFurnace_1_20_1$onLoad(CompoundTag tag, CallbackInfo ci) {
        this.everFurnace_1_20_1$lastGameTime = tag.m_128454_(LAST_GAME_TIME_TAG);
    }

    @Inject(method={"cookTick"}, at={@At(value="HEAD")})
    private static void everFurnace_1_20_1$onCookTick(Level world, BlockPos pos, BlockState state, CampfireBlockEntity blockEntity, CallbackInfo ci) {
        if (!EverFurnaceApi.isCatchupEnabled()) {
            return;
        }
        CampfireBlockEntityMixin mixin = (CampfireBlockEntityMixin)blockEntity;
        long currentGameTime = world.m_46467_();
        long localLastGameTime = mixin.everFurnace_1_20_1$lastGameTime;
        mixin.everFurnace_1_20_1$lastGameTime = currentGameTime;
        if (localLastGameTime == 0L) {
            return;
        }
        long deltaTime = currentGameTime - localLastGameTime;
        if (deltaTime < (long)EverFurnaceApi.getMinDeltaThreshold()) {
            return;
        }
        deltaTime = Math.min(deltaTime, EverFurnaceApi.getMaxCatchupTicks());
        if (!(world instanceof ServerLevel)) {
            return;
        }
        ServerLevel serverLevel = (ServerLevel)world;
        long finalDelta = deltaTime;
        EverFurnaceApi.findHandler((BlockEntity)blockEntity).ifPresent(handler -> handler.applyCatchup((BlockEntity)blockEntity, finalDelta, serverLevel, pos));
    }
}

