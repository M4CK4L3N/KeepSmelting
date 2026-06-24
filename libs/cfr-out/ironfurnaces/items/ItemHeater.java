/*
 * Decompiled with CFR.
 */
package ironfurnaces.items;

import ironfurnaces.gui.furnaces.BlockIronFurnaceScreenBase;
import ironfurnaces.util.StringHelper;
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

public class ItemHeater
extends Item {
    public ItemHeater(Item.Properties properties) {
        super(properties);
    }

    @OnlyIn(value=Dist.CLIENT)
    public void m_7373_(ItemStack stack, Level worldIn, List<Component> tooltip, TooltipFlag flagIn) {
        if (BlockIronFurnaceScreenBase.isShiftKeyDown()) {
            if (stack.m_41782_()) {
                tooltip.add((Component)Component.m_237115_((String)"tooltip.ironfurnaces.heater").m_6270_(Style.f_131099_.m_131157_(ChatFormatting.GRAY)));
                tooltip.add((Component)Component.m_237115_((String)"tooltip.ironfurnaces.heaterX").m_6270_(Style.f_131099_.m_131157_(ChatFormatting.GRAY)).m_7220_((Component)Component.m_237113_((String)("" + stack.m_41783_().m_128451_("X"))).m_6270_(Style.f_131099_.m_131157_(ChatFormatting.GRAY))));
                tooltip.add((Component)Component.m_237115_((String)"tooltip.ironfurnaces.heaterY").m_6270_(Style.f_131099_.m_131157_(ChatFormatting.GRAY)).m_7220_((Component)Component.m_237113_((String)("" + stack.m_41783_().m_128451_("Y"))).m_6270_(Style.f_131099_.m_131157_(ChatFormatting.GRAY))));
                tooltip.add((Component)Component.m_237115_((String)"tooltip.ironfurnaces.heaterZ").m_6270_(Style.f_131099_.m_131157_(ChatFormatting.GRAY)).m_7220_((Component)Component.m_237113_((String)("" + stack.m_41783_().m_128451_("Z"))).m_6270_(Style.f_131099_.m_131157_(ChatFormatting.GRAY))));
            } else {
                tooltip.add((Component)Component.m_237115_((String)"tooltip.ironfurnaces.heater_not_bound").m_6270_(Style.f_131099_.m_131157_(ChatFormatting.GRAY)));
                tooltip.add((Component)Component.m_237115_((String)"tooltip.ironfurnaces.heater_tip").m_6270_(Style.f_131099_.m_131157_(ChatFormatting.GRAY)));
                tooltip.add((Component)Component.m_237115_((String)"tooltip.ironfurnaces.heater_tip1").m_6270_(Style.f_131099_.m_131157_(ChatFormatting.GRAY)));
            }
        } else {
            tooltip.add(StringHelper.getShiftInfoText());
        }
    }
}

