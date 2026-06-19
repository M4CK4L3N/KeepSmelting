# План реструктуризации KeepSmelting (по фазам)

> **Ключевое решение:** Никакой `shared/` папки. Никаких Junction/симлинков.
> Core-код лежит в `modern/common/`. Остальные проекты подключают его через Gradle source set.

## Общая концепция

```
ФАЗА 1 (сейчас):   1.20.1 Forge       ← реструктуризация текущего проекта
                      ↓
ФАЗА 2 (скоро):    1.20.1 Forge + Fabric     ← multi-loader в modern/
                   1.21 Forge/NeoForge + Fabric  ← Stonecutter
                   1.21.4 Forge/NeoForge + Fabric ← Stonecutter
                      ↓
ФАЗА 3 (опционально):
                   1.16.5 Forge  ← отдельный legacy-проект
                   1.12.2 Forge  ← отдельный legacy-проект
                   1.7.10 Forge  ← отдельный legacy-проект
```

## Принцип: как код переиспользуется между проектами

```
modern/common/src/main/java/com/keepsmelting/
├── api/                      ← ☑️ ЕДИНСТВЕННОЕ МЕСТО core-кода
├── internal/
└── mixin/

modern/forge/                 ← подключает modern/common через Gradle sourceSet
modern/fabric/                ← подключает modern/common через Gradle sourceSet

legacy-1.16.5/                ← подключает modern/common через srcDirs +=
legacy-1.12.2/                ← подключает modern/common через srcDirs +=
legacy-1.7.10/                ← подключает modern/common через srcDirs +=
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
│   ├── forge/                         ← 🟠 Forge-специфичный код
│   │   └── src/main/java/com/keepsmelting/
│   │       ├── KeepSmelting.java
│   │       ├── KeepSmeltingConfig.java
│   │       └── command/
│   │           └── KeepSmeltingCommand.java
│   │
│   ├── fabric/                        ← 🟡 пока пусто (Фаза 2)
│   │   └── src/main/java/com/keepsmelting/
│   │       └── (пусто)
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

// templates, publishing, idea — как в текущем build.gradle
```

### Шаг 1.3: `modern/settings.gradle`

```gradle
plugins {
    id 'org.gradle.toolchains.foojay-resolver-convention' version '0.9.0'
}

rootProject.name = 'keepsmelting-modern'
```

### Шаг 1.4: `modern/gradle.properties`

Скопировать из текущего `gradle.properties`.

### Шаг 1.5: Перенести ресурсы и templates

```
modern/forge/src/main/resources/
├── META-INF/
│   └── accesstransformer.cfg    ← из src/main/resources/META-INF/
├── keepsmelting.mixins.json     ← из src/main/resources/
└── pack.mcmeta                  ← из src/main/templates/pack.mcmeta → скопировать

modern/forge/src/main/templates/
└── META-INF/
    └── mods.toml                ← из src/main/templates/META-INF/
```

### Шаг 1.6: Перенести `gradlew` + `gradlew.bat` + `gradle/wrapper/`

```powershell
# скопировать gradle wrapper из текущего корня в modern/
copy ..\gradlew .\
copy ..\gradlew.bat .\
copy ..\gradle\wrapper\gradle-wrapper.properties .\gradle\wrapper\
copy ..\gradle\wrapper\gradle-wrapper.jar .\gradle\wrapper\
```

### Шаг 1.7: Проверка

```powershell
cd modern
.\gradlew clean
.\gradlew build
.\gradlew runClient
```

**Критерий готовности Фазы 1:** Мод собирается и работает как раньше. Код лежит в `modern/common/` + `modern/forge/`.

---

## ФАЗА 2: Multi-loader + Multi-version (Stonecutter)

**Цель:** Добавить Fabric + поддержку нескольких MC-версий.

### Шаг 2.1: Установить JDK 25

