/*
 * Decompiled with CFR.
 */
package ironfurnaces.tileentity.furnaces;

import ironfurnaces.Config;
import ironfurnaces.container.furnaces.BlockObsidianFurnaceContainer;
import ironfurnaces.init.Registration;
import ironfurnaces.tileentity.furnaces.BlockIronFurnaceTileBase;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.ForgeConfigSpec;

public class BlockObsidianFurnaceTile
extends BlockIronFurnaceTileBase {
    public BlockObsidianFurnaceTile(BlockPos pos, BlockState state) {
        super((BlockEntityType)Registration.OBSIDIAN_FURNACE_TILE.get(), pos, state);
    }

    @Override
    public ForgeConfigSpec.IntValue getCookTimeConfig() {
        return Config.obsidianFurnaceSpeed;
    }

    @Override
    public String IgetName() {
        return "container.ironfurnaces.obsidian_furnace";
    }

    @Override
    public AbstractContainerMenu IcreateMenu(int i, Inventory playerInventory, Player playerEntity) {
        return new BlockObsidianFurnaceContainer(i, this.f_58857_, this.f_58858_, playerInventory, playerEntity);
    }

    @Override
    public int getTier() {
        return (Integer)Config.obsidianFurnaceTier.get();
    }
}

