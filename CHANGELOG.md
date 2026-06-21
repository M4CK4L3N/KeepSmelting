# Changelog

## v0.1.1

### Fixes
- **No longer crashes without Iron Furnaces** — IF-dependent commands (`simulate`, `spawn`, `test`) have been extracted into a separate class that is loaded only when the mod is present. Prevents `NoClassDefFoundError` on `BlockIronFurnaceTileBase`.
- **JAR filename now includes version** — the file is named `keepsmelting-forge-1.20.1-0.1.1.jar` instead of just `keepsmelting-forge-1.20.1.jar`.

### Technical Details
- All IF-dependent code has been moved into a new class `IronFurnaceCommands`, which is registered at runtime only when `ModList.get().isLoaded("ironfurnaces")`.
- `KeepSmeltingCommand` no longer contains direct references to Iron Furnaces classes.
