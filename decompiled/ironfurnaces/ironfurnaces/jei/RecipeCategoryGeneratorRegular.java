/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.blaze3d.platform.InputConstants$Key
 *  mezz.jei.api.constants.VanillaTypes
 *  mezz.jei.api.gui.builder.IRecipeLayoutBuilder
 *  mezz.jei.api.gui.drawable.IDrawable
 *  mezz.jei.api.gui.drawable.IDrawableAnimated
 *  mezz.jei.api.gui.drawable.IDrawableAnimated$StartDirection
 *  mezz.jei.api.gui.drawable.IDrawableStatic
 *  mezz.jei.api.gui.ingredient.IRecipeSlotsView
 *  mezz.jei.api.helpers.IGuiHelper
 *  mezz.jei.api.ingredients.IIngredientType
 *  mezz.jei.api.recipe.IFocusGroup
 *  mezz.jei.api.recipe.RecipeIngredientRole
 *  mezz.jei.api.recipe.RecipeType
 *  mezz.jei.api.recipe.category.IRecipeCategory
 *  net.minecraft.client.gui.GuiGraphics
 *  net.minecraft.network.chat.Component
 *  net.minecraft.resources.ResourceLocation
 *  net.minecraft.world.item.ItemStack
 *  net.minecraft.world.item.crafting.Ingredient
 *  net.minecraft.world.level.ItemLike
 *  org.apache.commons.compress.utils.Lists
 *  org.jetbrains.annotations.Nullable
 */
package ironfurnaces.jei;

import com.mojang.blaze3d.platform.InputConstants;
import ironfurnaces.init.Registration;
import ironfurnaces.recipes.SimpleGeneratorRecipe;
import ironfurnaces.util.StringHelper;
import java.util.ArrayList;
import java.util.List;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.drawable.IDrawableAnimated;
import mezz.jei.api.gui.drawable.IDrawableStatic;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.ingredients.IIngredientType;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.ItemLike;
import org.apache.commons.compress.utils.Lists;
import org.jetbrains.annotations.Nullable;

public class RecipeCategoryGeneratorRegular
implements IRecipeCategory<SimpleGeneratorRecipe> {
    public static final ResourceLocation UID = new ResourceLocation("ironfurnaces", "category_generator_regular");
    public IGuiHelper guiHelper;
    protected final IDrawableStatic staticFlame;
    protected final IDrawableAnimated animatedFlame;
    protected final IDrawableStatic staticEnergy;
    protected final IDrawableAnimated animatedEnergy;

    public RecipeCategoryGeneratorRegular(IGuiHelper guiHelper) {
        this.guiHelper = guiHelper;
        this.staticFlame = guiHelper.createDrawable(new ResourceLocation("ironfurnaces", "textures/gui/jei.png"), 68, 0, 14, 14);
        this.animatedFlame = guiHelper.createAnimatedDrawable(this.staticFlame, 300, IDrawableAnimated.StartDirection.TOP, true);
        this.staticEnergy = guiHelper.createDrawable(new ResourceLocation("ironfurnaces", "textures/gui/jei.png"), 82, 0, 14, 42);
        this.animatedEnergy = guiHelper.createAnimatedDrawable(this.staticEnergy, 300, IDrawableAnimated.StartDirection.BOTTOM, false);
    }

    public RecipeType<SimpleGeneratorRecipe> getRecipeType() {
        return Registration.RecipeTypes.GENERATOR_REGULAR;
    }

    public Component getTitle() {
        return Component.m_237115_((String)"ironfurnaces.jei_category_regular");
    }

    public IDrawable getBackground() {
        return this.guiHelper.createDrawable(new ResourceLocation("ironfurnaces", "textures/gui/jei.png"), 0, 0, 68, 42);
    }

    public IDrawable getIcon() {
        return this.guiHelper.createDrawableIngredient((IIngredientType)VanillaTypes.ITEM_STACK, (Object)new ItemStack((ItemLike)Registration.GENERATOR_AUGMENT.get()));
    }

    public void setRecipe(IRecipeLayoutBuilder builder, SimpleGeneratorRecipe recipe, IFocusGroup focuses) {
        builder.addSlot(RecipeIngredientRole.INPUT, 1, 18).addIngredients(Ingredient.m_43929_((ItemLike[])new ItemLike[]{recipe.getIngredient().m_41720_()}));
    }

    public void draw(SimpleGeneratorRecipe recipe, IRecipeSlotsView recipeSlotsView, GuiGraphics stack, double mouseX, double mouseY) {
        this.animatedFlame.draw(stack, 1, 1);
        this.animatedEnergy.draw(stack, 54, 0);
    }

    public List<Component> getTooltipStrings(SimpleGeneratorRecipe recipe, IRecipeSlotsView recipeSlotsView, double mouseX, double mouseY) {
        ArrayList list = Lists.newArrayList();
        if (mouseX >= 55.0 && mouseX <= 68.0 && mouseY >= 1.0 && mouseY <= 42.0) {
            list.add(Component.m_237113_((String)StringHelper.displayEnergy(recipe.getEnergy()).get(0)));
        }
        return list;
    }

    public boolean handleInput(SimpleGeneratorRecipe recipe, double mouseX, double mouseY, InputConstants.Key input) {
        return super.handleInput((Object)recipe, mouseX, mouseY, input);
    }

    public boolean isHandled(SimpleGeneratorRecipe recipe) {
        return super.isHandled((Object)recipe);
    }

    @Nullable
    public ResourceLocation getRegistryName(SimpleGeneratorRecipe recipe) {
        return super.getRegistryName((Object)recipe);
    }
}

