package com.keepsmelting.command;

import com.keepsmelting.KeepSmelting;
import com.keepsmelting.api.CatchupHandlerRegistry;
import com.keepsmelting.api.IFurnaceCatchupHandler;
import com.keepsmelting.internal.catchup.VanillaCatchupHandler;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.fml.ModList;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * /keepsmelting test <config> [<ticks> [keep]]
 */
public class TestSubcommand {

    /** Паттерны для IF (строки, без классов) */
    public static final Set<String> IF_PATTERNS = Set.of("basic", "gen", "factory", "nw");

    public static int runSpawn(CommandContext<CommandSourceStack> ctx, String config) {
        ServerPlayer player = CommandUtil.getPlayer(ctx);
        if (player == null) return 0;

        ServerLevel level = player.serverLevel();
        BlockPos origin = player.blockPosition().offset(4, 0, 0);

        List<VanillaTestPatterns.FurnacePlacement> placements = buildPlacements(config, origin, level, false);
        if (placements == null) return 0;

        spawnPlacements(level, placements, config);
        CommandUtil.send(player, " §aSpawned " + placements.size() + " block(s).");
        return 1;
    }

    public static int runTest(CommandContext<CommandSourceStack> ctx, String config, long ticks, boolean cleanup) {
        ServerPlayer player = CommandUtil.getPlayer(ctx);
        if (player == null) return 0;

        boolean hasIF = ModList.get().isLoaded("ironfurnaces");
        if (isIFPattern(config) && !hasIF) {
            CommandUtil.send(player, " §cIron Furnaces required for config: " + config);
            return 0;
        }

        ServerLevel level = player.serverLevel();
        BlockPos origin = player.blockPosition().offset(4, 0, 0);

        KeepSmelting.LOGGER.info("[Test] config={} ticks={} cleanup={} hasIF={}", config, ticks, cleanup, hasIF);

        List<VanillaTestPatterns.FurnacePlacement> placements = buildPlacements(config, origin, level, false);
        if (placements == null) return 0;

        for (VanillaTestPatterns.FurnacePlacement p : placements) {
            p.oldState = level.getBlockState(p.pos);
        }

        spawnPlacements(level, placements, config);

        if (ticks > 0) {
            for (VanillaTestPatterns.FurnacePlacement p : placements) {
                if (CommandUtil.isChestType(p.type)) continue;
                BlockEntity be = level.getBlockEntity(p.pos);
                if (be == null) continue;
                IFurnaceCatchupHandler h = CatchupHandlerRegistry.find(be.getClass());
                if (h != null) {
                    h.applyCatchup(be, ticks, level, p.pos);
                } else {
                    VanillaCatchupHandler.INSTANCE.applyCatchup(be, ticks, level, p.pos);
                }
            }
            CommandUtil.send(player, " §aSimulated §e" + ticks + "§7t on " + placements.size() + " blocks");
        }

        if (ticks > 0) {
            StringBuilder sb = new StringBuilder();
            for (VanillaTestPatterns.FurnacePlacement p : placements) {
                if (CommandUtil.isChestType(p.type)) continue;
                BlockEntity be = level.getBlockEntity(p.pos);
                if (be == null) continue;
                if (be instanceof AbstractFurnaceBlockEntity furnace) {
                    sb.append("§7Furnace§f")
                            .append(" i:").append(furnace.getItem(0).getCount())
                            .append(" f:").append(furnace.getItem(1).getCount())
                            .append(" o:").append(furnace.getItem(2).getCount())
                            .append(" | ");
                }
            }
            if (sb.length() > 0) CommandUtil.send(player, " §aTest §e" + ticks + "§7t §a| " + sb.toString());
        }

        if (ticks > 0 && cleanup) {
            for (VanillaTestPatterns.FurnacePlacement p : placements) {
                level.setBlock(p.pos, p.oldState, 3);
                level.removeBlockEntity(p.pos);
            }
            CommandUtil.send(player, " §7Очищено.");
        } else if (ticks > 0 && !cleanup) {
            CommandUtil.send(player, " §7Блоки оставлены.");
        }
        return 1;
    }

    static List<VanillaTestPatterns.FurnacePlacement> buildPlacements(
            String config, BlockPos origin, ServerLevel level, boolean forceVanilla) {
        if (!forceVanilla && isIFPattern(config)) {
            return IFPlacementBuilder.buildIFPlacements(config, origin);
        }
        if (VanillaTestPatterns.isVanillaPattern(config)) {
            return VanillaTestPatterns.build(config, origin);
        }
        return null;
    }

    static boolean isIFPattern(String config) {
        String base = config.endsWith("+chest") ? config.replace("+chest", "") : config;
        return IF_PATTERNS.contains(base);
    }

    static void spawnPlacements(ServerLevel level, List<VanillaTestPatterns.FurnacePlacement> placements, String config) {
        for (VanillaTestPatterns.FurnacePlacement p : placements) {
            VanillaTestPatterns.spawnFurnace(level, p, config);
        }
    }
}
