/*
 * Decompiled with CFR.
 */
package ironfurnaces.container.furnaces;

import ironfurnaces.container.furnaces.BlockIronFurnaceContainerBase;
import ironfurnaces.init.Registration;
import ironfurnaces.tileentity.furnaces.BlockGoldFurnaceTile;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.level.Level;

public class BlockGoldFurnaceContainer
extends BlockIronFurnaceContainerBase {
    public BlockGoldFurnaceContainer(int windowId, Level world, BlockPos pos, Inventory playerInventory, Player player) {
        super((MenuType)Registration.GOLD_FURNACE_CONTAINER.get(), windowId, world, pos, playerInventory, player);
        this.te = (BlockGoldFurnaceTile)world.m_7702_(pos);
    }
}

