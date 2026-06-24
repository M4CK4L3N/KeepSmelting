/*
 * Decompiled with CFR.
 */
package ironfurnaces.util.gui;

import ironfurnaces.util.StringHelper;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

public class FurnaceGuiEnergy {
    private int left;
    private int top;
    public int x;
    public int y;
    public int width;
    public int height;
    public int u;
    public int v;

    public FurnaceGuiEnergy(int left, int top, int x, int y, int width, int height, int u, int v) {
        this.left = left;
        this.top = top;
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        this.u = u;
        this.v = v;
    }

    public void changePos(int newX, int newY, boolean condition) {
        if (condition) {
            this.x = newX;
            this.y = newY;
        }
    }

    public void changeUV(int newU, int newV, boolean condition) {
        if (condition) {
            this.u = newU;
            this.v = newV;
        }
    }

    public void render(ResourceLocation location, GuiGraphics matrix, int scaled) {
        matrix.m_280218_(location, this.left + this.x, this.top + this.y + 42 - scaled, this.u, this.v + this.height - scaled, this.width, scaled);
    }

    public boolean hovering(double mouseX, double mouseY) {
        return mouseX >= (double)this.x && mouseX <= (double)(this.x + this.width) && mouseY >= (double)this.y && mouseY <= (double)(this.y + this.height);
    }

    public void renderTooltip(Font font, GuiGraphics matrix, int mouseX, int mouseY, int energy, int capacity, boolean condition) {
        if (condition && this.hovering(mouseX, mouseY)) {
            matrix.m_280557_(font, (Component)Component.m_237113_((String)StringHelper.displayEnergy(energy, capacity).get(0)), mouseX, mouseY);
        }
    }
}

