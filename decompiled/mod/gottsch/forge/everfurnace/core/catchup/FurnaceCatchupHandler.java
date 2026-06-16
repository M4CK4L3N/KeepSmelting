/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.core.BlockPos
 *  net.minecraft.core.NonNullList
 *  net.minecraft.server.level.ServerLevel
 *  net.minecraft.world.Container
 *  net.minecraft.world.item.ItemStack
 *  net.minecraft.world.item.crafting.AbstractCookingRecipe
 *  net.minecraft.world.item.crafting.Recipe
 *  net.minecraft.world.item.crafting.RecipeType
 *  net.minecraft.world.level.Level
 *  net.minecraft.world.level.block.AbstractFurnaceBlock
 *  net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity
 *  net.minecraft.world.level.block.entity.BlockEntity
 *  net.minecraft.world.level.block.entity.BlockEntityType
 *  net.minecraft.world.level.block.state.BlockState
 *  net.minecraft.world.level.block.state.properties.Property
 */
package mod.gottsch.forge.everfurnace.core.catchup;

import mod.gottsch.forge.everfurnace.api.CookingCatchupHandler;
import mod.gottsch.forge.everfurnace.core.config.EverFurnaceConfig;
import mod.gottsch.forge.everfurnace.core.furnace.IEverFurnaceBlockEntity;
import mod.gottsch.forge.everfurnace.core.mixin.IEverFurnaceBlockEntityMixin;
import mod.gottsch.forge.everfurnace.core.network.ModNetwork;
import mod.gottsch.forge.everfurnace.core.util.CookResult;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.AbstractCookingRecipe;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.AbstractFurnaceBlock;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;

