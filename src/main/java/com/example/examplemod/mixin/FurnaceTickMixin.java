package com.example.examplemod.mixin;

import com.example.examplemod.KeepSmelting;
import com.example.examplemod.KeepSmeltingConfig;
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
    private static final String LAST_REAL_TIME_TAG = "keepsmelting_lastRealTime";
    @Unique
    private static final String NBT_VERSION_TAG = "keepsmelting_version";
    @Unique
    private static final int CURRENT_NBT_VERSION = 1;

    @Unique
    private long keepsmelting$lastRealTime;

    @Unique
    private String keepsmelting$activeTimeMode;

    // ──────────────────────────────────────────────
    //  NBT save / load
    // ──────────────────────────────────────────────

    @Inject(method = "saveAdditional", at = @At("TAIL"))
    private void onSave(net.minecraft.nbt.CompoundTag tag, CallbackInfo ci) {
        tag.putInt(NBT_VERSION_TAG, CURRENT_NBT_VERSION);
        tag.putLong(LAST_REAL_TIME_TAG, this.keepsmelting$lastRealTime);
        tag.putString("keepsmelting_timeMode", KeepSmeltingConfig.COMMON.timeMode.get().name());
    }

    @Inject(method = "load", at = @At("TAIL"))
    private void onLoad(net.minecraft.nbt.CompoundTag tag, CallbackInfo ci) {
        String savedMode = tag.getString("keepsmelting_timeMode");
        String currentMode = KeepSmeltingConfig.COMMON.timeMode.get().name();
        if (!savedMode.isEmpty() && savedMode.equals(currentMode)) {
            this.keepsmelting$lastRealTime = tag.getLong(LAST_REAL_TIME_TAG);
        } else {
            this.keepsmelting$lastRealTime = 0L;
        }
    }

    // ──────────────────────────────────────────────
    //  serverTick interception
    // ──────────────────────────────────────────────

    @Inject(method = "serverTick", at = @At("HEAD"))
    private static void onTick(Level world, BlockPos pos, BlockState state,
                               AbstractFurnaceBlockEntity blockEntity, CallbackInfo ci) {
        if (world.isClientSide) return;

        if (!KeepSmeltingConfig.COMMON.catchupEnabled.get()) return;

        FurnaceTickMixin self = (FurnaceTickMixin) (Object) blockEntity;

        String currentMode = KeepSmeltingConfig.COMMON.timeMode.get().name();
        if (self.keepsmelting$activeTimeMode != null && !self.keepsmelting$activeTimeMode.equals(currentMode)) {
            self.keepsmelting$lastRealTime = 0L;
        }
        self.keepsmelting$activeTimeMode = currentMode;

        long lastReal = self.keepsmelting$lastRealTime;
        long elapsedTicks;
        if (KeepSmeltingConfig.COMMON.timeMode.get() == KeepSmeltingConfig.TimeMode.GAMETIME) {
            self.keepsmelting$lastRealTime = world.getGameTime();
            elapsedTicks = self.keepsmelting$lastRealTime - lastReal;
        } else {
            self.keepsmelting$lastRealTime = System.currentTimeMillis();
            elapsedTicks = (self.keepsmelting$lastRealTime - lastReal) / 50L;
        }

        if (lastReal == 0) return;

        int minDelta = KeepSmeltingConfig.COMMON.minDeltaThreshold.get();
        if (elapsedTicks < minDelta) return;

        long maxTicks = KeepSmeltingConfig.COMMON.maxCatchupTicks.get();
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

        int cookTotal = acc.getCookingTotalTime();
        if (cookTotal <= 0) return;

        int litDur = acc.getLitDuration();
        if (litDur <= 0) return;

        // snapshot before — items may be replaced by callBurn (new ItemStack ref)
        int inputBefore = input.getCount();
        int outputBefore = output.getCount();
        int fuelBefore = fuel.getCount();
        int progressBefore = acc.getCookingProgress();

        // max ticks available — handle empty fuel (last ember still burning)
        long fuelTicks;
        if (fuel.isEmpty()) {
            fuelTicks = acc.getLitTime();              // only remaining burn time, no more fuel
        } else {
            fuelTicks = (long) (fuel.getCount() - 1) * litDur + acc.getLitTime();
        }
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

        // ── debug output — only when something actually happened ──
        if (KeepSmeltingConfig.COMMON.debugMode.get() != KeepSmeltingConfig.DebugMode.OFF) {
            NonNullList<ItemStack> itemsAfter = acc.getItems();
            int outputDelta = itemsAfter.get(2).getCount() - outputBefore;
            int fuelDelta = fuelBefore - itemsAfter.get(1).getCount();
            int progressDelta = acc.getCookingProgress() - progressBefore;
            int rfDelta = 0;
            boolean lit = acc.callIsLit();

            if (outputDelta == 0 && fuelDelta == 0 && progressDelta == 0) return;

            Component msg = Component.literal(
                    String.format("§7[§6KeepSmelting§7] §f%s §7| §e%d§7t | §a+%ditem §c-%dfuel §7lit=%s",
                            pos.toShortString(), deltaTime,
                            outputDelta,
                            fuelDelta,
                            lit));
            if (KeepSmeltingConfig.COMMON.debugMode.get() == KeepSmeltingConfig.DebugMode.LOG) {
                KeepSmelting.LOGGER.info(msg.getString());
            } else {
                sendToNearbyPlayers(level, pos, msg);
            }
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

        // if no fuel left, burn what we can and stop
        if (fuelStack.isEmpty()) {
            acc.setLitTime(0);
            return;
        }

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
