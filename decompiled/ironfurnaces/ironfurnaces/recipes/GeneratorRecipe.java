/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.gson.JsonObject
 *  net.minecraft.core.RegistryAccess
 *  net.minecraft.network.FriendlyByteBuf
 *  net.minecraft.resources.ResourceLocation
 *  net.minecraft.util.GsonHelper
 *  net.minecraft.world.Container
 *  net.minecraft.world.item.ItemStack
 *  net.minecraft.world.item.crafting.Recipe
 *  net.minecraft.world.item.crafting.RecipeSerializer
 *  net.minecraft.world.item.crafting.RecipeType
 *  net.minecraft.world.item.crafting.ShapedRecipe
 *  net.minecraft.world.level.Level
 *  org.jetbrains.annotations.Nullable
 */
package ironfurnaces.recipes;

import com.google.gson.JsonObject;
import ironfurnaces.init.Registration;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

public class GeneratorRecipe
implements Recipe<Container> {
    private final ResourceLocation recipeId;
    private int energy;
    private ItemStack stack;

    public GeneratorRecipe(ResourceLocation recipeId, int energy, ItemStack stack) {
        this.recipeId = recipeId;
        this.energy = energy;
        this.stack = stack;
    }

    public boolean m_142505_() {
        return this.stack.m_41619_();
    }

    public ItemStack getIngredient() {
        return this.stack;
    }

    public int getEnergy() {
        return this.energy;
    }

    public static int getTotalCount(Container inventory, ItemStack input) {
        ItemStack stack = inventory.m_8020_(0);
        if (!stack.m_41619_() && stack.m_41720_() == input.m_41720_()) {
            return stack.m_41613_();
        }
        return 0;
    }

    public boolean m_5818_(Container inv, Level level) {
        int required = this.stack.m_41613_();
        int found = GeneratorRecipe.getTotalCount(inv, this.stack);
        return found >= required;
    }

    public ItemStack m_5874_(Container p_44001_, RegistryAccess p_267165_) {
        return ItemStack.f_41583_;
    }

    public boolean m_8004_(int p_43999_, int p_44000_) {
        return true;
    }

    public ItemStack m_8043_(RegistryAccess p_267052_) {
        return this.stack;
    }

    public boolean m_5598_() {
        return true;
    }

    public ResourceLocation m_6423_() {
        return this.recipeId;
    }

    public RecipeSerializer<?> m_7707_() {
        return (RecipeSerializer)Registration.GENERATOR_RECIPE_SERIALIZER.get();
    }

    public RecipeType<?> m_6671_() {
        return (RecipeType)Registration.GENERATOR_RECIPE_TYPE.get();
    }

    public static class Serializer
    implements RecipeSerializer<GeneratorRecipe> {
        public GeneratorRecipe fromJson(ResourceLocation recipeId, JsonObject json) {
            int energy = GsonHelper.m_13824_((JsonObject)json, (String)"energy", (int)10000);
            ItemStack input = ShapedRecipe.m_151274_((JsonObject)json);
            GeneratorRecipe recipe = new GeneratorRecipe(recipeId, energy, input);
            return recipe;
        }

        @Nullable
        public GeneratorRecipe fromNetwork(ResourceLocation recipeId, FriendlyByteBuf buffer) {
            GeneratorRecipe recipe = new GeneratorRecipe(recipeId, buffer.m_130242_(), buffer.m_130267_());
            return recipe;
        }

        public void toNetwork(FriendlyByteBuf buffer, GeneratorRecipe recipe) {
            buffer.m_130130_(recipe.energy);
            buffer.m_130055_(recipe.stack);
        }
    }
}

