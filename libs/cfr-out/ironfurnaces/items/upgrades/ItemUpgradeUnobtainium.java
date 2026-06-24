/*
 * Decompiled with CFR.
 */
package ironfurnaces.items.upgrades;

import ironfurnaces.init.Registration;
import ironfurnaces.items.upgrades.ItemUpgrade;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;

public class ItemUpgradeUnobtainium
extends ItemUpgrade {
    public ItemUpgradeUnobtainium(Item.Properties properties) {
        super(properties, (Block)Registration.VIBRANIUM_FURNACE.get(), (Block)Registration.UNOBTAINIUM_FURNACE.get());
    }
}

