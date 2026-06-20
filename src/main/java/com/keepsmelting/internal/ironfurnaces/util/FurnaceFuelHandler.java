package com.keepsmelting.internal.ironfurnaces.util;

import ironfurnaces.tileentity.furnaces.BlockIronFurnaceTileBase;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.common.ForgeHooks;

/**
 * Единая логика работы с топливом для генераторов Iron Furnaces.
 */
public class FurnaceFuelHandler {

    private FurnaceFuelHandler() {}

    public static void burnFuelIn(BlockIronFurnaceTileBase genTile, int amount, Level level) {
        int toBurn = amount;
        ItemStack fuelStack = genTile.inventory.get(6);
        while (toBurn > 0) {
            if (fuelStack.isEmpty()) {
                pullFuelFromNeighbors(genTile, level);
                fuelStack = genTile.inventory.get(6);
                if (fuelStack.isEmpty()) break;
            }
            int take = Math.min(toBurn, fuelStack.getCount());
            fuelStack.shrink(take);
            toBurn -= take;
            if (fuelStack.isEmpty()) {
                genTile.inventory.set(6, fuelStack.getCraftingRemainingItem());
            }
            genTile.setChanged();
        }
        genTile.generatorBurn = genTile.getGeneratorBurn();
        genTile.generatorRecentRecipeRF = (int) genTile.generatorBurn;
    }

    public static boolean igniteFuel(BlockIronFurnaceTileBase genTile, Level level) {
        if (genTile.generatorBurn > 0.0) return true;

        ItemStack fuel = genTile.inventory.get(6);
        if (fuel.isEmpty()) {
            pullFuelFromNeighbors(genTile, level);
            fuel = genTile.inventory.get(6);
            if (fuel.isEmpty()) return false;
        }

        genTile.generatorBurn = genTile.getGeneratorBurn();
        genTile.generatorRecentRecipeRF = (int) genTile.generatorBurn;

        if (fuel.hasCraftingRemainingItem()) {
            genTile.inventory.set(6, fuel.getCraftingRemainingItem());
        } else {
            fuel.shrink(1);
            if (fuel.isEmpty()) {
                genTile.inventory.set(6, fuel.getCraftingRemainingItem());
            }
        }
        genTile.setChanged();
        return genTile.generatorBurn > 0.0;
    }

    public static int countGeneratorFuel(BlockIronFurnaceTileBase genTile, Level level) {
        int total = genTile.inventory.get(6).getCount();
        for (Direction dir : Direction.values()) {
            BlockPos neighbor = genTile.getBlockPos().relative(dir);
            if (!level.isLoaded(neighbor)) continue;
            BlockEntity be = level.getBlockEntity(neighbor);
            if (!(be instanceof Container container)) continue;
            for (int i = 0; i < container.getContainerSize(); i++) {
                ItemStack stack = container.getItem(i);
                if (stack.isEmpty()) continue;
                if (ForgeHooks.getBurnTime(stack, null) > 0) {
                    total += stack.getCount();
                }
            }
        }
        return total;
    }

    private static void pullFuelFromNeighbors(BlockIronFurnaceTileBase genTile, Level level) {
        BlockPos pos = genTile.getBlockPos();
        for (Direction dir : Direction.values()) {
            BlockPos neighbor = pos.relative(dir);
            if (!level.isLoaded(neighbor)) continue;
            if (!(level.getBlockEntity(neighbor) instanceof Container container)) continue;

            for (int i = 0; i < container.getContainerSize(); i++) {
                ItemStack stack = container.getItem(i);
                if (stack.isEmpty()) continue;
                if (ForgeHooks.getBurnTime(stack, null) <= 0) continue;

                ItemStack taken = container.removeItem(i, 1);
                if (taken.isEmpty()) continue;

                ItemStack current = genTile.inventory.get(6);
                if (current.isEmpty()) {
                    genTile.inventory.set(6, taken);
                } else if (ItemStack.isSameItemSameTags(current, taken)
                        && current.getCount() < current.getMaxStackSize()) {
                    current.grow(1);
                    genTile.inventory.set(6, current);
                } else {
                    container.setItem(i, taken);
                    continue;
                }
                container.setChanged();
                genTile.setChanged();
                return;
            }
        }
    }

    public static long getBurnTicksPerFuel(BlockIronFurnaceTileBase genTile) {
        int gen = Math.max(1, genTile.getGeneration());
        return (long) Math.ceil(genTile.getGeneratorBurn() * 20.0 / gen);
    }
}
