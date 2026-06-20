# Симуляция Factory + Generator без потерь RF

## Текущие проблемы

| Проблема | Где | Описание |
|---|---|---|
| Топливо сжигается до проверки | `GeneratorMode.apply()` :26 vs :69 | Сначала `fuel.shrink(1)`, потом проверка `capacity` — если завод полон, RF пропадает |
| Hopper-вытягивание один раз | `FactoryMode` :145, `FurnaceMode` :53 | Не моделирует многократную подгрузку из сундуков на протяжении догонялки |
| Нет учёта предметов в сундуках | Все mode | Смотрят только в свои слоты, не в hopper'ы/сундуки |
| Нет единого прохода gen→factory | `IronFurnaceCatchupHandler` | Генераторы отдельно, завод отдельно — несогласованно |
| Избыток RF испаряется | `GeneratorMode.apply()` :26 | Если генератор полон И завод полон — топливо сгорело впустую |
| Нет учёта выхода | `FactoryMode` :58 | Не проверяет свободное место в hopper снизу |

## Новая архитектура

### Общая схема

```
[Тик завода] → [Phase 1: Count] → [Phase 2: Simulate] → [Phase 3: Apply]
```

### Phase 1: Count — подсчёт всех ресурсов

#### Топливо генераторов

Источники топлива:
1. Слот 6 генератора (`genTile.inventory.get(6)`)
2. Соседние сундуки / hopper'ы по горизонтали (только те, у кого есть топливо для горения)
3. Hopper'ы за сундуками (один уровень вложенности)

```java
int countFuelInSlot(genTile)                    // топливо в самом генераторе
int countFuelInNearbyContainers(genTile, level) // топливо в соседних контейнерах
int totalFuelItems = fuelInSlot + fuelInNearbyContainers
long maxBurnTicksFromFuel = totalFuelItems * burnTicksPerFuelItem
int rfPerTick = genTile.getGeneration()         // RF/тик
```

#### Ингредиенты завода

Источники ингредиентов:
1. 6 слотов входа завода (`slots 7-12`)
2. Контейнер сверху от завода (hopper / сундук)
3. То же для каждого слота отдельно

```java
int countItemsInSlots(factoryTile)              // предметы в 6 слотах
int countItemsInHopperAbove(factoryTile, level) // предметы в контейнере сверху
int totalSmeltableItems = itemsInSlots + itemsInHopperAbove
```

#### Место на выходе

Проверка:
1. 6 слотов выхода (`slots 13-18`) — свободные стаки
2. Контейнер снизу — свободные стаки

```java
int countOutputSlotsFree(factoryTile)           // свободные стаки на выходе
int countOutputHopperFree(factoryTile, level)   // свободные стаки в hopper снизу
int totalOutputSpace = outputSlotsFree + outputHopperFree
int maxSmeltableByOutput = totalOutputSpace * maxStackSize
```

#### RF / ёмкость системы

```java
int generatorCapacity = genTile.getCapacity()
int generatorCurrentRf = genTile.getEnergy()
int factoryCapacity = factoryTile.getCapacity()
int factoryCurrentRf = factoryTile.getEnergy()
int totalSystemCapacity = generatorCapacity + factoryCapacity
int totalCurrentRf = generatorCurrentRf + factoryCurrentRf
int rfStorageAvailable = max(0, totalSystemCapacity - totalCurrentRf)
```

#### Параметры плавки

```java
// Для каждого активного слота завода:
int cookingTime = factoryTile.getFactoryCookTime(slot)  // тиков на предмет
int rfPerItem = cookingTime * 20                        // RF на предмет
int rfPerTickConsumption = rfPerItem / cookingTime      // RF/тик

// Суммарно:
int worstCaseRfPerItem = max(slots.rfPerItem)
int totalRfPerTickConsumption = sum(slots.rfPerTickConsumption)
long totalCookTicksNeeded = activeSlots * averageCookingTime
```

### Phase 2: Simulate — математика без сайд-эффектов

