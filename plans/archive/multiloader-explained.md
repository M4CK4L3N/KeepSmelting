# MultiLoader: как работает и как билдится

## 1. Структура проекта MultiLoader

```
keepsmelting/                          ← корень Git-репозитория
├── settings.gradle                    ← какие модули включены
├── build.gradle                       ← общие настройки для всех модулей
├── gradle.properties                  ← версии (MC, загрузчики, маппинги)
│
├── common/                            ← ☑️ ВСЯ ЛОГИКА
│   ├── build.gradle                   — без Minecraft-плагина
│   └── src/main/java/
│       └── com/keepsmelting/
│           ├── api/                   — интерфейсы
│           └── internal/              — хендлеры (чистая Java)
│
├── forge/                             ← 🟠 Форжевский модуль
│   ├── build.gradle                   — legacyForge / neoForged плагин
│   └── src/main/java/
│       └── com/keepsmelting/
│           ├── KeepSmelting.java      — @Mod
│           ├── KeepSmeltingConfig.java
│           ├── command/
│           └── mixin/                 — миксины (платформенно-зависимые)
│               ├── FurnaceTickMixin.java
│               └── ironfurnaces/
│
├── fabric/                            ← 🟡 Фабричный модуль
│   ├── build.gradle                   — fabric-loom плагин
│   └── src/main/java/
│       └── com/keepsmelting/
│           ├── KeepSmelting.java      — ModInitializer
│           ├── KeepSmeltingConfig.java — Fabric API / YACL
│           └── mixin/
│               └── FurnaceTickMixin.java
│
└── neoforge/                          ← 🔵 Неофоржевский модуль (опционально)
    ├── build.gradle
    └── src/main/java/
        └── com/keepsmelting/
            ├── KeepSmelting.java      — @Mod (NeoForge)
            └── mixin/
```

## 2. Как common-модуль подключается к платформам

**`common/build.gradle`** — это чистая Java library, без Minecraft:
```gradle
plugins {
    id 'java-library'
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}
```

**`forge/build.gradle`** — подключает common:
```gradle
plugins {
    id 'net.neoforged.moddev.legacyforge' version '2.0.91'
}

dependencies {
    implementation project(':common')       // ☑️ берёт common
    compileOnly files('libs/ironfurnaces.jar')
}
```

**`settings.gradle`** — включает все модули:
```gradle
rootProject.name = 'keepsmelting'

include 'common'
include 'forge'
include 'fabric'
// include 'neoforge'
```

## 3. Как это билдится

```bash
# Собрать всё
./gradlew build

# Собрать только Forge
./gradlew :forge:build

# Собрать только Fabric
./gradlew :fabric:build

# Запустить Forge-клиент
./gradlew :forge:runClient

# Запустить Fabric-клиент
./gradlew :fabric:runClient
```

**Результат:**
```
forge/build/libs/keepsmelting-forge-1.0.0.jar
fabric/build/libs/keepsmelting-fabric-1.0.0.jar
```

## 4. Как common видит Minecraft без импорта Minecraft

**Проблема:** common-модуль не имеет Minecraft в зависимостях. Но хендлеры принимают `BlockEntity`, `Level`, `BlockPos`.

**Решение:** common НЕ импортирует Minecraft напрямую. Он работает через `java.lang.reflect` или через интерфейсы, которые платформенный модуль реализует:

```java
// common/api/IFurnaceCatchupHandler.java
// ❌ НЕТ импорта Minecraft классов
public interface IFurnaceCatchupHandler {
    void applyCatchup(Object tile, long elapsed, Object level, Object pos);
    // ↑ Object вместо BlockEntity/Level/BlockPos
}
```

ИЛИ **лучший вариант** — common всё-таки знает о Minecraft, если мы собираем его **через loom/forge плагин**, но не регистрируем как мод:

```gradle
// common/build.gradle — с платформенным плагином, НО не мод
neoForge {
    // не добавляем mods { } — common не мод
}
dependencies {
    implementation "net.minecraft:minecraft:${minecraft_version}"
    // только compile scope — не включается в jar
}
```

