/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.core.BlockPos
 *  net.minecraft.world.entity.player.Inventory
 *  net.minecraft.world.entity.player.Player
 *  net.minecraft.world.inventory.AbstractContainerMenu
 *  net.minecraft.world.level.block.entity.BlockEntityType
 *  net.minecraft.world.level.block.state.BlockState
 *  net.minecraftforge.common.ForgeConfigSpec$IntValue
 */
package ironfurnaces.tileentity.furnaces;

import ironfurnaces.Config;
import ironfurnaces.container.furnaces.BlockCopperFurnaceContainer;
import ironfurnaces.init.Registration;
import ironfurnaces.tileentity.furnaces.BlockIronFurnaceTileBase;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.ForgeConfigSpec;

public class BlockCopperFurnaceTile
extends BlockIronFurnaceTileBase {
    public BlockCopperFurnaceTile(BlockPos pos, BlockState state) {
        super((BlockEntityType)Registration.COPPER_FURNACE_TILE.get(), pos, state);
    }

    @Override
    public ForgeConfigSpec.IntValue getCookTimeConfig() {
        return Config.copperFurnaceSpeed;
    }

    @Override
    public String IgetName() {
        return "container.ironfurnaces.copper_furnace";
    }

    @Override
    public AbstractContainerMenu IcreateMenu(int i, Inventory playerInventory, Player playerEntity) {
        return new BlockCopperFurnaceContainer(i, this.f_58857_, this.f_58858_, playerInventory, playerEntity);
    }

    @Override
    public int getTier() {
        return (Integer)Config.copperFurnaceTier.get();
    }
}

