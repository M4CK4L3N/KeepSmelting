package com.keepsmelting.internal.ironfurnaces.util;

import ironfurnaces.tileentity.furnaces.BlockIronFurnaceTileBase;
import net.minecraft.core.BlockPos;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * Хелперы для взаимодействия с контейнерами (сундуки, воронки) рядом с печами.
 */
public class HopperHelper {

    private HopperHelper() {}

    /** Выталкивает предметы из выходных слотов завода в контейнер снизу. */
    public static void pushFactoryOutputBelow(BlockIronFurnaceTileBase factoryTile, Level level) {
        BlockPos below = factoryTile.getBlockPos().below();
        if (!level.isLoaded(below)) return;
        if (!(level.getBlockEntity(below) instanceof Container container)) return;

        int[] outputSlots = new int[]{13, 14, 15, 16, 17, 18};
        for (int slot : outputSlots) {
            ItemStack out = factoryTile.inventory.get(slot);
            if (out.isEmpty()) continue;

            for (int i = 0; i < container.getContainerSize(); i++) {
                ItemStack dest = container.getItem(i);
                if (dest.isEmpty()) {
                    ItemStack toPush = out.copy();
                    int toPushCount = Math.min(toPush.getCount(), toPush.getMaxStackSize());
                    toPush.setCount(toPushCount);
                    out.shrink(toPushCount);
                    container.setItem(i, toPush);
                    if (out.isEmpty()) factoryTile.inventory.set(slot, ItemStack.EMPTY);
                    container.setChanged();
                    factoryTile.setChanged();
                    return;
                } else if (ItemStack.isSameItemSameTags(out, dest)
                        && dest.getCount() < dest.getMaxStackSize()) {
                    int space = dest.getMaxStackSize() - dest.getCount();
                    int toMove = Math.min(out.getCount(), space);
                    out.shrink(toMove);
                    dest.grow(toMove);
                    if (out.isEmpty()) factoryTile.inventory.set(slot, ItemStack.EMPTY);
                    container.setChanged();
                    factoryTile.setChanged();
                    return;
                }
            }
        }
    }
}
