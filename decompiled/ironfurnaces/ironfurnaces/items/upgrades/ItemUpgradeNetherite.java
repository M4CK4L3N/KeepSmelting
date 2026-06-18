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

public class ItemUpgradeNetherite
extends ItemUpgrade {
    public ItemUpgradeNetherite(Item.Properties properties) {
        super(properties, (Block)Registration.OBSIDIAN_FURNACE.get(), (Block)Registration.NETHERITE_FURNACE.get());
    }
}

