/*
 * Decompiled with CFR.
 */
package ironfurnaces.tileentity;

import ironfurnaces.tileentity.ITileInventory;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.Nameable;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

public abstract class TileEntityInventory
extends BlockEntity
implements ITileInventory,
WorldlyContainer,
MenuProvider,
Nameable {
    public NonNullList<ItemStack> inventory;
    protected Component name;

    public TileEntityInventory(BlockEntityType<?> tileEntityTypeIn, BlockPos pos, BlockState state, int sizeInventory) {
        super(tileEntityTypeIn, pos, state);
        this.inventory = NonNullList.m_122780_((int)sizeInventory, (Object)ItemStack.f_41583_);
    }

    public void handleUpdateTag(CompoundTag tag) {
        super.handleUpdateTag(tag);
    }

    @Nullable
    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        this.m_6596_();
        return ClientboundBlockEntityDataPacket.m_195640_((BlockEntity)this);
    }

    public void onDataPacket(Connection net, ClientboundBlockEntityDataPacket pkt) {
        CompoundTag tag = pkt.m_131708_();
        this.m_142466_(tag);
        this.m_6596_();
        this.f_58857_.markAndNotifyBlock(this.f_58858_, this.f_58857_.m_46745_(this.f_58858_), this.f_58857_.m_8055_(this.f_58858_).m_60734_().m_49966_(), this.f_58857_.m_8055_(this.f_58858_), 2, 3);
    }

    public CompoundTag m_5995_() {
        CompoundTag tag = new CompoundTag();
        this.m_183515_(tag);
        return tag;
    }

    public void setCustomName(Component name) {
        this.name = name;
    }

    public Component m_7755_() {
        return this.name != null ? this.name : Component.m_237115_((String)this.IgetName());
    }

    public int[] m_7071_(Direction side) {
        return this.IgetSlotsForFace(side);
    }

    public boolean m_7013_(int i, ItemStack itemStack) {
        return this.IisItemValidForSlot(i, itemStack);
    }

    public boolean m_7155_(int i, ItemStack itemStack, @Nullable Direction direction) {
        return this.IisItemValidForSlot(i, itemStack);
    }

    public boolean m_7157_(int i, ItemStack itemStack, Direction direction) {
        return this.IcanExtractItem(i, itemStack, direction);
    }

    public int m_6643_() {
        return this.inventory.size();
    }

    public boolean m_7983_() {
        for (ItemStack itemstack : this.inventory) {
            if (itemstack.m_41619_()) continue;
            return false;
        }
        return true;
    }

    public ItemStack m_8020_(int slot) {
        return (ItemStack)this.inventory.get(slot);
    }

    public ItemStack m_7407_(int i, int i1) {
        return ContainerHelper.m_18969_(this.inventory, (int)i, (int)i1);
    }

    public ItemStack m_8016_(int i) {
        return ContainerHelper.m_18966_(this.inventory, (int)i);
    }

    public void m_6836_(int index, ItemStack stack) {
        ItemStack itemstack = (ItemStack)this.inventory.get(index);
        boolean flag = !stack.m_41619_() && ItemStack.m_150942_((ItemStack)itemstack, (ItemStack)stack);
        this.inventory.set(index, (Object)stack);
        if (stack.m_41613_() > this.m_6893_()) {
            stack.m_41764_(this.m_6893_());
        }
    }

    public int m_6893_() {
        return super.m_6893_();
    }

    public void m_142466_(CompoundTag nbt) {
        super.m_142466_(nbt);
        this.inventory = NonNullList.m_122780_((int)this.m_6893_(), (Object)ItemStack.f_41583_);
        ContainerHelper.m_18980_((CompoundTag)nbt, this.inventory);
        if (nbt.m_128425_("CustomName", 8)) {
            this.name = Component.Serializer.m_130701_((String)nbt.m_128461_("CustomName"));
        }
    }

    public CompoundTag save(CompoundTag tag) {
        super.m_183515_(tag);
        return tag;
    }

    protected void m_183515_(CompoundTag nbt) {
        super.m_183515_(nbt);
        if (this.name != null) {
            nbt.m_128359_("CustomName", Component.Serializer.m_130703_((Component)this.name));
        }
        ContainerHelper.m_18973_((CompoundTag)nbt, this.inventory);
    }

    public boolean m_6542_(Player playerEntity) {
        if (this.f_58857_.m_7702_(this.f_58858_) != this) {
            return false;
        }
        return !(playerEntity.m_20275_((double)this.f_58858_.m_123341_() + 0.5, (double)this.f_58858_.m_123342_() + 0.5, (double)this.f_58858_.m_123343_() + 0.5) > 64.0);
    }

    public boolean m_8077_() {
        return this.name != null;
    }

    @Nullable
    public Component m_7770_() {
        return this.name;
    }

    public Component m_5446_() {
        return this.m_7755_();
    }

    @Nullable
    public AbstractContainerMenu m_7208_(int i, Inventory inventory, Player player) {
        return this.IcreateMenu(i, inventory, player);
    }

    public void m_6211_() {
        this.inventory.clear();
    }
}