public class FurnaceCatchupHandler
implements CookingCatchupHandler {
    private static final int INPUT_SLOT = 0;
    private static final int FUEL_SLOT = 1;
    private static final int OUTPUT_SLOT = 2;

    @Override
    public void applyCatchup(BlockEntity blockEntity, long deltaTime, ServerLevel level, BlockPos pos) {
        long totalCookTimeRemaining;
        AbstractFurnaceBlockEntity furnace = (AbstractFurnaceBlockEntity)blockEntity;
        IEverFurnaceBlockEntityMixin ife = (IEverFurnaceBlockEntityMixin)blockEntity;
        IEverFurnaceBlockEntity mixin = (IEverFurnaceBlockEntity)blockEntity;
        if (!ife.callIsLit()) {
            return;
        }
        NonNullList<ItemStack> items = ife.getItems();
        ItemStack cookStack = furnace.m_8020_(0);
        if (cookStack.m_41619_()) {
            return;
        }
        ItemStack outputStack = furnace.m_8020_(2);
        if (!outputStack.m_41619_() && outputStack.m_41613_() == furnace.m_6893_()) {
            return;
        }
        Recipe<?> recipe = FurnaceCatchupHandler.findRecipe(furnace, (Level)level);
        if (!ife.callCanBurn(level.m_9598_(), recipe, items, furnace.m_6893_())) {
            return;
        }
        ItemStack fuelStack = furnace.m_8020_(1);
        if (fuelStack.m_41619_()) {
            return;
        }
        long totalBurnTimeRemaining = (long)(fuelStack.m_41613_() - 1) * (long)ife.getLitDuration() + (long)ife.getLitTime();
        long maxApplicableTime = Math.min(totalBurnTimeRemaining, totalCookTimeRemaining = (long)(cookStack.m_41613_() - 1) * (long)ife.getCookingTotalTime() + (long)(ife.getCookingTotalTime() - ife.getCookingProgress()));
        long actualAppliedTime = Math.min(deltaTime, maxApplicableTime);
        if (actualAppliedTime <= 0L) {
            return;
        }
        FurnaceCatchupHandler.applyFuelTime(ife, furnace, fuelStack, actualAppliedTime);
        CookResult result = FurnaceCatchupHandler.applyCookTime((Level)level, furnace, ife, recipe, cookStack, actualAppliedTime);
        if (result.itemsCooked() > 0 && ((Boolean)EverFurnaceConfig.COMMON.notifyPlayerOnCatchup.get()).booleanValue()) {
            mixin.everFurnace_1_20_1$setPendingNotification(mixin.everFurnace_1_20_1$getPendingNotification() + result.itemsCooked());
            long currentGameTime = level.m_46467_();
            long cooldown = (Long)EverFurnaceConfig.COMMON.notificationCooldownTicks.get();
            long lastArmed = mixin.everFurnace_1_20_1$getLastNotificationTime();
            if (cooldown == 0L || lastArmed == 0L || currentGameTime - lastArmed >= cooldown) {
                mixin.everFurnace_1_20_1$setLastNotificationTime(currentGameTime);
            }
        }
        if (result.xpEarned() > 0.0f) {
            mixin.everFurnace_1_20_1$setPendingXp(mixin.everFurnace_1_20_1$getPendingXp() + result.xpEarned());
        }
        if (result.itemsCooked() > 0) {
            ModNetwork.sendCatchupParticles(level, pos);
        }
        furnace.m_6596_();
        if (!ife.callIsLit()) {
            BlockState state = level.m_8055_(pos);
            BlockState newState = (BlockState)state.m_61124_((Property)AbstractFurnaceBlock.f_48684_, (Comparable)Boolean.valueOf(false));
            level.m_7731_(pos, newState, 3);
            furnace.m_6596_();
        }
    }

    private static Recipe<?> findRecipe(AbstractFurnaceBlockEntity furnace, Level level) {
        BlockEntityType type = furnace.m_58903_();
        RecipeType recipeType = type == BlockEntityType.f_58907_ ? RecipeType.f_44109_ : (type == BlockEntityType.f_58906_ ? RecipeType.f_44110_ : RecipeType.f_44108_);
        return level.m_7465_().m_44015_(recipeType, (Container)furnace, level).orElse(null);
    }

    private static void applyFuelTime(IEverFurnaceBlockEntityMixin ife, AbstractFurnaceBlockEntity furnace, ItemStack fuelStack, long ticks) {
        long totalConsumed = ticks;
        int litDuration = ife.getLitDuration();
        if (totalConsumed <= (long)ife.getLitTime()) {
            ife.setLitTime((int)((long)ife.getLitTime() - totalConsumed));
        } else {
            ife.setLitTime(0);
            int wholeItemsNeeded = (int)Math.ceil((double)(totalConsumed -= (long)ife.getLitTime()) / (double)litDuration);
            wholeItemsNeeded = Math.min(wholeItemsNeeded, fuelStack.m_41613_());
            long ticksCoveredByNewItems = (long)wholeItemsNeeded * (long)litDuration;
            long leftover = ticksCoveredByNewItems - totalConsumed;
            fuelStack.m_41774_(wholeItemsNeeded);
            if (fuelStack.m_41619_()) {
                ife.setLitTime(0);
                furnace.m_6836_(1, fuelStack.getCraftingRemainingItem());
            } else {
                ife.setLitTime((int)leftover);
            }
        }
    }

    private static CookResult applyCookTime(Level world, AbstractFurnaceBlockEntity furnace, IEverFurnaceBlockEntityMixin ife, Recipe<?> recipe, ItemStack cookStack, long ticks) {
        float f;
        int cookingTotalTime = ife.getCookingTotalTime();
        if (cookingTotalTime <= 0) {
            return new CookResult(0, 0.0f);
        }
        if (recipe instanceof AbstractCookingRecipe) {
            AbstractCookingRecipe cookingRecipe = (AbstractCookingRecipe)recipe;
            f = cookingRecipe.m_43750_();
        } else {
            f = 0.0f;
        }
        float xpPerItem = f;
        int cooked = 0;
        float xpEarned = 0.0f;
        NonNullList<ItemStack> items = ife.getItems();
        long ticksToFinishCurrent = cookingTotalTime - ife.getCookingProgress();
        if (ticks < ticksToFinishCurrent) {
            ife.setCookingProgress(ife.getCookingProgress() + (int)ticks);
        } else {
            ticks -= ticksToFinishCurrent;
            ife.setCookingProgress(cookingTotalTime);
            if (ife.callBurn(world.m_9598_(), recipe, items, furnace.m_6893_())) {
                furnace.m_6029_(recipe);
                ++cooked;
                xpEarned += xpPerItem;
            }
            ife.setCookingProgress(0);
            if (!cookStack.m_41619_() && cookingTotalTime > 0) {
                long additionalItems = ticks / (long)cookingTotalTime;
                long remainder = ticks % (long)cookingTotalTime;
                for (long i = 0L; i < additionalItems && ife.callCanBurn(world.m_9598_(), recipe, items, furnace.m_6893_()); ++i) {
                    if (!ife.callBurn(world.m_9598_(), recipe, items, furnace.m_6893_())) continue;
                    furnace.m_6029_(recipe);
                    ++cooked;
                    xpEarned += xpPerItem;
                }
                ife.setCookingProgress((int)remainder);
            }
        }
        return new CookResult(cooked, xpEarned);
    }
}

