package com.keepsmelting.internal.catchup;

import com.keepsmelting.mixin.IFurnaceAccessor;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.AbstractFurnaceBlock;
import net.minecraft.world.level.block.HopperBlock;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.HopperBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.ForgeHooks;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

/**
 * Симулятор конвейера с воронками для ванильных печей.
 * <p>
 * Строит {@link Pipeline} из BlockEntity вокруг печи, вычисляет узкое место (bottleneck)
 * и применяет результат к реальным контейнерам.
 * <p>
 * Модель потока:
 * <pre>
 * INPUT_SOURCE → INPUT_HOPPER → FURNACE → OUTPUT_HOPPER → OUTPUT_DEST
 * FUEL_SOURCE  → FUEL_HOPPER  → FURNACE
 * </pre>
 */
public class VanillaHopperSimulator {

    /** Скорость воронки: 1 предмет за 8 тиков (HopperBlockEntity.MOVE_ITEM_SPEED). */
    public static final long HOPPER_TICKS_PER_ITEM = 8;

    // ========================================================================
    // МОДЕЛЬ ДАННЫХ
    // ========================================================================

    /** Тип узла конвейера. */
    public enum NodeType {
        INPUT_SOURCE,    // Контейнер над печью/воронкой (руда/еда)
        INPUT_HOPPER,    // Воронка над печью (FACING=DOWN)
        FUEL_SOURCE,     // Контейнер сбоку/над боковой воронкой (топливо)
        FUEL_HOPPER,     // Воронка сбоку (FACING в сторону печи)
        FURNACE,         // Сама печь
        OUTPUT_HOPPER,   // Воронка под печью (выталкивает готовое)
        OUTPUT_DEST      // Контейнер под печью/воронкой (приёмник)
    }

    /**
     * Узел конвейера.
     *
     * @param type        тип узла
     * @param pos         позиция в мире
     * @param facing      направление (для воронок; {@code null} для контейнеров)
     * @param ticksPerItem сколько тиков на предмет (0 = безлимит)
     */
    public record PipelineNode(
            NodeType type,
            BlockPos pos,
            @Nullable Direction facing,
            long ticksPerItem
    ) {
        /** Пропускная способность узла за {@code elapsed} тиков. */
        public long throughput(long elapsed) {
            if (ticksPerItem <= 0) return Long.MAX_VALUE;
            return elapsed / ticksPerItem;
        }
    }

    /**
     * Конвейер — полная цепочка от источников до приёмников.
     *
     * @param nodes            все узлы
     * @param inputItemTotal   всего smeltable предметов на входе (печь + контейнеры)
     * @param fuelItemTotal    всего топлива (печь + контейнеры)
     * @param outputSlotSpace  свободное место на выходе (печь + контейнеры)
     * @param inputHopperCount есть ли воронка на входе (0 или 1)
     * @param fuelHopperCount  сколько боковых воронок с топливом (0-4)
     * @param outputHopperCount есть ли воронка на выходе (0 или 1)
     */
    public record Pipeline(
            List<PipelineNode> nodes,
            int inputItemTotal,
            int fuelItemTotal,
            int outputSlotSpace,
            int inputHopperCount,
            int fuelHopperCount,
            int outputHopperCount
    ) {
        /** Пропускная способность входа. */
        public long inputThroughput(long elapsed) {
            if (inputHopperCount == 0) return Long.MAX_VALUE;
            return elapsed / HOPPER_TICKS_PER_ITEM;
        }

        /** Пропускная способность топливных воронок (все параллельно). */
        public long fuelThroughput(long elapsed) {
            if (fuelHopperCount == 0) return Long.MAX_VALUE;
            return (long) fuelHopperCount * (elapsed / HOPPER_TICKS_PER_ITEM);
        }

        /** Пропускная способность выхода. */
        public long outputThroughput(long elapsed) {
            if (outputHopperCount == 0) return Long.MAX_VALUE;
            return elapsed / HOPPER_TICKS_PER_ITEM;
        }
    }

    /** Результат симуляции. */
    public record SimulationResult(
            int itemsToCook,
            int fuelConsumed,
            int inputConsumed
    ) {}

    // ========================================================================
    // DISCOVERY — сканирование окружения
    // ========================================================================

