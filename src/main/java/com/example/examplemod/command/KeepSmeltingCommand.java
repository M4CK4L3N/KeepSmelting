package com.example.examplemod.command;

import com.example.examplemod.KeepSmeltingConfig;
import com.example.examplemod.KeepSmeltingConfig.DebugMode;
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
import net.minecraft.network.chat.Component;

public class KeepSmeltingCommand {

    private static final SuggestionProvider<CommandSourceStack> DEBUG_SUGGESTIONS =
            (ctx, builder) -> SharedSuggestionProvider.suggest(
                    new String[]{"OFF", "CHAT", "LOG"}, builder);

    private static final SuggestionProvider<CommandSourceStack> TIME_SUGGESTIONS =
            (ctx, builder) -> SharedSuggestionProvider.suggest(
                    new String[]{"REALTIME", "GAMETIME"}, builder);

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("keepsmelting")
                .requires(src -> src.hasPermission(2))

                // /keepsmelting catchup <true|false>
                .then(Commands.literal("catchup")
                        .then(Commands.argument("value", BoolArgumentType.bool())
                                .executes(ctx -> setCatchup(ctx, BoolArgumentType.getBool(ctx, "value")))))

                // /keepsmelting debug <OFF|CHAT|LOG>
                .then(Commands.literal("debug")
                        .then(Commands.argument("mode", StringArgumentType.word())
                                .suggests(DEBUG_SUGGESTIONS)
                                .executes(ctx -> setDebugMode(ctx, StringArgumentType.getString(ctx, "mode")))))

                // /keepsmelting time <REALTIME|GAMETIME>
                .then(Commands.literal("time")
                        .then(Commands.argument("mode", StringArgumentType.word())
                                .suggests(TIME_SUGGESTIONS)
                                .executes(ctx -> setTimeMode(ctx, StringArgumentType.getString(ctx, "mode")))))

                // /keepsmelting maxTicks <1-192000>
                .then(Commands.literal("maxTicks")
                        .then(Commands.argument("value", LongArgumentType.longArg(1, 192000))
                                .executes(ctx -> setMaxTicks(ctx, LongArgumentType.getLong(ctx, "value")))))

                // /keepsmelting minDelta <1-72000>
                .then(Commands.literal("minDelta")
                        .then(Commands.argument("value", IntegerArgumentType.integer(1, 72000))
                                .executes(ctx -> setMinDelta(ctx, IntegerArgumentType.getInteger(ctx, "value")))))

                // /keepsmelting status
                .then(Commands.literal("status")
                        .executes(KeepSmeltingCommand::showStatus))

