package com.keepsmelting.command;

import com.keepsmelting.KeepSmeltingConfig;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;

/**
 * /keepsmelting catchup|debug|time|maxTicks|minDelta|status
 */
public class ConfigSubcommand {

    public static int setCatchup(CommandContext<CommandSourceStack> ctx, boolean value) {
        KeepSmeltingConfig.COMMON.catchupEnabled.set(value);
        KeepSmeltingConfig.COMMON.catchupEnabled.save();
        CommandUtil.send(ctx, " catchup §a→ §f" + value);
        return 1;
    }

    public static int setMaxTicks(CommandContext<CommandSourceStack> ctx, long value) {
        KeepSmeltingConfig.COMMON.maxCatchupTicks.set(value);
        KeepSmeltingConfig.COMMON.maxCatchupTicks.save();
        CommandUtil.send(ctx, " maxTicks §a→ §f" + value);
        return 1;
    }

    public static int setMinDelta(CommandContext<CommandSourceStack> ctx, int value) {
        KeepSmeltingConfig.COMMON.minDeltaThreshold.set(value);
        KeepSmeltingConfig.COMMON.minDeltaThreshold.save();
        CommandUtil.send(ctx, " minDelta §a→ §f" + value);
        return 1;
    }

    public static int setDebugMode(CommandContext<CommandSourceStack> ctx, String mode) {
        try {
            KeepSmeltingConfig.DebugMode dm = KeepSmeltingConfig.DebugMode.valueOf(mode.toUpperCase());
            KeepSmeltingConfig.COMMON.debugMode.set(dm);
            KeepSmeltingConfig.COMMON.debugMode.save();
            CommandUtil.send(ctx, " debug §a→ §f" + dm);
            return 1;
        } catch (IllegalArgumentException e) {
            ctx.getSource().sendFailure(Component.translatable("command.keepsmelting.prefix")
                    .append(" §c").append(Component.translatable("command.keepsmelting.debug.error.invalid")));
            return 0;
        }
    }

    public static int setTimeMode(CommandContext<CommandSourceStack> ctx, String mode) {
        try {
            KeepSmeltingConfig.TimeMode tm = KeepSmeltingConfig.TimeMode.valueOf(mode.toUpperCase());
            KeepSmeltingConfig.COMMON.timeMode.set(tm);
            KeepSmeltingConfig.COMMON.timeMode.save();
            CommandUtil.send(ctx, " time §a→ §f" + tm);
            return 1;
        } catch (IllegalArgumentException e) {
            ctx.getSource().sendFailure(Component.translatable("command.keepsmelting.prefix")
                    .append(" §c").append(Component.translatable("command.keepsmelting.time.error.invalid")));
            return 0;
        }
    }

    public static int showStatus(CommandContext<CommandSourceStack> ctx) {
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
}
