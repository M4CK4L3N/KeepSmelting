/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.client.Minecraft
 *  net.minecraft.client.player.LocalPlayer
 *  net.minecraft.client.resources.language.I18n
 *  net.minecraft.network.chat.Component
 *  net.minecraft.network.chat.Component$Serializer
 *  net.minecraftforge.api.distmarker.Dist
 *  net.minecraftforge.api.distmarker.OnlyIn
 *  net.minecraftforge.common.MinecraftForge
 *  net.minecraftforge.event.TickEvent$ClientTickEvent
 *  net.minecraftforge.eventbus.api.SubscribeEvent
 *  net.minecraftforge.fml.common.Mod$EventBusSubscriber
 */
package ironfurnaces.update;

import ironfurnaces.Config;
import ironfurnaces.IronFurnaces;
import ironfurnaces.update.ThreadUpdateChecker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber
public class UpdateChecker {
    public static final String DOWNLOAD_LINK = "https://www.curseforge.com/minecraft/mc-mods/iron-furnaces";
    public static final String CHANGELOG_LINK = "https://raw.githubusercontent.com/Qelifern/IronFurnaces/1.20.1/ifchangelog.txt";
    public static boolean checkFailed;
    public static boolean needsUpdateNotify;
    public static int updateVersionInt;
    public static String updateVersionString;
    public static boolean threadFinished;

    public UpdateChecker() {
        IronFurnaces.LOGGER.info("Initializing Update Checker...");
        if (!((Boolean)Config.disableWebContent.get()).booleanValue()) {
            new ThreadUpdateChecker();
            MinecraftForge.EVENT_BUS.register((Object)this);
        }
    }

    @OnlyIn(value=Dist.CLIENT)
    @SubscribeEvent(receiveCanceled=true)
    public void onTick(TickEvent.ClientTickEvent event) {
        if (!((Boolean)Config.disableWebContent.get()).booleanValue() && Minecraft.m_91087_().f_91074_ != null) {
            LocalPlayer player = Minecraft.m_91087_().f_91074_;
            boolean id = false;
            if (checkFailed) {
                player.m_213846_((Component)Component.Serializer.m_130701_((String)I18n.m_118938_((String)"ironfurnaces.update.failed", (Object[])new Object[0])));
            } else if (needsUpdateNotify) {
                player.m_213846_((Component)Component.Serializer.m_130701_((String)I18n.m_118938_((String)"ironfurnaces.update.speech", (Object[])new Object[0])));
                player.m_213846_((Component)Component.Serializer.m_130701_((String)I18n.m_118938_((String)"ironfurnaces.update.version", (Object[])new Object[]{"1.20.1-beta418", updateVersionString})));
                player.m_213846_((Component)Component.Serializer.m_130701_((String)I18n.m_118938_((String)"ironfurnaces.update.buttons", (Object[])new Object[]{CHANGELOG_LINK, DOWNLOAD_LINK})));
            }
            if (threadFinished) {
                MinecraftForge.EVENT_BUS.unregister((Object)this);
            }
        }
    }

    static {
        threadFinished = false;
    }
}

