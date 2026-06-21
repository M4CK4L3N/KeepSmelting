# KeepSmelting API

## For Mod Developers

KeepSmelting provides an API for third-party mods to get catchup processing for custom furnaces.

## Gradle Dependency

```gradle
repositories {
    maven {
        url "https://cursemaven.com"
    }
}

dependencies {
    implementation fg.deobf("curse.maven:keepsmelting:XXXXXXXX")
}
```

## Quick Start

### Step 1: Extend AbstractCatchupHandler

```java
package com.example;

import com.keepsmelting.api.catchup.AbstractCatchupHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

public class MyFurnaceHandler extends AbstractCatchupHandler {
    @Override
    public void applyCatchup(BlockEntity tile, long elapsed, Level level, BlockPos pos) {
        // YOUR CATCHUP LOGIC HERE
        // 'elapsed' — ticks since last processing
    }
}
```

### Step 2: Register

```java
import com.keepsmelting.api.CatchupHandlerRegistry;

@Mod("example")
public class ExampleMod {
    public ExampleMod() {
        CatchupHandlerRegistry.register(
            MyFurnaceTileEntity.class,
            new MyFurnaceHandler()
        );
    }
}
```

### Step 3: Done

KeepSmelting calls `applyCatchup()` automatically when the furnace ticks.

## What AbstractCatchupHandler provides

| Method | Purpose |
|--------|---------|
| `calcElapsed(level, now)` | Calculate missed ticks since last save |
| `saveTime(tile, tag)` | Persist last timestamp to NBT |
| `loadTime(tile, tag)` | Restore last timestamp from NBT |
| `timeModeChanged()` | Reset on config change |
| `sendChatDebug(...)` | Send debug message to nearby players |

No need to reimplement time tracking — `AbstractCatchupHandler` handles it.

## Full RF Furnace Example

```java
public class ThermalFurnaceHandler extends AbstractCatchupHandler {

    @Override
    public void applyCatchup(BlockEntity tile, long elapsed, Level level, BlockPos pos) {
        MyTileEntity furnace = (MyTileEntity) tile;

        int energyStored = furnace.getEnergy();
        int energyCapacity = furnace.getMaxEnergy();
        int rfPerTick = furnace.getRfPerTick();
        int rfPerItem = furnace.getRfPerItem();

        int smeltableItems = countInputs(furnace);
        int outputSpace = countOutputSpace(furnace);

        // Bottleneck: RF available vs RF needed
        int maxSmeltable = Math.min(smeltableItems, outputSpace);
        long maxRfToConsume = (long) maxSmeltable * rfPerItem;
        long rfAvailable = (long) elapsed * rfPerTick + energyStored;
        long effectiveRf = Math.min(rfAvailable, maxRfToConsume);
        int itemsToSmelt = (int) (effectiveRf / rfPerItem);

        // Apply
        for (int i = 0; i < itemsToSmelt; i++) {
            smeltOneItem(furnace, level);
        }

        int rfSpent = itemsToSmelt * rfPerItem;
        int rfFromGen = (int) Math.min(rfSpent, rfAvailable - energyStored);
        int rfFromBuffer = rfSpent - rfFromGen;
        furnace.setEnergy(energyStored + rfFromGen - rfFromBuffer);
        furnace.setChanged();

        sendChatDebug(level, pos, "MyFurnace", elapsed,
                0, itemsToSmelt, 0, 0, furnace.getEnergy() > 0);
    }

    private int countInputs(MyTileEntity f) { return f.getItem(0).getCount(); }
    private int countOutputSpace(MyTileEntity f) {
        ItemStack out = f.getItem(2);
        return out.isEmpty() ? 64 : out.getMaxStackSize() - out.getCount();
    }
    private void smeltOneItem(MyTileEntity f, Level level) {
        // implement item smelting logic
    }
}
```

## Tips

1. **RF furnaces:** simulation is always `min(availableRF, neededRF) / rfPerItem`
2. **Fuel-only furnaces:** simulation is `min(fuelTicks, cookTicks, elapsed)`
3. **Hopper IO:** implement inside `applyCatchup()` — call `autoIO()` or `pullFuelFromSides()`
4. **Factory/Generator networks:** write your own collector + engine using same bottleneck pattern

## What is NOT API

Everything under `com.keepsmelting.internal.*` — **internal only**. May change without notice. Do not use directly.

---

**See also:** [Documentation Index](INDEX.md) | [Features & Performance](FEATURES.md) | [README](../../README.md)
