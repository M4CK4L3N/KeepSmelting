package com.keepsmelting.command;

import com.keepsmelting.api.CatchupHandlerRegistry;
import com.keepsmelting.api.IFurnaceCatchupHandler;
import com.keepsmelting.internal.catchup.VanillaCatchupHandler;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;

/**
 * /keepsmelting simulate <ticks>
 */
public class SimulateSubcommand {

    public static int run(CommandContext<CommandSourceStack> ctx, long ticks) {
        ServerPlayer player = CommandUtil.getPlayer(ctx);
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
        CommandUtil.send(ctx, " §aSimulated §e" + ticks + "§7t §afor §f" + found + " §afurnace(s)");
        return 1;
    }
}
