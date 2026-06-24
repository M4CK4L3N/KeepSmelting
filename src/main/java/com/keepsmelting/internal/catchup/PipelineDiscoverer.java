package com.keepsmelting.internal.catchup;

import com.keepsmelting.internal.catchup.PipelineData.*;
import com.keepsmelting.mixin.IFurnaceAccessor;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
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
 * Phase 1: Pipeline discovery — scans BlockEntity around the furnace
 * and builds a {@link Pipeline} describing the hopper chain.
 */
public class PipelineDiscoverer {

    private PipelineDiscoverer() {}

    /**
     * Строит {@link Pipeline}, сканируя BlockEntity вокруг печи.
     */
    public static Pipeline discover(
            ServerLevel level,
            BlockPos furnacePos,
            AbstractFurnaceBlockEntity furnace,
            @Nullable Recipe<?> recipe
    ) {
        List<PipelineNode> nodes = new ArrayList<>();
        var items = ((IFurnaceAccessor) furnace).getItems();
        int inputItemTotal = items.get(0).getCount();
        int fuelItemTotal = items.get(1).getCount();
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
                    BlockPos aboveHopper = above.above();
                    if (level.isLoaded(aboveHopper)) {
                        BlockEntity src = level.getBlockEntity(aboveHopper);
                        if (src instanceof Container c) {
                            inputItemTotal += countSmeltable(furnace, c);
                            nodes.add(new PipelineNode(NodeType.INPUT_SOURCE, aboveHopper, null, 0));
                        }
                    }
                    nodes.add(new PipelineNode(NodeType.INPUT_HOPPER, above, facing, PipelineData.HOPPER_TICKS_PER_ITEM));
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
                if (facing == dir.getOpposite()) {
                    fuelHopperCount++;
                    fuelItemTotal += countBurnable(hopper);
                    BlockPos aboveHopper = neighbor.above();
                    if (level.isLoaded(aboveHopper)) {
                        BlockEntity src = level.getBlockEntity(aboveHopper);
                        if (src instanceof Container c) {
                            fuelItemTotal += countBurnable(c);
                            nodes.add(new PipelineNode(NodeType.FUEL_SOURCE, aboveHopper, null, 0));
                        }
                    }
                    nodes.add(new PipelineNode(NodeType.FUEL_HOPPER, neighbor, facing, PipelineData.HOPPER_TICKS_PER_ITEM));
                }
            } else if (be instanceof Container c) {
                fuelItemTotal += countBurnable(c);
                nodes.add(new PipelineNode(NodeType.FUEL_SOURCE, neighbor, null, 0));
            }
        }

        // ---- OUTPUT (под печью) ----
        ItemStack outputStack = furnace.getItem(2);
        int maxStack = furnace.getMaxStackSize();
        outputSlotSpace += outputStack.isEmpty() ? maxStack : maxStack - outputStack.getCount();

        BlockPos below = furnacePos.below();
        if (level.isLoaded(below)) {
            BlockEntity be = level.getBlockEntity(below);
            if (be instanceof HopperBlockEntity hopper) {
                outputHopperCount = 1;
                outputSlotSpace += countSpace(hopper);
                BlockPos belowHopper = below.below();
                if (level.isLoaded(belowHopper)) {
                    BlockEntity dest = level.getBlockEntity(belowHopper);
                    if (dest instanceof Container c) {
                        outputSlotSpace += countSpace(c);
                        nodes.add(new PipelineNode(NodeType.OUTPUT_DEST, belowHopper, null, 0));
                    }
                }
                nodes.add(new PipelineNode(NodeType.OUTPUT_HOPPER, below,
                        hopperFacing(level, below), PipelineData.HOPPER_TICKS_PER_ITEM));
            } else if (be instanceof Container c) {
                outputSlotSpace += countSpace(c);
                nodes.add(new PipelineNode(NodeType.OUTPUT_DEST, below, null, 0));
            }
        }

        return new Pipeline(nodes,
                inputItemTotal, fuelItemTotal, outputSlotSpace,
                inputHopperCount, fuelHopperCount, outputHopperCount);
    }

    // ---- HELPERS ----

    /** Направление воронки из BlockState. */
    static Direction hopperFacing(Level level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        if (state.hasProperty(HopperBlock.FACING)) {
            return state.getValue(HopperBlock.FACING);
        }
        return Direction.DOWN;
    }

    /** Количество smeltable предметов в контейнере. */
    static int countSmeltable(AbstractFurnaceBlockEntity furnace, Container container) {
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
    static int countBurnable(Container container) {
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
    static int countSpace(Container container) {
        int space = 0;
        for (int i = 0; i < container.getContainerSize(); i++) {
            ItemStack s = container.getItem(i);
            space += s.isEmpty() ? container.getMaxStackSize() : container.getMaxStackSize() - s.getCount();
        }
        return space;
    }
}
