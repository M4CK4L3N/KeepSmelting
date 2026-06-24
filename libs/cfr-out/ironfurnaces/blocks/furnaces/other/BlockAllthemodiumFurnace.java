/*
 * Decompiled with CFR.
 */
package ironfurnaces.blocks.furnaces.other;

import ironfurnaces.blocks.furnaces.BlockIronFurnaceBase;
import ironfurnaces.init.Registration;
import ironfurnaces.tileentity.furnaces.BlockIronFurnaceTileBase;
import ironfurnaces.tileentity.furnaces.other.BlockAllthemodiumFurnaceTile;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;

public class BlockAllthemodiumFurnace
extends BlockIronFurnaceBase {
    public static final String ALLTHEMODIUM_FURNACE = "allthemodium_furnace";

    public BlockAllthemodiumFurnace(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Nullable
    public BlockEntity m_142194_(BlockPos p_153215_, BlockState p_153216_) {
        return new BlockAllthemodiumFurnaceTile(p_153215_, p_153216_);
    }

    @Nullable
    public <T extends BlockEntity> BlockEntityTicker<T> m_142354_(Level level, BlockState state, BlockEntityType<T> type) {
        return BlockAllthemodiumFurnace.createFurnaceTicker(level, type, (BlockEntityType<? extends BlockIronFurnaceTileBase>)((BlockEntityType)Registration.ALLTHEMODIUM_FURNACE_TILE.get()));
    }
}

