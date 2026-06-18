/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.blaze3d.platform.InputConstants
 *  com.mojang.blaze3d.platform.InputConstants$Key
 *  com.mojang.blaze3d.platform.InputConstants$Type
 *  net.minecraft.client.Minecraft
 *  net.minecraft.client.gui.Font
 *  net.minecraft.client.gui.GuiGraphics
 *  net.minecraft.client.resources.sounds.SimpleSoundInstance
 *  net.minecraft.client.resources.sounds.SoundInstance
 *  net.minecraft.core.BlockPos
 *  net.minecraft.network.chat.Component
 *  net.minecraft.resources.ResourceLocation
 *  net.minecraft.sounds.SoundEvent
 *  net.minecraft.sounds.SoundEvents
 */
package ironfurnaces.util.gui;

import com.mojang.blaze3d.platform.InputConstants;
import ironfurnaces.network.Messages;
import ironfurnaces.network.PacketSettingsButton;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;

public class FurnaceGuiButton {
    public int left;
    public int top;
    public int x;
    public int y;
    public int width;
    public int height;
    public int u;
    public int v;
    public int u_hover;
    public int v_hover;
    public int u_enabled;
    public int v_enabled;

    public FurnaceGuiButton(int left, int top, int x, int y, int width, int height, int u, int v, int u_hover, int v_hover, int u_enabled, int v_enabled) {
        this.left = left;
        this.top = top;
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        this.u = u;
        this.v = v;
        this.u_hover = u_hover;
        this.v_hover = v_hover;
        this.u_enabled = u_enabled;
        this.v_enabled = v_enabled;
    }

    public FurnaceGuiButton(int left, int top, int x, int y, int width, int height) {
        this(left, top, x, y, width, height, -1, -1, -1, -1, -1, -1);
    }

    public FurnaceGuiButton(int left, int top, int x, int y, int width, int height, int u_hover, int v_hover) {
        this(left, top, x, y, width, height, -1, -1, u_hover, v_hover, u_hover, v_hover);
    }

    public void changeEnabledUV(int u, int v) {
        this.u_enabled = u;
        this.v_enabled = v;
    }

    public void onClick(double mouseX, double mouseY, BlockPos pos, int index, int set, boolean condition) {
        if (condition && this.hovering(mouseX, mouseY)) {
            Messages.INSTANCE.sendToServer((Object)new PacketSettingsButton(pos, index, set));
            Minecraft.m_91087_().m_91106_().m_120367_((SoundInstance)SimpleSoundInstance.m_119755_((SoundEvent)((SoundEvent)SoundEvents.f_12490_.get()), (float)0.6f, (float)0.3f));
        }
    }

    public void onRightClick(double mouseX, double mouseY, int button, BlockPos pos, int index, int set, boolean condition) {
        if (button == 1 && condition && this.hovering(mouseX, mouseY)) {
            Messages.INSTANCE.sendToServer((Object)new PacketSettingsButton(pos, index, set));
            Minecraft.m_91087_().m_91106_().m_120367_((SoundInstance)SimpleSoundInstance.m_119755_((SoundEvent)((SoundEvent)SoundEvents.f_12490_.get()), (float)0.3f, (float)0.3f));
        }
    }

    public void render(ResourceLocation location, GuiGraphics matrix, int mouseX, int mouseY, boolean enabled) {
        if (!this.hovering(mouseX, mouseY) && this.hasUV()) {
            matrix.m_280218_(location, this.left + this.x, this.top + this.y, this.u, this.v, this.width, this.height);
        }
        if (this.hovering(mouseX, mouseY) && this.hasUVHover()) {
            matrix.m_280218_(location, this.left + this.x, this.top + this.y, this.u_hover, this.v_hover, this.width, this.height);
        }
        if (enabled && this.hasUVEnabled()) {
            matrix.m_280218_(location, this.left + this.x, this.top + this.y, this.u_enabled, this.v_enabled, this.width, this.height);
        }
    }

    public boolean hovering(double mouseX, double mouseY) {
        return mouseX >= (double)this.x && mouseX <= (double)(this.x + this.width) && mouseY >= (double)this.y && mouseY <= (double)(this.y + this.height);
    }

    public void renderTooltip(Font font, GuiGraphics matrix, Component text, int mouseX, int mouseY, boolean condition) {
        if (condition && this.hovering(mouseX, mouseY)) {
            matrix.m_280557_(font, text, mouseX, mouseY);
        }
    }

    public void renderComponentTooltip(Font font, GuiGraphics matrix, List<Component> text, int mouseX, int mouseY, boolean condition) {
        if (condition && this.hovering(mouseX, mouseY)) {
            matrix.m_280666_(font, text, mouseX, mouseY);
        }
    }

    public boolean hasUV() {
        return this.u >= 0 && this.v >= 0;
    }

    public boolean hasUVHover() {
        return this.u_hover >= 0 && this.v_hover >= 0;
    }

    public boolean hasUVEnabled() {
        return this.u_enabled >= 0 && this.v_enabled >= 0;
    }

    public static boolean isShiftKeyDown() {
        return FurnaceGuiButton.isKeyDown(340) || FurnaceGuiButton.isKeyDown(344);
    }

    public static boolean isKeyDown(int glfw) {
        InputConstants.Key key = InputConstants.Type.KEYSYM.m_84895_(glfw);
        int keyCode = key.m_84873_();
        if (keyCode != InputConstants.f_84822_.m_84873_()) {
            long windowHandle = Minecraft.m_91087_().m_91268_().m_85439_();
            try {
                if (key.m_84868_() == InputConstants.Type.KEYSYM) {
                    return InputConstants.m_84830_((long)windowHandle, (int)keyCode);
                }
            }
            catch (Exception exception) {
                // empty catch block
            }
        }
        return false;
    }
}

