/*
 * Decompiled with CFR.
 */
package ironfurnaces.container.slots;

import ironfurnaces.tileentity.furnaces.BlockIronFurnaceTileBase;
import net.minecraft.world.Container;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.RecipeType;

public class SlotIronFurnaceFuel
extends Slot {
    BlockIronFurnaceTileBase te;

    public SlotIronFurnaceFuel(BlockIronFurnaceTileBase te, int index, int x, int y) {
        super((Container)te, index, x, y);
        this.te = te;
    }

    public boolean m_5857_(ItemStack stack) {
        return BlockIronFurnaceTileBase.isItemFuel(stack, RecipeType.f_44108_) || SlotIronFurnaceFuel.isBucket(stack);
    }

    public int m_5866_(ItemStack stack) {
        return SlotIronFurnaceFuel.isBucket(stack) ? 1 : super.m_5866_(stack);
    }

    public static boolean isBucket(ItemStack stack) {
        return stack.m_41720_() == Items.f_42446_;
    }

    public boolean m_6659_() {
        return this.te.getAugmentGUI() == 0 && this.te.isFurnace();
    }
}

