package com.keepsmelting.mixin;

import net.minecraft.core.NonNullList;
import net.minecraft.core.RegistryAccess;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

import javax.annotation.Nullable;

@Mixin(AbstractFurnaceBlockEntity.class)
public interface IFurnaceAccessor {

    @Accessor("items")
    NonNullList<ItemStack> getItems();

    @Accessor("litTime")
    int getLitTime();

    @Accessor("litTime")
    void setLitTime(int litTime);

    @Accessor("litDuration")
    int getLitDuration();

    @Accessor("litDuration")
    void setLitDuration(int litDuration);

    @Accessor("cookingProgress")
    int getCookingProgress();

    @Accessor("cookingProgress")
    void setCookingProgress(int progress);

    @Accessor("cookingTotalTime")
    int getCookingTotalTime();

    @Accessor("cookingTotalTime")
    void setCookingTotalTime(int cookingTotalTime);

    @Invoker("isLit")
    boolean callIsLit();

    @Invoker("canBurn")
    boolean callCanBurn(RegistryAccess registryAccess,
                        @Nullable Recipe<?> recipe,
                        NonNullList<ItemStack> items,
                        int maxStackSize);

    @Invoker("burn")
    boolean callBurn(RegistryAccess registryAccess,
                     @Nullable Recipe<?> recipe,
                     NonNullList<ItemStack> items,
                     int maxStackSize);
}
