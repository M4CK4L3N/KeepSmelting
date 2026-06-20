package com.keepsmelting.internal.ironfurnaces.apply;

import com.keepsmelting.internal.ironfurnaces.util.CatchupDedup;
import com.keepsmelting.internal.ironfurnaces.FurnaceNetwork;
import com.keepsmelting.internal.ironfurnaces.collect.NetworkDataCollector;
import com.keepsmelting.internal.ironfurnaces.data.SimulationData.NetworkResources;
import com.keepsmelting.internal.ironfurnaces.data.SimulationData.SimulationResult;
import com.keepsmelting.internal.ironfurnaces.util.FurnaceFuelHandler;
import com.keepsmelting.internal.ironfurnaces.util.HopperHelper;
import com.keepsmelting.internal.catchup.AbstractCatchupHandler;
import com.keepsmelting.mixin.ironfurnaces.IronFurnaceAccessor;
import ironfurnaces.items.augments.ItemAugmentFuel;
import ironfurnaces.items.augments.ItemAugmentSpeed;
import ironfurnaces.tileentity.furnaces.BlockIronFurnaceTileBase;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import java.util.Optional;

/**
 * Phase 3: применение результатов симуляции к печам (сайд-эффекты).
 */
public class SimulationApplicator {

    private SimulationApplicator() {}

    public static void distributeToNetwork(NetworkResources nr, SimulationResult r, Level level) {
        com.keepsmelting.KeepSmelting.LOGGER.info(
                "[Distribute] fuel={} items={} rfGen={} rfFact={} rfStore={}",
                r.fuelToBurn, r.itemsToSmelt, r.rfForGenerators, r.rfForFactory, r.rfForFactoryStorage);

        if (r.fuelToBurn <= 0 && r.itemsToSmelt <= 0
                && r.rfForGenerators <= 0 && r.rfForFactoryStorage <= 0) return;

        if (r.fuelToBurn > 0 && !nr.network.generators.isEmpty()) {
            int totalGen = nr.network.generators.size();
            int baseShare = r.fuelToBurn / totalGen;
            int remainder = r.fuelToBurn - baseShare * totalGen;
            for (int i = 0; i < totalGen; i++) {
                int share = baseShare + (i < remainder ? 1 : 0);
                if (share > 0) {
                    FurnaceFuelHandler.burnFuelIn(nr.network.generators.get(i), share, level);
                }
            }
        }

        if (r.rfForFactory > 0 && !nr.network.factories.isEmpty()) {
            int totalFact = nr.network.factories.size();
            int baseRf = r.rfForFactory / totalFact;
            int rfRemainder = r.rfForFactory - baseRf * totalFact;
            for (int i = 0; i < totalFact; i++) {
                int rfShare = baseRf + (i < rfRemainder ? 1 : 0);
                nr.network.factories.get(i).setEnergy(nr.network.factories.get(i).getEnergy() + rfShare);
                nr.network.factories.get(i).setChanged();
            }
        }

        if (r.itemsToSmelt > 0 && !nr.network.factories.isEmpty()) {
            int totalFact = nr.network.factories.size();
            int baseItems = r.itemsToSmelt / totalFact;
            int itemsRemainder = r.itemsToSmelt - baseItems * totalFact;
            for (int i = 0; i < totalFact; i++) {
                int share = baseItems + (i < itemsRemainder ? 1 : 0);
                if (share > 0) {
                    applyFactorySmelt(nr.network.factories.get(i), level, share, 0);
                }
            }
        }

        if (r.rfForGenerators > 0 && !nr.network.generators.isEmpty()) {
            int totalGen = nr.network.generators.size();
            int baseRf = r.rfForGenerators / totalGen;
            int rfRemainder = r.rfForGenerators - baseRf * totalGen;
            for (int i = 0; i < totalGen; i++) {
                int share = baseRf + (i < rfRemainder ? 1 : 0);
                BlockIronFurnaceTileBase gen = nr.network.generators.get(i);
                int space = gen.getCapacity() - gen.getEnergy();
                if (space > 0 && share > 0) {
                    gen.setEnergy(gen.getEnergy() + Math.min(space, share));
                    gen.setChanged();
                }
            }
        }

        if (r.rfForFactoryStorage > 0 && !nr.network.factories.isEmpty()) {
            int totalFact = nr.network.factories.size();
            int baseRf = r.rfForFactoryStorage / totalFact;
            int rfRemainder = r.rfForFactoryStorage - baseRf * totalFact;
            for (int i = 0; i < totalFact; i++) {
                int share = baseRf + (i < rfRemainder ? 1 : 0);
                BlockIronFurnaceTileBase factory = nr.network.factories.get(i);
                int space = factory.getCapacity() - factory.getEnergy();
                if (space > 0 && share > 0) {
                    factory.setEnergy(factory.getEnergy() + Math.min(space, share));
                    factory.setChanged();
                }
            }
        }

        for (BlockIronFurnaceTileBase gen : nr.network.generators) {
            CatchupDedup.mark(gen.getBlockPos());
        }
        for (BlockIronFurnaceTileBase fact : nr.network.factories) {
            CatchupDedup.mark(fact.getBlockPos());
        }
        if (!nr.network.factories.isEmpty()) {
            CatchupDedup.mark(nr.network.factories.get(0).getBlockPos());
        }
    }

