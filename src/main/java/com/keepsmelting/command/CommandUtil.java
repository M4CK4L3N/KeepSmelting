package com.keepsmelting.command;

import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.fml.ModList;

/**
 * Утилиты для команд KeepSmelting.
 */
public class CommandUtil {

    private CommandUtil() {}

    public static void send(CommandContext<CommandSourceStack> ctx, String msg) {
        ctx.getSource().sendSuccess(() -> Component.literal("§7[§6KeepSmelting§7]" + msg), false);
    }

    public static void send(ServerPlayer player, String msg) {
        player.sendSystemMessage(Component.literal("§7[§6KeepSmelting§7]" + msg));
    }

    public static void sendHelp(CommandContext<CommandSourceStack> ctx, String msg) {
        ctx.getSource().sendSuccess(() -> Component.literal(msg), false);
    }

    public static ServerPlayer getPlayer(CommandContext<CommandSourceStack> ctx) {
        if (!(ctx.getSource().getEntity() instanceof ServerPlayer player)) {
            ctx.getSource().sendFailure(Component.literal("§cТолько для игроков"));
            return null;
        }
        return player;
    }

    public static boolean isChestType(String type) {
        return "chest".equals(type) || "chest_out".equals(type) || "chest_fuel".equals(type)
                || "chest_input".equals(type)
                || "hopper_in".equals(type) || "hopper_out".equals(type)
                || "hopper_into_top".equals(type) || "hopper_into_side".equals(type)
                || "hopper_out_of_bottom".equals(type);
    }

    public static String getPatternsHelp() {
        StringBuilder sb = new StringBuilder();
        for (String k : VanillaTestPatterns.PATTERNS.keySet()) {
            sb.append("§e").append(k).append("§7, ");
        }
        if (ModList.get().isLoaded("ironfurnaces")) {
            for (String k : TestSubcommand.IF_PATTERNS) {
                sb.append("§e").append(k).append("§7, ");
            }
        }
        String result = sb.toString();
        return result.isEmpty() ? "" : result.substring(0, result.length() - 2);
    }
}
