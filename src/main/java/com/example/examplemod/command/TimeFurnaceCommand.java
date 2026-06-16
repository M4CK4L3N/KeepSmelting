package com.example.examplemod.command;

import com.example.examplemod.TimeFurnaceConfig;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;

public class TimeFurnaceCommand {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("timefurnace")
                .requires(src -> src.hasPermission(2))

                // /timefurnace chatdebug <true|false>
                .then(Commands.literal("chatdebug")
                        .then(Commands.argument("value", BoolArgumentType.bool())
                                .executes(ctx -> setChatDebug(ctx, BoolArgumentType.getBool(ctx, "value")))))

                // /timefurnace status
                .then(Commands.literal("status")
                        .executes(TimeFurnaceCommand::showStatus))

                // /timefurnace — show help
                .executes(TimeFurnaceCommand::showHelp)
        );
    }

    private static int setChatDebug(CommandContext<CommandSourceStack> ctx, boolean value) {
        TimeFurnaceConfig.COMMON.chatDebug.set(value);
        TimeFurnaceConfig.COMMON.chatDebug.save();
        ctx.getSource().sendSuccess(() ->
                Component.literal(String.format("§7[§6TimeFurnace§7] chatDebug §a→ §f%b", value)),
                true);
        return 1;
    }

    private static int showStatus(CommandContext<CommandSourceStack> ctx) {
        boolean enabled = TimeFurnaceConfig.COMMON.catchupEnabled.get();
        long maxTicks = TimeFurnaceConfig.COMMON.maxCatchupTicks.get();
        int minDelta = TimeFurnaceConfig.COMMON.minDeltaThreshold.get();
        boolean debug = TimeFurnaceConfig.COMMON.chatDebug.get();

        ctx.getSource().sendSuccess(() ->
                Component.literal(String.format(
                        "§7[§6TimeFurnace§7] §fcatchup=%s §7| §emaxTicks=%d §7| §fminDelta=%d §7| §fchatDebug=%s",
                        enabled, maxTicks, minDelta, debug)),
                false);
        return 1;
    }

    private static int showHelp(CommandContext<CommandSourceStack> ctx) {
        ctx.getSource().sendSuccess(() ->
                Component.literal("§7[§6TimeFurnace§7] §e/timefurnace status §7— show settings"),
                false);
        ctx.getSource().sendSuccess(() ->
                Component.literal("§7[§6TimeFurnace§7] §e/timefurnace chatdebug <true|false> §7— toggle debug chat"),
                false);
        return 1;
    }
}
