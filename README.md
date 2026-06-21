# KeepSmelting

[![CurseForge](https://img.shields.io/badge/CurseForge-keepsmelting-orange?style=flat-square)](https://www.curseforge.com/minecraft/mc-mods/keepsmelting)
[![Modrinth](https://img.shields.io/badge/Modrinth-keepsmelting-green?style=flat-square)](https://modrinth.com/mod/keepsmelting)
[![GitHub](https://img.shields.io/badge/GitHub-M4CK4L3N/KeepSmelting-181717?style=flat-square&logo=github)](https://github.com/M4CK4L3N/KeepSmelting)

Furnaces keep smelting even when you are offline. Uses real-world time to calculate how many items would have been cooked.

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

## Learn More

- [📖 Documentation Index](docs/en/INDEX.md)
- [✨ Features & Performance](docs/en/FEATURES.md)
- [🔧 API for Mod Developers](docs/en/API_USAGE.md)
- [📋 Changelog](CHANGELOG.md)

## License

MIT © M4CK4L3N

---

[🇷🇺 Русская документация](docs/ru/README.md)
