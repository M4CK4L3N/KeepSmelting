/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  javax.annotation.Nullable
 *  net.minecraft.core.BlockPos
 *  net.minecraft.server.level.ServerPlayer
 *  net.minecraft.world.Container
 *  net.minecraft.world.Containers
 *  net.minecraft.world.InteractionHand
 *  net.minecraft.world.InteractionResult
 *  net.minecraft.world.MenuProvider
 *  net.minecraft.world.entity.LivingEntity
 *  net.minecraft.world.entity.player.Player
 *  net.minecraft.world.item.ItemStack
 *  net.minecraft.world.level.ItemLike
 *  net.minecraft.world.level.Level
 *  net.minecraft.world.level.block.Block
 *  net.minecraft.world.level.block.EntityBlock
 *  net.minecraft.world.level.block.entity.BlockEntity
 *  net.minecraft.world.level.block.entity.BlockEntityTicker
 *  net.minecraft.world.level.block.entity.BlockEntityType
 *  net.minecraft.world.level.block.state.BlockBehaviour$Properties
 *  net.minecraft.world.level.block.state.BlockState
 *  net.minecraft.world.level.material.FluidState
 *  net.minecraft.world.phys.BlockHitResult
 *  net.minecraftforge.common.capabilities.ForgeCapabilities
 *  net.minecraftforge.network.NetworkHooks
 */
package ironfurnaces.blocks;

import ironfurnaces.init.Registration;
import ironfurnaces.tileentity.BlockWirelessEnergyHeaterTile;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.network.NetworkHooks;

public class BlockWirelessEnergyHeater
extends Block
implements EntityBlock {
    public static final String HEATER = "heater";

    public BlockWirelessEnergyHeater(BlockBehaviour.Properties properties) {
        super(properties);
        this.m_49959_(this.m_49966_());
    }

    @Nullable
    public BlockEntity m_142194_(BlockPos p_153215_, BlockState p_153216_) {
        return new BlockWirelessEnergyHeaterTile(p_153215_, p_153216_);
    }

    @Nullable
    public <T extends BlockEntity> BlockEntityTicker<T> m_142354_(Level level, BlockState state, BlockEntityType<T> type) {
        return BlockWirelessEnergyHeater.createTicker(level, type, (BlockEntityType<? extends BlockWirelessEnergyHeaterTile>)((BlockEntityType)Registration.HEATER_TILE.get()));
    }

    @Nullable
    protected static <E extends BlockEntity, A extends BlockEntity> BlockEntityTicker<A> createTickerHelper(BlockEntityType<A> p_152133_, BlockEntityType<E> p_152134_, BlockEntityTicker<? super E> p_152135_) {
        return p_152134_ == p_152133_ ? p_152135_ : null;
    }

    @Nullable
    protected static <T extends BlockEntity> BlockEntityTicker<T> createTicker(Level p_151988_, BlockEntityType<T> p_151989_, BlockEntityType<? extends BlockWirelessEnergyHeaterTile> p_151990_) {
        return p_151988_.f_46443_ ? null : BlockWirelessEnergyHeater.createTickerHelper(p_151989_, p_151990_, BlockWirelessEnergyHeaterTile::tick);
    }

    public boolean onDestroyedByPlayer(BlockState state, Level world, BlockPos pos, Player player, boolean willHarvest, FluidState fluid) {
        if (!world.f_46443_) {
            BlockWirelessEnergyHeaterTile te = (BlockWirelessEnergyHeaterTile)world.m_7702_(pos);
            ItemStack stack = new ItemStack((ItemLike)Registration.HEATER.get());
            if (te.m_8077_()) {
                stack.m_41714_(te.m_5446_());
            }
            if (te.getEnergy() > 0) {
                stack.m_41784_().m_128405_("Energy", te.getEnergy());
            }
            if (!player.m_7500_()) {
                Containers.m_18992_((Level)world, (double)te.m_58899_().m_123341_(), (double)te.m_58899_().m_123342_(), (double)te.m_58899_().m_123343_(), (ItemStack)stack);
            }
        }
        return super.onDestroyedByPlayer(state, world, pos, player, willHarvest, fluid);
    }

    public void m_6402_(Level world, BlockPos pos, BlockState state, @Nullable LivingEntity entity, ItemStack stack) {
        if (entity != null) {
            BlockWirelessEnergyHeaterTile te = (BlockWirelessEnergyHeaterTile)world.m_7702_(pos);
            if (stack.m_41788_()) {
                te.setCustomName(stack.m_41611_());
            }
            if (stack.m_41782_()) {
                te.getCapability(ForgeCapabilities.ENERGY).ifPresent(h -> h.receiveEnergy(stack.m_41783_().m_128451_("Energy"), false));
            }
        }
    }

    public InteractionResult m_6227_(BlockState p_225533_1_, Level world, BlockPos pos, Player player, InteractionHand p_225533_5_, BlockHitResult p_225533_6_) {
        if (!world.f_46443_) {
            this.interactWith(world, pos, player);
        }
        return InteractionResult.SUCCESS;
    }

    private void interactWith(Level world, BlockPos pos, Player player) {
        BlockEntity tileEntity = world.m_7702_(pos);
        if (tileEntity instanceof MenuProvider) {
            NetworkHooks.openScreen((ServerPlayer)((ServerPlayer)player), (MenuProvider)((MenuProvider)tileEntity), (BlockPos)tileEntity.m_58899_());
        }
    }

    public void m_6810_(BlockState state, Level world, BlockPos pos, BlockState oldState, boolean p_196243_5_) {
        if (state.m_60734_() != oldState.m_60734_()) {
            BlockEntity te = world.m_7702_(pos);
            if (te instanceof BlockWirelessEnergyHeaterTile) {
                Containers.m_19002_((Level)world, (BlockPos)pos, (Container)((BlockWirelessEnergyHeaterTile)te));
                world.m_46717_(pos, (Block)this);
            }
            super.m_6810_(state, world, pos, oldState, p_196243_5_);
        }
    }
}

