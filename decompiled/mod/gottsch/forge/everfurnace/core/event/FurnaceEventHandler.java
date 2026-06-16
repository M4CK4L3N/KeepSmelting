/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.ChatFormatting
 *  net.minecraft.network.chat.Component
 *  net.minecraft.server.level.ChunkHolder
 *  net.minecraft.server.level.ServerLevel
 *  net.minecraft.world.Container
 *  net.minecraft.world.entity.player.Player
 *  net.minecraft.world.inventory.AbstractContainerMenu
 *  net.minecraft.world.inventory.AbstractFurnaceMenu
 *  net.minecraft.world.level.Level
 *  net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity
 *  net.minecraft.world.level.chunk.LevelChunk
 *  net.minecraftforge.event.entity.player.PlayerContainerEvent$Open
 *  net.minecraftforge.event.entity.player.PlayerEvent$PlayerLoggedInEvent
 *  net.minecraftforge.eventbus.api.SubscribeEvent
 *  net.minecraftforge.fml.common.Mod$EventBusSubscriber
 */
package mod.gottsch.forge.everfurnace.core.event;

import java.util.Map;
import mod.gottsch.forge.everfurnace.core.config.EverFurnaceConfig;
import mod.gottsch.forge.everfurnace.core.furnace.IEverFurnaceBlockEntity;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ChunkHolder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.AbstractFurnaceMenu;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraftforge.event.entity.player.PlayerContainerEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid="everfurnace")
public class FurnaceEventHandler {
    @SubscribeEvent
    public static void onContainerOpen(PlayerContainerEvent.Open event) {
        int count;
        Player player = event.getEntity();
        if (player.m_9236_().m_5776_()) {
            return;
        }
        AbstractContainerMenu abstractContainerMenu = event.getContainer();
        if (!(abstractContainerMenu instanceof AbstractFurnaceMenu)) {
            return;
        }
        AbstractFurnaceMenu furnaceMenu = (AbstractFurnaceMenu)abstractContainerMenu;
        Container container = furnaceMenu.f_38955_;
        if (!(container instanceof AbstractFurnaceBlockEntity)) {
            return;
        }
        AbstractFurnaceBlockEntity furnace = (AbstractFurnaceBlockEntity)container;
        boolean dirty = false;
        if (((Boolean)EverFurnaceConfig.COMMON.notifyPlayerOnCatchup.get()).booleanValue() && (count = FurnaceEventHandler.consumePendingNotification(furnace)) > 0) {
            FurnaceEventHandler.sendNotification(player, count);
            dirty = true;
        }
        if (dirty |= FurnaceEventHandler.awardPendingXp(player, furnace)) {
            furnace.m_6596_();
        }
    }

    @SubscribeEvent
    public static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (!((Boolean)EverFurnaceConfig.COMMON.notifyPlayerOnCatchup.get()).booleanValue()) {
            return;
        }
        if (!((Boolean)EverFurnaceConfig.COMMON.notifyOnLogin.get()).booleanValue()) {
            return;
        }
        Player player = event.getEntity();
        Level level = player.m_9236_();
        if (!(level instanceof ServerLevel)) {
            return;
        }
        ServerLevel serverLevel = (ServerLevel)level;
        int furnaceCount = 0;
        int totalItems = 0;
        for (ChunkHolder holder : serverLevel.m_7726_().f_8325_.m_140416_()) {
            LevelChunk chunk = holder.m_140085_();
            if (chunk == null) continue;
            for (Map.Entry entry : chunk.m_62954_().entrySet()) {
                AbstractFurnaceBlockEntity furnace;
                int count;
                Object v = entry.getValue();
                if (!(v instanceof AbstractFurnaceBlockEntity) || (count = FurnaceEventHandler.consumePendingNotification(furnace = (AbstractFurnaceBlockEntity)v)) <= 0) continue;
                furnace.m_6596_();
                ++furnaceCount;
                totalItems += count;
            }
        }
        if (totalItems <= 0) {
            return;
        }
        if (furnaceCount == 1) {
            FurnaceEventHandler.sendNotification(player, totalItems);
        } else {
            player.m_213846_((Component)Component.m_237113_((String)"[EverFurnace] ").m_130938_(style -> style.m_178520_(16753920)).m_7220_((Component)Component.m_237113_((String)String.valueOf(furnaceCount)).m_130938_(style -> style.m_131140_(ChatFormatting.GOLD).m_131136_(Boolean.valueOf(true)))).m_7220_((Component)Component.m_237113_((String)" furnaces cooked a combined ").m_130938_(style -> style.m_131140_(ChatFormatting.WHITE))).m_7220_((Component)Component.m_237113_((String)String.valueOf(totalItems)).m_130938_(style -> style.m_131140_(ChatFormatting.GOLD).m_131136_(Boolean.valueOf(true)))).m_7220_((Component)Component.m_237113_((String)" items while you were away.").m_130938_(style -> style.m_131140_(ChatFormatting.WHITE))));
        }
    }

    private static int consumePendingNotification(AbstractFurnaceBlockEntity furnace) {
        IEverFurnaceBlockEntity mixin = (IEverFurnaceBlockEntity)furnace;
        int count = mixin.everFurnace_1_20_1$getPendingNotification();
        if (count <= 0) {
            return 0;
        }
        mixin.everFurnace_1_20_1$setPendingNotification(0);
        return count;
    }

    private static boolean awardPendingXp(Player player, AbstractFurnaceBlockEntity furnace) {
        IEverFurnaceBlockEntity mixin = (IEverFurnaceBlockEntity)furnace;
        float pending = mixin.everFurnace_1_20_1$getPendingXp();
        if (pending <= 0.0f) {
            return false;
        }
        int toAward = (int)pending;
        float remainder = pending - (float)toAward;
        if (toAward > 0) {
            player.m_6756_(toAward);
        }
        mixin.everFurnace_1_20_1$setPendingXp(remainder);
        return true;
    }

    private static void sendNotification(Player player, int count) {
        player.m_213846_((Component)Component.m_237113_((String)"[EverFurnace] ").m_130938_(style -> style.m_178520_(16753920)).m_7220_((Component)Component.m_237113_((String)"Your furnace cooked ").m_130938_(style -> style.m_131140_(ChatFormatting.WHITE))).m_7220_((Component)Component.m_237113_((String)String.valueOf(count)).m_130938_(style -> style.m_131140_(ChatFormatting.GOLD).m_131136_(Boolean.valueOf(true)))).m_7220_((Component)Component.m_237113_((String)((count == 1 ? " item" : " items") + " while you were away.")).m_130938_(style -> style.m_131140_(ChatFormatting.WHITE))));
    }
}

