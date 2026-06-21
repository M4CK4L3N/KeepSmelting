# KeepSmelting — Возможности

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
Все 13 уровней печей + 3 режима (Furnace, Factory, Generator)

## API для других модов

Полное руководство: [API_USAGE.md](API_USAGE.md)

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
| 🇬🇧 Английский | [`en_us.json`](../../src/main/resources/assets/keepsmelting/lang/en_us.json) |
| 🇷🇺 Русский | [`ru_ru.json`](../../src/main/resources/assets/keepsmelting/lang/ru_ru.json) |

Язык игры определяется автоматически. Все команды, сообщения помощи и статус выводятся на выбранном языке.

### Как добавить новый перевод

1. Скопируйте [`en_us.json`](../../src/main/resources/assets/keepsmelting/lang/en_us.json) в `assets/keepsmelting/lang/<локаль>.json`
2. Переведите значения (ключи оставьте без изменений)
3. Отправьте pull request

---

**Смотри также:** [Указатель](INDEX.md) | [API для разработчиков](API_USAGE.md) | [README](README.md)
