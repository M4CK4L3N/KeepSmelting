/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.core.BlockPos
 *  net.minecraft.resources.ResourceLocation
 *  net.minecraft.server.level.ServerLevel
 *  net.minecraft.server.level.ServerPlayer
 *  net.minecraftforge.network.NetworkDirection
 *  net.minecraftforge.network.NetworkRegistry
 *  net.minecraftforge.network.PacketDistributor
 *  net.minecraftforge.network.simple.SimpleChannel
 */
package mod.gottsch.forge.everfurnace.core.network;

import java.util.Optional;
import java.util.function.Predicate;
import mod.gottsch.forge.everfurnace.core.network.CatchupParticlePacket;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

public class ModNetwork {
    private static final String PROTOCOL_VERSION = "1";
    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel((ResourceLocation)new ResourceLocation("everfurnace", "main"), () -> "1", (Predicate)NetworkRegistry.acceptMissingOr("1"::equals), "1"::equals);

    public static void register() {
        CHANNEL.registerMessage(0, CatchupParticlePacket.class, CatchupParticlePacket::encode, CatchupParticlePacket::decode, CatchupParticlePacket::handle, Optional.of(NetworkDirection.PLAY_TO_CLIENT));
    }

    public static void sendCatchupParticles(ServerLevel level, BlockPos pos) {
        CatchupParticlePacket packet = new CatchupParticlePacket(pos);
        double radiusSq = 1024.0;
        double cx = (double)pos.m_123341_() + 0.5;
        double cy = (double)pos.m_123342_() + 0.5;
        double cz = (double)pos.m_123343_() + 0.5;
        for (ServerPlayer player : level.m_6907_()) {
            if (!CHANNEL.isRemotePresent(player.f_8906_.f_9742_) || player.m_20275_(cx, cy, cz) > radiusSq) continue;
            CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), (Object)packet);
        }
    }
}

