/*
 * Decompiled with CFR.
 */
package ironfurnaces.gui.furnaces;

import ironfurnaces.container.furnaces.BlockNetheriteFurnaceContainer;
import ironfurnaces.gui.furnaces.BlockIronFurnaceScreenBase;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public class BlockNetheriteFurnaceScreen
extends BlockIronFurnaceScreenBase<BlockNetheriteFurnaceContainer> {
    public BlockNetheriteFurnaceScreen(BlockNetheriteFurnaceContainer container, Inventory inv, Component name) {
        super(container, inv, name);
        this.GUI = GUI_NETHERITE;
    }
}