```java
// 1. Сколько RF можем сгенерировать из топлива за elapsed
long maxBurnTicks = min(maxBurnTicksFromFuel, elapsedTicks)
long maxRfFromFuel = maxBurnTicks * rfPerTick

// 2. Сколько RF можем потребить (из предметов + место на выходе)
int actualSmeltable = min(totalSmeltableItems, maxSmeltableByOutput)
long ticksToSmeltAll = actualSmeltable * averageCookingTime
long maxRfToConsume = min(ticksToSmeltAll, elapsedTicks) * totalRfPerTickConsumption

// 3. Сколько RF можем сохранить
int rfStorageAvailable = max(0, totalSystemCapacity - totalCurrentRf)

// 4. Узкое место
long effectiveRf = min(maxRfFromFuel, maxRfToConsume + rfStorageAvailable)

// 5. Сколько топлива сжечь (ровно столько, сколько нужно для effectiveRf)
int fuelToBurn = effectiveRf / (rfPerTick * burnTicksPerFuelItem)
// +1 если нужно дожечь для последнего предмета (округление вверх)

// 6. Сколько предметов расплавить
int itemsToSmelt = min(actualSmeltable, effectiveRf / worstCaseRfPerItem)

// 7. RF распределение
long rfForItems = itemsToSmelt * worstCaseRfPerItem
long rfRemaining = effectiveRf - rfForItems
// rfRemaining распределяется: генератор <= capacity, остаток заводу
```

### Phase 3: Apply — применение с сайд-эффектами

```java
// 1. Сжечь топливо
actualFuelToBurn = min(fuelToBurn, fuelInSlot)
если не хватило → взять из соседних контейнеров (removeItem)
сжечь = уменьшить count, обновить generatorBurn

// 2. Загрузить ингредиенты
for (каждый слот):
    если в слоте не хватает до itemsToSmeltPerSlot → вытянуть из hopper сверху

// 3. Расплавить
for (каждый предмет):
    уменьшить input[slot], увеличить output[slot]
    если output полон → push в hopper снизу

// 4. Распределить RF
// Сначала наполнить генераторы до capacity
for (каждый генератор):
    space = genCapacity - genCurrentRf
    add = min(space, rfRemaining)
    genTile.setEnergy(genCurrentRf + add)
    rfRemaining -= add

// Потом остаток заводу
space = factoryCapacity - factoryCurrentRf
add = min(space, rfRemaining)
factoryTile.setEnergy(factoryCurrentRf + add)
```

## Файлы для изменений

| Файл | Изменения |
|---|---|
| `src/main/java/com/keepsmelting/internal/ironfurnaces/IronFurnaceCatchupHandler.java` | Новая `applyFactoryWithNeighbors()` |
| `src/main/java/com/keepsmelting/internal/ironfurnaces/GeneratorMode.java` | Новая `processNeighborGenerators()` возвращает `ResourceCount`, не сжигает топливо |
| `src/main/java/com/keepsmelting/internal/ironfurnaces/FactoryMode.java` | Новый `apply()` — принимает готовые `effectiveRf` и `itemsToSmelt` |
| `src/main/java/com/keepsmelting/internal/ironfurnaces/FurnaceMode.java` | Можно не трогать (простой режим не имеет проблемы потерь) |
| **Новый:** `src/main/java/com/keepsmelting/internal/ironfurnaces/CatchupSimulation.java` | Чистая математика без сайд-эффектов |

## ResourceCount DTO

```java
package com.keepsmelting.internal.ironfurnaces;

/**
 * Результат подсчёта ресурсов для симуляции.
 * Все поля — pre-count, без сайд-эффектов.
 */
public class ResourceCount {
    // Топливо
    public int totalFuelItems;
    public long maxBurnTicksFromFuel;
    public int rfPerTick;
    
    // Завод
    public int totalSmeltableItems;
    public int maxSmeltableByOutput;
    public long totalCookTicksNeeded;
    public int totalRfPerTickConsumption;
    
    // RF
    public int totalSystemCapacity;
    public int totalCurrentRf;
    public long elapsedTicks;
}
```
