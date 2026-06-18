/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  io.netty.buffer.ByteBuf
 *  net.minecraft.server.level.ServerPlayer
 *  net.minecraftforge.network.NetworkEvent$Context
 */
package ironfurnaces.network;

import io.netty.buffer.ByteBuf;
import ironfurnaces.capability.CapabilityPlayerShowConfig;
import java.util.function.Supplier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

public class PacketShowConfigButton {
    private int set;

    public PacketShowConfigButton(ByteBuf buf) {
        this.set = buf.readInt();
    }

    public void toBytes(ByteBuf buf) {
        buf.writeInt(this.set);
    }

    public PacketShowConfigButton(int set) {
        this.set = set;
    }

    public void handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context ctx = supplier.get();
        ctx.enqueueWork(() -> {
            ServerPlayer player = ctx.getSender();
            player.getCapability(CapabilityPlayerShowConfig.CONFIG).ifPresent(h -> h.set(this.set));
        });
        ctx.setPacketHandled(true);
    }
}

