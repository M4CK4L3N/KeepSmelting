/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.core.BlockPos
 *  net.minecraft.network.chat.Component
 *  net.minecraft.world.Container
 *  net.minecraft.world.entity.player.Inventory
 *  net.minecraft.world.entity.player.Player
 *  net.minecraft.world.inventory.AbstractContainerMenu
 *  net.minecraft.world.inventory.DataSlot
 *  net.minecraft.world.inventory.MenuType
 *  net.minecraft.world.inventory.Slot
 *  net.minecraft.world.item.ItemStack
 *  net.minecraft.world.item.crafting.RecipeType
 *  net.minecraft.world.level.Level
 *  net.minecraftforge.common.capabilities.ForgeCapabilities
 *  net.minecraftforge.energy.IEnergyStorage
 *  net.minecraftforge.items.IItemHandler
 *  net.minecraftforge.items.SlotItemHandler
 *  net.minecraftforge.items.wrapper.InvWrapper
 */
package ironfurnaces.container.furnaces;

import ironfurnaces.container.slots.SlotIronFurnace;
import ironfurnaces.container.slots.SlotIronFurnaceAugmentBlue;
import ironfurnaces.container.slots.SlotIronFurnaceAugmentGreen;
import ironfurnaces.container.slots.SlotIronFurnaceAugmentRed;
import ironfurnaces.container.slots.SlotIronFurnaceFuel;
import ironfurnaces.container.slots.SlotIronFurnaceInput;
import ironfurnaces.container.slots.SlotIronFurnaceInputFactory;
import ironfurnaces.container.slots.SlotIronFurnaceInputGenerator;
import ironfurnaces.container.slots.SlotIronFurnaceOutputFactory;
import ironfurnaces.energy.FEnergyStorage;
import ironfurnaces.items.ItemHeater;
import ironfurnaces.items.augments.ItemAugmentBlasting;
import ironfurnaces.items.augments.ItemAugmentSmoking;
import ironfurnaces.tileentity.furnaces.BlockIronFurnaceTileBase;
import ironfurnaces.util.container.FactoryDataSlot;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.energy.IEnergyStorage;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.SlotItemHandler;
import net.minecraftforge.items.wrapper.InvWrapper;

