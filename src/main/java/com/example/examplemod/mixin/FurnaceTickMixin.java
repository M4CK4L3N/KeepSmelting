package com.example.examplemod.mixin;

import com.example.examplemod.TimeFurnaceConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.AbstractFurnaceBlock;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Optional;

/**
 * Core mixin — intercepts AbstractFurnaceBlockEntity.serverTick.
 * <p>
 * Tracks real-world time (System.currentTimeMillis) so catchup works
 * across chunk unload, game exit, and menu pause.
 * <p>
 * Logic mirrors EverFurnace's FurnaceCatchupHandler:
 *   • burn fuel ticks for the elapsed period
 *   • advance cooking progress
 *   • produce finished items
 *   • update block state when fuel runs out
 */
@Mixin(AbstractFurnaceBlockEntity.class)
public abstract class FurnaceTickMixin {

    @Unique
    private static final String LAST_REAL_TIME_TAG = "timefurnace_lastRealTime";
    @Unique
    private static final String NBT_VERSION_TAG = "timefurnace_version";
    @Unique
    private static final int CURRENT_NBT_VERSION = 1;

    @Unique
    private long timefurnace$lastRealTime;

    // ──────────────────────────────────────────────
    //  NBT save / load
    // ──────────────────────────────────────────────

    @Inject(method = "saveAdditional", at = @At("TAIL"))
    private void onSave(net.minecraft.nbt.CompoundTag tag, CallbackInfo ci) {
        tag.putInt(NBT_VERSION_TAG, CURRENT_NBT_VERSION);
        tag.putLong(LAST_REAL_TIME_TAG, this.timefurnace$lastRealTime);
    }

    @Inject(method = "load", at = @At("TAIL"))
    private void onLoad(net.minecraft.nbt.CompoundTag tag, CallbackInfo ci) {
        this.timefurnace$lastRealTime = tag.getLong(LAST_REAL_TIME_TAG);
    }

    // ──────────────────────────────────────────────
    //  serverTick interception
    // ──────────────────────────────────────────────

    @Inject(method = "serverTick", at = @At("HEAD"))
    private static void onTick(Level world, BlockPos pos, BlockState state,
                               AbstractFurnaceBlockEntity blockEntity, CallbackInfo ci) {
        if (world.isClientSide) return;

        if (!TimeFurnaceConfig.COMMON.catchupEnabled.get()) return;

        FurnaceTickMixin self = (FurnaceTickMixin) (Object) blockEntity;

        long now = System.currentTimeMillis();
        long lastReal = self.timefurnace$lastRealTime;
        self.timefurnace$lastRealTime = now;

        if (lastReal == 0) return;

        long elapsedMs = now - lastReal;
        long elapsedTicks = elapsedMs / 50L;

        int minDelta = TimeFurnaceConfig.COMMON.minDeltaThreshold.get();
        if (elapsedTicks < minDelta) return;

        long maxTicks = TimeFurnaceConfig.COMMON.maxCatchupTicks.get();
        elapsedTicks = Math.min(elapsedTicks, maxTicks);
        if (elapsedTicks <= 0) return;

        applyCatchup(blockEntity, elapsedTicks, (ServerLevel) world, pos);
    }

    // ──────────────────────────────────────────────
    //  Catchup — fuel burning + cooking
    // ──────────────────────────────────────────────

