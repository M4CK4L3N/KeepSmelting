/*
 * Decompiled with CFR.
 */
package ironfurnaces.network;

import ironfurnaces.network.PacketSettingsButton;
import ironfurnaces.network.PacketShowConfigButton;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

public class Messages {
    public static SimpleChannel INSTANCE;
    private static int ID;

    private static int nextID() {
        return ID++;
    }

    public static void registerMessages(String channelName) {
        INSTANCE = NetworkRegistry.newSimpleChannel((ResourceLocation)new ResourceLocation("ironfurnaces", channelName), () -> "1.0", s -> true, s -> true);
        INSTANCE.registerMessage(Messages.nextID(), PacketSettingsButton.class, PacketSettingsButton::toBytes, PacketSettingsButton::new, PacketSettingsButton::handle);
        INSTANCE.registerMessage(Messages.nextID(), PacketShowConfigButton.class, PacketShowConfigButton::toBytes, PacketShowConfigButton::new, PacketShowConfigButton::handle);
    }

    static {
        ID = 0;
    }
}

