/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.world.item.Item$Properties
 *  net.minecraft.world.level.block.Block
 */
package ironfurnaces.items.upgrades;

import ironfurnaces.init.Registration;
import ironfurnaces.items.upgrades.ItemUpgrade;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;

public class ItemUpgradeIron2
extends ItemUpgrade {
    public ItemUpgradeIron2(Item.Properties properties) {
        super(properties, (Block)Registration.COPPER_FURNACE.get(), (Block)Registration.IRON_FURNACE.get());
    }
}

