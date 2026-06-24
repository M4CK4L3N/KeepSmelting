# Структура репозитория: Stonecutter + отдельные проекты (v2)

> **Упрощение:** JDK 25 может компилировать код для Java 17, 21 и 25 через `--release`.
> Все modern-версии используют **один JDK 25** и только флаг `--release`.

## 1. Реальная картина JDK

| Версия Minecraft | JDK для запуска игры | Целевая Java (`--release`) | Gradle JDK |
|---|---|---|---|
| 1.17 – 1.20.4 | 17+ | **17** | 25 |
| 1.20.5 – 1.21.1 | 21+ | **21** | 25 |
| 1.21.2+ | 25+ | **25** | 25 |
| 1.12.2 – 1.16.5 | **8** | **8** | **8** (отдельный проект) |
| 1.7.10 | **8** | **8** | **8** (отдельный проект) |

**Ключевой вывод:** Для MC ≥ 1.17 — **один JDK 25**, один Stonecutter-проект. Разные `--release` флаги.

---

## 2. Итоговая структура

```
keepsmelting/                          ← корень Git-репозитория
│
├── .github/
│   └── workflows/
│       ├── build-modern.yml           ← CI для Stonecutter (1.17+)
│       ├── build-1.16.5.yml           ← CI для 1.16.5
│       ├── build-1.12.2.yml           ← CI для 1.12.2
│       └── build-1.7.10.yml           ← CI для 1.7.10
│
├── shared/                            ← ☑️ ИСТИННЫЙ ИСТОЧНИК общего кода
│   └── src/main/java/com/keepsmelting/
│       ├── api/                       ← IFurnaceCatchupHandler, CatchupHandlerRegistry
│       └── internal/                  ← AbstractCatchupHandler, VanillaCatchupHandler, etc.
│
├── modern/                            ← 🟢 Stonecutter (MC 1.17+), Gradle JDK 25
│   ├── settings.gradle                ← Stonecutter 0.9.6 + fooijay
│   ├── build.gradle                   ← общие настройки, java { } блок
│   ├── gradle.properties
│   ├── gradlew / gradlew.bat
│   ├── gradle/wrapper/
│   ├── stonecutter.gradle.kts
│   │
│   ├── common/                        ← общий код для ВСЕХ modern-версий
│   │   └── src/main/java/com/keepsmelting/
│   │       ├── api/                   ← Junction → shared/api/
│   │       ├── internal/              ← Junction → shared/internal/
│   │       └── mixin/                 ← общие миксины (VanillaCatchupHandler)
│   │
│   ├── versions/
│   │   ├── 1.17.1/                    ← --release 17
│   │   │   ├── build.gradle
│   │   │   └── src/main/java/com/keepsmelting/
│   │   │       ├── KeepSmelting.java  ← платформенный entry point
│   │   │       └── mixin/             ← версионные миксины
│   │   │
│   │   ├── 1.18.2/                    ← --release 17
│   │   │   └── ...
│   │   │
│   │   ├── 1.20.1/                    ← --release 17 ← ⭐ ВАША ТЕКУЩАЯ
│   │   │   ├── build.gradle
│   │   │   └── src/main/java/com/keepsmelting/
│   │   │       ├── KeepSmelting.java
│   │   │       └── mixin/
│   │   │
│   │   ├── 1.21/                      ← --release 21
│   │   │   └── ...
│   │   │
│   │   └── 1.21.4/                    ← --release 25
│   │       └── ...
│   │
│   └── libs/
│       └── ironfurnaces-1.20.1-4.1.8.jar
│
├── legacy-1.16.5/                     ← 🟠 отдельный проект (JDK 8)
│   ├── build.gradle                   ← ForgeGradle 5.1+
│   ├── settings.gradle
│   ├── gradlew
│   └── src/main/java/com/keepsmelting/
│       ├── api/                       ← Junction → ../../shared/api/
│       ├── internal/                  ← Junction → ../../shared/internal/
│       ├── KeepSmelting.java
│       ├── KeepSmeltingConfig.java
│       └── mixin/
│
├── legacy-1.12.2/                     ← 🟠 отдельный проект (JDK 8)
│   ├── build.gradle                   ← ForgeGradle 2.3
│   ├── settings.gradle
│   ├── gradlew
│   └── src/main/java/com/keepsmelting/
│       ├── api/                       ← Junction → shared/
│       ├── internal/                  ← Junction → shared/
│       ├── KeepSmelting.java
│       └── mixin/
│
├── legacy-1.7.10/                     ← 🟠 отдельный проект (JDK 8)
│   ├── build.gradle                   ← ForgeGradle 1.2
│   ├── settings.gradle
│   ├── gradlew
│   └── src/main/java/com/keepsmelting/
│       ├── api/                       ← Junction → shared/
│       ├── internal/                  ← Junction → shared/
│       ├── KeepSmelting.java
│       └── mixin/
│
├── sync-shared.ps1                    ← 🔄 создаёт Junction из shared/ во все проекты
├── build-all.ps1                      ← 🚀 сборка всего
└── README.md
```

