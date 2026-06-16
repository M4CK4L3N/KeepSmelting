/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  javax.annotation.Nullable
 *  net.minecraft.core.BlockPos
 *  net.minecraft.core.Direction
 *  net.minecraft.core.Direction$Axis
 *  net.minecraft.core.particles.ParticleOptions
 *  net.minecraft.core.particles.ParticleTypes
 *  net.minecraft.network.chat.Component
 *  net.minecraft.server.level.ServerLevel
 *  net.minecraft.server.level.ServerPlayer
 *  net.minecraft.sounds.SoundEvents
 *  net.minecraft.sounds.SoundSource
 *  net.minecraft.stats.Stats
 *  net.minecraft.util.Mth
 *  net.minecraft.util.RandomSource
 *  net.minecraft.world.Container
 *  net.minecraft.world.Containers
 *  net.minecraft.world.InteractionHand
 *  net.minecraft.world.InteractionResult
 *  net.minecraft.world.MenuProvider
 *  net.minecraft.world.WorldlyContainer
 *  net.minecraft.world.entity.Entity
 *  net.minecraft.world.entity.LivingEntity
 *  net.minecraft.world.entity.item.ItemEntity
 *  net.minecraft.world.entity.player.Player
 *  net.minecraft.world.item.ItemStack
 *  net.minecraft.world.item.context.BlockPlaceContext
 *  net.minecraft.world.level.BlockGetter
 *  net.minecraft.world.level.ItemLike
 *  net.minecraft.world.level.Level
 *  net.minecraft.world.level.block.Block
 *  net.minecraft.world.level.block.EntityBlock
 *  net.minecraft.world.level.block.Mirror
 *  net.minecraft.world.level.block.RenderShape
 *  net.minecraft.world.level.block.Rotation
 *  net.minecraft.world.level.block.entity.BlockEntity
 *  net.minecraft.world.level.block.entity.BlockEntityTicker
 *  net.minecraft.world.level.block.entity.BlockEntityType
 *  net.minecraft.world.level.block.state.BlockBehaviour$Properties
 *  net.minecraft.world.level.block.state.BlockState
 *  net.minecraft.world.level.block.state.StateDefinition$Builder
 *  net.minecraft.world.level.block.state.properties.BlockStateProperties
 *  net.minecraft.world.level.block.state.properties.IntegerProperty
 *  net.minecraft.world.level.block.state.properties.Property
 *  net.minecraft.world.phys.BlockHitResult
 *  net.minecraft.world.phys.Vec3
 *  net.minecraftforge.api.distmarker.Dist
 *  net.minecraftforge.api.distmarker.OnlyIn
 *  net.minecraftforge.network.NetworkHooks
 *  org.jetbrains.annotations.NotNull
 */
package ironfurnaces.blocks.furnaces;

import ironfurnaces.Config;
import ironfurnaces.capability.CapabilityPlayerFurnacesList;
import ironfurnaces.init.Registration;
import ironfurnaces.items.ItemFurnaceCopy;
import ironfurnaces.items.ItemSpooky;
import ironfurnaces.items.ItemXmas;
import ironfurnaces.items.augments.ItemAugment;
import ironfurnaces.items.augments.ItemAugmentGreen;
import ironfurnaces.items.augments.ItemAugmentRed;
import ironfurnaces.tileentity.furnaces.BlockIronFurnaceTileBase;
import ironfurnaces.tileentity.furnaces.BlockMillionFurnaceTile;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Container;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.network.NetworkHooks;
import org.jetbrains.annotations.NotNull;

