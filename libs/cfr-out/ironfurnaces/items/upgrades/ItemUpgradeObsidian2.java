/*
 * Decompiled with CFR.
 */
package ironfurnaces.items.upgrades;

import ironfurnaces.init.Registration;
import ironfurnaces.items.upgrades.ItemUpgrade;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;

public class ItemUpgradeObsidian2
extends ItemUpgrade {
    public ItemUpgradeObsidian2(Item.Properties properties) {
        super(properties, (Block)Registration.CRYSTAL_FURNACE.get(), (Block)Registration.OBSIDIAN_FURNACE.get());
    }
}

