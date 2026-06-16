package com.example.examplemod.mixin.ironfurnaces;

import net.minecraft.world.item.ItemStack;
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
}
