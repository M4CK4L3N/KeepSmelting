/*
 * Decompiled with CFR.
 */
package ironfurnaces.tileentity.furnaces;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import harmonised.pmmo.api.events.FurnaceBurnEvent;
import harmonised.pmmo.events.impl.FurnaceHandler;
import ironfurnaces.Config;
import ironfurnaces.blocks.furnaces.BlockIronFurnaceBase;
import ironfurnaces.blocks.furnaces.BlockMillionFurnace;
import ironfurnaces.capability.CapabilityPlayerFurnacesList;
import ironfurnaces.energy.FEnergyStorage;
import ironfurnaces.init.ModSetup;
import ironfurnaces.init.Registration;
import ironfurnaces.items.ItemHeater;
import ironfurnaces.items.augments.ItemAugment;
import ironfurnaces.items.augments.ItemAugmentBlasting;
import ironfurnaces.items.augments.ItemAugmentBlue;
import ironfurnaces.items.augments.ItemAugmentFactory;
import ironfurnaces.items.augments.ItemAugmentFuel;
import ironfurnaces.items.augments.ItemAugmentGenerator;
import ironfurnaces.items.augments.ItemAugmentGreen;
import ironfurnaces.items.augments.ItemAugmentRed;
import ironfurnaces.items.augments.ItemAugmentSmoking;
import ironfurnaces.items.augments.ItemAugmentSpeed;
import ironfurnaces.recipes.GeneratorRecipe;
import ironfurnaces.tileentity.BlockWirelessEnergyHeaterTile;
import ironfurnaces.tileentity.TileEntityInventory;
import ironfurnaces.tileentity.furnaces.BlockCopperFurnaceTile;
import ironfurnaces.tileentity.furnaces.BlockCrystalFurnaceTile;
import ironfurnaces.tileentity.furnaces.BlockDiamondFurnaceTile;
import ironfurnaces.tileentity.furnaces.BlockEmeraldFurnaceTile;
import ironfurnaces.tileentity.furnaces.BlockGoldFurnaceTile;
import ironfurnaces.tileentity.furnaces.BlockIronFurnaceTile;
import ironfurnaces.tileentity.furnaces.BlockMillionFurnaceTile;
import ironfurnaces.tileentity.furnaces.BlockNetheriteFurnaceTile;
import ironfurnaces.tileentity.furnaces.BlockObsidianFurnaceTile;
import ironfurnaces.tileentity.furnaces.BlockSilverFurnaceTile;
import ironfurnaces.tileentity.furnaces.other.BlockAllthemodiumFurnaceTile;
import ironfurnaces.tileentity.furnaces.other.BlockUnobtainiumFurnaceTile;
import ironfurnaces.tileentity.furnaces.other.BlockVibraniumFurnaceTile;
import ironfurnaces.util.DirectionUtil;
import ironfurnaces.util.FurnaceSettings;
import ironfurnaces.util.LRUCache;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.RegistryAccess;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.Container;
import net.minecraft.world.Containers;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.player.StackedContents;
import net.minecraft.world.inventory.RecipeHolder;
import net.minecraft.world.inventory.StackedContentsCompatible;
import net.minecraft.world.item.AirItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.AbstractCookingRecipe;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.common.ForgeHooks;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.energy.IEnergyStorage;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemHandlerHelper;
import net.minecraftforge.items.wrapper.SidedInvWrapper;
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.NotNull;

