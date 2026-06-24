# План реструктуризации KeepSmelting (по фазам)

> **Ключевое решение:** Никакой `shared/` папки. Никаких Junction/симлинков.
> Core-код лежит в `modern/common/`. Остальные проекты подключают его через Gradle source set.

## Общая концепция

```
ФАЗА 1 (сейчас):   1.20.1 Forge              ← реструктуризация текущего проекта
                      ↓
ФАЗА 2 (скоро):    modern/ (Stonecutter, JDK 25)
                    ├── 1.20.1-forge + 1.20.1-fabric
                    ├── 1.21-forge + 1.21-fabric
                    └── 1.21.4-forge + 1.21.4-fabric
                      ↓
ФАЗА 3 (опционально):
                    legacy-1.16.5/ (Forge + Fabric)
                    legacy-1.12.2/ (Forge + Fabric Legacy)
                    legacy-1.7.10/ (Forge + Fabric Legacy)
```

## Принцип: как код переиспользуется между проектами

```
modern/common/src/main/java/com/keepsmelting/
├── api/                      ← ☑️ ЕДИНСТВЕННОЕ МЕСТО core-кода
├── internal/
└── mixin/

modern/versions/*/forge/      ← подключает modern/common через Gradle sourceSet
modern/versions/*/fabric/     ← подключает modern/common через Gradle sourceSet

legacy-1.16.5/forge/          ← подключает modern/common через srcDirs +=
legacy-1.16.5/fabric/         ← подключает modern/common через srcDirs +=
legacy-1.12.2/forge/          ← подключает modern/common через srcDirs +=
```

**Никаких Junction. Никакой `shared/`. Файлы лежат только в `modern/common/`.**

---

## ФАЗА 1: Реструктуризация (только 1.20.1 Forge)

**Цель:** Разложить текущий проект в новую структуру, ничего не сломав.
**Gradle JDK:** 17 (пока не обновляем до 25)
**Minecraft:** 1.20.1 Forge

### Шаг 1.1: Создать файловую структуру

```
keepsmelting/                          ← корень Git-репозитория
│
├── modern/                            ← 🟢 основной проект
│   ├── settings.gradle
│   ├── build.gradle
│   ├── gradle.properties
│   ├── gradlew / gradlew.bat
│   ├── gradle/wrapper/
│   │
│   ├── common/                        ← ☑️ CORE-КОД (api + internal + общие миксины)
│   │   └── src/main/java/com/keepsmelting/
│   │       ├── api/
│   │       │   ├── IFurnaceCatchupHandler.java
│   │       │   └── CatchupHandlerRegistry.java
│   │       ├── internal/
│   │       │   ├── catchup/
│   │       │   │   ├── AbstractCatchupHandler.java
│   │       │   │   ├── VanillaCatchupHandler.java
│   │       │   │   └── VanillaHopperIO.java
│   │       │   └── ironfurnaces/
│   │       │       ├── FurnaceMode.java
│   │       │       ├── FactoryMode.java
│   │       │       ├── GeneratorMode.java
│   │       │       └── IronFurnaceCatchupHandler.java
│   │       └── mixin/
│   │           ├── IFurnaceAccessor.java
│   │           └── FurnaceTickMixin.java
│   │
│   ├── forge/                         ← 🟠 Forge-специфичный код (временно, до Stonecutter)
│   │   └── src/main/java/com/keepsmelting/
│   │       ├── KeepSmelting.java
│   │       ├── KeepSmeltingConfig.java
│   │       └── command/
│   │           └── KeepSmeltingCommand.java
│   │
│   └── libs/
│       └── ironfurnaces-1.20.1-4.1.8.jar
│
├── legacy-1.16.5/                     ← 🟠 пока пусто (Фаза 3)
├── legacy-1.12.2/                     ← 🟠 пока пусто (Фаза 3)
├── legacy-1.7.10/                     ← 🟠 пока пусто (Фаза 3)
│
└── README.md
```

### Шаг 1.2: `modern/build.gradle`

