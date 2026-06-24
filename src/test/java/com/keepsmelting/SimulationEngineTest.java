package com.keepsmelting;

import com.keepsmelting.internal.ironfurnaces.data.SimulationData.SimulationResult;
import com.keepsmelting.internal.ironfurnaces.simulate.SimulationEngine;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for SimulationEngine — pure math, no Minecraft dependencies.
 *
 * NOTE: simulateNetwork() is NOT tested here because NetworkResources
 * requires FurnaceNetwork which depends on Minecraft runtime classes.
 * Test full-network logic via simulate() which accepts flat params.
 */
class SimulationEngineTest {

    // ========================================================================
    // simulateGeneratorOnly
    // ========================================================================

    @Test
    void simulateGeneratorOnly_noFuel_returnsEmpty() {
        SimulationResult r = SimulationEngine.simulateGeneratorOnly(0, 1600, 10, 10000, 0, 100);
        assertEquals(0, r.fuelToBurn);
        assertEquals(0, r.rfForGenerators);
        assertEquals(0, r.effectiveTicks);
    }

    @Test
    void simulateGeneratorOnly_fullCapacity_returnsEmpty() {
        SimulationResult r = SimulationEngine.simulateGeneratorOnly(64, 1600, 10, 10000, 10000, 100);
        assertEquals(0, r.fuelToBurn);
        assertEquals(0, r.rfForGenerators);
    }

    @Test
    void simulateGeneratorOnly_fillsBuffer() {
        // 1 уголь (1600 ticks) * 10 RF/tick = 16000 RF max
        // Буфер пустой, ёмкость 10000 → заполнится 10000 RF
        SimulationResult r = SimulationEngine.simulateGeneratorOnly(1, 1600, 10, 10000, 0, 2000);
        assertTrue(r.fuelToBurn > 0);
        assertEquals(10000, r.rfForGenerators);
        assertTrue(r.effectiveTicks > 0);
    }

    @Test
    void simulateGeneratorOnly_limitedByElapsedTicks() {
        // 64 угля * 1600 = 102400 ticks, но elapsed всего 100
        SimulationResult r = SimulationEngine.simulateGeneratorOnly(64, 1600, 10, 100000, 0, 100);
        assertEquals(1, r.fuelToBurn); // 100/1600 = 0.0625 → ceil = 1
        assertTrue(r.rfForGenerators > 0);
    }

    @Test
    void simulateGeneratorOnly_limitedByCapacity() {
        // 5 угля * 1600 = 8000 ticks → 80000 RF max
        // Буфер 50000 — заполнится на 50000, остальное не сгорит
        SimulationResult r = SimulationEngine.simulateGeneratorOnly(5, 1600, 10, 50000, 0, 10000);
        assertEquals(4, r.fuelToBurn); // 50000/10=5000 ticks → 5000/1600=3.125 → ceil=4
        assertEquals(50000, r.rfForGenerators);
        assertEquals(5000, r.effectiveTicks);
    }

    // ========================================================================
    // simulateFactoryOnly
    // ========================================================================

    @Test
    void simulateFactoryOnly_noItems_returnsEmpty() {
        SimulationResult r = SimulationEngine.simulateFactoryOnly(0, 64, 10, 5, 100, 1000, 100);
        assertEquals(0, r.itemsToSmelt);
    }

    @Test
    void simulateFactoryOnly_noRF_returnsEmpty() {
        SimulationResult r = SimulationEngine.simulateFactoryOnly(64, 64, 10, 5, 100, 0, 100);
        assertEquals(0, r.itemsToSmelt);
    }

    @Test
    void simulateFactoryOnly_smeltsItems() {
        // 64 items, 1000 RF, 10 RF/item → 100 items possible, capped at 64
        SimulationResult r = SimulationEngine.simulateFactoryOnly(64, 64, 10, 5, 100, 1000, 1000);
        assertTrue(r.itemsToSmelt > 0);
        assertEquals(r.itemsToSmelt * 10, r.rfForFactory);
    }

    @Test
    void simulateFactoryOnly_limitedByRF() {
        // 10 RF, 10 RF per item → 1 item
        SimulationResult r = SimulationEngine.simulateFactoryOnly(64, 64, 10, 5, 100, 10, 1000);
        assertEquals(1, r.itemsToSmelt);
    }

    @Test
    void simulateFactoryOnly_limitedByOutputSpace() {
        // 64 items, enough RF, but outputSpace = 5
        SimulationResult r = SimulationEngine.simulateFactoryOnly(64, 5, 10, 5, 100, 10000, 1000);
        assertTrue(r.itemsToSmelt <= 5);
    }

    // ========================================================================
    // simulate (full network — flat params, no Minecraft deps)
    // ========================================================================

    @Test
    void simulate_basicGeneratorAndFactory() {
        SimulationResult r = SimulationEngine.simulate(
                10, 1600, 10,   // fuel, burnTicks, rfPerTick
                32, 64, 10,     // smeltable, outputSpace, rfPerItem
                5, 100,          // factory rfPerTick, cookTime
                10000, 0,       // gen capacity, gen current RF
                5000, 0,        // factory capacity, factory current RF
                1000            // elapsed ticks
        );
        assertTrue(r.fuelToBurn > 0);
        assertTrue(r.itemsToSmelt > 0);
        assertTrue(r.rfForFactory > 0);
        assertTrue(r.effectiveTicks > 0);
    }

    @Test
    void simulate_zeroFuel_returnsEmpty() {
        SimulationResult r = SimulationEngine.simulate(
                0, 1600, 10,
                32, 64, 10,
                5, 100,
                10000, 0,
                5000, 0,
                1000
        );
        assertEquals(0, r.fuelToBurn);
        assertEquals(0, r.itemsToSmelt);
        assertEquals(0, r.rfForFactory);
        assertEquals(0, r.rfForGenerators);
    }

    @Test
    void simulate_genPoolFeedsFactory() {
        // Lots of fuel + factory items → verify RF routing
        SimulationResult r = SimulationEngine.simulate(
                100, 1600, 10,
                64, 200, 10,
                5, 100,
                50000, 0,
                50000, 0,
                10000
        );
        assertTrue(r.fuelToBurn > 0, "should burn fuel");
        assertTrue(r.itemsToSmelt > 0, "should smelt items");
        // Remaining RF should go to gen storage or factory storage
        assertTrue(r.rfForGenerators > 0 || r.rfForFactoryStorage > 0,
                "excess RF should go to storage");
    }

    @Test
    void simulate_excessRFGoesToStorage() {
        // Factory consumes less than produced → RF overflows to gen/factory storage
        // 10 fuel * 1600 * 10 = 160000 RF from fuel
        // Factory: 1 item * 10 = 10 RF consumed
        SimulationResult r = SimulationEngine.simulate(
                10, 1600, 10,
                1, 200, 10,
                5, 100,
                50000, 0,
                50000, 0,
                10000
        );
        assertTrue(r.fuelToBurn > 0);
        assertEquals(1, r.itemsToSmelt);
        assertEquals(10, r.rfForFactory);
        assertTrue(r.rfForGenerators > 0, "excess should fill gen storage");
    }
}
