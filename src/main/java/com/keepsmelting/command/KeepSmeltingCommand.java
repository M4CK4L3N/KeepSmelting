package com.keepsmelting.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.LongArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraftforge.fml.ModList;

import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Router for all /keepsmelting subcommands.
 * Delegates to:
 * <ul>
 *   <li>{@link ConfigSubcommand} — catchup|debug|time|maxTicks|minDelta|status</li>
 *   <li>{@link SimulateSubcommand} — simulate</li>
 *   <li>{@link TestSubcommand} — test</li>
 * </ul>
 */
public class KeepSmeltingCommand {

    private static final SuggestionProvider<CommandSourceStack> DEBUG_SUGGESTIONS =
            (ctx, builder) -> SharedSuggestionProvider.suggest(
                    new String[]{"OFF", "CHAT", "LOG"}, builder);

    private static final SuggestionProvider<CommandSourceStack> TIME_SUGGESTIONS =
            (ctx, builder) -> SharedSuggestionProvider.suggest(
                    new String[]{"REALTIME", "GAMETIME"}, builder);

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        // Собираем suggestions: ванильные + IF (если загружен)
        Set<String> allPatterns = new LinkedHashSet<>(VanillaTestPatterns.PATTERNS.keySet());
        if (ModList.get().isLoaded("ironfurnaces")) {
            allPatterns.addAll(TestSubcommand.IF_PATTERNS);
        }
        String[] patternArr = allPatterns.toArray(new String[0]);
        SuggestionProvider<CommandSourceStack> configSuggestions =
                (ctx, builder) -> SharedSuggestionProvider.suggest(patternArr, builder);

        var cmd = Commands.literal("keepsmelting")
                .requires(src -> src.hasPermission(2))

                .then(Commands.literal("catchup")
                        .then(Commands.argument("value", BoolArgumentType.bool())
                                .executes(ctx -> ConfigSubcommand.setCatchup(ctx, BoolArgumentType.getBool(ctx, "value")))))

                .then(Commands.literal("debug")
                        .then(Commands.argument("mode", StringArgumentType.word())
                                .suggests(DEBUG_SUGGESTIONS)
                                .executes(ctx -> ConfigSubcommand.setDebugMode(ctx, StringArgumentType.getString(ctx, "mode")))))

                .then(Commands.literal("time")
                        .then(Commands.argument("mode", StringArgumentType.word())
                                .suggests(TIME_SUGGESTIONS)
                                .executes(ctx -> ConfigSubcommand.setTimeMode(ctx, StringArgumentType.getString(ctx, "mode")))))

                .then(Commands.literal("maxTicks")
                        .then(Commands.argument("value", LongArgumentType.longArg(1, 192000))
                                .executes(ctx -> ConfigSubcommand.setMaxTicks(ctx, LongArgumentType.getLong(ctx, "value")))))

                .then(Commands.literal("minDelta")
                        .then(Commands.argument("value", IntegerArgumentType.integer(1, 72000))
                                .executes(ctx -> ConfigSubcommand.setMinDelta(ctx, IntegerArgumentType.getInteger(ctx, "value")))))

                .then(Commands.literal("status")
                        .executes(ConfigSubcommand::showStatus))

                .then(Commands.literal("simulate")
                        .then(Commands.argument("ticks", LongArgumentType.longArg(1, 192000))
                                .executes(ctx -> SimulateSubcommand.run(ctx,
                                        LongArgumentType.getLong(ctx, "ticks")))))

                .then(Commands.literal("test")
                        .then(Commands.argument("config", StringArgumentType.word())
                                .suggests(configSuggestions)
                                .executes(ctx -> TestSubcommand.runSpawn(ctx,
                                        StringArgumentType.getString(ctx, "config")))
                                .then(Commands.argument("ticks", LongArgumentType.longArg(1, 192000))
                                        .executes(ctx -> TestSubcommand.runTest(ctx,
                                                StringArgumentType.getString(ctx, "config"),
                                                LongArgumentType.getLong(ctx, "ticks"),
                                                true))
                                        .then(Commands.literal("keep")
                                                .executes(ctx -> TestSubcommand.runTest(ctx,
                                                        StringArgumentType.getString(ctx, "config"),
                                                        LongArgumentType.getLong(ctx, "ticks"),
                                                        false))))))

                .executes(KeepSmeltingCommand::showHelp);

        dispatcher.register(cmd);
    }

    private static int showHelp(CommandContext<CommandSourceStack> ctx) {
        CommandUtil.sendHelp(ctx, " §7=== KeepSmelting Commands ===");
        CommandUtil.sendHelp(ctx, " /keepsmelting status — текущие настройки");
        CommandUtil.sendHelp(ctx, " /keepsmelting catchup|debug|time|maxTicks|minDelta <val> — настройки");
        CommandUtil.sendHelp(ctx, " /keepsmelting simulate <ticks> — симуляция догонялки для всех печей рядом");
        CommandUtil.sendHelp(ctx, " /keepsmelting test <config> — спавн тестовой схемы");
        CommandUtil.sendHelp(ctx, " /keepsmelting test <config> <ticks> — спавн + симуляция + отчёт + очистка");
        CommandUtil.sendHelp(ctx, " /keepsmelting test <config> <ticks> keep — спавн + симуляция + отчёт (оставить)");
        CommandUtil.sendHelp(ctx, " §7Паттерны: " + CommandUtil.getPatternsHelp());
        return 1;
    }
}
