package com.keepsmelting.internal.ironfurnaces.collect;

import com.keepsmelting.internal.ironfurnaces.collect.FurnaceNetwork;
import com.keepsmelting.internal.ironfurnaces.data.SimulationData.FactorySmeltParams;
import com.keepsmelting.internal.ironfurnaces.data.SimulationData.NetworkResources;
import com.keepsmelting.internal.ironfurnaces.util.FurnaceFuelHandler;
import com.keepsmelting.mixin.ironfurnaces.IronFurnaceAccessor;
import ironfurnaces.items.augments.ItemAugmentFuel;
import ironfurnaces.items.augments.ItemAugmentSpeed;
import ironfurnaces.tileentity.furnaces.BlockIronFurnaceTileBase;
import net.minecraft.core.BlockPos;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import java.util.Optional;

/**
 * Phase 1: сбор данных из печей Iron Furnaces.
 * Подсчитывает ресурсы сети без сайд-эффектов.
 */
public class NetworkDataCollector {

    private NetworkDataCollector() {}

    public static int[] getFactoryInputSlots(BlockIronFurnaceTileBase factoryTile) {
        int tier = factoryTile.getTier();
        if (tier == 0) return new int[]{9, 10};
        if (tier == 1) return new int[]{8, 9, 10, 11};
        return new int[]{7, 8, 9, 10, 11, 12};
    }

    public static int getGeneratorRfPerTick(BlockIronFurnaceTileBase genTile) {
        return Math.max(1, genTile.getGeneration());
    }

    public static long getBurnTicksPerFuel(BlockIronFurnaceTileBase genTile) {
        return FurnaceFuelHandler.getBurnTicksPerFuel(genTile);
    }

    public static int countGeneratorFuel(BlockIronFurnaceTileBase genTile, Level level) {
        return FurnaceFuelHandler.countGeneratorFuel(genTile, level);
    }

    public static int countFactoryInputs(BlockIronFurnaceTileBase factoryTile, Level level) {
        int[] inputSlots = getFactoryInputSlots(factoryTile);
        int total = 0;
        for (int slot : inputSlots) {
            total += factoryTile.inventory.get(slot).getCount();
        }
        BlockPos above = factoryTile.getBlockPos().above();
        if (level.isLoaded(above) && level.getBlockEntity(above) instanceof Container container) {
            for (int i = 0; i < container.getContainerSize(); i++) {
                total += container.getItem(i).getCount();
            }
        }
        return total;
    }

    public static int countFactoryOutputSpace(BlockIronFurnaceTileBase factoryTile, Level level) {
        int[] outputSlots = new int[]{13, 14, 15, 16, 17, 18};
        int space = 0;
        for (int slot : outputSlots) {
            ItemStack stack = factoryTile.inventory.get(slot);
            space += stack.isEmpty() ? 64 : stack.getMaxStackSize() - stack.getCount();
        }
        BlockPos below = factoryTile.getBlockPos().below();
        if (level.isLoaded(below) && level.getBlockEntity(below) instanceof Container container) {
            for (int i = 0; i < container.getContainerSize(); i++) {
                ItemStack stack = container.getItem(i);
                space += stack.isEmpty() ? 64 : stack.getMaxStackSize() - stack.getCount();
            }
        }
        return space;
    }

    public static FactorySmeltParams computeFactoryParams(BlockIronFurnaceTileBase factoryTile) {
        int maxRfPerItem = 0;
        int totalRfPerTick = 0;
        int maxCookTime = 0;

        ItemStack greenAug = factoryTile.inventory.get(4);
        boolean hasSpeed = !greenAug.isEmpty() && greenAug.getItem() instanceof ItemAugmentSpeed;
        boolean hasFuel = !greenAug.isEmpty() && greenAug.getItem() instanceof ItemAugmentFuel;

        IronFurnaceAccessor acc = (IronFurnaceAccessor) factoryTile;

        for (int slot : new int[]{7, 8, 9, 10, 11, 12}) {
            ItemStack input = factoryTile.inventory.get(slot);
            if (input.isEmpty()) continue;

            Optional<? extends net.minecraft.world.item.crafting.AbstractCookingRecipe> recipeOpt =
                    acc.invokeGetRecipeFactory(slot, input);
            if (recipeOpt.isEmpty()) continue;

            int baseRecipeTime = recipeOpt.get().getCookingTime();
            int rfPerItem = baseRecipeTime * 20;
            if (hasSpeed) rfPerItem *= 2;
            if (hasFuel) rfPerItem /= 2;

            int cookTime = acc.invokeGetFactoryCookTime(slot);
            if (cookTime <= 0) continue;

            maxRfPerItem = Math.max(maxRfPerItem, rfPerItem);
            totalRfPerTick += Math.max(1, rfPerItem / Math.max(1, cookTime));
            maxCookTime = Math.max(maxCookTime, cookTime);
        }

        FactorySmeltParams p = new FactorySmeltParams();
        p.maxRfPerItem = Math.max(1, maxRfPerItem);
        p.totalRfPerTick = Math.max(1, totalRfPerTick);
        p.maxCookTime = Math.max(1, maxCookTime);
        return p;
    }

    public static NetworkResources aggregateNetwork(FurnaceNetwork network, Level level) {
        NetworkResources nr = new NetworkResources();

        long totalBurnSum = 0;
        long totalGenRfPerTick = 0;
        for (BlockIronFurnaceTileBase gen : network.generators) {
            nr.totalFuel += countGeneratorFuel(gen, level);
            nr.totalGenCapacity += gen.getCapacity();
            nr.totalGenCurrentRf += gen.getEnergy();
            int rfPerTick = getGeneratorRfPerTick(gen);
            nr.totalRfPerTick += rfPerTick;
            long burnTicks = getBurnTicksPerFuel(gen);
            totalBurnSum += burnTicks * rfPerTick;
            totalGenRfPerTick += rfPerTick;
        }
        nr.totalAvgBurnTicksPerFuel = totalGenRfPerTick > 0
                ? (int) (totalBurnSum / totalGenRfPerTick)
                : 1200;

        for (BlockIronFurnaceTileBase factory : network.factories) {
            nr.totalSmeltableItems += countFactoryInputs(factory, level);
            nr.totalOutputSpace += countFactoryOutputSpace(factory, level);
            nr.totalFactoryCapacity += factory.getCapacity();
            nr.totalFactoryCurrentRf += factory.getEnergy();

            FactorySmeltParams params = computeFactoryParams(factory);
            nr.totalRfPerTickConsumption += params.totalRfPerTick;
            nr.maxRfPerItem = Math.max(nr.maxRfPerItem, params.maxRfPerItem);
            nr.maxCookTime = Math.max(nr.maxCookTime, params.maxCookTime);
        }

        nr.network = network;
        return nr;
    }
}
