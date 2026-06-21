package com.keepsmelting.internal.catchup;

import com.keepsmelting.KeepSmelting;
import com.keepsmelting.KeepSmeltingConfig;
import com.keepsmelting.api.catchup.AbstractCatchupHandler;
import com.keepsmelting.mixin.IFurnaceAccessor;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.AABB;

/**
 * Обработчик catchup для ванильных печей.
 * <p>
 * Использует {@link VanillaHopperSimulator} для симуляции конвейера с воронками:
 * discovery → bottleneck calc → apply.
 */
public class VanillaCatchupHandler extends AbstractCatchupHandler {

    public static final VanillaCatchupHandler INSTANCE = new VanillaCatchupHandler();

    @Override
    public void applyCatchup(BlockEntity tile, long elapsed, Level level, BlockPos pos) {
        if (!(level instanceof ServerLevel serverLevel)) return;
        AbstractFurnaceBlockEntity furnace = (AbstractFurnaceBlockEntity) tile;
        IFurnaceAccessor acc = (IFurnaceAccessor) furnace;
        if (!acc.callIsLit()) return;

        // Рецепт
        Recipe<?> recipe = VanillaHopperIO.findRecipe(furnace, level);
        if (recipe == null) return;
        int cookTotal = acc.getCookingTotalTime();
        if (cookTotal <= 0) return;

        int cookingProgressBefore = acc.getCookingProgress();
        int litDuration = acc.getLitDuration();
        int litTime = acc.getLitTime();

        // ====================================================================
        // PHASE 1: Вытолкнуть готовое вниз (чтобы освободить слот 2)
        // ====================================================================
        VanillaHopperIO.pushToBelow(furnace, serverLevel, pos, 64);

        // ====================================================================
        // PHASE 2: DISCOVERY — построить Pipeline
        // ====================================================================
        var pipeline = VanillaHopperSimulator.discover(serverLevel, pos, furnace, recipe);

        // ====================================================================
        // PHASE 3: SIMULATE — вычислить bottleneck
        // ====================================================================
        var result = VanillaHopperSimulator.simulate(
                pipeline, elapsed,
                cookingProgressBefore, cookTotal,
                litDuration, litTime
        );

        // ====================================================================
        // PHASE 4: APPLY — распределить предметы по узлам
        // ====================================================================
        VanillaHopperSimulator.apply(
                furnace, acc, serverLevel, pos, result,
                recipe, cookTotal, litDuration,
                cookingProgressBefore, elapsed
        );

        // ====================================================================
        // PHASE 5: Вытолкнуть готовое вниз (если в слоте 2 осталось)
        // ====================================================================
        VanillaHopperIO.pushToBelow(furnace, serverLevel, pos, Integer.MAX_VALUE);

        // ====================================================================
        // DEBUG
        // ====================================================================
        var items = acc.getItems();
        if (KeepSmeltingConfig.COMMON.debugMode.get() != KeepSmeltingConfig.DebugMode.OFF) {
            boolean litStatus = acc.callIsLit();
            int hopperCount = getItemCount(serverLevel, pos.below());
            int barrelCount = getItemCount(serverLevel, pos.below().below());
            String debugInfo = String.format(
                    "§7[§6VanillaCatchup§7] §f%s §e%d§7t " +
                            "§7| §6cook§f%d " +
                            "§7| §6fuel§f%d(inF=%d) §7| §6input§f%d(inF=%d) " +
                            "§7| §6out→§ff=%d,h=%d,barrel=%d " +
                            "§7| §6lit=%s",
                    pos.toShortString(), elapsed,
                    result.itemsToCook(),
                    pipeline.fuelItemTotal(), items.get(1).getCount(),
                    pipeline.inputItemTotal(), items.get(0).getCount(),
                    items.get(2).getCount(), hopperCount, barrelCount,
                    litStatus
            );
            if (KeepSmeltingConfig.COMMON.debugMode.get() == KeepSmeltingConfig.DebugMode.LOG) {
                KeepSmelting.LOGGER.info(debugInfo.replace("§.", ""));
            }
            sendToNearbyPlayers(serverLevel, pos, Component.literal(debugInfo));
        }
    }

    private static int getItemCount(ServerLevel level, BlockPos pos) {
        if (!level.isLoaded(pos)) return -1;
        BlockEntity be = level.getBlockEntity(pos);
        if (be == null) return -1;
        int total = 0;
        if (be instanceof net.minecraft.world.Container c) {
            for (int i = 0; i < c.getContainerSize(); i++) {
                total += c.getItem(i).getCount();
            }
        }
        return total;
    }

    // ========================================================================
    // DEBUG
    // ========================================================================

    private void sendToNearbyPlayers(ServerLevel level, BlockPos pos, Component msg) {
        AABB box = new AABB(pos).inflate(16);
        for (ServerPlayer p : level.getEntitiesOfClass(ServerPlayer.class, box)) {
            p.sendSystemMessage(msg);
        }
    }
}
