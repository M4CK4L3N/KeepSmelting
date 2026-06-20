package com.keepsmelting.internal.ironfurnaces;

import ironfurnaces.tileentity.furnaces.BlockIronFurnaceTileBase;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Сеть связанных печей Iron Furnaces.
 * Discovery находит все печи рядом — и Factory, и Generator.
 */
public class FurnaceNetwork {

    public final List<BlockIronFurnaceTileBase> generators = new ArrayList<>();
    public final List<BlockIronFurnaceTileBase> factories = new ArrayList<>();

    private final Set<BlockPos> visited = new HashSet<>();

    /**
     * Собирает сеть: все Iron Furnaces печи рядом (BFS глубина 1).
     */
    public boolean discover(BlockIronFurnaceTileBase start, Level level) {
        visited.clear();
        generators.clear();
        factories.clear();

        com.keepsmelting.KeepSmelting.LOGGER.info(
                "[Network] Discover starting from {} pos={}",
                start.isFactory() ? "Factory" : start.isGenerator() ? "Generator" : "Furnace",
                start.getBlockPos().toShortString());

        List<BlockIronFurnaceTileBase> queue = new ArrayList<>();
        queue.add(start);

        while (!queue.isEmpty()) {
            BlockIronFurnaceTileBase current = queue.remove(0);
            if (!visited.add(current.getBlockPos())) continue;

            if (current.isGenerator()) {
                generators.add(current);
                com.keepsmelting.KeepSmelting.LOGGER.info("[Network] Found Generator at {} fuel={} curRF={}/{}",
                        current.getBlockPos().toShortString(),
                        current.inventory.get(6).getCount(),
                        current.getEnergy(), current.getCapacity());
            }
            if (current.isFactory()) {
                factories.add(current);
                com.keepsmelting.KeepSmelting.LOGGER.info("[Network] Found Factory at {} curRF={}/{}",
                        current.getBlockPos().toShortString(),
                        current.getEnergy(), current.getCapacity());
            }

            for (Direction dir : Direction.values()) {
                BlockPos neighborPos = current.getBlockPos().relative(dir);
                if (visited.contains(neighborPos)) continue;
                if (!level.isLoaded(neighborPos)) continue;

                BlockEntity be = level.getBlockEntity(neighborPos);
                if (!(be instanceof BlockIronFurnaceTileBase neighbor)) continue;

                int curSetting = current.furnaceSettings.get(dir.ordinal());
                int nebSetting = neighbor.furnaceSettings.get(dir.getOpposite().ordinal());

                String curType = current.isFactory() ? "Fact" : current.isGenerator() ? "Gen" : "Furn";
                String nebType = neighbor.isFactory() ? "Fact" : neighbor.isGenerator() ? "Gen" : "Furn";

                com.keepsmelting.KeepSmelting.LOGGER.info(
                        "[Network]  {}→{}: cur={} side={} → neb={} side={}",
                        curType, nebType,
                        curSetting, dir, nebSetting, dir.getOpposite());

                // Соединение есть, если хотя бы одна сторона настроена на обмен
                boolean connected = false;

                // RF output: setting 2 или 3 на стороне генератора
                if (neighbor.isGenerator() && (nebSetting == 2 || nebSetting == 3)) connected = true;
                if (current.isGenerator() && (curSetting == 2 || curSetting == 3)) connected = true;

                // Item IO: setting 1, 2 или 3 на стороне Factory
                if (neighbor.isFactory() && (nebSetting == 1 || nebSetting == 2 || nebSetting == 3)) connected = true;
                if (current.isFactory() && (curSetting == 1 || curSetting == 2 || curSetting == 3)) connected = true;

                if (connected) {
                    com.keepsmelting.KeepSmelting.LOGGER.info("[Network]  → CONNECTED");
                    queue.add(neighbor);
                }
            }
        }

        com.keepsmelting.KeepSmelting.LOGGER.info("[Network] Done: {} gen(s), {} fact(s)", generators.size(), factories.size());
        return !generators.isEmpty() || !factories.isEmpty();
    }

    public boolean hasFactories() { return !factories.isEmpty(); }
    public boolean hasGenerators() { return !generators.isEmpty(); }
    public int size() { return generators.size() + factories.size(); }
}
