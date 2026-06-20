package com.keepsmelting.command;

import com.keepsmelting.KeepSmeltingConfig;
import com.keepsmelting.KeepSmeltingConfig.DebugMode;
import com.keepsmelting.api.CatchupHandlerRegistry;
import com.keepsmelting.api.IFurnaceCatchupHandler;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.LongArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.registries.ForgeRegistries;

public class KeepSmeltingCommand {

    private static final SuggestionProvider<CommandSourceStack> DEBUG_SUGGESTIONS =
            (ctx, builder) -> SharedSuggestionProvider.suggest(
                    new String[]{"OFF", "CHAT", "LOG"}, builder);

    private static final SuggestionProvider<CommandSourceStack> TIME_SUGGESTIONS =
            (ctx, builder) -> SharedSuggestionProvider.suggest(
                    new String[]{"REALTIME", "GAMETIME"}, builder);

    /** Шаблоны: имя → описание */
    private static final java.util.Map<String, String> PATTERNS = new java.util.HashMap<>();
    static {
        PATTERNS.put("basic", "Gen→Fact (RF) | coal+ore в печах");
        PATTERNS.put("basic+chest", "Gen→Fact (RF) | barrel:coal → gen, barrel:ore → fact, barrel:out ← fact");
        PATTERNS.put("gen", "Generator solo | coal в печи");
        PATTERNS.put("factory", "Factory solo | ore в печи");
        PATTERNS.put("nw", "Gen→Gen→Fact (сеть) | всё в печах");
        PATTERNS.put("nw+chest", "Gen→Gen→Fact (сеть) | chests: топливо → gens, ore → fact, out ← fact");
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("keepsmelting")
                .requires(src -> src.hasPermission(2))

                .then(Commands.literal("catchup")
                        .then(Commands.argument("value", BoolArgumentType.bool())
                                .executes(ctx -> setCatchup(ctx, BoolArgumentType.getBool(ctx, "value")))))

                .then(Commands.literal("debug")
                        .then(Commands.argument("mode", StringArgumentType.word())
                                .suggests(DEBUG_SUGGESTIONS)
                                .executes(ctx -> setDebugMode(ctx, StringArgumentType.getString(ctx, "mode")))))

                .then(Commands.literal("time")
                        .then(Commands.argument("mode", StringArgumentType.word())
                                .suggests(TIME_SUGGESTIONS)
                                .executes(ctx -> setTimeMode(ctx, StringArgumentType.getString(ctx, "mode")))))

                .then(Commands.literal("maxTicks")
                        .then(Commands.argument("value", LongArgumentType.longArg(1, 192000))
                                .executes(ctx -> setMaxTicks(ctx, LongArgumentType.getLong(ctx, "value")))))

                .then(Commands.literal("minDelta")
                        .then(Commands.argument("value", IntegerArgumentType.integer(1, 72000))
                                .executes(ctx -> setMinDelta(ctx, IntegerArgumentType.getInteger(ctx, "value")))))

                .then(Commands.literal("simulate")
                        .then(Commands.argument("ticks", LongArgumentType.longArg(1, 192000))
                                .executes(ctx -> runSimulate(ctx, LongArgumentType.getLong(ctx, "ticks")))))

                .then(Commands.literal("spawn")
                        .then(Commands.argument("config", StringArgumentType.word())
                                .suggests((ctx, builder) -> SharedSuggestionProvider.suggest(
                                        PATTERNS.keySet().stream().toArray(String[]::new), builder))
                                .executes(ctx -> runSpawn(ctx, StringArgumentType.getString(ctx, "config"), 0))
                                .then(Commands.argument("ticks", LongArgumentType.longArg(1, 192000))
                                        .executes(ctx -> runSpawn(ctx, StringArgumentType.getString(ctx, "config"),
                                                LongArgumentType.getLong(ctx, "ticks"))))))

                .then(Commands.literal("test")
                        .then(Commands.argument("config", StringArgumentType.word())
                                .suggests((ctx, builder) -> SharedSuggestionProvider.suggest(
                                        PATTERNS.keySet().stream().toArray(String[]::new), builder))
                                .then(Commands.argument("ticks", LongArgumentType.longArg(1, 192000))
                                        .executes(ctx -> runTest(ctx, StringArgumentType.getString(ctx, "config"),
                                                LongArgumentType.getLong(ctx, "ticks"))))))

                .then(Commands.literal("status")
                        .executes(KeepSmeltingCommand::showStatus))

                .executes(KeepSmeltingCommand::showHelp)
        );
    }

    // ========================================================================
    // SETTINGS
    // ========================================================================

    private static int setCatchup(CommandContext<CommandSourceStack> ctx, boolean value) {
        KeepSmeltingConfig.COMMON.catchupEnabled.set(value);
        KeepSmeltingConfig.COMMON.catchupEnabled.save();
        send(ctx, " catchup §a→ §f" + value);
        return 1;
    }

    private static int setMaxTicks(CommandContext<CommandSourceStack> ctx, long value) {
        KeepSmeltingConfig.COMMON.maxCatchupTicks.set(value);
        KeepSmeltingConfig.COMMON.maxCatchupTicks.save();
        send(ctx, " maxTicks §a→ §f" + value);
        return 1;
    }

    private static int setMinDelta(CommandContext<CommandSourceStack> ctx, int value) {
        KeepSmeltingConfig.COMMON.minDeltaThreshold.set(value);
        KeepSmeltingConfig.COMMON.minDeltaThreshold.save();
        send(ctx, " minDelta §a→ §f" + value);
        return 1;
    }

    private static int setDebugMode(CommandContext<CommandSourceStack> ctx, String mode) {
        try {
            DebugMode dm = DebugMode.valueOf(mode.toUpperCase());
            KeepSmeltingConfig.COMMON.debugMode.set(dm);
            KeepSmeltingConfig.COMMON.debugMode.save();
            send(ctx, " debug §a→ §f" + dm);
            return 1;
        } catch (IllegalArgumentException e) {
            ctx.getSource().sendFailure(Component.translatable("command.keepsmelting.prefix")
                    .append(" §c").append(Component.translatable("command.keepsmelting.debug.error.invalid")));
            return 0;
        }
    }

    private static int setTimeMode(CommandContext<CommandSourceStack> ctx, String mode) {
        try {
            KeepSmeltingConfig.TimeMode tm = KeepSmeltingConfig.TimeMode.valueOf(mode.toUpperCase());
            KeepSmeltingConfig.COMMON.timeMode.set(tm);
            KeepSmeltingConfig.COMMON.timeMode.save();
            send(ctx, " time §a→ §f" + tm);
            return 1;
        } catch (IllegalArgumentException e) {
            ctx.getSource().sendFailure(Component.translatable("command.keepsmelting.prefix")
                    .append(" §c").append(Component.translatable("command.keepsmelting.time.error.invalid")));
            return 0;
        }
    }

    private static int showStatus(CommandContext<CommandSourceStack> ctx) {
        ctx.getSource().sendSuccess(() ->
                Component.translatable("command.keepsmelting.prefix").append(" §f")
                        .append(Component.translatable("command.keepsmelting.status",
                                KeepSmeltingConfig.COMMON.catchupEnabled.get(),
                                KeepSmeltingConfig.COMMON.maxCatchupTicks.get(),
                                KeepSmeltingConfig.COMMON.minDeltaThreshold.get(),
                                KeepSmeltingConfig.COMMON.debugMode.get(),
                                KeepSmeltingConfig.COMMON.timeMode.get())),
                false);
        return 1;
    }

    private static int showHelp(CommandContext<CommandSourceStack> ctx) {
        sendHelp(ctx, " §7=== KeepSmelting Commands ===");
        sendHelp(ctx, " /keepsmelting simulate <ticks> — симуляция всех IF рядом");
        sendHelp(ctx, " /keepsmelting spawn <config> [ticks] — спавн + опц. симуляция");
        sendHelp(ctx, " /keepsmelting test <config> <ticks> — спавн → симуляция → отчёт → удаление");
        sendHelp(ctx, " §7Шаблоны:");
        for (java.util.Map.Entry<String, String> e : PATTERNS.entrySet()) {
            sendHelp(ctx, "  §e" + e.getKey() + "§7 — " + e.getValue());
        }
        sendHelp(ctx, " /keepsmelting status — текущие настройки");
        sendHelp(ctx, " /keepsmelting catchup|debug|time|maxTicks|minDelta <val> — настройки");
        return 1;
    }

    // ========================================================================
    // COMMANDS
    // ========================================================================

    private static int runSimulate(CommandContext<CommandSourceStack> ctx, long ticks) {
        ServerPlayer player = getPlayer(ctx);
        if (player == null) return 0;
        ServerLevel level = player.serverLevel();
        BlockPos origin = player.blockPosition();

        int found = 0;
        for (int dx = -10; dx <= 10; dx++)
            for (int dy = -5; dy <= 5; dy++)
                for (int dz = -10; dz <= 10; dz++) {
                    BlockPos p = origin.offset(dx, dy, dz);
                    if (!level.isLoaded(p)) continue;
                    BlockEntity be = level.getBlockEntity(p);
                    if (!(be instanceof ironfurnaces.tileentity.furnaces.BlockIronFurnaceTileBase ift)) continue;
                    IFurnaceCatchupHandler h = CatchupHandlerRegistry.find(ift.getClass());
                    if (h == null) continue;
                    h.applyCatchup(ift, ticks, level, p);
                    found++;
                }
        send(ctx, " §aSimulated §e" + ticks + "§7t §afor §f" + found + " §afurnace(s)");
        return 1;
    }

    private static int runSpawn(CommandContext<CommandSourceStack> ctx, String config, long ticks) {
        ServerPlayer player = getPlayer(ctx);
        if (player == null) return 0;
        return spawnAndMaybeSim(player, config, ticks, false);
    }

    private static int runTest(CommandContext<CommandSourceStack> ctx, String config, long ticks) {
        ServerPlayer player = getPlayer(ctx);
        if (player == null) return 0;
        return spawnAndMaybeSim(player, config, ticks, true);
    }

    // ========================================================================
    // SPAWN ENGINE
    // ========================================================================

    private static int spawnAndMaybeSim(ServerPlayer player, String config, long ticks, boolean cleanup) {
        ServerLevel level = player.serverLevel();
        BlockPos origin = player.blockPosition().offset(4, 0, 0);

        com.keepsmelting.KeepSmelting.LOGGER.info("[Test] config={} ticks={} cleanup={}", config, ticks, cleanup);

        Block ironBlock = ForgeRegistries.BLOCKS.getValue(new ResourceLocation("ironfurnaces", "iron_furnace"));
        if (ironBlock == null || ironBlock == Blocks.AIR) {
            send(player, " §cIron Furnaces не найден");
            return 0;
        }

        // Строим схему
        java.util.List<FurnacePlacement> placements = new java.util.ArrayList<>();
        boolean hasChests = config.endsWith("+chest");
        String baseConfig = hasChests ? config.replace("+chest", "") : config;

        switch (baseConfig) {
            case "basic":
                placements.add(fp(origin, "gen", 3));
                placements.add(fp(origin.south(), "fact", 2));
                if (hasChests) {
                    placements.add(fp(origin.above(), "chest", 0, Items.COAL, 64));
                    placements.add(fp(origin.south().above(), "chest", 0, Items.IRON_ORE, 64));
                    placements.add(fp(origin.south().below(), "chest_out", 0, null, 0));
                }
                break;
            case "gen":
                placements.add(fp(origin, "gen", 3));
                if (hasChests) {
                    placements.add(fp(origin.above(), "chest", 0, Items.COAL, 64));
                }
                break;
            case "factory":
                placements.add(fp(origin, "fact", 2));
                if (hasChests) {
                    placements.add(fp(origin.above(), "chest", 0, Items.IRON_ORE, 64));
                    placements.add(fp(origin.below(), "chest_out", 0, null, 0));
                }
                break;
            case "nw":
                placements.add(fp(origin, "gen", 3));
                placements.add(fp(origin.south(), "gen", 3));
                placements.add(fp(origin.south().south(), "fact", 2));
                if (hasChests) {
                    placements.add(fp(origin.above(), "chest", 0, Items.COAL, 64));
                    placements.add(fp(origin.south().above(), "chest", 0, Items.COAL, 64));
                    placements.add(fp(origin.south().south().above(), "chest", 0, Items.IRON_ORE, 64));
                    placements.add(fp(origin.south().south().below(), "chest_out", 0, null, 0));
                }
                break;
            default:
                send(player, " §cНеизвестный шаблон: " + config);
                send(player, " §7Доступны: " + String.join(", ", PATTERNS.keySet()));
                return 0;
        }

        // Сохраняем старые состояния
        for (FurnacePlacement p : placements) p.oldState = level.getBlockState(p.pos);

        // Спавним
        for (FurnacePlacement p : placements) {
            if ("chest".equals(p.type) || "chest_out".equals(p.type)) {
                spawnBarrel(level, p);
            } else {
                spawnFurnace(level, p, ironBlock);
            }
        }

        // Симуляция
        if (ticks > 0) {
            for (FurnacePlacement p : placements) {
                if ("gen".equals(p.type) || "fact".equals(p.type)) {
                    BlockEntity be = level.getBlockEntity(p.pos);
                    if (be instanceof ironfurnaces.tileentity.furnaces.BlockIronFurnaceTileBase ft) {
                        IFurnaceCatchupHandler handler = CatchupHandlerRegistry.find(ft.getClass());
                        if (handler != null) handler.applyCatchup(ft, ticks, level, ft.getBlockPos());
                    }
                }
            }
            send(player, " §aSimulated §e" + ticks + "§7t on " + placements.size() + " blocks");
        }

        // Отчёт + очистка
        if (cleanup) {
            StringBuilder sb = new StringBuilder();
            for (FurnacePlacement p : placements) {
                if ("chest".equals(p.type) || "chest_out".equals(p.type)) continue;
                BlockEntity be = level.getBlockEntity(p.pos);
                if (be instanceof ironfurnaces.tileentity.furnaces.BlockIronFurnaceTileBase ft) {
                    sb.append("§7").append("gen".equals(p.type)?"Gen":"Fact");
                    sb.append("§f").append(ft.getEnergy()).append("§7/").append(ft.getCapacity());
                    sb.append(" §7f:§f").append(ft.inventory.get(6).getCount());
                    sb.append(" §7i:§f").append(ft.inventory.get(7).getCount()).append(" | ");
                }
            }
            for (FurnacePlacement p : placements) {
                level.setBlock(p.pos, p.oldState, 3);
                level.removeBlockEntity(p.pos);
            }
            if (sb.length() > 0) send(player, " §aTest §e" + ticks + "§7t §a| " + sb.toString());
            send(player, " §7Удалено.");
        } else {
            send(player, " §aSpawned " + placements.size() + " block(s).");
        }
        return 1;
    }

    // ========================================================================
    // PLACEMENT HELPERS
    // ========================================================================

    private static FurnacePlacement fp(BlockPos pos, String type, int side) {
        return new FurnacePlacement(pos, type, side, null, 0);
    }
    private static FurnacePlacement fp(BlockPos pos, String type, int side, net.minecraft.world.item.Item item, int count) {
        return new FurnacePlacement(pos, type, side, item, count);
    }

    private static class FurnacePlacement {
        BlockPos pos; String type; int side;
        net.minecraft.world.item.Item chestItem; int chestCount;
        BlockState oldState;
        FurnacePlacement(BlockPos pos, String type, int side, net.minecraft.world.item.Item chestItem, int chestCount) {
            this.pos = pos; this.type = type; this.side = side;
            this.chestItem = chestItem; this.chestCount = chestCount;
        }
    }

    private static void spawnFurnace(ServerLevel level, FurnacePlacement p, Block ironBlock) {
        BlockEntity old = level.getBlockEntity(p.pos);
        if (old != null) old.setRemoved();
        level.setBlock(p.pos, ironBlock.defaultBlockState(), 3);

        BlockEntity be = level.getBlockEntity(p.pos);
        if (be instanceof ironfurnaces.tileentity.furnaces.BlockIronFurnaceTileBase tile) {
            // ID аугментов
            net.minecraft.world.item.Item augmentGenItem = ForgeRegistries.ITEMS.getValue(
                    new ResourceLocation("ironfurnaces", "augment_generator"));
            net.minecraft.world.item.Item augmentFactItem = ForgeRegistries.ITEMS.getValue(
                    new ResourceLocation("ironfurnaces", "augment_factory"));

            if ("gen".equals(p.type)) {
                tile.currentAugment[2] = 2; // Generator mode
                if (augmentGenItem != null) tile.inventory.set(5, new ItemStack(augmentGenItem));
                tile.inventory.set(6, new ItemStack(Items.COAL, 32));
                // Настройки сторон Generator:
                tile.furnaceSettings.set(1, 4); // UP = топливо input (barrel сверху)
                tile.furnaceSettings.set(p.side, 2); // SOUTH = RF output к Factory
                tile.furnaceSettings.set(6, 1); // auto input ON (тянет топливо)
            } else {
                tile.currentAugment[2] = 1; // Factory mode
                if (augmentFactItem != null) tile.inventory.set(5, new ItemStack(augmentFactItem));
                // Iron Furnace (tier 0): входные слоты 9 и 10; Gold (tier 1): слоты 8-11; остальные: 7-12
                int tier = tile.getTier();
                if (tier == 0) {
                    tile.inventory.set(9, new ItemStack(Items.IRON_ORE, 16));
                } else if (tier == 1) {
                    tile.inventory.set(8, new ItemStack(Items.IRON_ORE, 16));
                } else {
                    tile.inventory.set(7, new ItemStack(Items.IRON_ORE, 16));
                }
                // Настройки сторон Factory:
                tile.furnaceSettings.set(1, 1); // UP = input (barrel с рудой сверху)
                tile.furnaceSettings.set(0, 2); // DOWN = output (barrel снизу)
                tile.furnaceSettings.set(p.side, 2); // NORTH = RF input от Generator
                tile.furnaceSettings.set(6, 1); // auto input ON
                tile.furnaceSettings.set(7, 1); // auto output ON
            }
            tile.setChanged();
        }
    }

    private static void spawnBarrel(ServerLevel level, FurnacePlacement p) {
        BlockEntity old = level.getBlockEntity(p.pos);
        if (old != null) old.setRemoved();
        level.setBlock(p.pos, net.minecraft.world.level.block.Blocks.BARREL.defaultBlockState(), 3);

        BlockEntity be = level.getBlockEntity(p.pos);
        if (be instanceof net.minecraft.world.level.block.entity.BarrelBlockEntity barrel) {
            if ("chest".equals(p.type) && p.chestItem != null && p.chestCount > 0) {
                barrel.setItem(0, new ItemStack(p.chestItem, p.chestCount));
            }
            barrel.setChanged();
        }
    }

    // ========================================================================
    // UTILITY
    // ========================================================================

    private static ServerPlayer getPlayer(CommandContext<CommandSourceStack> ctx) {
        if (!(ctx.getSource().getEntity() instanceof ServerPlayer player)) {
            ctx.getSource().sendFailure(Component.literal("§cТолько для игроков"));
            return null;
        }
        if (!net.minecraftforge.fml.ModList.get().isLoaded("ironfurnaces")) {
            ctx.getSource().sendFailure(Component.literal("§cIron Furnaces не установлен"));
            return null;
        }
        return player;
    }

    private static void send(CommandContext<CommandSourceStack> ctx, String msg) {
        ctx.getSource().sendSuccess(() -> Component.literal("§7[§6KeepSmelting§7]" + msg), false);
    }

    private static void send(ServerPlayer player, String msg) {
        player.sendSystemMessage(Component.literal("§7[§6KeepSmelting§7]" + msg));
    }

    private static void sendHelp(CommandContext<CommandSourceStack> ctx, String msg) {
        ctx.getSource().sendSuccess(() -> Component.literal(msg), false);
    }
}