    public static void applyGeneratorOnly(
            BlockIronFurnaceTileBase genTile, Level level, SimulationResult simResult) {
        if (simResult.fuelToBurn <= 0 && simResult.rfForGenerators <= 0) return;
        if (simResult.fuelToBurn > 0) {
            FurnaceFuelHandler.burnFuelIn(genTile, simResult.fuelToBurn, level);
        }
        if (simResult.rfForGenerators > 0) {
            int space = genTile.getCapacity() - genTile.getEnergy();
            if (space > 0) {
                genTile.setEnergy(genTile.getEnergy() + Math.min(space, simResult.rfForGenerators));
                genTile.setChanged();
            }
        }
        AbstractCatchupHandler.sendChatDebug(level, genTile.getBlockPos(),
                "Generator", simResult.effectiveTicks,
                simResult.fuelToBurn, simResult.rfForGenerators,
                0, 0, genTile.getEnergy() > 0);
    }

    public static void applyFactoryOnly(
            BlockIronFurnaceTileBase factoryTile, Level level, SimulationResult simResult) {
        if (simResult.itemsToSmelt <= 0 && simResult.rfForFactory <= 0) return;
        applyFactorySmelt(factoryTile, level, simResult.itemsToSmelt, simResult.rfForFactory);
    }

    public static void applyResult(
            BlockIronFurnaceTileBase genTile, BlockIronFurnaceTileBase factoryTile,
            Level level, BlockPos pos, SimulationResult simResult) {

        if (simResult.fuelToBurn <= 0 && simResult.itemsToSmelt <= 0
                && simResult.rfForGenerators <= 0 && simResult.rfForFactoryStorage <= 0) return;

        if (simResult.fuelToBurn > 0 && genTile != null) {
            FurnaceFuelHandler.burnFuelIn(genTile, simResult.fuelToBurn, level);
        }
        if (simResult.itemsToSmelt > 0 && factoryTile != null) {
            applyFactorySmelt(factoryTile, level, simResult.itemsToSmelt, simResult.rfForFactory);
        }
        if (simResult.rfForGenerators > 0 && genTile != null) {
            int space = genTile.getCapacity() - genTile.getEnergy();
            if (space > 0) {
                genTile.setEnergy(genTile.getEnergy() + Math.min(space, simResult.rfForGenerators));
                genTile.setChanged();
            }
        }
        if (simResult.rfForFactoryStorage > 0 && factoryTile != null) {
            int space = factoryTile.getCapacity() - factoryTile.getEnergy();
            if (space > 0) {
                factoryTile.setEnergy(factoryTile.getEnergy() + Math.min(space, simResult.rfForFactoryStorage));
                factoryTile.setChanged();
            }
        }
    }