    /**
     * Строит {@link Pipeline}, сканируя BlockEntity вокруг печи.
     * <p>
     * Проверяет:
     * <ul>
     *   <li>Над печью (y+1) — вход: hopper FACING=DOWN или контейнер</li>
     *   <li>Под печью (y-1) — выход: hopper или контейнер</li>
     *   <li>С боков (N/S/E/W) — топливо: hopper FACING к печи или контейнер</li>
     *   <li>За воронками ещё +1 блок — их источник/приёмник</li>
     * </ul>
     */
    public static Pipeline discover(
            ServerLevel level,
            BlockPos furnacePos,
            AbstractFurnaceBlockEntity furnace,
            @Nullable Recipe<?> recipe
    ) {
        List<PipelineNode> nodes = new ArrayList<>();
        var items = ((IFurnaceAccessor) furnace).getItems();
        // СЧИТАЕМ ПРЕДМЕТЫ В САМОЙ ПЕЧИ (добавлено!)
        int inputItemTotal = items.get(0).getCount();   // руда в печи
        int fuelItemTotal = items.get(1).getCount();    // уголь в печи
        int outputSlotSpace = 0;
        int inputHopperCount = 0;
        int fuelHopperCount = 0;
        int outputHopperCount = 0;

        // ---- INPUT (над печью) ----
        BlockPos above = furnacePos.above();
        if (level.isLoaded(above)) {
            BlockEntity be = level.getBlockEntity(above);
            if (be instanceof HopperBlockEntity hopper) {
                Direction facing = hopperFacing(level, above);
                if (facing == Direction.DOWN) {
                    inputHopperCount = 1;
                    inputItemTotal += countSmeltable(furnace, hopper);
                    // Контейнер над воронкой
                    BlockPos aboveHopper = above.above();
                    if (level.isLoaded(aboveHopper)) {
                        BlockEntity src = level.getBlockEntity(aboveHopper);
                        if (src instanceof Container c) {
                            inputItemTotal += countSmeltable(furnace, c);
                            nodes.add(new PipelineNode(NodeType.INPUT_SOURCE, aboveHopper, null, 0));
                        }
                    }
                    nodes.add(new PipelineNode(NodeType.INPUT_HOPPER, above, facing, HOPPER_TICKS_PER_ITEM));
                }
            } else if (be instanceof Container c) {
                inputItemTotal += countSmeltable(furnace, c);
                nodes.add(new PipelineNode(NodeType.INPUT_SOURCE, above, null, 0));
            }
        }

        // ---- FUEL (с боков: N/S/E/W) ----
        for (Direction dir : Direction.Plane.HORIZONTAL) {
            BlockPos neighbor = furnacePos.relative(dir);
            if (!level.isLoaded(neighbor)) continue;
            BlockEntity be = level.getBlockEntity(neighbor);

            if (be instanceof HopperBlockEntity hopper) {
                Direction facing = hopperFacing(level, neighbor);
                // Воронка должна смотреть В СТОРОНУ печи
                if (facing == dir.getOpposite()) {
                    fuelHopperCount++;
                    fuelItemTotal += countBurnable(hopper);
                    // Контейнер над воронкой
                    BlockPos aboveHopper = neighbor.above();
                    if (level.isLoaded(aboveHopper)) {
                        BlockEntity src = level.getBlockEntity(aboveHopper);
                        if (src instanceof Container c) {
                            fuelItemTotal += countBurnable(c);
                            nodes.add(new PipelineNode(NodeType.FUEL_SOURCE, aboveHopper, null, 0));
                        }
                    }
                    nodes.add(new PipelineNode(NodeType.FUEL_HOPPER, neighbor, facing, HOPPER_TICKS_PER_ITEM));
                }
            } else if (be instanceof Container c) {
                fuelItemTotal += countBurnable(c);
                nodes.add(new PipelineNode(NodeType.FUEL_SOURCE, neighbor, null, 0));
            }
        }

        // ---- OUTPUT (под печью) ----
        // Выходной слот печи
        ItemStack outputStack = furnace.getItem(2);
        int maxStack = furnace.getMaxStackSize();
        outputSlotSpace += outputStack.isEmpty() ? maxStack : maxStack - outputStack.getCount();

        BlockPos below = furnacePos.below();
        if (level.isLoaded(below)) {
            BlockEntity be = level.getBlockEntity(below);
            if (be instanceof HopperBlockEntity hopper) {
                outputHopperCount = 1;
                outputSlotSpace += countSpace(hopper);
                // Контейнер под воронкой
                BlockPos belowHopper = below.below();
                if (level.isLoaded(belowHopper)) {
                    BlockEntity dest = level.getBlockEntity(belowHopper);
                    if (dest instanceof Container c) {
                        outputSlotSpace += countSpace(c);
                        nodes.add(new PipelineNode(NodeType.OUTPUT_DEST, belowHopper, null, 0));
                    }
                }
                nodes.add(new PipelineNode(NodeType.OUTPUT_HOPPER, below,
                        hopperFacing(level, below), HOPPER_TICKS_PER_ITEM));
            } else if (be instanceof Container c) {
                outputSlotSpace += countSpace(c);
                nodes.add(new PipelineNode(NodeType.OUTPUT_DEST, below, null, 0));
            }
        }

        return new Pipeline(nodes,
                inputItemTotal, fuelItemTotal, outputSlotSpace,
                inputHopperCount, fuelHopperCount, outputHopperCount);
    }

