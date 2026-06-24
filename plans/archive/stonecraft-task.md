# Задача: Развернуть Stonecraft-template для KeepSmelting

## Цель
Создать новый multi-version + multi-loader проект на основе Stonecraft-template,
перенести core-код из modern/common/ и адаптировать KeepSmelting под Architectury.

## Ограничения
- **modern/** остаётся как есть (Forge 1.20.1, legacyforge, Gradle 8.14)
- Новый проект создаётся в отдельной папке `modern-multiloader/`
- Core-код из `modern/common/` копируется в новый проект
- Iron Furnaces поддержка ТОЛЬКО для Forge

## Структура после выполнения

```
keepsmelting/
├── modern/                          ← Forge 1.20.1 (Фаза 1, НЕ ТРОГАТЬ)
│   ├── common/src/                  ← ☑️ core-код (источник)
│   ├── forge/src/                   ← Forge entry point
│   ├── build.gradle                 ← legacyforge, Gradle 8.14
│   └── gradlew
│
├── modern-multiloader/              ← 🆕 Stonecraft-template
│   ├── settings.gradle.kts
│   ├── build.gradle.kts
│   ├── stonecutter.gradle.kts
│   ├── src/main/java/               ← common код (common/)
│   ├── versions/dependencies/       ← версии зависимостей по MC
│   ├── forge/                       ← Forge platform
│   ├── fabric/                      ← Fabric platform
│   ├── neoforge/                    ← NeoForge platform (1.21+)
│   ├── gradlew (Gradle 9.3)
│   └── libs/
│       └── ironfurnaces-1.20.1-4.1.8.jar
│
├── legacy-1.16.5/                   ← пусто (Фаза 3)
├── legacy-1.12.2/                   ← пусто (Фаза 3)
├── legacy-1.7.10/                   ← пусто (Фаза 3)
│
└── plans/
    └── restructure-phases.md
```

## Пошаговый план

### Шаг 1: Развернуть Stonecraft-template

Скопировать содержимое `templates/Stonecraft-template-main/` в `modern-multiloader/`.

Файлы для копирования:
- build.gradle.kts
- settings.gradle.kts
- stonecutter.gradle.kts
- gradle.properties
- gradlew / gradlew.bat
- gradle/wrapper/ (весь)
- scripts/
- .github/
- .gitignore
- .editorconfig
- src/main/java/com/example/ExampleMod.java → переименовать позже
- src/main/resources/

НЕ копировать:
- .releaserc.json (пока не нужен)
- README.md

### Шаг 2: Настроить settings.gradle.kts

```kotlin
pluginManagement {
    repositories {
        mavenLocal()
        mavenCentral()
        gradlePluginPortal()
        maven("https://maven.kikugie.dev/releases")
        maven("https://maven.kikugie.dev/snapshots")
        maven("https://maven.fabricmc.net/")
        maven("https://maven.architectury.dev")
        maven("https://maven.minecraftforge.net")
        maven("https://maven.neoforged.net/releases/")
    }
}
plugins {
    id("gg.meza.stonecraft") version "1.10.+"
    id("dev.kikugie.stonecutter") version "0.9.+"
}

stonecutter {
    centralScript = "build.gradle.kts"
    kotlinController = true
    shared {
        fun mc(version: String, vararg loaders: String) {
            for (it in loaders) version("$version-$it", version)
        }
        mc("1.20.1", "forge", "fabric")
        mc("1.21", "neoforge", "fabric")
    }
    create(rootProject)
}

rootProject.name = "keepsmelting-multiloader"
```

### Шаг 3: Создать файлы версий

`versions/dependencies/1.20.1.properties`:
```properties
minecraft_version=1.20.1
loader_version=0.15.11

# Forge
forge_version=47.4.10

# Fabric
fabric_version=0.92.0+1.20.1
```

`versions/dependencies/1.21.properties`:
```properties
minecraft_version=1.21
loader_version=0.16.5

# NeoForge
neoforge_version=21.0.0

# Fabric
fabric_version=0.100.0+1.21
```

### Шаг 4: Обновить gradle.properties

```properties
org.gradle.jvmargs=-Xmx2G
org.gradle.parallel=false
org.gradle.caching=false

mod.id=keepsmelting
mod.name=KeepSmelting
mod.version=1.0.0
mod.group=com.keepsmelting
mod.description=Furnaces keep smelting even when you are offline.
```

### Шаг 5: Перенести core-код из modern/common/

Копировать:
```
modern/common/src/main/java/com/keepsmelting/api/*         → modern-multiloader/src/main/java/com/keepsmelting/api/
modern/common/src/main/java/com/keepsmelting/internal/*     → modern-multiloader/src/main/java/com/keepsmelting/internal/
modern/common/src/main/java/com/keepsmelting/mixin/*        → modern-multiloader/src/main/java/com/keepsmelting/mixin/
```

Убрать:
- `ForgeHooks.getBurnTime()` → заменить на `@ExpectPlatform`
- `KeepSmeltingConfig` → разделить на ForgeConfigSpec (Forge) и Fabric Config (Fabric)
- Iron Furnaces миксины → только для Forge

### Шаг 6: Создать common entry point

`src/main/java/com/keepsmelting/KeepSmeltingCommon.java`:
```java
package com.keepsmelting;

import com.keepsmelting.api.CatchupHandlerRegistry;
import com.keepsmelting.internal.catchup.VanillaCatchupHandler;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class KeepSmeltingCommon {
    public static final String MOD_ID = "keepsmelting";
    public static final Logger LOGGER = LoggerFactory.getLogger("KeepSmelting");

    public static void init() {
        CatchupHandlerRegistry.register(AbstractFurnaceBlockEntity.class, VanillaCatchupHandler.INSTANCE);
        LOGGER.info("KeepSmelting initialized");
    }
}
```

### Шаг 7: Создать @ExpectPlatform для BurnTime

```java
// src/main/java/com/keepsmelting/internal/catchup/BurnTimeHelper.java
package com.keepsmelting.internal.catchup;

import dev.architectury.injectables.annotations.ExpectPlatform;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeType;

public class BurnTimeHelper {
    @ExpectPlatform
    public static int getBurnTime(ItemStack fuel, RecipeType<?> recipeType) {
        throw new AssertionError();
    }
}
```

Реализация для Forge в `forge/src/main/java/com/keepsmelting/mixin/.../ForgeBurnTimeImpl.java`:
```java
package com.keepsmelting.internal.catchup;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraftforge.common.ForgeHooks;

public class BurnTimeHelperImpl {
    public static int getBurnTime(ItemStack fuel, RecipeType<?> recipeType) {
        return ForgeHooks.getBurnTime(fuel, recipeType);
    }
}
```

Реализация для Fabric в `fabric/src/main/java/.../FabricBurnTimeImpl.java`:
```java
package com.keepsmelting.internal.catchup;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;

public class BurnTimeHelperImpl {
    public static int getBurnTime(ItemStack fuel, RecipeType<?> recipeType) {
        return AbstractFurnaceBlockEntity.getBurnTime(
            MinecraftServer.getServer().registryAccess(), recipeType, fuel);
    }
}
```

### Шаг 8: Создать Forge entry point

`forge/src/main/java/com/keepsmelting/KeepSmeltingForge.java`:
```java
package com.keepsmelting;

import com.keepsmelting.api.CatchupHandlerRegistry;
import com.keepsmelting.command.KeepSmeltingCommand;
import com.keepsmelting.internal.ironfurnaces.IronFurnaceCatchupHandler;
import ironfurnaces.tileentity.furnaces.BlockIronFurnaceTileBase;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.fml.common.Mod;

@Mod(KeepSmeltingCommon.MOD_ID)
public class KeepSmeltingForge {
    public KeepSmeltingForge() {
        KeepSmeltingCommon.init();
        KeepSmeltingForgeConfig.register();

        try {
            Class.forName("ironfurnaces.tileentity.furnaces.BlockIronFurnaceTileBase");
            CatchupHandlerRegistry.register(BlockIronFurnaceTileBase.class, IronFurnaceCatchupHandler.INSTANCE);
            KeepSmeltingCommon.LOGGER.info("Iron Furnaces support registered");
        } catch (ClassNotFoundException e) {
            KeepSmeltingCommon.LOGGER.info("Iron Furnaces not detected");
        }

        MinecraftForge.EVENT_BUS.addListener(this::onRegisterCommands);
    }

    private void onRegisterCommands(RegisterCommandsEvent event) {
        KeepSmeltingCommand.register(event.getDispatcher());
    }
}
```

### Шаг 9: Создать Fabric entry point

`fabric/src/main/java/com/keepsmelting/KeepSmeltingFabric.java`:
```java
package com.keepsmelting;

import com.keepsmelting.command.KeepSmeltingCommand;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;

public class KeepSmeltingFabric implements ModInitializer {
    @Override
    public void onInitialize() {
        KeepSmeltingCommon.init();
        KeepSmeltingFabricConfig.init();

        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            KeepSmeltingCommand.register(dispatcher);
        });
    }
}
```

### Шаг 10: Настроить миксины

- Общие миксины (FurnaceTickMixin, IFurnaceAccessor) → `src/main/resources/`
- Forge миксины (IronFurnaceAccessor, IronFurnaceTickMixin) → `forge/src/main/resources/`
- Fabric миксины → `fabric/src/main/resources/`

Указать в `architectury.common.json`:
```json
{
    "injectors": {
        "defaultRequire": 1
    }
}
```

### Шаг 11: Проверить сборку

```bash
cd modern-multiloader
./gradlew chiseledBuild
```

### Шаг 12: Перенести библиотеку Iron Furnaces

```bash
copy modern\libs\ironfurnaces-1.20.1-4.1.8.jar modern-multiloader\libs\
```

Добавить зависимость в forge/build.gradle.kts:
```kotlin
dependencies {
    // ... существующие зависимости
    compileOnly(files("../../libs/ironfurnaces-1.20.1-4.1.8.jar"))
}
```

## Важные замечания

1. **Iron Furnaces** — только на Forge. Fabric сборка не включает ironfurnaces миксины.
2. **Конфиг** — разделить на три версии:
   - Common: интерфейс/абстракция
   - Forge: ForgeConfigSpec
   - Fabric: простой properties-файл (уже есть в Fabric версии)
3. **@ExpectPlatform** используется только для статических методов (типа getBurnTime).
4. **Mixins** — общие остаются в common, loader-специфичные в своих папках.
5. **gradlew** — использовать из шаблона (Gradle 9.3.1), он уже правильный.

## Что НЕ нужно делать

- ❌ Не трогать modern/ — он остаётся как Forge 1.20.1
- ❌ Не переносить .gradle/ кеш
- ❌ Не трогать планы в plans/
- ❌ Не изменять корневой settings.gradle
