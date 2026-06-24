/*
 * Decompiled with CFR.
 */
package ironfurnaces.items.upgrades;

import ironfurnaces.init.Registration;
import ironfurnaces.items.upgrades.ItemUpgrade;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;

public class ItemUpgradeGold2
extends ItemUpgrade {
    public ItemUpgradeGold2(Item.Properties properties) {
        super(properties, (Block)Registration.SILVER_FURNACE.get(), (Block)Registration.GOLD_FURNACE.get());
    }
}

