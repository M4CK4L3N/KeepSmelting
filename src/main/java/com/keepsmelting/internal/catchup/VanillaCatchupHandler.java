package com.keepsmelting.internal.catchup;

import com.keepsmelting.KeepSmelting;
import com.keepsmelting.KeepSmeltingConfig;
import com.keepsmelting.api.catchup.AbstractCatchupHandler;
import com.keepsmelting.mixin.IFurnaceAccessor;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.AbstractFurnaceBlock;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.entity.HopperBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.common.ForgeHooks;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Optional;

public class VanillaCatchupHandler extends AbstractCatchupHandler {

    public static final VanillaCatchupHandler INSTANCE = new VanillaCatchupHandler();

    @Override
    public void applyCatchup(BlockEntity tile, long elapsed, Level level, BlockPos pos) {
        if (!(level instanceof ServerLevel serverLevel)) return;
        AbstractFurnaceBlockEntity furnace = (AbstractFurnaceBlockEntity) tile;
        IFurnaceAccessor acc = (IFurnaceAccessor) furnace;
        if (!acc.callIsLit()) return;

        NonNullList<ItemStack> items = acc.getItems();
        int cookingProgressBefore = acc.getCookingProgress();

        // ====================================================================
        // PHASE 1: COLLECT
        // ====================================================================

        // Выталкиваем готовое вниз
        pushToDest(furnace, serverLevel, pos);

        int fuelInFurnace = items.get(1).getCount();
        int fuelInContainers = countExternalFuel(serverLevel, pos);
        int totalFuelItems = fuelInFurnace + fuelInContainers;

        int inputInFurnace = items.get(0).getCount();
        int inputInContainers = countExternalInput(furnace, serverLevel, pos);
        int totalInputItems = inputInFurnace + inputInContainers;

        int outputSpace = countOutputSpace(furnace, serverLevel, pos);

        Recipe<?> recipe = findRecipe(furnace, level);
        if (recipe == null) return;
        int cookTotal = acc.getCookingTotalTime();
        if (cookTotal <= 0) return;
        int litDur = acc.getLitDuration();
        if (litDur <= 0) return;

        // ====================================================================
        // PHASE 2: SIMULATE
        // ====================================================================

        int litTime = acc.getLitTime();
        long totalBurnTicks = (long) totalFuelItems * litDur + (litTime > 0 ? litTime : 0);
        long actualTicks = Math.min(elapsed, totalBurnTicks);

        // Максимум по времени
        int maxByTime = 0;
        if (cookTotal > 0) {
            int progressNeeded = cookTotal - cookingProgressBefore;
            if (actualTicks >= progressNeeded) {
                long leftover = actualTicks - progressNeeded;
                maxByTime = 1 + (int) (leftover / cookTotal);
            }
        }

        int itemsToCook = Math.min(maxByTime, Math.min(totalInputItems, outputSpace));

        // ====================================================================
        // PHASE 3: APPLY
        // ====================================================================

        // Топливо
        int fuelConsumed = 0;
        if (itemsToCook > 0 && litDur > 0) {
            long ticksNeeded = (long) itemsToCook * cookTotal - cookingProgressBefore;
            if (ticksNeeded < 0) ticksNeeded = 0;
            fuelConsumed = Math.min(totalFuelItems, (int) Math.ceil((double) ticksNeeded / litDur));
        }

        int fuelRemaining = fuelConsumed;
        if (fuelRemaining > 0 && fuelInFurnace > 0) {
            int take = Math.min(fuelRemaining, fuelInFurnace);
            items.get(1).shrink(take);
            fuelRemaining -= take;
        }
        if (fuelRemaining > 0) {
            consumeExternalFuel(serverLevel, pos, fuelRemaining);
        }

        // Вход
        int inputRemaining = itemsToCook;
        if (inputRemaining > 0 && inputInFurnace > 0) {
            int take = Math.min(inputRemaining, inputInFurnace);
            items.get(0).shrink(take);
            inputRemaining -= take;
        }
        if (inputRemaining > 0) {
            consumeExternalInput(furnace, serverLevel, pos, inputRemaining);
        }

        // Прогресс
        if (itemsToCook > 0) {
            long leftover = actualTicks - (long) itemsToCook * cookTotal;
            if (leftover < 0) leftover = 0;
            acc.setCookingProgress((int) Math.min(Math.max(0, leftover), cookTotal - 1));
        } else if (actualTicks < cookTotal - cookingProgressBefore) {
            acc.setCookingProgress(cookingProgressBefore + (int) actualTicks);
        }

        // Выход — кладём максимум 64 в слот 2, остальное сразу ниже
        if (itemsToCook > 0) {
            ItemStack resultTemplate = recipe.getResultItem(level.registryAccess());
            if (resultTemplate.isEmpty()) resultTemplate = new ItemStack(net.minecraft.world.item.Items.IRON_INGOT);

            int maxStack = furnace.getMaxStackSize();
            int remaining = itemsToCook;

            // В слот 2 печи
            ItemStack output = items.get(2);
            if (output.isEmpty()) {
                int place = Math.min(remaining, maxStack);
                items.set(2, new ItemStack(resultTemplate.getItem(), place));
                remaining -= place;
            } else {
                int space = maxStack - output.getCount();
                if (space > 0) {
                    int place = Math.min(remaining, space);
                    output.grow(place);
                    remaining -= place;
                }
            }

            // Остаток сразу в контейнер снизу
            if (remaining > 0) {
                ItemStack overflow = new ItemStack(resultTemplate.getItem(), remaining);
                pushOverflowToDest(serverLevel, pos, overflow);
            }
        }

        // Lit status
        long totalBurnUsed = (long) fuelConsumed * litDur;
        if (totalBurnUsed >= totalBurnTicks) {
            acc.setLitTime(0);
            BlockState bs = level.getBlockState(pos);
            if (bs.hasProperty(AbstractFurnaceBlock.LIT)) {
                level.setBlock(pos, bs.setValue(AbstractFurnaceBlock.LIT, false), 3);
            }
        } else if (itemsToCook > 0) {
            acc.setLitTime(litDur);
        }

        furnace.setChanged();

        // Выталкиваем что осталось
        pushToDest(furnace, serverLevel, pos);

        // ====================================================================
        // DEBUG
        // ====================================================================
        if (KeepSmeltingConfig.COMMON.debugMode.get() != KeepSmeltingConfig.DebugMode.OFF) {
            boolean litStatus = acc.callIsLit();
            String debugInfo = String.format(
                    "§7[§6VanillaCatchup§7] §f%s §e%d§7t " +
                            "§7| §6cook§f%d §7| §6fuel§f%d(inF=%d,ext=%d) §7| §6input§f%d(inF=%d,ext=%d)" +
                            " §7| §6outSpace§f%d §7| §6maxByTime§f%d §7| §6burn§f%d §7| §6lit=%s",
                    pos.toShortString(), elapsed,
                    itemsToCook,
                    totalFuelItems, fuelInFurnace, fuelInContainers,
                    totalInputItems, inputInFurnace, inputInContainers,
                    outputSpace, maxByTime, (int) actualTicks,
                    litStatus
            );
            if (KeepSmeltingConfig.COMMON.debugMode.get() == KeepSmeltingConfig.DebugMode.LOG) {
                KeepSmelting.LOGGER.info(debugInfo.replace("§.", ""));
            }
            sendToNearbyPlayers(serverLevel, pos, Component.literal(debugInfo));
        }
    }

