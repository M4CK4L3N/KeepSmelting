package com.keepsmelting.command;

import com.keepsmelting.KeepSmelting;
import com.keepsmelting.KeepSmeltingConfig;
import com.keepsmelting.KeepSmeltingConfig.DebugMode;
import com.keepsmelting.api.CatchupHandlerRegistry;
import com.keepsmelting.api.IFurnaceCatchupHandler;
import com.keepsmelting.internal.catchup.VanillaCatchupHandler;
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
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.*;

/**
 * Центральная команда KeepSmelting.
 * Не имеет прямых ссылок на IF-классы — вся IF-логика вызывается через рефлексию.
 */
public class KeepSmeltingCommand {

    private static final SuggestionProvider<CommandSourceStack> DEBUG_SUGGESTIONS =
            (ctx, builder) -> SharedSuggestionProvider.suggest(
                    new String[]{"OFF", "CHAT", "LOG"}, builder);

    private static final SuggestionProvider<CommandSourceStack> TIME_SUGGESTIONS =
            (ctx, builder) -> SharedSuggestionProvider.suggest(
                    new String[]{"REALTIME", "GAMETIME"}, builder);

    /** Паттерны для IF (строки, без классов) */
    private static final Set<String> IF_PATTERNS = Set.of("basic", "gen", "factory", "nw");

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        // Собираем suggestions: ванильные + IF (если загружен)
        Set<String> allPatterns = new LinkedHashSet<>(VanillaTestPatterns.PATTERNS.keySet());
        if (ModList.get().isLoaded("ironfurnaces")) {
            allPatterns.addAll(IF_PATTERNS);
        }
        String[] patternArr = allPatterns.toArray(new String[0]);
        SuggestionProvider<CommandSourceStack> configSuggestions =
                (ctx, builder) -> SharedSuggestionProvider.suggest(patternArr, builder);

        var cmd = Commands.literal("keepsmelting")
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

                .then(Commands.literal("status")
                        .executes(KeepSmeltingCommand::showStatus))

                .then(Commands.literal("simulate")
                        .then(Commands.argument("ticks", LongArgumentType.longArg(1, 192000))
                                .executes(ctx -> runSimulate(ctx,
                                        LongArgumentType.getLong(ctx, "ticks")))))

                .then(Commands.literal("test")
                        .then(Commands.argument("config", StringArgumentType.word())
                                .suggests(configSuggestions)
                                .executes(ctx -> runTestSpawn(ctx,
                                        StringArgumentType.getString(ctx, "config")))
                                .then(Commands.argument("ticks", LongArgumentType.longArg(1, 192000))
                                        .executes(ctx -> runTest(ctx,
                                                StringArgumentType.getString(ctx, "config"),
                                                LongArgumentType.getLong(ctx, "ticks"),
                                                true))
                                        .then(Commands.literal("keep")
                                                .executes(ctx -> runTest(ctx,
                                                        StringArgumentType.getString(ctx, "config"),
                                                        LongArgumentType.getLong(ctx, "ticks"),
                                                        false))))))

                .executes(KeepSmeltingCommand::showHelp);

