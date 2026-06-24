/*
 * Decompiled with CFR.
 */
package ironfurnaces.jei;

import com.mojang.blaze3d.platform.InputConstants;
import ironfurnaces.init.Registration;
import ironfurnaces.recipes.GeneratorRecipe;
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

public class RecipeCategoryGeneratorBlasting
implements IRecipeCategory<GeneratorRecipe> {
    public static final ResourceLocation UID = new ResourceLocation("ironfurnaces", "category_generator_blasting");
    public IGuiHelper guiHelper;
    protected final IDrawableStatic staticFlame;
    protected final IDrawableAnimated animatedFlame;
    protected final IDrawableStatic staticEnergy;
    protected final IDrawableAnimated animatedEnergy;

    public RecipeCategoryGeneratorBlasting(IGuiHelper guiHelper) {
        this.guiHelper = guiHelper;
        this.staticFlame = guiHelper.createDrawable(new ResourceLocation("ironfurnaces", "textures/gui/jei.png"), 68, 0, 14, 14);
        this.animatedFlame = guiHelper.createAnimatedDrawable(this.staticFlame, 300, IDrawableAnimated.StartDirection.TOP, true);
        this.staticEnergy = guiHelper.createDrawable(new ResourceLocation("ironfurnaces", "textures/gui/jei.png"), 82, 0, 14, 42);
        this.animatedEnergy = guiHelper.createAnimatedDrawable(this.staticEnergy, 300, IDrawableAnimated.StartDirection.BOTTOM, false);
    }

    public RecipeType<GeneratorRecipe> getRecipeType() {
        return Registration.RecipeTypes.GENERATOR_BLASTING;
    }

    public Component getTitle() {
        return Component.m_237115_((String)"ironfurnaces.jei_category_blasting");
    }

    public IDrawable getBackground() {
        return this.guiHelper.createDrawable(new ResourceLocation("ironfurnaces", "textures/gui/jei.png"), 0, 0, 68, 42);
    }

    public IDrawable getIcon() {
        return this.guiHelper.createDrawableIngredient((IIngredientType)VanillaTypes.ITEM_STACK, (Object)new ItemStack((ItemLike)Registration.GENERATOR_AUGMENT.get()));
    }

    public void setRecipe(IRecipeLayoutBuilder builder, GeneratorRecipe recipe, IFocusGroup focuses) {
        builder.addSlot(RecipeIngredientRole.INPUT, 1, 18).addIngredients(Ingredient.m_43929_((ItemLike[])new ItemLike[]{recipe.getIngredient().m_41720_()}));
    }

    public void draw(GeneratorRecipe recipe, IRecipeSlotsView recipeSlotsView, GuiGraphics stack, double mouseX, double mouseY) {
        this.animatedFlame.draw(stack, 1, 1);
        this.animatedEnergy.draw(stack, 54, 0);
    }

    public List<Component> getTooltipStrings(GeneratorRecipe recipe, IRecipeSlotsView recipeSlotsView, double mouseX, double mouseY) {
        ArrayList list = Lists.newArrayList();
        if (mouseX >= 55.0 && mouseX <= 68.0 && mouseY >= 1.0 && mouseY <= 42.0) {
            list.add(Component.m_237113_((String)StringHelper.displayEnergy(recipe.getEnergy()).get(0)));
        }
        return list;
    }

    public boolean handleInput(GeneratorRecipe recipe, double mouseX, double mouseY, InputConstants.Key input) {
        return super.handleInput((Object)recipe, mouseX, mouseY, input);
    }

    public boolean isHandled(GeneratorRecipe recipe) {
        return super.isHandled((Object)recipe);
    }

    @Nullable
    public ResourceLocation getRegistryName(GeneratorRecipe recipe) {
        return super.getRegistryName((Object)recipe);
    }
}

