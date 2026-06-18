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

public class SlotIronFurnaceInputFactory
extends Slot {
    BlockIronFurnaceTileBase te;
    public int index;

    public SlotIronFurnaceInputFactory(int index, BlockIronFurnaceTileBase te, int slotIndex, int x, int y) {
        super((Container)te, slotIndex, x, y);
        this.te = te;
        this.index = index;
    }

    public boolean m_5857_(ItemStack stack) {
        return this.te.hasRecipe(stack);
    }

    public boolean m_6659_() {
        if (this.index == 0 || this.index == 5) {
            if (this.te.getTier() > 1) {
                return this.te.isFactory() && this.te.getAugmentGUI() == 0;
            }
            return false;
        }
        if (this.index == 1 || this.index == 4) {
            if (this.te.getTier() > 0) {
                return this.te.isFactory() && this.te.getAugmentGUI() == 0;
            }
            return false;
        }
        return this.te.isFactory() && this.te.getAugmentGUI() == 0;
    }
}

