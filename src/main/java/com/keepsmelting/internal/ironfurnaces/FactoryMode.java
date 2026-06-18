package com.keepsmelting.internal.ironfurnaces;

import com.keepsmelting.internal.catchup.AbstractCatchupHandler;
import com.keepsmelting.mixin.ironfurnaces.IronFurnaceAccessor;
import ironfurnaces.items.augments.ItemAugmentFuel;
import ironfurnaces.items.augments.ItemAugmentSpeed;
import ironfurnaces.tileentity.furnaces.BlockIronFurnaceTileBase;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import java.util.Optional;

/**
 * Factory mode catchup — adaptive batch O(events).
 * Pre-computes per-slot RF costs, batches ticks until next item completion or RF exhaustion.
 */
public class FactoryMode {

    private static final int[] FACTORY_INPUT = new int[]{7, 8, 9, 10, 11, 12};

    public static void apply(BlockIronFurnaceTileBase tile, long elapsed, Level level, BlockPos pos) {
        IronFurnaceAccessor acc = (IronFurnaceAccessor) tile;

        int energyBefore = tile.getEnergy();
        int[] outputBefore = new int[6];
        for (int i = 0; i < 6; i++) {
            outputBefore[i] = tile.inventory.get(FACTORY_INPUT[i] + 6).getCount();
        }

        ItemStack greenAug = tile.inventory.get(4);
        boolean hasSpeed = !greenAug.isEmpty() && greenAug.getItem() instanceof ItemAugmentSpeed;
        boolean hasFuel = !greenAug.isEmpty() && greenAug.getItem() instanceof ItemAugmentFuel;

        // Pull all RF from neighbor generators
        int pulledRf = GeneratorMode.pullAllRFFromNeighborGenerators(tile, level, pos);

        int rfConsumed = 0;
        boolean anyWorked = false;
        long remaining = elapsed;

        int[] slotEnergyRecipe = new int[6];
        int[] slotRfPerTick = new int[6];
        int[] slotCookTotal = new int[6];
        boolean[] slotActive = new boolean[6];
        net.minecraft.world.item.crafting.AbstractCookingRecipe[] slotRecipe =
                new net.minecraft.world.item.crafting.AbstractCookingRecipe[6];

        for (int i = 0; i < 6; i++) {
            int slot = FACTORY_INPUT[i];
            ItemStack input = tile.inventory.get(slot);
            if (input.isEmpty()) continue;

            int outputSlot = slot + 6;
            ItemStack out = tile.inventory.get(outputSlot);
            if (!out.isEmpty() && out.getCount() >= out.getMaxStackSize()) continue;

            int totalCook = acc.invokeGetFactoryCookTime(slot);
            if (totalCook <= 0) continue;

            int cookTotal = tile.factoryTotalCookTime[i];
            if (cookTotal <= 0) cookTotal = totalCook;
            slotCookTotal[i] = cookTotal;

            Optional<? extends net.minecraft.world.item.crafting.AbstractCookingRecipe> recipeOpt =
                    acc.invokeGetRecipeFactory(slot, input);
            if (recipeOpt.isEmpty()) continue;
            net.minecraft.world.item.crafting.AbstractCookingRecipe recipe = recipeOpt.get();
            if (!acc.invokeCanFactorySmelt(recipe, slot)) continue;

            int recipeCookTime = recipe.getCookingTime();
            int energyRecipe = recipeCookTime * 20;
            if (hasSpeed) energyRecipe *= 2;
            if (hasFuel) energyRecipe /= 2;
            slotEnergyRecipe[i] = energyRecipe;

            int rfPerTick = Math.max(1, energyRecipe / Math.max(1, cookTotal));
            slotRfPerTick[i] = rfPerTick;
            slotRecipe[i] = recipe;
            slotActive[i] = true;
        }

        while (remaining > 0) {
            int totalRfPerTick = 0;
            long minToEvent = remaining;
            for (int i = 0; i < 6; i++) {
                if (!slotActive[i]) continue;
                if (tile.getEnergy() < slotEnergyRecipe[i] && tile.factoryCookTime[i] <= 0) {
                    slotActive[i] = false;
                    continue;
                }
                if (tile.getEnergy() < slotRfPerTick[i]) continue;
                totalRfPerTick += slotRfPerTick[i];

                long need = slotCookTotal[i] - tile.factoryCookTime[i];
                if (need > 0) {
                    minToEvent = Math.min(minToEvent, need);
                }
            }

            if (totalRfPerTick <= 0) break;

            long maxRfTicks = tile.getEnergy() / Math.max(1, totalRfPerTick);
            long batch = Math.min(minToEvent, maxRfTicks);
            if (batch <= 0) batch = 1;
            batch = Math.min(batch, remaining);

            int rfBatchCost = 0;
            for (int i = 0; i < 6; i++) {
                if (!slotActive[i]) continue;
                if (tile.getEnergy() < slotRfPerTick[i]) continue;
                rfBatchCost += slotRfPerTick[i] * (int) batch;
                tile.usedRF[i] += slotRfPerTick[i] * (int) batch;
                tile.factoryCookTime[i] += (int) batch;
            }

            tile.setEnergy(tile.getEnergy() - rfBatchCost);
            rfConsumed += rfBatchCost;
            remaining -= batch;
            anyWorked = true;

            for (int i = 0; i < 6; i++) {
                if (!slotActive[i]) continue;
                if (tile.factoryCookTime[i] < slotCookTotal[i]) continue;

                tile.factoryCookTime[i] = 0;
                int slot = FACTORY_INPUT[i];

                if (tile.usedRF[i] < (double) slotEnergyRecipe[i]) {
                    double diff = (double) slotEnergyRecipe[i] - tile.usedRF[i];
                    int actualDrain = Math.min(tile.getEnergy(), (int) diff);
                    tile.setEnergy(tile.getEnergy() - actualDrain);
                    rfConsumed += actualDrain;
                }
                tile.usedRF[i] = 0.0;
                tile.factoryTotalCookTime[i] = acc.invokeGetFactoryCookTime(slot);
                acc.invokeFactorySmelt(slotRecipe[i], slot);
                tile.setChanged();

                ItemStack input = tile.inventory.get(slot);
                if (input.isEmpty()) {
                    tile.setChanged();
                    acc.invokeAutoFactoryIO();
                    input = tile.inventory.get(slot);
                }
                if (!input.isEmpty()) {
                    int outputSlot = slot + 6;
                    ItemStack out = tile.inventory.get(outputSlot);
                    if (out.isEmpty() || out.getCount() < out.getMaxStackSize()) {
                        int newTotal = acc.invokeGetFactoryCookTime(slot);
                        if (newTotal > 0) {
                            Optional<? extends net.minecraft.world.item.crafting.AbstractCookingRecipe> newRecipe =
                                    acc.invokeGetRecipeFactory(slot, input);
                            if (newRecipe.isPresent() && acc.invokeCanFactorySmelt(newRecipe.get(), slot)) {
                                int newRC = newRecipe.get().getCookingTime();
                                int newER = newRC * 20;
                                if (hasSpeed) newER *= 2;
                                if (hasFuel) newER /= 2;
                                slotEnergyRecipe[i] = newER;
                                slotRfPerTick[i] = Math.max(1, newER / Math.max(1, newTotal));
                                slotCookTotal[i] = newTotal;
                                slotRecipe[i] = newRecipe.get();
                                slotActive[i] = true;
                                continue;
                            }
                        }
                    }
                }
                slotActive[i] = false;
            }
        }

        if (anyWorked) {
            tile.setChanged();
            int finalOutput = calcTotalOutput(tile, outputBefore, 6);
            int rfPerItem = rfConsumed > 0 && finalOutput > 0 ? rfConsumed / finalOutput : 0;
            String itemStr = finalOutput > 0
                    ? String.format("smelted: §a%d", finalOutput)
                    : "smelted: 0";
            String rfStr = String.format("rf: -%d (pull=%d, %dRF/item)",
                    rfConsumed, pulledRf, rfPerItem);
            Component msg = Component.literal(
                    String.format("§7[§6KeepSmelting§7] §e[Factory] §f%s §7| §e%d§7t§r | %s §7| %s",
                            pos.toShortString(), elapsed,
                            itemStr, rfStr));
            AbstractCatchupHandler.sendChatDebug(level, pos, "Factory", elapsed,
                    0, finalOutput, 0, 0, true);
        }
    }

    private static int calcTotalOutput(BlockIronFurnaceTileBase tile, int[] before, int slots) {
        int total = 0;
        for (int i = 0; i < slots; i++) {
            total += tile.inventory.get(FACTORY_INPUT[i] + 6).getCount() - before[i];
        }
        return total;
    }
}