public abstract class BlockIronFurnaceTileBase
extends TileEntityInventory
implements RecipeHolder,
StackedContentsCompatible {
    public static final int INPUT = 0;
    public static final int FUEL = 1;
    public static final int OUTPUT = 2;
    public static final int AUGMENT_RED = 3;
    public static final int AUGMENT_GREEN = 4;
    public static final int AUGMENT_BLUE = 5;
    public static final int GENERATOR_FUEL = 6;
    public static final int[] FACTORY_INPUT = new int[]{7, 8, 9, 10, 11, 12};
    public final int[] provides = new int[Direction.values().length];
    protected final int[] lastProvides = new int[this.provides.length];
    public int jovial;
    public int[] currentAugment = new int[3];
    public int[] factoryCookTime = new int[6];
    public int[] factoryTotalCookTime = new int[6];
    public double[] usedRF = new double[6];
    public double generatorBurn;
    public int generatorRecentRecipeRF;
    public double gottenRF;
    public int furnaceBurnTime;
    public int cookTime;
    public int totalCookTime;
    public int recipesUsed;
    public long lastGameTickEnergyUpdated;
    public UUID owner;
    public boolean rainbowGenerating;
    public final Object2IntOpenHashMap<ResourceLocation> recipes = new Object2IntOpenHashMap();
    public RecipeType<? extends AbstractCookingRecipe> recipeType;
    public FurnaceSettings furnaceSettings;
    public LRUCache<Item, Optional<AbstractCookingRecipe>> cache = LRUCache.newInstance((Integer)Config.cache_capacity.get());
    public LRUCache<Item, Optional<AbstractCookingRecipe>> blasting_cache = LRUCache.newInstance((Integer)Config.cache_capacity.get());
    public LRUCache<Item, Optional<AbstractCookingRecipe>> smoking_cache = LRUCache.newInstance((Integer)Config.cache_capacity.get());
    public LRUCache<Item, Optional<GeneratorRecipe>> generator_cache = LRUCache.newInstance((Integer)Config.cache_capacity.get());
    public List<LRUCache<Item, Optional<AbstractCookingRecipe>>> factory_cache = Lists.newArrayList((Object[])new LRUCache[]{LRUCache.newInstance((Integer)Config.cache_capacity.get()), LRUCache.newInstance((Integer)Config.cache_capacity.get()), LRUCache.newInstance((Integer)Config.cache_capacity.get()), LRUCache.newInstance((Integer)Config.cache_capacity.get()), LRUCache.newInstance((Integer)Config.cache_capacity.get()), LRUCache.newInstance((Integer)Config.cache_capacity.get())});
    public List<LRUCache<Item, Optional<AbstractCookingRecipe>>> factory_blasting_cache = Lists.newArrayList((Object[])new LRUCache[]{LRUCache.newInstance((Integer)Config.cache_capacity.get()), LRUCache.newInstance((Integer)Config.cache_capacity.get()), LRUCache.newInstance((Integer)Config.cache_capacity.get()), LRUCache.newInstance((Integer)Config.cache_capacity.get()), LRUCache.newInstance((Integer)Config.cache_capacity.get()), LRUCache.newInstance((Integer)Config.cache_capacity.get())});
    public List<LRUCache<Item, Optional<AbstractCookingRecipe>>> factory_smoking_cache = Lists.newArrayList((Object[])new LRUCache[]{LRUCache.newInstance((Integer)Config.cache_capacity.get()), LRUCache.newInstance((Integer)Config.cache_capacity.get()), LRUCache.newInstance((Integer)Config.cache_capacity.get()), LRUCache.newInstance((Integer)Config.cache_capacity.get()), LRUCache.newInstance((Integer)Config.cache_capacity.get()), LRUCache.newInstance((Integer)Config.cache_capacity.get())});
    public FEnergyStorage energyStorage = new FEnergyStorage((int)((Integer)Config.furnaceEnergyCapacityTier2.get())){

        @Override
        protected void onEnergyChanged() {
            if (BlockIronFurnaceTileBase.this.f_58857_ != null && BlockIronFurnaceTileBase.this.f_58857_.m_7702_(BlockIronFurnaceTileBase.this.m_58899_()) != null) {
                if (BlockIronFurnaceTileBase.this.lastGameTickEnergyUpdated <= 0L) {
                    BlockIronFurnaceTileBase.this.m_6596_();
                    BlockIronFurnaceTileBase.this.lastGameTickEnergyUpdated = BlockIronFurnaceTileBase.this.f_58857_.m_46467_();
                } else if (BlockIronFurnaceTileBase.this.f_58857_.m_46467_() - BlockIronFurnaceTileBase.this.lastGameTickEnergyUpdated >= 20L) {
                    BlockIronFurnaceTileBase.this.m_6596_();
                    BlockIronFurnaceTileBase.this.lastGameTickEnergyUpdated = BlockIronFurnaceTileBase.this.f_58857_.m_46467_();
                }
            }
        }
    };
    public LazyOptional<IEnergyStorage> energy = LazyOptional.of(() -> this.energyStorage);
    LazyOptional<? extends IItemHandler>[] invHandlers = SidedInvWrapper.create((WorldlyContainer)this, (Direction[])new Direction[]{Direction.DOWN, Direction.UP, Direction.NORTH, Direction.SOUTH, Direction.WEST, Direction.EAST});

    public BlockIronFurnaceTileBase(BlockEntityType<?> tileentitytypeIn, BlockPos pos, BlockState state) {
        super(tileentitytypeIn, pos, state, 19);
        this.recipeType = RecipeType.f_44108_;
        this.furnaceSettings = new FurnaceSettings(){

            @Override
            public void onChanged() {
                BlockIronFurnaceTileBase.this.m_6596_();
            }
        };
    }

    public int getEnergy() {
        return this.energyStorage.getEnergy();
    }

    public int getCapacity() {
        return this.energyStorage.getCapacity();
    }

    public void setEnergy(int energy) {
        this.energyStorage.setEnergy(energy);
    }

    public void setMaxEnergy(int energy) {
        this.energyStorage.setCapacity(energy);
    }

    public void removeEnergy(int energy) {
        this.energyStorage.setEnergy(this.energyStorage.getEnergy() - energy);
    }

    public boolean hasRecipe(ItemStack stack) {
        Item item = stack.m_41720_();
        if (this.recipeType == RecipeType.f_44110_) {
            return ModSetup.HAS_RECIPE_SMOKING.computeIfAbsent((Holder.Reference<Item>)ForgeRegistries.ITEMS.getDelegateOrThrow((Object)item), value -> this.f_58857_.m_7465_().m_44015_(this.recipeType, (Container)new SimpleContainer(new ItemStack[]{stack}), this.f_58857_).isPresent());
        }
        if (this.recipeType == RecipeType.f_44109_) {
            return ModSetup.HAS_RECIPE_BLASTING.computeIfAbsent((Holder.Reference<Item>)ForgeRegistries.ITEMS.getDelegateOrThrow((Object)item), value -> this.f_58857_.m_7465_().m_44015_(this.recipeType, (Container)new SimpleContainer(new ItemStack[]{stack}), this.f_58857_).isPresent());
        }
        return ModSetup.HAS_RECIPE.computeIfAbsent((Holder.Reference<Item>)ForgeRegistries.ITEMS.getDelegateOrThrow((Object)item), value -> this.f_58857_.m_7465_().m_44015_(this.recipeType, (Container)new SimpleContainer(new ItemStack[]{stack}), this.f_58857_).isPresent());
    }

    public boolean hasGeneratorBlastingRecipe(ItemStack stack) {
        return this.getRecipeGeneratorBlasting(stack).isPresent();
    }

    protected Optional<AbstractCookingRecipe> getRecipe(ItemStack stack) {
        Optional recipe = this.getCache().computeIfAbsent(stack.m_41720_(), item -> stack.m_41720_() instanceof AirItem ? Optional.empty() : Optional.ofNullable(this.f_58857_.m_7465_().m_44015_(this.recipeType, (Container)new SimpleContainer(new ItemStack[]{stack}), this.f_58857_).orElse(null)));
        return recipe;
    }

    protected Optional<AbstractCookingRecipe> getRecipeFactory(int slot, ItemStack stack) {
        Optional recipe = this.getFactoryCache().get(slot - FACTORY_INPUT[0]).computeIfAbsent(stack.m_41720_(), item -> stack.m_41720_() instanceof AirItem ? Optional.empty() : Optional.ofNullable(this.f_58857_.m_7465_().m_44015_(this.recipeType, (Container)new SimpleContainer(new ItemStack[]{stack}), this.f_58857_).orElse(null)));
        return recipe;
    }

    protected Optional<AbstractCookingRecipe> getRecipeNonCached(ItemStack stack) {
        return stack.m_41720_() instanceof AirItem ? Optional.empty() : Optional.ofNullable(this.f_58857_.m_7465_().m_44015_(this.recipeType, (Container)new SimpleContainer(new ItemStack[]{stack}), this.f_58857_).orElse(null));
    }

    protected Optional<GeneratorRecipe> getRecipeGeneratorBlasting(ItemStack item) {
        return item.m_41720_() instanceof AirItem ? Optional.empty() : Optional.ofNullable(this.f_58857_.m_7465_().m_44015_((RecipeType)Registration.GENERATOR_RECIPE_TYPE.get(), (Container)new SimpleContainer(new ItemStack[]{item}), this.f_58857_).orElse(null));
    }

    protected void checkRecipeType() {
        ItemStack stack = this.m_8020_(3);
        if (stack.m_41720_() instanceof ItemAugmentBlasting && this.recipeType != RecipeType.f_44109_) {
            this.recipeType = RecipeType.f_44109_;
        }
        if (stack.m_41720_() instanceof ItemAugmentSmoking && this.recipeType != RecipeType.f_44110_) {
            this.recipeType = RecipeType.f_44110_;
        }
        if (!(stack.m_41720_() instanceof ItemAugmentSmoking) && !(stack.m_41720_() instanceof ItemAugmentBlasting) && this.recipeType != RecipeType.f_44108_) {
            this.recipeType = RecipeType.f_44108_;
        }
    }

    protected LRUCache<Item, Optional<AbstractCookingRecipe>> getCache() {
        this.checkRecipeType();
        if (this.recipeType == RecipeType.f_44109_) {
            return this.blasting_cache;
        }
        if (this.recipeType == RecipeType.f_44110_) {
            return this.smoking_cache;
        }
        return this.cache;
    }

    protected List<LRUCache<Item, Optional<AbstractCookingRecipe>>> getFactoryCache() {
        this.checkRecipeType();
        if (this.recipeType == RecipeType.f_44109_) {
            return this.factory_blasting_cache;
        }
        if (this.recipeType == RecipeType.f_44110_) {
            return this.factory_smoking_cache;
        }
        return this.factory_cache;
    }

    public int getCookTime() {
        ItemStack stack = this.m_8020_(4);
        if (this.m_8020_(0).m_41720_() == Items.f_41852_) {
            return this.totalCookTime;
        }
        int speed = this.getSpeed();
        if (!stack.m_41619_()) {
            if (stack.m_41720_() instanceof ItemAugmentSpeed) {
                speed = Math.max(1, speed / 2);
            }
            if (stack.m_41720_() instanceof ItemAugmentFuel) {
                speed = Math.max(1, (int)Math.ceil((double)speed * 1.25));
            }
        }
        return Math.max(1, speed);
    }

    protected int getSpeed() {
        int regular = (Integer)this.getCookTimeConfig().get();
        int recipe = this.getCache().computeIfAbsent(this.m_8020_(0).m_41720_(), item -> this.getRecipeNonCached(new ItemStack((ItemLike)item))).map(AbstractCookingRecipe::m_43753_).orElse(0);
        double div = 200.0 / (double)recipe;
        double i = (double)regular / div;
        return (int)Math.max(1.0, i);
    }

    protected int getFactoryCookTime(int slot) {
        ItemStack stack = this.m_8020_(4);
        if (this.m_8020_(slot).m_41720_() == Items.f_41852_) {
            return this.factoryTotalCookTime[slot - FACTORY_INPUT[0]];
        }
        int speed = this.getFactorySpeed(slot);
        if (!stack.m_41619_()) {
            if (stack.m_41720_() instanceof ItemAugmentSpeed) {
                speed = Math.max(1, speed / 2);
            }
            if (stack.m_41720_() instanceof ItemAugmentFuel) {
                speed = Math.max(1, (int)Math.ceil((double)speed * 1.25));
            }
        }
        return Math.max(1, speed);
    }

    protected int getFactorySpeed(int slot) {
        int regular = (Integer)this.getCookTimeConfig().get();
        int recipe = this.getFactoryCache().get(slot - FACTORY_INPUT[0]).computeIfAbsent(this.m_8020_(slot).m_41720_(), item -> this.getRecipeNonCached(new ItemStack((ItemLike)item))).map(AbstractCookingRecipe::m_43753_).orElse(0);
        double div = 200.0 / (double)recipe;
        double i = (double)regular / div;
        return (int)Math.max(1.0, i);
    }

    public ForgeConfigSpec.IntValue getCookTimeConfig() {
        return null;
    }

    protected int getAugment(ItemStack stack) {
        if (stack.m_41720_() instanceof ItemAugmentBlasting) {
            return 1;
        }
        if (stack.m_41720_() instanceof ItemAugmentSmoking) {
            return 2;
        }
        if (stack.m_41720_() instanceof ItemAugmentSpeed) {
            return 1;
        }
        if (stack.m_41720_() instanceof ItemAugmentFuel) {
            return 2;
        }
        if (stack.m_41720_() instanceof ItemAugmentFactory) {
            return 1;
        }
        if (stack.m_41720_() instanceof ItemAugmentGenerator) {
            return 2;
        }
        return 0;
    }

    public void forceUpdateAllStates() {
        BlockState state = this.f_58857_.m_8055_(this.f_58858_);
        if (((Boolean)state.m_61143_((Property)BlockStateProperties.f_61443_)).booleanValue() != this.isBurning()) {
            this.f_58857_.m_7731_(this.f_58858_, (BlockState)state.m_61124_((Property)BlockStateProperties.f_61443_, (Comparable)Boolean.valueOf(this.isBurning())), 3);
        }
        if (((Integer)state.m_61143_((Property)BlockIronFurnaceBase.TYPE)).intValue() != this.getStateType()) {
            this.f_58857_.m_7731_(this.f_58858_, (BlockState)state.m_61124_((Property)BlockIronFurnaceBase.TYPE, (Comparable)Integer.valueOf(this.getStateType())), 3);
        }
        if ((Integer)state.m_61143_((Property)BlockIronFurnaceBase.JOVIAL) != this.jovial) {
            this.f_58857_.m_7731_(this.f_58858_, (BlockState)state.m_61124_((Property)BlockIronFurnaceBase.JOVIAL, (Comparable)Integer.valueOf(this.jovial)), 3);
        }
    }

    public void dropContents() {
        for (int i = 0; i <= 18; ++i) {
            if (i >= 3 && i <= 5) continue;
            ItemStack stack = this.m_8020_(i);
            Containers.m_18992_((Level)this.f_58857_, (double)this.f_58858_.m_123341_(), (double)this.f_58858_.m_123342_(), (double)this.f_58858_.m_123343_(), (ItemStack)stack);
        }
    }

    public int getGeneration() {
        int rf = 0;
        if (this instanceof BlockCopperFurnaceTile) {
            rf = (Integer)Config.copperFurnaceGeneration.get();
        } else if (this instanceof BlockIronFurnaceTile) {
            rf = (Integer)Config.ironFurnaceGeneration.get();
        } else if (this instanceof BlockSilverFurnaceTile) {
            rf = (Integer)Config.silverFurnaceGeneration.get();
        } else if (this instanceof BlockGoldFurnaceTile) {
            rf = (Integer)Config.goldFurnaceGeneration.get();
        } else if (this instanceof BlockDiamondFurnaceTile) {
            rf = (Integer)Config.diamondFurnaceGeneration.get();
        } else if (this instanceof BlockEmeraldFurnaceTile) {
            rf = (Integer)Config.emeraldFurnaceGeneration.get();
        } else if (this instanceof BlockCrystalFurnaceTile) {
            rf = (Integer)Config.crystalFurnaceGeneration.get();
        } else if (this instanceof BlockObsidianFurnaceTile) {
            rf = (Integer)Config.obsidianFurnaceGeneration.get();
        } else if (this instanceof BlockNetheriteFurnaceTile) {
            rf = (Integer)Config.netheriteFurnaceGeneration.get();
        } else if (this instanceof BlockMillionFurnaceTile) {
            rf = (Integer)Config.millionFurnaceGeneration.get();
        } else if (this instanceof BlockAllthemodiumFurnaceTile) {
            rf = (Integer)Config.allthemodiumGeneration.get();
        } else if (this instanceof BlockVibraniumFurnaceTile) {
            rf = (Integer)Config.vibraniumGeneration.get();
        } else if (this instanceof BlockUnobtainiumFurnaceTile) {
            rf = (Integer)Config.unobtainiumGeneration.get();
        }
        return this.m_8020_(4).m_41720_() instanceof ItemAugmentSpeed ? rf * 2 : (this.m_8020_(4).m_41720_() instanceof ItemAugmentFuel ? (int)((double)rf * 0.75) : rf);
    }

    public static int getSmokingBurn(ItemStack stack) {
        if (stack.m_41619_()) {
            return 0;
        }
        Item item = stack.m_41720_();
        return ModSetup.SMOKING_BURNS.getOrDefault(ForgeRegistries.ITEMS.getDelegateOrThrow((Object)item), BlockIronFurnaceTileBase.addSmokingBurn(stack));
    }

    public static int addSmokingBurn(ItemStack stack) {
        int burnTime = BlockIronFurnaceTileBase.getSmokingBurnTime(stack);
        Item item = stack.m_41720_();
        ModSetup.SMOKING_BURNS.put((Holder.Reference<Item>)ForgeRegistries.ITEMS.getDelegateOrThrow((Object)item), burnTime);
        return 0;
    }

    public static int getSmokingBurnTime(ItemStack stack) {
        if (!stack.m_41619_() && stack.m_41720_().m_41473_() != null && stack.m_41720_().m_41473_().m_38744_() > 0) {
            return stack.m_41720_().m_41473_().m_38744_() * 800;
        }
        return 0;
    }

    public int getGeneratorBurn() {
        int burn = 0;
        if (this.m_8020_(3).m_41720_() instanceof ItemAugmentSmoking) {
            burn = BlockIronFurnaceTileBase.getSmokingBurn(this.m_8020_(6));
        } else if (this.m_8020_(3).m_41720_() instanceof ItemAugmentBlasting) {
            if (!this.m_8020_(6).m_41619_()) {
                int energy = this.generator_cache.computeIfAbsent(this.m_8020_(6).m_41720_(), item -> this.getRecipeGeneratorBlasting(new ItemStack((ItemLike)item))).map(GeneratorRecipe::getEnergy).orElse(0);
                burn = energy / 20;
            }
        } else {
            burn = BlockIronFurnaceTileBase.getBurnTime(this.m_8020_(6), RecipeType.f_44108_);
        }
        if (this.m_8020_(4).m_41720_() instanceof ItemAugmentSpeed) {
            burn /= 2;
        } else if (this.m_8020_(4).m_41720_() instanceof ItemAugmentFuel) {
            burn *= 2;
        }
        return burn;
    }

    public boolean isFactoryCooking() {
        for (int i = 0; i < this.factoryCookTime.length; ++i) {
            if (this.factoryCookTime[i] <= 0) continue;
            return true;
        }
        return false;
    }

    public Map<Integer, Integer> getSplitCounts(int[] slot, int[] input) {
        if (slot.length != input.length) {
            return null;
        }
        HashMap output = Maps.newHashMap();
        double sum = 0.0;
        for (int i = 0; i < input.length; ++i) {
            sum += (double)input[i];
        }
        double splitted = sum / (double)input.length;
        if (sum % (double)input.length != 0.0) {
            if (Math.floor(splitted) < splitted) {
                double lowest = Math.floor(sum / (double)input.length) * (double)input.length;
                int itemsLeftOver = (int)sum - (int)lowest;
                for (int i = 0; i < input.length; ++i) {
                    if (itemsLeftOver > 0) {
                        input[i] = (int)Math.ceil(splitted);
                        --itemsLeftOver;
                        continue;
                    }
                    input[i] = (int)splitted;
                }
            }
        } else {
            for (int i = 0; i < input.length; ++i) {
                input[i] = (int)splitted;
            }
        }
        for (int i = 0; i < input.length; ++i) {
            output.put(slot[i], input[i]);
        }
        return output;
    }

    public void fillEmptySlots(int start, int size) {
        int amount = 0;
        for (int i = start; i < size; ++i) {
            if (!this.m_8020_(FACTORY_INPUT[i]).m_41619_()) continue;
            ++amount;
        }
        if (amount == 0) {
            return;
        }
        ItemStack stack = ItemStack.f_41583_;
        for (int j = start; j < size; ++j) {
            if (this.m_8020_(FACTORY_INPUT[j]).m_41619_() || this.m_8020_(FACTORY_INPUT[j]).m_41613_() <= 1 || amount <= 0) continue;
            if (amount >= this.m_8020_(FACTORY_INPUT[j]).m_41613_()) {
                amount = this.m_8020_(FACTORY_INPUT[j]).m_41613_() - 1;
            }
            CompoundTag stackTag = this.m_8020_(FACTORY_INPUT[j]).m_41783_();
            stack = new ItemStack((ItemLike)this.m_8020_(FACTORY_INPUT[j]).m_41720_());
            stack.m_41751_(stackTag);
            this.m_8020_(FACTORY_INPUT[j]).m_41774_(amount);
            for (int i = start; i < size; ++i) {
                if (!this.m_8020_(FACTORY_INPUT[i]).m_41619_() || amount <= 0) continue;
                this.m_6836_(FACTORY_INPUT[i], stack.m_41777_());
                --amount;
                this.m_6596_();
            }
            this.m_6596_();
            break;
        }
    }

    public void split(boolean fullCheck, int start, int size) {
        int i;
        ItemStack itemToCheck = ItemStack.f_41583_;
        int fullCheckCount = 0;
        if (!fullCheck) {
            for (i = start; i < size; ++i) {
                if (!this.m_8020_(FACTORY_INPUT[i]).m_41619_()) continue;
                ++fullCheckCount;
            }
            if (fullCheckCount == 0) {
                return;
            }
        }
        for (i = start; i < size; ++i) {
            if (this.m_8020_(FACTORY_INPUT[i]).m_41619_()) continue;
            itemToCheck = this.m_8020_(FACTORY_INPUT[i]).m_41777_();
        }
        if (itemToCheck.m_41619_()) {
            return;
        }
        this.fillEmptySlots(start, size);
        HashMap items = Maps.newHashMap();
        Map<Object, Object> setCounts = Maps.newHashMap();
        for (int i2 = start; i2 < size; ++i2) {
            if (this.m_8020_(FACTORY_INPUT[i2]).m_41619_() || this.m_8020_(FACTORY_INPUT[i2]).m_41720_() != itemToCheck.m_41720_()) continue;
            items.put(FACTORY_INPUT[i2], this.m_8020_(FACTORY_INPUT[i2]).m_41613_());
        }
        if (items.isEmpty()) {
            return;
        }
        int[] slot = new int[items.size()];
        int[] input = new int[items.size()];
        int j = 0;
        for (Map.Entry itemEntry : items.entrySet()) {
            slot[j] = (Integer)itemEntry.getKey();
            input[j] = (Integer)itemEntry.getValue();
            ++j;
        }
        setCounts = this.getSplitCounts(slot, input);
        int check = 0;
        for (Map.Entry<Object, Object> countsEntry : setCounts.entrySet()) {
            int count = this.m_8020_((Integer)countsEntry.getKey()).m_41613_();
            if (count != (Integer)countsEntry.getValue()) continue;
            ++check;
        }
        if (check == setCounts.size()) {
            return;
        }
        for (Map.Entry<Object, Object> countsEntry : setCounts.entrySet()) {
            CompoundTag newTag = this.m_8020_((Integer)countsEntry.getKey()).m_41783_();
            ItemStack newStack = new ItemStack((ItemLike)this.m_8020_((Integer)countsEntry.getKey()).m_41720_(), ((Integer)countsEntry.getValue()).intValue());
            newStack.m_41751_(newTag);
            this.m_6836_((Integer)countsEntry.getKey(), newStack);
            this.m_6596_();
        }
    }

    boolean rainbowCheckFurnaceTiers(List<BlockIronFurnaceTileBase> list) {
        if (list.isEmpty()) {
            return false;
        }
        int check = 0;
        for (BlockIronFurnaceTileBase furnace : list) {
            if (!(furnace.generatorBurn > 0.0) || furnace.getEnergy() >= furnace.getCapacity()) continue;
            ++check;
        }
        return check != 0;
    }

    public static void tick(Level level, BlockPos worldPosition, BlockState blockState, final BlockIronFurnaceTileBase e) {
        if (!e.f_58857_.f_46443_ && e.isGenerator()) {
            boolean flag3 = false;
            ArrayList<BlockIronFurnaceTileBase> iron = new ArrayList<BlockIronFurnaceTileBase>();
            ArrayList<BlockIronFurnaceTileBase> gold = new ArrayList<BlockIronFurnaceTileBase>();
            ArrayList<BlockIronFurnaceTileBase> diamond = new ArrayList<BlockIronFurnaceTileBase>();
            Direction[] emerald = new ArrayList();
            ArrayList<BlockIronFurnaceTileBase> obsidian = new ArrayList<BlockIronFurnaceTileBase>();
            ArrayList<BlockIronFurnaceTileBase> crystal = new ArrayList<BlockIronFurnaceTileBase>();
            ArrayList<BlockIronFurnaceTileBase> netherite = new ArrayList<BlockIronFurnaceTileBase>();
            ArrayList<BlockIronFurnaceTileBase> copper = new ArrayList<BlockIronFurnaceTileBase>();
            ArrayList<BlockIronFurnaceTileBase> silver = new ArrayList<BlockIronFurnaceTileBase>();
            ArrayList<BlockMillionFurnaceTile> rainbow = new ArrayList<BlockMillionFurnaceTile>();
            if (e instanceof BlockMillionFurnaceTile) {
                int i;
                BlockMillionFurnaceTile furnaceTile = (BlockMillionFurnaceTile)e;
                if (furnaceTile.owner != null) {
                    List furnacesBlockPos;
                    flag3 = true;
                    if (level.m_46003_(furnaceTile.owner) != null && !(furnacesBlockPos = (List)level.m_46003_(furnaceTile.owner).getCapability(CapabilityPlayerFurnacesList.FURNACES_LIST).map(h -> h.get()).orElse(new ArrayList())).isEmpty()) {
                        for (i = 0; i < furnacesBlockPos.size(); ++i) {
                            level.m_46745_((BlockPos)furnacesBlockPos.get(i)).m_62913_(true);
                            BlockEntity be = level.m_7702_((BlockPos)furnacesBlockPos.get(i));
                            if (be == null || !(be instanceof BlockIronFurnaceTileBase)) continue;
                            BlockIronFurnaceTileBase te = (BlockIronFurnaceTileBase)be;
                            if (te instanceof BlockIronFurnaceTile) {
                                iron.add((BlockIronFurnaceTile)te);
                            }
                            if (te instanceof BlockGoldFurnaceTile) {
                                gold.add((BlockGoldFurnaceTile)te);
                            }
                            if (te instanceof BlockDiamondFurnaceTile) {
                                diamond.add((BlockDiamondFurnaceTile)te);
                            }
                            if (te instanceof BlockEmeraldFurnaceTile) {
                                emerald.add((BlockEmeraldFurnaceTile)te);
                            }
                            if (te instanceof BlockObsidianFurnaceTile) {
                                obsidian.add((BlockObsidianFurnaceTile)te);
                            }
                            if (te instanceof BlockCrystalFurnaceTile) {
                                crystal.add((BlockCrystalFurnaceTile)te);
                            }
                            if (te instanceof BlockNetheriteFurnaceTile) {
                                netherite.add((BlockNetheriteFurnaceTile)te);
                            }
                            if (te instanceof BlockCopperFurnaceTile) {
                                copper.add((BlockCopperFurnaceTile)te);
                            }
                            if (te instanceof BlockSilverFurnaceTile) {
                                silver.add((BlockSilverFurnaceTile)te);
                            }
                            if (!(te instanceof BlockMillionFurnaceTile)) continue;
                            rainbow.add((BlockMillionFurnaceTile)te);
                        }
                    }
                }
                if (rainbow.size() > 1) {
                    int rainbowGens = 0;
                    for (i = 0; i < rainbow.size(); ++i) {
                        if (!((BlockIronFurnaceTileBase)rainbow.get(i)).isGenerator()) continue;
                        ++rainbowGens;
                    }
                    if (rainbowGens > 1) {
                        flag3 = false;
                    }
                }
                if (flag3 && e.rainbowCheckFurnaceTiers(iron) && e.rainbowCheckFurnaceTiers(gold) && e.rainbowCheckFurnaceTiers(diamond) && e.rainbowCheckFurnaceTiers((List<BlockIronFurnaceTileBase>)emerald) && e.rainbowCheckFurnaceTiers(obsidian) && e.rainbowCheckFurnaceTiers(crystal) && e.rainbowCheckFurnaceTiers(netherite) && e.rainbowCheckFurnaceTiers(copper) && e.rainbowCheckFurnaceTiers(silver)) {
                    e.rainbowGenerating = flag3;
                    state = level.m_8055_(worldPosition);
                    if ((Boolean)state.m_61143_((Property)BlockMillionFurnace.RAINBOW_GENERATING) != e.rainbowGenerating) {
                        level.m_7731_(worldPosition, (BlockState)state.m_61124_((Property)BlockMillionFurnace.RAINBOW_GENERATING, (Comparable)Boolean.valueOf(e.rainbowGenerating)), 3);
                    }
                    e.rainbowEnergyOut();
                } else {
                    e.rainbowGenerating = false;
                    state = level.m_8055_(worldPosition);
                    if ((Boolean)state.m_61143_((Property)BlockMillionFurnace.RAINBOW_GENERATING) != e.rainbowGenerating) {
                        level.m_7731_(worldPosition, (BlockState)state.m_61124_((Property)BlockMillionFurnace.RAINBOW_GENERATING, (Comparable)Boolean.valueOf(e.rainbowGenerating)), 3);
                    }
                }
            }
        }
        boolean flag1 = false;
        boolean wasBurning = e.isBurning();
        if (e.furnaceSettings.size() <= 0) {
            e.furnaceSettings = new FurnaceSettings(){

                @Override
                public void onChanged() {
                    e.m_6596_();
                }
            };
        }
        for (int i = 3; i <= 5; ++i) {
            if (e.currentAugment[i - 3] == e.getAugment(e.m_8020_(i))) continue;
            e.currentAugment[i - 3] = e.getAugment(e.m_8020_(i));
            e.furnaceBurnTime = 0;
            e.generatorBurn = 0.0;
            if (i - 3 != 2 && (!e.isGenerator() || i - 3 != 0)) continue;
            e.dropContents();
        }
        if (!e.f_58857_.f_46443_) {
            int mode;
            if (e.getCapacity() != e.getCapacityFromTier()) {
                e.setMaxEnergy(e.getCapacityFromTier());
            }
            if (e.totalCookTime != e.getCookTime()) {
                e.totalCookTime = e.getCookTime();
            }
            if ((mode = e.getRedstoneSetting()) != 0) {
                if (mode == 2) {
                    int i = 0;
                    for (Direction side : Direction.values()) {
                        if (level.m_277185_(worldPosition.m_121955_(side.m_122436_()), side) <= 0) continue;
                        ++i;
                    }
                    if (i != 0) {
                        e.cookTime = 0;
                        e.furnaceBurnTime = 0;
                        e.forceUpdateAllStates();
                        return;
                    }
                }
                if (mode == 1) {
                    boolean flag = false;
                    for (Direction side : Direction.values()) {
                        if (level.m_277185_(worldPosition.m_121955_(side.m_122436_()), side) <= 0) continue;
                        flag = true;
                    }
                    if (!flag) {
                        e.cookTime = 0;
                        e.furnaceBurnTime = 0;
                        e.forceUpdateAllStates();
                        return;
                    }
                }
                for (i = 0; i < Direction.values().length; ++i) {
                    e.provides[i] = e.m_58900_().m_60775_((BlockGetter)e.f_58857_, worldPosition, DirectionUtil.fromId(i));
                }
            } else {
                for (i = 0; i < Direction.values().length; ++i) {
                    e.provides[i] = 0;
                }
            }
            if (e.doesNeedUpdateSend()) {
                e.onUpdateSent();
            }
        }
        if (e.isFactory()) {
            if (!e.f_58857_.f_46443_) {
                int size;
                int start;
                e.getCapability(ForgeCapabilities.ENERGY).ifPresent(h -> {
                    if (!h.canReceive()) {
                        ((FEnergyStorage)((Object)h)).setMaxReceive(h.getMaxEnergyStored());
                    }
                    if (h.canExtract()) {
                        ((FEnergyStorage)((Object)h)).setMaxExtract(0);
                    }
                });
                e.checkRecipeType();
                int n = e.getTier() == 0 ? 2 : (start = e.getTier() == 1 ? 1 : 0);
                int n2 = e.getTier() == 0 ? 4 : (size = e.getTier() == 1 ? 5 : 6);
                if (e.isAutoSplit()) {
                    e.split(false, start, size);
                }
                for (int i = start; i < size; ++i) {
                    int slot = FACTORY_INPUT[i];
                    if (e.factoryTotalCookTime[i] != e.getFactoryCookTime(slot)) {
                        e.factoryTotalCookTime[i] = e.getFactoryCookTime(slot);
                    }
                    if (!e.m_8020_(slot).m_41619_()) {
                        int energy;
                        Optional<AbstractCookingRecipe> irecipe = e.getRecipeFactory(slot, e.m_8020_(slot));
                        boolean valid = e.canFactorySmelt(irecipe.orElse(null), slot);
                        if (!valid) continue;
                        int energyRecipe = irecipe.get().m_43753_() * 20;
                        int n3 = e.m_8020_(4).m_41720_() instanceof ItemAugmentSpeed ? energyRecipe * 2 : (energy = e.m_8020_(4).m_41720_() instanceof ItemAugmentFuel ? energyRecipe / 2 : energyRecipe);
                        if (e.getEnergy() < energy && e.factoryCookTime[i] <= 0) continue;
                        int n4 = i;
                        e.factoryCookTime[n4] = e.factoryCookTime[n4] + 1;
                        int n5 = i;
                        e.usedRF[n5] = e.usedRF[n5] + (double)(energy / e.factoryTotalCookTime[i]);
                        e.setEnergy((int)((double)e.getEnergy() - (double)(energy / e.factoryTotalCookTime[i])));
                        if (((Boolean)level.m_8055_(e.m_58899_()).m_61143_((Property)BlockStateProperties.f_61443_)).booleanValue() != e.isFactoryCooking()) {
                            level.m_7731_(worldPosition, (BlockState)level.m_8055_(worldPosition).m_61124_((Property)BlockStateProperties.f_61443_, (Comparable)Boolean.valueOf(e.isFactoryCooking())), 3);
                        }
                        if (e.factoryCookTime[i] < e.factoryTotalCookTime[i]) continue;
                        e.factoryCookTime[i] = 0;
                        if (e.usedRF[i] < (double)energy) {
                            double diff = (double)energy - e.usedRF[i];
                            e.setEnergy((int)((double)e.getEnergy() - diff));
                        }
                        e.usedRF[i] = 0.0;
                        e.factoryTotalCookTime[i] = e.getFactoryCookTime(slot);
                        if (e.isAutoSplit()) {
                            e.split(true, start, size);
                        }
                        e.factorySmelt(irecipe.orElse(null), slot);
                        e.m_6596_();
                        continue;
                    }
                    e.factoryCookTime[i] = 0;
                    if (((Boolean)level.m_8055_(e.m_58899_()).m_61143_((Property)BlockStateProperties.f_61443_)).booleanValue() == e.isFactoryCooking()) continue;
                    level.m_7731_(worldPosition, (BlockState)level.m_8055_(worldPosition).m_61124_((Property)BlockStateProperties.f_61443_, (Comparable)Boolean.valueOf(e.isFactoryCooking())), 3);
                }
                if (e.f_58857_.m_46467_() % 24L == 0L) {
                    BlockState state = level.m_8055_(worldPosition);
                    if (((Integer)state.m_61143_((Property)BlockIronFurnaceBase.TYPE)).intValue() != e.getStateType()) {
                        level.m_7731_(worldPosition, (BlockState)state.m_61124_((Property)BlockIronFurnaceBase.TYPE, (Comparable)Integer.valueOf(e.getStateType())), 3);
                    }
                    if ((Integer)state.m_61143_((Property)BlockIronFurnaceBase.JOVIAL) != e.jovial) {
                        level.m_7731_(worldPosition, (BlockState)state.m_61124_((Property)BlockIronFurnaceBase.JOVIAL, (Comparable)Integer.valueOf(e.jovial)), 3);
                    }
                    for (int i = 0; i < e.factoryCookTime.length; ++i) {
                        int j;
                        if (e.factoryCookTime[i] > 0) continue;
                        for (j = 0; j < FACTORY_INPUT.length; ++j) {
                            if (e.m_8020_(FACTORY_INPUT[j]).m_41619_()) {
                                e.autoFactoryIO();
                                e.m_6596_();
                                continue;
                            }
                            if (e.m_8020_(FACTORY_INPUT[j]).m_41613_() >= e.m_8020_(FACTORY_INPUT[j]).m_41741_()) continue;
                            e.autoFactoryIO();
                            e.m_6596_();
                        }
                        for (j = 0; j < FACTORY_INPUT.length; ++j) {
                            int outputSlot = FACTORY_INPUT[j] + 6;
                            if (e.m_8020_(outputSlot).m_41619_() || e.m_8020_(outputSlot).m_41613_() < 64) continue;
                            e.autoFactoryIO();
                        }
                    }
                }
            }
        } else if (e.isGenerator()) {
            if (!level.f_46443_) {
                e.getCapability(ForgeCapabilities.ENERGY).ifPresent(h -> {
                    if (h.canReceive()) {
                        ((FEnergyStorage)((Object)h)).setMaxReceive(0);
                    }
                    if (!h.canExtract()) {
                        ((FEnergyStorage)((Object)h)).setMaxExtract(h.getMaxEnergyStored());
                    }
                });
                if (e.getEnergy() < e.getCapacity()) {
                    if (!e.m_8020_(6).m_41619_() && e.generatorBurn <= 0.0) {
                        e.generatorBurn = e.getGeneratorBurn();
                        e.generatorRecentRecipeRF = (int)e.generatorBurn;
                        if (e.m_8020_(6).hasCraftingRemainingItem()) {
                            e.m_6836_(6, e.m_8020_(6).getCraftingRemainingItem());
                        } else if (!e.m_8020_(6).m_41619_()) {
                            e.m_8020_(6).m_41774_(1);
                            if (e.m_8020_(6).m_41619_()) {
                                e.m_6836_(6, e.m_8020_(6).getCraftingRemainingItem());
                            }
                        }
                        e.m_6596_();
                    }
                    if (e.isGenerator() && (Boolean)level.m_8055_(e.m_58899_()).m_61143_((Property)BlockStateProperties.f_61443_) != e.generatorBurn > 0.0) {
                        level.m_7731_(worldPosition, (BlockState)level.m_8055_(worldPosition).m_61124_((Property)BlockStateProperties.f_61443_, (Comparable)Boolean.valueOf(e.generatorBurn > 0.0)), 3);
                    }
                    if (e.generatorBurn > 0.0) {
                        double max = e.generatorRecentRecipeRF * 20;
                        e.gottenRF += (double)e.getGeneration();
                        e.setEnergy(e.getEnergy() + e.getGeneration());
                        if (e.generatorBurn - (double)e.getGeneration() / 20.0 <= 0.0) {
                            if (e.gottenRF + (double)e.getGeneration() > max && e.gottenRF + (double)e.getGeneration() < (double)e.getCapacity()) {
                                int diff = (int)(e.gottenRF + (double)e.getGeneration() - max);
                                e.setEnergy(e.getEnergy() + e.getGeneration());
                                e.removeEnergy(diff);
                            }
                            if (e.gottenRF + (double)e.getGeneration() < max) {
                                int diff = (int)(max - e.gottenRF + (double)e.getGeneration());
                                e.setEnergy(e.getEnergy() + e.getGeneration());
                                e.setEnergy(e.getEnergy() + diff);
                            }
                            e.gottenRF = 0.0;
                        }
                        e.generatorBurn -= (double)e.getGeneration() / 20.0;
                        if (e.generatorBurn <= 0.0) {
                            e.autoIOGenerator();
                            e.generatorBurn = 0.0;
                        }
                    }
                }
                if (e.generatorBurn <= 0.0) {
                    e.generatorBurn = 0.0;
                }
                e.energyOut();
                if (e.f_58857_.m_46467_() % 24L == 0L && e.generatorBurn <= 0.0) {
                    if (e.m_8020_(6).m_41619_()) {
                        e.autoIOGenerator();
                        e.m_6596_();
                    } else if (e.m_8020_(6).m_41613_() < e.m_8020_(6).m_41741_()) {
                        e.autoIOGenerator();
                        e.m_6596_();
                    }
                }
            }
            if (e.f_58857_.m_46467_() % 24L == 0L) {
                BlockState state = level.m_8055_(worldPosition);
                if (((Integer)state.m_61143_((Property)BlockIronFurnaceBase.TYPE)).intValue() != e.getStateType()) {
                    level.m_7731_(worldPosition, (BlockState)state.m_61124_((Property)BlockIronFurnaceBase.TYPE, (Comparable)Integer.valueOf(e.getStateType())), 3);
                }
                if ((Integer)state.m_61143_((Property)BlockIronFurnaceBase.JOVIAL) != e.jovial) {
                    level.m_7731_(worldPosition, (BlockState)state.m_61124_((Property)BlockIronFurnaceBase.JOVIAL, (Comparable)Integer.valueOf(e.jovial)), 3);
                }
            }
        } else if (e.isFurnace()) {
            e.getCapability(ForgeCapabilities.ENERGY).ifPresent(h -> {
                if (h.canReceive()) {
                    ((FEnergyStorage)((Object)h)).setMaxReceive(0);
                }
                if (h.canExtract()) {
                    ((FEnergyStorage)((Object)h)).setMaxExtract(0);
                }
            });
            if (!e.f_58857_.f_46443_) {
                if (e.isBurning()) {
                    --e.furnaceBurnTime;
                }
                e.checkRecipeType();
                ItemStack itemstack = e.m_8020_(1);
                if (e.isBurning() || !itemstack.m_41619_() && !e.m_8020_(0).m_41619_()) {
                    Optional<Object> irecipe = Optional.empty();
                    if (!e.m_8020_(0).m_41619_()) {
                        irecipe = e.getRecipe(e.m_8020_(0));
                    }
                    boolean valid = e.canSmelt(irecipe.orElse(null));
                    if (!e.isBurning() && valid) {
                        if (itemstack.m_41720_() instanceof ItemHeater) {
                            int energy;
                            int z;
                            int y;
                            int x;
                            BlockEntity te;
                            if (itemstack.m_41782_() && (te = level.m_7702_(new BlockPos(x = itemstack.m_41783_().m_128451_("X"), y = itemstack.m_41783_().m_128451_("Y"), z = itemstack.m_41783_().m_128451_("Z")))) instanceof BlockWirelessEnergyHeaterTile && (energy = ((BlockWirelessEnergyHeaterTile)te).getEnergy()) >= 2000) {
                                if (!e.m_8020_(4).m_41619_() && e.m_8020_(4).m_41720_() instanceof ItemAugmentFuel) {
                                    e.furnaceBurnTime = 400 * e.getCookTime() / 200;
                                } else if (!e.m_8020_(4).m_41619_() && e.m_8020_(4).m_41720_() instanceof ItemAugmentSpeed) {
                                    if (energy >= 4000) {
                                        e.furnaceBurnTime = 100 * e.getCookTime() / 200;
                                    }
                                } else {
                                    e.furnaceBurnTime = 200 * e.getCookTime() / 200;
                                }
                                if (e.furnaceBurnTime > 0) {
                                    ((BlockWirelessEnergyHeaterTile)te).removeEnergy(2000);
                                }
                                e.recipesUsed = e.furnaceBurnTime;
                            }
                        } else {
                            if (!e.m_8020_(4).m_41619_()) {
                                if (e.m_8020_(4).m_41720_() instanceof ItemAugmentFuel) {
                                    e.furnaceBurnTime = BlockIronFurnaceTileBase.getBurnTime(itemstack, e.recipeType) * e.getCookTime() / 200 * 2;
                                } else if (e.m_8020_(4).m_41720_() instanceof ItemAugmentSpeed) {
                                    e.furnaceBurnTime = BlockIronFurnaceTileBase.getBurnTime(itemstack, e.recipeType) * e.getCookTime() / 200 / 2;
                                }
                            } else {
                                e.furnaceBurnTime = BlockIronFurnaceTileBase.getBurnTime(itemstack, e.recipeType) * e.getCookTime() / 200;
                            }
                            e.recipesUsed = e.furnaceBurnTime;
                        }
                        if (e.isBurning()) {
                            flag1 = true;
                            if (!(itemstack.m_41720_() instanceof ItemHeater)) {
                                if (itemstack.hasCraftingRemainingItem()) {
                                    e.m_6836_(1, itemstack.getCraftingRemainingItem());
                                } else if (!itemstack.m_41619_()) {
                                    itemstack.m_41774_(1);
                                    if (itemstack.m_41619_()) {
                                        e.m_6836_(1, itemstack.getCraftingRemainingItem());
                                    }
                                }
                            }
                        }
                    }
                    if (e.isBurning() && valid) {
                        ++e.cookTime;
                        if (e.cookTime >= e.totalCookTime) {
                            e.cookTime = 0;
                            e.totalCookTime = e.getCookTime();
                            e.smelt(irecipe.orElse(null));
                            e.autoIO();
                            flag1 = true;
                        }
                    } else {
                        e.cookTime = 0;
                    }
                } else if (!e.isBurning() && e.cookTime > 0) {
                    e.cookTime = BlockIronFurnaceTileBase.clamp(e.cookTime - 2, 0, e.totalCookTime);
                }
                if (e.f_58857_.m_46467_() % 24L == 0L && e.cookTime <= 0) {
                    if (e.m_8020_(0).m_41619_()) {
                        e.autoIO();
                        flag1 = true;
                    } else if (e.m_8020_(0).m_41613_() < e.m_8020_(0).m_41741_()) {
                        e.autoIO();
                        flag1 = true;
                    }
                    if (e.m_8020_(1).m_41619_()) {
                        e.autoIO();
                        flag1 = true;
                    } else if (e.m_8020_(1).m_41613_() < e.m_8020_(1).m_41741_()) {
                        e.autoIO();
                        flag1 = true;
                    }
                    if (!e.m_8020_(2).m_41619_() && e.m_8020_(2).m_41613_() >= 64) {
                        e.autoIO();
                    }
                }
            }
            if (wasBurning != e.isBurning()) {
                level.m_7731_(worldPosition, (BlockState)level.m_8055_(e.f_58858_).m_61124_((Property)BlockStateProperties.f_61443_, (Comparable)Boolean.valueOf(e.isBurning())), 3);
            }
            if (e.f_58857_.m_46467_() % 24L == 0L) {
                BlockState state = level.m_8055_(worldPosition);
                if (((Integer)state.m_61143_((Property)BlockIronFurnaceBase.TYPE)).intValue() != e.getStateType()) {
                    level.m_7731_(worldPosition, (BlockState)state.m_61124_((Property)BlockIronFurnaceBase.TYPE, (Comparable)Integer.valueOf(e.getStateType())), 3);
                }
                if ((Integer)state.m_61143_((Property)BlockIronFurnaceBase.JOVIAL) != e.jovial) {
                    level.m_7731_(worldPosition, (BlockState)state.m_61124_((Property)BlockIronFurnaceBase.JOVIAL, (Comparable)Integer.valueOf(e.jovial)), 3);
                }
            }
            if (flag1) {
                e.m_6596_();
            }
        }
    }

    public static int clamp(int p_76125_0_, int p_76125_1_, int p_76125_2_) {
        if (p_76125_0_ < p_76125_1_) {
            return p_76125_1_;
        }
        return p_76125_0_ > p_76125_2_ ? p_76125_2_ : p_76125_0_;
    }

    protected int getCapacityFromTier() {
        return switch (this.getTier()) {
            case 1 -> (Integer)Config.furnaceEnergyCapacityTier1.get();
            case 2 -> (Integer)Config.furnaceEnergyCapacityTier2.get();
            default -> (Integer)Config.furnaceEnergyCapacityTier0.get();
        };
    }

    protected void rainbowEnergyOut() {
        HashMap tiles = Maps.newHashMap();
        for (Direction dir : Direction.values()) {
            IEnergyStorage other;
            BlockEntity tile = this.f_58857_.m_7702_(this.f_58858_.m_121955_(dir.m_122436_()));
            if (tile == null || this.furnaceSettings.get(dir.ordinal()) != 2 && this.furnaceSettings.get(dir.ordinal()) != 3 || (other = (IEnergyStorage)tile.getCapability(ForgeCapabilities.ENERGY, dir.m_122424_()).map(other1 -> other1).orElse(null)) == null || !other.canReceive() || other.getEnergyStored() >= other.getMaxEnergyStored()) continue;
            tiles.put(tile, dir.m_122424_());
        }
        for (Map.Entry entry : tiles.entrySet()) {
            int energy = (Integer)Config.millionFurnacePowerToGenerate.get() / tiles.size();
            ((BlockEntity)entry.getKey()).getCapability(ForgeCapabilities.ENERGY, (Direction)entry.getValue()).ifPresent(h -> h.receiveEnergy(energy, false));
        }
    }

    protected void energyOut() {
        HashMap tiles = Maps.newHashMap();
        for (Direction dir : Direction.values()) {
            IEnergyStorage other;
            BlockEntity tile = this.f_58857_.m_7702_(this.f_58858_.m_121955_(dir.m_122436_()));
            if (tile == null || this.furnaceSettings.get(dir.ordinal()) != 2 && this.furnaceSettings.get(dir.ordinal()) != 3 || (other = (IEnergyStorage)tile.getCapability(ForgeCapabilities.ENERGY, dir.m_122424_()).map(other1 -> other1).orElse(null)) == null || !other.canReceive() || other.getEnergyStored() >= other.getMaxEnergyStored()) continue;
            tiles.put(tile, dir.m_122424_());
        }
        for (Map.Entry entry : tiles.entrySet()) {
            int energy = Math.min(this.getCapability(ForgeCapabilities.ENERGY).map(h -> ((FEnergyStorage)((Object)((Object)h))).getMaxExtract()).orElse(0), this.getEnergy()) / tiles.size();
            ((BlockEntity)entry.getKey()).getCapability(ForgeCapabilities.ENERGY, (Direction)entry.getValue()).ifPresent(h -> this.removeEnergy(h.receiveEnergy(energy, false)));
        }
    }

    protected void autoIO() {
        for (Direction dir : Direction.values()) {
            boolean check;
            ItemStack stack;
            int i;
            IItemHandler other;
            BlockEntity tile = this.f_58857_.m_7702_(this.f_58858_.m_121955_(dir.m_122436_()));
            if (tile == null || this.furnaceSettings.get(dir.ordinal()) != 1 && this.furnaceSettings.get(dir.ordinal()) != 2 && this.furnaceSettings.get(dir.ordinal()) != 3 && this.furnaceSettings.get(dir.ordinal()) != 4 || tile == null || (other = (IItemHandler)tile.getCapability(ForgeCapabilities.ITEM_HANDLER, dir.m_122424_()).map(other1 -> other1).orElse(null)) == null || other == null || this.getAutoInput() == 0 && this.getAutoOutput() == 0) continue;
            if (this.getAutoInput() == 1) {
                if (this.furnaceSettings.get(dir.ordinal()) == 1 || this.furnaceSettings.get(dir.ordinal()) == 3) {
                    if (this.m_8020_(0).m_41613_() >= this.m_8020_(0).m_41741_()) continue;
                    for (i = 0; i < other.getSlots(); ++i) {
                        if (other.getStackInSlot(i).m_41619_() || (!this.hasRecipe(stack = other.extractItem(i, other.getStackInSlot(i).m_41741_(), true)) || !this.m_8020_(0).m_41619_()) && !ItemHandlerHelper.canItemStacksStack((ItemStack)this.m_8020_(0), (ItemStack)stack)) continue;
                        this.insertItemInternal(0, other.extractItem(i, other.getStackInSlot(i).m_41741_() - this.m_8020_(0).m_41613_(), false), false);
                    }
                }
                if (this.furnaceSettings.get(dir.ordinal()) == 4) {
                    if (this.m_8020_(1).m_41613_() >= this.m_8020_(1).m_41741_()) continue;
                    for (i = 0; i < other.getSlots(); ++i) {
                        if (other.getStackInSlot(i).m_41619_() || !BlockIronFurnaceTileBase.isItemFuel(other.getStackInSlot(i), this.recipeType) || (!BlockIronFurnaceTileBase.isItemFuel(stack = other.extractItem(i, other.getStackInSlot(i).m_41741_(), true), this.recipeType) || !this.m_8020_(1).m_41619_()) && !ItemHandlerHelper.canItemStacksStack((ItemStack)this.m_8020_(1), (ItemStack)stack)) continue;
                        this.insertItemInternal(1, other.extractItem(i, other.getStackInSlot(i).m_41741_() - this.m_8020_(1).m_41613_(), false), false);
                    }
                }
            }
            if (this.getAutoOutput() != 1) continue;
            if (this.furnaceSettings.get(dir.ordinal()) == 4) {
                if (this.m_8020_(1).m_41619_() || BlockIronFurnaceTileBase.isItemFuel(this.m_8020_(1), this.recipeType)) continue;
                for (i = 0; i < other.getSlots(); ++i) {
                    stack = this.extractItemInternal(1, other.getSlotLimit(i) - other.getStackInSlot(i).m_41613_(), true);
                    if (!other.isItemValid(i, stack) || !other.getStackInSlot(i).m_41619_() && (!ItemHandlerHelper.canItemStacksStack((ItemStack)other.getStackInSlot(i), (ItemStack)stack) || other.getStackInSlot(i).m_41613_() + stack.m_41613_() > other.getSlotLimit(i)) || !(check = other.insertItem(i, this.extractItemInternal(1, stack.m_41613_(), true), true).m_41619_())) continue;
                    other.insertItem(i, this.extractItemInternal(1, stack.m_41613_(), false), false);
                }
            }
            if (this.furnaceSettings.get(dir.ordinal()) != 2 && this.furnaceSettings.get(dir.ordinal()) != 3 || this.m_8020_(2).m_41619_()) continue;
            for (i = 0; i < other.getSlots(); ++i) {
                stack = this.extractItemInternal(2, other.getSlotLimit(i) - other.getStackInSlot(i).m_41613_(), true);
                if (!other.isItemValid(i, stack) || !other.getStackInSlot(i).m_41619_() && (!ItemHandlerHelper.canItemStacksStack((ItemStack)other.getStackInSlot(i), (ItemStack)stack) || other.getStackInSlot(i).m_41613_() + stack.m_41613_() > other.getSlotLimit(i)) || !(check = other.insertItem(i, this.extractItemInternal(2, stack.m_41613_(), true), true).m_41619_())) continue;
                other.insertItem(i, this.extractItemInternal(2, stack.m_41613_(), false), false);
            }
        }
    }

    protected void autoIOGenerator() {
        for (Direction dir : Direction.values()) {
            ItemStack stack;
            int i;
            IItemHandler other;
            BlockEntity tile = this.f_58857_.m_7702_(this.f_58858_.m_121955_(dir.m_122436_()));
            if (tile == null || this.furnaceSettings.get(dir.ordinal()) != 4 || tile == null || (other = (IItemHandler)tile.getCapability(ForgeCapabilities.ITEM_HANDLER, dir.m_122424_()).map(other1 -> other1).orElse(null)) == null || other == null) continue;
            if (this.getAutoInput() != 0 && this.furnaceSettings.get(dir.ordinal()) == 4) {
                if (this.m_8020_(6).m_41613_() >= this.m_8020_(6).m_41741_()) continue;
                for (i = 0; i < other.getSlots(); ++i) {
                    if (other.getStackInSlot(i).m_41619_() || other.getStackInSlot(i).m_41720_() == Items.f_42446_ || (stack = other.extractItem(i, other.getStackInSlot(i).m_41741_(), true)).m_41720_() instanceof ItemHeater || (!BlockIronFurnaceTileBase.isItemFuel(stack, this.recipeType) || !this.m_8020_(6).m_41619_()) && !ItemHandlerHelper.canItemStacksStack((ItemStack)this.m_8020_(6), (ItemStack)stack)) continue;
                    this.insertItemInternal(6, other.extractItem(i, other.getStackInSlot(i).m_41741_() - this.m_8020_(6).m_41613_(), false), false);
                }
            }
            if (this.getAutoOutput() == 0 || this.furnaceSettings.get(dir.ordinal()) != 4 || this.m_8020_(6).m_41619_() || BlockIronFurnaceTileBase.isItemFuel(this.m_8020_(6), this.recipeType)) continue;
            for (i = 0; i < other.getSlots(); ++i) {
                boolean check;
                stack = this.extractItemInternal(6, this.m_8020_(6).m_41741_() - other.getStackInSlot(i).m_41613_(), true);
                if (!other.isItemValid(i, stack) || !other.getStackInSlot(i).m_41619_() && (!ItemHandlerHelper.canItemStacksStack((ItemStack)other.getStackInSlot(i), (ItemStack)stack) || other.getStackInSlot(i).m_41613_() + stack.m_41613_() > other.getSlotLimit(i)) || !(check = other.insertItem(i, this.extractItemInternal(6, stack.m_41613_(), true), true).m_41619_())) continue;
                other.insertItem(i, this.extractItemInternal(6, stack.m_41613_(), false), false);
            }
        }
    }

    protected void autoFactoryIO() {
        for (Direction dir : Direction.values()) {
            ItemStack stack;
            int i;
            int j;
            int size;
            int start;
            IItemHandler other;
            BlockEntity tile = this.f_58857_.m_7702_(this.f_58858_.m_121955_(dir.m_122436_()));
            if (tile == null || this.furnaceSettings.get(dir.ordinal()) != 1 && this.furnaceSettings.get(dir.ordinal()) != 2 && this.furnaceSettings.get(dir.ordinal()) != 3 || tile == null || (other = (IItemHandler)tile.getCapability(ForgeCapabilities.ITEM_HANDLER, dir.m_122424_()).map(other1 -> other1).orElse(null)) == null || other == null || this.getAutoInput() == 0 && this.getAutoOutput() == 0) continue;
            if (this.getAutoInput() == 1 && (this.furnaceSettings.get(dir.ordinal()) == 1 || this.furnaceSettings.get(dir.ordinal()) == 3)) {
                int n = this.getTier() == 0 ? 2 : (start = this.getTier() == 1 ? 1 : 0);
                size = this.getTier() == 0 ? 4 : (this.getTier() == 1 ? 5 : 6);
                for (j = start; j < size; ++j) {
                    if (this.m_8020_(FACTORY_INPUT[j]).m_41613_() >= this.m_8020_(FACTORY_INPUT[j]).m_41741_()) continue;
                    for (i = 0; i < other.getSlots(); ++i) {
                        if (other.getStackInSlot(i).m_41619_() || (!this.hasRecipe(stack = other.extractItem(i, other.getStackInSlot(i).m_41741_(), true)) || !this.m_8020_(FACTORY_INPUT[j]).m_41619_()) && !BlockIronFurnaceTileBase.canItemStacksStack(this.m_8020_(FACTORY_INPUT[j]), stack)) continue;
                        this.insertItemInternal(FACTORY_INPUT[j], other.extractItem(i, other.getStackInSlot(i).m_41741_() - this.m_8020_(FACTORY_INPUT[j]).m_41613_(), false), false);
                    }
                }
            }
            if (this.getAutoOutput() != 1 || this.furnaceSettings.get(dir.ordinal()) != 2 && this.furnaceSettings.get(dir.ordinal()) != 3) continue;
            int n = this.getTier() == 0 ? 2 : (start = this.getTier() == 1 ? 1 : 0);
            size = this.getTier() == 0 ? 4 : (this.getTier() == 1 ? 5 : 6);
            for (j = start; j < size; ++j) {
                if (this.m_8020_(FACTORY_INPUT[j] + 6).m_41619_()) continue;
                for (i = 0; i < other.getSlots(); ++i) {
                    boolean check;
                    stack = this.extractItemInternal(FACTORY_INPUT[j] + 6, other.getSlotLimit(i) - other.getStackInSlot(i).m_41613_(), true);
                    if (!other.isItemValid(i, stack) || !other.getStackInSlot(i).m_41619_() && (!BlockIronFurnaceTileBase.canItemStacksStack(other.getStackInSlot(i), stack) || other.getStackInSlot(i).m_41613_() + stack.m_41613_() > other.getSlotLimit(i)) || !(check = other.insertItem(i, this.extractItemInternal(FACTORY_INPUT[j] + 6, stack.m_41613_(), true), true).m_41619_())) continue;
                    other.insertItem(i, this.extractItemInternal(FACTORY_INPUT[j] + 6, stack.m_41613_(), false), false);
                }
            }
        }
    }

    public static boolean canItemStacksStack(@NotNull ItemStack a, @NotNull ItemStack b) {
        return ItemHandlerHelper.canItemStacksStack((ItemStack)a, (ItemStack)b);
    }

    @Nonnull
    public ItemStack insertItemInternal(int slot, @Nonnull ItemStack stack, boolean simulate) {
        boolean reachedLimit;
        if (stack.m_41619_()) {
            return ItemStack.f_41583_;
        }
        if (!this.m_7155_(slot, stack, null)) {
            return stack;
        }
        ItemStack existing = this.m_8020_(slot);
        int limit = stack.m_41741_();
        if (!existing.m_41619_()) {
            if (!ItemHandlerHelper.canItemStacksStack((ItemStack)stack, (ItemStack)existing)) {
                return stack;
            }
            limit -= existing.m_41613_();
        }
        if (limit <= 0) {
            return stack;
        }
        boolean bl = reachedLimit = stack.m_41613_() > limit;
        if (!simulate) {
            if (existing.m_41619_()) {
                this.m_6836_(slot, reachedLimit ? ItemHandlerHelper.copyStackWithSize((ItemStack)stack, (int)limit) : stack);
            } else {
                existing.m_41769_(reachedLimit ? limit : stack.m_41613_());
            }
            this.m_6596_();
        }
        return reachedLimit ? ItemHandlerHelper.copyStackWithSize((ItemStack)stack, (int)(stack.m_41613_() - limit)) : ItemStack.f_41583_;
    }

    @Nonnull
    private ItemStack extractItemInternal(int slot, int amount, boolean simulate) {
        if (amount == 0) {
            return ItemStack.f_41583_;
        }
        ItemStack existing = this.m_8020_(slot);
        if (existing.m_41619_()) {
            return ItemStack.f_41583_;
        }
        int toExtract = Math.min(amount, existing.m_41741_());
        if (existing.m_41613_() <= toExtract) {
            if (!simulate) {
                this.m_6836_(slot, ItemStack.f_41583_);
                this.m_6596_();
                return existing;
            }
            return existing.m_41777_();
        }
        if (!simulate) {
            this.m_6836_(slot, ItemHandlerHelper.copyStackWithSize((ItemStack)existing, (int)(existing.m_41613_() - toExtract)));
            this.m_6596_();
        }
        return ItemHandlerHelper.copyStackWithSize((ItemStack)existing, (int)toExtract);
    }

    public boolean isAutoSplit() {
        return this.furnaceSettings.autoSplit == 1;
    }

    public int getSettingBottom() {
        return this.furnaceSettings.get(0);
    }

    public int getSettingTop() {
        return this.furnaceSettings.get(1);
    }

    public int getSettingFront() {
        int i = DirectionUtil.getId((Direction)this.m_58900_().m_61143_((Property)BlockStateProperties.f_61374_));
        return this.furnaceSettings.get(i);
    }

    public int getSettingBack() {
        int i = DirectionUtil.getId(((Direction)this.m_58900_().m_61143_((Property)BlockStateProperties.f_61374_)).m_122424_());
        return this.furnaceSettings.get(i);
    }

    public int getSettingLeft() {
        Direction facing = (Direction)this.m_58900_().m_61143_((Property)BlockStateProperties.f_61374_);
        if (facing == Direction.NORTH) {
            return this.furnaceSettings.get(DirectionUtil.getId(Direction.EAST));
        }
        if (facing == Direction.WEST) {
            return this.furnaceSettings.get(DirectionUtil.getId(Direction.NORTH));
        }
        if (facing == Direction.SOUTH) {
            return this.furnaceSettings.get(DirectionUtil.getId(Direction.WEST));
        }
        return this.furnaceSettings.get(DirectionUtil.getId(Direction.SOUTH));
    }

    public int getSettingRight() {
        Direction facing = (Direction)this.m_58900_().m_61143_((Property)BlockStateProperties.f_61374_);
        if (facing == Direction.NORTH) {
            return this.furnaceSettings.get(DirectionUtil.getId(Direction.WEST));
        }
        if (facing == Direction.WEST) {
            return this.furnaceSettings.get(DirectionUtil.getId(Direction.SOUTH));
        }
        if (facing == Direction.SOUTH) {
            return this.furnaceSettings.get(DirectionUtil.getId(Direction.EAST));
        }
        return this.furnaceSettings.get(DirectionUtil.getId(Direction.NORTH));
    }

    public int getIndexFront() {
        int i = ((Direction)this.m_58900_().m_61143_((Property)BlockStateProperties.f_61374_)).ordinal();
        return i;
    }

    public int getIndexBack() {
        int i = ((Direction)this.m_58900_().m_61143_((Property)BlockStateProperties.f_61374_)).m_122424_().ordinal();
        return i;
    }

    public int getIndexLeft() {
        Direction facing = (Direction)this.m_58900_().m_61143_((Property)BlockStateProperties.f_61374_);
        if (facing == Direction.NORTH) {
            return Direction.EAST.ordinal();
        }
        if (facing == Direction.WEST) {
            return Direction.NORTH.ordinal();
        }
        if (facing == Direction.SOUTH) {
            return Direction.WEST.ordinal();
        }
        return Direction.SOUTH.ordinal();
    }

    public int getIndexRight() {
        Direction facing = (Direction)this.m_58900_().m_61143_((Property)BlockStateProperties.f_61374_);
        if (facing == Direction.NORTH) {
            return Direction.WEST.ordinal();
        }
        if (facing == Direction.WEST) {
            return Direction.SOUTH.ordinal();
        }
        if (facing == Direction.SOUTH) {
            return Direction.EAST.ordinal();
        }
        return Direction.NORTH.ordinal();
    }

    public int getAutoInput() {
        return this.furnaceSettings.get(6);
    }

    public int getAugmentGUI() {
        return this.furnaceSettings.get(10);
    }

    public int getAutoOutput() {
        return this.furnaceSettings.get(7);
    }

    public int getRedstoneSetting() {
        return this.furnaceSettings.get(8);
    }

    public int getRedstoneComSub() {
        return this.furnaceSettings.get(9);
    }

    protected int getStateType() {
        if (this.m_8020_(3).m_41720_() == Registration.SMOKING_AUGMENT.get()) {
            return 1;
        }
        if (this.m_8020_(3).m_41720_() == Registration.BLASTING_AUGMENT.get()) {
            return 2;
        }
        return 0;
    }

    public boolean isBurning() {
        return this.furnaceBurnTime > 0;
    }

    public boolean isRainbowFurnace() {
        return this instanceof BlockMillionFurnaceTile;
    }

    protected void smelt(@Nullable Recipe<?> recipe) {
        if (this instanceof BlockMillionFurnaceTile) {
            this.smeltItemMult(recipe, 64);
        } else if (this instanceof BlockAllthemodiumFurnaceTile) {
            this.smeltItemMult(recipe, (Integer)Config.allthemodiumFurnaceSmeltMult.get());
        } else if (this instanceof BlockVibraniumFurnaceTile) {
            this.smeltItemMult(recipe, (Integer)Config.vibraniumFurnaceSmeltMult.get());
        } else if (this instanceof BlockUnobtainiumFurnaceTile) {
            this.smeltItemMult(recipe, (Integer)Config.unobtainiumFurnaceSmeltMult.get());
        } else {
            this.smeltItem(recipe);
        }
    }

    protected void factorySmelt(@Nullable Recipe<?> recipe, int slot) {
        if (this instanceof BlockMillionFurnaceTile) {
            this.smeltFactoryItemMult(recipe, slot, 64);
        } else if (this instanceof BlockAllthemodiumFurnaceTile) {
            this.smeltFactoryItemMult(recipe, slot, (Integer)Config.allthemodiumFurnaceSmeltMult.get());
        } else if (this instanceof BlockVibraniumFurnaceTile) {
            this.smeltFactoryItemMult(recipe, slot, (Integer)Config.vibraniumFurnaceSmeltMult.get());
        } else if (this instanceof BlockUnobtainiumFurnaceTile) {
            this.smeltFactoryItemMult(recipe, slot, (Integer)Config.unobtainiumFurnaceSmeltMult.get());
        } else {
            this.smeltFactoryItem(recipe, slot);
        }
    }

    protected boolean canSmelt(@Nullable Recipe<?> recipe) {
        ItemStack recipeOutput;
        if (!this.m_8020_(0).m_41619_() && recipe != null && !(recipeOutput = recipe.m_8043_((RegistryAccess)RegistryAccess.f_243945_)).m_41619_()) {
            ItemStack output = this.m_8020_(2);
            if (output.m_41619_()) {
                return true;
            }
            if (!ItemStack.m_150942_((ItemStack)output, (ItemStack)recipeOutput)) {
                return false;
            }
            return output.m_41613_() + recipeOutput.m_41613_() <= Math.min(output.m_41741_(), 64);
        }
        return false;
    }

    protected void smeltItem(@Nullable Recipe<?> recipe) {
        if (recipe != null && this.canSmelt(recipe)) {
            ItemStack itemstack = this.m_8020_(0);
            ItemStack itemstack1 = recipe.m_8043_((RegistryAccess)RegistryAccess.f_243945_);
            ItemStack itemstack2 = this.m_8020_(2);
            if (itemstack2.m_41619_()) {
                this.m_6836_(2, itemstack1.m_41777_());
            } else if (itemstack2.m_41720_() == itemstack1.m_41720_()) {
                itemstack2.m_41769_(itemstack1.m_41613_());
            }
            if (!this.f_58857_.f_46443_) {
                this.m_6029_(recipe);
            }
            if (itemstack.m_41720_() == Blocks.f_50057_.m_5456_() && !this.m_8020_(1).m_41619_() && this.m_8020_(1).m_41720_() == Items.f_42446_) {
                this.m_6836_(1, new ItemStack((ItemLike)Items.f_42447_));
            }
            if (ModList.get().isLoaded("pmmo")) {
                this.handleSmeltedPMMO(itemstack, this.f_58857_, this.f_58858_);
            }
            itemstack.m_41774_(1);
        }
    }

    protected boolean canFactorySmelt(@Nullable Recipe<?> recipe, int slot) {
        ItemStack recipeOutput;
        int outputSlot = slot + 6;
        if (!this.m_8020_(slot).m_41619_() && recipe != null && !(recipeOutput = recipe.m_8043_((RegistryAccess)RegistryAccess.f_243945_)).m_41619_()) {
            ItemStack output = this.m_8020_(outputSlot);
            if (output.m_41619_()) {
                return true;
            }
            if (!ItemStack.m_150942_((ItemStack)output, (ItemStack)recipeOutput)) {
                return false;
            }
            return output.m_41613_() + recipeOutput.m_41613_() <= output.m_41741_();
        }
        return false;
    }

    protected void smeltFactoryItem(@Nullable Recipe<?> recipe, int slot) {
        int outputSlot = slot + 6;
        if (recipe != null && this.canFactorySmelt(recipe, slot)) {
            ItemStack itemstack = this.m_8020_(slot);
            ItemStack itemstack1 = recipe.m_8043_((RegistryAccess)RegistryAccess.f_243945_);
            ItemStack itemstack2 = this.m_8020_(outputSlot);
            if (itemstack2.m_41619_()) {
                this.m_6836_(outputSlot, itemstack1.m_41777_());
            } else if (itemstack2.m_41720_() == itemstack1.m_41720_()) {
                itemstack2.m_41769_(itemstack1.m_41613_());
            }
            if (!this.f_58857_.f_46443_) {
                this.m_6029_(recipe);
            }
            if (ModList.get().isLoaded("pmmo")) {
                this.handleSmeltedPMMO(itemstack, this.f_58857_, this.f_58858_);
            }
            itemstack.m_41774_(1);
        }
    }

    protected void smeltItemMult(@Nullable Recipe<?> recipe, int div) {
        if (recipe != null && this.canSmelt(recipe)) {
            ItemStack itemstack = this.m_8020_(0);
            ItemStack itemstack1 = recipe.m_8043_((RegistryAccess)RegistryAccess.f_243945_);
            ItemStack itemstack2 = this.m_8020_(2);
            int maxCanSmelt = (64 - itemstack2.m_41613_()) / itemstack1.m_41613_();
            int wantToSmeltCount = Math.min(Math.min(div, maxCanSmelt), itemstack.m_41613_());
            int whenSmelted = itemstack1.m_41613_() * wantToSmeltCount;
            int decrement = whenSmelted / itemstack1.m_41613_();
            if (itemstack2.m_41619_()) {
                this.m_6836_(2, new ItemStack((ItemLike)itemstack1.m_41777_().m_41720_(), whenSmelted));
            } else if (itemstack2.m_41720_() == itemstack1.m_41720_()) {
                itemstack2.m_41769_(whenSmelted);
            }
            if (!this.f_58857_.f_46443_) {
                for (int i = 0; i < decrement; ++i) {
                    this.m_6029_(recipe);
                }
            }
            if (itemstack.m_41720_() == Blocks.f_50057_.m_5456_() && !this.m_8020_(1).m_41619_() && this.m_8020_(1).m_41720_() == Items.f_42446_) {
                this.m_6836_(1, new ItemStack((ItemLike)Items.f_42447_));
            }
            if (ModList.get().isLoaded("pmmo")) {
                this.handleSmeltedPMMO(itemstack, this.f_58857_, this.f_58858_);
            }
            itemstack.m_41774_(decrement);
        }
    }

    protected void smeltFactoryItemMult(@Nullable Recipe<?> recipe, int slot, int div) {
        int outputSlot = slot + 6;
        if (recipe != null && this.canFactorySmelt(recipe, slot)) {
            ItemStack itemstack = this.m_8020_(slot);
            ItemStack itemstack1 = recipe.m_8043_((RegistryAccess)RegistryAccess.f_243945_);
            ItemStack itemstack2 = this.m_8020_(outputSlot);
            int maxCanSmelt = (64 - itemstack2.m_41613_()) / itemstack1.m_41613_();
            int wantToSmeltCount = Math.min(Math.min(div, maxCanSmelt), itemstack.m_41613_());
            int whenSmelted = itemstack1.m_41613_() * wantToSmeltCount;
            int decrement = whenSmelted / itemstack1.m_41613_();
            if (itemstack2.m_41619_()) {
                this.m_6836_(outputSlot, new ItemStack((ItemLike)itemstack1.m_41777_().m_41720_(), whenSmelted));
            } else if (itemstack2.m_41720_() == itemstack1.m_41720_()) {
                itemstack2.m_41769_(whenSmelted);
            }
            if (!this.f_58857_.f_46443_) {
                for (int i = 0; i < decrement; ++i) {
                    this.m_6029_(recipe);
                }
            }
            if (ModList.get().isLoaded("pmmo")) {
                this.handleSmeltedPMMO(itemstack, this.f_58857_, this.f_58858_);
            }
            itemstack.m_41774_(decrement);
        }
    }

    private void handleSmeltedPMMO(ItemStack stack, Level level, BlockPos pos) {
        FurnaceHandler.handle((FurnaceBurnEvent)new FurnaceBurnEvent(stack, level, pos));
    }

    @Override
    public void m_142466_(CompoundTag tag) {
        int[] tagArr;
        int i;
        if (tag.m_128423_("Owner") != null) {
            this.owner = tag.m_128342_("Owner");
        }
        tag.m_128471_("RainbowGen");
        for (i = 0; i < this.factoryCookTime.length; ++i) {
            tagArr = tag.m_128465_("FactoryCookTime");
            if (tagArr.length != this.factoryCookTime.length) continue;
            this.factoryCookTime[i] = tagArr[i];
        }
        for (i = 0; i < this.factoryTotalCookTime.length; ++i) {
            tagArr = tag.m_128465_("FactoryTotalCookTime");
            if (tagArr.length != this.factoryTotalCookTime.length) continue;
            this.factoryTotalCookTime[i] = tagArr[i];
        }
        for (i = 0; i < this.usedRF.length; ++i) {
            double tagRF;
            this.usedRF[i] = tagRF = tag.m_128459_("UsedRF" + i);
        }
        this.generatorBurn = tag.m_128459_("GeneratorBurn");
        this.generatorRecentRecipeRF = tag.m_128451_("GeneratorRecent");
        this.gottenRF = tag.m_128459_("GottenRF");
        this.furnaceBurnTime = tag.m_128451_("BurnTime");
        this.cookTime = tag.m_128451_("CookTime");
        this.totalCookTime = tag.m_128451_("CookTimeTotal");
        this.currentAugment = tag.m_128465_("Augment");
        this.jovial = tag.m_128451_("Jovial");
        this.recipesUsed = BlockIronFurnaceTileBase.getBurnTime(this.m_8020_(1), this.recipeType);
        CompoundTag compoundnbt = tag.m_128469_("RecipesUsed");
        for (String s : compoundnbt.m_128431_()) {
            this.recipes.put((Object)new ResourceLocation(s), compoundnbt.m_128451_(s));
        }
        this.furnaceSettings.read(tag);
        this.setEnergy(tag.m_128451_("Energy"));
        this.lastGameTickEnergyUpdated = 0L;
        super.m_142466_(tag);
    }

    @Override
    protected void m_183515_(CompoundTag tag) {
        super.m_183515_(tag);
        if (this.owner != null) {
            tag.m_128362_("Owner", this.owner);
        }
        tag.m_128379_("RainbowGen", this.rainbowGenerating);
        tag.m_128385_("FactoryCookTime", this.factoryCookTime);
        tag.m_128385_("FactoryTotalCookTime", this.factoryTotalCookTime);
        for (int i = 0; i < this.usedRF.length; ++i) {
            tag.m_128347_("UsedRF" + i, this.usedRF[i]);
        }
        tag.m_128347_("GeneratorBurn", this.generatorBurn);
        tag.m_128405_("GeneratorRecent", this.generatorRecentRecipeRF);
        tag.m_128347_("GottenRF", this.gottenRF);
        tag.m_128405_("BurnTime", this.furnaceBurnTime);
        tag.m_128405_("CookTime", this.cookTime);
        tag.m_128405_("CookTimeTotal", this.totalCookTime);
        tag.m_128385_("Augment", this.currentAugment);
        tag.m_128405_("Jovial", this.jovial);
        this.furnaceSettings.write(tag);
        tag.m_128405_("Energy", this.getEnergy());
        CompoundTag compoundnbt = new CompoundTag();
        this.recipes.forEach((recipeId, craftedAmount) -> compoundnbt.m_128405_(recipeId.toString(), craftedAmount.intValue()));
        tag.m_128365_("RecipesUsed", (Tag)compoundnbt);
    }

    public static int getBurnTime(ItemStack stack, RecipeType recipeType) {
        return ForgeHooks.getBurnTime((ItemStack)stack, (RecipeType)recipeType);
    }

    public static boolean isItemFuel(ItemStack stack, RecipeType recipeType) {
        return BlockIronFurnaceTileBase.getBurnTime(stack, recipeType) > 0 || stack.m_41720_() instanceof ItemHeater;
    }

    public static boolean isItemAugment(ItemStack stack, int type) {
        if (type == 0) {
            return stack.m_41720_() instanceof ItemAugmentRed;
        }
        if (type == 1) {
            return stack.m_41720_() instanceof ItemAugmentGreen;
        }
        if (type == 2) {
            return stack.m_41720_() instanceof ItemAugmentBlue;
        }
        return stack.m_41720_() instanceof ItemAugment;
    }

    @Nonnull
    public <T> LazyOptional<T> getCapability(Capability<T> capability, @Nullable Direction facing) {
        if (!this.m_58901_() && facing != null && capability == ForgeCapabilities.ITEM_HANDLER) {
            if (facing == Direction.DOWN) {
                return this.invHandlers[0].cast();
            }
            if (facing == Direction.UP) {
                return this.invHandlers[1].cast();
            }
            if (facing == Direction.NORTH) {
                return this.invHandlers[2].cast();
            }
            if (facing == Direction.SOUTH) {
                return this.invHandlers[3].cast();
            }
            if (facing == Direction.WEST) {
                return this.invHandlers[4].cast();
            }
            return this.invHandlers[5].cast();
        }
        if (!this.m_58901_() && capability == ForgeCapabilities.ENERGY && (this.isGenerator() || this.isFactory())) {
            return this.energy.cast();
        }
        return super.getCapability(capability, facing);
    }

    @Override
    public int[] IgetSlotsForFace(Direction side) {
        if (this.isFurnace()) {
            if (this.furnaceSettings.get(DirectionUtil.getId(side)) == 0) {
                return new int[0];
            }
            if (this.furnaceSettings.get(DirectionUtil.getId(side)) == 1) {
                return new int[]{0, 1};
            }
            if (this.furnaceSettings.get(DirectionUtil.getId(side)) == 2) {
                return new int[]{2};
            }
            if (this.furnaceSettings.get(DirectionUtil.getId(side)) == 3) {
                return new int[]{0, 1, 2};
            }
            if (this.furnaceSettings.get(DirectionUtil.getId(side)) == 4) {
                return new int[]{1};
            }
        } else if (this.isGenerator()) {
            if (this.furnaceSettings.get(DirectionUtil.getId(side)) == 4) {
                return new int[]{6};
            }
        } else if (this.isFactory()) {
            if (this.furnaceSettings.get(DirectionUtil.getId(side)) == 0) {
                return new int[0];
            }
            if (this.furnaceSettings.get(DirectionUtil.getId(side)) == 1) {
                return FACTORY_INPUT;
            }
            if (this.furnaceSettings.get(DirectionUtil.getId(side)) == 2) {
                return new int[]{13, 14, 15, 16, 17, 18};
            }
            if (this.furnaceSettings.get(DirectionUtil.getId(side)) == 3) {
                return new int[]{7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18};
            }
        }
        return new int[0];
    }

    @Override
    public boolean IcanExtractItem(int index, ItemStack stack, Direction direction) {
        if (this.isFurnace()) {
            if (this.furnaceSettings.get(DirectionUtil.getId(direction)) == 0) {
                return false;
            }
            if (this.furnaceSettings.get(DirectionUtil.getId(direction)) == 1) {
                return false;
            }
            if (this.furnaceSettings.get(DirectionUtil.getId(direction)) == 2) {
                return index == 2;
            }
            if (this.furnaceSettings.get(DirectionUtil.getId(direction)) == 3) {
                return index == 2;
            }
            if (this.furnaceSettings.get(DirectionUtil.getId(direction)) == 4 && stack.m_41720_() != Items.f_42446_) {
                return false;
            }
            if (this.furnaceSettings.get(DirectionUtil.getId(direction)) == 4 && stack.m_41720_() == Items.f_42446_) {
                return true;
            }
        } else if (this.isGenerator()) {
            if (this.furnaceSettings.get(DirectionUtil.getId(direction)) == 4 && stack.m_41720_() == Items.f_42446_) {
                return true;
            }
        } else if (this.isFactory()) {
            if (this.furnaceSettings.get(DirectionUtil.getId(direction)) == 2) {
                return index >= 13 && index <= 18;
            }
            if (this.furnaceSettings.get(DirectionUtil.getId(direction)) == 3) {
                return index >= 13 && index <= 18;
            }
        }
        return false;
    }

    @Override
    public boolean IisItemValidForSlot(int index, ItemStack stack) {
        if (this.isFurnace()) {
            if (index == 2 || index == 3 || index == 4 || index == 5) {
                return false;
            }
            if (index == 0) {
                if (stack.m_41619_()) {
                    return false;
                }
                return this.hasRecipe(stack);
            }
            if (index == 1) {
                ItemStack itemstack = this.m_8020_(1);
                return BlockIronFurnaceTileBase.getBurnTime(stack, this.recipeType) > 0 || stack.m_41720_() == Items.f_42446_ && itemstack.m_41720_() != Items.f_42446_ || stack.m_41720_() instanceof ItemHeater;
            }
        } else if (this.isGenerator()) {
            if (index == 6) {
                if (this.m_8020_(3).m_41720_() instanceof ItemAugmentSmoking && BlockIronFurnaceTileBase.getSmokingBurn(stack) > 0) {
                    return true;
                }
                if (this.m_8020_(3).m_41720_() instanceof ItemAugmentBlasting && this.hasGeneratorBlastingRecipe(stack)) {
                    return true;
                }
                if (this.m_8020_(3).m_41619_() && BlockIronFurnaceTileBase.getBurnTime(stack, this.recipeType) > 0) {
                    return true;
                }
                if (stack.m_41720_() instanceof ItemHeater) {
                    return false;
                }
            }
        } else if (this.isFactory()) {
            if (index >= 13 && index <= 18 || index == 3 || index == 4 || index == 5) {
                return false;
            }
            if (index >= 7 && index <= 12) {
                if (stack.m_41619_()) {
                    return false;
                }
                if (this.getTier() == 0) {
                    if (index >= 9 && index <= 10) {
                        return this.hasRecipe(stack);
                    }
                    return false;
                }
                if (this.getTier() == 1) {
                    if (index >= 8 && index <= 11) {
                        return this.hasRecipe(stack);
                    }
                    return false;
                }
                return this.hasRecipe(stack);
            }
        }
        return false;
    }

    public void setJovial(int value) {
        this.jovial = value;
    }

    public int getXpNeededForNextLevel(int experienceLevel) {
        if (experienceLevel >= 30) {
            return 112 + (experienceLevel - 30) * 9;
        }
        return experienceLevel >= 15 ? 37 + (experienceLevel - 15) * 5 : 7 + experienceLevel * 2;
    }

    public int getXpNeededForLevel(int level) {
        int xp = 0;
        for (int i = 0; i < level; ++i) {
            xp += this.getXpNeededForNextLevel(i);
        }
        return xp + 1;
    }

    public void m_6029_(@Nullable Recipe<?> recipe) {
        if (recipe != null) {
            ResourceLocation resourcelocation = recipe.m_6423_();
            float xpRecipe = ((AbstractCookingRecipe)recipe).m_43750_();
            if ((float)(this.recipes.getInt((Object)resourcelocation) + 1) * xpRecipe <= (float)(this.getXpNeededForLevel((Integer)Config.recipeMaxXPLevel.get()) + 1)) {
                this.recipes.addTo((Object)resourcelocation, 1);
            }
        }
    }

    @Nullable
    public Recipe<?> m_7928_() {
        return null;
    }

    public void unlockRecipes(ServerPlayer player) {
        List<Recipe<?>> list = this.grantStoredRecipeExperience(player.m_284548_(), player.m_20182_());
        player.m_7281_(list);
        this.recipes.clear();
    }

    public List<Recipe<?>> grantStoredRecipeExperience(ServerLevel level, Vec3 worldPosition) {
        ArrayList list = Lists.newArrayList();
        for (Object2IntMap.Entry entry : this.recipes.object2IntEntrySet()) {
            level.m_7465_().m_44043_((ResourceLocation)entry.getKey()).ifPresent(h -> {
                list.add(h);
                BlockIronFurnaceTileBase.splitAndSpawnExperience(level, worldPosition, entry.getIntValue(), ((AbstractCookingRecipe)h).m_43750_());
            });
        }
        return list;
    }

    private static void splitAndSpawnExperience(ServerLevel level, Vec3 worldPosition, int craftedAmount, float experience) {
        int i = Mth.m_14143_((float)((float)craftedAmount * experience));
        float f = Mth.m_14187_((float)((float)craftedAmount * experience));
        if (f != 0.0f && Math.random() < (double)f) {
            ++i;
        }
        ExperienceOrb.m_147082_((ServerLevel)level, (Vec3)worldPosition, (int)i);
    }

    public void m_5809_(StackedContents helper) {
        for (ItemStack itemstack : this.inventory) {
            helper.m_36491_(itemstack);
        }
    }

    protected boolean doesNeedUpdateSend() {
        return !Arrays.equals(this.provides, this.lastProvides);
    }

    public void onUpdateSent() {
        System.arraycopy(this.provides, 0, this.lastProvides, 0, this.provides.length);
        this.f_58857_.m_46672_(this.f_58858_, this.m_58900_().m_60734_());
    }

    public void placeConfig() {
        if (this.furnaceSettings != null) {
            this.furnaceSettings.set(0, 2);
            this.furnaceSettings.set(1, 1);
            for (Direction dir : Direction.values()) {
                if (dir == Direction.DOWN || dir == Direction.UP) continue;
                this.furnaceSettings.set(dir.ordinal(), 4);
            }
            this.f_58857_.markAndNotifyBlock(this.f_58858_, this.f_58857_.m_46745_(this.f_58858_), this.f_58857_.m_8055_(this.f_58858_).m_60734_().m_49966_(), this.f_58857_.m_8055_(this.f_58858_), 3, 3);
        }
    }

    public boolean isGenerator() {
        return this.currentAugment[2] == 2;
    }

    public boolean isFactory() {
        return this.currentAugment[2] == 1;
    }

    public boolean isFurnace() {
        return this.currentAugment[2] == 0;
    }

    public void m_7651_() {
        this.energy.invalidate();
        super.m_7651_();
    }

    public int getTier() {
        return 0;
    }
}

