/*
 * Decompiled with CFR.
 */
package ironfurnaces.items.augments;

import ironfurnaces.items.augments.ItemAugmentRed;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

public class ItemAugmentBlasting
extends ItemAugmentRed {
    public ItemAugmentBlasting(Item.Properties properties) {
        super(properties);
    }

    @Override
    @OnlyIn(value=Dist.CLIENT)
    public void m_7373_(ItemStack stack, Level worldIn, List<Component> tooltip, TooltipFlag flagIn) {
        super.m_7373_(stack, worldIn, tooltip, flagIn);
        tooltip.add((Component)Component.m_237115_((String)"tooltip.ironfurnaces.augment_blasting").m_6270_(Style.f_131099_.m_131157_(ChatFormatting.GOLD)));
    }
}