```gradle
plugins {
    id 'idea'
    id 'java-library'
    id 'maven-publish'
    id 'net.neoforged.moddev.legacyforge' version '2.0.91'
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

version = mod_version
group = mod_group_id
base { archivesName = mod_id }

legacyForge {
    version = project.minecraft_version + '-' + project.forge_version
    parchment {
        mappingsVersion = project.parchment_mappings_version
        minecraftVersion = project.parchment_minecraft_version
    }
    runs {
        client { client(); systemProperty 'forge.enabledGameTestNamespaces', project.mod_id }
        server { server(); programArgument '--nogui'; systemProperty 'forge.enabledGameTestNamespaces', project.mod_id }
        gameTestServer { type = "gameTestServer"; systemProperty 'forge.enabledGameTestNamespaces', project.mod_id }
        data {
            data()
            programArguments.addAll '--mod', project.mod_id, '--all', '--output', file('src/generated/resources/').getAbsolutePath(), '--existing', file('src/main/resources/').getAbsolutePath()
        }
        configureEach {
            systemProperty 'forge.logging.markers', 'REGISTRIES'
            logLevel = org.slf4j.event.Level.DEBUG
        }
    }
    mods {
        "${mod_id}" { sourceSet(sourceSets.main) }
    }
}

sourceSets {
    main {
        java {
            // ☑️ core-код из common/
            srcDir 'common/src/main/java'
            // 🟠 Forge-специфичный код
            srcDir 'forge/src/main/java'
        }
        resources {
            srcDir 'forge/src/main/resources'
        }
    }
}

dependencies {
    compileOnly files('libs/ironfurnaces-1.20.1-4.1.8.jar')
    annotationProcessor 'org.spongepowered:mixin:0.8.5:processor'
}

mixin {
    add sourceSets.main, "${mod_id}.refmap.json"
    config "${mod_id}.mixins.json"
}

jar {
    manifest.attributes(["MixinConfigs": "${mod_id}.mixins.json"])
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
}
```

### Шаг 1.3: Перенести файлы

**Из `src/main/java/com/keepsmelting/` в `modern/common/...`:**

| Файл | Новый путь |
|---|---|
| `api/IFurnaceCatchupHandler.java` | `modern/common/src/main/java/com/keepsmelting/api/` |
| `api/CatchupHandlerRegistry.java` | `modern/common/src/main/java/com/keepsmelting/api/` |
| `internal/catchup/AbstractCatchupHandler.java` | `modern/common/src/main/java/.../internal/catchup/` |
| `internal/catchup/VanillaCatchupHandler.java` | `modern/common/src/main/java/.../internal/catchup/` |
| `internal/catchup/VanillaHopperIO.java` | `modern/common/src/main/java/.../internal/catchup/` |
| `internal/ironfurnaces/FurnaceMode.java` | `modern/common/src/main/java/.../internal/ironfurnaces/` |
| `internal/ironfurnaces/FactoryMode.java` | `modern/common/src/main/java/.../internal/ironfurnaces/` |
| `internal/ironfurnaces/GeneratorMode.java` | `modern/common/src/main/java/.../internal/ironfurnaces/` |
| `internal/ironfurnaces/IronFurnaceCatchupHandler.java` | `modern/common/src/main/java/.../internal/ironfurnaces/` |
| `mixin/IFurnaceAccessor.java` | `modern/common/src/main/java/com/keepsmelting/mixin/` |
| `mixin/FurnaceTickMixin.java` | `modern/common/src/main/java/com/keepsmelting/mixin/` |

**Из `src/main/java/com/keepsmelting/` в `modern/forge/...`:**

| Файл | Новый путь |
|---|---|
| `KeepSmelting.java` | `modern/forge/src/main/java/com/keepsmelting/` |
| `KeepSmeltingConfig.java` | `modern/forge/src/main/java/com/keepsmelting/` |
| `command/KeepSmeltingCommand.java` | `modern/forge/src/main/java/com/keepsmelting/command/` |
| `mixin/ironfurnaces/IronFurnaceAccessor.java` | `modern/forge/src/main/java/.../mixin/ironfurnaces/` |

