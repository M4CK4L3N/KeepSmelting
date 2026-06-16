package com.example.examplemod.command;

import com.example.examplemod.KeepSmeltingConfig;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;

public class KeepSmeltingCommand {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("keepsmelting")
                .requires(src -> src.hasPermission(2))

                // /keepsmelting chatdebug <true|false>
                .then(Commands.literal("chatdebug")
                        .then(Commands.argument("value", BoolArgumentType.bool())
                                .executes(ctx -> setChatDebug(ctx, BoolArgumentType.getBool(ctx, "value")))))

                // /keepsmelting status
                .then(Commands.literal("status")
                        .executes(KeepSmeltingCommand::showStatus))

                // /keepsmelting — show help
                .executes(KeepSmeltingCommand::showHelp)
        );
    }

    private static int setChatDebug(CommandContext<CommandSourceStack> ctx, boolean value) {
        KeepSmeltingConfig.COMMON.chatDebug.set(value);
        KeepSmeltingConfig.COMMON.chatDebug.save();
        ctx.getSource().sendSuccess(() ->
                Component.literal(String.format("§7[§6KeepSmelting§7] chatDebug §a→ §f%b", value)),
                true);
        return 1;
    }

    private static int showStatus(CommandContext<CommandSourceStack> ctx) {
        boolean enabled = KeepSmeltingConfig.COMMON.catchupEnabled.get();
        long maxTicks = KeepSmeltingConfig.COMMON.maxCatchupTicks.get();
        int minDelta = KeepSmeltingConfig.COMMON.minDeltaThreshold.get();
        boolean debug = KeepSmeltingConfig.COMMON.chatDebug.get();

        ctx.getSource().sendSuccess(() ->
                Component.literal(String.format(
                        "§7[§6KeepSmelting§7] §fcatchup=%s §7| §emaxTicks=%d §7| §fminDelta=%d §7| §fchatDebug=%s",
                        enabled, maxTicks, minDelta, debug)),
                false);
        return 1;
    }

    private static int showHelp(CommandContext<CommandSourceStack> ctx) {
        ctx.getSource().sendSuccess(() ->
                Component.literal("§7[§6KeepSmelting§7] §e/keepsmelting status §7— show settings"),
                false);
        ctx.getSource().sendSuccess(() ->
                Component.literal("§7[§6KeepSmelting§7] §e/keepsmelting chatdebug <true|false> §7— toggle debug chat"),
                false);
        return 1;
    }
}
