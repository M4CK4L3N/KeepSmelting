/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.world.Container
 *  net.minecraft.world.inventory.Slot
 *  net.minecraft.world.item.ItemStack
 */
package ironfurnaces.container.slots;

import ironfurnaces.items.ItemHeater;
import ironfurnaces.tileentity.BlockWirelessEnergyHeaterTile;
import net.minecraft.world.Container;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public class SlotHeater
extends Slot {
    private BlockWirelessEnergyHeaterTile te;

    public SlotHeater(BlockWirelessEnergyHeaterTile te, int slotIndex, int xPosition, int yPosition) {
        super((Container)te, slotIndex, xPosition, yPosition);
    }

    public boolean m_5857_(ItemStack stack) {
        return stack.m_41720_() instanceof ItemHeater;
    }
}