**Ресурсы:**

| Файл | Новый путь |
|---|---|
| `keepsmelting.mixins.json` | `modern/forge/src/main/resources/` |
| `META-INF/accesstransformer.cfg` | `modern/forge/src/main/resources/META-INF/` |
| `templates/META-INF/mods.toml` | `modern/forge/src/main/templates/META-INF/` |
| `templates/pack.mcmeta` | `modern/forge/src/main/resources/` |

### Шаг 1.4: Проверка

```powershell
cd modern
.\gradlew clean
.\gradlew build
.\gradlew runClient
```

**Критерий готовности Фазы 1:** Мод собирается и работает как раньше. Код в новой структуре.

---

## ФАЗА 2: Multi-loader + Multi-version (Stonecutter, MC ≥ 1.17)

**Цель:** Один проект `modern/` собирает Forge + Fabric для 1.17+.

### Шаг 2.1: Установить JDK 25

```powershell
# Скачать Temurin JDK 25 MSI с https://adoptium.net
java -version  # → openjdk version "25" ...
```

### Шаг 2.2: Обновить Gradle до 9.1+

```powershell
cd modern
.\gradlew wrapper --gradle-version 9.1.0
```

### Шаг 2.3: Обновить legacyforge до 2.0.141

```gradle
// modern/build.gradle
plugins {
    id 'net.neoforged.moddev.legacyforge' version '2.0.141'
}
```

### Шаг 2.4: Добавить Stonecutter

```gradle
// modern/settings.gradle
plugins {
    id 'org.gradle.toolchains.foojay-resolver-convention' version '0.9.0'
    id 'stonecutter' version '0.9.6'
}

stonecutter {
    create(rootProject) {
        versions(
            "1.20.1-forge",
            "1.20.1-fabric",
            "1.21-forge",
            "1.21-fabric"
        )
        vcs("1.20.1-forge", "1.20.1-fabric")
    }
}

rootProject.name = 'keepsmelting-modern'
```

### Шаг 2.5: Структура modern/ под Stonecutter

```
modern/
├── settings.gradle
├── build.gradle                 ← общие настройки (java, publishing)
├── stonecutter.gradle.kts       ← конфиг Stonecutter
│
├── common/                      ← ☑️ core-код (НЕ меняется)
│   └── src/main/java/com/keepsmelting/
│       ├── api/
│       ├── internal/
│       └── mixin/
│
├── versions/
│   ├── 1.20.1-forge/
│   │   ├── build.gradle
│   │   └── src/main/java/com/keepsmelting/
│   │       ├── KeepSmelting.java       ← @Mod
│   │       ├── KeepSmeltingConfig.java ← ForgeConfigSpec
│   │       └── command/
│   │
│   ├── 1.20.1-fabric/
│   │   ├── build.gradle
│   │   └── src/main/java/com/keepsmelting/
│   │       ├── KeepSmelting.java       ← ModInitializer
│   │       └── KeepSmeltingConfig.java ← Fabric Config API
│   │
│   ├── 1.21-forge/
│   │   └── ...
│   │
│   └── 1.21-fabric/
│       └── ...
│
└── libs/
    └── ironfurnaces-1.20.1-4.1.8.jar
```

### Шаг 2.6: Forge-версия `versions/1.20.1-forge/build.gradle`

```gradle
plugins {
    id 'java-library'
    id 'net.neoforged.moddev.legacyforge' version '2.0.141'
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

legacyForge {
    version = "1.20.1-47.4.10"
    runs {
        client { ideName = "Forge 1.20.1 Client" }
        server { ideName = "Forge 1.20.1 Server" }
    }
}

sourceSets.main.java.srcDirs = [
    '../../common/src/main/java',  // ☑️ core-код
    'src/main/java'                // 🟠 Forge-код
]

dependencies {
    compileOnly files('../../libs/ironfurnaces-1.20.1-4.1.8.jar')
}

mixin {
    add sourceSets.main, "keepsmelting.refmap.json"
    config "keepsmelting.mixins.json"
}
```

