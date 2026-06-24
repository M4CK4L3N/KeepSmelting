/*
 * Decompiled with CFR.
 */
package ironfurnaces.tileentity;

import ironfurnaces.container.BlockWirelessEnergyHeaterContainer;
import ironfurnaces.energy.FEnergyStorage;
import ironfurnaces.init.Registration;
import ironfurnaces.items.ItemHeater;
import ironfurnaces.tileentity.TileEntityInventory;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.energy.IEnergyStorage;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.wrapper.SidedInvWrapper;

public class BlockWirelessEnergyHeaterTile
extends TileEntityInventory {
    private LazyOptional<IEnergyStorage> energy = LazyOptional.of(this::createEnergy);
    LazyOptional<? extends IItemHandler>[] handlers = SidedInvWrapper.create((WorldlyContainer)this, (Direction[])new Direction[]{Direction.UP, Direction.DOWN, Direction.NORTH});

    public BlockWirelessEnergyHeaterTile(BlockPos pos, BlockState state) {
        super((BlockEntityType)Registration.HEATER_TILE.get(), pos, state, 1);
    }

    private FEnergyStorage createEnergy() {
        return new FEnergyStorage(1000000, 1000000, 0){

            @Override
            protected void onEnergyChanged() {
                BlockWirelessEnergyHeaterTile.this.m_6596_();
            }
        };
    }

    public static void tick(Level level, BlockPos worldPosition, BlockState blockState, BlockWirelessEnergyHeaterTile e) {
        ItemStack stack = e.m_8020_(0);
        if (!stack.m_41619_()) {
            CompoundTag nbt = new CompoundTag();
            stack.m_41751_(nbt);
            nbt.m_128405_("X", e.f_58858_.m_123341_());
            nbt.m_128405_("Y", e.f_58858_.m_123342_());
            nbt.m_128405_("Z", e.f_58858_.m_123343_());
        }
    }

    public int getEnergy() {
        return this.getCapability(ForgeCapabilities.ENERGY).map(h -> h.getEnergyStored()).orElse(0);
    }

    public int getCapacity() {
        return this.getCapability(ForgeCapabilities.ENERGY).map(h -> h.getMaxEnergyStored()).orElse(0);
    }

    public void setEnergy(int energy) {
        this.energy.ifPresent(h -> ((FEnergyStorage)((Object)h)).setEnergy(energy));
    }

    public void setMaxEnergy(int energy) {
        this.energy.ifPresent(h -> ((FEnergyStorage)((Object)h)).setCapacity(energy));
    }

    public void removeEnergy(int energy) {
        this.energy.ifPresent(h -> ((FEnergyStorage)((Object)h)).setEnergy(h.getEnergyStored() - energy));
    }

    @Override
    public void m_142466_(CompoundTag nbt) {
        super.m_142466_(nbt);
        this.energy.ifPresent(h -> ((FEnergyStorage)((Object)h)).setEnergy(nbt.m_128451_("Energy")));
    }

    @Override
    protected void m_183515_(CompoundTag nbt) {
        super.m_183515_(nbt);
        nbt.m_128405_("Energy", this.getEnergy());
    }

    @Override
    public int[] IgetSlotsForFace(Direction side) {
        return new int[0];
    }

    @Override
    public boolean IcanExtractItem(int index, ItemStack stack, Direction direction) {
        return true;
    }

    @Override
    public String IgetName() {
        return "container.ironfurnaces.wireless_energy_heater";
    }

    @Override
    public boolean IisItemValidForSlot(int index, ItemStack stack) {
        return stack.m_41720_() instanceof ItemHeater;
    }

    @Override
    public AbstractContainerMenu IcreateMenu(int i, Inventory playerInventory, Player playerEntity) {
        return new BlockWirelessEnergyHeaterContainer(i, this.f_58857_, this.f_58858_, playerInventory, playerEntity);
    }

    public <T> LazyOptional<T> getCapability(Capability<T> capability, @Nullable Direction facing) {
        if (!this.m_58901_() && facing != null && capability == ForgeCapabilities.ITEM_HANDLER) {
            if (facing == Direction.UP) {
                return this.handlers[0].cast();
            }
            if (facing == Direction.DOWN) {
                return this.handlers[1].cast();
            }
            return this.handlers[2].cast();
        }
        if (!this.m_58901_() && capability == ForgeCapabilities.ENERGY) {
            return this.energy.cast();
        }
        return super.getCapability(capability, facing);
    }

    public void m_7651_() {
        this.energy.invalidate();
        super.m_7651_();
    }
}

