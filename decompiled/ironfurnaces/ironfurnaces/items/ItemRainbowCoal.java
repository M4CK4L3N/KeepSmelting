/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  javax.annotation.Nullable
 *  net.minecraft.util.Mth
 *  net.minecraft.world.item.Item
 *  net.minecraft.world.item.Item$Properties
 *  net.minecraft.world.item.ItemStack
 *  net.minecraft.world.item.crafting.RecipeType
 *  net.minecraft.world.item.enchantment.Enchantment
 *  net.minecraft.world.level.ItemLike
 */
package ironfurnaces.items;

import javax.annotation.Nullable;
import net.minecraft.util.Mth;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.level.ItemLike;

public class ItemRainbowCoal
extends Item {
    public ItemRainbowCoal(Item.Properties properties) {
        super(properties);
    }

    public boolean m_142522_(ItemStack p_150899_) {
        return true;
    }

    public int m_142158_(ItemStack stack) {
        return (int)(13.0 * (1.0 - (double)stack.m_41773_() / 5120.0));
    }

    public int m_142159_(ItemStack p_150901_) {
        float f = Math.max(0.0f, (5120.0f - (float)p_150901_.m_41773_()) / 5120.0f);
        return Mth.m_14169_((float)(f / 3.0f), (float)1.0f, (float)1.0f);
    }

    public int getBurnTime(ItemStack itemStack, @Nullable RecipeType<?> recipeType) {
        return 200;
    }

    public boolean hasCraftingRemainingItem(ItemStack stack) {
        return true;
    }

    public ItemStack getCraftingRemainingItem(ItemStack itemStack) {
        ItemStack stack = new ItemStack((ItemLike)this);
        stack.m_41721_(this.getDamage(itemStack) + 1);
        if (stack.m_41773_() >= 5120) {
            stack = ItemStack.f_41583_;
        }
        return stack;
    }

    public boolean canApplyAtEnchantingTable(ItemStack stack, Enchantment enchantment) {
        return false;
    }

    public boolean isBookEnchantable(ItemStack stack, ItemStack book) {
        return false;
    }

    public boolean m_8120_(ItemStack p_77616_1_) {
        return false;
    }
}

