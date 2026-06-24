/*
 * Decompiled with CFR.
 */
package ironfurnaces.container.furnaces;

import ironfurnaces.container.furnaces.BlockIronFurnaceContainerBase;
import ironfurnaces.init.Registration;
import ironfurnaces.tileentity.furnaces.BlockDiamondFurnaceTile;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.level.Level;

public class BlockDiamondFurnaceContainer
extends BlockIronFurnaceContainerBase {
    public BlockDiamondFurnaceContainer(int windowId, Level world, BlockPos pos, Inventory playerInventory, Player player) {
        super((MenuType)Registration.DIAMOND_FURNACE_CONTAINER.get(), windowId, world, pos, playerInventory, player);
        this.te = (BlockDiamondFurnaceTile)world.m_7702_(pos);
    }
}