### Шаг 2.7: Fabric-версия `versions/1.20.1-fabric/build.gradle`

```gradle
plugins {
    id 'fabric-loom' version '1.7.+'
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

sourceSets.main.java.srcDirs = [
    '../../common/src/main/java',  // ☑️ core-код
    'src/main/java'                // 🟡 Fabric-код
]

dependencies {
    minecraft "com.mojang:minecraft:1.20.1"
    mappings loom.officialMojangMappings()
    modImplementation "net.fabricmc:fabric-loader:0.15.11"
    modImplementation "net.fabricmc.fabric-api:fabric-api:0.92.0+1.20.1"
}

loom {
    mixin.defaultRefmapName = "keepsmelting.refmap.json"
}

processResources {
    inputs.property "version", project.version
    filesMatching("fabric.mod.json") {
        expand "version": project.version
    }
}
```

### Шаг 2.8: Что различается между Forge и Fabric

| Аспект | Forge | Fabric |
|---|---|---|
| **Entry point** | `@Mod("keepsmelting")` class | `ModInitializer` class |
| **Конфиг** | `ForgeConfigSpec` + `@Config` | Fabric Config API или YACL |
| **Burn time API** | `ForgeHooks.getBurnTime(fuel, recipeType)` | `AbstractFurnaceBlockEntity.getBurnTime(registryAccess, recipeType, fuel)` |
| **Регистрация команд** | `RegisterCommandsEvent` | `CommandRegistrationCallback.EVENT.register(...)` |
| **Мод-описатель** | `META-INF/mods.toml` | `fabric.mod.json` |
| **Iron Furnaces** | ✅ Поддерживается миксинами | ❌ Не существует на Fabric |
| **Dependencies** | `compileOnly ironfurnaces.jar` | Нет ironfurnaces |

**Iron Furnaces работает только на Forge.** Fabric-сборка просто не включает `ironfurnaces/` миксины.

### Шаг 2.9: BurnTimeProvider — абстракция между loader'ами

Чтобы core-код в `common/` не зависел от `ForgeHooks`:

```java
// modern/common/src/main/java/com/keepsmelting/api/BurnTimeProvider.java
@FunctionalInterface
public interface BurnTimeProvider {
    int getBurnTime(ItemStack fuel, @Nullable RecipeType<?> recipeType);
}
```

Регистрация при старте мода:

```java
// forge/.../KeepSmelting.java — Forge
@Mod("keepsmelting")
public class KeepSmelting {
    public KeepSmelting() {
        CatchupHandlerRegistry.setBurnTimeProvider((fuel, recipeType) ->
            ForgeHooks.getBurnTime(fuel, recipeType));
    }
}
```

```java
// fabric/.../KeepSmelting.java — Fabric
public class KeepSmelting implements ModInitializer {
    @Override
    public void onInitialize() {
        CatchupHandlerRegistry.setBurnTimeProvider((fuel, recipeType) ->
            AbstractFurnaceBlockEntity.getBurnTime(
                MinecraftServer.getServer().registryAccess(),
                recipeType, fuel  // Fabric: recipe != null ? recipe.value() : null
            ));
    }
}
```

### Шаг 2.10: Добавить новые MC-версии

Просто создать новую папку в `versions/`:

```
versions/
├── 1.20.1-forge/      (--release 17)
├── 1.20.1-fabric/     (--release 17)
├── 1.21-forge/        (--release 21)
├── 1.21-fabric/       (--release 21)
├── 1.21.4-forge/      (--release 25)
└── 1.21.4-fabric/     (--release 25)
```

Каждый `build.gradle` подключает `../../common/src/main/java` и указывает свой `--release`.

---

## ФАЗА 3: Legacy-версии (опционально)

**Цель:** Добавить поддержку старых версий Minecraft (Forge + где возможно Fabric).

### Какие loader'ы доступны для старых версий

