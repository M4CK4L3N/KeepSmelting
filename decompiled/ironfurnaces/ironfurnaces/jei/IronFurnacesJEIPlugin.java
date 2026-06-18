/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.collect.Lists
 *  mezz.jei.api.IModPlugin
 *  mezz.jei.api.JeiPlugin
 *  mezz.jei.api.constants.RecipeTypes
 *  mezz.jei.api.recipe.RecipeType
 *  mezz.jei.api.recipe.category.IRecipeCategory
 *  mezz.jei.api.registration.IAdvancedRegistration
 *  mezz.jei.api.registration.IGuiHandlerRegistration
 *  mezz.jei.api.registration.IRecipeCatalystRegistration
 *  mezz.jei.api.registration.IRecipeCategoryRegistration
 *  mezz.jei.api.registration.IRecipeRegistration
 *  net.minecraft.client.Minecraft
 *  net.minecraft.resources.ResourceLocation
 *  net.minecraft.world.item.Item
 *  net.minecraft.world.item.ItemStack
 *  net.minecraft.world.item.crafting.RecipeType
 *  net.minecraft.world.level.ItemLike
 *  net.minecraftforge.common.ForgeHooks
 *  net.minecraftforge.fml.ModList
 *  net.minecraftforge.registries.ForgeRegistries
 */
package ironfurnaces.jei;

import com.google.common.collect.Lists;
import ironfurnaces.Config;
import ironfurnaces.init.Registration;
import ironfurnaces.jei.RecipeCategoryGeneratorBlasting;
import ironfurnaces.jei.RecipeCategoryGeneratorRegular;
import ironfurnaces.jei.RecipeCategoryGeneratorSmoking;
import ironfurnaces.recipes.GeneratorRecipe;
import ironfurnaces.recipes.SimpleGeneratorRecipe;
import ironfurnaces.tileentity.furnaces.BlockIronFurnaceTileBase;
import java.util.ArrayList;
import java.util.List;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.constants.RecipeTypes;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import mezz.jei.api.registration.IAdvancedRegistration;
import mezz.jei.api.registration.IGuiHandlerRegistration;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;
import net.minecraftforge.common.ForgeHooks;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.registries.ForgeRegistries;

