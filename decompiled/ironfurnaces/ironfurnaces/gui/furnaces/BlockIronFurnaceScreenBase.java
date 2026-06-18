/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.collect.Lists
 *  com.mojang.blaze3d.platform.InputConstants
 *  com.mojang.blaze3d.platform.InputConstants$Key
 *  com.mojang.blaze3d.platform.InputConstants$Type
 *  com.mojang.blaze3d.systems.RenderSystem
 *  net.minecraft.ChatFormatting
 *  net.minecraft.client.Minecraft
 *  net.minecraft.client.gui.GuiGraphics
 *  net.minecraft.client.gui.screens.inventory.AbstractContainerScreen
 *  net.minecraft.core.Direction
 *  net.minecraft.network.chat.Component
 *  net.minecraft.network.chat.MutableComponent
 *  net.minecraft.resources.ResourceLocation
 *  net.minecraft.world.entity.player.Inventory
 */
package ironfurnaces.gui.furnaces;

import com.google.common.collect.Lists;
import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.systems.RenderSystem;
import ironfurnaces.capability.ClientShowConfig;
import ironfurnaces.container.furnaces.BlockIronFurnaceContainerBase;
import ironfurnaces.items.ItemMillionFurnace;
import ironfurnaces.network.Messages;
import ironfurnaces.network.PacketSettingsButton;
import ironfurnaces.network.PacketShowConfigButton;
import ironfurnaces.util.StringHelper;
import ironfurnaces.util.gui.FurnaceGuiButton;
import ironfurnaces.util.gui.FurnaceGuiEnergy;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