    // ========================================================================
    // COLLECT
    // ========================================================================

    private int countExternalFuel(ServerLevel level, BlockPos pos) {
        int total = 0;
        for (Direction dir : Direction.Plane.HORIZONTAL) {
            BlockPos n = pos.relative(dir);
            if (!level.isLoaded(n)) continue;
            BlockEntity be = level.getBlockEntity(n);
            // Если воронка — считаем и саму воронку, и контейнер над ней
            if (be instanceof HopperBlockEntity hopper) {
                for (int i = 0; i < hopper.getContainerSize(); i++) {
                    ItemStack s = hopper.getItem(i);
                    if (ForgeHooks.getBurnTime(s, RecipeType.SMELTING) > 0) total += s.getCount();
                }
                BlockPos above = n.above();
                if (level.isLoaded(above)) {
                    BlockEntity hbe = level.getBlockEntity(above);
                    if (hbe instanceof Container c) {
                        for (int i = 0; i < c.getContainerSize(); i++) {
                            ItemStack s = c.getItem(i);
                            if (ForgeHooks.getBurnTime(s, RecipeType.SMELTING) > 0) total += s.getCount();
                        }
                    }
                }
                continue;
            }
            if (be instanceof Container c) {
                for (int i = 0; i < c.getContainerSize(); i++) {
                    ItemStack s = c.getItem(i);
                    if (ForgeHooks.getBurnTime(s, RecipeType.SMELTING) > 0) total += s.getCount();
                }
            }
        }
        return total;
    }

