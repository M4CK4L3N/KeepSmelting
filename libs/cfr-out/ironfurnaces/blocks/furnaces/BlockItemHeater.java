/*
 * Decompiled with CFR.
 */
package ironfurnaces.blocks.furnaces;

import ironfurnaces.gui.furnaces.BlockIronFurnaceScreenBase;
import ironfurnaces.util.StringHelper;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jetbrains.annotations.NotNull;

public class BlockItemHeater
extends BlockItem {
    public BlockItemHeater(Block block, Item.Properties properties) {
        super(block, properties);
    }

    @OnlyIn(value=Dist.CLIENT)
    public void m_7373_(ItemStack stack, Level worldIn, @NotNull List<Component> tooltip, @NotNull TooltipFlag flagIn) {
        if (stack.m_41782_()) {
            assert (stack.m_41783_() != null);
            tooltip.add((Component)Component.m_237113_((String)StringHelper.displayEnergy(stack.m_41783_().m_128451_("Energy"), 1000000).get(0)).m_130940_(ChatFormatting.GOLD));
        }
        if (BlockIronFurnaceScreenBase.isShiftKeyDown()) {
            tooltip.add((Component)Component.m_237115_((String)"tooltip.ironfurnaces.heater_block").m_6270_(Style.f_131099_.m_131157_(ChatFormatting.GRAY)));
            tooltip.add((Component)Component.m_237115_((String)"tooltip.ironfurnaces.heater_block1").m_6270_(Style.f_131099_.m_131157_(ChatFormatting.GRAY)));
        } else {
            tooltip.add(StringHelper.getShiftInfoText());
        }
    }

    public boolean m_142522_(ItemStack stack) {
        return stack.m_41782_();
    }

    public int m_142158_(ItemStack stack) {
        if (stack.m_41782_()) {
            assert (stack.m_41783_() != null);
            int energy = stack.m_41783_().m_128451_("Energy");
            return (int)(13.0 * ((double)energy / 1000000.0));
        }
        return 0;
    }

    public int m_142159_(@NotNull ItemStack p_150901_) {
        return -8387072;
    }
}

