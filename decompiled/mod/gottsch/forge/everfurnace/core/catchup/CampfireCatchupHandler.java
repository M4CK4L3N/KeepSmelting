/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.core.BlockPos
 *  net.minecraft.core.NonNullList
 *  net.minecraft.server.level.ServerLevel
 *  net.minecraft.world.item.ItemStack
 *  net.minecraft.world.level.block.entity.BlockEntity
 *  net.minecraft.world.level.block.entity.CampfireBlockEntity
 */
package mod.gottsch.forge.everfurnace.core.catchup;

import mod.gottsch.forge.everfurnace.api.CookingCatchupHandler;
import mod.gottsch.forge.everfurnace.core.mixin.ICampfireBlockEntityMixin;
import mod.gottsch.forge.everfurnace.core.network.ModNetwork;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.CampfireBlockEntity;

public class CampfireCatchupHandler
implements CookingCatchupHandler {
    @Override
    public void applyCatchup(BlockEntity blockEntity, long deltaTime, ServerLevel level, BlockPos pos) {
        CampfireBlockEntity campfire = (CampfireBlockEntity)blockEntity;
        ICampfireBlockEntityMixin accessor = (ICampfireBlockEntityMixin)blockEntity;
        NonNullList items = campfire.m_59065_();
        int[] cookingProgress = accessor.getCookingProgress();
        int[] cookingTime = accessor.getCookingTime();
        boolean anyCompleted = false;
        for (int i = 0; i < items.size(); ++i) {
            int total;
            if (((ItemStack)items.get(i)).m_41619_() || (total = cookingTime[i]) <= 0) continue;
            int remaining = total - cookingProgress[i];
            if (deltaTime >= (long)remaining) {
                cookingProgress[i] = total;
                anyCompleted = true;
                continue;
            }
            int n = i;
            cookingProgress[n] = cookingProgress[n] + (int)deltaTime;
        }
        if (anyCompleted) {
            ModNetwork.sendCatchupParticles(level, pos);
        }
    }
}

