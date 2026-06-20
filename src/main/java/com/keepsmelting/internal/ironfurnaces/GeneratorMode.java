package com.keepsmelting.internal.ironfurnaces;

import com.keepsmelting.internal.catchup.AbstractCatchupHandler;
import com.keepsmelting.mixin.ironfurnaces.IronFurnaceAccessor;
import ironfurnaces.tileentity.furnaces.BlockIronFurnaceTileBase;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

/**
 * Generator mode catchup + neighbor helpers.
 * Adaptive batch O(events).
 */
public class GeneratorMode {

    public static void apply(BlockIronFurnaceTileBase tile, long elapsed, Level level, BlockPos pos) {
        IronFurnaceAccessor acc = (IronFurnaceAccessor) tile;

        int energyBefore = tile.getEnergy();
        int fuelBefore = tile.inventory.get(6).getCount();
        long remaining = elapsed;

        while (remaining > 0) {
            if (tile.getEnergy() >= tile.getCapacity()) break;

            if (tile.generatorBurn <= 0.0) {
                ItemStack fuel = tile.inventory.get(6);
                if (fuel.isEmpty()) {
                    acc.invokeAutoIOGenerator();
                    fuel = tile.inventory.get(6);
                    if (fuel.isEmpty()) break;
                }

                tile.generatorBurn = tile.getGeneratorBurn();
                tile.generatorRecentRecipeRF = (int) tile.generatorBurn;

                if (fuel.hasCraftingRemainingItem()) {
                    tile.inventory.set(6, fuel.getCraftingRemainingItem());
                } else {
                    fuel.shrink(1);
                    if (fuel.isEmpty()) {
                        tile.inventory.set(6, fuel.getCraftingRemainingItem());
                    }
                }
                tile.setChanged();
            }

            if (tile.generatorBurn <= 0.0) break;

            int gen = tile.getGeneration();
            if (gen <= 0) break;

            long ticksThisBurn = (long) Math.ceil(tile.generatorBurn * 20.0 / gen);
            long ticksToCap = 0;
            int remainingCap = tile.getCapacity() - tile.getEnergy();
            if (remainingCap > 0) {
                ticksToCap = (long) Math.ceil((double) remainingCap / gen);
            }

            long batch = Math.min(remaining, Math.min(ticksThisBurn, ticksToCap));
            if (batch <= 0) batch = 1;

            double totalGen = gen * batch;
            double totalBurn = gen / 20.0 * batch;

            tile.gottenRF += totalGen;
            tile.setEnergy(tile.getEnergy() + (int) totalGen);
            tile.generatorBurn -= totalBurn;

            if (tile.generatorBurn <= 0.0) {
                double max = tile.generatorRecentRecipeRF * 20.0;
                if (tile.gottenRF + gen > max && tile.gottenRF + gen < tile.getCapacity()) {
                    int diff = (int) (tile.gottenRF + gen - max);
                    tile.setEnergy(tile.getEnergy() + gen);
                    tile.removeEnergy(diff);
                }
                if (tile.gottenRF + gen < max) {
                    int diff = (int) (max - tile.gottenRF + gen);
                    tile.setEnergy(tile.getEnergy() + gen);
                    tile.setEnergy(tile.getEnergy() + diff);
                }
                tile.gottenRF = 0.0;
                acc.invokeAutoIOGenerator();
                tile.generatorBurn = 0.0;
            }

            remaining -= batch;
            tile.setChanged();
        }

        if (tile.generatorBurn <= 0.0) {
            tile.generatorBurn = 0.0;
        }

        AbstractCatchupHandler.sendChatDebug(level, pos, "Generator", elapsed,
                fuelBefore - tile.inventory.get(6).getCount(),
                tile.getEnergy() - energyBefore,
                0, 0, tile.generatorBurn > 0.0);
    }

