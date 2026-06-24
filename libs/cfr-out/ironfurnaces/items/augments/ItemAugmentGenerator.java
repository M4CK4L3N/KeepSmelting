/*
 * Decompiled with CFR.
 */
package ironfurnaces.items.augments;

import ironfurnaces.items.augments.ItemAugmentBlue;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

public class ItemAugmentGenerator
extends ItemAugmentBlue {
    public ItemAugmentGenerator(Item.Properties properties) {
        super(properties);
    }

    @Override
    @OnlyIn(value=Dist.CLIENT)
    public void m_7373_(ItemStack stack, @Nullable Level worldIn, List<Component> tooltip, TooltipFlag flagIn) {
        super.m_7373_(stack, worldIn, tooltip, flagIn);
        tooltip.add((Component)Component.m_237115_((String)"tooltip.ironfurnaces.augment_generator_pro").m_6270_(Style.f_131099_.m_131157_(ChatFormatting.GREEN)));
        tooltip.add((Component)Component.m_237115_((String)"tooltip.ironfurnaces.augment_generator_con").m_6270_(Style.f_131099_.m_131157_(ChatFormatting.DARK_RED)));
    }
}