    /** Считает smeltable из бочки и воронки (если есть воронка над печью) */
    private int countExternalInput(AbstractFurnaceBlockEntity furnace, ServerLevel level, BlockPos pos) {
        int total = 0;
        BlockPos above = pos.above();
        if (!level.isLoaded(above)) return 0;
        BlockEntity be = level.getBlockEntity(above);

        // Если над печью воронка — считаем и воронку, и контейнер над ней
        if (be instanceof HopperBlockEntity hopper) {
            // Сама воронка
            for (int i = 0; i < hopper.getContainerSize(); i++) {
                ItemStack s = hopper.getItem(i);
                if (isSmeltable(furnace, s)) total += s.getCount();
            }
            // Контейнер над воронкой
            BlockPos aboveHopper = above.above();
            if (level.isLoaded(aboveHopper)) {
                BlockEntity hbe = level.getBlockEntity(aboveHopper);
                if (hbe instanceof Container c) {
                    for (int i = 0; i < c.getContainerSize(); i++) {
                        ItemStack s = c.getItem(i);
                        if (isSmeltable(furnace, s)) total += s.getCount();
                    }
                }
            }
            return total;
        }

        // Прямой контейнер
        if (be instanceof Container c) {
            for (int i = 0; i < c.getContainerSize(); i++) {
                ItemStack s = c.getItem(i);
                if (isSmeltable(furnace, s)) total += s.getCount();
            }
        }
        return total;
    }

    /** Считает свободное место: печь (слот 2) + контейнер снизу + если это воронка — контейнер ниже воронки */
    private int countOutputSpace(AbstractFurnaceBlockEntity furnace, ServerLevel level, BlockPos pos) {
        int space = 0;
        ItemStack cur = furnace.getItem(2);
        space += cur.isEmpty() ? furnace.getMaxStackSize() : furnace.getMaxStackSize() - cur.getCount();

        BlockPos below = pos.below();
        if (!level.isLoaded(below)) return space;
        BlockEntity be = level.getBlockEntity(below);

        if (be instanceof HopperBlockEntity hopper) {
            // Воронка
            for (int i = 0; i < hopper.getContainerSize(); i++) {
                ItemStack s = hopper.getItem(i);
                space += s.isEmpty() ? 64 : s.getMaxStackSize() - s.getCount();
            }
            // И контейнер под воронкой
            BlockPos belowHopper = below.below();
            if (level.isLoaded(belowHopper)) {
                BlockEntity hbe = level.getBlockEntity(belowHopper);
                if (hbe instanceof Container c) {
                    for (int i = 0; i < c.getContainerSize(); i++) {
                        ItemStack s = c.getItem(i);
                        space += s.isEmpty() ? 64 : s.getMaxStackSize() - s.getCount();
                    }
                }
            }
            return space;
        }

        if (be instanceof Container c) {
            for (int i = 0; i < c.getContainerSize(); i++) {
                ItemStack s = c.getItem(i);
                space += s.isEmpty() ? 64 : s.getMaxStackSize() - s.getCount();
            }
        }
        return space;
    }