В этом случае common компилируется против Minecraft, но в финальный jar не попадает — туда идёт только common через `implementation project(':common')`.

## 5. Что идёт в финальный jar

```
keepsmelting-forge-1.0.0.jar

META-INF/
├── mods.toml
└── MANIFEST.MF

com/keepsmelting/
├── api/IFurnaceCatchupHandler.class      ← from common
├── api/CatchupHandlerRegistry.class      ← from common
├── internal/catchup/VanillaCatchupHandler.class  ← from common
├── internal/catchup/VanillaHopperIO.class       ← from common
├── internal/debug/DebugOutput.class             ← from common
├── KeepSmelting.class                    ← from forge (латформа)
├── KeepSmeltingConfig.class              ← from forge
├── mixin/FurnaceTickMixin.class          ← from forge
└── mixin/ironfurnaces/...               ← from forge

keepsmelting.refmap.json                   ← from forge
keepsmelting.mixins.json                   ← from forge
```

**Важно:** common-классы **не дублируются** между модулями. Forge-модуль зависит от common через `implementation project(':common')`, и Gradle мерджит classpath при сборке.

## 6. Что разное между Forge и Fabric — на примере миксина

| Аспект | Forge 1.20.1 | Fabric 1.20.1 |
|---|---|---|
| Target class | `AbstractFurnaceBlockEntity` | то же самое |
| @Mixin | `@Mixin(AbstractFurnaceBlockEntity.class)` | то же самое |
| @Inject point | `serverTick(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/level/block/entity/AbstractFurnaceBlockEntity;Lnet/minecraft/world/level/block/entity/BlockEntityType;)V` | то же самое |
| Burn time | `ForgeHooks.getBurnTime(fuel, RecipeType.SMELTING)` | `AbstractFurnaceBlockEntity.getBurnTime(level.registryAccess(), recipe, fuel)` |
| Конфиг | `ForgeConfigSpec` | `FabricConfigApi` / YACL / custom TOML |
| Регистрация команд | `RegisterCommandsEvent` | `CommandRegistrationCallback.EVENT` |

**KeepSmelting для Fabric — что отвалится:**
- Iron Furnaces — только Forge → `ironfurnaces/` миксины исключены
- `ForgeHooks.getBurnTime()` → замена на `AbstractFurnaceBlockEntity.getBurnTime()`
- `ForgeConfigSpec` → замена на Fabric Config API

## 7. CI/CD — автоматическая сборка всех платформ

```yaml
# .github/workflows/build.yml
name: Build all platforms

on: [push, pull_request]

jobs:
  build:
    strategy:
      matrix:
        loader: [forge, fabric]
        mc: [1.20.1]
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-java@v4
        with:
          java-version: 17
          distribution: temurin
      - name: Grant execute permission
        run: chmod +x gradlew
      - name: Build ${{ matrix.loader }} MC ${{ matrix.mc }}
        run: ./gradlew :${{ matrix.loader }}:build
      - name: Upload ${{ matrix.loader }} artifact
        uses: actions/upload-artifact@v4
        with:
          name: keepsmelting-${{ matrix.loader }}-${{ matrix.mc }}
          path: ${{ matrix.loader }}/build/libs/*.jar
```

После пуша:
- GitHub Actions собирает Forge-версию → `keepsmelting-forge-1.20.1-1.0.0.jar`
- GitHub Actions собирает Fabric-версию → `keepsmelting-fabric-1.20.1-1.0.0.jar`
- Можно скачать из Artifacts

## 8. Версионирование для разных MC

```gradle
// gradle.properties (один файл для всех модулей)
minecraft_version=1.20.1
forge_version=47.4.10
fabric_loader_version=0.15.11
fabric_api_version=0.92.0+1.20.1
mod_version=1.0.0

// Для другой MC-версии — переключить всё разом
# minecraft_version=1.21
# forge_version=51.0.0
# fabric_loader_version=0.16.0
# ...
```

**Либо** иметь отдельные Git ветки под каждую MC-версию:
- `main` (1.20.1)
- `1.21` (с другими версиями зависимостей)
- `1.21.1`

## 9. Критические замечания нейросети (подтверждаю)

### 9.1 Чистота common-модуля

