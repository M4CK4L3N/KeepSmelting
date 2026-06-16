/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.brigadier.CommandDispatcher
 *  com.mojang.brigadier.arguments.ArgumentType
 *  com.mojang.brigadier.arguments.IntegerArgumentType
 *  com.mojang.brigadier.arguments.LongArgumentType
 *  com.mojang.brigadier.builder.LiteralArgumentBuilder
 *  com.mojang.brigadier.context.CommandContext
 *  com.mojang.brigadier.exceptions.CommandSyntaxException
 *  net.minecraft.ChatFormatting
 *  net.minecraft.commands.CommandSourceStack
 *  net.minecraft.commands.Commands
 *  net.minecraft.commands.arguments.coordinates.BlockPosArgument
 *  net.minecraft.core.BlockPos
 *  net.minecraft.core.Vec3i
 *  net.minecraft.network.chat.Component
 *  net.minecraft.server.level.ServerLevel
 *  net.minecraft.server.level.ServerPlayer
 *  net.minecraft.world.level.Level
 *  net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity
 *  net.minecraft.world.level.block.entity.BlockEntity
 *  net.minecraft.world.level.block.state.BlockState
 *  net.minecraft.world.level.chunk.LevelChunk
 */
package mod.gottsch.forge.everfurnace.core.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.LongArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import mod.gottsch.forge.everfurnace.core.config.EverFurnaceConfig;
import mod.gottsch.forge.everfurnace.core.furnace.IEverFurnaceBlockEntity;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;

