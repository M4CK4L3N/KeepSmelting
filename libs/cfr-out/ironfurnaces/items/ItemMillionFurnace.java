/*
 * Decompiled with CFR.
 */
package ironfurnaces.items;

import com.google.common.collect.Lists;
import ironfurnaces.Config;
import ironfurnaces.gui.furnaces.BlockIronFurnaceScreenBase;
import ironfurnaces.util.StringHelper;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import javax.annotation.Nullable;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

public class ItemMillionFurnace
extends BlockItem {
    private Random rand = new Random();
    private int timer = 0;

    public ItemMillionFurnace(Block blockIn, Item.Properties builder) {
        super(blockIn, builder);
    }

    @OnlyIn(value=Dist.CLIENT)
    public void m_7373_(ItemStack stack, @Nullable Level worldIn, List<Component> tooltip, TooltipFlag flagIn) {
        tooltip.add((Component)Component.m_237113_((String)("Cooktime: " + String.valueOf(Config.millionFurnaceSpeed.get()))).m_130940_(ChatFormatting.GRAY));
        ++this.timer;
        if (this.timer % 20 == 0) {
            this.timer = 0;
            String name = Component.m_237115_((String)"block.ironfurnaces.million_furnace").getString();
            ArrayList names = Lists.newArrayList();
            for (int i = 0; i < name.length(); ++i) {
                names.add(Component.m_237113_((String)("" + name.charAt(i))).m_130940_(ChatFormatting.m_126647_((int)ItemMillionFurnace.getIDRandom(this.rand.nextInt(6)))));
            }
            MutableComponent component = Component.m_237113_((String)"");
            for (int i = 0; i < names.size(); ++i) {
                component.m_7220_((Component)names.get(i));
            }
            stack.m_41714_((Component)component);
        }
        DecimalFormat decimal = new DecimalFormat();
        String part1 = Component.m_237115_((String)"tooltip.ironfurnaces.rainbow_gen1").getString();
        String part2 = Component.m_237115_((String)"tooltip.ironfurnaces.rainbow_gen2").getString();
        tooltip.add((Component)Component.m_237113_((String)(part1 + " " + decimal.format(Config.millionFurnacePowerToGenerate.get()).toString().replaceAll("\u00a0", ",") + " " + part2)).m_130940_(ChatFormatting.GRAY));
        if (BlockIronFurnaceScreenBase.isShiftKeyDown()) {
            tooltip.add((Component)Component.m_237115_((String)"tooltip.ironfurnaces.rainbow_gen3").m_130940_(ChatFormatting.GRAY));
            tooltip.add((Component)Component.m_237115_((String)"tooltip.ironfurnaces.rainbow_gen4").m_130940_(ChatFormatting.GRAY));
            tooltip.add((Component)Component.m_237115_((String)"tooltip.ironfurnaces.rainbow_gen5").m_130940_(ChatFormatting.GRAY));
        } else {
            tooltip.add(StringHelper.getShiftInfoText());
        }
    }

    public static int getIDRandom(int id) {
        switch (id) {
            case 0: {
                return 12;
            }
            case 1: {
                return 14;
            }
            case 2: {
                return 10;
            }
            case 3: {
                return 11;
            }
            case 4: {
                return 9;
            }
            case 5: {
                return 13;
            }
            case 6: {
                return 5;
            }
        }
        return 0;
    }
}

