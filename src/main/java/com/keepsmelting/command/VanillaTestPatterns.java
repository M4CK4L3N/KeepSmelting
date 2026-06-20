package com.keepsmelting.command;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HopperBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class VanillaTestPatterns {

    public static final Map<String, String> PATTERNS = new LinkedHashMap<>();
    static {
        PATTERNS.put("furnace",       "Furnace solo | coal+raw_iron");
        PATTERNS.put("furnace+chest", "Furnace | barrel(ore)↑→furnace, barrel(coal)←side→furnace, barrel(out)↓←furnace");
        PATTERNS.put("smoker",        "Smoker solo | coal+beef");
        PATTERNS.put("blast",         "Blast furnace solo | coal+raw_iron");
    }

    public static boolean isVanillaPattern(String config) {
        String base = config.endsWith("+chest") ? config.replace("+chest", "") : config;
        return PATTERNS.containsKey(base);
    }

    /**
     * Схема для ванильной печи с воронками (+chest):
     *
     *  [input barrel: ore]            [fuel barrel: coal]
     *        ↑(y+2)                        ↑(y, x-2)
     *        ↓ hopper (в верх)            → hopper (в бок)
     *        ↑(y+1)                        ↑(y, x-1)
     *     ┌──┴──┐                   ┌──────┘
     *     │  FURNACE  │← ← ← ← ← ← ←
     *     └──┬──┘
     *        ↓ hopper (из низа)
     *        ↑(y-1)
     *  [output barrel]
     *        ↑(y-2)
     */
    public static List<FurnacePlacement> build(String config, BlockPos origin) {
        List<FurnacePlacement> placements = new ArrayList<>();
        boolean hasChests = config.endsWith("+chest");
        String base = hasChests ? config.replace("+chest", "") : config;

        // Печь всегда
        placements.add(fp(origin, base));

        if (hasChests) {
            // Вход: бочка с рудой сверху → hopper в верх печи
            placements.add(fp(origin.above(2), "chest_input", getInputItem(base), 64));
            placements.add(fp(origin.above(), "hopper_into_top"));

            // Топливо: бочка с углём над боковой воронкой → hopper в бок печи (на восток)
            placements.add(fp(origin.west().above(), "chest_fuel", Items.COAL, 64));
            placements.add(fp(origin.west(), "hopper_into_side"));

            // Выход: hopper из низа печи → бочка снизу
            placements.add(fp(origin.below(), "hopper_out_of_bottom"));
            placements.add(fp(origin.below(2), "chest_out", Items.AIR, 0));
        }

        return placements;
    }

    // ========================================================================
    // SPAWN
    // ========================================================================

    public static void spawnFurnace(ServerLevel level, FurnacePlacement p, String config) {
        BlockEntity oldBe = level.getBlockEntity(p.pos);
        if (oldBe != null) oldBe.setRemoved();

        boolean hasChests = config.endsWith("+chest");
        String base = hasChests ? config.replace("+chest", "") : config;

        switch (p.type) {
            case "hopper_into_top":
                // Hopper pointing DOWN — в верх печи (вход: руда)
                level.setBlock(p.pos, Blocks.HOPPER.defaultBlockState()
                        .setValue(HopperBlock.FACING, Direction.DOWN), 3);
                return;
            case "hopper_into_side":
                // Hopper pointing EAST — в бок печи (топливо)
                level.setBlock(p.pos, Blocks.HOPPER.defaultBlockState()
                        .setValue(HopperBlock.FACING, Direction.EAST), 3);
                return;
            case "hopper_out_of_bottom":
                // Hopper pointing DOWN — в бочку выхода
                level.setBlock(p.pos, Blocks.HOPPER.defaultBlockState()
                        .setValue(HopperBlock.FACING, Direction.DOWN), 3);
                return;
            case "chest_input":
            case "chest_fuel":
            case "chest_out":
                spawnBarrel(level, p);
                return;
            default:
                // Печь
                break;
        }

        Block furnaceBlock;
        ItemStack inputItem;

        switch (base) {
            case "furnace":
                furnaceBlock = Blocks.FURNACE;
                inputItem = new ItemStack(Items.IRON_ORE, 16);
                break;
            case "smoker":
                furnaceBlock = Blocks.SMOKER;
                inputItem = new ItemStack(Items.BEEF, 16);
                break;
            case "blast":
                furnaceBlock = Blocks.BLAST_FURNACE;
                inputItem = new ItemStack(Items.IRON_ORE, 16);
                break;
            default:
                throw new IllegalArgumentException("Unknown pattern: " + base);
        }

        level.setBlock(p.pos, furnaceBlock.defaultBlockState(), 3);

        BlockEntity be = level.getBlockEntity(p.pos);
        if (be instanceof net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity furnace) {
            furnace.setItem(0, inputItem);
            furnace.setItem(1, new ItemStack(Items.COAL, 32));
            furnace.setChanged();
        }
    }

    private static net.minecraft.world.item.Item getInputItem(String base) {
        return switch (base) {
            case "furnace" -> Items.IRON_ORE;
            case "smoker" -> Items.BEEF;
            case "blast" -> Items.IRON_ORE;
            default -> Items.IRON_ORE;
        };
    }

    private static void spawnBarrel(ServerLevel level, FurnacePlacement p) {
        BlockEntity old = level.getBlockEntity(p.pos);
        if (old != null) old.setRemoved();
        level.setBlock(p.pos, Blocks.BARREL.defaultBlockState(), 3);

        BlockEntity be = level.getBlockEntity(p.pos);
        if (be instanceof net.minecraft.world.level.block.entity.BarrelBlockEntity barrel) {
            if (p.chestItem != null && p.chestItem != Items.AIR && p.chestCount > 0) {
                barrel.setItem(0, new ItemStack(p.chestItem, p.chestCount));
            }
            barrel.setChanged();
        }
    }

    // ========================================================================
    // PLACEMENT
    // ========================================================================

    public static FurnacePlacement fp(BlockPos pos, String type) {
        return new FurnacePlacement(pos, type, Items.AIR, 0, 0);
    }

    public static FurnacePlacement fp(BlockPos pos, String type, int side) {
        return new FurnacePlacement(pos, type, Items.AIR, 0, side);
    }

    public static FurnacePlacement fp(BlockPos pos, String type, net.minecraft.world.item.Item item, int count) {
        return new FurnacePlacement(pos, type, item, count, 0);
    }

    public static class FurnacePlacement {
        public final BlockPos pos;
        public final String type;
        public int side;
        public final net.minecraft.world.item.Item chestItem;
        public final int chestCount;
        public BlockState oldState;

        public FurnacePlacement(BlockPos pos, String type, net.minecraft.world.item.Item chestItem, int chestCount, int side) {
            this.pos = pos;
            this.type = type;
            this.chestItem = chestItem;
            this.chestCount = chestCount;
            this.side = side;
        }
    }
}
