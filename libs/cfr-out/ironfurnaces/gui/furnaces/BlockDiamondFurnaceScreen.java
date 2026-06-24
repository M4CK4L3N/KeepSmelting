/*
 * Decompiled with CFR.
 */
package ironfurnaces.gui.furnaces;

import ironfurnaces.container.furnaces.BlockDiamondFurnaceContainer;
import ironfurnaces.gui.furnaces.BlockIronFurnaceScreenBase;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public class BlockDiamondFurnaceScreen
extends BlockIronFurnaceScreenBase<BlockDiamondFurnaceContainer> {
    public BlockDiamondFurnaceScreen(BlockDiamondFurnaceContainer container, Inventory inv, Component name) {
        super(container, inv, name);
    }
}