    // ========================================================================
    // SIMULATION — расчёт bottleneck
    // ========================================================================

    /**
     * Вычисляет, сколько предметов можно переплавить за {@code elapsed} тиков
     * с учётом pipeline, топлива и места на выходе.
     */
    public static SimulationResult simulate(
            Pipeline pipeline,
            long elapsed,
            int cookingProgressBefore,
            int cookTotal,
            int litDuration,
            int litTime
    ) {
        if (cookTotal <= 0 || litDuration <= 0) {
            return new SimulationResult(0, 0, 0);
        }

        // 1. Пропускная способность каждого узла цепочки
        long inputTP = pipeline.inputThroughput(elapsed);
        long fuelTP = pipeline.fuelThroughput(elapsed);
        long outputTP = pipeline.outputThroughput(elapsed);
        long furnaceTP = elapsed / cookTotal;

        // 2. Bottleneck цепочки (без топлива)
        long chainBottleneck = min(inputTP, furnaceTP, outputTP);

        // 3. Топливный лимит
        // Сколько топлива может прийти за elapsed
        long maxFuelArrivals = Math.min(pipeline.fuelItemTotal(), fuelTP);
        long totalBurnTicks = maxFuelArrivals * litDuration + (litTime > 0 ? litTime : 0);
        long actualTicks = Math.min(elapsed, totalBurnTicks);

        // Сколько предметов можно переплавить по топливу
        long maxByFuel = 0;
        if (cookTotal > 0) {
            int progressNeeded = cookTotal - cookingProgressBefore;
            if (actualTicks >= progressNeeded) {
                long leftover = actualTicks - progressNeeded;
                maxByFuel = 1 + (leftover / cookTotal);
            }
        }

        // 4. Финальный лимит = минимум из всех ограничений
        long itemsToCook = min(
                chainBottleneck,
                maxByFuel,
                pipeline.inputItemTotal(),
                pipeline.outputSlotSpace()
        );

        // 5. Расход топлива
        int fi = (int) Math.min(itemsToCook, Integer.MAX_VALUE);
        int fuelConsumed = 0;
        if (fi > 0) {
            long ticksNeeded = (long) fi * cookTotal - cookingProgressBefore;
            if (ticksNeeded < 0) ticksNeeded = 0;
            fuelConsumed = (int) Math.min(pipeline.fuelItemTotal(),
                    (int) Math.ceil((double) ticksNeeded / litDuration));
        }

        return new SimulationResult(fi, fuelConsumed, fi);
    }

    // ========================================================================
    // APPLY — распределение предметов по узлам
    // ========================================================================

