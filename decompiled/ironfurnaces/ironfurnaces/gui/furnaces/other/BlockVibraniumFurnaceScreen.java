/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.network.chat.Component
 *  net.minecraft.world.entity.player.Inventory
 *  net.minecraftforge.api.distmarker.Dist
 *  net.minecraftforge.api.distmarker.OnlyIn
 */
package ironfurnaces.gui.furnaces.other;

import ironfurnaces.container.furnaces.other.BlockVibraniumFurnaceContainer;
import ironfurnaces.gui.furnaces.BlockIronFurnaceScreenBase;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(value=Dist.CLIENT)
public class BlockVibraniumFurnaceScreen
extends BlockIronFurnaceScreenBase<BlockVibraniumFurnaceContainer> {
    public BlockVibraniumFurnaceScreen(BlockVibraniumFurnaceContainer container, Inventory inv, Component name) {
        super(container, inv, name);
        this.GUI = GUI_VIB;
    }
}

