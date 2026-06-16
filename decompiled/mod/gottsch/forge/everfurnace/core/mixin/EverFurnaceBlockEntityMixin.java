/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.core.BlockPos
 *  net.minecraft.nbt.CompoundTag
 *  net.minecraft.server.level.ServerLevel
 *  net.minecraft.world.WorldlyContainer
 *  net.minecraft.world.inventory.RecipeHolder
 *  net.minecraft.world.inventory.StackedContentsCompatible
 *  net.minecraft.world.level.Level
 *  net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity
 *  net.minecraft.world.level.block.entity.BaseContainerBlockEntity
 *  net.minecraft.world.level.block.entity.BlockEntity
 *  net.minecraft.world.level.block.entity.BlockEntityType
 *  net.minecraft.world.level.block.state.BlockState
 *  org.spongepowered.asm.mixin.Mixin
 *  org.spongepowered.asm.mixin.Unique
 *  org.spongepowered.asm.mixin.injection.At
 *  org.spongepowered.asm.mixin.injection.Inject
 *  org.spongepowered.asm.mixin.injection.callback.CallbackInfo
 */
package mod.gottsch.forge.everfurnace.core.mixin;

import mod.gottsch.forge.everfurnace.api.EverFurnaceApi;
import mod.gottsch.forge.everfurnace.core.mixin.IEverFurnaceBlockEntityMixin;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.inventory.RecipeHolder;
import net.minecraft.world.inventory.StackedContentsCompatible;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value={AbstractFurnaceBlockEntity.class})
public abstract class EverFurnaceBlockEntityMixin
extends BaseContainerBlockEntity
implements IEverFurnaceBlockEntityMixin,
WorldlyContainer,
RecipeHolder,
StackedContentsCompatible {
    @Unique
    private static final String LAST_GAME_TIME_TAG = "everfurnace_lastGameTime";
    @Unique
    private static final String PENDING_NOTIFICATION_TAG = "everfurnace_pendingNotification";
    @Unique
    private static final String LAST_NOTIFICATION_TIME_TAG = "everfurnace_lastNotificationTime";
    @Unique
    private static final String PENDING_XP_TAG = "everfurnace_pendingXp";
    @Unique
    private static final int CURRENT_NBT_VERSION = 1;
    @Unique
    private static final String NBT_VERSION_TAG = "everfurnace_version";
    @Unique
    private long everFurnace_1_20_1$lastGameTime;
    @Unique
    private int everFurnace_1_20_1$pendingNotification;
    @Unique
    private long everFurnace_1_20_1$lastNotificationTime;
    @Unique
    private float everFurnace_1_20_1$pendingXp;

    protected EverFurnaceBlockEntityMixin(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Inject(method={"saveAdditional"}, at={@At(value="TAIL")})
    private void onSave(CompoundTag tag, CallbackInfo ci) {
        tag.m_128405_(NBT_VERSION_TAG, 1);
        tag.m_128356_(LAST_GAME_TIME_TAG, this.everFurnace_1_20_1$lastGameTime);
        tag.m_128405_(PENDING_NOTIFICATION_TAG, this.everFurnace_1_20_1$pendingNotification);
        tag.m_128356_(LAST_NOTIFICATION_TIME_TAG, this.everFurnace_1_20_1$lastNotificationTime);
        tag.m_128350_(PENDING_XP_TAG, this.everFurnace_1_20_1$pendingXp);
    }

    @Inject(method={"load"}, at={@At(value="TAIL")})
    private void onLoad(CompoundTag tag, CallbackInfo ci) {
        this.everFurnace_1_20_1$lastGameTime = tag.m_128454_(LAST_GAME_TIME_TAG);
        this.everFurnace_1_20_1$pendingNotification = tag.m_128451_(PENDING_NOTIFICATION_TAG);
        this.everFurnace_1_20_1$lastNotificationTime = tag.m_128454_(LAST_NOTIFICATION_TIME_TAG);
        this.everFurnace_1_20_1$pendingXp = tag.m_128457_(PENDING_XP_TAG);
    }

    @Inject(method={"serverTick"}, at={@At(value="HEAD")})
    private static void onTick(Level world, BlockPos pos, BlockState state, AbstractFurnaceBlockEntity blockEntity, CallbackInfo ci) {
        if (!EverFurnaceApi.isCatchupEnabled()) {
            return;
        }
        EverFurnaceBlockEntityMixin mixin = (EverFurnaceBlockEntityMixin)blockEntity;
        long currentGameTime = blockEntity.m_58904_().m_46467_();
        long localLastGameTime = mixin.everFurnace_1_20_1$lastGameTime;
        mixin.everFurnace_1_20_1$lastGameTime = currentGameTime;
        if (localLastGameTime == 0L) {
            return;
        }
        long deltaTime = currentGameTime - localLastGameTime;
        if (deltaTime < (long)EverFurnaceApi.getMinDeltaThreshold()) {
            return;
        }
        long finalDelta = deltaTime = Math.min(deltaTime, EverFurnaceApi.getMaxCatchupTicks());
        EverFurnaceApi.findHandler((BlockEntity)blockEntity).ifPresent(handler -> handler.applyCatchup((BlockEntity)blockEntity, finalDelta, (ServerLevel)world, pos));
    }

    @Override
    @Unique
    public long everFurnace_1_20_1$getLastGameTime() {
        return this.everFurnace_1_20_1$lastGameTime;
    }

    @Override
    @Unique
    public void everFurnace_1_20_1$setLastGameTime(long gameTime) {
        this.everFurnace_1_20_1$lastGameTime = gameTime;
    }

    @Override
    @Unique
    public int everFurnace_1_20_1$getPendingNotification() {
        return this.everFurnace_1_20_1$pendingNotification;
    }

    @Override
    @Unique
    public void everFurnace_1_20_1$setPendingNotification(int count) {
        this.everFurnace_1_20_1$pendingNotification = count;
    }

    @Override
    @Unique
    public long everFurnace_1_20_1$getLastNotificationTime() {
        return this.everFurnace_1_20_1$lastNotificationTime;
    }

    @Override
    @Unique
    public void everFurnace_1_20_1$setLastNotificationTime(long gameTime) {
        this.everFurnace_1_20_1$lastNotificationTime = gameTime;
    }

    @Override
    @Unique
    public float everFurnace_1_20_1$getPendingXp() {
        return this.everFurnace_1_20_1$pendingXp;
    }

    @Override
    @Unique
    public void everFurnace_1_20_1$setPendingXp(float xp) {
        this.everFurnace_1_20_1$pendingXp = xp;
    }
}