    /**
     * Применяет результат симуляции к реальным BlockEntity:
     * вычитает топливо, вычитает вход, кладёт результат на выход.
     */
    @SuppressWarnings("deprecation")
    public static void apply(
            AbstractFurnaceBlockEntity furnace,
            IFurnaceAccessor acc,
            ServerLevel level,
            BlockPos pos,
            SimulationResult result,
            Recipe<?> recipe,
            int cookTotal,
            int litDuration,
            int cookingProgressBefore,
            long elapsed
    ) {
        var items = acc.getItems();

        // ====================================================================
        // 1. ПРЕДВАРИТЕЛЬНО: перемещаем предметы из источников в воронки
        //    (бочка → hopper, если hopper не пуст — сначала его, потом бочку)
        // ====================================================================
        // Топливо: бочка → воронка
        fillHoppersFromSources(level, pos);

        // ====================================================================
        // 2. ТОПЛИВО: печь → боковые контейнеры
        // ====================================================================
        int fuelRemaining = result.fuelConsumed();
        if (fuelRemaining > 0 && !items.get(1).isEmpty()) {
            int take = Math.min(fuelRemaining, items.get(1).getCount());
            items.get(1).shrink(take);
            fuelRemaining -= take;
        }
        if (fuelRemaining > 0) {
            consumeExternalFuel(level, pos, fuelRemaining);
        }

        // ====================================================================
        // 3. ВХОД: печь → контейнеры сверху
        // ====================================================================
        int inputRemaining = result.inputConsumed();
        if (inputRemaining > 0 && !items.get(0).isEmpty()) {
            int take = Math.min(inputRemaining, items.get(0).getCount());
            items.get(0).shrink(take);
            inputRemaining -= take;
        }
        if (inputRemaining > 0) {
            consumeExternalInput(furnace, level, pos, inputRemaining);
        }

        // ====================================================================
        // 4. ОПУСТОШАЕМ ВОРОНКИ: остатки из воронок НЕ возвращаются в бочки
        //    (просто оставляем как есть — в реальности hopper медленно перетечёт)
        // ====================================================================

        // ====================================================================
        // 3. ВЫХОД: печь → контейнеры снизу
        // ====================================================================
        if (result.itemsToCook() > 0) {
            ItemStack resultItem = recipe.getResultItem(level.registryAccess());
            if (resultItem.isEmpty()) {
                resultItem = new ItemStack(net.minecraft.world.item.Items.IRON_INGOT);
            }

            int maxStack = furnace.getMaxStackSize();
            int remaining = result.itemsToCook();

            // Сначала в слот 2 печи
            ItemStack output = items.get(2);
            if (output.isEmpty()) {
                int place = Math.min(remaining, maxStack);
                items.set(2, new ItemStack(resultItem.getItem(), place));
                remaining -= place;
            } else if (ItemStack.isSameItemSameTags(output, resultItem)) {
                int space = maxStack - output.getCount();
                if (space > 0) {
                    int place = Math.min(remaining, space);
                    output.grow(place);
                    remaining -= place;
                }
            }

            // Остаток — сразу в контейнер снизу (через воронку или напрямую)
            if (remaining > 0) {
                pushOverflowToBelow(level, pos, resultItem, remaining);
            }
        }

        // ====================================================================
        // 4. ПРОГРЕСС
        // ====================================================================
        long actualTicks = Math.min(elapsed,
                (long) result.fuelConsumed() * litDuration + (acc.getLitTime() > 0 ? acc.getLitTime() : 0));

        if (result.itemsToCook() > 0) {
            long leftover = actualTicks - ((long) result.itemsToCook() * cookTotal - cookingProgressBefore);
            if (leftover < 0) leftover = 0;
            acc.setCookingProgress((int) Math.min(leftover, cookTotal - 1));
        } else if (actualTicks < cookTotal - cookingProgressBefore) {
            acc.setCookingProgress(cookingProgressBefore + (int) actualTicks);
        }

        // ====================================================================
        // 5. LIT STATUS
        // ====================================================================
        if (result.fuelConsumed() > 0) {
            acc.setLitTime(litDuration);
            BlockState bs = level.getBlockState(pos);
            if (bs.hasProperty(AbstractFurnaceBlock.LIT) && !bs.getValue(AbstractFurnaceBlock.LIT)) {
                level.setBlock(pos, bs.setValue(AbstractFurnaceBlock.LIT, true), 3);
            }
        } else if (result.itemsToCook() <= 0) {
            acc.setLitTime(0);
            BlockState bs = level.getBlockState(pos);
            if (bs.hasProperty(AbstractFurnaceBlock.LIT)) {
                level.setBlock(pos, bs.setValue(AbstractFurnaceBlock.LIT, false), 3);
            }
        }

        furnace.setChanged();
    }

    // ====================================================================
    // ПРЕДВАРИТЕЛЬНОЕ ЗАПОЛНЕНИЕ ВОРОНОК ИЗ ИСТОЧНИКОВ
    // ====================================================================

