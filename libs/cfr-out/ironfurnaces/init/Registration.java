/*
 * Decompiled with CFR.
 */
package ironfurnaces.init;

import ironfurnaces.Config;
import ironfurnaces.blocks.BlockWirelessEnergyHeater;
import ironfurnaces.blocks.furnaces.BlockCopperFurnace;
import ironfurnaces.blocks.furnaces.BlockCrystalFurnace;
import ironfurnaces.blocks.furnaces.BlockDiamondFurnace;
import ironfurnaces.blocks.furnaces.BlockEmeraldFurnace;
import ironfurnaces.blocks.furnaces.BlockGoldFurnace;
import ironfurnaces.blocks.furnaces.BlockIronFurnace;
import ironfurnaces.blocks.furnaces.BlockItemHeater;
import ironfurnaces.blocks.furnaces.BlockMillionFurnace;
import ironfurnaces.blocks.furnaces.BlockNetheriteFurnace;
import ironfurnaces.blocks.furnaces.BlockObsidianFurnace;
import ironfurnaces.blocks.furnaces.BlockSilverFurnace;
import ironfurnaces.blocks.furnaces.other.BlockAllthemodiumFurnace;
import ironfurnaces.blocks.furnaces.other.BlockUnobtainiumFurnace;
import ironfurnaces.blocks.furnaces.other.BlockVibraniumFurnace;
import ironfurnaces.container.BlockWirelessEnergyHeaterContainer;
import ironfurnaces.container.furnaces.BlockCopperFurnaceContainer;
import ironfurnaces.container.furnaces.BlockCrystalFurnaceContainer;
import ironfurnaces.container.furnaces.BlockDiamondFurnaceContainer;
import ironfurnaces.container.furnaces.BlockEmeraldFurnaceContainer;
import ironfurnaces.container.furnaces.BlockGoldFurnaceContainer;
import ironfurnaces.container.furnaces.BlockIronFurnaceContainer;
import ironfurnaces.container.furnaces.BlockMillionFurnaceContainer;
import ironfurnaces.container.furnaces.BlockNetheriteFurnaceContainer;
import ironfurnaces.container.furnaces.BlockObsidianFurnaceContainer;
import ironfurnaces.container.furnaces.BlockSilverFurnaceContainer;
import ironfurnaces.container.furnaces.other.BlockAllthemodiumFurnaceContainer;
import ironfurnaces.container.furnaces.other.BlockUnobtainiumFurnaceContainer;
import ironfurnaces.container.furnaces.other.BlockVibraniumFurnaceContainer;
import ironfurnaces.items.ItemFurnace;
import ironfurnaces.items.ItemFurnaceCopy;
import ironfurnaces.items.ItemHeater;
import ironfurnaces.items.ItemMillionFurnace;
import ironfurnaces.items.ItemRainbowCoal;
import ironfurnaces.items.ItemSpooky;
import ironfurnaces.items.ItemXmas;
import ironfurnaces.items.augments.ItemAugmentBlasting;
import ironfurnaces.items.augments.ItemAugmentFactory;
import ironfurnaces.items.augments.ItemAugmentFuel;
import ironfurnaces.items.augments.ItemAugmentGenerator;
import ironfurnaces.items.augments.ItemAugmentSmoking;
import ironfurnaces.items.augments.ItemAugmentSpeed;
import ironfurnaces.items.upgrades.ItemUpgradeAllthemodium;
import ironfurnaces.items.upgrades.ItemUpgradeCopper;
import ironfurnaces.items.upgrades.ItemUpgradeCrystal;
import ironfurnaces.items.upgrades.ItemUpgradeDiamond;
import ironfurnaces.items.upgrades.ItemUpgradeEmerald;
import ironfurnaces.items.upgrades.ItemUpgradeGold;
import ironfurnaces.items.upgrades.ItemUpgradeGold2;
import ironfurnaces.items.upgrades.ItemUpgradeIron;
import ironfurnaces.items.upgrades.ItemUpgradeIron2;
import ironfurnaces.items.upgrades.ItemUpgradeNetherite;
import ironfurnaces.items.upgrades.ItemUpgradeObsidian;
import ironfurnaces.items.upgrades.ItemUpgradeObsidian2;
import ironfurnaces.items.upgrades.ItemUpgradeSilver;
import ironfurnaces.items.upgrades.ItemUpgradeSilver2;
import ironfurnaces.items.upgrades.ItemUpgradeUnobtainium;
import ironfurnaces.items.upgrades.ItemUpgradeVibranium;
import ironfurnaces.recipes.GeneratorRecipe;
import ironfurnaces.recipes.SimpleGeneratorRecipe;
import ironfurnaces.tileentity.BlockWirelessEnergyHeaterTile;
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
import mezz.jei.api.recipe.RecipeType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.extensions.IForgeMenuType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.IForgeRegistry;
import net.minecraftforge.registries.RegistryObject;

