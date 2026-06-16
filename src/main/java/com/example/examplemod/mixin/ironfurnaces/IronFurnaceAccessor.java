package com.example.examplemod.mixin.ironfurnaces;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.AbstractCookingRecipe;
import net.minecraft.world.item.crafting.Recipe;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.gen.Invoker;

import javax.annotation.Nullable;
import java.util.Optional;

/**
 * Invoker mixin for BlockIronFurnaceTileBase.
 * String target + @Pseudo — safely skipped if ironfurnaces absent.
 */
@Pseudo
@Mixin(targets = "ironfurnaces.tileentity.furnaces.BlockIronFurnaceTileBase")
public interface IronFurnaceAccessor {

    @Invoker(value = "smelt", remap = false)
    void invokeSmelt(@Nullable Recipe<?> recipe);

    @Invoker(value = "getRecipe", remap = false)
    Optional<Recipe<?>> invokeGetRecipe(ItemStack stack);

    @Invoker(value = "checkRecipeType", remap = false)
    void invokeCheckRecipeType();

    @Invoker(value = "canSmelt", remap = false)
    boolean invokeCanSmelt(@Nullable Recipe<?> recipe);

    // ── Factory ──

    @Invoker(value = "factorySmelt", remap = false)
    void invokeFactorySmelt(@Nullable Recipe<?> recipe, int slot);

    @Invoker(value = "canFactorySmelt", remap = false)
    boolean invokeCanFactorySmelt(@Nullable Recipe<?> recipe, int slot);

    @Invoker(value = "getRecipeFactory", remap = false)
    Optional<AbstractCookingRecipe> invokeGetRecipeFactory(int slot, ItemStack stack);

    @Invoker(value = "getFactoryCookTime", remap = false)
    int invokeGetFactoryCookTime(int slot);

    // ── AutoIO ──

    @Invoker(value = "autoIO", remap = false)
    void invokeAutoIO();

    @Invoker(value = "autoIOGenerator", remap = false)
    void invokeAutoIOGenerator();

    @Invoker(value = "autoFactoryIO", remap = false)
    void invokeAutoFactoryIO();

    @Invoker(value = "energyOut", remap = false)
    void invokeEnergyOut();
}