> common не должен импортировать Forge или Fabric. Только чистый Java + Minecraft API (который общий для всех загрузчиков).

✅ Подтверждаю. `common/` может импортировать:
- `net.minecraft.world.level.block.entity.BlockEntity` — одинаков во всех загрузчиках
- `net.minecraft.core.BlockPos`, `net.minecraft.world.level.Level` — одинаков

❌ НЕ может импортировать:
- `net.minecraftforge.common.ForgeHooks` — только в forge/
- `net.minecraftforge.fml.common.Mod` — только в forge/
- `net.fabricmc.api.ModInitializer` — только в fabric/

**Связь common → платформа:** через интерфейсы или паттерн "фабрика/стратегия":

```java
// common/api/BurnTimeProvider.java — интерфейс в common
public interface BurnTimeProvider {
    int getBurnTime(ItemStack fuel, @Nullable RecipeType<?> recipeType);
}

// forge/internal/ForgeBurnTimeProvider.java — реализация в forge
public class ForgeBurnTimeProvider implements BurnTimeProvider {
    @Override
    public int getBurnTime(ItemStack fuel, RecipeType<?> recipeType) {
        return ForgeHooks.getBurnTime(fuel, recipeType);
    }
}

// fabric/internal/FabricBurnTimeProvider.java — реализация в fabric
public class FabricBurnTimeProvider implements BurnTimeProvider {
    @Override
    public int getBurnTime(ItemStack fuel, RecipeType<?> recipeType) {
        return AbstractFurnaceBlockEntity.getBurnTime(
            level.registryAccess(), recipe, fuel
        );
    }
}
```

Платформенный модуль регистрирует реализацию при старте, common использует её через интерфейс.

### 9.2 Iron Furnaces — изоляция в Forge-модуль

Весь код Iron Furnaces должен быть **только** в `forge/src/main/java/`, не в `common/`.

```
forge/src/main/java/com/keepsmelting/
├── ironfurnaces/
│   ├── IronFurnaceCatchupHandler.java
│   ├── IronFurnaceFurnaceMode.java
│   ├── IronFurnaceFactoryMode.java
│   ├── IronFurnaceGeneratorMode.java
│   └── IronFurnaceNeighborHelper.java
└── mixin/ironfurnaces/
    ├── IronFurnaceAccessor.java
    └── IronFurnaceTickMixin.java
```

`common/` **ничего не знает** про Iron Furnaces. Если собрать fabric — этой папки просто нет. Не нужно ни условной компиляции, ни `@Pseudo`/`defaultRequire: 0`.

### 9.3 Выбор MultiLoader шаблона

| Шаблон | Плюсы | Минусы |
|---|---|---|
| **Vanilla Gradle + submodules** (описан в этом документе) | Минимум магии. Работает. Понятно | Нет встроенной поддержки нескольких MC-версий |
| **Architectury Loom** | Мультиверсионность встроена | Тяжёлый, много магии, Architectury API как доп. зависимость |
| **Stonecutter** (от Kikugie) | Лёгкий, переключает MC-версию одной командой | Новый, мало документации |
| **MultiLoader-Template** (neoforged) | Официальный шаблон NeoForge | Сложный, много церемоний |

**Рекомендация:** начать с vanilla submodules (этот план). Если понадобится поддержка нескольких MC-версий — добавить Stonecutter.

## 10. Итог

| Вопрос | Ответ |
| Код для Forge и Fabric **общий**? | Весь catchup — да (common). Миксины и конфиг — нет (свои под каждую платформу). |
| Как собрать? | `./gradlew build` — всё сразу. Или `:forge:build` / `:fabric:build` — отдельно. |
| Сколько jar-файлов на выходе? | 1 на платформу: `forge/build/libs/keepsmelting-forge-1.0.0.jar` + `fabric/build/libs/keepsmelting-fabric-1.0.0.jar` |
| Iron Furnaces на Fabric? | Нет (мод только Forge). Сборка для Fabric просто не включает ironfurnaces/ миксины. |
| CI/CD? | GitHub Actions matrix: forge + fabric + neoforge. Автосборка при каждом пуше. |
