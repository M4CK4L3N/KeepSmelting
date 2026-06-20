# Сетевая симуляция N генераторов ↔ M заводов

## Проблема

Текущая симуляция поддерживает только связку 1 Generator → 1 Factory. В реальности Iron Furnaces позволяет ставить любое количество печей рядом, настраивать стороны на вход/выход/RF.

## Цель

Симуляция должна учитывать все печи в сети (независимо от их расположения) за один проход.

## Как Iron Furnaces соединяет печи

Из декомпилированного кода `BlockIronFurnaceTileBase`:

### `furnaceSettings` — массив из 11 элементов:

| Индекс | Направление | Значения |
|---|---|---|
| 0 | DOWN | 0=выкл, 1=вход, 2=выход, 3=вход+выход, 4=топливо |
| 1 | UP | то же |
| 2-5 | NORTH, SOUTH, WEST, EAST | то же |
| ... | | |

### `energyOut()` — передача RF:
```java
// Ищет соседей с setting 2 или 3 на этой стороне
// Делит RF поровну между всеми подходящими соседями
```

### `autoIO()` / `autoIOGenerator()` / `autoFactoryIO()` — предметы:
```java
// Проверяет setting: 1/3 = вход, 2/3 = выход, 4 = топливо
// Пул/пуш по одному предмету через IItemHandler
```

## Архитектура

```mermaid
flowchart TD
    T[Factory получил тик] --> D[Discovery: найти все связанные печи]
    D --> D1[Проверить все 4 стороны + верх/низ]
    D1 --> D2[Generator с setting 2/3 → добавить]
    D1 --> D3[Factory с setting 1/2/3 → добавить]
    D1 --> D4[Повторить для новых печей BFS до 1 глубины]
    
    D --> A[Aggregate: суммировать ресурсы всей сети]
    A --> A1[Топливо: все генераторы слоты + контейнеры]
    A --> A2[Ингредиенты: все Factory слоты + сверху]
    A --> A3[Выход: все Factory слоты + снизу]
    A --> A4[RF/тик: сумма всех генераторов]
    A --> A5[Потребление: сумма всех Factory]
    A --> A6[Capacity: сумма всех печей]
    
    A --> S[Simulate: общий баланс]
    S --> S1[maxRfFromFuel = min(топливо, время) * rfPerTick]
    S --> S2[maxRfToConsume = min(ингредиенты, время) * consumption]
    S --> S3[storage = capacity - currentRf]
    S --> S4[effectiveRf = min(S1, S2 + S3)]
    
    S --> DST[Distribute: применить к каждой печи]
    DST --> DST1[Топливо: сжечь пропорционально запасу генераторов]
    DST --> DST2[RF генераторам: ≤ capacity каждого]
    DST --> DST3[Предметы: расплавить пропорционально ингредиентам заводов]
    
    DST --> M[Mark: пометить все печи как обработанные в этом тике]
```

## Алгоритм

### 1. Discovery — поиск сети

```java
FurnaceNetwork discover(BlockIronFurnaceTileBase startTile, Level level, BlockPos pos) {
    generators = []
    factories = []
    visited = []
    
    // BFS с глубиной 1 (только прямые соседи)
    queue = [startTile]
    while queue not empty:
        tile = queue.pop()
        if tile in visited: continue
        visited.add(tile)
        
        for dir in HORIZONTAL + UP + DOWN:
            neighbor = tile.pos.relative(dir)
            if not loaded: continue
            be = level.getBlockEntity(neighbor)
            if be not instanceof BlockIronFurnaceTileBase: continue
            
            int setting = tile.furnaceSettings.get(dir.ordinal())
            
            if be.isGenerator() && (setting == 2 || setting == 3):
                generators.add(be)
                queue.add(be)  // проверяем соседей генератора
            else if be.isFactory() && (setting == 1 || setting == 2 || setting == 3):
                factories.add(be)
                queue.add(be)  // проверяем соседей завода
    
    return FurnaceNetwork(generators, factories)
}
```

### 2. Aggregate — подсчёт всех ресурсов сети

```java
class FurnaceNetwork {
    List<BlockIronFurnaceTileBase> generators;
    List<BlockIronFurnaceTileBase> factories;
    
    // Суммарные ресурсы
    int totalFuel;
    long totalBurnTicksPerFuel;
    int totalRfPerTick;
    int totalGenCapacity;
    int totalGenCurrentRf;
    
    int totalSmeltableItems;
    int totalOutputSpace;
    int totalRfPerTickConsumption;
    int maxRfPerItem;
    int maxCookTime;
    int totalFactoryCapacity;
    int totalFactoryCurrentRf;
}
```

### 3. Simulate — общий баланс

```java
long maxRfFromFuel = min(totalFuel * burnTicksPerFuel, elapsed) * totalRfPerTick;
long maxRfToConsume = min(totalSmeltable * maxCookTime, elapsed) * totalRfPerTickConsumption;
int storageAvailable = max(0, totalCapacity - totalCurrentRf);
long effectiveRf = min(maxRfFromFuel, maxRfToConsume + storageAvailable);
```

### 4. Distribute — распределение

```java
// Топливо: пропорционально текущему запасу генератора
for (genTile : generators) {
    int share = fuelToBurn * genTile.fuel / max(1, totalFuel);
    burnFuel(genTile, share, level);
}

// RF генераторам: не больше capacity каждого
for (genTile : generators) {
    int space = genTile.getCapacity() - genTile.getEnergy();
    int add = min(space, remainingRf / generators.size());
    genTile.setEnergy(genTile.getEnergy() + add);
}

// Предметы: пропорционально ингредиентам
for (factoryTile : factories) {
    int share = itemsToSmelt * factoryTile.items / max(1, totalSmeltable);
    smeltItems(factoryTile, share, share * maxRfPerItem, level);
}
```

### 5. Дедупликация

Уже реализована в `IronFurnaceTickMixin` через `keepsmelting$processedThisTick`:
- Первый Factory делает discovery всей сети
- Помечает все печи в сети как обработанные
- Остальные Factory пропускают свой тик

## Файлы для изменений

| Файл | Изменения |
|---|---|
| **Новый:** `FurnaceNetwork.java` | DTO + discovery |
| [`CatchupSimulation.java`](src/main/java/com/keepsmelting/internal/ironfurnaces/CatchupSimulation.java) | `simulateNetwork()`, `distributeToNetwork()` |
| [`IronFurnaceCatchupHandler.java`](src/main/java/com/keepsmelting/internal/ironfurnaces/IronFurnaceCatchupHandler.java) | Новая `applyFactoryWithNeighbors()` |

## Ограничения

Симуляция **не** копирует поштучную логику Iron Furnaces:
- Не симулирует каждый отдельный RF transfer (их тысячи за 24000 тиков)
- Не симулирует поштучную передачу предметов через autoIO
- Не поддерживает цепочки Factory → Factory

Вместо этого симуляция считает **net effect** за всё офлайн-время:
- Общее RF = общее топливо × RF/тик × время
- Общее потребление = общие ингредиенты × RF/предмет
- Узкое место = min(генерация, потребление, хранение)
