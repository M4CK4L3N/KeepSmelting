/*
 * Decompiled with CFR.
 */
package ironfurnaces.blocks.furnaces;

import ironfurnaces.blocks.furnaces.BlockIronFurnaceBase;
import ironfurnaces.init.Registration;
import ironfurnaces.tileentity.furnaces.BlockIronFurnaceTileBase;
import ironfurnaces.tileentity.furnaces.BlockNetheriteFurnaceTile;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jetbrains.annotations.NotNull;

public class BlockNetheriteFurnace
extends BlockIronFurnaceBase {
    public static final String NETHERITE_FURNACE = "netherite_furnace";

    public BlockNetheriteFurnace(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Nullable
    public <T extends BlockEntity> BlockEntityTicker<T> m_142354_(Level level, BlockState state, BlockEntityType<T> type) {
        return BlockNetheriteFurnace.createFurnaceTicker(level, type, (BlockEntityType<? extends BlockIronFurnaceTileBase>)((BlockEntityType)Registration.NETHERITE_FURNACE_TILE.get()));
    }

    @Override
    @OnlyIn(value=Dist.CLIENT)
    public void m_214162_(BlockState state, @NotNull Level world, @NotNull BlockPos pos, @NotNull RandomSource rand) {
        if (((Boolean)state.m_61143_((Property)BlockStateProperties.f_61443_)).booleanValue()) {
            if (world.m_7702_(pos) == null) {
                return;
            }
            if (!(world.m_7702_(pos) instanceof BlockIronFurnaceTileBase)) {
                return;
            }
            BlockIronFurnaceTileBase tile = (BlockIronFurnaceTileBase)world.m_7702_(pos);
            if (tile.m_8020_(3).m_41720_() == Registration.SMOKING_AUGMENT.get()) {
                super.m_214162_(state, world, pos, rand);
            } else if (tile.m_8020_(3).m_41720_() == Registration.BLASTING_AUGMENT.get()) {
                super.m_214162_(state, world, pos, rand);
            } else {
                double d0 = (double)pos.m_123341_() + 0.5;
                double d1 = pos.m_123342_();
                double d2 = (double)pos.m_123343_() + 0.5;
                if (rand.m_188500_() < 0.1) {
                    world.m_7785_(d0, d1, d2, SoundEvents.f_11907_, SoundSource.BLOCKS, 1.0f, 1.0f, false);
                }
                Direction direction = (Direction)state.m_61143_((Property)BlockStateProperties.f_61374_);
                Direction.Axis direction$axis = direction.m_122434_();
                double d3 = 0.52;
                double d4 = rand.m_188500_() * 0.6 - 0.3;
                double d5 = direction$axis == Direction.Axis.X ? (double)direction.m_122429_() * 0.52 : d4;
                double d6 = rand.m_188500_() * 6.0 / 16.0;
                double d7 = direction$axis == Direction.Axis.Z ? (double)direction.m_122431_() * 0.52 : d4;
                world.m_7106_((ParticleOptions)ParticleTypes.f_123762_, d0 + d5, d1 + d6, d2 + d7, 0.0, 0.0, 0.0);
                world.m_7106_((ParticleOptions)ParticleTypes.f_123745_, d0 + d5, d1 + d6, d2 + d7, 0.0, 0.0, 0.0);
            }
        }
    }

    @Nullable
    public BlockEntity m_142194_(BlockPos p_153215_, BlockState p_153216_) {
        return new BlockNetheriteFurnaceTile(p_153215_, p_153216_);
    }
}