    @Nullable
    private Container resolveContainer(BlockEntity be, ServerLevel level, BlockPos pos) {
        if (be instanceof HopperBlockEntity) {
            BlockPos above = pos.above();
            if (level.isLoaded(above)) {
                BlockEntity ab = level.getBlockEntity(above);
                if (ab instanceof Container c) return c;
            }
            return (Container) be;
        }
        if (be instanceof Container c) return c;
        return null;
    }

    // ========================================================================
    // CONSUME
    // ========================================================================

    private void consumeExternalFuel(ServerLevel level, BlockPos pos, int amount) {
        for (Direction dir : Direction.Plane.HORIZONTAL) {
            if (amount <= 0) return;
            BlockPos n = pos.relative(dir);
            if (!level.isLoaded(n)) continue;
            BlockEntity be = level.getBlockEntity(n);

            if (be instanceof HopperBlockEntity hopper) {
                // Сначала из бочки над воронкой
                BlockPos above = n.above();
                if (level.isLoaded(above)) {
                    BlockEntity hbe = level.getBlockEntity(above);
                    if (hbe instanceof Container c) {
                        for (int i = 0; i < c.getContainerSize(); i++) {
                            if (amount <= 0) return;
                            ItemStack s = c.getItem(i);
                            if (s.isEmpty()) continue;
                            if (ForgeHooks.getBurnTime(s, RecipeType.SMELTING) <= 0) continue;
                            int take = Math.min(amount, s.getCount());
                            s.shrink(take);
                            amount -= take;
                        }
                    }
                }
                // Потом из самой воронки
                for (int i = 0; i < hopper.getContainerSize(); i++) {
                    if (amount <= 0) return;
                    ItemStack s = hopper.getItem(i);
                    if (s.isEmpty()) continue;
                    if (ForgeHooks.getBurnTime(s, RecipeType.SMELTING) <= 0) continue;
                    int take = Math.min(amount, s.getCount());
                    s.shrink(take);
                    amount -= take;
                }
                continue;
            }

            if (be instanceof Container c) {
                for (int i = 0; i < c.getContainerSize(); i++) {
                    if (amount <= 0) return;
                    ItemStack s = c.getItem(i);
                    if (s.isEmpty()) continue;
                    if (ForgeHooks.getBurnTime(s, RecipeType.SMELTING) <= 0) continue;
                    int take = Math.min(amount, s.getCount());
                    s.shrink(take);
                    amount -= take;
                }
            }
        }
    }

    private void consumeExternalInput(AbstractFurnaceBlockEntity furnace, ServerLevel level, BlockPos pos, int amount) {
        BlockPos above = pos.above();
        if (!level.isLoaded(above) || amount <= 0) return;
        BlockEntity be = level.getBlockEntity(above);

        if (be instanceof HopperBlockEntity hopper) {
            // Сначала из бочки, потом из воронки
            BlockPos aboveHopper = above.above();
            if (level.isLoaded(aboveHopper)) {
                BlockEntity hbe = level.getBlockEntity(aboveHopper);
                if (hbe instanceof Container c) {
                    for (int i = 0; i < c.getContainerSize(); i++) {
                        if (amount <= 0) return;
                        ItemStack s = c.getItem(i);
                        if (s.isEmpty()) continue;
                        if (!isSmeltable(furnace, s)) continue;
                        int take = Math.min(amount, s.getCount());
                        s.shrink(take);
                        amount -= take;
                    }
                }
            }
            // Потом из самой воронки
            for (int i = 0; i < hopper.getContainerSize(); i++) {
                if (amount <= 0) return;
                ItemStack s = hopper.getItem(i);
                if (s.isEmpty()) continue;
                if (!isSmeltable(furnace, s)) continue;
                int take = Math.min(amount, s.getCount());
                s.shrink(take);
                amount -= take;
            }
            return;
        }

        // Прямой контейнер
        if (be instanceof Container c) {
            for (int i = 0; i < c.getContainerSize(); i++) {
                if (amount <= 0) return;
                ItemStack s = c.getItem(i);
                if (s.isEmpty()) continue;
                if (!isSmeltable(furnace, s)) continue;
                int take = Math.min(amount, s.getCount());
                s.shrink(take);
                amount -= take;
            }
        }
    }

