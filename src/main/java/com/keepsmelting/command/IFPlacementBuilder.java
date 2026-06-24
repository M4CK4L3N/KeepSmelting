package com.keepsmelting.command;

import net.minecraft.core.BlockPos;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.List;

/**
 * Построение IF-схем для /keepsmelting test (строки, без IF-классов).
 */
public class IFPlacementBuilder {

    private IFPlacementBuilder() {}

    static List<VanillaTestPatterns.FurnacePlacement> buildIFPlacements(String config, BlockPos origin) {
        List<VanillaTestPatterns.FurnacePlacement> placements = new ArrayList<>();
        boolean hasChests = config.endsWith("+chest");
        String baseConfig = hasChests ? config.replace("+chest", "") : config;

        switch (baseConfig) {
            case "basic":
                placements.add(fp(origin, "gen", 3));
                placements.add(fp(origin.south(), "fact", 2));
                if (hasChests) {
                    placements.add(fp(origin.above(), "chest", 64));
                    placements.add(fp(origin.south().above(), "chest", 64));
                    placements.add(fp(origin.south().below(), "chest_out", 0));
                }
                break;
            case "gen":
                placements.add(fp(origin, "gen", 3));
                if (hasChests) {
                    placements.add(fp(origin.above(), "chest", 64));
                }
                break;
            case "factory":
                placements.add(fp(origin, "fact", 2));
                if (hasChests) {
                    placements.add(fp(origin.above(), "chest", 64));
                    placements.add(fp(origin.below(), "chest_out", 0));
                }
                break;
            case "nw":
                placements.add(fp(origin, "gen", 3));
                placements.add(fp(origin.south(), "gen", 3));
                placements.add(fp(origin.south().south(), "fact", 2));
                if (hasChests) {
                    placements.add(fp(origin.above(), "chest", 64));
                    placements.add(fp(origin.south().above(), "chest", 64));
                    placements.add(fp(origin.south().south().above(), "chest", 64));
                    placements.add(fp(origin.south().south().below(), "chest_out", 0));
                }
                break;
            default:
                return null;
        }
        return placements;
    }

    private static VanillaTestPatterns.FurnacePlacement fp(BlockPos pos, String type, int side) {
        return new VanillaTestPatterns.FurnacePlacement(pos, type, Items.AIR, 0, side);
    }
}
