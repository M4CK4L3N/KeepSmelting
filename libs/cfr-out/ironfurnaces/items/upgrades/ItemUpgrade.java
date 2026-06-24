/*
 * Decompiled with CFR.
 */
package ironfurnaces.items.upgrades;

import ironfurnaces.energy.FEnergyStorage;
import ironfurnaces.tileentity.furnaces.BlockIronFurnaceTileBase;
import ironfurnaces.util.FurnaceSettings;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.FurnaceBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

public class ItemUpgrade
extends Item {
    private Block from;
    private Block to;

    public ItemUpgrade(Item.Properties properties, Block from, Block to) {
        super(properties);
        this.from = from;
        this.to = to;
    }

    @OnlyIn(value=Dist.CLIENT)
    public void m_7373_(ItemStack stack, @Nullable Level worldIn, List<Component> tooltip, TooltipFlag flagIn) {
        tooltip.add((Component)Component.m_237115_((String)"tooltip.ironfurnaces.upgrade_right_click").m_6270_(Style.f_131099_.m_131157_(ChatFormatting.GRAY)));
    }

    public InteractionResult m_6225_(UseOnContext ctx) {
        Level world = ctx.m_43725_();
        BlockPos pos = ctx.m_8083_();
        if (!world.f_46443_) {
            BlockState next;
            BlockEntity te = world.m_7702_(pos);
            BlockPlaceContext ctx2 = new BlockPlaceContext(ctx);
            if (te.m_58900_().m_60734_() != this.from) {
                return InteractionResult.PASS;
            }
            BlockState blockState = next = this.to.m_5573_(ctx2) != Blocks.f_50016_.m_5573_(ctx2) ? this.to.m_5573_(ctx2) : world.m_8055_(pos);
            if (next == world.m_8055_(pos)) {
                return InteractionResult.PASS;
            }
            if (te instanceof FurnaceBlockEntity) {
                FurnaceBlockEntity furnace = (FurnaceBlockEntity)te;
                for (int i = 0; i < 3; ++i) {
                    ItemStack stack = furnace.m_8020_(i);
                    Containers.m_18992_((Level)furnace.m_58904_(), (double)furnace.m_58899_().m_123341_(), (double)furnace.m_58899_().m_123342_(), (double)furnace.m_58899_().m_123343_(), (ItemStack)stack);
                }
                world.m_46747_(te.m_58899_());
                world.m_7731_(pos, Blocks.f_50016_.m_49966_(), 3);
                world.m_7731_(pos, next, 3);
                world.markAndNotifyBlock(pos, world.m_46745_(pos), world.m_8055_(pos).m_60734_().m_49966_(), world.m_8055_(pos), 3, 3);
                BlockEntity te2 = world.m_7702_(pos);
                if (te2 instanceof BlockIronFurnaceTileBase) {
                    ((BlockIronFurnaceTileBase)te2).placeConfig();
                }
            }
            if (te instanceof BlockIronFurnaceTileBase) {
                FEnergyStorage energyStorage = ((BlockIronFurnaceTileBase)te).energyStorage;
                int[] FACTORY_COOKTIME = ((BlockIronFurnaceTileBase)te).factoryCookTime;
                int[] FACTORY_TOTALCOOKTIME = ((BlockIronFurnaceTileBase)te).factoryTotalCookTime;
                double[] usedRF = ((BlockIronFurnaceTileBase)te).usedRF;
                double generatorBurn = ((BlockIronFurnaceTileBase)te).generatorBurn;
                int generatorRecentRecipeRF = ((BlockIronFurnaceTileBase)te).generatorRecentRecipeRF;
                double gottenRF = ((BlockIronFurnaceTileBase)te).gottenRF;
                int furnaceBurnTime = ((BlockIronFurnaceTileBase)te).furnaceBurnTime;
                int cookTime = ((BlockIronFurnaceTileBase)te).cookTime;
                int totalCookTime = ((BlockIronFurnaceTileBase)te).totalCookTime;
                int recipesUsed = ((BlockIronFurnaceTileBase)te).recipesUsed;
                FurnaceSettings settings = ((BlockIronFurnaceTileBase)te).furnaceSettings;
                NonNullList inventory = ((BlockIronFurnaceTileBase)te).inventory;
                world.m_46747_(te.m_58899_());
                world.m_7731_(pos, Blocks.f_50016_.m_49966_(), 3);
                world.m_7731_(pos, next, 3);
                BlockEntity newTe = world.m_7702_(pos);
                if (newTe instanceof BlockIronFurnaceTileBase) {
                    ((BlockIronFurnaceTileBase)newTe).energyStorage = energyStorage;
                    ((BlockIronFurnaceTileBase)newTe).factoryCookTime = FACTORY_COOKTIME;
                    ((BlockIronFurnaceTileBase)newTe).factoryTotalCookTime = FACTORY_TOTALCOOKTIME;
                    ((BlockIronFurnaceTileBase)newTe).usedRF = usedRF;
                    ((BlockIronFurnaceTileBase)newTe).generatorBurn = generatorBurn;
                    ((BlockIronFurnaceTileBase)newTe).generatorRecentRecipeRF = generatorRecentRecipeRF;
                    ((BlockIronFurnaceTileBase)newTe).gottenRF = gottenRF;
                    ((BlockIronFurnaceTileBase)newTe).furnaceBurnTime = furnaceBurnTime;
                    ((BlockIronFurnaceTileBase)newTe).cookTime = cookTime;
                    ((BlockIronFurnaceTileBase)newTe).totalCookTime = totalCookTime;
                    ((BlockIronFurnaceTileBase)newTe).recipesUsed = recipesUsed;
                    ((BlockIronFurnaceTileBase)newTe).furnaceSettings = settings;
                    ((BlockIronFurnaceTileBase)newTe).inventory = inventory;
                }
                world.markAndNotifyBlock(pos, world.m_46745_(pos), world.m_8055_(pos).m_60734_().m_49966_(), world.m_8055_(pos), 3, 3);
            }
            if (!ctx.m_43723_().m_7500_()) {
                ctx.m_43722_().m_41774_(1);
            }
        }
        return super.m_6225_(ctx);
    }
}

