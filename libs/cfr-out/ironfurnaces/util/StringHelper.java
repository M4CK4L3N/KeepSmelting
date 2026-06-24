/*
 * Decompiled with CFR.
 */
package ironfurnaces.util;

import com.google.common.collect.Lists;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

public class StringHelper {
    public static List<String> displayEnergy(int energy, int capacity) {
        ArrayList<String> text = new ArrayList<String>();
        NumberFormat format = DecimalFormat.getNumberInstance();
        String i = format.format(energy);
        String j = format.format(capacity);
        i = i.replaceAll("\u00a0", ",");
        j = j.replaceAll("\u00a0", ",");
        text.add(i + " / " + j + " RF");
        return text;
    }

    public static List<String> displayEnergy(int energy) {
        ArrayList<String> text = new ArrayList<String>();
        NumberFormat format = DecimalFormat.getNumberInstance();
        String i = format.format(energy);
        i = i.replaceAll("\u00a0", ",");
        text.add(i + " RF");
        return text;
    }

    public static List<Component> getShiftInfoGui() {
        ArrayList list = Lists.newArrayList();
        list.add(Component.m_237115_((String)"tooltip.ironfurnaces.gui_close"));
        MutableComponent tooltip1 = Component.m_237115_((String)"tooltip.ironfurnaces.gui_hold_shift");
        MutableComponent shift = Component.m_237113_((String)"[Shift]");
        MutableComponent tooltip2 = Component.m_237115_((String)"tooltip.ironfurnaces.gui_shift_more_options");
        tooltip1.m_130940_(ChatFormatting.GRAY);
        shift.m_130944_(new ChatFormatting[]{ChatFormatting.GOLD, ChatFormatting.ITALIC});
        tooltip2.m_130940_(ChatFormatting.GRAY);
        list.add(tooltip1.m_7220_((Component)shift).m_7220_((Component)tooltip2));
        return list;
    }

    public static Component getShiftInfoText() {
        MutableComponent tooltip1 = Component.m_237115_((String)"tooltip.ironfurnaces.hold");
        MutableComponent shift = Component.m_237113_((String)"[Shift]");
        MutableComponent tooltip2 = Component.m_237115_((String)"tooltip.ironfurnaces.for_details");
        tooltip1.m_130940_(ChatFormatting.GRAY);
        shift.m_130944_(new ChatFormatting[]{ChatFormatting.GOLD, ChatFormatting.ITALIC});
        tooltip2.m_130940_(ChatFormatting.GRAY);
        return tooltip1.m_7220_((Component)shift).m_7220_((Component)tooltip2);
    }
}

