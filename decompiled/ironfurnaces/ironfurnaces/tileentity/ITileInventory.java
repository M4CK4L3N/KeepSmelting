/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.core.Direction
 *  net.minecraft.world.entity.player.Inventory
 *  net.minecraft.world.entity.player.Player
 *  net.minecraft.world.inventory.AbstractContainerMenu
 *  net.minecraft.world.item.ItemStack
 */
package ironfurnaces.tileentity;

import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;

public interface ITileInventory {
    public int[] IgetSlotsForFace(Direction var1);

    public boolean IcanExtractItem(int var1, ItemStack var2, Direction var3);

    public String IgetName();

    public boolean IisItemValidForSlot(int var1, ItemStack var2);

    public AbstractContainerMenu IcreateMenu(int var1, Inventory var2, Player var3);
}

