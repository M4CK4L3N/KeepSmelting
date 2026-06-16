/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.core.BlockPos
 *  net.minecraft.network.FriendlyByteBuf
 *  net.minecraftforge.api.distmarker.Dist
 *  net.minecraftforge.fml.DistExecutor
 *  net.minecraftforge.network.NetworkEvent$Context
 */
package mod.gottsch.forge.everfurnace.core.network;

import java.util.function.Supplier;
import mod.gottsch.forge.everfurnace.core.network.CatchupParticleHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

public class CatchupParticlePacket {
    private final BlockPos pos;

    public CatchupParticlePacket(BlockPos pos) {
        this.pos = pos;
    }

    public static void encode(CatchupParticlePacket packet, FriendlyByteBuf buf) {
        buf.m_130064_(packet.pos);
    }

    public static CatchupParticlePacket decode(FriendlyByteBuf buf) {
        return new CatchupParticlePacket(buf.m_130135_());
    }

    public static void handle(CatchupParticlePacket packet, Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();
        ctx.enqueueWork(() -> DistExecutor.unsafeRunWhenOn((Dist)Dist.CLIENT, () -> () -> CatchupParticleHandler.handle(packet.pos)));
        ctx.setPacketHandled(true);
    }
}

