/*
 * Decompiled with CFR.
 */
package ironfurnaces.blocks.furnaces;

import ironfurnaces.blocks.furnaces.BlockIronFurnaceBase;
import ironfurnaces.init.Registration;
import ironfurnaces.tileentity.furnaces.BlockIronFurnaceTileBase;
import ironfurnaces.tileentity.furnaces.BlockMillionFurnaceTile;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.Property;
import org.jetbrains.annotations.NotNull;

public class BlockMillionFurnace
extends BlockIronFurnaceBase {
    public static final String MILLION_FURNACE = "million_furnace";
    public static final BooleanProperty RAINBOW_GENERATING = BooleanProperty.m_61465_((String)"rainbow");

    public BlockMillionFurnace(BlockBehaviour.Properties properties) {
        super(properties);
    }

    public BlockEntity m_142194_(BlockPos p_153277_, BlockState p_153278_) {
        return new BlockMillionFurnaceTile(p_153277_, p_153278_);
    }

    @Override
    public void m_214162_(BlockState state, @NotNull Level world, @NotNull BlockPos pos, @NotNull RandomSource rand) {
        if (world.m_7702_(pos) != null && world.m_7702_(pos) instanceof BlockMillionFurnaceTile && ((BlockMillionFurnaceTile)world.m_7702_(pos)).m_8020_(5).m_41720_() == Registration.GENERATOR_AUGMENT.get() && ((Boolean)state.m_61143_((Property)RAINBOW_GENERATING)).booleanValue()) {
            for (Direction direction : Direction.values()) {
                if (Direction.m_122376_((int)direction.m_122411_()) == Direction.UP || Direction.m_122376_((int)direction.m_122411_()) == Direction.DOWN) continue;
                double d0 = (double)pos.m_123341_() + 0.5;
                double d1 = pos.m_123342_();
                double d2 = (double)pos.m_123343_() + 0.5;
                Direction.Axis direction$axis = direction.m_122434_();
                double d3 = 0.52;
                double d4 = rand.m_188500_() * 0.6 - 0.3;
                double d5 = direction$axis == Direction.Axis.X ? (double)direction.m_122429_() * 0.52 : d4;
                double d6 = rand.m_188500_() * 6.0 / 16.0;
                double d7 = direction$axis == Direction.Axis.Z ? (double)direction.m_122431_() * 0.52 : d4;
                for (int i = 0; i < 10; ++i) {
                    world.m_7106_((ParticleOptions)ParticleTypes.f_123797_, d0 + d5, d1 + d6, d2 + d7, rand.m_188583_() * 0.05, 0.0, rand.m_188583_() * 0.05);
                    world.m_7106_((ParticleOptions)ParticleTypes.f_123770_, d0 + d5, d1 + d6, d2 + d7, rand.m_188583_() * 0.05, 0.0, rand.m_188583_() * 0.05);
                }
            }
        }
        super.m_214162_(state, world, pos, rand);
    }

    @Nullable
    public <T extends BlockEntity> BlockEntityTicker<T> m_142354_(Level level, BlockState state, BlockEntityType<T> type) {
        return BlockMillionFurnace.createFurnaceTicker(level, type, (BlockEntityType<? extends BlockIronFurnaceTileBase>)((BlockEntityType)Registration.MILLION_FURNACE_TILE.get()));
    }

    @Override
    protected void m_7926_(StateDefinition.Builder<Block, BlockState> builder) {
        builder.m_61104_(new Property[]{BlockStateProperties.f_61374_, BlockStateProperties.f_61443_, TYPE, JOVIAL, RAINBOW_GENERATING});
    }
}

