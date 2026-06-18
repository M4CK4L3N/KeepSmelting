package com.keepsmelting.internal.ironfurnaces;

import com.keepsmelting.internal.catchup.AbstractCatchupHandler;
import ironfurnaces.tileentity.furnaces.BlockIronFurnaceTileBase;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

public class IronFurnaceCatchupHandler extends AbstractCatchupHandler {

    public static final IronFurnaceCatchupHandler INSTANCE = new IronFurnaceCatchupHandler();

    @Override
    public void applyCatchup(BlockEntity tile, long elapsed, Level level, BlockPos pos) {
        BlockIronFurnaceTileBase ift = (BlockIronFurnaceTileBase) tile;
        // Dispatches to mode-specific handlers
        if (ift.isFurnace()) {
            FurnaceMode.apply(ift, elapsed, level, pos);
        } else if (ift.isFactory()) {
            // Trigger neighbor generators first, then pull RF
            GeneratorMode.processNeighborGenerators(ift, elapsed, level, pos);
            FactoryMode.apply(ift, elapsed, level, pos);
        } else if (ift.isGenerator()) {
            GeneratorMode.apply(ift, elapsed, level, pos);
        }
    }
}