    private static void applyFactorySmelt(
            BlockIronFurnaceTileBase factoryTile, Level level,
            int itemsToSmelt, int rfBudget) {

        IronFurnaceAccessor acc = (IronFurnaceAccessor) factoryTile;
        int[] inputSlots = NetworkDataCollector.getFactoryInputSlots(factoryTile);
        int slotCount = inputSlots.length;

        acc.invokeAutoFactoryIO();

        int remaining = itemsToSmelt;
        int rfSpent = 0;
        int smeltedCount = 0;
        int consecutiveFails = 0;

        while (remaining > 0) {
            boolean anyWorked = false;
            for (int i = 0; i < slotCount; i++) {
                int slot = inputSlots[i];
                ItemStack input = factoryTile.inventory.get(slot);
                if (input.isEmpty()) {
                    acc.invokeAutoFactoryIO();
                    input = factoryTile.inventory.get(slot);
                    if (input.isEmpty()) continue;
                }

                int outputSlot = slot + 6;
                ItemStack out = factoryTile.inventory.get(outputSlot);
                if (!out.isEmpty() && out.getCount() >= out.getMaxStackSize()) {
                    HopperHelper.pushFactoryOutputBelow(factoryTile, level);
                    out = factoryTile.inventory.get(outputSlot);
                    if (!out.isEmpty() && out.getCount() >= out.getMaxStackSize()) continue;
                }

                factoryTile.usedRF[i] = 0.0;
                factoryTile.factoryCookTime[i] = 0;

                Optional<? extends net.minecraft.world.item.crafting.AbstractCookingRecipe> recipeOpt =
                        acc.invokeGetRecipeFactory(slot, input);
                if (recipeOpt.isEmpty()) continue;

                int rfPerItem = recipeOpt.get().getCookingTime() * 20;
                ItemStack greenAug = factoryTile.inventory.get(4);
                boolean hasSpeed = !greenAug.isEmpty() && greenAug.getItem() instanceof ItemAugmentSpeed;
                boolean hasFuel = !greenAug.isEmpty() && greenAug.getItem() instanceof ItemAugmentFuel;
                if (hasSpeed) rfPerItem *= 2;
                if (hasFuel) rfPerItem /= 2;

                input.shrink(1);
                if (input.isEmpty()) {
                    factoryTile.inventory.set(slot, ItemStack.EMPTY);
                }

                ItemStack result = recipeOpt.get().getResultItem(factoryTile.getLevel().registryAccess());
                if (!result.isEmpty()) {
                    ItemStack currentOut = factoryTile.inventory.get(outputSlot);
                    if (currentOut.isEmpty()) {
                        factoryTile.inventory.set(outputSlot, result.copy());
                    } else if (ItemStack.isSameItemSameTags(currentOut, result)
                            && currentOut.getCount() < currentOut.getMaxStackSize()) {
                        currentOut.grow(result.getCount());
                    }
                }

                int drain = Math.min(rfPerItem, factoryTile.getEnergy());
                factoryTile.setEnergy(factoryTile.getEnergy() - drain);
                rfSpent += drain;
                remaining--;
                smeltedCount++;
                anyWorked = true;
                consecutiveFails = 0;
                factoryTile.setChanged();

                if (remaining <= 0) break;
            }
            if (!anyWorked) {
                consecutiveFails++;
                if (consecutiveFails >= 3) break;
                acc.invokeAutoFactoryIO();
            }
        }

        AbstractCatchupHandler.sendChatDebug(level, factoryTile.getBlockPos(),
                "Factory", 0, 0,
                smeltedCount, 0, 0, true);
    }
}
