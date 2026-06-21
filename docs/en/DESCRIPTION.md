# KeepSmelting

**KeepSmelting** — a Minecraft Forge mod that makes furnaces continue smelting while you're offline.

Instead of halting progress when you exit the game or unload a chunk, KeepSmelting tracks real‑world time and **catches up** all missed smelting in batches when you log back in.

## Features

- ⏰ **Real‑time & Game‑time modes** — survives chunk unloading, game exit, and menu pause
- 🔥 **Vanilla furnaces** — furnace, smoker, blast furnace
- ⚙️ **Iron Furnaces** — full support for all 13 tiers and 3 modes (Furnace, Factory, Generator) with every augment slot
- 📦 **Hopper pipeline simulation** — auto‑pull inputs/fuel from adjacent containers, push outputs through full hopper chains
- 🛠️ **API for modders** — add KeepSmelting support to your custom furnace with one registration call
- 🌍 **English & Русский** — fully localized into both languages

## Configuration

Use `/keepsmelting status` to view current settings. Available options:

| Setting | Description |
|---|---|
| `catchup <true/false>` | Enable or disable offline smelting |
| `time <REALTIME/GAMETIME>` | Choose time tracking mode |
| `debug <OFF/CHAT/LOG>` | Toggle debug output |

## Installation

1. Download the jar
2. Put it in your `mods/` folder
3. (Optional) Install Iron Furnaces for extra furnace types

## Requirements

- Minecraft 1.20.1
- Forge 47+
- Iron Furnaces (optional — adds 13 extra furnace tiers)

## License

MIT

---

**See also:** [Documentation Index](INDEX.md) | [README](../../README.md)
