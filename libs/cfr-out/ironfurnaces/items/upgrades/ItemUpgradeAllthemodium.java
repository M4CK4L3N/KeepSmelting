/*
 * Decompiled with CFR.
 */
package ironfurnaces.items.upgrades;

import ironfurnaces.init.Registration;
import ironfurnaces.items.upgrades.ItemUpgrade;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;

public class ItemUpgradeAllthemodium
extends ItemUpgrade {
    public ItemUpgradeAllthemodium(Item.Properties properties) {
        super(properties, (Block)Registration.NETHERITE_FURNACE.get(), (Block)Registration.ALLTHEMODIUM_FURNACE.get());
    }
}

