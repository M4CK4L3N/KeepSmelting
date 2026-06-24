/*
 * Decompiled with CFR.
 */
package ironfurnaces.gui;

import ironfurnaces.container.BlockWirelessEnergyHeaterContainer;
import ironfurnaces.gui.BlockWirelessEnergyHeaterScreenBase;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public class BlockWirelessEnergyHeaterScreen
extends BlockWirelessEnergyHeaterScreenBase<BlockWirelessEnergyHeaterContainer> {
    public BlockWirelessEnergyHeaterScreen(BlockWirelessEnergyHeaterContainer blockWirelessEnergyHeaterContainer, Inventory inv, Component name) {
        super(blockWirelessEnergyHeaterContainer, inv, name);
    }
}

