/*
 * Decompiled with CFR.
 */
package ironfurnaces.items.upgrades;

import ironfurnaces.init.Registration;
import ironfurnaces.items.upgrades.ItemUpgrade;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;

public class ItemUpgradeSilver
extends ItemUpgrade {
    public ItemUpgradeSilver(Item.Properties properties) {
        super(properties, (Block)Registration.COPPER_FURNACE.get(), (Block)Registration.SILVER_FURNACE.get());
    }
}

