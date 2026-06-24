/*
 * Decompiled with CFR.
 */
package ironfurnaces.container.slots;

import ironfurnaces.tileentity.furnaces.BlockIronFurnaceTileBase;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.ForgeEventFactory;

public class SlotIronFurnaceOutputFactory
extends Slot {
    private final Player player;
    private int removeCount;
    private BlockIronFurnaceTileBase te;
    public int index;

    public SlotIronFurnaceOutputFactory(int index, Player player, BlockIronFurnaceTileBase te, int slotIndex, int xPosition, int yPosition) {
        super((Container)te, slotIndex, xPosition, yPosition);
        this.player = player;
        this.te = te;
        this.index = index;
    }

    public boolean m_6659_() {
        if (this.index == 0 || this.index == 5) {
            if (this.te.getTier() > 1) {
                return this.te.isFactory() && this.te.getAugmentGUI() == 0;
            }
        } else if (this.index == 1 || this.index == 4) {
            if (this.te.getTier() > 0) {
                return this.te.isFactory() && this.te.getAugmentGUI() == 0;
            }
        } else {
            return this.te.isFactory() && this.te.getAugmentGUI() == 0;
        }
        return false;
    }

    public boolean m_5857_(ItemStack p_40231_) {
        return false;
    }

    public void m_142406_(Player thePlayer, ItemStack stack) {
        this.m_5845_(stack);
        super.m_142406_(thePlayer, stack);
    }

    public ItemStack m_6201_(int p_39548_) {
        if (this.m_6657_()) {
            this.removeCount += Math.min(p_39548_, this.m_7993_().m_41613_());
        }
        return super.m_6201_(p_39548_);
    }

    protected void m_7169_(ItemStack stack, int p_75210_2_) {
        stack.m_41678_(this.player.m_9236_(), this.player, this.removeCount);
        if (!this.player.m_9236_().f_46443_ && this.te instanceof BlockIronFurnaceTileBase) {
            this.te.unlockRecipes((ServerPlayer)this.player);
        }
        this.removeCount = 0;
    }

    protected void m_5845_(ItemStack p_39558_) {
        p_39558_.m_41678_(this.player.m_9236_(), this.player, this.removeCount);
        if (this.player instanceof ServerPlayer && this.f_40218_ instanceof BlockIronFurnaceTileBase) {
            ((BlockIronFurnaceTileBase)this.f_40218_).unlockRecipes((ServerPlayer)this.player);
        }
        this.removeCount = 0;
        ForgeEventFactory.firePlayerSmeltedEvent((Player)this.player, (ItemStack)p_39558_);
    }
}