public abstract class BlockIronFurnaceContainerBase
extends AbstractContainerMenu {
    protected BlockIronFurnaceTileBase te;
    protected Player playerEntity;
    protected IItemHandler playerInventory;
    protected final Level world;

    public BlockIronFurnaceContainerBase(MenuType<?> containerType, int windowId, Level world, BlockPos pos, Inventory playerInventory, Player player) {
        super(containerType, windowId);
        this.te = (BlockIronFurnaceTileBase)world.m_7702_(pos);
        this.playerEntity = player;
        this.playerInventory = new InvWrapper((Container)playerInventory);
        this.world = playerInventory.f_35978_.m_9236_();
        this.m_38897_(new SlotIronFurnaceInput(this.te, 0, 56, 17));
        this.m_38897_(new SlotIronFurnaceFuel(this.te, 1, 56, 53));
        this.m_38897_(new SlotIronFurnace(this.playerEntity, this.te, 2, 116, 35));
        this.m_38897_(new SlotIronFurnaceAugmentRed(this.te, 3, 26, 35));
        this.m_38897_(new SlotIronFurnaceAugmentGreen(this.te, 4, 80, 35));
        this.m_38897_(new SlotIronFurnaceAugmentBlue(this.te, 5, 134, 35));
        this.m_38897_(new SlotIronFurnaceInputGenerator(this.te, 6, 56, 40));
        this.m_38897_(new SlotIronFurnaceInputFactory(0, this.te, 7, 28, 6));
        this.m_38897_(new SlotIronFurnaceInputFactory(1, this.te, 8, 49, 6));
        this.m_38897_(new SlotIronFurnaceInputFactory(2, this.te, 9, 70, 6));
        this.m_38897_(new SlotIronFurnaceInputFactory(3, this.te, 10, 91, 6));
        this.m_38897_(new SlotIronFurnaceInputFactory(4, this.te, 11, 112, 6));
        this.m_38897_(new SlotIronFurnaceInputFactory(5, this.te, 12, 133, 6));
        this.m_38897_(new SlotIronFurnaceOutputFactory(0, this.playerEntity, this.te, 13, 28, 55));
        this.m_38897_(new SlotIronFurnaceOutputFactory(1, this.playerEntity, this.te, 14, 49, 55));
        this.m_38897_(new SlotIronFurnaceOutputFactory(2, this.playerEntity, this.te, 15, 70, 55));
        this.m_38897_(new SlotIronFurnaceOutputFactory(3, this.playerEntity, this.te, 16, 91, 55));
        this.m_38897_(new SlotIronFurnaceOutputFactory(4, this.playerEntity, this.te, 17, 112, 55));
        this.m_38897_(new SlotIronFurnaceOutputFactory(5, this.playerEntity, this.te, 18, 133, 55));
        this.layoutPlayerInventorySlots(8, 84);
        BlockIronFurnaceContainerBase.m_38869_((Container)this.te, (int)19);
        this.addDataSlots();
    }

    public void addDataSlots() {
        this.m_38895_(new DataSlot(){

            public int m_6501_() {
                return BlockIronFurnaceContainerBase.this.getAugmentGUI() ? 1 : 0;
            }

            public void m_6422_(int value) {
                BlockIronFurnaceContainerBase.this.te.furnaceSettings.set(10, value);
            }
        });
        this.m_38895_(new DataSlot(){

            public int m_6501_() {
                return BlockIronFurnaceContainerBase.this.getIsFurnace() ? 1 : 0;
            }

            public void m_6422_(int value) {
                if (value == 1) {
                    BlockIronFurnaceContainerBase.this.te.currentAugment[2] = 0;
                }
            }
        });
        this.m_38895_(new DataSlot(){

            public int m_6501_() {
                return BlockIronFurnaceContainerBase.this.getIsGenerator() ? 1 : 0;
            }

            public void m_6422_(int value) {
                if (value == 1) {
                    BlockIronFurnaceContainerBase.this.te.currentAugment[2] = 2;
                }
            }
        });
        this.m_38895_(new DataSlot(){

            public int m_6501_() {
                return BlockIronFurnaceContainerBase.this.getIsFactory() ? 1 : 0;
            }

            public void m_6422_(int value) {
                if (value == 1) {
                    BlockIronFurnaceContainerBase.this.te.currentAugment[2] = 1;
                }
            }
        });
        this.addEnergyData();
        this.addFurnaceData();
        this.addGeneratorData();
        this.addFactoryData();
    }

    public void addFurnaceData() {
        this.m_38895_(new DataSlot(){

            public int m_6501_() {
                return BlockIronFurnaceContainerBase.this.te.furnaceBurnTime & 0xFFFF;
            }

            public void m_6422_(int value) {
                int add = BlockIronFurnaceContainerBase.this.te.furnaceBurnTime & 0xFFFF0000;
                BlockIronFurnaceContainerBase.this.te.furnaceBurnTime = add + (value & 0xFFFF);
            }
        });
        this.m_38895_(new DataSlot(){

            public int m_6501_() {
                return BlockIronFurnaceContainerBase.this.te.furnaceBurnTime >> 16 & 0xFFFF;
            }

            public void m_6422_(int value) {
                int add = BlockIronFurnaceContainerBase.this.te.furnaceBurnTime & 0xFFFF;
                BlockIronFurnaceContainerBase.this.te.furnaceBurnTime = add | value << 16;
            }
        });
        this.m_38895_(new DataSlot(){

            public int m_6501_() {
                return BlockIronFurnaceContainerBase.this.te.recipesUsed & 0xFFFF;
            }

            public void m_6422_(int value) {
                int add = BlockIronFurnaceContainerBase.this.te.recipesUsed & 0xFFFF0000;
                BlockIronFurnaceContainerBase.this.te.recipesUsed = add + (value & 0xFFFF);
            }
        });
        this.m_38895_(new DataSlot(){

            public int m_6501_() {
                return BlockIronFurnaceContainerBase.this.te.recipesUsed >> 16 & 0xFFFF;
            }

            public void m_6422_(int value) {
                int add = BlockIronFurnaceContainerBase.this.te.recipesUsed & 0xFFFF;
                BlockIronFurnaceContainerBase.this.te.recipesUsed = add | value << 16;
            }
        });
        this.m_38895_(new DataSlot(){

            public int m_6501_() {
                return BlockIronFurnaceContainerBase.this.te.cookTime & 0xFFFF;
            }

            public void m_6422_(int value) {
                int add = BlockIronFurnaceContainerBase.this.te.cookTime & 0xFFFF0000;
                BlockIronFurnaceContainerBase.this.te.cookTime = add + (value & 0xFFFF);
            }
        });
        this.m_38895_(new DataSlot(){

            public int m_6501_() {
                return BlockIronFurnaceContainerBase.this.te.cookTime >> 16 & 0xFFFF;
            }

            public void m_6422_(int value) {
                int add = BlockIronFurnaceContainerBase.this.te.cookTime & 0xFFFF;
                BlockIronFurnaceContainerBase.this.te.cookTime = add | value << 16;
            }
        });
        this.m_38895_(new DataSlot(){

            public int m_6501_() {
                return BlockIronFurnaceContainerBase.this.te.totalCookTime & 0xFFFF;
            }

            public void m_6422_(int value) {
                int add = BlockIronFurnaceContainerBase.this.te.totalCookTime & 0xFFFF0000;
                BlockIronFurnaceContainerBase.this.te.totalCookTime = add + (value & 0xFFFF);
            }
        });
        this.m_38895_(new DataSlot(){

            public int m_6501_() {
                return BlockIronFurnaceContainerBase.this.te.totalCookTime >> 16 & 0xFFFF;
            }

            public void m_6422_(int value) {
                int add = BlockIronFurnaceContainerBase.this.te.totalCookTime & 0xFFFF;
                BlockIronFurnaceContainerBase.this.te.totalCookTime = add | value << 16;
            }
        });
    }

    public void addGeneratorData() {
        this.m_38895_(new DataSlot(){

            public int m_6501_() {
                return (int)BlockIronFurnaceContainerBase.this.te.generatorBurn & 0xFFFF;
            }

            public void m_6422_(int value) {
                int add = (int)BlockIronFurnaceContainerBase.this.te.generatorBurn & 0xFFFF0000;
                BlockIronFurnaceContainerBase.this.te.generatorBurn = add + (value & 0xFFFF);
            }
        });
        this.m_38895_(new DataSlot(){

            public int m_6501_() {
                return (int)BlockIronFurnaceContainerBase.this.te.generatorBurn >> 16 & 0xFFFF;
            }

            public void m_6422_(int value) {
                int add = (int)BlockIronFurnaceContainerBase.this.te.generatorBurn & 0xFFFF;
                BlockIronFurnaceContainerBase.this.te.generatorBurn = add | value << 16;
            }
        });
        this.m_38895_(new DataSlot(){

            public int m_6501_() {
                return BlockIronFurnaceContainerBase.this.te.generatorRecentRecipeRF & 0xFFFF;
            }

            public void m_6422_(int value) {
                int add = BlockIronFurnaceContainerBase.this.te.generatorRecentRecipeRF & 0xFFFF0000;
                BlockIronFurnaceContainerBase.this.te.generatorRecentRecipeRF = add + (value & 0xFFFF);
            }
        });
        this.m_38895_(new DataSlot(){

            public int m_6501_() {
                return BlockIronFurnaceContainerBase.this.te.generatorRecentRecipeRF >> 16 & 0xFFFF;
            }

            public void m_6422_(int value) {
                int add = BlockIronFurnaceContainerBase.this.te.generatorRecentRecipeRF & 0xFFFF;
                BlockIronFurnaceContainerBase.this.te.generatorRecentRecipeRF = add | value << 16;
            }
        });
    }

    public void addFactoryData() {
        int i;
        for (i = 0; i < this.te.factoryCookTime.length; ++i) {
            this.m_38895_(new FactoryDataSlot(i){

                public int m_6501_() {
                    return BlockIronFurnaceContainerBase.this.te.factoryCookTime[this.index] & 0xFFFF;
                }

                public void m_6422_(int value) {
                    int add = BlockIronFurnaceContainerBase.this.te.factoryCookTime[this.index] & 0xFFFF0000;
                    BlockIronFurnaceContainerBase.this.te.factoryCookTime[this.index] = add + (value & 0xFFFF);
                }
            });
            this.m_38895_(new FactoryDataSlot(i){

                public int m_6501_() {
                    return BlockIronFurnaceContainerBase.this.te.factoryCookTime[this.index] >> 16 & 0xFFFF;
                }

                public void m_6422_(int value) {
                    int add = BlockIronFurnaceContainerBase.this.te.factoryCookTime[this.index] & 0xFFFF;
                    BlockIronFurnaceContainerBase.this.te.factoryCookTime[this.index] = add | value << 16;
                }
            });
        }
        for (i = 0; i < this.te.factoryTotalCookTime.length; ++i) {
            this.m_38895_(new FactoryDataSlot(i){

                public int m_6501_() {
                    return BlockIronFurnaceContainerBase.this.te.factoryTotalCookTime[this.index] & 0xFFFF;
                }

                public void m_6422_(int value) {
                    int add = BlockIronFurnaceContainerBase.this.te.factoryTotalCookTime[this.index] & 0xFFFF0000;
                    BlockIronFurnaceContainerBase.this.te.factoryTotalCookTime[this.index] = add + (value & 0xFFFF);
                }
            });
            this.m_38895_(new FactoryDataSlot(i){

                public int m_6501_() {
                    return BlockIronFurnaceContainerBase.this.te.factoryTotalCookTime[this.index] >> 16 & 0xFFFF;
                }

                public void m_6422_(int value) {
                    int add = BlockIronFurnaceContainerBase.this.te.factoryTotalCookTime[this.index] & 0xFFFF;
                    BlockIronFurnaceContainerBase.this.te.factoryTotalCookTime[this.index] = add | value << 16;
                }
            });
        }
    }

    public int getEnergy() {
        return this.te.getCapability(ForgeCapabilities.ENERGY).map(IEnergyStorage::getEnergyStored).orElse(0);
    }

    public int getMaxEnergy() {
        return this.te.getCapability(ForgeCapabilities.ENERGY).map(IEnergyStorage::getMaxEnergyStored).orElse(0);
    }

    private void addEnergyData() {
        this.m_38895_(new DataSlot(){

            public int m_6501_() {
                return BlockIronFurnaceContainerBase.this.getMaxEnergy() & 0xFFFF;
            }

            public void m_6422_(int value) {
                BlockIronFurnaceContainerBase.this.te.getCapability(ForgeCapabilities.ENERGY).ifPresent(h -> {
                    int capacity = h.getMaxEnergyStored() & 0xFFFF0000;
                    ((FEnergyStorage)((Object)h)).setCapacity(capacity + (value & 0xFFFF));
                });
            }
        });
        this.m_38895_(new DataSlot(){

            public int m_6501_() {
                return BlockIronFurnaceContainerBase.this.getMaxEnergy() >> 16 & 0xFFFF;
            }

            public void m_6422_(int value) {
                BlockIronFurnaceContainerBase.this.te.getCapability(ForgeCapabilities.ENERGY).ifPresent(h -> {
                    int capacity = h.getMaxEnergyStored() & 0xFFFF;
                    ((FEnergyStorage)((Object)h)).setCapacity(capacity | value << 16);
                });
            }
        });
        this.m_38895_(new DataSlot(){

            public int m_6501_() {
                return BlockIronFurnaceContainerBase.this.getEnergy() & 0xFFFF;
            }

            public void m_6422_(int value) {
                BlockIronFurnaceContainerBase.this.te.getCapability(ForgeCapabilities.ENERGY).ifPresent(h -> {
                    int energyStored = h.getEnergyStored() & 0xFFFF0000;
                    ((FEnergyStorage)((Object)h)).setEnergy(energyStored + (value & 0xFFFF));
                });
            }
        });
        this.m_38895_(new DataSlot(){

            public int m_6501_() {
                return BlockIronFurnaceContainerBase.this.getEnergy() >> 16 & 0xFFFF;
            }

            public void m_6422_(int value) {
                BlockIronFurnaceContainerBase.this.te.getCapability(ForgeCapabilities.ENERGY).ifPresent(h -> {
                    int energyStored = h.getEnergyStored() & 0xFFFF;
                    ((FEnergyStorage)((Object)h)).setEnergy(energyStored | value << 16);
                });
            }
        });
    }

    public boolean m_6875_(Player player) {
        return this.te.m_6542_(player);
    }

    public int getTier() {
        return this.te.getTier();
    }

    public boolean isAutoSplit() {
        return this.te.isAutoSplit();
    }

    public int getRedstoneMode() {
        return this.te.getRedstoneSetting();
    }

    public int getComSub() {
        return this.te.getRedstoneComSub();
    }

    public boolean getAutoInput() {
        return this.te.getAutoInput() == 1;
    }

    public boolean getAugmentGUI() {
        return this.te.getAugmentGUI() == 1;
    }

    public boolean getIsFactory() {
        return this.te.isFactory();
    }

    public boolean getIsFurnace() {
        return this.te.isFurnace();
    }

    public boolean getIsGenerator() {
        return this.te.isGenerator();
    }

    public boolean getAutoOutput() {
        return this.te.getAutoOutput() == 1;
    }

    public Component getTooltip(int index) {
        switch (this.te.furnaceSettings.get(index)) {
            case 1: {
                return Component.m_237115_((String)"tooltip.ironfurnaces.gui_input");
            }
            case 2: {
                return Component.m_237115_((String)"tooltip.ironfurnaces.gui_output");
            }
            case 3: {
                return Component.m_237115_((String)"tooltip.ironfurnaces.gui_input_output");
            }
            case 4: {
                return Component.m_237115_((String)"tooltip.ironfurnaces.gui_fuel");
            }
        }
        return Component.m_237115_((String)"tooltip.ironfurnaces.gui_none");
    }

    public int getSettingTop() {
        return this.te.getSettingTop();
    }

    public int getSettingBottom() {
        return this.te.getSettingBottom();
    }

    public int getSettingFront() {
        return this.te.getSettingFront();
    }

    public int getSettingBack() {
        return this.te.getSettingBack();
    }

    public int getSettingLeft() {
        return this.te.getSettingLeft();
    }

    public int getSettingRight() {
        return this.te.getSettingRight();
    }

    public int getIndexFront() {
        return this.te.getIndexFront();
    }

    public int getIndexBack() {
        return this.te.getIndexBack();
    }

    public int getIndexLeft() {
        return this.te.getIndexLeft();
    }

    public int getIndexRight() {
        return this.te.getIndexRight();
    }

    public BlockPos getPos() {
        return this.te.m_58899_();
    }

    public boolean isBurning() {
        return this.te.isBurning();
    }

    public boolean isRainbowFurnace() {
        return this.te.isRainbowFurnace();
    }

    public int getCookScaled(int pixels) {
        int i = this.te.cookTime;
        int j = this.te.totalCookTime;
        return j != 0 && i != 0 ? i * pixels / j : 0;
    }

    public int getFactoryCookScaled(int index, int pixels) {
        int i = this.te.factoryCookTime[index];
        int j = this.te.factoryTotalCookTime[index];
        return j != 0 && i != 0 ? i * pixels / j : 0;
    }

    public int getFactoryCooktimeSize() {
        return this.te.factoryCookTime.length;
    }

    public int getBurnLeftScaled(int pixels) {
        int i = this.te.recipesUsed;
        if (i == 0) {
            i = 200;
        }
        return this.te.furnaceBurnTime * pixels / i;
    }

    public int getGeneratorBurnScaled(int pixels) {
        int i = this.te.generatorRecentRecipeRF;
        if (i == 0) {
            i = 200;
        }
        return (int)this.te.generatorBurn * pixels / i;
    }

    public boolean isGeneratorBurning() {
        return this.te.generatorBurn > 0.0;
    }

    public int getEnergyScaled(int pixels) {
        int i = this.te.getEnergy();
        int j = this.te.getCapacity();
        return j != 0 && i != 0 ? i * pixels / j : 0;
    }

    public ItemStack m_7648_(Player playerIn, int index) {
        ItemStack itemstack = ItemStack.f_41583_;
        Slot slot = (Slot)this.f_38839_.get(index);
        if (slot != null && slot.m_6657_()) {
            ItemStack itemstack1 = slot.m_7993_();
            itemstack = itemstack1.m_41777_();
            if (this.te.isGenerator()) {
                if (index != 6 && index != 3 && index != 4 && index != 5) {
                    if (this.te.m_8020_(3).m_41720_() instanceof ItemAugmentSmoking) {
                        if (BlockIronFurnaceTileBase.getSmokingBurn(itemstack1) > 0 && !this.m_38903_(itemstack1, 6, 7, false)) {
                            return ItemStack.f_41583_;
                        }
                    } else if (this.te.m_8020_(3).m_41720_() instanceof ItemAugmentBlasting ? this.te.hasGeneratorBlastingRecipe(itemstack1) && !this.m_38903_(itemstack1, 6, 7, false) : BlockIronFurnaceTileBase.isItemFuel(itemstack1, RecipeType.f_44108_) && !(itemstack1.m_41720_() instanceof ItemHeater) && !this.m_38903_(itemstack1, 6, 7, false)) {
                        return ItemStack.f_41583_;
                    }
                    if (BlockIronFurnaceTileBase.isItemAugment(itemstack1, 0) ? !this.m_38903_(itemstack1, 3, 4, false) : (BlockIronFurnaceTileBase.isItemAugment(itemstack1, 1) ? !this.m_38903_(itemstack1, 4, 5, false) : (BlockIronFurnaceTileBase.isItemAugment(itemstack1, 2) ? !this.m_38903_(itemstack1, 5, 6, false) : (index >= 19 && index < 45 ? !this.m_38903_(itemstack1, 45, 54, false) : index >= 45 && index < 54 && !this.m_38903_(itemstack1, 19, 45, false))))) {
                        return ItemStack.f_41583_;
                    }
                } else if (!this.m_38903_(itemstack1, 19, 54, false)) {
                    return ItemStack.f_41583_;
                }
            }
            if (this.te.isFactory()) {
                if (index >= 12 && index <= 18) {
                    if (!this.m_38903_(itemstack1, 19, 54, true)) {
                        return ItemStack.f_41583_;
                    }
                    slot.m_40234_(itemstack1, itemstack);
                } else if (index >= 19 ? (this.te.hasRecipe(itemstack1) ? (this.getTier() == 2 ? !this.m_38903_(itemstack1, 7, 13, false) : (this.getTier() == 1 ? !this.m_38903_(itemstack1, 8, 12, false) : !this.m_38903_(itemstack1, 9, 11, false))) : (BlockIronFurnaceTileBase.isItemAugment(itemstack1, 0) ? !this.m_38903_(itemstack1, 3, 4, false) : (BlockIronFurnaceTileBase.isItemAugment(itemstack1, 1) ? !this.m_38903_(itemstack1, 4, 5, false) : (BlockIronFurnaceTileBase.isItemAugment(itemstack1, 2) ? !this.m_38903_(itemstack1, 5, 6, false) : (index >= 19 && index < 45 ? !this.m_38903_(itemstack1, 45, 54, false) : index >= 45 && index < 54 && !this.m_38903_(itemstack1, 19, 45, false)))))) : !this.m_38903_(itemstack1, 19, 54, false)) {
                    return ItemStack.f_41583_;
                }
            }
            if (this.te.isFurnace()) {
                if (index == 2) {
                    if (!this.m_38903_(itemstack1, 19, 54, true)) {
                        return ItemStack.f_41583_;
                    }
                    slot.m_40234_(itemstack1, itemstack);
                } else if (index != 1 && index != 0 && index != 3 && index != 4 && index != 5 ? (this.te.hasRecipe(itemstack1) ? !this.m_38903_(itemstack1, 0, 1, false) : (BlockIronFurnaceTileBase.isItemFuel(itemstack1, RecipeType.f_44108_) ? !this.m_38903_(itemstack1, 1, 2, false) : (BlockIronFurnaceTileBase.isItemAugment(itemstack1, 0) ? !this.m_38903_(itemstack1, 3, 4, false) : (BlockIronFurnaceTileBase.isItemAugment(itemstack1, 1) ? !this.m_38903_(itemstack1, 4, 5, false) : (BlockIronFurnaceTileBase.isItemAugment(itemstack1, 2) ? !this.m_38903_(itemstack1, 5, 6, false) : (index >= 19 && index < 45 ? !this.m_38903_(itemstack1, 45, 54, false) : index >= 45 && index < 54 && !this.m_38903_(itemstack1, 19, 45, false))))))) : !this.m_38903_(itemstack1, 19, 54, false)) {
                    return ItemStack.f_41583_;
                }
            }
            if (itemstack1.m_41619_()) {
                slot.m_5852_(ItemStack.f_41583_);
            } else {
                slot.m_6654_();
            }
            if (itemstack1.m_41613_() == itemstack.m_41613_()) {
                return ItemStack.f_41583_;
            }
            slot.m_142406_(playerIn, itemstack1);
        }
        return itemstack;
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
}

