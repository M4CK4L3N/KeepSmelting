/*
 * Decompiled with CFR.
 */
package ironfurnaces.blocks.furnaces;

import ironfurnaces.blocks.furnaces.BlockIronFurnaceBase;
import ironfurnaces.init.Registration;
import ironfurnaces.tileentity.furnaces.BlockCopperFurnaceTile;
import ironfurnaces.tileentity.furnaces.BlockIronFurnaceTileBase;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;

public class BlockCopperFurnace
extends BlockIronFurnaceBase {
    public static final String COPPER_FURNACE = "copper_furnace";

    public BlockCopperFurnace(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Nullable
    public BlockEntity m_142194_(BlockPos p_153215_, BlockState p_153216_) {
        return new BlockCopperFurnaceTile(p_153215_, p_153216_);
    }

    @Nullable
    public <T extends BlockEntity> BlockEntityTicker<T> m_142354_(Level level, BlockState state, BlockEntityType<T> type) {
        return BlockCopperFurnace.createFurnaceTicker(level, type, (BlockEntityType<? extends BlockIronFurnaceTileBase>)((BlockEntityType)Registration.COPPER_FURNACE_TILE.get()));
    }
}

