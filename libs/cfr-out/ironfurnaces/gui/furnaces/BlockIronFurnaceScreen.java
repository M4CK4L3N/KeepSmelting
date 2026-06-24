/*
 * Decompiled with CFR.
 */
package ironfurnaces.gui.furnaces;

import ironfurnaces.container.furnaces.BlockIronFurnaceContainer;
import ironfurnaces.gui.furnaces.BlockIronFurnaceScreenBase;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public class BlockIronFurnaceScreen
extends BlockIronFurnaceScreenBase<BlockIronFurnaceContainer> {
    public BlockIronFurnaceScreen(BlockIronFurnaceContainer container, Inventory inv, Component name) {
        super(container, inv, name);
    }
}

