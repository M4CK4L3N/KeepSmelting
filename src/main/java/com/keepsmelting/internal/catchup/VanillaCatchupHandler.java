package com.keepsmelting.internal.catchup;

import com.keepsmelting.KeepSmelting;
import com.keepsmelting.KeepSmeltingConfig;
import com.keepsmelting.api.CatchupHandlerRegistry;
import com.keepsmelting.api.catchup.AbstractCatchupHandler;
import com.keepsmelting.api.IFurnaceCatchupHandler;
import com.keepsmelting.mixin.IFurnaceAccessor;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.level.block.AbstractFurnaceBlock;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.ForgeHooks;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.minecraft.core.NonNullList;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.AABB;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Optional;

/**
 * Catchup handler for vanilla AbstractFurnaceBlockEntity (furnace, smoker, blast furnace).
 * Uses adaptive batch — O(events), not O(ticks).
 */
public class VanillaCatchupHandler extends AbstractCatchupHandler {

    public static final VanillaCatchupHandler INSTANCE = new VanillaCatchupHandler();

    @Override
    public void applyCatchup(net.minecraft.world.level.block.entity.BlockEntity tile, long elapsed, Level level, BlockPos pos) {
        AbstractFurnaceBlockEntity furnace = (AbstractFurnaceBlockEntity) tile;
        IFurnaceAccessor acc = (IFurnaceAccessor) furnace;
        if (!acc.callIsLit()) return;

        NonNullList<ItemStack> items = acc.getItems();
        int outputBefore = items.get(2).getCount();
        int fuelBefore = items.get(1).getCount();
        int progressBefore = acc.getCookingProgress();
        long remaining = elapsed;

        // Batch hopper IO
        {
            int hopperOps = Math.max(1, (int)(elapsed / 8));
            if (!items.get(2).isEmpty()) {
                VanillaHopperIO.pushToBelow(furnace, (ServerLevel) level, pos, hopperOps);
                items = acc.getItems();
            }
            int space = furnace.getMaxStackSize() - items.get(0).getCount();
            if (space > 0) {
                VanillaHopperIO.fillInputFromAbove(furnace, (ServerLevel) level, pos, Math.min(hopperOps, space));
                items = acc.getItems();
            }
            if (items.get(1).isEmpty() && acc.getLitTime() <= 0) {
                VanillaHopperIO.pullFuelFromSides(furnace, (ServerLevel) level, pos);
                items = acc.getItems();
                if (!items.get(1).isEmpty()) {
                    int bt = ForgeHooks.getBurnTime(items.get(1), RecipeType.SMELTING);
                    if (bt > 0) {
                        ItemStack f = items.get(1);
                        acc.setLitTime(bt);
                        f.shrink(1);
                        if (f.isEmpty()) items.set(1, f.getCraftingRemainingItem());
                        BlockState bs = level.getBlockState(pos);
                        if (!bs.getValue(AbstractFurnaceBlock.LIT)) {
                            level.setBlock(pos, bs.setValue(AbstractFurnaceBlock.LIT, true), 3);
                        }
                    }
                }
            }
        }

        ItemStack input = items.get(0);
        ItemStack output = items.get(2);
        ItemStack fuel = items.get(1);

        if (input.isEmpty()) return;
        if (!output.isEmpty() && output.getCount() >= furnace.getMaxStackSize()) return;

        Recipe<?> recipe = VanillaHopperIO.findRecipe(furnace, level);
        if (recipe == null) return;
        if (!acc.callCanBurn(level.registryAccess(), recipe, items, furnace.getMaxStackSize())) return;

        int cookTotal = acc.getCookingTotalTime();
        if (cookTotal <= 0) return;
        int litDur = acc.getLitDuration();
        if (litDur <= 0) return;

        long fuelTicks;
        if (fuel.isEmpty()) {
            fuelTicks = acc.getLitTime();
        } else {
            fuelTicks = (long) (fuel.getCount() - 1) * litDur + acc.getLitTime();
        }

        long cookTicksNeeded = (long) (input.getCount() - 1) * cookTotal
                + (cookTotal - acc.getCookingProgress());

        long applyTicks = Math.min(remaining, Math.min(fuelTicks, cookTicksNeeded));
        if (applyTicks <= 0) return;

        int inputCountBefore = input.getCount();

        VanillaHopperIO.applyFuelTime(acc, fuel, applyTicks, litDur);
        VanillaHopperIO.applyCookTime(level, furnace, acc, recipe, input, applyTicks, cookTotal);

        int itemsProduced = inputCountBefore - items.get(0).getCount();
        furnace.setChanged();

        if (!acc.callIsLit()) {
            BlockState bs = level.getBlockState(pos);
            if (bs.hasProperty(AbstractFurnaceBlock.LIT)) {
                level.setBlock(pos, bs.setValue(AbstractFurnaceBlock.LIT, false), 3);
            }
            furnace.setChanged();
        }

        if (!acc.getItems().get(2).isEmpty()) {
            VanillaHopperIO.pushToBelow(furnace, (ServerLevel) level, pos, Integer.MAX_VALUE);
        }

        // Debug
        if (KeepSmeltingConfig.COMMON.debugMode.get() != KeepSmeltingConfig.DebugMode.OFF) {
            items = acc.getItems();
            boolean lit = acc.callIsLit();
            int progressDelta = acc.getCookingProgress() - progressBefore;
            if (progressDelta == 0 && itemsProduced == 0 && fuelBefore == items.get(1).getCount()) return;

            int fuelUsed = fuelBefore - items.get(1).getCount();
            String fuelStr = fuelUsed > 0 ? String.format("fuel: -%d", fuelUsed) : "fuel: 0";
            String itemStr = itemsProduced > 0 ? String.format("smelted: §a%d", itemsProduced) : "smelted: 0";
            Component msg = Component.literal(
                    String.format("§7[§6KeepSmelting§7] §f%s §7| §e%d§7t§r | %s §7| %s §7| §7lit=%s",
                            pos.toShortString(), elapsed, itemStr, fuelStr, lit));
            if (KeepSmeltingConfig.COMMON.debugMode.get() == KeepSmeltingConfig.DebugMode.LOG) {
                KeepSmelting.LOGGER.info(msg.getString());
            } else {
                sendToNearbyPlayers((ServerLevel) level, pos, msg);
            }
        }
    }

    private static void sendToNearbyPlayers(ServerLevel level, BlockPos pos, Component msg) {
        AABB box = new AABB(pos).inflate(16);
        List<ServerPlayer> players = level.getEntitiesOfClass(ServerPlayer.class, box);
        for (ServerPlayer p : players) {
            p.sendSystemMessage(msg);
        }
    }
}
