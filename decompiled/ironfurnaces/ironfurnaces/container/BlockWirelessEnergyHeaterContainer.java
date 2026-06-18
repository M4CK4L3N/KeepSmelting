/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.core.BlockPos
 *  net.minecraft.world.Container
 *  net.minecraft.world.entity.player.Inventory
 *  net.minecraft.world.entity.player.Player
 *  net.minecraft.world.inventory.AbstractContainerMenu
 *  net.minecraft.world.inventory.DataSlot
 *  net.minecraft.world.inventory.MenuType
 *  net.minecraft.world.inventory.Slot
 *  net.minecraft.world.item.ItemStack
 *  net.minecraft.world.level.Level
 *  net.minecraftforge.api.distmarker.Dist
 *  net.minecraftforge.api.distmarker.OnlyIn
 *  net.minecraftforge.common.capabilities.ForgeCapabilities
 *  net.minecraftforge.energy.IEnergyStorage
 *  net.minecraftforge.items.IItemHandler
 *  net.minecraftforge.items.SlotItemHandler
 *  net.minecraftforge.items.wrapper.InvWrapper
 */
package ironfurnaces.container;

import ironfurnaces.container.slots.SlotHeater;
import ironfurnaces.energy.FEnergyStorage;
import ironfurnaces.init.Registration;
import ironfurnaces.items.ItemHeater;
import ironfurnaces.tileentity.BlockWirelessEnergyHeaterTile;
import net.minecraft.core.BlockPos;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.energy.IEnergyStorage;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.SlotItemHandler;
import net.minecraftforge.items.wrapper.InvWrapper;

public class BlockWirelessEnergyHeaterContainer
extends AbstractContainerMenu {
    protected BlockWirelessEnergyHeaterTile te;
    protected Player playerEntity;
    protected IItemHandler playerInventory;
    protected final Level world;

    public BlockWirelessEnergyHeaterContainer(int windowId, Level world, BlockPos pos, Inventory playerInventory, Player player) {
        super((MenuType)Registration.HEATER_CONTAINER.get(), windowId);
        this.te = (BlockWirelessEnergyHeaterTile)world.m_7702_(pos);
        this.playerEntity = player;
        this.playerInventory = new InvWrapper((Container)playerInventory);
        this.world = playerInventory.f_35978_.m_9236_();
        this.trackPower();
        this.m_38897_(new SlotHeater(this.te, 0, 80, 37));
        this.layoutPlayerInventorySlots(8, 84);
    }

    public int getEnergy() {
        return this.te.getCapability(ForgeCapabilities.ENERGY).map(IEnergyStorage::getEnergyStored).orElse(0);
    }

    public int getMaxEnergy() {
        return this.te.getCapability(ForgeCapabilities.ENERGY).map(IEnergyStorage::getMaxEnergyStored).orElse(0);
    }

    private void trackPower() {
        this.m_38895_(new DataSlot(){

            public int m_6501_() {
                return BlockWirelessEnergyHeaterContainer.this.getMaxEnergy() & 0xFFFF;
            }

            public void m_6422_(int value) {
                BlockWirelessEnergyHeaterContainer.this.te.getCapability(ForgeCapabilities.ENERGY).ifPresent(h -> {
                    int capacity = h.getMaxEnergyStored() & 0xFFFF0000;
                    ((FEnergyStorage)((Object)h)).setCapacity(capacity + (value & 0xFFFF));
                });
            }
        });
        this.m_38895_(new DataSlot(){

            public int m_6501_() {
                return BlockWirelessEnergyHeaterContainer.this.getMaxEnergy() >> 16 & 0xFFFF;
            }

            public void m_6422_(int value) {
                BlockWirelessEnergyHeaterContainer.this.te.getCapability(ForgeCapabilities.ENERGY).ifPresent(h -> {
                    int capacity = h.getMaxEnergyStored() & 0xFFFF;
                    ((FEnergyStorage)((Object)h)).setCapacity(capacity | value << 16);
                });
            }
        });
        this.m_38895_(new DataSlot(){

            public int m_6501_() {
                return BlockWirelessEnergyHeaterContainer.this.getEnergy() & 0xFFFF;
            }

            public void m_6422_(int value) {
                BlockWirelessEnergyHeaterContainer.this.te.getCapability(ForgeCapabilities.ENERGY).ifPresent(h -> {
                    int energyStored = h.getEnergyStored() & 0xFFFF0000;
                    ((FEnergyStorage)((Object)h)).setEnergy(energyStored + (value & 0xFFFF));
                });
            }
        });
        this.m_38895_(new DataSlot(){

            public int m_6501_() {
                return BlockWirelessEnergyHeaterContainer.this.getEnergy() >> 16 & 0xFFFF;
            }

            public void m_6422_(int value) {
                BlockWirelessEnergyHeaterContainer.this.te.getCapability(ForgeCapabilities.ENERGY).ifPresent(h -> {
                    int energyStored = h.getEnergyStored() & 0xFFFF;
                    ((FEnergyStorage)((Object)h)).setEnergy(energyStored | value << 16);
                });
            }
        });
    }

    @OnlyIn(value=Dist.CLIENT)
    public int getEnergyScaled(int pixels) {
        int i = this.getEnergy();
        int j = this.getMaxEnergy();
        return j != 0 && i != 0 ? i * pixels / j : 0;
    }

    private int addSlotRange(IItemHandler handler, int index, int x, int y, int amount, int dx) {
        for (int i = 0; i < amount; ++i) {
            this.m_38897_((Slot)new SlotItemHandler(handler, index, x, y));
            x += dx;
            ++index;
        }
        return index;
    }

    private int addSlotBox(IItemHandler handler, int index, int x, int y, int horAmount, int dx, int verAmount, int dy) {
        for (int j = 0; j < verAmount; ++j) {
            index = this.addSlotRange(handler, index, x, y, horAmount, dx);
            y += dy;
        }
        return index;
    }

    private void layoutPlayerInventorySlots(int leftCol, int topRow) {
        this.addSlotBox(this.playerInventory, 9, leftCol, topRow, 9, 18, 3, 18);
        this.addSlotRange(this.playerInventory, 0, leftCol, topRow += 58, 9, 18);
    }

    public ItemStack m_7648_(Player playerIn, int index) {
        ItemStack itemstack = ItemStack.f_41583_;
        Slot slot = (Slot)this.f_38839_.get(index);
        if (slot != null && slot.m_6657_()) {
            ItemStack itemstack1 = slot.m_7993_();
            itemstack = itemstack1.m_41777_();
            if (!(itemstack.m_41720_() instanceof ItemHeater)) {
                return ItemStack.f_41583_;
            }
            if (index < 1 ? !this.m_38903_(itemstack1, 1, this.f_38839_.size(), true) : !this.m_38903_(itemstack1, 0, 1, false)) {
                return ItemStack.f_41583_;
            }
            if (itemstack1.m_41619_()) {
                slot.m_5852_(ItemStack.f_41583_);
            } else {
                slot.m_6654_();
            }
        }
        return itemstack;
    }

    public boolean m_6875_(Player p_38874_) {
        return this.te.m_6542_(p_38874_);
    }
}

