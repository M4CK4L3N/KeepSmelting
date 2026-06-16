package com.example.examplemod.command;

import com.example.examplemod.KeepSmeltingConfig;
import com.example.examplemod.KeepSmeltingConfig.DebugMode;
import com.mojang.brigadier.CommandDispatcher;
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

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("keepsmelting")
                .requires(src -> src.hasPermission(2))

                // /keepsmelting debug <OFF|CHAT|LOG>
                .then(Commands.literal("debug")
                        .then(Commands.argument("mode", StringArgumentType.word())
                                .suggests(DEBUG_SUGGESTIONS)
                                .executes(ctx -> setDebugMode(ctx, StringArgumentType.getString(ctx, "mode")))))

                // /keepsmelting status
                .then(Commands.literal("status")
                        .executes(KeepSmeltingCommand::showStatus))

                // /keepsmelting — show help
                .executes(KeepSmeltingCommand::showHelp)
        );
    }

    private static int setDebugMode(CommandContext<CommandSourceStack> ctx, String mode) {
        String upper = mode.toUpperCase();
        DebugMode dm;
        try {
            dm = DebugMode.valueOf(upper);
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

    private static int showStatus(CommandContext<CommandSourceStack> ctx) {
        boolean enabled = KeepSmeltingConfig.COMMON.catchupEnabled.get();
        long maxTicks = KeepSmeltingConfig.COMMON.maxCatchupTicks.get();
        int minDelta = KeepSmeltingConfig.COMMON.minDeltaThreshold.get();
        DebugMode dm = KeepSmeltingConfig.COMMON.debugMode.get();

        ctx.getSource().sendSuccess(() ->
                Component.literal(String.format(
                        "§7[§6KeepSmelting§7] §fcatchup=%s §7| §emaxTicks=%d §7| §fminDelta=%d §7| §fdebug=%s",
                        enabled, maxTicks, minDelta, dm)),
                false);
        return 1;
    }

    private static int showHelp(CommandContext<CommandSourceStack> ctx) {
        ctx.getSource().sendSuccess(() ->
                Component.literal("§7[§6KeepSmelting§7] §e/keepsmelting status §7— show settings"),
                false);
        ctx.getSource().sendSuccess(() ->
                Component.literal("§7[§6KeepSmelting§7] §e/keepsmelting debug <OFF|CHAT|LOG> §7— set debug output mode"),
                false);
        return 1;
    }
}
