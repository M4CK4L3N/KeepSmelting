/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.client.gui.GuiGraphics
 *  net.minecraft.client.gui.screens.inventory.AbstractContainerScreen
 *  net.minecraft.network.chat.Component
 *  net.minecraft.resources.ResourceLocation
 *  net.minecraft.world.entity.player.Inventory
 */
package ironfurnaces.gui;

import ironfurnaces.container.BlockWirelessEnergyHeaterContainer;
import ironfurnaces.util.StringHelper;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

public abstract class BlockWirelessEnergyHeaterScreenBase<T extends BlockWirelessEnergyHeaterContainer>
extends AbstractContainerScreen<T> {
    public ResourceLocation GUI = new ResourceLocation("ironfurnaces:textures/gui/heater.png");
    Inventory playerInv;
    Component name;

    public BlockWirelessEnergyHeaterScreenBase(T t, Inventory inv, Component name) {
        super(t, inv, name);
        this.playerInv = inv;
        this.name = name;
    }

    public void m_88315_(GuiGraphics matrix, int mouseX, int mouseY, float partialTicks) {
        this.m_280273_(matrix);
        super.m_88315_(matrix, mouseX, mouseY, partialTicks);
        this.m_280072_(matrix, mouseX, mouseY);
    }

    protected void m_280003_(GuiGraphics matrix, int mouseX, int mouseY) {
        matrix.m_280614_(this.f_96547_, this.playerInv.m_5446_(), 7, this.getYSize() - 93, 0x404040, false);
        matrix.m_280614_(this.f_96547_, this.name, this.getXSize() / 2 - this.f_96541_.f_91062_.m_92895_(this.name.getString()) / 2, 6, 0x404040, false);
        int actualMouseX = mouseX - (this.f_96543_ - this.getXSize()) / 2;
        int actualMouseY = mouseY - (this.f_96544_ - this.getYSize()) / 2;
        if (actualMouseX >= 68 && actualMouseX <= 108 && actualMouseY >= 64 && actualMouseY <= 76) {
            int energy = ((BlockWirelessEnergyHeaterContainer)this.m_6262_()).getEnergy();
            int capacity = ((BlockWirelessEnergyHeaterContainer)this.m_6262_()).getMaxEnergy();
            matrix.m_280557_(this.f_96547_, (Component)Component.m_237113_((String)StringHelper.displayEnergy(energy, capacity).get(0)), actualMouseX, actualMouseY);
        }
    }

    protected void m_7286_(GuiGraphics matrix, float partialTicks, int mouseX, int mouseY) {
        int relX = (this.f_96543_ - this.getXSize()) / 2;
        int relY = (this.f_96544_ - this.getYSize()) / 2;
        matrix.m_280218_(this.GUI, relX, relY, 0, 0, this.getXSize(), this.getYSize());
        int i = ((BlockWirelessEnergyHeaterContainer)this.m_6262_()).getEnergyScaled(42);
        matrix.m_280218_(this.GUI, this.getGuiLeft() + 67, this.getGuiTop() + 63, 176, 0, i + 1, 14);
    }
}

