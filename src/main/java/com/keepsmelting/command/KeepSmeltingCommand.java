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

                .then(Commands.literal("status")
                        .executes(KeepSmeltingCommand::showStatus))

                .executes(KeepSmeltingCommand::showHelp)
        );
    }

    private static int setCatchup(CommandContext<CommandSourceStack> ctx, boolean value) {
        KeepSmeltingConfig.COMMON.catchupEnabled.set(value);
        KeepSmeltingConfig.COMMON.catchupEnabled.save();
        ctx.getSource().sendSuccess(() ->
                Component.translatable("command.keepsmelting.prefix").append(" catchup §a→ §f" + value),
                true);
        return 1;
    }

    private static int setMaxTicks(CommandContext<CommandSourceStack> ctx, long value) {
        KeepSmeltingConfig.COMMON.maxCatchupTicks.set(value);
        KeepSmeltingConfig.COMMON.maxCatchupTicks.save();
        ctx.getSource().sendSuccess(() ->
                Component.translatable("command.keepsmelting.prefix").append(" maxTicks §a→ §f" + value),
                true);
        return 1;
    }

    private static int setMinDelta(CommandContext<CommandSourceStack> ctx, int value) {
        KeepSmeltingConfig.COMMON.minDeltaThreshold.set(value);
        KeepSmeltingConfig.COMMON.minDeltaThreshold.save();
        ctx.getSource().sendSuccess(() ->
                Component.translatable("command.keepsmelting.prefix").append(" minDelta §a→ §f" + value),
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
                    Component.translatable("command.keepsmelting.prefix").append(" §c").append(Component.translatable("command.keepsmelting.debug.error.invalid")));
            return 0;
        }
        KeepSmeltingConfig.COMMON.debugMode.set(dm);
        KeepSmeltingConfig.COMMON.debugMode.save();
        ctx.getSource().sendSuccess(() ->
                Component.translatable("command.keepsmelting.prefix").append(" debug §a→ §f" + dm),
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
                    Component.translatable("command.keepsmelting.prefix").append(" §c").append(Component.translatable("command.keepsmelting.time.error.invalid")));
            return 0;
        }
        KeepSmeltingConfig.COMMON.timeMode.set(tm);
        KeepSmeltingConfig.COMMON.timeMode.save();
        ctx.getSource().sendSuccess(() ->
                Component.translatable("command.keepsmelting.prefix").append(" time §a→ §f" + tm),
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
                Component.translatable("command.keepsmelting.prefix")
                        .append(" §f")
                        .append(Component.translatable("command.keepsmelting.status", enabled, maxTicks, minDelta, dm, tm)),
                false);
        return 1;
    }

    private static int showHelp(CommandContext<CommandSourceStack> ctx) {
        ctx.getSource().sendSuccess(() ->
                Component.translatable("command.keepsmelting.prefix")
                        .append(" §e").append(Component.translatable("command.keepsmelting.help.status")),
                false);
        ctx.getSource().sendSuccess(() ->
                Component.translatable("command.keepsmelting.prefix")
                        .append(" §e").append(Component.translatable("command.keepsmelting.help.catchup")),
                false);
        ctx.getSource().sendSuccess(() ->
                Component.translatable("command.keepsmelting.prefix")
                        .append(" §e").append(Component.translatable("command.keepsmelting.help.maxTicks")),
                false);
        ctx.getSource().sendSuccess(() ->
                Component.translatable("command.keepsmelting.prefix")
                        .append(" §e").append(Component.translatable("command.keepsmelting.help.minDelta")),
                false);
        ctx.getSource().sendSuccess(() ->
                Component.translatable("command.keepsmelting.prefix")
                        .append(" §e").append(Component.translatable("command.keepsmelting.help.debug")),
                false);
        ctx.getSource().sendSuccess(() ->
                Component.translatable("command.keepsmelting.prefix")
                        .append(" §e").append(Component.translatable("command.keepsmelting.help.time")),
                false);
        return 1;
    }
}
