/*
 * Decompiled with CFR.
 */
package ironfurnaces.items.upgrades;

import ironfurnaces.init.Registration;
import ironfurnaces.items.upgrades.ItemUpgrade;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

public class ItemUpgradeIron
extends ItemUpgrade {
    public ItemUpgradeIron(Item.Properties properties) {
        super(properties, Blocks.f_50094_, (Block)Registration.IRON_FURNACE.get());
    }
}