public abstract class BlockIronFurnaceScreenBase<T extends BlockIronFurnaceContainerBase>
extends AbstractContainerScreen<T> {
    public ResourceLocation GUI = new ResourceLocation("ironfurnaces:textures/gui/furnace.png");
    public static final ResourceLocation GUI_NETHERITE = new ResourceLocation("ironfurnaces:textures/gui/furnace_netherite.png");
    public static final ResourceLocation GUI_ATM = new ResourceLocation("ironfurnaces:textures/gui/furnace_allthemodium.png");
    public static final ResourceLocation GUI_VIB = new ResourceLocation("ironfurnaces:textures/gui/furnace_vibranium.png");
    public static final ResourceLocation GUI_UNOB = new ResourceLocation("ironfurnaces:textures/gui/furnace_unobtainium.png");
    public static final ResourceLocation GUI_FACTORY = new ResourceLocation("ironfurnaces:textures/gui/furnace_factory.png");
    public static final ResourceLocation GUI_GENERATOR = new ResourceLocation("ironfurnaces:textures/gui/furnace_generator.png");
    public static final ResourceLocation GUI_GENERATOR_NETHERITE = new ResourceLocation("ironfurnaces:textures/gui/furnace_generator_netherite.png");
    public static final ResourceLocation GUI_GENERATOR_ALLTHEMODIUM = new ResourceLocation("ironfurnaces:textures/gui/furnace_generator_allthemodium.png");
    public static final ResourceLocation GUI_GENERATOR_VIBRANIUM = new ResourceLocation("ironfurnaces:textures/gui/furnace_generator_vibranium.png");
    public static final ResourceLocation GUI_GENERATOR_UNOBTAINIUM = new ResourceLocation("ironfurnaces:textures/gui/furnace_generator_unobtainium.png");
    public static final ResourceLocation GUI_AUGMENTS = new ResourceLocation("ironfurnaces:textures/gui/augment.png");
    public static final ResourceLocation WIDGETS = new ResourceLocation("ironfurnaces:textures/gui/widgets.png");
    Inventory playerInv;
    Component name;
    public List<FurnaceGuiButton> sideButtons = Lists.newArrayList();
    public FurnaceGuiButton autoSplitButton;
    public FurnaceGuiButton augmentButton;
    public FurnaceGuiButton autoInputButton;
    public FurnaceGuiButton autoOutputButton;
    public FurnaceGuiButton topButton;
    public FurnaceGuiButton leftButton;
    public FurnaceGuiButton frontButton;
    public FurnaceGuiButton rightButton;
    public FurnaceGuiButton bottomButton;
    public FurnaceGuiButton backButton;
    public FurnaceGuiButton redstoneIgnoredButton;
    public FurnaceGuiButton redstoneLowButton;
    public FurnaceGuiButton redstoneHighButton;
    public FurnaceGuiButton comparatorButton;
    public FurnaceGuiButton comparatorSubButton;
    public FurnaceGuiButton addButton;
    public FurnaceGuiButton subButton;
    public FurnaceGuiEnergy energyBar;
    private int timer;
    private Random rand = new Random();

    public BlockIronFurnaceScreenBase(T t, Inventory inv, Component name) {
        super(t, inv, name);
        this.playerInv = inv;
        this.name = name;
    }

    public void m_88315_(GuiGraphics matrix, int mouseX, int mouseY, float partialTicks) {
        this.m_280273_(matrix);
        super.m_88315_(matrix, mouseX, mouseY, partialTicks);
        this.m_280072_(matrix, mouseX, mouseY);
    }

    protected void m_7856_() {
        super.m_7856_();
        int left = this.getGuiLeft();
        int top = this.getGuiTop();
        this.energyBar = new FurnaceGuiEnergy(left, top, 109, 22, 14, 42, 176, 14);
        this.autoSplitButton = new FurnaceGuiButton(left, top, 9, 56, 14, 14, 112, 189);
        this.augmentButton = new FurnaceGuiButton(left, top, 161, 4, 11, 11);
        this.autoInputButton = new FurnaceGuiButton(left, top, -47, 12, 14, 14, 0, 189);
        this.autoOutputButton = new FurnaceGuiButton(left, top, -29, 12, 14, 14, 14, 189);
        this.redstoneIgnoredButton = new FurnaceGuiButton(left, top, -47, 70, 14, 14, 28, 189);
        this.redstoneLowButton = new FurnaceGuiButton(left, top, -31, 70, 14, 14, 84, 189, 98, 189, 98, 189);
        this.redstoneHighButton = new FurnaceGuiButton(left, top, -31, 70, 14, 14, 42, 189);
        this.comparatorButton = new FurnaceGuiButton(left, top, -15, 70, 14, 14, 56, 189);
        this.comparatorSubButton = new FurnaceGuiButton(left, top, -47, 86, 14, 14, 70, 189);
        this.addButton = new FurnaceGuiButton(left, top, -31, 86, 14, 14, 0, 14, 14, 14, 28, 14);
        this.subButton = new FurnaceGuiButton(left, top, -31, 86, 14, 14, 0, 0, 14, 0, 28, 0);
        this.bottomButton = new FurnaceGuiButton(left, top, -32, 55, 10, 10);
        this.sideButtons.add(this.bottomButton);
        this.topButton = new FurnaceGuiButton(left, top, -32, 31, 10, 10);
        this.sideButtons.add(this.topButton);
        this.frontButton = new FurnaceGuiButton(left, top, -32, 43, 10, 10);
        this.sideButtons.add(this.frontButton);
        this.backButton = new FurnaceGuiButton(left, top, -20, 55, 10, 10);
        this.sideButtons.add(this.backButton);
        this.leftButton = new FurnaceGuiButton(left, top, -44, 43, 10, 10);
        this.sideButtons.add(this.leftButton);
        this.rightButton = new FurnaceGuiButton(left, top, -20, 43, 10, 10);
        this.sideButtons.add(this.rightButton);
    }

    private boolean showInventoryButtons() {
        return this.getShowConfig() == 1;
    }

    public void setShowConfig(int value) {
        ClientShowConfig.set(value);
        Messages.INSTANCE.sendToServer((Object)new PacketShowConfigButton(value));
    }

    public int getShowConfig() {
        return ClientShowConfig.getShowConfig();
    }

    protected void m_280003_(GuiGraphics matrix, int mouseX, int mouseY) {
        int actualMouseX = mouseX - (this.f_96543_ - this.getXSize()) / 2;
        int actualMouseY = mouseY - (this.f_96544_ - this.getYSize()) / 2;
        if (((BlockIronFurnaceContainerBase)this.m_6262_()).isRainbowFurnace()) {
            ++this.timer;
            if (this.timer % 20 == 0) {
                this.timer = 0;
                String name = this.name.getString();
                ArrayList names = Lists.newArrayList();
                for (int i = 0; i < name.length(); ++i) {
                    names.add(Component.m_237113_((String)("" + name.charAt(i))).m_130940_(ChatFormatting.m_126647_((int)ItemMillionFurnace.getIDRandom(this.rand.nextInt(6)))));
                }
                MutableComponent component = Component.m_237113_((String)"");
                for (int i = 0; i < names.size(); ++i) {
                    component.m_7220_((Component)names.get(i));
                }
                this.name = component;
            }
        }
        if (((BlockIronFurnaceContainerBase)this.m_6262_()).getIsFactory()) {
            matrix.m_280614_(this.f_96547_, this.name, this.getXSize() / 2 - this.f_96541_.f_91062_.m_92895_(this.name.getString()) / 2, -10, 0xFFFFFF, false);
        } else {
            matrix.m_280614_(this.f_96547_, this.name, ((BlockIronFurnaceContainerBase)this.m_6262_()).getIsFurnace() ? 7 + this.getXSize() / 2 - this.f_96541_.f_91062_.m_92895_(this.name.getString()) / 2 : this.getXSize() / 2 - this.f_96541_.f_91062_.m_92895_(this.name.getString()) / 2, 6, 0x404040, false);
        }
        matrix.m_280614_(this.f_96547_, this.playerInv.m_5446_(), 7, this.getYSize() - 93, 0x404040, false);
        if (this.showInventoryButtons() && ((BlockIronFurnaceContainerBase)this.m_6262_()).getRedstoneMode() == 4) {
            int comSub = ((BlockIronFurnaceContainerBase)this.m_6262_()).getComSub();
            int i = comSub > 9 ? 28 : 31;
            matrix.m_280614_(this.f_96547_, (Component)Component.m_237113_((String)("" + comSub)), i - 42, 90, 0x404040, false);
        }
        this.addTooltips(matrix, actualMouseX, actualMouseY);
    }

    private void addTooltips(GuiGraphics matrix, int mouseX, int mouseY) {
        this.augmentButton.renderTooltip(this.f_96547_, matrix, (Component)Component.m_237115_((String)"tooltip.ironfurnaces.gui_open_augments"), mouseX, mouseY, !((BlockIronFurnaceContainerBase)this.m_6262_()).getAugmentGUI());
        this.augmentButton.renderTooltip(this.f_96547_, matrix, (Component)Component.m_237115_((String)"tooltip.ironfurnaces.gui_open_furnace"), mouseX, mouseY, ((BlockIronFurnaceContainerBase)this.m_6262_()).getAugmentGUI());
        this.energyBar.changePos(109, 22, ((BlockIronFurnaceContainerBase)this.m_6262_()).getIsGenerator() && !((BlockIronFurnaceContainerBase)this.m_6262_()).getAugmentGUI());
        this.energyBar.changePos(9, 7, ((BlockIronFurnaceContainerBase)this.m_6262_()).getIsFactory() && !((BlockIronFurnaceContainerBase)this.m_6262_()).getAugmentGUI());
        this.energyBar.renderTooltip(this.f_96547_, matrix, mouseX, mouseY, ((BlockIronFurnaceContainerBase)this.m_6262_()).getEnergy(), ((BlockIronFurnaceContainerBase)this.m_6262_()).getMaxEnergy(), ((BlockIronFurnaceContainerBase)this.m_6262_()).getIsGenerator() && !((BlockIronFurnaceContainerBase)this.m_6262_()).getAugmentGUI());
        this.energyBar.renderTooltip(this.f_96547_, matrix, mouseX, mouseY, ((BlockIronFurnaceContainerBase)this.m_6262_()).getEnergy(), ((BlockIronFurnaceContainerBase)this.m_6262_()).getMaxEnergy(), ((BlockIronFurnaceContainerBase)this.m_6262_()).getIsFactory() && !((BlockIronFurnaceContainerBase)this.m_6262_()).getAugmentGUI());
        ArrayList tl = Lists.newArrayList((Object[])new Component[]{Component.m_237113_((String)"Auto Split"), Component.m_237113_((String)"ON")});
        this.autoSplitButton.renderComponentTooltip(this.f_96547_, matrix, tl, mouseX, mouseY, ((BlockIronFurnaceContainerBase)this.m_6262_()).isAutoSplit() && ((BlockIronFurnaceContainerBase)this.m_6262_()).getIsFactory() && !((BlockIronFurnaceContainerBase)this.m_6262_()).getAugmentGUI());
        tl = Lists.newArrayList((Object[])new Component[]{Component.m_237113_((String)"Auto Split"), Component.m_237113_((String)"OFF")});
        this.autoSplitButton.renderComponentTooltip(this.f_96547_, matrix, tl, mouseX, mouseY, !((BlockIronFurnaceContainerBase)this.m_6262_()).isAutoSplit() && ((BlockIronFurnaceContainerBase)this.m_6262_()).getIsFactory() && !((BlockIronFurnaceContainerBase)this.m_6262_()).getAugmentGUI());
        if (!this.showInventoryButtons()) {
            if (mouseX >= -20 && mouseX <= 0 && mouseY >= 4 && mouseY <= 26) {
                matrix.m_280557_(this.f_96547_, (Component)Component.m_237115_((String)"tooltip.ironfurnaces.gui_open"), mouseX, mouseY);
            }
        } else {
            if (mouseX >= -13 && mouseX <= 0 && mouseY >= 4 && mouseY <= 26) {
                matrix.m_280666_(this.f_96547_, StringHelper.getShiftInfoGui(), mouseX, mouseY);
            }
            ArrayList list = Lists.newArrayList();
            list.add(Component.m_237115_((String)"tooltip.ironfurnaces.gui_auto_input"));
            list.add(Component.m_237113_((String)(((BlockIronFurnaceContainerBase)this.m_6262_()).getAutoInput() ? "ON" : "OFF")));
            this.autoInputButton.renderComponentTooltip(this.f_96547_, matrix, list, mouseX, mouseY, true);
            list = Lists.newArrayList();
            list.add(Component.m_237115_((String)"tooltip.ironfurnaces.gui_auto_output"));
            list.add(Component.m_237113_((String)(((BlockIronFurnaceContainerBase)this.m_6262_()).getAutoOutput() ? "ON" : "OFF")));
            this.autoOutputButton.renderComponentTooltip(this.f_96547_, matrix, list, mouseX, mouseY, true);
            list = Lists.newArrayList();
            list.add(Component.m_237115_((String)"tooltip.ironfurnaces.gui_top"));
            list.add(((BlockIronFurnaceContainerBase)this.m_6262_()).getTooltip(Direction.UP.ordinal()));
            this.topButton.renderComponentTooltip(this.f_96547_, matrix, list, mouseX, mouseY, true);
            list = Lists.newArrayList();
            list.add(Component.m_237115_((String)"tooltip.ironfurnaces.gui_bottom"));
            list.add(((BlockIronFurnaceContainerBase)this.m_6262_()).getTooltip(Direction.DOWN.ordinal()));
            this.bottomButton.renderComponentTooltip(this.f_96547_, matrix, list, mouseX, mouseY, true);
            list = Lists.newArrayList();
            if (BlockIronFurnaceScreenBase.isShiftKeyDown()) {
                list.add(Component.m_237115_((String)"tooltip.ironfurnaces.gui_reset"));
            } else {
                list.add(Component.m_237115_((String)"tooltip.ironfurnaces.gui_front"));
                list.add(((BlockIronFurnaceContainerBase)this.m_6262_()).getTooltip(((BlockIronFurnaceContainerBase)this.m_6262_()).getIndexFront()));
            }
            this.frontButton.renderComponentTooltip(this.f_96547_, matrix, list, mouseX, mouseY, true);
            list = Lists.newArrayList();
            list.add(Component.m_237115_((String)"tooltip.ironfurnaces.gui_back"));
            list.add(((BlockIronFurnaceContainerBase)this.m_6262_()).getTooltip(((BlockIronFurnaceContainerBase)this.m_6262_()).getIndexBack()));
            this.backButton.renderComponentTooltip(this.f_96547_, matrix, list, mouseX, mouseY, true);
            list = Lists.newArrayList();
            list.add(Component.m_237115_((String)"tooltip.ironfurnaces.gui_left"));
            list.add(((BlockIronFurnaceContainerBase)this.m_6262_()).getTooltip(((BlockIronFurnaceContainerBase)this.m_6262_()).getIndexLeft()));
            this.leftButton.renderComponentTooltip(this.f_96547_, matrix, list, mouseX, mouseY, true);
            list = Lists.newArrayList();
            list.add(Component.m_237115_((String)"tooltip.ironfurnaces.gui_right"));
            list.add(((BlockIronFurnaceContainerBase)this.m_6262_()).getTooltip(((BlockIronFurnaceContainerBase)this.m_6262_()).getIndexRight()));
            this.rightButton.renderComponentTooltip(this.f_96547_, matrix, list, mouseX, mouseY, true);
            this.redstoneIgnoredButton.renderTooltip(this.f_96547_, matrix, (Component)Component.m_237115_((String)"tooltip.ironfurnaces.gui_redstone_ignored"), mouseX, mouseY, true);
            this.redstoneLowButton.renderTooltip(this.f_96547_, matrix, (Component)Component.m_237115_((String)"tooltip.ironfurnaces.gui_redstone_low"), mouseX, mouseY, BlockIronFurnaceScreenBase.isShiftKeyDown());
            this.redstoneHighButton.renderTooltip(this.f_96547_, matrix, (Component)Component.m_237115_((String)"tooltip.ironfurnaces.gui_redstone_high"), mouseX, mouseY, !BlockIronFurnaceScreenBase.isShiftKeyDown());
            this.comparatorButton.renderTooltip(this.f_96547_, matrix, (Component)Component.m_237115_((String)"tooltip.ironfurnaces.gui_redstone_comparator"), mouseX, mouseY, true);
            this.comparatorSubButton.renderTooltip(this.f_96547_, matrix, (Component)Component.m_237115_((String)"tooltip.ironfurnaces.gui_redstone_comparator_sub"), mouseX, mouseY, true);
        }
    }

    private void bg(GuiGraphics matrix, int relX, int relY) {
        if (!((BlockIronFurnaceContainerBase)this.m_6262_()).getAugmentGUI()) {
            if (((BlockIronFurnaceContainerBase)this.m_6262_()).getIsFactory()) {
                matrix.m_280218_(GUI_FACTORY, relX, relY, 0, 0, this.getXSize(), this.getYSize());
            }
            if (((BlockIronFurnaceContainerBase)this.m_6262_()).getIsGenerator()) {
                matrix.m_280218_(GUI_GENERATOR, relX, relY, 0, 0, this.getXSize(), this.getYSize());
            }
            if (!((BlockIronFurnaceContainerBase)this.m_6262_()).getIsGenerator() && !((BlockIronFurnaceContainerBase)this.m_6262_()).getIsFactory()) {
                matrix.m_280218_(this.GUI, relX, relY, 0, 0, this.getXSize(), this.getYSize());
            }
        } else {
            matrix.m_280218_(GUI_AUGMENTS, relX, relY, 0, 0, this.getXSize(), this.getYSize());
        }
    }

    protected void renderFurnaceBg(GuiGraphics matrix) {
        if (((BlockIronFurnaceContainerBase)this.m_6262_()).getIsFurnace() && !((BlockIronFurnaceContainerBase)this.m_6262_()).getAugmentGUI()) {
            int i;
            if (((BlockIronFurnaceContainerBase)this.m_6262_()).isBurning()) {
                i = ((BlockIronFurnaceContainerBase)this.m_6262_()).getBurnLeftScaled(13);
                matrix.m_280218_(this.GUI, this.getGuiLeft() + 56, this.getGuiTop() + 36 + 12 - i, 176, 12 - i, 14, i + 1);
            }
            i = ((BlockIronFurnaceContainerBase)this.m_6262_()).getCookScaled(24);
            matrix.m_280218_(this.GUI, this.getGuiLeft() + 79, this.getGuiTop() + 34, 176, 14, i + 1, 16);
        }
    }

    protected void renderGeneratorBg(GuiGraphics matrix) {
        if (((BlockIronFurnaceContainerBase)this.m_6262_()).getIsGenerator() && !((BlockIronFurnaceContainerBase)this.m_6262_()).getAugmentGUI()) {
            if (((BlockIronFurnaceContainerBase)this.m_6262_()).isGeneratorBurning()) {
                int i = ((BlockIronFurnaceContainerBase)this.m_6262_()).getGeneratorBurnScaled(13);
                matrix.m_280218_(GUI_GENERATOR, this.getGuiLeft() + 56, this.getGuiTop() + 23 + 12 - i, 176, 12 - i, 14, i + 1);
            }
            this.energyBar.render(GUI_GENERATOR, matrix, ((BlockIronFurnaceContainerBase)this.m_6262_()).getEnergyScaled(42));
        }
    }

    protected void renderFactoryBg(GuiGraphics matrix) {
        if (((BlockIronFurnaceContainerBase)this.m_6262_()).getIsFactory() && !((BlockIronFurnaceContainerBase)this.m_6262_()).getAugmentGUI()) {
            this.addSlots(matrix, ((BlockIronFurnaceContainerBase)this.m_6262_()).getTier());
            this.energyBar.changePos(9, 7, true);
            this.energyBar.changeUV(176, 22, true);
            this.energyBar.render(GUI_FACTORY, matrix, ((BlockIronFurnaceContainerBase)this.m_6262_()).getEnergyScaled(42));
            for (int j = 0; j < ((BlockIronFurnaceContainerBase)this.m_6262_()).getFactoryCooktimeSize(); ++j) {
                int i = ((BlockIronFurnaceContainerBase)this.m_6262_()).getFactoryCookScaled(j, 22);
                matrix.m_280218_(GUI_FACTORY, this.getGuiLeft() + 29 + 21 * j, this.getGuiTop() + 27, 176, 0, 15, i + 1);
            }
        }
    }

    protected void m_7286_(GuiGraphics matrix, float partialTicks, int mouseX, int mouseY) {
        int relX = (this.f_96543_ - this.getXSize()) / 2;
        int relY = (this.f_96544_ - this.getYSize()) / 2;
        this.bg(matrix, relX, relY);
        this.renderFurnaceBg(matrix);
        this.renderGeneratorBg(matrix);
        this.renderFactoryBg(matrix);
        RenderSystem.setShaderTexture((int)0, (ResourceLocation)WIDGETS);
        int actualMouseX = mouseX - (this.f_96543_ - this.getXSize()) / 2;
        int actualMouseY = mouseY - (this.f_96544_ - this.getYSize()) / 2;
        this.addFactoryButtons(matrix, actualMouseX, actualMouseY);
        this.addInventoryButtons(matrix, actualMouseX, actualMouseY);
        this.addRedstoneButtons(matrix, actualMouseX, actualMouseY);
    }

    protected void addSlots(GuiGraphics matrix, int amount) {
        if (((BlockIronFurnaceContainerBase)this.m_6262_()).getIsFactory() && amount > 0) {
            matrix.m_280218_(GUI_FACTORY, this.getGuiLeft() + 48, this.getGuiTop() + 5, 176, 64, 18, 67);
            matrix.m_280218_(GUI_FACTORY, this.getGuiLeft() + 111, this.getGuiTop() + 5, 176, 64, 18, 67);
            if (amount == 2) {
                matrix.m_280218_(GUI_FACTORY, this.getGuiLeft() + 27, this.getGuiTop() + 5, 176, 64, 18, 67);
                matrix.m_280218_(GUI_FACTORY, this.getGuiLeft() + 132, this.getGuiTop() + 5, 176, 64, 18, 67);
            }
        }
    }

    private void addFactoryButtons(GuiGraphics matrix, int mouseX, int mouseY) {
        if (((BlockIronFurnaceContainerBase)this.m_6262_()).getIsFactory() && !((BlockIronFurnaceContainerBase)this.m_6262_()).getAugmentGUI()) {
            this.autoSplitButton.render(WIDGETS, matrix, mouseX, mouseY, ((BlockIronFurnaceContainerBase)this.m_6262_()).isAutoSplit());
        }
    }

    private void addRedstoneButtons(GuiGraphics matrix, int mouseX, int mouseY) {
        if (this.showInventoryButtons()) {
            boolean flag = BlockIronFurnaceScreenBase.isShiftKeyDown();
            int setting = ((BlockIronFurnaceContainerBase)this.m_6262_()).getRedstoneMode();
            if (setting == 0) {
                this.redstoneIgnoredButton.render(WIDGETS, matrix, mouseX, mouseY, true);
            }
            if (flag) {
                this.redstoneLowButton.render(WIDGETS, matrix, mouseX, mouseY, setting == 2);
            }
            if (!flag) {
                this.redstoneHighButton.render(WIDGETS, matrix, mouseX, mouseY, setting == 1);
            }
            if (setting == 3) {
                this.comparatorButton.render(WIDGETS, matrix, mouseX, mouseY, true);
            }
            if (setting == 4) {
                this.comparatorSubButton.render(WIDGETS, matrix, mouseX, mouseY, true);
                int comSub = ((BlockIronFurnaceContainerBase)this.m_6262_()).getComSub();
                this.addButton.render(WIDGETS, matrix, mouseX, mouseY, comSub == 15);
                if (flag) {
                    this.subButton.render(WIDGETS, matrix, mouseX, mouseY, comSub == 0);
                }
            }
        }
    }

    private void addInventoryButtons(GuiGraphics matrix, int mouseX, int mouseY) {
        if (!this.showInventoryButtons()) {
            matrix.m_280218_(WIDGETS, this.getGuiLeft() - 20, this.getGuiTop() + 4, 0, 28, 23, 26);
        } else if (this.showInventoryButtons()) {
            matrix.m_280218_(WIDGETS, this.getGuiLeft() - 56, this.getGuiTop() + 4, 0, 54, 59, 107);
            this.autoInputButton.render(WIDGETS, matrix, mouseX, mouseY, ((BlockIronFurnaceContainerBase)this.m_6262_()).getAutoInput());
            this.autoOutputButton.render(WIDGETS, matrix, mouseX, mouseY, ((BlockIronFurnaceContainerBase)this.m_6262_()).getAutoOutput());
            this.blitIO(matrix, mouseX, mouseY);
        }
    }

    private void blitIO(GuiGraphics matrix, int mouseX, int mouseY) {
        int[] settings = new int[]{((BlockIronFurnaceContainerBase)this.m_6262_()).getSettingBottom(), ((BlockIronFurnaceContainerBase)this.m_6262_()).getSettingTop(), ((BlockIronFurnaceContainerBase)this.m_6262_()).getSettingFront(), ((BlockIronFurnaceContainerBase)this.m_6262_()).getSettingBack(), ((BlockIronFurnaceContainerBase)this.m_6262_()).getSettingLeft(), ((BlockIronFurnaceContainerBase)this.m_6262_()).getSettingRight()};
        for (int i = 0; i < settings.length && settings.length == this.sideButtons.size(); ++i) {
            if (settings[i] == 0) continue;
            FurnaceGuiButton button = this.sideButtons.get(i);
            button.changeEnabledUV(10 * settings[i] - 10, 161);
            button.render(WIDGETS, matrix, mouseX, mouseY, true);
        }
        if (((BlockIronFurnaceContainerBase)this.m_6262_()).getAugmentGUI()) {
            return;
        }
        boolean input = false;
        boolean output = false;
        boolean both = false;
        boolean fuel = false;
        for (int set : settings) {
            if (set == 1) {
                input = true;
                continue;
            }
            if (set == 2) {
                output = true;
                continue;
            }
            if (set == 3) {
                both = true;
                continue;
            }
            if (set != 4) continue;
            fuel = true;
        }
        if (input || both) {
            if (((BlockIronFurnaceContainerBase)this.m_6262_()).getIsFurnace()) {
                matrix.m_280218_(WIDGETS, this.getGuiLeft() + 55, this.getGuiTop() + 16, 0, 171, 18, 18);
            }
            if (((BlockIronFurnaceContainerBase)this.m_6262_()).getIsFactory()) {
                matrix.m_280218_(WIDGETS, this.getGuiLeft() + 69, this.getGuiTop() + 5, 0, 171, 18, 18);
                matrix.m_280218_(WIDGETS, this.getGuiLeft() + 90, this.getGuiTop() + 5, 0, 171, 18, 18);
                if (((BlockIronFurnaceContainerBase)this.m_6262_()).getTier() > 0) {
                    matrix.m_280218_(WIDGETS, this.getGuiLeft() + 48, this.getGuiTop() + 5, 0, 171, 18, 18);
                    matrix.m_280218_(WIDGETS, this.getGuiLeft() + 111, this.getGuiTop() + 5, 0, 171, 18, 18);
                    if (((BlockIronFurnaceContainerBase)this.m_6262_()).getTier() > 1) {
                        matrix.m_280218_(WIDGETS, this.getGuiLeft() + 27, this.getGuiTop() + 5, 0, 171, 18, 18);
                        matrix.m_280218_(WIDGETS, this.getGuiLeft() + 132, this.getGuiTop() + 5, 0, 171, 18, 18);
                    }
                }
            }
        }
        if (output || both) {
            if (((BlockIronFurnaceContainerBase)this.m_6262_()).getIsFurnace()) {
                matrix.m_280218_(WIDGETS, this.getGuiLeft() + 111, this.getGuiTop() + 30, 0, 203, 26, 26);
            }
            if (((BlockIronFurnaceContainerBase)this.m_6262_()).getIsFactory()) {
                matrix.m_280218_(WIDGETS, this.getGuiLeft() + 69, this.getGuiTop() + 54, 36, 171, 18, 18);
                matrix.m_280218_(WIDGETS, this.getGuiLeft() + 90, this.getGuiTop() + 54, 36, 171, 18, 18);
                if (((BlockIronFurnaceContainerBase)this.m_6262_()).getTier() > 0) {
                    matrix.m_280218_(WIDGETS, this.getGuiLeft() + 48, this.getGuiTop() + 54, 36, 171, 18, 18);
                    matrix.m_280218_(WIDGETS, this.getGuiLeft() + 111, this.getGuiTop() + 54, 36, 171, 18, 18);
                    if (((BlockIronFurnaceContainerBase)this.m_6262_()).getTier() > 1) {
                        matrix.m_280218_(WIDGETS, this.getGuiLeft() + 27, this.getGuiTop() + 54, 36, 171, 18, 18);
                        matrix.m_280218_(WIDGETS, this.getGuiLeft() + 132, this.getGuiTop() + 54, 36, 171, 18, 18);
                    }
                }
            }
        }
        if (fuel) {
            if (((BlockIronFurnaceContainerBase)this.m_6262_()).getIsFurnace()) {
                matrix.m_280218_(WIDGETS, this.getGuiLeft() + 55, this.getGuiTop() + 52, 18, 171, 18, 18);
            }
            if (((BlockIronFurnaceContainerBase)this.m_6262_()).getIsGenerator()) {
                matrix.m_280218_(WIDGETS, this.getGuiLeft() + 55, this.getGuiTop() + 39, 18, 171, 18, 18);
            }
        }
    }

    public boolean m_6375_(double mouseX, double mouseY, int button) {
        double actualMouseX = mouseX - ((double)this.f_96543_ - (double)this.getXSize()) / 2.0;
        double actualMouseY = mouseY - ((double)this.f_96544_ - (double)this.getYSize()) / 2.0;
        this.mouseClickedRedstoneButtons(actualMouseX, actualMouseY);
        this.mouseClickedInventoryButtons(button, actualMouseX, actualMouseY);
        this.mouseClickedAugmentButton(actualMouseX, actualMouseY);
        this.mouseClickedAutoSplitButton(actualMouseX, actualMouseY);
        return super.m_6375_(mouseX, mouseY, button);
    }

    public void mouseClickedAutoSplitButton(double mouseX, double mouseY) {
        if (!((BlockIronFurnaceContainerBase)this.m_6262_()).isAutoSplit()) {
            this.autoSplitButton.onClick(mouseX, mouseY, ((BlockIronFurnaceContainerBase)this.m_6262_()).getPos(), 11, 1, ((BlockIronFurnaceContainerBase)this.m_6262_()).getIsFactory());
        } else {
            this.autoSplitButton.onClick(mouseX, mouseY, ((BlockIronFurnaceContainerBase)this.m_6262_()).getPos(), 11, 0, ((BlockIronFurnaceContainerBase)this.m_6262_()).getIsFactory());
        }
    }

    public void mouseClickedAugmentButton(double mouseX, double mouseY) {
        if (!((BlockIronFurnaceContainerBase)this.m_6262_()).getAugmentGUI()) {
            this.augmentButton.onClick(mouseX, mouseY, ((BlockIronFurnaceContainerBase)this.m_6262_()).getPos(), 10, 1, true);
        } else {
            this.augmentButton.onClick(mouseX, mouseY, ((BlockIronFurnaceContainerBase)this.m_6262_()).getPos(), 10, 0, true);
        }
    }

    public void mouseClickedInventoryButtons(int button, double mouseX, double mouseY) {
        if (!this.showInventoryButtons()) {
            if (mouseX >= -20.0 && mouseX <= 0.0 && mouseY >= 4.0 && mouseY <= 26.0) {
                this.setShowConfig(1);
            }
        } else {
            if (mouseX >= -13.0 && mouseX <= 0.0 && mouseY >= 4.0 && mouseY <= 26.0) {
                this.setShowConfig(0);
            }
            if (!((BlockIronFurnaceContainerBase)this.m_6262_()).getAutoInput()) {
                this.autoInputButton.onClick(mouseX, mouseY, ((BlockIronFurnaceContainerBase)this.m_6262_()).getPos(), 6, 1, true);
            } else if (((BlockIronFurnaceContainerBase)this.m_6262_()).getAutoInput()) {
                this.autoInputButton.onClick(mouseX, mouseY, ((BlockIronFurnaceContainerBase)this.m_6262_()).getPos(), 6, 0, true);
            }
            if (!((BlockIronFurnaceContainerBase)this.m_6262_()).getAutoOutput()) {
                this.autoOutputButton.onClick(mouseX, mouseY, ((BlockIronFurnaceContainerBase)this.m_6262_()).getPos(), 7, 1, true);
            } else if (((BlockIronFurnaceContainerBase)this.m_6262_()).getAutoOutput()) {
                this.autoOutputButton.onClick(mouseX, mouseY, ((BlockIronFurnaceContainerBase)this.m_6262_()).getPos(), 7, 0, true);
            }
            this.clickInvButton(mouseX, mouseY, this.topButton, button, ((BlockIronFurnaceContainerBase)this.m_6262_()).getSettingTop(), Direction.UP.ordinal());
            this.clickInvButton(mouseX, mouseY, this.bottomButton, button, ((BlockIronFurnaceContainerBase)this.m_6262_()).getSettingBottom(), Direction.DOWN.ordinal());
            this.clickInvButton(mouseX, mouseY, this.frontButton, button, ((BlockIronFurnaceContainerBase)this.m_6262_()).getSettingFront(), ((BlockIronFurnaceContainerBase)this.m_6262_()).getIndexFront(), BlockIronFurnaceScreenBase.isShiftKeyDown());
            this.clickInvButton(mouseX, mouseY, this.backButton, button, ((BlockIronFurnaceContainerBase)this.m_6262_()).getSettingBack(), ((BlockIronFurnaceContainerBase)this.m_6262_()).getIndexBack());
            this.clickInvButton(mouseX, mouseY, this.leftButton, button, ((BlockIronFurnaceContainerBase)this.m_6262_()).getSettingLeft(), ((BlockIronFurnaceContainerBase)this.m_6262_()).getIndexLeft());
            this.clickInvButton(mouseX, mouseY, this.rightButton, button, ((BlockIronFurnaceContainerBase)this.m_6262_()).getSettingRight(), ((BlockIronFurnaceContainerBase)this.m_6262_()).getIndexRight());
        }
    }

    protected void clickInvButton(double mouseX, double mouseY, FurnaceGuiButton button, int buttonid, int setting, int index) {
        this.clickInvButton(mouseX, mouseY, button, buttonid, setting, index, false);
    }

    protected void clickInvButton(double mouseX, double mouseY, FurnaceGuiButton button, int buttonid, int setting, int index, boolean shift) {
        int set = setting == 4 ? 0 : setting + 1;
        button.onClick(mouseX, mouseY, ((BlockIronFurnaceContainerBase)this.m_6262_()).getPos(), index, set, buttonid == 0);
        set = setting == 0 ? 4 : setting - 1;
        button.onRightClick(mouseX, mouseY, buttonid, ((BlockIronFurnaceContainerBase)this.m_6262_()).getPos(), index, set, true);
        if (shift && this.frontButton.hovering(mouseX, mouseY)) {
            for (int i = 0; i < this.sideButtons.size(); ++i) {
                Messages.INSTANCE.sendToServer((Object)new PacketSettingsButton(((BlockIronFurnaceContainerBase)this.m_6262_()).getPos(), i, 0));
            }
        }
    }

    public void mouseClickedRedstoneButtons(double mouseX, double mouseY) {
        if (this.showInventoryButtons()) {
            boolean shift = BlockIronFurnaceScreenBase.isShiftKeyDown();
            this.addButton.onClick(mouseX, mouseY, ((BlockIronFurnaceContainerBase)this.m_6262_()).getPos(), 9, ((BlockIronFurnaceContainerBase)this.m_6262_()).getComSub() + 1, !shift && ((BlockIronFurnaceContainerBase)this.m_6262_()).getComSub() < 15);
            this.subButton.onClick(mouseX, mouseY, ((BlockIronFurnaceContainerBase)this.m_6262_()).getPos(), 9, ((BlockIronFurnaceContainerBase)this.m_6262_()).getComSub() - 1, shift && ((BlockIronFurnaceContainerBase)this.m_6262_()).getComSub() > 0);
            this.redstoneIgnoredButton.onClick(mouseX, mouseY, ((BlockIronFurnaceContainerBase)this.m_6262_()).getPos(), 8, 0, ((BlockIronFurnaceContainerBase)this.m_6262_()).getRedstoneMode() != 0);
            this.redstoneLowButton.onClick(mouseX, mouseY, ((BlockIronFurnaceContainerBase)this.m_6262_()).getPos(), 8, 2, ((BlockIronFurnaceContainerBase)this.m_6262_()).getRedstoneMode() != 2 && shift);
            this.redstoneHighButton.onClick(mouseX, mouseY, ((BlockIronFurnaceContainerBase)this.m_6262_()).getPos(), 8, 1, ((BlockIronFurnaceContainerBase)this.m_6262_()).getRedstoneMode() != 1 && !shift);
            this.comparatorButton.onClick(mouseX, mouseY, ((BlockIronFurnaceContainerBase)this.m_6262_()).getPos(), 8, 3, ((BlockIronFurnaceContainerBase)this.m_6262_()).getRedstoneMode() != 3);
            this.comparatorSubButton.onClick(mouseX, mouseY, ((BlockIronFurnaceContainerBase)this.m_6262_()).getPos(), 8, 4, ((BlockIronFurnaceContainerBase)this.m_6262_()).getRedstoneMode() != 4);
        }
    }

    public static boolean isShiftKeyDown() {
        return BlockIronFurnaceScreenBase.isKeyDown(340) || BlockIronFurnaceScreenBase.isKeyDown(344);
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

