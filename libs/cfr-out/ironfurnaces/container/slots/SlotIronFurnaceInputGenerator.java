/*
 * Decompiled with CFR.
 */
package ironfurnaces.container.slots;

import ironfurnaces.items.ItemHeater;
import ironfurnaces.items.augments.ItemAugmentBlasting;
import ironfurnaces.items.augments.ItemAugmentSmoking;
import ironfurnaces.tileentity.furnaces.BlockIronFurnaceTileBase;
import net.minecraft.world.Container;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeType;

public class SlotIronFurnaceInputGenerator
extends Slot {
    BlockIronFurnaceTileBase te;

    public SlotIronFurnaceInputGenerator(BlockIronFurnaceTileBase te, int index, int x, int y) {
        super((Container)te, index, x, y);
        this.te = te;
    }

    public boolean m_5857_(ItemStack stack) {
        if (!this.te.m_8020_(3).m_41619_()) {
            if (this.te.m_8020_(3).m_41720_() instanceof ItemAugmentBlasting) {
                return this.te.hasGeneratorBlastingRecipe(stack);
            }
            if (this.te.m_8020_(3).m_41720_() instanceof ItemAugmentSmoking) {
                return BlockIronFurnaceTileBase.getSmokingBurn(stack) > 0;
            }
        }
        if (stack.m_41720_() instanceof ItemHeater) {
            return false;
        }
        return BlockIronFurnaceTileBase.isItemFuel(stack, RecipeType.f_44108_);
    }

    public boolean m_6659_() {
        return this.te.isGenerator() && this.te.getAugmentGUI() == 0;
    }
}

