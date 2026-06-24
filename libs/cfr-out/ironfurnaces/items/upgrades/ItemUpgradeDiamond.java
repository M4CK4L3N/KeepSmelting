/*
 * Decompiled with CFR.
 */
package ironfurnaces.items.upgrades;

import ironfurnaces.init.Registration;
import ironfurnaces.items.upgrades.ItemUpgrade;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;

public class ItemUpgradeDiamond
extends ItemUpgrade {
    public ItemUpgradeDiamond(Item.Properties properties) {
        super(properties, (Block)Registration.GOLD_FURNACE.get(), (Block)Registration.DIAMOND_FURNACE.get());
    }
}

