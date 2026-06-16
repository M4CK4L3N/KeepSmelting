package com.example.examplemod.mixin.ironfurnaces;

import com.example.examplemod.KeepSmeltingConfig;
import ironfurnaces.tileentity.furnaces.BlockIronFurnaceTileBase;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.common.ForgeHooks;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;
import java.util.Optional;

/**
 * Catchup mixin for Iron Furnaces BlockIronFurnaceTileBase.
 * Uses targets string + Pseudo — safe when ironfurnaces absent.
 * @Inject with exact type matches runtime signature.
 */
@Pseudo
@Mixin(targets = "ironfurnaces.tileentity.furnaces.BlockIronFurnaceTileBase")
public abstract class IronFurnaceTickMixin {

    @Unique
    private static final String TAG_LAST_TIME = "keepsmelting_lastRealTime";

    @Unique
    private long keepsmelting$lastRealTime;

    @Inject(method = "saveAdditional", at = @At("TAIL"))
    private void onSave(CompoundTag tag, CallbackInfo ci) {
        tag.putLong(TAG_LAST_TIME, this.keepsmelting$lastRealTime);
    }

    @Inject(method = "load", at = @At("TAIL"))
    private void onLoad(CompoundTag tag, CallbackInfo ci) {
        this.keepsmelting$lastRealTime = tag.getLong(TAG_LAST_TIME);
    }

    @Inject(method = "tick", at = @At("HEAD"), remap = false)
    private static void onTick(Level level, BlockPos pos, BlockState state,
                               BlockIronFurnaceTileBase tile, CallbackInfo ci) {
        if (level.isClientSide) return;
        if (!KeepSmeltingConfig.COMMON.catchupEnabled.get()) return;

        IronFurnaceTickMixin self = (IronFurnaceTickMixin) (Object) tile;
        long now = System.currentTimeMillis();
        long last = self.keepsmelting$lastRealTime;
        self.keepsmelting$lastRealTime = now;
        if (last == 0) return;

        long elapsed = (now - last) / 50L;
        int minDelta = KeepSmeltingConfig.COMMON.minDeltaThreshold.get();
        if (elapsed < minDelta) return;
        long max = KeepSmeltingConfig.COMMON.maxCatchupTicks.get();
        elapsed = Math.min(elapsed, max);
        if (elapsed <= 0) return;

        // furnace mode only for safety
        if (tile.currentAugment[2] != 0) return;
        if (tile.furnaceBurnTime <= 0 || tile.totalCookTime <= 0) return;

        // ── snapshot before for chatDebug ──
        int cookTimeBefore = tile.cookTime;
        int burnTimeBefore = tile.furnaceBurnTime;
        int inputBefore = tile.inventory.get(0).getCount();
        int outputBefore = tile.inventory.get(2).getCount();
        int fuelBefore = tile.inventory.get(1).getCount();

        ItemStack input = tile.inventory.get(0);
        if (input.isEmpty()) return;

        int inputCount = input.getCount();
        int maxStack = tile.getMaxStackSize();
        long remaining = elapsed;
        int cookTime = tile.cookTime;
        int burnTime = tile.furnaceBurnTime;
        int totalCookTime = tile.totalCookTime;
        ItemStack fuel = tile.inventory.get(1);
        ItemStack output = tile.inventory.get(2);

        while (remaining > 0 && burnTime > 0 && !input.isEmpty()) {
            long applyTick = Math.min(remaining, Math.min(burnTime,
                    totalCookTime - cookTime + (long) (inputCount - 1) * totalCookTime));
            cookTime += (int) applyTick;
            burnTime -= (int) applyTick;
            remaining -= applyTick;

            while (cookTime >= totalCookTime && !input.isEmpty()) {
                cookTime -= totalCookTime;
                if (!output.isEmpty() && output.getCount() >= maxStack) break;
                input.shrink(1);
                inputCount--;

                IronFurnaceAccessor acc = (IronFurnaceAccessor) tile;
                java.util.Optional<?> recipeOpt = acc.invokeGetRecipe(
                        input.isEmpty() ? ItemStack.EMPTY : input);
                if (recipeOpt.isPresent()) {
                    net.minecraft.world.item.crafting.Recipe recipe = (net.minecraft.world.item.crafting.Recipe) recipeOpt.get();
                    ItemStack result = recipe.getResultItem(level.registryAccess());
                    if (output.isEmpty()) {
                        tile.inventory.set(2, result.copy());
                        output = tile.inventory.get(2);
                    } else if (ItemStack.isSameItemSameTags(output, result)) {
                        output.grow(result.getCount());
                    }
                }
                if (input.getCount() <= 0) {
                    tile.inventory.set(0, ItemStack.EMPTY);
                    input = ItemStack.EMPTY;
                }
            }

            if (burnTime <= 0 && remaining > 0 && !fuel.isEmpty()) {
                int fb = ForgeHooks.getBurnTime(fuel, RecipeType.SMELTING);
                if (fb > 0) {
                    burnTime = fb;
                    fuel.shrink(1);
                    if (fuel.getCount() <= 0) {
                        tile.inventory.set(1, fuel.getCraftingRemainingItem());
                    }
                } else break;
            }
        }
        tile.furnaceBurnTime = Math.max(0, burnTime);
        tile.cookTime = Math.max(0, cookTime);
        tile.setChanged();

        // ── chat debug ──
        if (KeepSmeltingConfig.COMMON.chatDebug.get() && level instanceof ServerLevel serverLevel) {
            int outputAfter = tile.inventory.get(2).getCount();
            int fuelAfter = tile.inventory.get(1).getCount();
            int cookTimeAfter = tile.cookTime;
            int burnTimeAfter = tile.furnaceBurnTime;

            Component msg = Component.literal(
                    String.format("§7[§6KeepSmelting§7] §e[IronFurnace] §f%s §7| §e%d§7t elapsed | §c-%dfuel §a+%dout §b+%dcook §d-%dburn §7lit=%s",
                            pos.toShortString(), elapsed,
                            fuelBefore - fuelAfter,
                            outputAfter - outputBefore,
                            cookTimeAfter - cookTimeBefore,
                            burnTimeBefore - burnTimeAfter,
                            burnTimeAfter > 0));
            sendToNearbyPlayers(serverLevel, pos, msg);
        }
    }

    @Unique
    private static void sendToNearbyPlayers(ServerLevel level, BlockPos pos, Component msg) {
        AABB box = new AABB(pos).inflate(16);
        List<ServerPlayer> players = level.getEntitiesOfClass(ServerPlayer.class, box);
        for (ServerPlayer p : players) {
            p.sendSystemMessage(msg);
        }
    }
}
