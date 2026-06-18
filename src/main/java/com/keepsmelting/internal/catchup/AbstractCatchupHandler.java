package com.keepsmelting.internal.catchup;

import com.keepsmelting.KeepSmeltingConfig;
import com.keepsmelting.api.IFurnaceCatchupHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.AABB;

import java.util.List;

public abstract class AbstractCatchupHandler implements IFurnaceCatchupHandler {

    private static final String TAG_LAST_TIME = "keepsmelting_lastRealTime";

    protected long lastRealTime;
    protected String activeTimeMode;

    @Override
    public void saveTime(BlockEntity tile, CompoundTag tag) {
        tag.putLong(TAG_LAST_TIME, this.lastRealTime);
        tag.putString("keepsmelting_timeMode", KeepSmeltingConfig.COMMON.timeMode.get().name());
    }

    @Override
    public void loadTime(BlockEntity tile, CompoundTag tag) {
        String savedMode = tag.getString("keepsmelting_timeMode");
        String currentMode = KeepSmeltingConfig.COMMON.timeMode.get().name();
        if (!savedMode.isEmpty() && savedMode.equals(currentMode)) {
            this.lastRealTime = tag.getLong(TAG_LAST_TIME);
        } else {
            this.lastRealTime = 0L;
        }
    }

    public long calcElapsed(Level level, long now) {
        long last = lastRealTime;
        lastRealTime = now;
        if (last == 0) return 0;

        long elapsed;
        if (KeepSmeltingConfig.COMMON.timeMode.get() == KeepSmeltingConfig.TimeMode.GAMETIME) {
            elapsed = now - last;
        } else {
            elapsed = (now - last) / 50L;
        }

        int minDelta = KeepSmeltingConfig.COMMON.minDeltaThreshold.get();
        if (elapsed < minDelta) return 0;

        long max = KeepSmeltingConfig.COMMON.maxCatchupTicks.get();
        return Math.min(elapsed, max);
    }

    public boolean timeModeChanged() {
        String current = KeepSmeltingConfig.COMMON.timeMode.get().name();
        if (activeTimeMode != null && !activeTimeMode.equals(current)) {
            lastRealTime = 0L;
            return true;
        }
        activeTimeMode = current;
        return false;
    }

    public static void sendChatDebug(Level level, BlockPos pos, String mode, long elapsed,
                                     int fuelDelta, int outputDelta, int cookDelta, int burnDelta, boolean lit) {
        KeepSmeltingConfig.DebugMode dm = KeepSmeltingConfig.COMMON.debugMode.get();
        if (dm == KeepSmeltingConfig.DebugMode.OFF) return;
        if (!(level instanceof ServerLevel serverLevel)) return;

        Component msg;
        if ("Generator".equals(mode)) {
            String rfStr = outputDelta > 0 ? String.format("rf: §a+%d", outputDelta) : "rf: 0";
            String fuelStr = fuelDelta > 0 ? String.format("fuel: -%d", fuelDelta) : "fuel: 0";
            msg = Component.literal(String.format("§7[§6KeepSmelting§7] §e[Generator] §f%s §7| §e%d§7t§r | %s §7| %s §7| §7lit=%s",
                    pos.toShortString(), elapsed, rfStr, fuelStr, lit));
        } else {
            String itemStr = outputDelta > 0 ? String.format("smelted: §a%d", outputDelta) : "smelted: 0";
            String fuelStr = fuelDelta > 0 ? String.format("fuel: -%d", fuelDelta) : "fuel: 0";
            msg = Component.literal(String.format("§7[§6KeepSmelting§7] §e[%s] §f%s §7| §e%d§7t§r | %s §7| %s §7| §7lit=%s",
                    mode, pos.toShortString(), elapsed, itemStr, fuelStr, lit));
        }

        if (dm == KeepSmeltingConfig.DebugMode.CHAT) {
            sendToNearbyPlayers(serverLevel, pos, msg);
        } else {
            com.keepsmelting.KeepSmelting.LOGGER.info(msg.getString());
        }
    }

    private static void sendToNearbyPlayers(ServerLevel level, BlockPos pos, Component msg) {
        AABB box = new AABB(pos).inflate(16);
        List<ServerPlayer> players = level.getEntitiesOfClass(ServerPlayer.class, box);
        for (ServerPlayer p : players) {
            p.sendSystemMessage(msg);
        }
    }
}