public abstract class BlockIronFurnaceBase
extends Block
implements EntityBlock {
    public static final IntegerProperty TYPE = IntegerProperty.m_61631_((String)"type", (int)0, (int)2);
    public static final IntegerProperty JOVIAL = IntegerProperty.m_61631_((String)"jovial", (int)0, (int)2);

    public BlockIronFurnaceBase(BlockBehaviour.Properties properties) {
        super(properties.m_155954_(3.0f));
        this.m_49959_((BlockState)((BlockState)((BlockState)this.m_49966_().m_61124_((Property)BlockStateProperties.f_61443_, (Comparable)Boolean.valueOf(false))).m_61124_((Property)TYPE, (Comparable)Integer.valueOf(0))).m_61124_((Property)JOVIAL, (Comparable)Integer.valueOf(0)));
    }

    public MenuProvider m_7246_(@NotNull BlockState p_49234_, Level p_49235_, @NotNull BlockPos p_49236_) {
        BlockEntity blockentity = p_49235_.m_7702_(p_49236_);
        return blockentity instanceof MenuProvider ? (MenuProvider)blockentity : null;
    }

    public int getLightEmission(BlockState state, BlockGetter world, BlockPos pos) {
        if (((Boolean)Config.disableLightupdates.get()).booleanValue()) {
            return 0;
        }
        return (Boolean)state.m_61143_((Property)BlockStateProperties.f_61443_) != false ? 14 : 0;
    }

    public BlockState m_5573_(BlockPlaceContext ctx) {
        return (BlockState)this.m_49966_().m_61124_((Property)BlockStateProperties.f_61374_, (Comparable)ctx.m_8125_().m_122424_());
    }

    public void m_6402_(@NotNull Level world, @NotNull BlockPos pos, @NotNull BlockState p_180633_3_, @Nullable LivingEntity entity, @NotNull ItemStack stack) {
        if (entity != null) {
            BlockIronFurnaceTileBase te = (BlockIronFurnaceTileBase)world.m_7702_(pos);
            if (stack.m_41788_() && !stack.m_41611_().getString().contains("[")) {
                Objects.requireNonNull(te).setCustomName(stack.m_41611_());
            }
            Objects.requireNonNull(te).totalCookTime = (Integer)te.getCookTimeConfig().get();
            te.placeConfig();
            if (entity instanceof Player) {
                Player player = (Player)entity;
                player.getCapability(CapabilityPlayerFurnacesList.FURNACES_LIST).ifPresent(h -> h.add(pos));
                if (te instanceof BlockMillionFurnaceTile) {
                    te.owner = player.m_20148_();
                }
            }
        }
    }

    @NotNull
    public InteractionResult m_6227_(@NotNull BlockState state, Level world, @NotNull BlockPos pos, Player player, @NotNull InteractionHand handIn, @NotNull BlockHitResult p_225533_6_) {
        ItemStack stack = player.m_21120_(handIn).m_41777_();
        if (world.f_46443_) {
            return InteractionResult.SUCCESS;
        }
        if (player.m_21120_(handIn).m_41720_() instanceof ItemAugment && !player.m_6047_()) {
            return this.interactAugment(world, pos, player, handIn, stack);
        }
        if (player.m_21120_(handIn).m_41720_() instanceof ItemSpooky && !player.m_6047_()) {
            return this.interactJovial(world, pos, player, handIn, 1);
        }
        if (player.m_21120_(handIn).m_41720_() instanceof ItemXmas && !player.m_6047_()) {
            return this.interactJovial(world, pos, player, handIn, 2);
        }
        if (player.m_21120_(handIn).m_41619_() && player.m_6047_()) {
            return this.interactJovial(world, pos, player, handIn, 0);
        }
        if (player.m_21120_(handIn).m_41720_() instanceof ItemFurnaceCopy && !player.m_6047_()) {
            return this.interactCopy(world, pos, player);
        }
        this.interactWith(world, pos, player);
        return InteractionResult.SUCCESS;
    }

    private InteractionResult interactCopy(Level world, BlockPos pos, Player player) {
        int j = player.m_150109_().f_35977_;
        ItemStack stack = player.m_150109_().m_8020_(j);
        if (!(stack.m_41720_() instanceof ItemFurnaceCopy)) {
            return InteractionResult.SUCCESS;
        }
        BlockEntity te = world.m_7702_(pos);
        if (!(te instanceof BlockIronFurnaceTileBase)) {
            return InteractionResult.SUCCESS;
        }
        int[] settings = new int[((BlockIronFurnaceTileBase)te).furnaceSettings.size()];
        for (int i = 0; i < ((BlockIronFurnaceTileBase)te).furnaceSettings.size(); ++i) {
            settings[i] = ((BlockIronFurnaceTileBase)te).furnaceSettings.get(i);
        }
        stack.m_41784_().m_128385_("settings", settings);
        ((BlockIronFurnaceTileBase)te).onUpdateSent();
        player.m_213846_((Component)Component.m_237113_((String)"Settings copied"));
        return InteractionResult.SUCCESS;
    }

    private InteractionResult interactAugment(Level world, BlockPos pos, Player player, InteractionHand handIn, ItemStack stack) {
        int slot;
        if (!(player.m_21120_(handIn).m_41720_() instanceof ItemAugment)) {
            return InteractionResult.SUCCESS;
        }
        BlockEntity te = world.m_7702_(pos);
        if (!(te instanceof BlockIronFurnaceTileBase)) {
            return InteractionResult.SUCCESS;
        }
        int n = player.m_21120_(handIn).m_41720_() instanceof ItemAugmentRed ? 3 : (slot = player.m_21120_(handIn).m_41720_() instanceof ItemAugmentGreen ? 4 : 5);
        if (!((WorldlyContainer)te).m_8020_(slot).m_41619_() && !player.m_7500_()) {
            world.m_7967_((Entity)new ItemEntity(world, (double)pos.m_123341_(), (double)(pos.m_123342_() + 1), (double)pos.m_123343_(), ((WorldlyContainer)te).m_8020_(slot)));
        }
        ItemStack newStack = new ItemStack((ItemLike)stack.m_41720_(), 1);
        newStack.m_41751_(stack.m_41783_());
        ((WorldlyContainer)te).m_6836_(slot, newStack);
        world.m_5594_(null, te.m_58899_(), SoundEvents.f_11671_, SoundSource.BLOCKS, 0.05f, 1.0f);
        if (!player.m_7500_()) {
            player.m_21120_(handIn).m_41774_(1);
        }
        ((BlockIronFurnaceTileBase)te).onUpdateSent();
        Objects.requireNonNull(te.m_58904_()).markAndNotifyBlock(pos, player.m_9236_().m_46745_(pos), te.m_58904_().m_8055_(pos).m_60734_().m_49966_(), te.m_58904_().m_8055_(pos), 2, 0);
        return InteractionResult.SUCCESS;
    }

    private InteractionResult interactJovial(Level world, BlockPos pos, Player player, InteractionHand handIn, int jovial) {
        if (!(player.m_21120_(handIn).m_41720_() instanceof ItemSpooky) && player.m_21120_(handIn).m_41720_() instanceof ItemXmas && player.m_21120_(handIn).m_41619_()) {
            return InteractionResult.SUCCESS;
        }
        BlockEntity te = world.m_7702_(pos);
        if (!(te instanceof BlockIronFurnaceTileBase)) {
            return InteractionResult.SUCCESS;
        }
        ((BlockIronFurnaceTileBase)te).setJovial(jovial);
        return InteractionResult.SUCCESS;
    }

    private void interactWith(Level world, BlockPos pos, Player player) {
        BlockEntity tileEntity;
        if (!world.f_46443_ && (tileEntity = world.m_7702_(pos)) instanceof MenuProvider) {
            NetworkHooks.openScreen((ServerPlayer)((ServerPlayer)player), (MenuProvider)((MenuProvider)tileEntity), (BlockPos)tileEntity.m_58899_());
            player.m_36220_(Stats.f_12966_);
            if (tileEntity instanceof BlockIronFurnaceTileBase) {
                ((BlockIronFurnaceTileBase)tileEntity).furnaceSettings.set(10, 0);
            }
        }
    }

    @OnlyIn(value=Dist.CLIENT)
    public void m_214162_(BlockState state, @NotNull Level world, @NotNull BlockPos pos, @NotNull RandomSource rand) {
        if (((Boolean)state.m_61143_((Property)BlockStateProperties.f_61443_)).booleanValue()) {
            if (world.m_7702_(pos) == null) {
                return;
            }
            BlockEntity blockEntity = world.m_7702_(pos);
            if (!(blockEntity instanceof BlockIronFurnaceTileBase)) {
                return;
            }
            BlockIronFurnaceTileBase tile = (BlockIronFurnaceTileBase)blockEntity;
            if (Objects.requireNonNull(tile).m_8020_(3).m_41720_() == Registration.SMOKING_AUGMENT.get()) {
                double lvt_5_1_ = (double)pos.m_123341_() + 0.5;
                double lvt_7_1_ = pos.m_123342_();
                double lvt_9_1_ = (double)pos.m_123343_() + 0.5;
                if (rand.m_188500_() < 0.1) {
                    world.m_7785_(lvt_5_1_, lvt_7_1_, lvt_9_1_, SoundEvents.f_12472_, SoundSource.BLOCKS, 1.0f, 1.0f, false);
                }
                world.m_7106_((ParticleOptions)ParticleTypes.f_123762_, lvt_5_1_, lvt_7_1_ + 1.1, lvt_9_1_, 0.0, 0.0, 0.0);
            } else if (tile.m_8020_(3).m_41720_() == Registration.BLASTING_AUGMENT.get()) {
                double lvt_5_1_ = (double)pos.m_123341_() + 0.5;
                double lvt_7_1_ = pos.m_123342_();
                double lvt_9_1_ = (double)pos.m_123343_() + 0.5;
                if (rand.m_188500_() < 0.1) {
                    world.m_7785_(lvt_5_1_, lvt_7_1_, lvt_9_1_, SoundEvents.f_11715_, SoundSource.BLOCKS, 1.0f, 1.0f, false);
                }
                Direction lvt_11_1_ = (Direction)state.m_61143_((Property)BlockStateProperties.f_61374_);
                Direction.Axis lvt_12_1_ = lvt_11_1_.m_122434_();
                double lvt_13_1_ = 0.52;
                double lvt_15_1_ = rand.m_188500_() * 0.6 - 0.3;
                double lvt_17_1_ = lvt_12_1_ == Direction.Axis.X ? (double)lvt_11_1_.m_122429_() * 0.52 : lvt_15_1_;
                double lvt_19_1_ = rand.m_188500_() * 9.0 / 16.0;
                double lvt_21_1_ = lvt_12_1_ == Direction.Axis.Z ? (double)lvt_11_1_.m_122431_() * 0.52 : lvt_15_1_;
                world.m_7106_((ParticleOptions)ParticleTypes.f_123762_, lvt_5_1_ + lvt_17_1_, lvt_7_1_ + lvt_19_1_, lvt_9_1_ + lvt_21_1_, 0.0, 0.0, 0.0);
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
                world.m_7106_((ParticleOptions)ParticleTypes.f_123744_, d0 + d5, d1 + d6, d2 + d7, 0.0, 0.0, 0.0);
            }
        }
    }

    public void m_6810_(BlockState state, @NotNull Level world, @NotNull BlockPos pos, BlockState oldState, boolean p_196243_5_) {
        if (state.m_60734_() != oldState.m_60734_()) {
            BlockEntity te = world.m_7702_(pos);
            if (te instanceof BlockIronFurnaceTileBase) {
                BlockIronFurnaceTileBase furnace = (BlockIronFurnaceTileBase)te;
                if (furnace.owner != null && world.m_46003_(furnace.owner) != null) {
                    Objects.requireNonNull(world.m_46003_(furnace.owner)).getCapability(CapabilityPlayerFurnacesList.FURNACES_LIST).ifPresent(h -> h.remove(te.m_58899_()));
                }
                Containers.m_19002_((Level)world, (BlockPos)pos, (Container)furnace);
                furnace.grantStoredRecipeExperience((ServerLevel)world, new Vec3((double)pos.m_123341_(), (double)pos.m_123342_(), (double)pos.m_123343_()));
                world.m_46717_(pos, (Block)this);
            }
            super.m_6810_(state, world, pos, oldState, p_196243_5_);
        }
    }

    public int getComparatorInputOverride(BlockState state, Level world, BlockPos pos) {
        ArrayList<Integer> slots = new ArrayList<Integer>();
        if (world.m_7702_(pos) instanceof BlockIronFurnaceTileBase) {
            int tier;
            BlockIronFurnaceTileBase te = (BlockIronFurnaceTileBase)world.m_7702_(pos);
            if (Objects.requireNonNull(te).isFurnace()) {
                slots.add(0);
            }
            slots.add(1);
            slots.add(2);
            if (te.isGenerator()) {
                slots.add(6);
            }
            if (te.isFactory() && (tier = te.getTier()) >= 0) {
                slots.add(9);
                slots.add(10);
                slots.add(15);
                slots.add(16);
                if (tier >= 1) {
                    slots.add(8);
                    slots.add(11);
                    slots.add(14);
                    slots.add(17);
                    if (tier >= 2) {
                        slots.add(7);
                        slots.add(12);
                        slots.add(13);
                        slots.add(18);
                    }
                }
            }
            return BlockIronFurnaceBase.getRedstoneSignalFromContainer((Container)((WorldlyContainer)world.m_7702_(pos)), slots);
        }
        return 0;
    }

    public static int getRedstoneSignalFromContainer(@Nullable Container container, List<Integer> slots) {
        if (container == null) {
            return 0;
        }
        int i = 0;
        float f = 0.0f;
        for (int slot : slots) {
            ItemStack itemstack = container.m_8020_(slot);
            if (itemstack.m_41619_()) continue;
            f += (float)itemstack.m_41613_() / (float)Math.min(container.m_6893_(), itemstack.m_41741_());
            ++i;
        }
        return Mth.m_14143_((float)((f /= (float)slots.size()) * 14.0f)) + (i > 0 ? 1 : 0);
    }

    @NotNull
    public RenderShape m_7514_(@NotNull BlockState p_60550_) {
        return RenderShape.MODEL;
    }

    @NotNull
    public BlockState m_6843_(BlockState p_185499_1_, Rotation p_185499_2_) {
        return (BlockState)p_185499_1_.m_61124_((Property)BlockStateProperties.f_61374_, (Comparable)p_185499_2_.m_55954_((Direction)p_185499_1_.m_61143_((Property)BlockStateProperties.f_61374_)));
    }

    @NotNull
    public BlockState m_6943_(BlockState p_185471_1_, Mirror p_185471_2_) {
        return p_185471_1_.m_60717_(p_185471_2_.m_54846_((Direction)p_185471_1_.m_61143_((Property)BlockStateProperties.f_61374_)));
    }

    private int calculateOutput(Level worldIn, BlockPos pos, BlockState state) {
        BlockIronFurnaceTileBase tile = (BlockIronFurnaceTileBase)worldIn.m_7702_(pos);
        int i = this.getComparatorInputOverride(state, worldIn, pos);
        if (tile != null) {
            int j = tile.furnaceSettings.get(9);
            return tile.furnaceSettings.get(8) == 4 ? Math.max(i - j, 0) : i;
        }
        return 0;
    }

    public boolean m_7899_(@NotNull BlockState p_149744_1_) {
        return true;
    }

    public int m_6378_(@NotNull BlockState p_180656_1_, @NotNull BlockGetter p_180656_2_, @NotNull BlockPos p_180656_3_, @NotNull Direction p_180656_4_) {
        return this.m_6376_(p_180656_1_, p_180656_2_, p_180656_3_, p_180656_4_);
    }

    public int m_6376_(@NotNull BlockState blockState, BlockGetter world, @NotNull BlockPos pos, @NotNull Direction direction) {
        BlockIronFurnaceTileBase furnace = (BlockIronFurnaceTileBase)world.m_7702_(pos);
        if (furnace != null) {
            int mode = furnace.furnaceSettings.get(8);
            if (mode == 0) {
                return 0;
            }
            if (mode == 1) {
                return 0;
            }
            if (mode == 2) {
                return 0;
            }
            return this.calculateOutput(Objects.requireNonNull(furnace.m_58904_()), pos, blockState);
        }
        return 0;
    }

    protected void m_7926_(StateDefinition.Builder<Block, BlockState> builder) {
        builder.m_61104_(new Property[]{BlockStateProperties.f_61374_, BlockStateProperties.f_61443_, TYPE, JOVIAL});
    }

    @Nullable
    protected static <E extends BlockEntity, A extends BlockEntity> BlockEntityTicker<A> createTickerHelper(BlockEntityType<A> p_152133_, BlockEntityType<E> p_152134_, BlockEntityTicker<? super E> p_152135_) {
        return p_152134_ == p_152133_ ? p_152135_ : null;
    }

    @Nullable
    protected static <T extends BlockEntity> BlockEntityTicker<T> createFurnaceTicker(Level p_151988_, BlockEntityType<T> p_151989_, BlockEntityType<? extends BlockIronFurnaceTileBase> p_151990_) {
        return p_151988_.f_46443_ ? null : BlockIronFurnaceBase.createTickerHelper(p_151989_, p_151990_, BlockIronFurnaceTileBase::tick);
    }
}
