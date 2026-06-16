/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  javax.annotation.Nullable
 *  net.minecraft.core.NonNullList
 *  net.minecraft.core.RegistryAccess
 *  net.minecraft.world.item.ItemStack
 *  net.minecraft.world.item.crafting.Recipe
 *  net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity
 *  org.spongepowered.asm.mixin.Mixin
 *  org.spongepowered.asm.mixin.gen.Accessor
 *  org.spongepowered.asm.mixin.gen.Invoker
 */
package mod.gottsch.forge.everfurnace.core.mixin;

import javax.annotation.Nullable;
import mod.gottsch.forge.everfurnace.core.furnace.IEverFurnaceBlockEntity;
import net.minecraft.core.NonNullList;
import net.minecraft.core.RegistryAccess;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(value={AbstractFurnaceBlockEntity.class})
public interface IEverFurnaceBlockEntityMixin
extends IEverFurnaceBlockEntity {
    @Accessor(value="items")
    public NonNullList<ItemStack> getItems();

    @Accessor(value="litTime")
    public int getLitTime();

    @Accessor(value="litTime")
    public void setLitTime(int var1);

    @Accessor(value="litDuration")
    public int getLitDuration();

    @Accessor(value="cookingProgress")
    public int getCookingProgress();

    @Accessor(value="cookingProgress")
    public void setCookingProgress(int var1);

    @Accessor(value="cookingTotalTime")
    public int getCookingTotalTime();

    @Invoker(value="isLit")
    public boolean callIsLit();

    @Invoker
    public boolean callCanBurn(RegistryAccess var1, @Nullable Recipe<?> var2, NonNullList<ItemStack> var3, int var4);

    @Invoker
    public boolean callBurn(RegistryAccess var1, @Nullable Recipe<?> var2, NonNullList<ItemStack> var3, int var4);
}

