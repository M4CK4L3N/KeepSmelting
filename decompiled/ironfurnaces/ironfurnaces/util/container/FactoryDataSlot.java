/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.world.inventory.DataSlot
 */
package ironfurnaces.util.container;

import net.minecraft.world.inventory.DataSlot;

public abstract class FactoryDataSlot
extends DataSlot {
    public int index;

    public FactoryDataSlot(int index) {
        this.index = index;
    }
}

