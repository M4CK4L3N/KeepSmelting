# KeepSmelting — Features

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
All 13 furnace tiers + 3 modes (Furnace, Factory, Generator)

## API for Other Mods

See [API_USAGE.md](API_USAGE.md) for full integration guide.

Quick example:

```java
// 1. Extend AbstractCatchupHandler (handles saveTime/loadTime/calcElapsed for you)
public class MyHandler extends AbstractCatchupHandler {
    @Override
    public void applyCatchup(BlockEntity tile, long elapsed, Level level, BlockPos pos) {
        // Your catchup logic
    }
}

// 2. Register
CatchupHandlerRegistry.register(MyTile.class, new MyHandler());
```

That's it. KeepSmelting will find your handler automatically.

## Localization

KeepSmelting supports multiple languages:

| Language | File |
|---|---|
| 🇬🇧 English | [`en_us.json`](../../src/main/resources/assets/keepsmelting/lang/en_us.json) |
| 🇷🇺 Russian | [`ru_ru.json`](../../src/main/resources/assets/keepsmelting/lang/ru_ru.json) |

The game language is detected automatically. All commands, help messages and status output are translated.

### Contributing Translations

To add a new language:

1. Copy [`en_us.json`](../../src/main/resources/assets/keepsmelting/lang/en_us.json) to `assets/keepsmelting/lang/<locale>.json`
2. Translate the values (keep the keys unchanged)
3. Submit a pull request

---

**See also:** [Documentation Index](INDEX.md) | [API for Mod Developers](API_USAGE.md) | [README](../../README.md)
