package com.keepsmelting.internal.ironfurnaces.handler;

import com.keepsmelting.api.catchup.AbstractCatchupHandler;
import com.keepsmelting.mixin.ironfurnaces.IronFurnaceAccessor;
import ironfurnaces.items.augments.ItemAugmentFuel;
import ironfurnaces.items.augments.ItemAugmentSpeed;
import ironfurnaces.tileentity.furnaces.BlockIronFurnaceTileBase;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.ForgeHooks;

import java.util.Optional;

/**
 * Furnace mode catchup — adaptive batch O(events).
 */
public class FurnaceMode {

    public static void apply(BlockIronFurnaceTileBase tile, long elapsed, Level level, BlockPos pos) {
        if (tile.furnaceBurnTime <= 0 || tile.totalCookTime <= 0) return;

        int cookTimeBefore = tile.cookTime;
        int burnTimeBefore = tile.furnaceBurnTime;
        int fuelBefore = tile.inventory.get(1).getCount();

        int maxStack = tile.getMaxStackSize();
        long remaining = elapsed;
        int cookTime = tile.cookTime;
        int burnTime = tile.furnaceBurnTime;
        int totalCookTime = tile.totalCookTime;
        ItemStack fuel = tile.inventory.get(1);
        ItemStack output = tile.inventory.get(2);
        ItemStack input = tile.inventory.get(0);

        IronFurnaceAccessor acc = (IronFurnaceAccessor) tile;
        long totalItems = 0;

        ItemStack greenAug = tile.inventory.get(4);
        boolean hasSpeed = !greenAug.isEmpty() && greenAug.getItem() instanceof ItemAugmentSpeed;
        boolean hasFuel = !greenAug.isEmpty() && greenAug.getItem() instanceof ItemAugmentFuel;

        while (remaining > 0 && burnTime > 0) {
            int newTotal = tile.getCookTime();
            if (newTotal > 0) totalCookTime = newTotal;

            if (input.isEmpty()) {
                tile.furnaceBurnTime = Math.max(0, burnTime);
                tile.cookTime = Math.max(0, cookTime);
                tile.setChanged();
                acc.invokeAutoIO();
                input = tile.inventory.get(0);
                if (input.isEmpty()) break;
            }

            long applyTick = Math.min(remaining, Math.min(burnTime,
                    totalCookTime - cookTime + (long) (input.getCount() - 1) * totalCookTime));
            cookTime += (int) applyTick;
            burnTime -= (int) applyTick;
            remaining -= applyTick;

            while (cookTime >= totalCookTime && !input.isEmpty()) {
                cookTime -= totalCookTime;

                if (!output.isEmpty() && output.getCount() >= maxStack) {
                    tile.furnaceBurnTime = Math.max(0, burnTime);
                    tile.cookTime = Math.max(0, cookTime);
                    tile.setChanged();
                    acc.invokeAutoIO();
                    output = tile.inventory.get(2);
                    if (!output.isEmpty() && output.getCount() >= maxStack) break;
                }

                input.shrink(1);

                Optional<?> recipeOpt = acc.invokeGetRecipe(
                        input.isEmpty() ? ItemStack.EMPTY : input);
                if (recipeOpt.isPresent()) {
                    Recipe<?> recipe = (Recipe<?>) recipeOpt.get();
                    ItemStack result = recipe.getResultItem(level.registryAccess());
                    if (output.isEmpty()) {
                        tile.inventory.set(2, result.copy());
                        output = tile.inventory.get(2);
                    } else if (ItemStack.isSameItemSameTags(output, result)) {
                        output.grow(result.getCount());
                    }
                }
                totalItems++;
                if (input.getCount() <= 0) {
                    tile.inventory.set(0, ItemStack.EMPTY);
                    input = ItemStack.EMPTY;
                }
            }

            if (burnTime <= 0 && remaining > 0) {
                if (fuel.isEmpty()) {
                    tile.furnaceBurnTime = 0;
                    tile.cookTime = cookTime;
                    tile.setChanged();
                    acc.invokeAutoIO();
                    fuel = tile.inventory.get(1);
                }
                if (!fuel.isEmpty()) {
                    RecipeType<?> rt = tile.recipeType;
                    int baseBurn = ForgeHooks.getBurnTime(fuel, rt);
                    if (baseBurn > 0) {
                        int adjustedCook = Math.max(1, totalCookTime);
                        int burn = baseBurn * adjustedCook / 200;
                        if (hasSpeed) burn /= 2;
                        if (hasFuel) burn *= 2;
                        burnTime = Math.max(1, burn);
                        fuel.shrink(1);
                        if (fuel.getCount() <= 0) {
                            tile.inventory.set(1, fuel.getCraftingRemainingItem());
                        }
                    } else break;
                } else break;
            }
        }
        tile.furnaceBurnTime = Math.max(0, burnTime);
        tile.cookTime = Math.max(0, cookTime);
        tile.setChanged();

        AbstractCatchupHandler.sendChatDebug(level, pos, "Furnace", elapsed,
                fuelBefore - tile.inventory.get(1).getCount(),
                (int) totalItems,
                tile.cookTime - cookTimeBefore,
                burnTimeBefore - tile.furnaceBurnTime,
                tile.furnaceBurnTime > 0);
    }
}
