# Реструктуризация internal/ironfurnaces/

## Текущее состояние

```
internal/ironfurnaces/
├── CatchupDedup.java          — дедупликация (OK)
├── CatchupSimulation.java     — фасад (OK)
├── FactoryMode.java           — МЁРТВ, заменён симуляцией → удалить
├── FurnaceFuelHandler.java    — общая логика топлива (OK)
├── FurnaceMode.java           — жив, используется, не требует симуляции (OK)
├── FurnaceNetwork.java        — discovery сети (OK)
├── GeneratorMode.java         — МЁРТВ, заменён симуляцией → удалить
├── IronFurnaceCatchupHandler.java — точка входа (OK)
├── NetworkDataCollector.java  — Phase 1 (OK)
├── SimulationApplicator.java  — Phase 3 (OK, но содержит 2 ответственности)
├── SimulationData.java        — DTO (OK)
└── SimulationEngine.java      — Phase 2 (OK)
```

## Проблемы

1. **Мёртвый код:** FactoryMode.java и GeneratorMode.java не вызываются
2. **SimulationApplicator.java делает 2 вещи:** распределение по сети + applyFactorySmelt (плавка) + pushFactoryOutputBelow (хоппер-хелпер)
3. **Нет подпакетов:** 12 файлов в одной плоскости

## План

### Step 1: Удалить мёртвый код
- `FactoryMode.java` — полностью удалить
- `GeneratorMode.java` — полностью удалить

### Step 2: Выделить HopperHelper
- `pushFactoryOutputBelow()` из SimulationApplicator → в `FurnaceFuelHandler.java` (там уже есть `pullFuelFromNeighbors`)
- Или в отдельный `HopperHelper.java` для наглядности

### Step 3: Разложить по подпакетам
```
internal/ironfurnaces/
├── data/
│   └── SimulationData.java
├── collect/
│   └── NetworkDataCollector.java
├── simulate/
│   └── SimulationEngine.java
├── apply/
│   └── SimulationApplicator.java    + FactorySmeltApplicator (внутри или отдельно)
├── handler/
│   ├── FurnaceMode.java            — catchup для furnace
│   └── IronFurnaceCatchupHandler.java — точка входа
├── util/
│   ├── FurnaceFuelHandler.java
│   ├── CatchupDedup.java
│   └── HopperHelper.java
├── FurnaceNetwork.java             — discovery, на верхнем уровне
├── CatchupSimulation.java          — фасад, на верхнем уровне
```

### Step 4: Обновить import'ы во всех файлах

### Step 5: Компиляция + коммит

## Порядок выполнения
1. Удалить FactoryMode.java
2. Удалить GeneratorMode.java
3. Выделить HopperHelper.java
4. Создать подпакеты (data/, collect/, simulate/, apply/, handler/, util/)
5. Переместить файлы, обновить package и import
6. Компиляция + коммит
