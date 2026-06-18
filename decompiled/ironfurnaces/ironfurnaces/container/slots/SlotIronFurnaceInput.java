/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.world.Container
 *  net.minecraft.world.inventory.Slot
 *  net.minecraft.world.item.ItemStack
 */
package ironfurnaces.container.slots;

import ironfurnaces.tileentity.furnaces.BlockIronFurnaceTileBase;
import net.minecraft.world.Container;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public class SlotIronFurnaceInput
extends Slot {
    private BlockIronFurnaceTileBase te;

    public SlotIronFurnaceInput(BlockIronFurnaceTileBase te, int slotIndex, int xPosition, int yPosition) {
        super((Container)te, slotIndex, xPosition, yPosition);
        this.te = te;
    }

    public boolean m_5857_(ItemStack stack) {
        return this.te.hasRecipe(stack);
    }

    public boolean m_6659_() {
        return this.te.getAugmentGUI() == 0 && this.te.isFurnace();
    }
}

