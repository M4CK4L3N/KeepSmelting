/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.client.Minecraft
 *  net.minecraft.client.multiplayer.ClientLevel
 *  net.minecraft.core.BlockPos
 *  net.minecraft.core.particles.ParticleOptions
 *  net.minecraft.core.particles.ParticleTypes
 *  net.minecraft.sounds.SoundEvents
 *  net.minecraft.sounds.SoundSource
 *  net.minecraft.world.level.Level
 *  net.minecraft.world.level.block.AbstractFurnaceBlock
 *  net.minecraft.world.level.block.state.BlockState
 *  net.minecraft.world.level.block.state.properties.Property
 *  net.minecraftforge.api.distmarker.Dist
 *  net.minecraftforge.api.distmarker.OnlyIn
 */
package mod.gottsch.forge.everfurnace.core.network;

import java.util.Random;
import mod.gottsch.forge.everfurnace.core.config.EverFurnaceConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.AbstractFurnaceBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(value=Dist.CLIENT)
public class CatchupParticleHandler {
    public static void handle(BlockPos pos) {
        Minecraft mc = Minecraft.m_91087_();
        ClientLevel level = mc.f_91073_;
        if (level == null) {
            return;
        }
        CatchupParticleHandler.spawnParticles((Level)level, pos);
        CatchupParticleHandler.playSound((Level)level, pos);
        CatchupParticleHandler.snapLitState((Level)level, pos);
    }

    public static void spawnParticles(Level level, BlockPos pos) {
        double oz;
        double oy;
        double ox;
        int i;
        if (!((Boolean)EverFurnaceConfig.CLIENT.particleBurstEnabled.get()).booleanValue()) {
            return;
        }
        Random rand = new Random();
        double cx = (double)pos.m_123341_() + 0.5;
        double cy = (double)pos.m_123342_() + 0.5;
        double cz = (double)pos.m_123343_() + 0.5;
        for (i = 0; i < 12; ++i) {
            ox = (rand.nextDouble() - 0.5) * 0.8;
            oy = rand.nextDouble() * 0.6;
            oz = (rand.nextDouble() - 0.5) * 0.8;
            level.m_7106_((ParticleOptions)ParticleTypes.f_123744_, cx + ox, cy + oy, cz + oz, 0.0, 0.02, 0.0);
        }
        for (i = 0; i < 8; ++i) {
            ox = (rand.nextDouble() - 0.5) * 0.8;
            oy = rand.nextDouble() * 0.8;
            oz = (rand.nextDouble() - 0.5) * 0.8;
            level.m_7106_((ParticleOptions)ParticleTypes.f_123755_, cx + ox, cy + oy, cz + oz, 0.0, 0.03, 0.0);
        }
    }

    private static void playSound(Level level, BlockPos pos) {
        if (!((Boolean)EverFurnaceConfig.CLIENT.soundCueEnabled.get()).booleanValue()) {
            return;
        }
        level.m_7785_((double)pos.m_123341_() + 0.5, (double)pos.m_123342_() + 0.5, (double)pos.m_123343_() + 0.5, SoundEvents.f_11907_, SoundSource.BLOCKS, 1.0f, 1.0f, false);
    }

    private static void snapLitState(Level level, BlockPos pos) {
        if (!((Boolean)EverFurnaceConfig.CLIENT.lightFlickerEnabled.get()).booleanValue()) {
            return;
        }
        BlockState state = level.m_8055_(pos);
        if (!state.m_61138_((Property)AbstractFurnaceBlock.f_48684_)) {
            return;
        }
        level.m_46597_(pos, state);
    }
}