                // /keepsmelting — show help
                .executes(KeepSmeltingCommand::showHelp)
        );
    }

    private static int setCatchup(CommandContext<CommandSourceStack> ctx, boolean value) {
        KeepSmeltingConfig.COMMON.catchupEnabled.set(value);
        KeepSmeltingConfig.COMMON.catchupEnabled.save();
        ctx.getSource().sendSuccess(() ->
                Component.literal(String.format("§7[§6KeepSmelting§7] catchup §a→ §f%b", value)),
                true);
        return 1;
    }

    private static int setMaxTicks(CommandContext<CommandSourceStack> ctx, long value) {
        KeepSmeltingConfig.COMMON.maxCatchupTicks.set(value);
        KeepSmeltingConfig.COMMON.maxCatchupTicks.save();
        ctx.getSource().sendSuccess(() ->
                Component.literal(String.format("§7[§6KeepSmelting§7] maxTicks §a→ §f%d", value)),
                true);
        return 1;
    }

    private static int setMinDelta(CommandContext<CommandSourceStack> ctx, int value) {
        KeepSmeltingConfig.COMMON.minDeltaThreshold.set(value);
        KeepSmeltingConfig.COMMON.minDeltaThreshold.save();
        ctx.getSource().sendSuccess(() ->
                Component.literal(String.format("§7[§6KeepSmelting§7] minDelta §a→ §f%d", value)),
                true);
        return 1;
    }

    private static int setDebugMode(CommandContext<CommandSourceStack> ctx, String mode) {
        String upper = mode.toUpperCase();
        KeepSmeltingConfig.DebugMode dm;
        try {
            dm = KeepSmeltingConfig.DebugMode.valueOf(upper);
        } catch (IllegalArgumentException e) {
            ctx.getSource().sendFailure(
                    Component.literal("§7[§6KeepSmelting§7] §cInvalid mode. Use: OFF, CHAT, or LOG"));
            return 0;
        }
        KeepSmeltingConfig.COMMON.debugMode.set(dm);
        KeepSmeltingConfig.COMMON.debugMode.save();
        ctx.getSource().sendSuccess(() ->
                Component.literal(String.format("§7[§6KeepSmelting§7] debug §a→ §f%s", dm)),
                true);
        return 1;
    }

    private static int setTimeMode(CommandContext<CommandSourceStack> ctx, String mode) {
        String upper = mode.toUpperCase();
        KeepSmeltingConfig.TimeMode tm;
        try {
            tm = KeepSmeltingConfig.TimeMode.valueOf(upper);
        } catch (IllegalArgumentException e) {
            ctx.getSource().sendFailure(
                    Component.literal("§7[§6KeepSmelting§7] §cInvalid mode. Use: REALTIME or GAMETIME"));
            return 0;
        }
        KeepSmeltingConfig.COMMON.timeMode.set(tm);
        KeepSmeltingConfig.COMMON.timeMode.save();
        ctx.getSource().sendSuccess(() ->
                Component.literal(String.format("§7[§6KeepSmelting§7] time §a→ §f%s", tm)),
                true);
        return 1;
    }

    private static int showStatus(CommandContext<CommandSourceStack> ctx) {
        boolean enabled = KeepSmeltingConfig.COMMON.catchupEnabled.get();
        long maxTicks = KeepSmeltingConfig.COMMON.maxCatchupTicks.get();
        int minDelta = KeepSmeltingConfig.COMMON.minDeltaThreshold.get();
        KeepSmeltingConfig.DebugMode dm = KeepSmeltingConfig.COMMON.debugMode.get();
        KeepSmeltingConfig.TimeMode tm = KeepSmeltingConfig.COMMON.timeMode.get();

        ctx.getSource().sendSuccess(() ->
                Component.literal(String.format(
                        "§7[§6KeepSmelting§7] §fcatchup=%s §7| §emaxTicks=%d §7| §fminDelta=%d §7| §fdebug=%s §7| §ftime=%s",
                        enabled, maxTicks, minDelta, dm, tm)),
                false);
        return 1;
    }

    private static int showHelp(CommandContext<CommandSourceStack> ctx) {
        ctx.getSource().sendSuccess(() ->
                Component.literal("§7[§6KeepSmelting§7] §e/keepsmelting status §7— show settings"),
                false);
        ctx.getSource().sendSuccess(() ->
                Component.literal("§7[§6KeepSmelting§7] §e/keepsmelting catchup <true|false> §7— toggle catchup"),
                false);
        ctx.getSource().sendSuccess(() ->
                Component.literal("§7[§6KeepSmelting§7] §e/keepsmelting maxTicks <1-192000> §7— max offline ticks"),
                false);
        ctx.getSource().sendSuccess(() ->
                Component.literal("§7[§6KeepSmelting§7] §e/keepsmelting minDelta <1-72000> §7— min tick gap to trigger"),
                false);
        ctx.getSource().sendSuccess(() ->
                Component.literal("§7[§6KeepSmelting§7] §e/keepsmelting debug <OFF|CHAT|LOG> §7— debug output mode"),
                false);
        ctx.getSource().sendSuccess(() ->
                Component.literal("§7[§6KeepSmelting§7] §e/keepsmelting time <REALTIME|GAMETIME> §7— time tracking mode"),
                false);
        return 1;
    }
}
