/*
 * Decompiled with CFR.
 */
package ironfurnaces.items;

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

public class ItemXmas
extends Item {
    public ItemXmas(Item.Properties properties) {
        super(properties);
    }

    @OnlyIn(value=Dist.CLIENT)
    public void m_7373_(ItemStack stack, @Nullable Level worldIn, List<Component> tooltip, TooltipFlag flagIn) {
        tooltip.add((Component)Component.m_237115_((String)"tooltip.ironfurnaces.xmas_right_click").m_6270_(Style.f_131099_.m_131157_(ChatFormatting.GRAY)));
        tooltip.add((Component)Component.m_237115_((String)"tooltip.ironfurnaces.xmas1").m_6270_(Style.f_131099_.m_131157_(ChatFormatting.GRAY)));
        tooltip.add((Component)Component.m_237115_((String)"tooltip.ironfurnaces.xmas2").m_6270_(Style.f_131099_.m_131157_(ChatFormatting.GRAY)));
    }
}