@Mod.EventBusSubscriber(bus=Mod.EventBusSubscriber.Bus.MOD)
public class Registration {
    private static final DeferredRegister<Block> BLOCKS = DeferredRegister.create((IForgeRegistry)ForgeRegistries.BLOCKS, (String)"ironfurnaces");
    private static final DeferredRegister<Item> ITEMS = DeferredRegister.create((IForgeRegistry)ForgeRegistries.ITEMS, (String)"ironfurnaces");
    private static final DeferredRegister<BlockEntityType<?>> TILES = DeferredRegister.create((IForgeRegistry)ForgeRegistries.BLOCK_ENTITY_TYPES, (String)"ironfurnaces");
    private static final DeferredRegister<MenuType<?>> CONTAINERS = DeferredRegister.create((IForgeRegistry)ForgeRegistries.MENU_TYPES, (String)"ironfurnaces");
    public static final DeferredRegister<RecipeSerializer<?>> RECIPE_SERIALIZERS = DeferredRegister.create((IForgeRegistry)ForgeRegistries.RECIPE_SERIALIZERS, (String)"ironfurnaces");
    public static final DeferredRegister<net.minecraft.world.item.crafting.RecipeType<?>> RECIPE_TYPES = DeferredRegister.create((IForgeRegistry)ForgeRegistries.RECIPE_TYPES, (String)"ironfurnaces");
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS = DeferredRegister.create((ResourceKey)Registries.f_279569_, (String)"ironfurnaces");
    public static final String GENERATOR_ID = "generator_blasting";
    public static RegistryObject<net.minecraft.world.item.crafting.RecipeType<GeneratorRecipe>> GENERATOR_RECIPE_TYPE = RECIPE_TYPES.register("generator_blasting", () -> new net.minecraft.world.item.crafting.RecipeType<GeneratorRecipe>(){

        public String toString() {
            return Registration.GENERATOR_ID;
        }
    });
    public static RegistryObject<RecipeSerializer<GeneratorRecipe>> GENERATOR_RECIPE_SERIALIZER = RECIPE_SERIALIZERS.register("generator_blasting", GeneratorRecipe.Serializer::new);
    public static final RegistryObject<BlockIronFurnace> IRON_FURNACE = BLOCKS.register("iron_furnace", () -> new BlockIronFurnace(BlockBehaviour.Properties.m_60926_((BlockBehaviour)Blocks.f_50075_)));
    public static final RegistryObject<Item> IRON_FURNACE_ITEM = ITEMS.register("iron_furnace", () -> new ItemFurnace((Block)IRON_FURNACE.get(), new Item.Properties(), (Integer)Config.ironFurnaceSpeed.get()));
    public static final RegistryObject<BlockEntityType<BlockIronFurnaceTile>> IRON_FURNACE_TILE = TILES.register("iron_furnace", () -> BlockEntityType.Builder.m_155273_(BlockIronFurnaceTile::new, (Block[])new Block[]{(Block)IRON_FURNACE.get()}).m_58966_(null));
    public static final RegistryObject<MenuType<BlockIronFurnaceContainer>> IRON_FURNACE_CONTAINER = CONTAINERS.register("iron_furnace", () -> IForgeMenuType.create((windowId, inv, data) -> {
        BlockPos pos = data.m_130135_();
        Level world = inv.f_35978_.m_20193_();
        return new BlockIronFurnaceContainer(windowId, world, pos, inv, inv.f_35978_);
    }));
    public static final RegistryObject<BlockGoldFurnace> GOLD_FURNACE = BLOCKS.register("gold_furnace", () -> new BlockGoldFurnace(BlockBehaviour.Properties.m_60926_((BlockBehaviour)Blocks.f_50074_)));
    public static final RegistryObject<Item> GOLD_FURNACE_ITEM = ITEMS.register("gold_furnace", () -> new ItemFurnace((Block)GOLD_FURNACE.get(), new Item.Properties(), (Integer)Config.goldFurnaceSpeed.get()));
    public static final RegistryObject<BlockEntityType<BlockGoldFurnaceTile>> GOLD_FURNACE_TILE = TILES.register("gold_furnace", () -> BlockEntityType.Builder.m_155273_(BlockGoldFurnaceTile::new, (Block[])new Block[]{(Block)GOLD_FURNACE.get()}).m_58966_(null));
    public static final RegistryObject<MenuType<BlockGoldFurnaceContainer>> GOLD_FURNACE_CONTAINER = CONTAINERS.register("gold_furnace", () -> IForgeMenuType.create((windowId, inv, data) -> {
        BlockPos pos = data.m_130135_();
        Level world = inv.f_35978_.m_20193_();
        return new BlockGoldFurnaceContainer(windowId, world, pos, inv, inv.f_35978_);
    }));
    public static final RegistryObject<BlockDiamondFurnace> DIAMOND_FURNACE = BLOCKS.register("diamond_furnace", () -> new BlockDiamondFurnace(BlockBehaviour.Properties.m_60926_((BlockBehaviour)Blocks.f_50090_)));
    public static final RegistryObject<Item> DIAMOND_FURNACE_ITEM = ITEMS.register("diamond_furnace", () -> new ItemFurnace((Block)DIAMOND_FURNACE.get(), new Item.Properties(), (Integer)Config.diamondFurnaceSpeed.get()));
    public static final RegistryObject<BlockEntityType<BlockDiamondFurnaceTile>> DIAMOND_FURNACE_TILE = TILES.register("diamond_furnace", () -> BlockEntityType.Builder.m_155273_(BlockDiamondFurnaceTile::new, (Block[])new Block[]{(Block)DIAMOND_FURNACE.get()}).m_58966_(null));
    public static final RegistryObject<MenuType<BlockDiamondFurnaceContainer>> DIAMOND_FURNACE_CONTAINER = CONTAINERS.register("diamond_furnace", () -> IForgeMenuType.create((windowId, inv, data) -> {
        BlockPos pos = data.m_130135_();
        Level world = inv.f_35978_.m_20193_();
        return new BlockDiamondFurnaceContainer(windowId, world, pos, inv, inv.f_35978_);
    }));
    public static final RegistryObject<BlockEmeraldFurnace> EMERALD_FURNACE = BLOCKS.register("emerald_furnace", () -> new BlockEmeraldFurnace(BlockBehaviour.Properties.m_60926_((BlockBehaviour)Blocks.f_50268_)));
    public static final RegistryObject<Item> EMERALD_FURNACE_ITEM = ITEMS.register("emerald_furnace", () -> new ItemFurnace((Block)EMERALD_FURNACE.get(), new Item.Properties(), (Integer)Config.emeraldFurnaceSpeed.get()));
    public static final RegistryObject<BlockEntityType<BlockEmeraldFurnaceTile>> EMERALD_FURNACE_TILE = TILES.register("emerald_furnace", () -> BlockEntityType.Builder.m_155273_(BlockEmeraldFurnaceTile::new, (Block[])new Block[]{(Block)EMERALD_FURNACE.get()}).m_58966_(null));
    public static final RegistryObject<MenuType<BlockEmeraldFurnaceContainer>> EMERALD_FURNACE_CONTAINER = CONTAINERS.register("emerald_furnace", () -> IForgeMenuType.create((windowId, inv, data) -> {
        BlockPos pos = data.m_130135_();
        Level world = inv.f_35978_.m_20193_();
        return new BlockEmeraldFurnaceContainer(windowId, world, pos, inv, inv.f_35978_);
    }));
    public static final RegistryObject<BlockObsidianFurnace> OBSIDIAN_FURNACE = BLOCKS.register("obsidian_furnace", () -> new BlockObsidianFurnace(BlockBehaviour.Properties.m_60926_((BlockBehaviour)Blocks.f_50080_).m_60913_(40.0f, 6000.0f)));
    public static final RegistryObject<Item> OBSIDIAN_FURNACE_ITEM = ITEMS.register("obsidian_furnace", () -> new ItemFurnace((Block)OBSIDIAN_FURNACE.get(), new Item.Properties(), (Integer)Config.obsidianFurnaceSpeed.get()));
    public static final RegistryObject<BlockEntityType<BlockObsidianFurnaceTile>> OBSIDIAN_FURNACE_TILE = TILES.register("obsidian_furnace", () -> BlockEntityType.Builder.m_155273_(BlockObsidianFurnaceTile::new, (Block[])new Block[]{(Block)OBSIDIAN_FURNACE.get()}).m_58966_(null));
    public static final RegistryObject<MenuType<BlockObsidianFurnaceContainer>> OBSIDIAN_FURNACE_CONTAINER = CONTAINERS.register("obsidian_furnace", () -> IForgeMenuType.create((windowId, inv, data) -> {
        BlockPos pos = data.m_130135_();
        Level world = inv.f_35978_.m_20193_();
        return new BlockObsidianFurnaceContainer(windowId, world, pos, inv, inv.f_35978_);
    }));
    public static final RegistryObject<BlockCrystalFurnace> CRYSTAL_FURNACE = BLOCKS.register("crystal_furnace", () -> new BlockCrystalFurnace(BlockBehaviour.Properties.m_60926_((BlockBehaviour)Blocks.f_50377_).m_60955_().m_60922_(Registration::isntSolid).m_60960_(Registration::isntSolid).m_60971_(Registration::isntSolid)));
    public static final RegistryObject<Item> CRYSTAL_FURNACE_ITEM = ITEMS.register("crystal_furnace", () -> new ItemFurnace((Block)CRYSTAL_FURNACE.get(), new Item.Properties(), (Integer)Config.crystalFurnaceSpeed.get()));
    public static final RegistryObject<BlockEntityType<BlockCrystalFurnaceTile>> CRYSTAL_FURNACE_TILE = TILES.register("crystal_furnace", () -> BlockEntityType.Builder.m_155273_(BlockCrystalFurnaceTile::new, (Block[])new Block[]{(Block)CRYSTAL_FURNACE.get()}).m_58966_(null));
    public static final RegistryObject<MenuType<BlockCrystalFurnaceContainer>> CRYSTAL_FURNACE_CONTAINER = CONTAINERS.register("crystal_furnace", () -> IForgeMenuType.create((windowId, inv, data) -> {
        BlockPos pos = data.m_130135_();
        Level world = inv.f_35978_.m_20193_();
        return new BlockCrystalFurnaceContainer(windowId, world, pos, inv, inv.f_35978_);
    }));
    public static final RegistryObject<BlockNetheriteFurnace> NETHERITE_FURNACE = BLOCKS.register("netherite_furnace", () -> new BlockNetheriteFurnace(BlockBehaviour.Properties.m_60926_((BlockBehaviour)Blocks.f_50721_).m_60913_(40.0f, 6000.0f)));
    public static final RegistryObject<Item> NETHERITE_FURNACE_ITEM = ITEMS.register("netherite_furnace", () -> new ItemFurnace((Block)NETHERITE_FURNACE.get(), new Item.Properties(), (Integer)Config.netheriteFurnaceSpeed.get()));
    public static final RegistryObject<BlockEntityType<BlockNetheriteFurnaceTile>> NETHERITE_FURNACE_TILE = TILES.register("netherite_furnace", () -> BlockEntityType.Builder.m_155273_(BlockNetheriteFurnaceTile::new, (Block[])new Block[]{(Block)NETHERITE_FURNACE.get()}).m_58966_(null));
    public static final RegistryObject<MenuType<BlockNetheriteFurnaceContainer>> NETHERITE_FURNACE_CONTAINER = CONTAINERS.register("netherite_furnace", () -> IForgeMenuType.create((windowId, inv, data) -> {
        BlockPos pos = data.m_130135_();
        Level world = inv.f_35978_.m_20193_();
        return new BlockNetheriteFurnaceContainer(windowId, world, pos, inv, inv.f_35978_);
    }));
    public static final RegistryObject<BlockCopperFurnace> COPPER_FURNACE = BLOCKS.register("copper_furnace", () -> new BlockCopperFurnace(BlockBehaviour.Properties.m_60926_((BlockBehaviour)Blocks.f_50074_)));
    public static final RegistryObject<Item> COPPER_FURNACE_ITEM = ITEMS.register("copper_furnace", () -> new ItemFurnace((Block)COPPER_FURNACE.get(), new Item.Properties(), (Integer)Config.copperFurnaceSpeed.get()));
    public static final RegistryObject<BlockEntityType<BlockCopperFurnaceTile>> COPPER_FURNACE_TILE = TILES.register("copper_furnace", () -> BlockEntityType.Builder.m_155273_(BlockCopperFurnaceTile::new, (Block[])new Block[]{(Block)COPPER_FURNACE.get()}).m_58966_(null));
    public static final RegistryObject<MenuType<BlockCopperFurnaceContainer>> COPPER_FURNACE_CONTAINER = CONTAINERS.register("copper_furnace", () -> IForgeMenuType.create((windowId, inv, data) -> {
        BlockPos pos = data.m_130135_();
        Level world = inv.f_35978_.m_20193_();
        return new BlockCopperFurnaceContainer(windowId, world, pos, inv, inv.f_35978_);
    }));
    public static final RegistryObject<BlockSilverFurnace> SILVER_FURNACE = BLOCKS.register("silver_furnace", () -> new BlockSilverFurnace(BlockBehaviour.Properties.m_60926_((BlockBehaviour)Blocks.f_50075_)));
    public static final RegistryObject<Item> SILVER_FURNACE_ITEM = ITEMS.register("silver_furnace", () -> new ItemFurnace((Block)SILVER_FURNACE.get(), new Item.Properties(), (Integer)Config.silverFurnaceSpeed.get()));
    public static final RegistryObject<BlockEntityType<BlockSilverFurnaceTile>> SILVER_FURNACE_TILE = TILES.register("silver_furnace", () -> BlockEntityType.Builder.m_155273_(BlockSilverFurnaceTile::new, (Block[])new Block[]{(Block)SILVER_FURNACE.get()}).m_58966_(null));
    public static final RegistryObject<MenuType<BlockSilverFurnaceContainer>> SILVER_FURNACE_CONTAINER = CONTAINERS.register("silver_furnace", () -> IForgeMenuType.create((windowId, inv, data) -> {
        BlockPos pos = data.m_130135_();
        Level world = inv.f_35978_.m_20193_();
        return new BlockSilverFurnaceContainer(windowId, world, pos, inv, inv.f_35978_);
    }));
    public static final RegistryObject<ItemUpgradeIron> IRON_UPGRADE = ITEMS.register("upgrade_iron", () -> new ItemUpgradeIron(new Item.Properties()));
    public static final RegistryObject<ItemUpgradeGold> GOLD_UPGRADE = ITEMS.register("upgrade_gold", () -> new ItemUpgradeGold(new Item.Properties()));
    public static final RegistryObject<ItemUpgradeDiamond> DIAMOND_UPGRADE = ITEMS.register("upgrade_diamond", () -> new ItemUpgradeDiamond(new Item.Properties()));
    public static final RegistryObject<ItemUpgradeEmerald> EMERALD_UPGRADE = ITEMS.register("upgrade_emerald", () -> new ItemUpgradeEmerald(new Item.Properties()));
    public static final RegistryObject<ItemUpgradeObsidian> OBSIDIAN_UPGRADE = ITEMS.register("upgrade_obsidian", () -> new ItemUpgradeObsidian(new Item.Properties()));
    public static final RegistryObject<ItemUpgradeCrystal> CRYSTAL_UPGRADE = ITEMS.register("upgrade_crystal", () -> new ItemUpgradeCrystal(new Item.Properties()));
    public static final RegistryObject<ItemUpgradeNetherite> NETHERITE_UPGRADE = ITEMS.register("upgrade_netherite", () -> new ItemUpgradeNetherite(new Item.Properties()));
    public static final RegistryObject<ItemUpgradeCopper> COPPER_UPGRADE = ITEMS.register("upgrade_copper", () -> new ItemUpgradeCopper(new Item.Properties()));
    public static final RegistryObject<ItemUpgradeSilver> SILVER_UPGRADE = ITEMS.register("upgrade_silver", () -> new ItemUpgradeSilver(new Item.Properties()));
    public static final RegistryObject<ItemUpgradeObsidian2> OBSIDIAN2_UPGRADE = ITEMS.register("upgrade_obsidian2", () -> new ItemUpgradeObsidian2(new Item.Properties()));
    public static final RegistryObject<ItemUpgradeIron2> IRON2_UPGRADE = ITEMS.register("upgrade_iron2", () -> new ItemUpgradeIron2(new Item.Properties()));
    public static final RegistryObject<ItemUpgradeGold2> GOLD2_UPGRADE = ITEMS.register("upgrade_gold2", () -> new ItemUpgradeGold2(new Item.Properties()));
    public static final RegistryObject<ItemUpgradeSilver2> SILVER2_UPGRADE = ITEMS.register("upgrade_silver2", () -> new ItemUpgradeSilver2(new Item.Properties()));
    public static RegistryObject<BlockAllthemodiumFurnace> ALLTHEMODIUM_FURNACE = BLOCKS.register("allthemodium_furnace", () -> new BlockAllthemodiumFurnace(BlockBehaviour.Properties.m_60926_((BlockBehaviour)Blocks.f_50074_)));
    public static final RegistryObject<Item> ALLTHEMODIUM_FURNACE_ITEM = ITEMS.register("allthemodium_furnace", () -> new ItemFurnace((Block)ALLTHEMODIUM_FURNACE.get(), new Item.Properties(), (Integer)Config.allthemodiumFurnaceSpeed.get()));
    public static final RegistryObject<BlockEntityType<BlockAllthemodiumFurnaceTile>> ALLTHEMODIUM_FURNACE_TILE = TILES.register("allthemodium_furnace", () -> BlockEntityType.Builder.m_155273_(BlockAllthemodiumFurnaceTile::new, (Block[])new Block[]{(Block)ALLTHEMODIUM_FURNACE.get()}).m_58966_(null));
    public static final RegistryObject<MenuType<BlockAllthemodiumFurnaceContainer>> ALLTHEMODIUM_FURNACE_CONTAINER = CONTAINERS.register("allthemodium_furnace", () -> IForgeMenuType.create((windowId, inv, data) -> {
        BlockPos pos = data.m_130135_();
        Level world = inv.f_35978_.m_20193_();
        return new BlockAllthemodiumFurnaceContainer(windowId, world, pos, inv, inv.f_35978_);
    }));
    public static final RegistryObject<BlockVibraniumFurnace> VIBRANIUM_FURNACE = BLOCKS.register("vibranium_furnace", () -> new BlockVibraniumFurnace(BlockBehaviour.Properties.m_60926_((BlockBehaviour)Blocks.f_50090_)));
    public static final RegistryObject<Item> VIBRANIUM_FURNACE_ITEM = ITEMS.register("vibranium_furnace", () -> new ItemFurnace((Block)VIBRANIUM_FURNACE.get(), new Item.Properties(), (Integer)Config.vibraniumFurnaceSpeed.get()));
    public static final RegistryObject<BlockEntityType<BlockVibraniumFurnaceTile>> VIBRANIUM_FURNACE_TILE = TILES.register("vibranium_furnace", () -> BlockEntityType.Builder.m_155273_(BlockVibraniumFurnaceTile::new, (Block[])new Block[]{(Block)VIBRANIUM_FURNACE.get()}).m_58966_(null));
    public static final RegistryObject<MenuType<BlockVibraniumFurnaceContainer>> VIBRANIUM_FURNACE_CONTAINER = CONTAINERS.register("vibranium_furnace", () -> IForgeMenuType.create((windowId, inv, data) -> {
        BlockPos pos = data.m_130135_();
        Level world = inv.f_35978_.m_20193_();
        return new BlockVibraniumFurnaceContainer(windowId, world, pos, inv, inv.f_35978_);
    }));
    public static final RegistryObject<BlockUnobtainiumFurnace> UNOBTAINIUM_FURNACE = BLOCKS.register("unobtainium_furnace", () -> new BlockUnobtainiumFurnace(BlockBehaviour.Properties.m_60926_((BlockBehaviour)Blocks.f_50721_)));
    public static final RegistryObject<Item> UNOBTAINIUM_FURNACE_ITEM = ITEMS.register("unobtainium_furnace", () -> new ItemFurnace((Block)UNOBTAINIUM_FURNACE.get(), new Item.Properties(), (Integer)Config.unobtainiumFurnaceSpeed.get()));
    public static final RegistryObject<BlockEntityType<BlockUnobtainiumFurnaceTile>> UNOBTAINIUM_FURNACE_TILE = TILES.register("unobtainium_furnace", () -> BlockEntityType.Builder.m_155273_(BlockUnobtainiumFurnaceTile::new, (Block[])new Block[]{(Block)UNOBTAINIUM_FURNACE.get()}).m_58966_(null));
    public static final RegistryObject<MenuType<BlockUnobtainiumFurnaceContainer>> UNOBTAINIUM_FURNACE_CONTAINER = CONTAINERS.register("unobtainium_furnace", () -> IForgeMenuType.create((windowId, inv, data) -> {
        BlockPos pos = data.m_130135_();
        Level world = inv.f_35978_.m_20193_();
        return new BlockUnobtainiumFurnaceContainer(windowId, world, pos, inv, inv.f_35978_);
    }));
    public static final RegistryObject<ItemUpgradeAllthemodium> ALLTHEMODIUM_UPGRADE = ITEMS.register("upgrade_allthemodium", () -> new ItemUpgradeAllthemodium(new Item.Properties()));
    public static final RegistryObject<ItemUpgradeVibranium> VIBRANIUM_UPGRADE = ITEMS.register("upgrade_vibranium", () -> new ItemUpgradeVibranium(new Item.Properties()));
    public static final RegistryObject<ItemUpgradeUnobtainium> UNOBTAINIUM_UPGRADE = ITEMS.register("upgrade_unobtainium", () -> new ItemUpgradeUnobtainium(new Item.Properties()));
    public static final RegistryObject<BlockWirelessEnergyHeater> HEATER = BLOCKS.register("heater", () -> new BlockWirelessEnergyHeater(BlockBehaviour.Properties.m_60926_((BlockBehaviour)Blocks.f_50075_)));
    public static final RegistryObject<Item> HEATER_ITEM = ITEMS.register("heater", () -> new BlockItemHeater((Block)HEATER.get(), new Item.Properties()));
    public static final RegistryObject<BlockEntityType<BlockWirelessEnergyHeaterTile>> HEATER_TILE = TILES.register("heater", () -> BlockEntityType.Builder.m_155273_(BlockWirelessEnergyHeaterTile::new, (Block[])new Block[]{(Block)HEATER.get()}).m_58966_(null));
    public static final RegistryObject<MenuType<BlockWirelessEnergyHeaterContainer>> HEATER_CONTAINER = CONTAINERS.register("heater", () -> IForgeMenuType.create((windowId, inv, data) -> {
        BlockPos pos = data.m_130135_();
        Level world = inv.f_35978_.m_20193_();
        return new BlockWirelessEnergyHeaterContainer(windowId, world, pos, inv, inv.f_35978_);
    }));
    public static final RegistryObject<ItemHeater> ITEM_HEATER = ITEMS.register("item_heater", () -> new ItemHeater(new Item.Properties().m_41487_(1)));
    public static final RegistryObject<ItemAugmentBlasting> BLASTING_AUGMENT = ITEMS.register("augment_blasting", () -> new ItemAugmentBlasting(new Item.Properties()));
    public static final RegistryObject<ItemAugmentSmoking> SMOKING_AUGMENT = ITEMS.register("augment_smoking", () -> new ItemAugmentSmoking(new Item.Properties()));
    public static final RegistryObject<ItemAugmentFactory> FACTORY_AUGMENT = ITEMS.register("augment_factory", () -> new ItemAugmentFactory(new Item.Properties()));
    public static final RegistryObject<ItemAugmentGenerator> GENERATOR_AUGMENT = ITEMS.register("augment_generator", () -> new ItemAugmentGenerator(new Item.Properties()));
    public static final RegistryObject<ItemAugmentSpeed> SPEED_AUGMENT = ITEMS.register("augment_speed", () -> new ItemAugmentSpeed(new Item.Properties()));
    public static final RegistryObject<ItemAugmentFuel> FUEL_AUGMENT = ITEMS.register("augment_fuel", () -> new ItemAugmentFuel(new Item.Properties()));
    public static final RegistryObject<ItemSpooky> ITEM_SPOOKY = ITEMS.register("item_spooky", () -> new ItemSpooky(new Item.Properties()));
    public static final RegistryObject<ItemXmas> ITEM_XMAS = ITEMS.register("item_xmas", () -> new ItemXmas(new Item.Properties()));
    public static final RegistryObject<ItemFurnaceCopy> ITEM_COPY = ITEMS.register("item_copy", () -> new ItemFurnaceCopy(new Item.Properties().m_41487_(1)));
    public static final RegistryObject<Item> RAINBOW_CORE = ITEMS.register("rainbow_core", () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> RAINBOW_PLATING = ITEMS.register("rainbow_plating", () -> new Item(new Item.Properties()));
    public static final RegistryObject<ItemRainbowCoal> RAINBOW_COAL = ITEMS.register("rainbow_coal", () -> new ItemRainbowCoal(new Item.Properties()));
    public static final RegistryObject<BlockMillionFurnace> MILLION_FURNACE = BLOCKS.register("million_furnace", () -> new BlockMillionFurnace(BlockBehaviour.Properties.m_60926_((BlockBehaviour)Blocks.f_50075_)));
    public static final RegistryObject<Item> MILLION_FURNACE_ITEM = ITEMS.register("million_furnace", () -> new ItemMillionFurnace((Block)MILLION_FURNACE.get(), new Item.Properties()));
    public static final RegistryObject<BlockEntityType<BlockMillionFurnaceTile>> MILLION_FURNACE_TILE = TILES.register("million_furnace", () -> BlockEntityType.Builder.m_155273_(BlockMillionFurnaceTile::new, (Block[])new Block[]{(Block)MILLION_FURNACE.get()}).m_58966_(null));
    public static final RegistryObject<MenuType<BlockMillionFurnaceContainer>> MILLION_FURNACE_CONTAINER = CONTAINERS.register("million_furnace", () -> IForgeMenuType.create((windowId, inv, data) -> {
        BlockPos pos = data.m_130135_();
        Level world = inv.f_35978_.m_20193_();
        return new BlockMillionFurnaceContainer(windowId, world, pos, inv, inv.f_35978_);
    }));
    public static final RegistryObject<CreativeModeTab> tabIronFurnaces = CREATIVE_MODE_TABS.register("ironfurnaces_tab", () -> CreativeModeTab.builder().withTabsBefore(new ResourceKey[]{CreativeModeTabs.f_256797_}).m_257737_(() -> ((BlockIronFurnace)((Object)((Object)((Object)IRON_FURNACE.get())))).m_5456_().m_7968_()).m_257941_((Component)Component.m_237115_((String)"itemGroup.ironfurnaces")).m_257501_((parameters, output) -> {
        output.m_246326_((ItemLike)IRON_FURNACE_ITEM.get());
        output.m_246326_((ItemLike)GOLD_FURNACE_ITEM.get());
        output.m_246326_((ItemLike)DIAMOND_FURNACE_ITEM.get());
        output.m_246326_((ItemLike)EMERALD_FURNACE_ITEM.get());
        output.m_246326_((ItemLike)OBSIDIAN_FURNACE_ITEM.get());
        output.m_246326_((ItemLike)CRYSTAL_FURNACE_ITEM.get());
        output.m_246326_((ItemLike)NETHERITE_FURNACE_ITEM.get());
        output.m_246326_((ItemLike)COPPER_FURNACE_ITEM.get());
        output.m_246326_((ItemLike)SILVER_FURNACE_ITEM.get());
        output.m_246326_((ItemLike)IRON_UPGRADE.get());
        output.m_246326_((ItemLike)GOLD_UPGRADE.get());
        output.m_246326_((ItemLike)DIAMOND_UPGRADE.get());
        output.m_246326_((ItemLike)EMERALD_UPGRADE.get());
        output.m_246326_((ItemLike)OBSIDIAN_UPGRADE.get());
        output.m_246326_((ItemLike)CRYSTAL_UPGRADE.get());
        output.m_246326_((ItemLike)NETHERITE_UPGRADE.get());
        output.m_246326_((ItemLike)COPPER_UPGRADE.get());
        output.m_246326_((ItemLike)SILVER_UPGRADE.get());
        output.m_246326_((ItemLike)OBSIDIAN2_UPGRADE.get());
        output.m_246326_((ItemLike)IRON2_UPGRADE.get());
        output.m_246326_((ItemLike)GOLD2_UPGRADE.get());
        output.m_246326_((ItemLike)SILVER2_UPGRADE.get());
        output.m_246326_((ItemLike)HEATER_ITEM.get());
        output.m_246326_((ItemLike)ITEM_HEATER.get());
        output.m_246326_((ItemLike)BLASTING_AUGMENT.get());
        output.m_246326_((ItemLike)SMOKING_AUGMENT.get());
        output.m_246326_((ItemLike)FACTORY_AUGMENT.get());
        output.m_246326_((ItemLike)GENERATOR_AUGMENT.get());
        output.m_246326_((ItemLike)SPEED_AUGMENT.get());
        output.m_246326_((ItemLike)FUEL_AUGMENT.get());
        output.m_246326_((ItemLike)ITEM_SPOOKY.get());
        output.m_246326_((ItemLike)ITEM_XMAS.get());
        output.m_246326_((ItemLike)ITEM_COPY.get());
        output.m_246326_((ItemLike)RAINBOW_CORE.get());
        output.m_246326_((ItemLike)RAINBOW_PLATING.get());
        output.m_246326_((ItemLike)MILLION_FURNACE_ITEM.get());
        output.m_246326_((ItemLike)RAINBOW_COAL.get());
        if (ModList.get().isLoaded("allthemodium")) {
            output.m_246326_((ItemLike)ALLTHEMODIUM_FURNACE_ITEM.get());
            output.m_246326_((ItemLike)VIBRANIUM_FURNACE_ITEM.get());
            output.m_246326_((ItemLike)UNOBTAINIUM_FURNACE_ITEM.get());
            output.m_246326_((ItemLike)ALLTHEMODIUM_UPGRADE.get());
            output.m_246326_((ItemLike)VIBRANIUM_UPGRADE.get());
            output.m_246326_((ItemLike)UNOBTAINIUM_UPGRADE.get());
        }
    }).m_257652_());

    public static void init() {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();
        BLOCKS.register(modEventBus);
        ITEMS.register(modEventBus);
        TILES.register(modEventBus);
        CONTAINERS.register(modEventBus);
        RECIPE_SERIALIZERS.register(modEventBus);
        RECIPE_TYPES.register(modEventBus);
        CREATIVE_MODE_TABS.register(modEventBus);
    }

    private static Boolean isntSolid(BlockState blockState, BlockGetter blockGetter, BlockPos blockPos, EntityType<?> entityType) {
        return false;
    }

    private static boolean isntSolid(BlockState p_50806_, BlockGetter p_50807_, BlockPos p_50808_) {
        return false;
    }

    public static final class RecipeTypes {
        public static RecipeType<GeneratorRecipe> GENERATOR_BLASTING = RecipeType.create((String)"ironfurnaces", (String)"generator_blasting", GeneratorRecipe.class);
        public static RecipeType<SimpleGeneratorRecipe> GENERATOR_SMOKING = RecipeType.create((String)"ironfurnaces", (String)"generator_smoking", SimpleGeneratorRecipe.class);
        public static RecipeType<SimpleGeneratorRecipe> GENERATOR_REGULAR = RecipeType.create((String)"ironfurnaces", (String)"generator_regular", SimpleGeneratorRecipe.class);
    }
}