---

## 3. Почему один JDK 25 для всех modern-версий

```gradle
// modern/versions/1.20.1/build.gradle
java {
    // Gradle на JDK 25, но байткод будет Java 17
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

// modern/versions/1.21/build.gradle
java {
    // Gradle на JDK 25, байткод Java 21
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
}
```

Компилятор **JDK 25** умеет:
- `--release 17` → байткод Java 17 (для MC 1.17–1.20.4)
- `--release 21` → байткод Java 21 (для MC 1.20.5–1.21.1)
- `--release 25` → байткод Java 25 (для MC ≥ 1.21.2)

Никаких дополнительных установок JDK 17 или 21.

---

## 4. Как работает `shared/`

`shared/` — единственное место для core-логики:

```
shared/src/main/java/com/keepsmelting/
├── api/
│   ├── IFurnaceCatchupHandler.java
│   └── CatchupHandlerRegistry.java
├── internal/
│   ├── AbstractCatchupHandler.java
│   ├── VanillaCatchupHandler.java
│   └── VanillaHopperIO.java
```

Каждый проект **симлинкует** этот код через `sync-shared.ps1`:

```powershell
# sync-shared.ps1
$shared = ".\shared\src\main\java\com\keepsmelting"
$targets = @(
    ".\modern\common\src\main\java\com\keepsmelting",
    ".\legacy-1.16.5\src\main\java\com\keepsmelting",
    ".\legacy-1.12.2\src\main\java\com\keepsmelting",
    ".\legacy-1.7.10\src\main\java\com\keepsmelting"
)

foreach ($target in $targets) {
    if (Test-Path "$target\api") { Remove-Item "$target\api" -Recurse -Force }
    if (Test-Path "$target\internal") { Remove-Item "$target\internal" -Recurse -Force }
    New-Item -ItemType Junction -Path "$target\api" -Target "$shared\api"
    New-Item -ItemType Junction -Path "$target\internal" -Target "$shared\internal"
}
```

---

## 5. `modern/settings.gradle` — Stonecutter config

```gradle
plugins {
    id 'org.gradle.toolchains.foojay-resolver-convention' version '0.9.0'
    id 'stonecutter' version '0.9.6'
}

stonecutter {
    create(rootProject) {
        versions("1.17.1", "1.18.2", "1.20.1", "1.21", "1.21.4")
        // vcs = только те, что реально поддерживаем
        vcs("1.20.1", "1.21", "1.21.4")
    }
}

rootProject.name = 'keepsmelting-modern'
```

---

## 6. `modern/versions/1.20.1/build.gradle`

```gradle
plugins {
    id 'java'
    id 'net.neoforged.moddev.legacyforge' version '2.0.141'
}

java {
    // Gradle на JDK 25, байткод для Java 17
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

dependencies {
    implementation project(':common')
    compileOnly files('libs/ironfurnaces-1.20.1-4.1.8.jar')
}

mixin {
    add sourceSets.main, "keepsmelting.refmap.json"
    config "keepsmelting.mixins.json"
}
```

---

## 7. Поток работы

### Разработка новой фичи в shared/api:

```
1. Правите shared/src/.../VanillaCatchupHandler.java
2. sync-shared.ps1          → симлинки обновлены во всех проектах
3. cd modern && ./gradlew :1.20.1:build   → проверка на 1.20.1
4. ./gradlew :stonecutter:setActiveTo_1_21
   ./gradlew :1.21:build                   → проверка на 1.21
5. cd ..\legacy-1.16.5 && .\gradlew build  → проверка на 1.16.5
```

### Добавление новой MC-версии:

```
1. Добавить в modern/settings.gradle (stonecutter.versions)
2. Создать modern/versions/X.X.X/build.gradle
3. Написать версионные миксины
4. ./gradlew :X.X.X:build
```

---

## 8. Сводка JDK

| Раздел репозитория | JDK для Gradle | Целевая Java | Почему |
|---|---|---|---|
| `modern/` | **25** | `--release 17/21/25` | Stonecutter требует JDK 25 |
| `legacy-1.16.5/` | **8** | Java 8 | ForgeGradle 5.x + Minecraft 1.16.5 |
| `legacy-1.12.2/` | **8** | Java 8 | ForgeGradle 2.3 + Minecraft 1.12.2 |
| `legacy-1.7.10/` | **8** | Java 8 | ForgeGradle 1.2 + Minecraft 1.7.10 |