    // ========================================================================
    // OUTPUT PUSH
    // ========================================================================

    private void pushToDest(AbstractFurnaceBlockEntity furnace, ServerLevel level, BlockPos pos) {
        ItemStack output = furnace.getItem(2);
        if (output.isEmpty()) return;
        BlockPos below = pos.below();
        if (!level.isLoaded(below)) return;
        BlockEntity be = level.getBlockEntity(below);
        if (!(be instanceof Container dest)) return;
        pushIntoContainer(dest, output);
        if (output.isEmpty()) furnace.setItem(2, ItemStack.EMPTY);
        furnace.setChanged();
    }

    private void pushOverflowToDest(ServerLevel level, BlockPos pos, ItemStack overflow) {
        if (overflow.isEmpty()) return;
        BlockPos below = pos.below();
        if (!level.isLoaded(below)) return;
        BlockEntity be = level.getBlockEntity(below);
        if (!(be instanceof Container dest)) return;
        pushIntoContainer(dest, overflow);
    }

    private void pushIntoContainer(Container container, ItemStack source) {
        if (source.isEmpty()) return;
        for (int i = 0; i < container.getContainerSize(); i++) {
            if (source.isEmpty()) break;
            ItemStack dest = container.getItem(i);
            int maxSlot = Math.min(source.getMaxStackSize(), container.getMaxStackSize());
            if (dest.isEmpty()) {
                int transfer = Math.min(source.getCount(), maxSlot);
                ItemStack put = source.copy();
                put.setCount(transfer);
                source.shrink(transfer);
                container.setItem(i, put);
                container.setChanged();
            } else if (ItemStack.isSameItemSameTags(dest, source) && dest.getCount() < maxSlot) {
                int space = maxSlot - dest.getCount();
                int transfer = Math.min(space, source.getCount());
                dest.grow(transfer);
                source.shrink(transfer);
                container.setChanged();
            }
        }
    }

    // ========================================================================
    // RECIPE
    // ========================================================================

    @Nullable
    @SuppressWarnings({"unchecked", "rawtypes"})
    private Recipe<?> findRecipe(AbstractFurnaceBlockEntity furnace, Level level) {
        RecipeType rt = (RecipeType) recipeTypeFor(furnace);
        Optional opt = level.getRecipeManager().getRecipeFor(rt, (Container) furnace, level);
        return (Recipe<?>) opt.orElse(null);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private boolean isSmeltable(AbstractFurnaceBlockEntity furnace, ItemStack stack) {
        if (stack.isEmpty()) return false;
        Level lvl = furnace.getLevel();
        if (lvl == null) return false;
        RecipeType rt = (RecipeType) recipeTypeFor(furnace);
        SimpleContainer inv = new SimpleContainer(stack);
        return lvl.getRecipeManager().getRecipeFor(rt, inv, lvl).isPresent();
    }

    private RecipeType<?> recipeTypeFor(AbstractFurnaceBlockEntity furnace) {
        BlockEntityType<?> t = furnace.getType();
        if (t == BlockEntityType.SMOKER) return RecipeType.SMOKING;
        if (t == BlockEntityType.BLAST_FURNACE) return RecipeType.BLASTING;
        return RecipeType.SMELTING;
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