    /**
     * Перемещает предметы из источников (бочек) в воронки.
     * Для топлива: бочка над воронкой → воронка
     * Для входа: бочка над воронкой → воронка
     */
    static void fillHoppersFromSources(ServerLevel level, BlockPos pos) {
        // Input hopper (над печью)
        BlockPos above = pos.above();
        if (level.isLoaded(above)) {
            BlockEntity be = level.getBlockEntity(above);
            if (be instanceof HopperBlockEntity hopper) {
                BlockPos aboveHopper = above.above();
                if (level.isLoaded(aboveHopper)) {
                    BlockEntity src = level.getBlockEntity(aboveHopper);
                    if (src instanceof Container source) {
                        transferToHopper(hopper, source);
                    }
                }
            }
        }

        // Fuel hoppers (с боков)
        for (Direction dir : Direction.Plane.HORIZONTAL) {
            BlockPos neighbor = pos.relative(dir);
            if (!level.isLoaded(neighbor)) continue;
            BlockEntity be = level.getBlockEntity(neighbor);
            if (be instanceof HopperBlockEntity hopper) {
                Direction facing = level.getBlockState(neighbor).getValue(HopperBlock.FACING);
                if (facing == dir.getOpposite()) {
                    BlockPos aboveHopper = neighbor.above();
                    if (level.isLoaded(aboveHopper)) {
                        BlockEntity src = level.getBlockEntity(aboveHopper);
                        if (src instanceof Container source) {
                            transferToHopper(hopper, source);
                        }
                    }
                }
            }
        }
    }

    /** Переносит предметы из source в hopper (по одному стаку на слот). */
    static void transferToHopper(HopperBlockEntity hopper, Container source) {
        for (int i = 0; i < source.getContainerSize(); i++) {
            ItemStack srcStack = source.getItem(i);
            if (srcStack.isEmpty()) continue;

            for (int j = 0; j < hopper.getContainerSize(); j++) {
                ItemStack destStack = hopper.getItem(j);
                int maxSlot = Math.min(srcStack.getMaxStackSize(), hopper.getMaxStackSize());

                if (destStack.isEmpty()) {
                    int transfer = Math.min(srcStack.getCount(), maxSlot);
                    hopper.setItem(j, new ItemStack(srcStack.getItem(), transfer));
                    srcStack.shrink(transfer);
                    if (srcStack.isEmpty()) source.setItem(i, ItemStack.EMPTY);
                    source.setChanged();
                    hopper.setChanged();
                    break;
                } else if (ItemStack.isSameItemSameTags(destStack, srcStack) && destStack.getCount() < maxSlot) {
                    int space = maxSlot - destStack.getCount();
                    int transfer = Math.min(space, srcStack.getCount());
                    destStack.grow(transfer);
                    srcStack.shrink(transfer);
                    if (srcStack.isEmpty()) source.setItem(i, ItemStack.EMPTY);
                    source.setChanged();
                    hopper.setChanged();
                    break;
                }
            }
        }
    }

    // ========================================================================
    // ========================================================================

    private static long min(long... values) {
        long m = Long.MAX_VALUE;
        for (long v : values) {
            if (v < m) m = v;
        }
        return m;
    }

    /** Направление воронки из BlockState. */
    private static Direction hopperFacing(Level level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        if (state.hasProperty(HopperBlock.FACING)) {
            return state.getValue(HopperBlock.FACING);
        }
        return Direction.DOWN;
    }

    /** Количество smeltable предметов в контейнере. */
    private static int countSmeltable(AbstractFurnaceBlockEntity furnace, Container container) {
        int total = 0;
        for (int i = 0; i < container.getContainerSize(); i++) {
            ItemStack s = container.getItem(i);
            if (VanillaHopperIO.isSmeltable(furnace, s)) {
                total += s.getCount();
            }
        }
        return total;
    }

    /** Количество горючих предметов в контейнере. */
    private static int countBurnable(Container container) {
        int total = 0;
        for (int i = 0; i < container.getContainerSize(); i++) {
            ItemStack s = container.getItem(i);
            if (ForgeHooks.getBurnTime(s, RecipeType.SMELTING) > 0) {
                total += s.getCount();
            }
        }
        return total;
    }

    /** Количество свободных слотов в контейнере (в предметах). */
    private static int countSpace(Container container) {
        int space = 0;
        for (int i = 0; i < container.getContainerSize(); i++) {
            ItemStack s = container.getItem(i);
            space += s.isEmpty() ? container.getMaxStackSize() : container.getMaxStackSize() - s.getCount();
        }
        return space;
    }