@JeiPlugin
public class IronFurnacesJEIPlugin
implements IModPlugin {
    public ResourceLocation getPluginUid() {
        return new ResourceLocation("ironfurnaces", "plugin_ironfurnaces");
    }

    public void registerAdvanced(IAdvancedRegistration registration) {
    }

    public void registerCategories(IRecipeCategoryRegistration registration) {
        if (((Boolean)Config.enableJeiPlugin.get()).booleanValue()) {
            registration.addRecipeCategories(new IRecipeCategory[]{new RecipeCategoryGeneratorBlasting(registration.getJeiHelpers().getGuiHelper())});
            registration.addRecipeCategories(new IRecipeCategory[]{new RecipeCategoryGeneratorSmoking(registration.getJeiHelpers().getGuiHelper())});
            registration.addRecipeCategories(new IRecipeCategory[]{new RecipeCategoryGeneratorRegular(registration.getJeiHelpers().getGuiHelper())});
        }
    }

    public void registerRecipes(IRecipeRegistration registration) {
        if (((Boolean)Config.enableJeiPlugin.get()).booleanValue()) {
            ArrayList recipes = Lists.newArrayList();
            for (Item item : ForgeRegistries.ITEMS.getValues()) {
                if (ForgeHooks.getBurnTime((ItemStack)new ItemStack((ItemLike)item), (net.minecraft.world.item.crafting.RecipeType)net.minecraft.world.item.crafting.RecipeType.f_44108_) <= 0) continue;
                ItemStack stack = new ItemStack((ItemLike)item);
                recipes.add(new SimpleGeneratorRecipe(ForgeHooks.getBurnTime((ItemStack)new ItemStack((ItemLike)item), (net.minecraft.world.item.crafting.RecipeType)net.minecraft.world.item.crafting.RecipeType.f_44108_) * 20, stack));
            }
            registration.addRecipes(Registration.RecipeTypes.GENERATOR_REGULAR, (List)recipes);
            ArrayList recipes1 = Lists.newArrayList();
            List list = Minecraft.m_91087_().f_91073_.m_7465_().m_44013_((net.minecraft.world.item.crafting.RecipeType)Registration.GENERATOR_RECIPE_TYPE.get()).stream().toList();
            for (GeneratorRecipe item : list) {
                recipes1.add(item);
            }
            registration.addRecipes(Registration.RecipeTypes.GENERATOR_BLASTING, (List)recipes1);
            ArrayList recipes2 = Lists.newArrayList();
            for (Item item : ForgeRegistries.ITEMS.getValues()) {
                if (item.m_41473_() == null || item.m_41473_().m_38744_() <= 0) continue;
                ItemStack stack = new ItemStack((ItemLike)item);
                recipes2.add(new SimpleGeneratorRecipe(BlockIronFurnaceTileBase.getSmokingBurn(stack) * 40, stack));
            }
            registration.addRecipes(Registration.RecipeTypes.GENERATOR_SMOKING, (List)recipes2);
        }
    }

    public void registerRecipeCatalysts(IRecipeCatalystRegistration registry) {
        if (((Boolean)Config.enableJeiPlugin.get()).booleanValue() && ((Boolean)Config.enableJeiCatalysts.get()).booleanValue()) {
            registry.addRecipeCatalyst(new ItemStack((ItemLike)Registration.BLASTING_AUGMENT.get()), new RecipeType[]{RecipeTypes.BLASTING});
            registry.addRecipeCatalyst(new ItemStack((ItemLike)Registration.SMOKING_AUGMENT.get()), new RecipeType[]{RecipeTypes.SMOKING});
            registry.addRecipeCatalyst(new ItemStack((ItemLike)Registration.GENERATOR_AUGMENT.get()), new RecipeType[]{Registration.RecipeTypes.GENERATOR_REGULAR});
            registry.addRecipeCatalyst(new ItemStack((ItemLike)Registration.GENERATOR_AUGMENT.get()), new RecipeType[]{Registration.RecipeTypes.GENERATOR_BLASTING});
            registry.addRecipeCatalyst(new ItemStack((ItemLike)Registration.GENERATOR_AUGMENT.get()), new RecipeType[]{Registration.RecipeTypes.GENERATOR_SMOKING});
            registry.addRecipeCatalyst(new ItemStack((ItemLike)Registration.FACTORY_AUGMENT.get()), new RecipeType[]{RecipeTypes.SMELTING});
            registry.addRecipeCatalyst(new ItemStack((ItemLike)Registration.IRON_FURNACE.get()), new RecipeType[]{RecipeTypes.SMELTING});
            registry.addRecipeCatalyst(new ItemStack((ItemLike)Registration.GOLD_FURNACE.get()), new RecipeType[]{RecipeTypes.SMELTING});
            registry.addRecipeCatalyst(new ItemStack((ItemLike)Registration.DIAMOND_FURNACE.get()), new RecipeType[]{RecipeTypes.SMELTING});
            registry.addRecipeCatalyst(new ItemStack((ItemLike)Registration.EMERALD_FURNACE.get()), new RecipeType[]{RecipeTypes.SMELTING});
            registry.addRecipeCatalyst(new ItemStack((ItemLike)Registration.OBSIDIAN_FURNACE.get()), new RecipeType[]{RecipeTypes.SMELTING});
            registry.addRecipeCatalyst(new ItemStack((ItemLike)Registration.CRYSTAL_FURNACE.get()), new RecipeType[]{RecipeTypes.SMELTING});
            registry.addRecipeCatalyst(new ItemStack((ItemLike)Registration.NETHERITE_FURNACE.get()), new RecipeType[]{RecipeTypes.SMELTING});
            registry.addRecipeCatalyst(new ItemStack((ItemLike)Registration.COPPER_FURNACE.get()), new RecipeType[]{RecipeTypes.SMELTING});
            registry.addRecipeCatalyst(new ItemStack((ItemLike)Registration.SILVER_FURNACE.get()), new RecipeType[]{RecipeTypes.SMELTING});
            if (((Boolean)Config.enableRainbowContent.get()).booleanValue()) {
                registry.addRecipeCatalyst(new ItemStack((ItemLike)Registration.MILLION_FURNACE.get()), new RecipeType[]{RecipeTypes.SMELTING});
            }
            registry.addRecipeCatalyst(new ItemStack((ItemLike)Registration.IRON_FURNACE.get()), new RecipeType[]{RecipeTypes.FUELING});
            registry.addRecipeCatalyst(new ItemStack((ItemLike)Registration.GOLD_FURNACE.get()), new RecipeType[]{RecipeTypes.FUELING});
            registry.addRecipeCatalyst(new ItemStack((ItemLike)Registration.DIAMOND_FURNACE.get()), new RecipeType[]{RecipeTypes.FUELING});
            registry.addRecipeCatalyst(new ItemStack((ItemLike)Registration.EMERALD_FURNACE.get()), new RecipeType[]{RecipeTypes.FUELING});
            registry.addRecipeCatalyst(new ItemStack((ItemLike)Registration.OBSIDIAN_FURNACE.get()), new RecipeType[]{RecipeTypes.FUELING});
            registry.addRecipeCatalyst(new ItemStack((ItemLike)Registration.CRYSTAL_FURNACE.get()), new RecipeType[]{RecipeTypes.FUELING});
            registry.addRecipeCatalyst(new ItemStack((ItemLike)Registration.NETHERITE_FURNACE.get()), new RecipeType[]{RecipeTypes.FUELING});
            registry.addRecipeCatalyst(new ItemStack((ItemLike)Registration.COPPER_FURNACE.get()), new RecipeType[]{RecipeTypes.FUELING});
            registry.addRecipeCatalyst(new ItemStack((ItemLike)Registration.SILVER_FURNACE.get()), new RecipeType[]{RecipeTypes.FUELING});
            if (((Boolean)Config.enableRainbowContent.get()).booleanValue()) {
                registry.addRecipeCatalyst(new ItemStack((ItemLike)Registration.MILLION_FURNACE.get()), new RecipeType[]{RecipeTypes.FUELING});
            }
            registry.addRecipeCatalyst(new ItemStack((ItemLike)Registration.BLASTING_AUGMENT.get()), new RecipeType[]{Registration.RecipeTypes.GENERATOR_BLASTING});
            registry.addRecipeCatalyst(new ItemStack((ItemLike)Registration.SMOKING_AUGMENT.get()), new RecipeType[]{Registration.RecipeTypes.GENERATOR_SMOKING});
            if (ModList.get().isLoaded("allthemodium")) {
                registry.addRecipeCatalyst(new ItemStack((ItemLike)Registration.ALLTHEMODIUM_FURNACE.get()), new RecipeType[]{RecipeTypes.SMELTING});
                registry.addRecipeCatalyst(new ItemStack((ItemLike)Registration.VIBRANIUM_FURNACE.get()), new RecipeType[]{RecipeTypes.SMELTING});
                registry.addRecipeCatalyst(new ItemStack((ItemLike)Registration.UNOBTAINIUM_FURNACE.get()), new RecipeType[]{RecipeTypes.SMELTING});
                registry.addRecipeCatalyst(new ItemStack((ItemLike)Registration.ALLTHEMODIUM_FURNACE.get()), new RecipeType[]{RecipeTypes.FUELING});
                registry.addRecipeCatalyst(new ItemStack((ItemLike)Registration.VIBRANIUM_FURNACE.get()), new RecipeType[]{RecipeTypes.FUELING});
                registry.addRecipeCatalyst(new ItemStack((ItemLike)Registration.UNOBTAINIUM_FURNACE.get()), new RecipeType[]{RecipeTypes.FUELING});
            }
        }
    }

    public void registerGuiHandlers(IGuiHandlerRegistration registry) {
    }
}

