# KeepSmelting API

## Для разработчиков модов

KeepSmelting предоставляет API для сторонних модов, чтобы их кастомные печи тоже получали catchup-обработку (догонялка пропущенных тиков, пока игрок был офлайн или чанк не загружен).

## Подключение

```gradle
dependencies {
    implementation fg.deobf("curse.maven:keepsmelting:XXXXXXXX")
}
```

## Быстрый старт

### Шаг 1: Наследуй AbstractCatchupHandler

```java
package com.example;

import com.keepsmelting.api.catchup.AbstractCatchupHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

public class MyFurnaceHandler extends AbstractCatchupHandler {
    @Override
    public void applyCatchup(BlockEntity tile, long elapsed, Level level, BlockPos pos) {
        // ТВОЯ ЛОГИКА ДОГОНЯЛКИ
        // elapsed — количество пропущенных тиков
    }
}
```

### Шаг 2: Зарегистрируй

```java
import com.keepsmelting.api.CatchupHandlerRegistry;

@Mod("example")
public class ExampleMod {
    public ExampleMod() {
        CatchupHandlerRegistry.register(
            MyFurnaceTileEntity.class,
            new MyFurnaceHandler()
        );
    }
}
```

### Шаг 3: Готово

KeepSmelting сам вызывает `applyCatchup()` при каждом серверном тике печи.

## Что даёт AbstractCatchupHandler

| Метод | Назначение |
|-------|-----------|
| `calcElapsed(level, now)` | Расчёт пропущенных тиков с последнего сохранения |
| `saveTime(tile, tag)` | Сохранение времени в NBT печи |
| `loadTime(tile, tag)` | Загрузка времени из NBT |
| `timeModeChanged()` | Сброс при смене конфига (gameTime/realTime) |
| `sendChatDebug(...)` | Отправка debug-сообщения рядом стоящим игрокам |

Не нужно писать расчёт времени, сохранение/загрузку или debug — всё уже готово.

## Пример для RF-печи

```java
public class MyRFHandler extends AbstractCatchupHandler {
    @Override
    public void applyCatchup(BlockEntity tile, long elapsed, Level level, BlockPos pos) {
        MyTileEntity furnace = (MyTileEntity) tile;

        int rfStored = furnace.getEnergy();
        int rfCapacity = furnace.getMaxEnergy();
        int rfPerTick = furnace.getRfPerTick();
        int rfPerItem = furnace.getRfPerItem();

        int items = furnace.getItem(0).getCount();
        int outSpace = calcOutputSpace(furnace);
        int maxSmelt = Math.min(items, outSpace);

        // Bottleneck: RF доступно vs RF нужно
        long rfNeeded = (long) maxSmelt * rfPerItem;
        long rfAvail = (long) elapsed * rfPerTick + rfStored;
        long rfEffective = Math.min(rfAvail, rfNeeded);
        int toSmelt = (int) (rfEffective / rfPerItem);

        // Применить
        for (int i = 0; i < toSmelt; i++) {
            smeltOne(furnace, level);
        }

        // Обновить RF
        int rfSpent = toSmelt * rfPerItem;
        int rfFromGen = (int) Math.min(rfSpent, rfAvail - rfStored);
        int rfFromBuf = rfSpent - rfFromGen;
        furnace.setEnergy(rfStored + rfFromGen - rfFromBuf);
        furnace.setChanged();

        sendChatDebug(level, pos, "MyFurnace", elapsed,
                0, toSmelt, 0, 0, furnace.getEnergy() > 0);
    }
}
```

## Советы

1. **RF-печи:** симуляция всегда `min(RF_доступно, RF_нужно) / RF_на_предмет`
2. **Печи без RF:** симуляция — `min(тиков_топлива, тиков_плавки, elapsed)`
3. **Хопперы и IO:** реализуй внутри `applyCatchup()` — вызови `autoIO()`
4. **Сети печей:** для factory+generator пиши свой сборщик данных по аналогии с `NetworkDataCollector`

## Что НЕ является API

Всё в пакете `com.keepsmelting.internal.*` — **внутреннее**. Может измениться в любой момент. Не используй напрямую.

---

**Смотри также:** [Содержание](INDEX.md) | [Возможности](FEATURES.md) | [README](README.md)