public class EverFurnaceCommand {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)Commands.m_82127_((String)"everfurnace").requires(source -> source.m_6761_(2))).then(Commands.m_82127_((String)"simulate").then(Commands.m_82129_((String)"radius", (ArgumentType)IntegerArgumentType.integer((int)1, (int)128)).then(Commands.m_82129_((String)"ticks", (ArgumentType)LongArgumentType.longArg((long)1L)).executes(ctx -> EverFurnaceCommand.simulate((CommandSourceStack)ctx.getSource(), IntegerArgumentType.getInteger((CommandContext)ctx, (String)"radius"), LongArgumentType.getLong((CommandContext)ctx, (String)"ticks"))))))).then(Commands.m_82127_((String)"tick").then(Commands.m_82129_((String)"radius", (ArgumentType)IntegerArgumentType.integer((int)1, (int)128)).executes(ctx -> EverFurnaceCommand.tick((CommandSourceStack)ctx.getSource(), IntegerArgumentType.getInteger((CommandContext)ctx, (String)"radius")))))).then(((LiteralArgumentBuilder)Commands.m_82127_((String)"inspect").executes(ctx -> EverFurnaceCommand.inspect((CommandSourceStack)ctx.getSource(), null))).then(Commands.m_82129_((String)"pos", (ArgumentType)BlockPosArgument.m_118239_()).executes(ctx -> EverFurnaceCommand.inspect((CommandSourceStack)ctx.getSource(), BlockPosArgument.m_118242_((CommandContext)ctx, (String)"pos"))))));
    }

    private static int simulate(CommandSourceStack source, int radius, long ticks) throws CommandSyntaxException {
        ServerLevel level = source.m_81372_();
        ServerPlayer player = source.m_81375_();
        BlockPos origin = player.m_20183_();
        List<BlockPos> positions = EverFurnaceCommand.furnacePositionsInRadius(level, origin, radius);
        int count = 0;
        for (BlockPos pos : positions) {
            AbstractFurnaceBlockEntity furnace;
            IEverFurnaceBlockEntity mixin;
            long current;
            BlockEntity be = level.m_7702_(pos);
            if (!(be instanceof AbstractFurnaceBlockEntity) || (current = (mixin = (IEverFurnaceBlockEntity)(furnace = (AbstractFurnaceBlockEntity)be)).everFurnace_1_20_1$getLastGameTime()) <= 0L) continue;
            mixin.everFurnace_1_20_1$setLastGameTime(current - ticks);
            furnace.m_6596_();
            ++count;
        }
        double minutes = (double)ticks / 1200.0;
        int result = count;
        source.m_288197_(() -> Component.m_237113_((String)String.format("Backdated %d furnace(s) by %d ticks (%.1f min) within %d blocks. Use '/everfurnace tick <radius>' to apply catch-up immediately.", result, ticks, minutes, radius)).m_130940_(ChatFormatting.GREEN), false);
        return count;
    }

    private static int tick(CommandSourceStack source, int radius) throws CommandSyntaxException {
        ServerLevel level = source.m_81372_();
        ServerPlayer player = source.m_81375_();
        BlockPos origin = player.m_20183_();
        List<BlockPos> positions = EverFurnaceCommand.furnacePositionsInRadius(level, origin, radius);
        int triggered = 0;
        for (BlockPos pos : positions) {
            BlockState state = level.m_8055_(pos);
            BlockEntity be = level.m_7702_(pos);
            if (!(be instanceof AbstractFurnaceBlockEntity)) continue;
            AbstractFurnaceBlockEntity furnace = (AbstractFurnaceBlockEntity)be;
            AbstractFurnaceBlockEntity.m_155013_((Level)level, (BlockPos)pos, (BlockState)state, (AbstractFurnaceBlockEntity)furnace);
            ++triggered;
        }
        int result = triggered;
        source.m_288197_(() -> Component.m_237113_((String)String.format("Triggered serverTick for %d furnace(s) within %d blocks.", result, radius)).m_130940_(ChatFormatting.GREEN), false);
        return triggered;
    }

    private static int inspect(CommandSourceStack source, BlockPos targetPos) throws CommandSyntaxException {
        BlockPos pos;
        ServerLevel level = source.m_81372_();
        BlockEntity be = level.m_7702_(pos = targetPos != null ? targetPos : source.m_81375_().m_20183_());
        if (!(be instanceof AbstractFurnaceBlockEntity)) {
            source.m_288197_(() -> Component.m_237113_((String)("No furnace block entity at " + pos.m_123344_() + " in " + level.m_46472_().m_135782_())).m_130940_(ChatFormatting.YELLOW), false);
            return 0;
        }
        AbstractFurnaceBlockEntity furnace = (AbstractFurnaceBlockEntity)be;
        IEverFurnaceBlockEntity mixin = (IEverFurnaceBlockEntity)furnace;
        long now = level.m_46467_();
        long lastGameTime = mixin.everFurnace_1_20_1$getLastGameTime();
        long delta = now - lastGameTime;
        long threshold = ((Integer)EverFurnaceConfig.COMMON.minDeltaThreshold.get()).intValue();
        boolean wouldTrigger = lastGameTime > 0L && delta >= threshold;
        source.m_288197_(() -> Component.m_237113_((String)("=== EverFurnace inspect @ " + pos.m_123344_() + " ===")).m_130940_(ChatFormatting.AQUA), false);
        source.m_288197_(() -> Component.m_237113_((String)("  lastGameTime         : " + lastGameTime + "  (delta: " + delta + " ticks / " + String.format("%.1f", (double)delta / 1200.0) + " min)")).m_130940_(ChatFormatting.WHITE), false);
        source.m_288197_(() -> Component.m_237113_((String)("  pendingNotification  : " + mixin.everFurnace_1_20_1$getPendingNotification())).m_130940_(ChatFormatting.WHITE), false);
        source.m_288197_(() -> Component.m_237113_((String)("  lastNotificationTime : " + mixin.everFurnace_1_20_1$getLastNotificationTime())).m_130940_(ChatFormatting.WHITE), false);
        source.m_288197_(() -> Component.m_237113_((String)("  pendingXp            : " + String.format("%.2f", Float.valueOf(mixin.everFurnace_1_20_1$getPendingXp())))).m_130940_(ChatFormatting.WHITE), false);
        source.m_288197_(() -> Component.m_237113_((String)("  catch-up next tick?  : " + (wouldTrigger ? "YES" : "no") + "  (need delta>=" + threshold + ")")).m_130940_(wouldTrigger ? ChatFormatting.GREEN : ChatFormatting.GRAY), false);
        return 1;
    }

    private static List<BlockPos> furnacePositionsInRadius(ServerLevel level, BlockPos origin, int radius) {
        ArrayList<BlockPos> result = new ArrayList<BlockPos>();
        long radiusSq = (long)radius * (long)radius;
        int minCx = origin.m_123341_() - radius >> 4;
        int maxCx = origin.m_123341_() + radius >> 4;
        int minCz = origin.m_123343_() - radius >> 4;
        int maxCz = origin.m_123343_() + radius >> 4;
        for (int cx = minCx; cx <= maxCx; ++cx) {
            for (int cz = minCz; cz <= maxCz; ++cz) {
                if (!(level.m_7726_().m_7131_(cx, cz) instanceof LevelChunk)) continue;
                LevelChunk chunk = level.m_6325_(cx, cz);
                for (Map.Entry entry : chunk.m_62954_().entrySet()) {
                    if (!(entry.getValue() instanceof AbstractFurnaceBlockEntity) || !(((BlockPos)entry.getKey()).m_123331_((Vec3i)origin) <= (double)radiusSq)) continue;
                    result.add((BlockPos)entry.getKey());
                }
            }
        }
        return result;
    }
}

