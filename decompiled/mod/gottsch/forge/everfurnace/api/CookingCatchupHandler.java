/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.core.BlockPos
 *  net.minecraft.server.level.ServerLevel
 *  net.minecraft.world.level.block.entity.BlockEntity
 */
package mod.gottsch.forge.everfurnace.api;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;

@FunctionalInterface
public interface CookingCatchupHandler {
    public void applyCatchup(BlockEntity var1, long var2, ServerLevel var4, BlockPos var5);
}