| Версия | Forge | Fabric | Примечание |
|---|---|---|---|
| **1.16.5** | ✅ Forge 36.2 | ✅ Fabric (Fabric Loom 0.x) | Fabric работает нормально |
| **1.12.2** | ✅ Forge 14.23 | ⚠️ [Fabric Legacy](https://legacy-fabric.github.io/) | Отдельный fabric-loom, меньше API |
| **1.7.10** | ✅ Forge 10.13 | ⚠️ Fabric Legacy | Очень мало API, чистый Minecraft |

**Fabric Legacy** — форк Fabric для версий 1.12.2 и ниже. Работает, но:
- Меньше модулей Fabric API
- Нет garantii совместимости со всеми модами
- `fabric.mod.json` — тот же формат

### Принцип: как legacy подключает core-код

```gradle
sourceSets.main.java.srcDirs += '../modern/common/src/main/java'
```

**Одна строка.** Gradle подхватывает файлы из `modern/common/`.

**Проблема:** Java 8 не понимает Java 17 синтаксис.
**Решение:** Писать core-код в Java 8 compatible стиле:

```java
// ✅ Java 8 compatible — никаких records, var, switch expressions
public final class CatchupResult {
    private final long itemsCooked;
    private final long fuelUsed;
    
    public CatchupResult(long itemsCooked, long fuelUsed) {
        this.itemsCooked = itemsCooked;
        this.fuelUsed = fuelUsed;
    }
    
    public long getItemsCooked() { return itemsCooked; }
    public long getFuelUsed() { return fuelUsed; }
}
```

### Шаг 3.1: `legacy-1.16.5/forge/build.gradle`

```gradle
buildscript {
    repositories {
        maven { url = 'https://maven.minecraftforge.net/' }
        mavenCentral()
    }
    dependencies {
        classpath 'net.minecraftforge.gradle:ForgeGradle:5.1.+'
    }
}
apply plugin: 'net.minecraftforge.gradle'

java.toolchain.languageVersion = JavaLanguageVersion.of(8)

minecraft {
    mappings channel: 'official', version: '1.16.5'
}

dependencies {
    minecraft 'net.minecraftforge:forge:1.16.5-36.2.39'
}

sourceSets.main.java.srcDirs += '../../modern/common/src/main/java'
```

### Шаг 3.2: `legacy-1.16.5/fabric/build.gradle`

```gradle
plugins {
    id 'fabric-loom' version '0.12.+'
}

java {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
}

dependencies {
    minecraft "com.mojang:minecraft:1.16.5"
    mappings "net.fabricmc:yarn:1.16.5+build.10:v2"
    modImplementation "net.fabricmc:fabric-loader:0.14.22"
    modImplementation "net.fabricmc.fabric-api:fabric-api:0.46.6+1.16.5"
}

sourceSets.main.java.srcDirs += '../../modern/common/src/main/java'
```

### Шаг 3.3: `legacy-1.12.2/forge/build.gradle`

```gradle
buildscript {
    repositories {
        jcenter()
        maven { url = 'https://files.minecraftforge.net/maven' }
    }
    dependencies {
        classpath 'net.minecraftforge.gradle:ForgeGradle:2.3-SNAPSHOT'
    }
}
apply plugin: 'net.minecraftforge.gradle.forge'

sourceCompatibility = JavaVersion.VERSION_1_8
targetCompatibility = JavaVersion.VERSION_1_8

minecraft {
    version = "1.12.2-14.23.5.2859"
    mappings = "stable_39"
}

sourceSets.main.java.srcDirs += '../../modern/common/src/main/java'
```

### Шаг 3.4: `legacy-1.12.2/fabric/build.gradle`

```gradle
plugins {
    id 'fabric-loom' version '1.0-SNAPSHOT' apply false
    // Используется Legacy Fabric: https://legacy-fabric.github.io/
}

// Fabric для 1.12.2 через Legacy Fabric Maven
repositories {
    maven { url = 'https://maven.legacyfabric.net/' }
}

dependencies {
    minecraft "com.mojang:minecraft:1.12.2"
    mappings "net.fabricmc:yarn:1.12.2+build.202102222318:v2"
    modImplementation "net.fabricmc:fabric-loader:0.14.22"
    // Legacy Fabric API — ограниченный набор
    modImplementation "net.legacyfabric:fabric-api:0.3.0+1.12.2"
}

sourceSets.main.java.srcDirs += '../../modern/common/src/main/java'
```

### Шаг 3.5: `legacy-1.7.10/forge/build.gradle`

```gradle
buildscript {
    repositories {
        mavenCentral()
        maven { url = 'https://files.minecraftforge.net/maven' }
    }
    dependencies {
        classpath 'net.minecraftforge.gradle:ForgeGradle:1.2-SNAPSHOT'
    }
}
apply plugin: 'forge'

minecraft {
    version = "1.7.10-10.13.4.1614-1.7.10"
}

sourceSets.main.java.srcDirs += '../../modern/common/src/main/java'
```

### Шаг 3.6: `legacy-1.7.10/fabric/build.gradle`

```gradle
plugins {
    id 'fabric-loom' version '1.0-SNAPSHOT' apply false
}

repositories {
    maven { url = 'https://maven.legacyfabric.net/' }
}

dependencies {
    minecraft "com.mojang:minecraft:1.7.10"
    mappings "net.fabricmc:yarn:1.7.10+build.202102222318:v2"
    modImplementation "net.fabricmc:fabric-loader:0.14.22"
    // Legacy Fabric API для 1.7.10 — минимальный
}

sourceSets.main.java.srcDirs += '../../modern/common/src/main/java'
```

### Шаг 3.7: Общая структура legacy после Фазы 3

```
legacy-1.16.5/
├── forge/
│   ├── build.gradle
│   └── src/main/java/com/keepsmelting/
│       ├── KeepSmelting.java       ← Forge entry point
│       ├── KeepSmeltingConfig.java ← Forge config
│       └── mixin/
├── fabric/
│   ├── build.gradle
│   └── src/main/java/com/keepsmelting/
│       ├── KeepSmelting.java       ← Fabric entry point
│       ├── KeepSmeltingConfig.java
│       └── mixin/
├── settings.gradle
├── gradlew
└── gradle/wrapper/

legacy-1.12.2/           ← та же структура
legacy-1.7.10/           ← та же структура
```

---

## Полная карта: что компилирует core-код

```
modern/common/src/main/java/  ← ☑️ ИСТИННЫЙ ИСТОЧНИК (Java 8 compatible)
  │
  ├── modern/versions/1.20.1-forge/     (--release 17)
  ├── modern/versions/1.20.1-fabric/    (--release 17)
  ├── modern/versions/1.21-forge/       (--release 21)
  ├── modern/versions/1.21-fabric/      (--release 21)
  ├── legacy-1.16.5/forge/              (Java 8)
  ├── legacy-1.16.5/fabric/             (Java 8)
  ├── legacy-1.12.2/forge/              (Java 8)
  └── legacy-1.12.2/fabric/             (Java 8)
```

Все подключают `modern/common/` через `srcDirs +=`. Изменяете core-код — пересобираете любой проект.

---

## Roadmap

```
ФАЗА 1 — 1.20.1 Forge                   ⬅️ НАЧАТЬ СЕГОДНЯ
  ├── modern/common/ + modern/forge/
  ├── сборка работает
  └── запуск клиента работает

ФАЗА 2 — Multi-loader + Multi-version   ⬅️ ПОТОМ
  ├── JDK 25 + Gradle 9.1
  ├── Stonecutter
  ├── Fabric поддержка (1.20.1)
  └── Новые MC-версии (1.21, 1.21.4)

ФАЗА 3 — Legacy                         ⬅️ ОПЦИОНАЛЬНО
  ├── 1.16.5 Forge + Fabric
  ├── 1.12.2 Forge + Fabric Legacy
  └── 1.7.10 Forge + Fabric Legacy
```
