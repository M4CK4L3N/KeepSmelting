/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.network.chat.Component
 *  net.minecraft.world.entity.player.Inventory
 */
package ironfurnaces.gui.furnaces;

import ironfurnaces.container.furnaces.BlockCopperFurnaceContainer;
import ironfurnaces.gui.furnaces.BlockIronFurnaceScreenBase;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public class BlockCopperFurnaceScreen
extends BlockIronFurnaceScreenBase<BlockCopperFurnaceContainer> {
    public BlockCopperFurnaceScreen(BlockCopperFurnaceContainer container, Inventory inv, Component name) {
        super(container, inv, name);
    }
}