    @Unique
    private static void applyCatchup(AbstractFurnaceBlockEntity furnace, long deltaTime,
                                     ServerLevel level, BlockPos pos) {
        IFurnaceAccessor acc = (IFurnaceAccessor) furnace;

        if (!acc.callIsLit()) return;

        NonNullList<ItemStack> items = acc.getItems();
        ItemStack input = items.get(0);
        if (input.isEmpty()) return;

        ItemStack output = items.get(2);
        if (!output.isEmpty() && output.getCount() >= furnace.getMaxStackSize()) return;

        Recipe<?> recipe = findRecipe(furnace, level);
        if (recipe == null) return;

        if (!acc.callCanBurn(level.registryAccess(), recipe, items, furnace.getMaxStackSize())) return;

        ItemStack fuel = items.get(1);
        if (fuel.isEmpty()) return;

        int cookTotal = acc.getCookingTotalTime();
        if (cookTotal <= 0) return;

        int litDur = acc.getLitDuration();
        if (litDur <= 0) return;

        // snapshot for chat debug
        int inputBefore = input.getCount();
        int outputBefore = output.getCount();
        int fuelBefore = fuel.getCount();
        int progressBefore = acc.getCookingProgress();

        // max ticks available from remaining fuel
        long fuelTicks = (long) (fuel.getCount() - 1) * litDur + acc.getLitTime();
        // max ticks needed to smelt all input
        long cookTicksNeeded = (long) (input.getCount() - 1) * cookTotal
                + (cookTotal - acc.getCookingProgress());

        long applyTicks = Math.min(deltaTime, Math.min(fuelTicks, cookTicksNeeded));
        if (applyTicks <= 0) return;

        // 1) burn fuel
        applyFuelTime(acc, fuel, applyTicks, litDur);

        // 2) smelt items
        applyCookTime(level, furnace, acc, recipe, input, applyTicks, cookTotal);

        furnace.setChanged();

        // toggle lit state if fuel exhausted
        if (!acc.callIsLit()) {
            BlockState bs = level.getBlockState(pos);
            if (bs.hasProperty(AbstractFurnaceBlock.LIT)) {
                level.setBlock(pos, bs.setValue(AbstractFurnaceBlock.LIT, false), 3);
            }
            furnace.setChanged();
        }

        // ── chat debug ──
        if (TimeFurnaceConfig.COMMON.chatDebug.get()) {
            int outputProduced = output.getCount() - outputBefore;
            int fuelConsumed = fuelBefore - fuel.getCount();
            int progressNow = acc.getCookingProgress();
            boolean lit = acc.callIsLit();

            Component msg = Component.literal(
                    String.format("§7[§6TimeFurnace§7] §f%s §7| §e%d§7t elapsed | §c-%dfuel §a+%dout §b+%dprogress §7lit=%s",
                            pos.toShortString(), deltaTime, fuelConsumed, outputProduced, progressNow - progressBefore, lit));
            sendToNearbyPlayers(level, pos, msg);
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

    // ──────────────────────────────────────────────
    //  Fuel burning (same algo as EverFurnace)
    // ──────────────────────────────────────────────

    @Unique
    private static void applyFuelTime(IFurnaceAccessor acc, ItemStack fuelStack,
                                      long ticks, int litDuration) {
        long remaining = ticks;
        int litTime = acc.getLitTime();

        if (remaining <= litTime) {
            acc.setLitTime(litTime - (int) remaining);
            return;
        }

        remaining -= litTime;
        int wholeItems = (int) Math.ceil((double) remaining / litDuration);
        wholeItems = Math.min(wholeItems, fuelStack.getCount());

        long ticksFromNew = (long) wholeItems * litDuration;
        long leftover = ticksFromNew - remaining;

        fuelStack.shrink(wholeItems);
        if (fuelStack.isEmpty()) {
            acc.setLitTime(0);
        } else {
            acc.setLitTime((int) leftover);
        }
    }

    // ──────────────────────────────────────────────
    //  Cooking application (same algo as EverFurnace)
    // ──────────────────────────────────────────────

    @Unique
    private static void applyCookTime(Level world, AbstractFurnaceBlockEntity furnace,
                                      IFurnaceAccessor acc, Recipe<?> recipe,
                                      ItemStack input, long ticks, int cookTotal) {
        int progress = acc.getCookingProgress();
        int neededForCurrent = cookTotal - progress;

        if (ticks < neededForCurrent) {
            acc.setCookingProgress(progress + (int) ticks);
            return;
        }

        long remaining = ticks - neededForCurrent;

        // finish current item
        acc.setCookingProgress(cookTotal);
        if (acc.callBurn(world.registryAccess(), recipe, acc.getItems(), furnace.getMaxStackSize())) {
            furnace.setRecipeUsed(recipe);
        }
        acc.setCookingProgress(0);

        // smelt additional full items
        if (!input.isEmpty() && cookTotal > 0) {
            long fullItems = remaining / cookTotal;
            int remainder = (int) (remaining % cookTotal);

            for (long i = 0; i < fullItems; i++) {
                if (!acc.callCanBurn(world.registryAccess(), recipe, acc.getItems(), furnace.getMaxStackSize()))
                    break;
                if (acc.callBurn(world.registryAccess(), recipe, acc.getItems(), furnace.getMaxStackSize())) {
                    furnace.setRecipeUsed(recipe);
                }
            }

            acc.setCookingProgress(remainder);
        }
    }

    // ──────────────────────────────────────────────
    //  Recipe lookup
    // ──────────────────────────────────────────────

    @Nullable
    @Unique
    @SuppressWarnings({"rawtypes", "unchecked"})
    private static Recipe<?> findRecipe(AbstractFurnaceBlockEntity furnace, Level level) {
        BlockEntityType<?> type = furnace.getType();
        RecipeType recipeType;
        if (type == BlockEntityType.SMOKER) {
            recipeType = RecipeType.SMOKING;
        } else if (type == BlockEntityType.BLAST_FURNACE) {
            recipeType = RecipeType.BLASTING;
        } else {
            recipeType = RecipeType.SMELTING;
        }
        Optional opt = level.getRecipeManager().getRecipeFor(recipeType, (Container) furnace, level);
        return (Recipe<?>) opt.orElse(null);
    }
}
