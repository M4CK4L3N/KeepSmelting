# KeepSmelting

Furnaces keep smelting even when you are offline. Uses real-world time to calculate how many items would have been cooked.

**Inspired by EverFurnace, but written from scratch with a different approach.**

## Features

- ⏰ **Real-time & Game-time modes** — works across chunk unload, game exit, and menu pause
- 🔥 **Vanilla furnaces** — furnace, smoker, blast furnace
- ⚙️ **Iron Furnaces** — full support for all 3 modes (Furnace, Factory, Generator) with all augment slots:
  - Red slot (3): recipe type — Smelting / Blasting / Smoking
  - Green slot (4): efficiency — normal / Speed (2× speed) / Fuel (2× burn time)
  - Blue slot (5): mode — Furnace / Factory / Generator
- 📦 **Hopper I/O** — auto-pulls input/fuel from adjacent containers, pushes output below
- 🛠️ **API for other mods** — add KeepSmelting support to your custom furnace
- 🔧 **Configurable** — `/keepsmelting` commands

## Installation

1. Download the jar from [Modrinth](#) or [CurseForge](#)
2. Put it in your `mods/` folder
3. (Optional) Install [Iron Furnaces](https://www.curseforge.com/minecraft/mc-mods/iron-furnaces) for extra furnace types

## Commands

| Command | Description |
|---|---|
| `/keepsmelting status` | Show current settings |
| `/keepsmelting catchup <true/false>` | Toggle offline catchup |
| `/keepsmelting maxTicks <1-192000>` | Max offline ticks to simulate (default: 24000 = 1 MC day) |
| `/keepsmelting minDelta <1-72000>` | Min tick gap before catchup fires (default: 20 = 1 second) |
| `/keepsmelting debug <OFF/CHAT/LOG>` | Debug output mode |
| `/keepsmelting time <REALTIME/GAMETIME>` | Time tracking mode |

## Config

File: `config/keepsmelting-common.toml`

```toml
[catchup]
catchupEnabled = true
maxCatchupTicks = 24000
minDeltaThreshold = 20
debugMode = "OFF"
timeMode = "REALTIME"
```

## Performance

All catchup modes use **adaptive batch** — O(events) instead of O(ticks):
- Vanilla furnace: ~5-20 iterations for 24000 ticks
- Iron Furnaces Furnace: ~5-20 iterations
- Iron Furnaces Factory: ~100 iterations (was 144000 before optimization)
- Iron Furnaces Generator: batch per fuel burn cycle

## Supported Furnaces

### Vanilla
- Furnace
- Smoker
- Blast Furnace

### Iron Furnaces (optional dependency)
All 13 furnace tiers + 3 modes

## API for Other Mods

### Add KeepSmelting support to your custom furnace

Add KeepSmelting as a dependency in your `build.gradle`:

```groovy
repositories {
    flatDir { dirs 'libs' }
}
dependencies {
    implementation 'com.keepsmelting:keepsmelting:1.0.0'
}
```

Implement the handler:

```java
import com.keepsmelting.api.IFurnaceCatchupHandler;
import com.keepsmelting.api.CatchupHandlerRegistry;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.nbt.CompoundTag;

public class MyFurnaceHandler implements IFurnaceCatchupHandler {
    @Override
    public void applyCatchup(BlockEntity tile, long elapsed, Level level, BlockPos pos) {
        MyFurnaceTile ft = (MyFurnaceTile) tile;
        // Your catchup logic here
        // elapsed = ticks to simulate
        // Delegate to your furnace's native cook/burn methods
    }
    
    @Override
    public void saveTime(BlockEntity tile, CompoundTag tag) {
        // Save your time tracker to NBT
    }
    
    @Override
    public void loadTime(BlockEntity tile, CompoundTag tag) {
        // Load your time tracker from NBT
    }
}
```

Register in your `@Mod` constructor:

```java
@Mod("mymod")
public class MyMod {
    public MyMod() {
        CatchupHandlerRegistry.register(MyFurnaceTile.class, new MyFurnaceHandler());
    }
}
```

That's it. When KeepSmelting is installed, it will find your handler and delegate catchup to it.

### Change the time mode and other settings

```java
KeepSmeltingConfig.COMMON.catchupEnabled.set(true);
KeepSmeltingConfig.COMMON.timeMode.set(TimeMode.REALTIME);
```

## License

MIT © M4CK4L3N

Inspired by EverFurnace (LGPL-3.0).
