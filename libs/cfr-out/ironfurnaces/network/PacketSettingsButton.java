/*
 * Decompiled with CFR.
 */
package ironfurnaces.network;

import io.netty.buffer.ByteBuf;
import ironfurnaces.tileentity.furnaces.BlockIronFurnaceTileBase;
import java.util.function.Supplier;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

public class PacketSettingsButton {
    private int x;
    private int y;
    private int z;
    private int index;
    private int set;

    public PacketSettingsButton(ByteBuf buf) {
        this.x = buf.readInt();
        this.y = buf.readInt();
        this.z = buf.readInt();
        this.index = buf.readInt();
        this.set = buf.readInt();
    }

    public void toBytes(ByteBuf buf) {
        buf.writeInt(this.x);
        buf.writeInt(this.y);
        buf.writeInt(this.z);
        buf.writeInt(this.index);
        buf.writeInt(this.set);
    }

    public PacketSettingsButton(BlockPos pos, int index, int set) {
        this.x = pos.m_123341_();
        this.y = pos.m_123342_();
        this.z = pos.m_123343_();
        this.index = index;
        this.set = set;
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ((NetworkEvent.Context)ctx.get()).getSender();
            BlockPos pos = new BlockPos(this.x, this.y, this.z);
            BlockIronFurnaceTileBase te = (BlockIronFurnaceTileBase)player.m_9236_().m_7702_(pos);
            if (player.m_9236_().m_46749_(pos)) {
                te.furnaceSettings.set(this.index, this.set);
                te.m_58904_().markAndNotifyBlock(pos, player.m_9236_().m_46745_(pos), te.m_58904_().m_8055_(pos).m_60734_().m_49966_(), te.m_58904_().m_8055_(pos), 2, 0);
                te.m_6596_();
            }
        });
        ctx.get().setPacketHandled(true);
    }
}

