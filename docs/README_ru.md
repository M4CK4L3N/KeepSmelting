# KeepSmelting

[![CurseForge](https://img.shields.io/badge/CurseForge-keepsmelting-orange?style=flat-square)](https://www.curseforge.com/minecraft/mc-mods/keepsmelting)
[![Modrinth](https://img.shields.io/badge/Modrinth-keepsmelting-green?style=flat-square)](https://modrinth.com/mod/keepsmelting)
[![GitHub](https://img.shields.io/badge/GitHub-M4CK4L3N/KeepSmelting-181717?style=flat-square&logo=github)](https://github.com/M4CK4L3N/KeepSmelting)

Печи продолжают плавить даже когда вы офлайн. Использует реальное время для расчёта, сколько предметов было бы переплавлено.

## Возможности

- ⏰ **Режимы Real-time и Game-time** — работает при выгрузке чанков, выходе из игры и паузе меню
- 🔥 **Ванильные печи** — печь, коптильня, плавильная печь
- ⚙️ **Iron Furnaces** — полная поддержка всех 3 режимов (Furnace, Factory, Generator) со всеми слотами улучшений:
  - Красный слот (3): тип рецепта — Smelting / Blasting / Smoking
  - Зелёный слот (4): эффективность — normal / Speed (2× скорость) / Fuel (2× время горения)
  - Синий слот (5): режим — Furnace / Factory / Generator
- 📦 **Hopper I/O** — автоматически вытягивает ингредиенты/топливо из соседних контейнеров, выталкивает результат вниз
- 🛠️ **API для других модов** — добавьте поддержку KeepSmelting в свою кастомную печь
- 🔧 **Настраиваемый** — команды `/keepsmelting`
- 🌍 **Мультиязычность** — английский и русский языки

## Установка

1. Скачайте jar с [Modrinth](#) или [CurseForge](#)
2. Поместите в папку `mods/`
3. (Опционально) Установите [Iron Furnaces](https://www.curseforge.com/minecraft/mc-mods/iron-furnaces) для дополнительных типов печей

## Команды

| Команда | Описание |
|---|---|
| `/keepsmelting status` | Показать текущие настройки |
| `/keepsmelting catchup <true/false>` | Вкл/выкл офлайн-догонялку |
| `/keepsmelting maxTicks <1-192000>` | Макс. количество офлайн-тиков для симуляции (по умолч.: 24000 = 1 игровой день) |
| `/keepsmelting minDelta <1-72000>` | Мин. разрыв тиков перед запуском догонялки (по умолч.: 20 = 1 секунда) |
| `/keepsmelting debug <OFF/CHAT/LOG>` | Режим вывода отладки |
| `/keepsmelting time <REALTIME/GAMETIME>` | Режим отслеживания времени |

## Конфиг

Файл: `config/keepsmelting-common.toml`

```toml
[catchup]
catchupEnabled = true
maxCatchupTicks = 24000
minDeltaThreshold = 20
debugMode = "OFF"
timeMode = "REALTIME"
```

## Производительность

Все режимы догонялки используют **адаптивный батчинг** — O(событий) вместо O(тиков):
- Ванильная печь: ~5-20 итераций на 24000 тиков
- Iron Furnaces Furnace: ~5-20 итераций
- Iron Furnaces Factory: ~100 итераций (было 144000 до оптимизации)
- Iron Furnaces Generator: батч на цикл горения топлива

## Поддерживаемые печи

### Ванильные
- Печь
- Коптильня
- Плавильная печь

### Iron Furnaces (опциональная зависимость)
Все 13 уровней печей + 3 режима

## API для других модов

Полное руководство: [docs/API_USAGE_ru.md](docs/API_USAGE_ru.md)

Быстрый старт:

```java
// 1. Наследуйте AbstractCatchupHandler (saveTime/loadTime/calcElapsed уже готовы)
public class MyHandler extends AbstractCatchupHandler {
    @Override
    public void applyCatchup(BlockEntity tile, long elapsed, Level level, BlockPos pos) {
        // Ваша логика догонялки
    }
}

// 2. Зарегистрируйте
CatchupHandlerRegistry.register(MyTile.class, new MyHandler());
```

KeepSmelting найдёт ваш обработчик автоматически.

## Локализация

KeepSmelting поддерживает несколько языков:

| Язык | Файл |
|---|---|
| 🇬🇧 Английский | [`en_us.json`](src/main/resources/assets/keepsmelting/lang/en_us.json) |
| 🇷🇺 Русский | [`ru_ru.json`](src/main/resources/assets/keepsmelting/lang/ru_ru.json) |

Язык игры определяется автоматически. Все команды, сообщения помощи и статус выводятся на выбранном языке.

### Как добавить новый перевод

1. Скопируйте [`en_us.json`](src/main/resources/assets/keepsmelting/lang/en_us.json) в `assets/keepsmelting/lang/<локаль>.json`
2. Переведите значения (ключи оставьте без изменений)
3. Отправьте pull request

## Лицензия

MIT © M4CK4L3N

---

[🇬🇧 English documentation](README.md)
