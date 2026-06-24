/*
 * Decompiled with CFR.
 */
package ironfurnaces.blocks.furnaces;

import ironfurnaces.blocks.furnaces.BlockIronFurnaceBase;
import ironfurnaces.init.Registration;
import ironfurnaces.tileentity.furnaces.BlockCrystalFurnaceTile;
import ironfurnaces.tileentity.furnaces.BlockIronFurnaceTileBase;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SimpleWaterloggedBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jetbrains.annotations.NotNull;

public class BlockCrystalFurnace
extends BlockIronFurnaceBase
implements SimpleWaterloggedBlock {
    public static final String CRYSTAL_FURNACE = "crystal_furnace";
    public static final BooleanProperty WATERLOGGED = BlockStateProperties.f_61362_;

    public BlockCrystalFurnace(BlockBehaviour.Properties properties) {
        super(properties);
        this.m_49959_((BlockState)((BlockState)((BlockState)((BlockState)this.m_49966_().m_61124_((Property)BlockStateProperties.f_61443_, (Comparable)Boolean.valueOf(false))).m_61124_((Property)TYPE, (Comparable)Integer.valueOf(0))).m_61124_((Property)JOVIAL, (Comparable)Integer.valueOf(0))).m_61124_((Property)WATERLOGGED, (Comparable)Boolean.FALSE));
    }

    @Nullable
    public <T extends BlockEntity> BlockEntityTicker<T> m_142354_(@NotNull Level level, @NotNull BlockState state, @NotNull BlockEntityType<T> type) {
        return BlockCrystalFurnace.createFurnaceTicker(level, type, (BlockEntityType<? extends BlockIronFurnaceTileBase>)((BlockEntityType)Registration.CRYSTAL_FURNACE_TILE.get()));
    }

    @Override
    public BlockState m_5573_(BlockPlaceContext ctx) {
        FluidState fluidState = ctx.m_43725_().m_6425_(ctx.m_8083_());
        return (BlockState)((BlockState)this.m_49966_().m_61124_((Property)BlockStateProperties.f_61374_, (Comparable)ctx.m_8125_().m_122424_())).m_61124_((Property)WATERLOGGED, (Comparable)Boolean.valueOf(fluidState.m_76152_() == Fluids.f_76193_));
    }

    @Override
    @OnlyIn(value=Dist.CLIENT)
    public void m_214162_(BlockState state, @NotNull Level world, @NotNull BlockPos pos, @NotNull RandomSource rand) {
        double d0 = (double)pos.m_123341_() + 0.5;
        double d1 = pos.m_123342_();
        double d2 = (double)pos.m_123343_() + 0.5;
        Direction direction = (Direction)state.m_61143_((Property)BlockStateProperties.f_61374_);
        Direction.Axis direction$axis = direction.m_122434_();
        double d3 = 0.52;
        double d4 = rand.m_188500_() * 0.6 - 0.3;
        double d5 = direction$axis == Direction.Axis.X ? (double)direction.m_122429_() * 0.52 : d4;
        double d6 = rand.m_188500_() * 6.0 / 16.0;
        double d7 = direction$axis == Direction.Axis.Z ? (double)direction.m_122431_() * 0.52 : d4;
        world.m_7106_((ParticleOptions)ParticleTypes.f_123760_, d0 + d5, d1 + d6 - 0.5, d2 + d7, 0.0, 0.0, 0.0);
        world.m_7106_((ParticleOptions)ParticleTypes.f_123760_, d0 + d5, d1 + d6 - 0.5, d2 + d7, 0.0, 0.0, 0.0);
        if (world.m_7702_(pos) == null) {
            return;
        }
        BlockEntity blockEntity = world.m_7702_(pos);
        if (!(blockEntity instanceof BlockIronFurnaceTileBase)) {
            return;
        }
        BlockIronFurnaceTileBase tile = (BlockIronFurnaceTileBase)blockEntity;
        if (tile.m_8020_(3).m_41720_() == Registration.SMOKING_AUGMENT.get()) {
            double lvt_5_1_ = (double)pos.m_123341_() + 0.5;
            double lvt_7_1_ = pos.m_123342_();
            double lvt_9_1_ = (double)pos.m_123343_() + 0.5;
            world.m_7106_((ParticleOptions)ParticleTypes.f_123760_, lvt_5_1_, lvt_7_1_ + 1.1, lvt_9_1_, 0.0, 0.0, 0.0);
        }
        super.m_214162_(state, world, pos, rand);
    }

    @NotNull
    public FluidState m_5888_(BlockState state) {
        return (Boolean)state.m_61143_((Property)WATERLOGGED) != false ? Fluids.f_76193_.m_76068_(false) : super.m_5888_(state);
    }

    @NotNull
    public BlockState m_7417_(BlockState stateIn, @NotNull Direction facing, @NotNull BlockState facingState, @NotNull LevelAccessor worldIn, @NotNull BlockPos currentPos, @NotNull BlockPos facingPos) {
        if (((Boolean)stateIn.m_61143_((Property)WATERLOGGED)).booleanValue()) {
            worldIn.m_186469_(currentPos, (Fluid)Fluids.f_76193_, Fluids.f_76193_.m_6718_((LevelReader)worldIn));
        }
        return super.m_7417_(stateIn, facing, facingState, worldIn, currentPos, facingPos);
    }

    @Override
    protected void m_7926_(StateDefinition.Builder<Block, BlockState> builder) {
        super.m_7926_((StateDefinition.Builder<Block, BlockState>)builder.m_61104_(new Property[]{WATERLOGGED}));
    }

    @Nullable
    public BlockEntity m_142194_(@NotNull BlockPos p_153215_, @NotNull BlockState p_153216_) {
        return new BlockCrystalFurnaceTile(p_153215_, p_153216_);
    }
}

