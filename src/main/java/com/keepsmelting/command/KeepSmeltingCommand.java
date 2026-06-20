package com.keepsmelting.command;

import com.keepsmelting.KeepSmeltingConfig;
import com.keepsmelting.KeepSmeltingConfig.DebugMode;
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
        sendHelp(ctx, " §7(Установите Iron Furnaces для simulate/spawn/test команд)");
        return 1;
    }

    private static void send(CommandContext<CommandSourceStack> ctx, String msg) {
        ctx.getSource().sendSuccess(() -> Component.literal("§7[§6KeepSmelting§7]" + msg), false);
    }

    private static void sendHelp(CommandContext<CommandSourceStack> ctx, String msg) {
        ctx.getSource().sendSuccess(() -> Component.literal(msg), false);
    }
}
