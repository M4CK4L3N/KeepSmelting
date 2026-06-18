/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.world.item.ItemStack
 */
package ironfurnaces.recipes;

import net.minecraft.world.item.ItemStack;

public class SimpleGeneratorRecipe {
    private int energy;
    private ItemStack stack;

    public SimpleGeneratorRecipe(int energy, ItemStack stack) {
        this.energy = energy;
        this.stack = stack;
    }

    public int getEnergy() {
        return this.energy;
    }

    public ItemStack getIngredient() {
        return this.stack;
    }
}

