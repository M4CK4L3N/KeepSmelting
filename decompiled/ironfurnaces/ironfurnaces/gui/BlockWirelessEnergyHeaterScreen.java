/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.network.chat.Component
 *  net.minecraft.world.entity.player.Inventory
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

