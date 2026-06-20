# Changelog

## v0.1.1

### Исправления
- **Больше не вылетает без Iron Furnaces** — IF-зависимые команды (`simulate`, `spawn`, `test`) вынесены в отдельный класс, который загружается только при наличии мода. Предотвращает `NoClassDefFoundError` на `BlockIronFurnaceTileBase`.
- **Имя JAR-файла теперь включает версию** — файл называется `keepsmelting-forge-1.20.1-0.1.1.jar`, а не просто `keepsmelting-forge-1.20.1.jar`.

### Технические детали
- Весь IF-зависимый код перемещён в новый класс `IronFurnaceCommands`, который регистрируется в рантайме только при `ModList.get().isLoaded("ironfurnaces")`.
- `KeepSmeltingCommand` больше не содержит прямых ссылок на классы Iron Furnaces.