```powershell
# Скачать Temurin JDK 25 MSI с https://adoptium.net
# Установить
# Проверить
java -version
# → openjdk version "25" ...
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

### Шаг 2.4: Добавить Stonecutter в `modern/settings.gradle`

```gradle
plugins {
    id 'org.gradle.toolchains.foojay-resolver-convention' version '0.9.0'
    id 'stonecutter' version '0.9.6'
}

stonecutter {
    create(rootProject) {
        versions("1.20.1-forge") // пока только одна
        vcs("1.20.1-forge")
        // позже: versions("1.20.1-forge", "1.20.1-fabric", "1.21-forge", "1.21-fabric")
    }
}

rootProject.name = 'keepsmelting-modern'
```

### Шаг 2.5: Реструктурировать под Stonecutter с multi-loader

```
modern/
├── settings.gradle
├── build.gradle
├── stonecutter.gradle.kts
│
├── common/                      ← ☑️ core-код (api + internal + общие миксины)
│   └── src/main/java/com/keepsmelting/
│       ├── api/
│       ├── internal/
│       └── mixin/
│
├── versions/
│   └── 1.20.1-forge/            ← Forge 1.20.1
│       ├── build.gradle
│       └── src/main/java/com/keepsmelting/
│           ├── KeepSmelting.java    ← @Mod
│           ├── KeepSmeltingConfig.java
│           └── command/
│
├── versions/
│   └── 1.20.1-fabric/           ← Fabric 1.20.1
│       ├── build.gradle
│       └── src/main/java/com/keepsmelting/
│           ├── KeepSmelting.java    ← ModInitializer
│           ├── KeepSmeltingConfig.java
│           └── command/
│
└── libs/
```

### Шаг 2.6: `versions/1.20.1-forge/build.gradle`

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
        client { client() }
        server { server() }
    }
}

sourceSets.main.java.srcDirs = [
    '../../common/src/main/java',   // ☑️ core-код из общей папки
    'src/main/java'                 // 🟠 Forge-специфичный код
]

dependencies {
    compileOnly files('../../libs/ironfurnaces-1.20.1-4.1.8.jar')
}

mixin {
    add sourceSets.main, "keepsmelting.refmap.json"
    config "keepsmelting.mixins.json"
}
```

### Шаг 2.7: `versions/1.20.1-fabric/build.gradle`

```gradle
plugins {
    id 'fabric-loom' version '1.7.+'
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

sourceSets.main.java.srcDirs = [
    '../../common/src/main/java',   // ☑️ core-код из общей папки
    'src/main/java'                 // 🟡 Fabric-специфичный код
]

dependencies {
    minecraft "com.mojang:minecraft:1.20.1"
    mappings loom.officialMojangMappings()
    modImplementation "net.fabricmc:fabric-loader:0.15.11"
    modImplementation "net.fabricmc.fabric-api:fabric-api:0.92.0+1.20.1"
}
```

### Шаг 2.8: BurnTimeProvider — абстракция для multi-loader

```java
// modern/common/src/main/java/com/keepsmelting/api/BurnTimeProvider.java
@FunctionalInterface
public interface BurnTimeProvider {
    int getBurnTime(ItemStack fuel, @Nullable RecipeType<?> recipeType);
}
```

```java
// forge/ — внутри VanillaCatchupHandler использует ForgeHooks
// fabric/ — внутри VanillaCatchupHandler использует AbstractFurnaceBlockEntity
```

### Шаг 2.9: Добавить новые MC-версии

Просто создать новую папку в `versions/`:

```
versions/
├── 1.20.1-forge/
├── 1.20.1-fabric/
├── 1.21-forge/
├── 1.21-fabric/
└── 1.21.4-forge/
    └── 1.21.4-fabric/
```

Каждый новый `build.gradle` подключает `../../common/src/main/java` и указывает свой `--release` флаг.

---

## ФАЗА 3: Legacy-версии (опционально)

**Цель:** Добавить поддержку старых версий Minecraft.

### Принцип: как legacy подключает core-код

