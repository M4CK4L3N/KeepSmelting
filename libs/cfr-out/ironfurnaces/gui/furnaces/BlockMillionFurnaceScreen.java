/*
 * Decompiled with CFR.
 */
package ironfurnaces.gui.furnaces;

import ironfurnaces.container.furnaces.BlockMillionFurnaceContainer;
import ironfurnaces.gui.furnaces.BlockIronFurnaceScreenBase;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public class BlockMillionFurnaceScreen
extends BlockIronFurnaceScreenBase<BlockMillionFurnaceContainer> {
    public BlockMillionFurnaceScreen(BlockMillionFurnaceContainer container, Inventory inv, Component name) {
        super(container, inv, name);
    }
}