        dispatcher.register(cmd);
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
        sendHelp(ctx, " /keepsmelting status — текущие настройки");
        sendHelp(ctx, " /keepsmelting catchup|debug|time|maxTicks|minDelta <val> — настройки");
        sendHelp(ctx, " /keepsmelting simulate <ticks> — симуляция догонялки для всех печей рядом");
        sendHelp(ctx, " /keepsmelting test <config> — спавн тестовой схемы");
        sendHelp(ctx, " /keepsmelting test <config> <ticks> — спавн + симуляция + отчёт + очистка");
        sendHelp(ctx, " /keepsmelting test <config> <ticks> keep — спавн + симуляция + отчёт (оставить)");
        sendHelp(ctx, " §7Паттерны: " + getPatternsHelp());
        return 1;
    }

    // ========================================================================
    // SIMULATE — универсальный, без IF-зависимостей
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
                    if (!(be instanceof AbstractFurnaceBlockEntity)) continue;
                    IFurnaceCatchupHandler h = CatchupHandlerRegistry.find(be.getClass());
                    if (h != null) {
                        h.applyCatchup(be, ticks, level, p);
                    } else {
                        VanillaCatchupHandler.INSTANCE.applyCatchup(be, ticks, level, p);
                    }
                    found++;
                }
        send(ctx, " §aSimulated §e" + ticks + "§7t §afor §f" + found + " §afurnace(s)");
        return 1;
    }

    // ========================================================================
    // TEST
    // ========================================================================

    /** Проверка, является ли конфиг IF-паттерном (без загрузки IF классов) */
    private static boolean isIFPattern(String config) {
        String base = config.endsWith("+chest") ? config.replace("+chest", "") : config;
        return IF_PATTERNS.contains(base);
    }

    private static int runTestSpawn(CommandContext<CommandSourceStack> ctx, String config) {
        ServerPlayer player = getPlayer(ctx);
        if (player == null) return 0;

        ServerLevel level = player.serverLevel();
        BlockPos origin = player.blockPosition().offset(4, 0, 0);

        List<VanillaTestPatterns.FurnacePlacement> placements = buildPlacements(config, origin, level);
        if (placements == null) return 0;

        spawnPlacements(level, placements, config);
        send(player, " §aSpawned " + placements.size() + " block(s).");
        return 1;
    }

    private static int runTest(CommandContext<CommandSourceStack> ctx, String config, long ticks, boolean cleanup) {
        ServerPlayer player = getPlayer(ctx);
        if (player == null) return 0;

        boolean hasIF = ModList.get().isLoaded("ironfurnaces");

        // Проверка: IF паттерн требует IF мод
        if (isIFPattern(config) && !hasIF) {
            send(player, " §cIron Furnaces required for config: " + config);
            return 0;
        }

        ServerLevel level = player.serverLevel();
        BlockPos origin = player.blockPosition().offset(4, 0, 0);

        KeepSmelting.LOGGER.info("[Test] config={} ticks={} cleanup={} hasIF={}", config, ticks, cleanup, hasIF);

        // Строим схему
        List<VanillaTestPatterns.FurnacePlacement> placements = buildPlacements(config, origin, level);
        if (placements == null) return 0;

        // Сохраняем старые состояния
        for (VanillaTestPatterns.FurnacePlacement p : placements) {
            p.oldState = level.getBlockState(p.pos);
        }

        // Спавним
        spawnPlacements(level, placements, config);

        // Симуляция
        if (ticks > 0) {
            for (VanillaTestPatterns.FurnacePlacement p : placements) {
                if (isChestType(p.type)) continue;
                BlockEntity be = level.getBlockEntity(p.pos);
                if (be == null) continue;
                IFurnaceCatchupHandler h = CatchupHandlerRegistry.find(be.getClass());
                if (h != null) {
                    h.applyCatchup(be, ticks, level, p.pos);
                } else {
                    VanillaCatchupHandler.INSTANCE.applyCatchup(be, ticks, level, p.pos);
                }
            }
            send(player, " §aSimulated §e" + ticks + "§7t on " + placements.size() + " blocks");
        }

        // Отчёт
        if (ticks > 0) {
            StringBuilder sb = new StringBuilder();
            for (VanillaTestPatterns.FurnacePlacement p : placements) {
                if (isChestType(p.type)) continue;
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
            if (sb.length() > 0) send(player, " §aTest §e" + ticks + "§7t §a| " + sb.toString());
        }

        // Очистка
        if (ticks > 0 && cleanup) {
            for (VanillaTestPatterns.FurnacePlacement p : placements) {
                level.setBlock(p.pos, p.oldState, 3);
                level.removeBlockEntity(p.pos);
            }
            send(player, " §7Очищено.");
        } else if (ticks > 0 && !cleanup) {
            send(player, " §7Блоки оставлены.");
        }
        return 1;
    }

    // ========================================================================
    // PLACEMENT BUILDERS
    // ========================================================================

    private static List<VanillaTestPatterns.FurnacePlacement> buildPlacements(
            String config, BlockPos origin, ServerLevel level) {
        if (VanillaTestPatterns.isVanillaPattern(config)) {
            return VanillaTestPatterns.build(config, origin);
        }
        if (isIFPattern(config)) {
            return buildIFPlacements(config, origin);
        }
        return null;
    }

    /** Построение IF-схемы (строки, без IF-классов) */
    private static List<VanillaTestPatterns.FurnacePlacement> buildIFPlacements(String config, BlockPos origin) {
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

    private static void spawnPlacements(ServerLevel level, List<VanillaTestPatterns.FurnacePlacement> placements, String config) {
        boolean isVanilla = VanillaTestPatterns.isVanillaPattern(config);

        for (VanillaTestPatterns.FurnacePlacement p : placements) {
            if (isChestType(p.type)) {
                VanillaTestPatterns.spawnFurnace(level, p, config);
            } else if (isVanilla) {
                VanillaTestPatterns.spawnFurnace(level, p, config);
            } else {
                // IF spawn
                Block ironBlock = ForgeRegistries.BLOCKS.getValue(
                        new ResourceLocation("ironfurnaces", "iron_furnace"));
                if (ironBlock == null || ironBlock == Blocks.AIR) continue;

                BlockEntity old = level.getBlockEntity(p.pos);
                if (old != null) old.setRemoved();
                level.setBlock(p.pos, ironBlock.defaultBlockState(), 3);

                // Всё остальное делает mixin — печь получает тик, IFTestPatterns не нужен
                // TODO: заполнение IF-настроек через рефлексию
            }
        }
    }

    // ========================================================================
    // UTILITY
    // ========================================================================

    private static boolean isChestType(String type) {
        return "chest".equals(type) || "chest_out".equals(type) || "chest_fuel".equals(type)
                || "chest_input".equals(type)
                || "hopper_in".equals(type) || "hopper_out".equals(type)
                || "hopper_into_top".equals(type) || "hopper_into_side".equals(type)
                || "hopper_out_of_bottom".equals(type);
    }

    /** С IF-side (для gen/fact) */
    private static VanillaTestPatterns.FurnacePlacement fp(BlockPos pos, String type, int side) {
        return new VanillaTestPatterns.FurnacePlacement(pos, type, net.minecraft.world.item.Items.AIR, 0, side);
    }

    /** С предметом и количеством (для chest) */
    private static VanillaTestPatterns.FurnacePlacement fp(BlockPos pos, String type, net.minecraft.world.item.Item item, int count) {
        return new VanillaTestPatterns.FurnacePlacement(pos, type, item, count, 0);
    }

    private static ServerPlayer getPlayer(CommandContext<CommandSourceStack> ctx) {
        if (!(ctx.getSource().getEntity() instanceof ServerPlayer player)) {
            ctx.getSource().sendFailure(Component.literal("§cТолько для игроков"));
            return null;
        }
        return player;
    }

    private static String getPatternsHelp() {
        StringBuilder sb = new StringBuilder();
        for (String k : VanillaTestPatterns.PATTERNS.keySet()) {
            sb.append("§e").append(k).append("§7, ");
        }
        if (ModList.get().isLoaded("ironfurnaces")) {
            for (String k : IF_PATTERNS) {
                sb.append("§e").append(k).append("§7, ");
            }
        }
        String result = sb.toString();
        return result.isEmpty() ? "" : result.substring(0, result.length() - 2);
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