    /**
     * Догоняет соседние генераторы, которые отдают RF заводу.
     * В отличие от apply(), не упирается в capacity — избыток RF
     * передаётся соседнему заводу. Повторяет цикл генерация → передача,
     * пока не закончится elapsed-время.
     */
    public static void processNeighborGenerators(BlockIronFurnaceTileBase factoryTile, long elapsed,
                                                  Level level, BlockPos pos) {
        for (Direction dir : Direction.Plane.HORIZONTAL) {
            BlockPos neighbor = pos.relative(dir);
            if (!level.isLoaded(neighbor)) continue;
            BlockEntity be = level.getBlockEntity(neighbor);
            if (!(be instanceof BlockIronFurnaceTileBase genTile) || !genTile.isGenerator()) continue;

            // Проверяем, что генератор отдаёт RF на завод
            Direction genSide = dir.getOpposite();
            int setting = genTile.furnaceSettings.get(genSide.ordinal());
            if (setting != 2 && setting != 3) continue;

            IronFurnaceAccessor acc = (IronFurnaceAccessor) genTile;
            long remaining = elapsed;
            int fuelConsumed = 0;

            while (remaining > 0) {
                // Шаг 1: зажечь топливо если прогорело
                if (genTile.generatorBurn <= 0.0) {
                    ItemStack fuel = genTile.inventory.get(6);
                    if (fuel.isEmpty()) {
                        acc.invokeAutoIOGenerator();
                        fuel = genTile.inventory.get(6);
                        if (fuel.isEmpty()) break;
                    }
                    genTile.generatorBurn = genTile.getGeneratorBurn();
                    genTile.generatorRecentRecipeRF = (int) genTile.generatorBurn;
                    fuelConsumed++;
                    if (fuel.hasCraftingRemainingItem()) {
                        genTile.inventory.set(6, fuel.getCraftingRemainingItem());
                    } else {
                        fuel.shrink(1);
                        if (fuel.isEmpty()) {
                            genTile.inventory.set(6, fuel.getCraftingRemainingItem());
                        }
                    }
                    genTile.setChanged();
                }
                if (genTile.generatorBurn <= 0.0) break;

                int gen = genTile.getGeneration();
                if (gen <= 0) break;

                long ticksThisBurn = (long) Math.ceil(genTile.generatorBurn * 20.0 / gen);
                long batch = Math.min(remaining, ticksThisBurn);
                if (batch <= 0) batch = 1;

                double totalGen = gen * batch;
                double totalBurn = gen / 20.0 * batch;

                genTile.gottenRF += totalGen;
                genTile.generatorBurn -= totalBurn;

                // Шаг 2: наполнить генератор до capacity
                int genCap = genTile.getCapacity();
                int totalGenInt = (int) totalGen;
                int spaceInGen = genCap - genTile.getEnergy();

                if (spaceInGen >= totalGenInt) {
                    // Всё RF влезает в генератор
                    genTile.setEnergy(genTile.getEnergy() + totalGenInt);
                } else {
                    // Генератор забит, часть RF — заводу
                    if (spaceInGen > 0) {
                        genTile.setEnergy(genCap);
                    }
                    int toFactory = totalGenInt - Math.max(0, spaceInGen);
                    int factoryFree = factoryTile.getCapacity() - factoryTile.getEnergy();
                    if (factoryFree > 0 && toFactory > 0) {
                        int pushNow = Math.min(factoryFree, toFactory);
                        factoryTile.setEnergy(factoryTile.getEnergy() + pushNow);
                        factoryTile.setChanged();
                    }
                }

                if (genTile.generatorBurn <= 0.0) {
                    genTile.gottenRF = 0.0;
                    acc.invokeAutoIOGenerator();
                    genTile.generatorBurn = 0.0;
                }

                remaining -= batch;
                genTile.setChanged();
            }

            AbstractCatchupHandler.sendChatDebug(level, neighbor, "Generator", elapsed,
                    fuelConsumed,
                    genTile.getEnergy(),
                    0, 0, genTile.generatorBurn > 0.0);
        }
    }

    public static int pullAllRFFromNeighborGenerators(BlockIronFurnaceTileBase tile, Level level,
                                                       BlockPos pos) {
        int totalPulled = 0;
        for (Direction dir : Direction.Plane.HORIZONTAL) {
            BlockPos neighbor = pos.relative(dir);
            if (!level.isLoaded(neighbor)) continue;
            BlockEntity be = level.getBlockEntity(neighbor);
            if (be instanceof BlockIronFurnaceTileBase neighborTile && neighborTile.isGenerator()) {
                Direction genSide = dir.getOpposite();
                int setting = neighborTile.furnaceSettings.get(genSide.ordinal());
                if (setting != 2 && setting != 3) continue;
                int available = neighborTile.getEnergy();
                if (available <= 0) continue;
                neighborTile.removeEnergy(available);
                neighborTile.setChanged();
                tile.setEnergy(tile.getEnergy() + available);
                totalPulled += available;
            }
        }
        return totalPulled;
    }
}
