/*
 * Decompiled with CFR.
 */
package ironfurnaces.gui.furnaces;

import ironfurnaces.container.furnaces.BlockSilverFurnaceContainer;
import ironfurnaces.gui.furnaces.BlockIronFurnaceScreenBase;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public class BlockSilverFurnaceScreen
extends BlockIronFurnaceScreenBase<BlockSilverFurnaceContainer> {
    public BlockSilverFurnaceScreen(BlockSilverFurnaceContainer container, Inventory inv, Component name) {
        super(container, inv, name);
    }
}

