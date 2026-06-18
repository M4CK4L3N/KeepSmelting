/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  javax.annotation.Nullable
 *  net.minecraft.ChatFormatting
 *  net.minecraft.network.chat.Component
 *  net.minecraft.network.chat.Style
 *  net.minecraft.world.item.Item$Properties
 *  net.minecraft.world.item.ItemStack
 *  net.minecraft.world.item.TooltipFlag
 *  net.minecraft.world.level.Level
 *  net.minecraftforge.api.distmarker.Dist
 *  net.minecraftforge.api.distmarker.OnlyIn
 */
package ironfurnaces.items.augments;

import ironfurnaces.items.augments.ItemAugmentGreen;
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

public class ItemAugmentSpeed
extends ItemAugmentGreen {
    public ItemAugmentSpeed(Item.Properties properties) {
        super(properties);
    }

    @Override
    @OnlyIn(value=Dist.CLIENT)
    public void m_7373_(ItemStack stack, @Nullable Level worldIn, List<Component> tooltip, TooltipFlag flagIn) {
        super.m_7373_(stack, worldIn, tooltip, flagIn);
        tooltip.add((Component)Component.m_237115_((String)"tooltip.ironfurnaces.augment_speed_pro").m_6270_(Style.f_131099_.m_131157_(ChatFormatting.GREEN)));
        tooltip.add((Component)Component.m_237115_((String)"tooltip.ironfurnaces.augment_speed_con").m_6270_(Style.f_131099_.m_131157_(ChatFormatting.DARK_RED)));
    }
}

