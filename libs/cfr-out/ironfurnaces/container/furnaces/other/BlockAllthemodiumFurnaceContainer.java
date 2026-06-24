/*
 * Decompiled with CFR.
 */
package ironfurnaces.container.furnaces.other;

import ironfurnaces.container.furnaces.BlockIronFurnaceContainerBase;
import ironfurnaces.init.Registration;
import ironfurnaces.tileentity.furnaces.other.BlockAllthemodiumFurnaceTile;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.level.Level;

public class BlockAllthemodiumFurnaceContainer
extends BlockIronFurnaceContainerBase {
    public BlockAllthemodiumFurnaceContainer(int windowId, Level world, BlockPos pos, Inventory playerInventory, Player player) {
        super((MenuType)Registration.ALLTHEMODIUM_FURNACE_CONTAINER.get(), windowId, world, pos, playerInventory, player);
        this.te = (BlockAllthemodiumFurnaceTile)world.m_7702_(pos);
    }
}