```gradle
// legacy-1.12.2/build.gradle
sourceSets.main.java.srcDirs += '../modern/common/src/main/java'
```

**Одна строка.** Gradle сам подхватывает файлы из `modern/common/`.
Никаких Junction, никакой `shared/`.

**Проблема:** Java 8 не понимает Java 17 синтаксис.
**Решение:** Писать core-код в Java 8 compatible стиле:

```java
// ✅ Java 8 compatible — никаких records, var, switch expressions
public class CatchupResult {
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

### Шаг 3.1: `legacy-1.16.5/build.gradle`

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

// ☑️ core-код из modern/common — одна строка
sourceSets.main.java.srcDirs += '../modern/common/src/main/java'
```

### Шаг 3.2: `legacy-1.12.2/build.gradle`

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

// ☑️ core-код из modern/common — одна строка
sourceSets.main.java.srcDirs += '../modern/common/src/main/java'
```

### Шаг 3.3: `legacy-1.7.10/build.gradle`

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

// ☑️ core-код из modern/common — одна строка
sourceSets.main.java.srcDirs += '../modern/common/src/main/java'
```

---

## Roadmap

```
ФАЗА 1 — 1.20.1 Forge                   ⬅️ НАЧАТЬ СЕГОДНЯ
  ├── modern/common/ + modern/forge/     ✅
  ├── сборка работает                    ✅
  └── запуск клиента работает            ✅

ФАЗА 2 — Multi-loader + Multi-version   ⬅️ ПОТОМ
  ├── JDK 25 + Gradle 9.1
  ├── Stonecutter
  ├── Fabric поддержка
  └── Новые MC-версии

ФАЗА 3 — Legacy                         ⬅️ ОПЦИОНАЛЬНО
  ├── 1.16.5
  ├── 1.12.2
  └── 1.7.10
```

---

## Сводка: что меняется в коде

| Файл | Куда переносится |
|---|---|
| `IFurnaceCatchupHandler.java` | → `modern/common/src/main/java/com/keepsmelting/api/` |
| `CatchupHandlerRegistry.java` | → `modern/common/src/main/java/com/keepsmelting/api/` |
| `AbstractCatchupHandler.java` | → `modern/common/src/main/java/com/keepsmelting/internal/catchup/` |
| `VanillaCatchupHandler.java` | → `modern/common/src/main/java/com/keepsmelting/internal/catchup/` |
| `VanillaHopperIO.java` | → `modern/common/src/main/java/com/keepsmelting/internal/catchup/` |
| `FurnaceMode.java` | → `modern/common/src/main/java/com/keepsmelting/internal/ironfurnaces/` |
| `FactoryMode.java` | → `modern/common/src/main/java/com/keepsmelting/internal/ironfurnaces/` |
| `GeneratorMode.java` | → `modern/common/src/main/java/com/keepsmelting/internal/ironfurnaces/` |
| `IronFurnaceCatchupHandler.java` | → `modern/common/src/main/java/com/keepsmelting/internal/ironfurnaces/` |
| `IFurnaceAccessor.java` | → `modern/common/src/main/java/com/keepsmelting/mixin/` |
| `FurnaceTickMixin.java` | → `modern/common/src/main/java/com/keepsmelting/mixin/` |
| `KeepSmelting.java` | → `modern/forge/src/main/java/com/keepsmelting/` |
| `KeepSmeltingConfig.java` | → `modern/forge/src/main/java/com/keepsmelting/` |
| `KeepSmeltingCommand.java` | → `modern/forge/src/main/java/com/keepsmelting/command/` |
| `IronFurnaceAccessor.java` | → `modern/forge/src/main/java/com/keepsmelting/mixin/ironfurnaces/` (остаётся в forge) |
| `keepsmelting.mixins.json` | → `modern/forge/src/main/resources/` |
| `mods.toml` | → `modern/forge/src/main/templates/META-INF/` |
| `accesstransformer.cfg` | → `modern/forge/src/main/resources/META-INF/` |