    // ---- ПОТРЕБЛЕНИЕ ----

    /** Вычитает топливо из боковых контейнеров. */
    static void consumeExternalFuel(ServerLevel level, BlockPos pos, int amount) {
        if (amount <= 0) return;
        for (Direction dir : Direction.Plane.HORIZONTAL) {
            if (amount <= 0) return;
            BlockPos n = pos.relative(dir);
            if (!level.isLoaded(n)) continue;
            BlockEntity be = level.getBlockEntity(n);

            if (be instanceof HopperBlockEntity hopper) {
                // Сначала из контейнера над воронкой
                BlockPos above = n.above();
                if (level.isLoaded(above)) {
                    BlockEntity src = level.getBlockEntity(above);
                    if (src instanceof Container c) {
                        amount = takeBurnable(c, amount);
                    }
                }
                // Потом из самой воронки
                amount = takeBurnable(hopper, amount);
                continue;
            }

            if (be instanceof Container c) {
                amount = takeBurnable(c, amount);
            }
        }
    }

    /** Вычитает smeltable предметы из контейнеров над печью. */
    static void consumeExternalInput(AbstractFurnaceBlockEntity furnace, ServerLevel level, BlockPos pos, int amount) {
        if (amount <= 0) return;
        BlockPos above = pos.above();
        if (!level.isLoaded(above)) return;
        BlockEntity be = level.getBlockEntity(above);

        if (be instanceof HopperBlockEntity hopper) {
            BlockPos aboveHopper = above.above();
            if (level.isLoaded(aboveHopper)) {
                BlockEntity src = level.getBlockEntity(aboveHopper);
                if (src instanceof Container c) {
                    amount = takeSmeltable(furnace, c, amount);
                }
            }
            takeSmeltable(furnace, hopper, amount);
            return;
        }

        if (be instanceof Container c) {
            takeSmeltable(furnace, c, amount);
        }
    }

    private static int takeBurnable(Container container, int amount) {
        if (amount <= 0) return 0;
        for (int i = 0; i < container.getContainerSize(); i++) {
            if (amount <= 0) break;
            ItemStack s = container.getItem(i);
            if (s.isEmpty()) continue;
            if (ForgeHooks.getBurnTime(s, RecipeType.SMELTING) <= 0) continue;
            int take = Math.min(amount, s.getCount());
            s.shrink(take);
            amount -= take;
        }
        return amount;
    }

    private static int takeSmeltable(AbstractFurnaceBlockEntity furnace, Container container, int amount) {
        if (amount <= 0) return 0;
        for (int i = 0; i < container.getContainerSize(); i++) {
            if (amount <= 0) break;
            ItemStack s = container.getItem(i);
            if (s.isEmpty()) continue;
            if (!VanillaHopperIO.isSmeltable(furnace, s)) continue;
            int take = Math.min(amount, s.getCount());
            s.shrink(take);
            amount -= take;
        }
        return amount;
    }

    // ---- ВЫТАЛКИВАНИЕ НА ВЫХОД ----

    /** Кладёт готовые предметы в контейнер под печью. */
    private static void pushOverflowToBelow(ServerLevel level, BlockPos pos, ItemStack template, int count) {
        if (count <= 0 || template.isEmpty()) return;
        BlockPos below = pos.below();
        if (!level.isLoaded(below)) return;
        BlockEntity be = level.getBlockEntity(below);
        if (!(be instanceof Container dest)) return;

        ItemStack stack = new ItemStack(template.getItem(), count);
        for (int i = 0; i < dest.getContainerSize(); i++) {
            if (stack.isEmpty()) break;
            ItemStack destStack = dest.getItem(i);
            int slotLimit = Math.min(stack.getMaxStackSize(), dest.getMaxStackSize());
            if (destStack.isEmpty()) {
                int transfer = Math.min(stack.getCount(), slotLimit);
                dest.setItem(i, new ItemStack(stack.getItem(), transfer));
                stack.shrink(transfer);
                dest.setChanged();
            } else if (ItemStack.isSameItemSameTags(destStack, stack) && destStack.getCount() < slotLimit) {
                int space = slotLimit - destStack.getCount();
                int transfer = Math.min(stack.getCount(), space);
                destStack.grow(transfer);
                stack.shrink(transfer);
                dest.setChanged();
            }
        }
    }
}
