/*
 * Decompiled with CFR.
 */
package ironfurnaces.items;

import ironfurnaces.tileentity.furnaces.BlockIronFurnaceTileBase;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

public class ItemFurnaceCopy
extends Item {
    public ItemFurnaceCopy(Item.Properties properties) {
        super(properties);
    }

    @OnlyIn(value=Dist.CLIENT)
    public void m_7373_(ItemStack stack, @Nullable Level worldIn, List<Component> tooltip, TooltipFlag flagIn) {
        if (stack.m_41782_() && stack.m_41783_().m_128465_("settings").length >= 10) {
            tooltip.add((Component)Component.m_237113_((String)("Down: " + stack.m_41783_().m_128465_("settings")[0])).m_6270_(Style.f_131099_.m_131157_(ChatFormatting.GRAY)));
            tooltip.add((Component)Component.m_237113_((String)("Up: " + stack.m_41783_().m_128465_("settings")[1])).m_6270_(Style.f_131099_.m_131157_(ChatFormatting.GRAY)));
            tooltip.add((Component)Component.m_237113_((String)("North: " + stack.m_41783_().m_128465_("settings")[2])).m_6270_(Style.f_131099_.m_131157_(ChatFormatting.GRAY)));
            tooltip.add((Component)Component.m_237113_((String)("South: " + stack.m_41783_().m_128465_("settings")[3])).m_6270_(Style.f_131099_.m_131157_(ChatFormatting.GRAY)));
            tooltip.add((Component)Component.m_237113_((String)("West: " + stack.m_41783_().m_128465_("settings")[4])).m_6270_(Style.f_131099_.m_131157_(ChatFormatting.GRAY)));
            tooltip.add((Component)Component.m_237113_((String)("East: " + stack.m_41783_().m_128465_("settings")[5])).m_6270_(Style.f_131099_.m_131157_(ChatFormatting.GRAY)));
            tooltip.add((Component)Component.m_237113_((String)("Auto Input: " + stack.m_41783_().m_128465_("settings")[6])).m_6270_(Style.f_131099_.m_131157_(ChatFormatting.GRAY)));
            tooltip.add((Component)Component.m_237113_((String)("Auto Output: " + stack.m_41783_().m_128465_("settings")[7])).m_6270_(Style.f_131099_.m_131157_(ChatFormatting.GRAY)));
            tooltip.add((Component)Component.m_237113_((String)("Redstone Mode: " + stack.m_41783_().m_128465_("settings")[8])).m_6270_(Style.f_131099_.m_131157_(ChatFormatting.GRAY)));
            tooltip.add((Component)Component.m_237113_((String)("Redstone Value: " + stack.m_41783_().m_128465_("settings")[9])).m_6270_(Style.f_131099_.m_131157_(ChatFormatting.GRAY)));
        }
        tooltip.add((Component)Component.m_237113_((String)"Right-click to copy settings").m_130940_(ChatFormatting.GRAY));
        tooltip.add((Component)Component.m_237113_((String)"Sneak & right-click to apply settings").m_130940_(ChatFormatting.GRAY));
    }

    public InteractionResult m_6225_(UseOnContext ctx) {
        Level world = ctx.m_43725_();
        BlockPos pos = ctx.m_8083_();
        if (!ctx.m_43723_().m_6047_()) {
            return super.m_6225_(ctx);
        }
        if (!world.f_46443_) {
            BlockEntity te = world.m_7702_(pos);
            if (!(te instanceof BlockIronFurnaceTileBase)) {
                return super.m_6225_(ctx);
            }
            ItemStack stack = ctx.m_43722_();
            if (stack.m_41782_() && stack.m_41783_().m_128465_("settings") != null && stack.m_41783_().m_128465_("settings").length > 0) {
                int[] settings = stack.m_41783_().m_128465_("settings");
                for (int i = 0; i < settings.length; ++i) {
                    ((BlockIronFurnaceTileBase)te).furnaceSettings.set(i, settings[i]);
                }
            }
            world.markAndNotifyBlock(pos, world.m_46745_(pos), world.m_8055_(pos).m_60734_().m_49966_(), world.m_8055_(pos), 3, 3);
            ctx.m_43723_().m_213846_((Component)Component.m_237113_((String)"Settings applied"));
        }
        return super.m_6225_(ctx);
    }
}

