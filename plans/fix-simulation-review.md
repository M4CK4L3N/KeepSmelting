# План исправлений симуляции IronFurnaces

## 1. Критический баг: двойной учёт аугментов Speed/Fuel

**Файлы:** [`CatchupSimulation.java`](../src/main/java/com/keepsmelting/internal/ironfurnaces/CatchupSimulation.java)

**Проблема:** в `computeFactoryParams()` (строка 106) и `applyFactorySmelt()` (строка 587) аугменты Speed/Fuel применяются дважды — оригинальный `getFactoryCookTime()` уже их учитывает.

**Исправление:** убрать строки 108-109 (hasSpeed/hasFuel) и 127-128 (rfPerItem *=2, /=2) в computeFactoryParams. Убрать строки 635-638 в applyFactorySmelt.

## 2. Хардкод avgBurnTicksPerFuel

**Файл:** [`CatchupSimulation.java`](../src/main/java/com/keepsmelting/internal/ironfurnaces/CatchupSimulation.java) строка 212

**Проблема:** `long avgBurnTicksPerFuel = 1200` — не учитывает реальное топливо (уголь=1600, лава=20000).

**Исправление:** вычислять среднее `avgBurnTicksPerFuel` из всех генераторов сети через `getBurnTicksPerFuel()`.

## 3. Потеря остатков при делении

**Файл:** [`CatchupSimulation.java`](../src/main/java/com/keepsmelting/internal/ironfurnaces/CatchupSimulation.java) строки 289-343

**Проблема:** целочисленное деление `value / total` теряет остаток. Ресурсы теряются.

**Исправление:** распределять остатки через накопительный алгоритм `(total - distributed) / (remainingCount)`.

## 4. RF заводов как источник

**Файл:** [`CatchupSimulation.java`](../src/main/java/com/keepsmelting/internal/ironfurnaces/CatchupSimulation.java) строка 445

**Проблема:** `totalRfAvailable = maxRfFromFuel + generatorCurrentRf + factoryCurrentRf` — RF внутри заводов уже зарезервировано для текущих рецептов.

**Исправление:** убрать `factoryCurrentRf` из общего пула.

## 5. Дублирование кода сжигания топлива

**Файлы:** GeneratorMode.java (apply, processNeighborGenerators), CatchupSimulation.java (burnFuelIn)

**Проблема:** три реализации сжигания топлива, могут рассинхронизироваться.

**Исправление:** выделить единый `FurnaceFuelHandler.java`.

## 6. Выделение DTO

**Файл:** CatchupSimulation.java (строки 173-191, 378-386, 780-784)

**Проблема:** SimulationResult, NetworkResources, FactorySmeltParams вложены в CatchupSimulation.

**Исправление:** вынести в отдельные файлы SimulationData.java.

## Порядок реализации

1. БАГ №1 — двойной учёт аугментов (критический)
2. БАГ №2 — хардкод avgBurnTicksPerFuel
3. БАГ №3 — потеря остатков
4. БАГ №4 — RF заводов как источник
5. Выделение DTO (рефакторинг)
6. FurnaceFuelHandler (рефакторинг)
