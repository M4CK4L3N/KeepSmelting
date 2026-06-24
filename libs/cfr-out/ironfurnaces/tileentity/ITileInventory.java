/*
 * Decompiled with CFR.
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

