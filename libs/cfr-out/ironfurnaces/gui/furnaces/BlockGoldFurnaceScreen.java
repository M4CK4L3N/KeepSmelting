/*
 * Decompiled with CFR.
 */
package ironfurnaces.gui.furnaces;

import ironfurnaces.container.furnaces.BlockGoldFurnaceContainer;
import ironfurnaces.gui.furnaces.BlockIronFurnaceScreenBase;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public class BlockGoldFurnaceScreen
extends BlockIronFurnaceScreenBase<BlockGoldFurnaceContainer> {
    public BlockGoldFurnaceScreen(BlockGoldFurnaceContainer container, Inventory inv, Component name) {
        super(container, inv, name);
    }
}

