# KeepSmelting Knowledge Base

## Build

- **Minecraft:** 1.20.1, **Forge** (LegacyForge via NeoForge MDK)
- **Java:** 17, **Gradle** 
- **Mod ID:** `keepsmelting`, **Group:** `com.keepsmelting`

```bash
./gradlew build          # Full build + test
./gradlew runClient      # Minecraft client
./gradlew test           # JUnit tests only
```

**IF dependency:** `curse.maven:iron-furnaces-237664:7888952` (compileOnly, runtime optional via mixin `defaultRequire:0`)

---

## Architecture

### Catchup flow
```
FurnaceTickMixin (@Mixin AbstractFurnaceBlockEntity)
  → CatchupHandlerRegistry.find(furnaceClass) → IFurnaceCatchupHandler
  → handler.applyCatchup(tile, elapsed, level, pos)

IronFurnaceTickMixin (@Pseudo @Mixin target=BlockIronFurnaceTileBase)
  → same pattern but targets IF tile directly
```

### CRITICAL: IF class hierarchy
`BlockIronFurnaceTileBase extends TileEntityInventory extends BlockEntity` — **does NOT** extend `AbstractFurnaceBlockEntity`. Two mixins target different hierarchies — no double processing risk.

### API for other mods
```java
// Implement this + register in your @Mod constructor:
public interface IFurnaceCatchupHandler {
    void applyCatchup(BlockEntity tile, long elapsed, Level level, BlockPos pos);
    void saveTime(BlockEntity tile, CompoundTag tag);   // optional, mixin handles
    void loadTime(BlockEntity tile, CompoundTag tag);   // optional, mixin handles
}

CatchupHandlerRegistry.register(MyFurnaceTile.class, new MyHandler());
```

`CatchupHandlerRegistry.find(class)` walks superclass hierarchy — registering against a base class catches all subclasses.

### Time tracking
**Per-tile mixin variables**, NOT handler fields:
- Vanilla: `FurnaceTickMixin.keepsmelting$lastRealTime`
- IF: `IronFurnaceTickMixin.keepsmelting$lastRealTime`
- Two modes: `REALTIME` (wall clock, works while paused) or `GAMETIME` (MC ticks)

### Deduplication: CatchupDedup
`internal.catchup.CatchupDedup` — per-tick dedup via `Set<BlockPos> processedThisTick`. Cleared each game tick by `checkNewTick(gameTime)`. Used by IF-mixin and `SimulationApplicator` (network distribution marks all gen/factory positions).

---

## Project Structure

```
src/main/java/com/keepsmelting/
├── KeepSmelting.java                   @Mod entry, IF handler registration
├── KeepSmeltingConfig.java             ForgeConfigSpec
├── api/
│   ├── IFurnaceCatchupHandler.java     3-method interface
│   └── CatchupHandlerRegistry.java     ConcurrentHashMap + superclass walk
├── api/catchup/
│   └── AbstractCatchupHandler.java     Base class with calcElapsed, sendChatDebug (static)
├── command/                            Brigadier commands
│   ├── KeepSmeltingCommand.java        Router
│   ├── ConfigSubcommand.java           /keepsmelting catchup|debug|time|maxTicks|minDelta|status
│   ├── SimulateSubcommand.java         /keepsmelting simulate <ticks>
│   ├── TestSubcommand.java            /keepsmelting test <config> [ticks]
│   ├── IFPlacementBuilder.java        IF test patterns (string-only, no IF classes)
│   └── CommandUtil.java               send, getPlayer, isChestType, getPatternsHelp
├── internal/catchup/                   Vanilla furnace pipeline
│   ├── VanillaCatchupHandler.java     extends AbstractCatchupHandler
│   ├── VanillaHopperSimulator.java    Facade
│   ├── PipelineData.java              NodeType, PipelineNode, Pipeline, SimulationResult records
│   ├── PipelineDiscoverer.java        discover() + countSmeltable/countBurnable/countSpace
│   ├── PipelineSimulator.java         bottleneck calc
│   ├── PipelineApplicator.java        apply() + fillHoppers/consumeExternal/pushOverflow
│   ├── VanillaHopperIO.java           Low-level item transfer helpers
│   └── CatchupDedup.java              Per-tick dedup
├── internal/ironfurnaces/             IF simulation
│   ├── handler/
│   │   ├── IronFurnaceCatchupHandler.java   implements IFurnaceCatchupHandler (does NOT extend AbstractCatchupHandler)
│   │   └── FurnaceMode.java                Adaptive batch furnace mode
│   ├── CatchupSimulation.java              Facade
│   ├── collect/
│   │   ├── FurnaceNetwork.java             Network discovery (gen+factory neighbors)
│   │   └── NetworkDataCollector.java       Aggregates resources from tiles
│   ├── data/
│   │   └── SimulationData.java             DTO: NetworkResources, SimulationResult, FactorySmeltParams
│   ├── simulate/
│   │   └── SimulationEngine.java           Pure math, no MC deps, JUnit tested
│   ├── apply/
│   │   └── SimulationApplicator.java       Applies results to real blocks
│   └── util/
│       ├── FurnaceFuelHandler.java
│       └── HopperHelper.java
└── mixin/
    ├── IFurnaceAccessor.java           Accessor/Invoker for AbstractFurnaceBlockEntity
    ├── FurnaceTickMixin.java           HEAD inject into serverTick
    └── ironfurnaces/
        ├── IronFurnaceAccessor.java    Accessor for BlockIronFurnaceTileBase
        └── IronFurnaceTickMixin.java   HEAD inject into tick (@Pseudo)
```

---

## Vanilla Furnace Pipeline (Hopper Simulation)

### Pipeline model
```
INPUT_SOURCE → INPUT_HOPPER → FURNACE → OUTPUT_HOPPER → OUTPUT_DEST
FUEL_SOURCE  → FUEL_HOPPER  → FURNACE
```

### Pipeline nodes
| Type | Detection | Rate |
|------|-----------|------|
| `INPUT_SOURCE` | Container above furnace or above input hopper | Unlimited |
| `INPUT_HOPPER` | Hopper above furnace with FACING=DOWN | 1 item / 8 ticks |
| `FUEL_HOPPER` | Hopper sideways with FACING towards furnace | 1 item / 8 ticks per hopper |
| `FUEL_SOURCE` | Container above fuel hopper or sideways | Unlimited |
| `OUTPUT_HOPPER` | Hopper below furnace | 1 item / 8 ticks |
| `OUTPUT_DEST` | Container below output hopper | Unlimited |

### Bottleneck formula
```
inputTP = (inputHopperCount > 0) ? elapsed / 8 : INF
fuelTP  = (fuelHopperCount > 0) ? fuelHopperCount * elapsed / 8 : INF
outputTP = (outputHopperCount > 0) ? elapsed / 8 : INF
furnaceTP = elapsed / cookTotal

chainBottleneck = min(inputTP, furnaceTP, outputTP)

maxFuelArrivals = min(fuelItemTotal, fuelTP)
totalBurnTicks = maxFuelArrivals * litDuration + litTime
actualTicks = min(elapsed, totalBurnTicks)

maxByFuel = (actualTicks >= cookTotal - cookingProgressBefore)
  ? 1 + (actualTicks - (cookTotal - cookingProgressBefore)) / cookTotal
  : 0

itemsToCook = min(chainBottleneck, maxByFuel, inputItemTotal, outputSlotSpace)
fuelConsumed = ceil(itemsToCook * cookTotal / litDuration)
```

---

## Iron Furnaces (decompiled v4.1.8, 1.20.1 Forge)

### Inheritance
```
TileEntityInventory extends BlockEntity
  └ implements ITileInventory, WorldlyContainer, MenuProvider, Nameable
  └ NonNullList<ItemStack> inventory
    └ BlockIronFurnaceTileBase extends TileEntityInventory
```

### 3 modes (determined by `currentAugment[2]`, slot 5)
| Value | Mode | Method | How catchup works |
|-------|------|--------|-------------------|
| 0 | 🔥 Furnace | `isFurnace()` | `FurnaceMode.apply()` — adaptive batch: loop burning fuel + smelting items, calling `invokeAutoIO()` when slots fill |
| 1 | 🏭 Factory | `isFactory()` | Build `FurnaceNetwork` (discover neighbor gens + factories), simulate RF routing via `SimulationEngine`, apply results via `SimulationApplicator` |
| 2 | ⚡ Generator | `isGenerator()` | Count fuel + compute RF capacity → simulate via `SimulationEngine.simulateGeneratorOnly()` → apply |

### Furnace mode key fields
| Field | Type | Notes |
|-------|------|-------|
| `furnaceBurnTime` | int | Remaining burn ticks, decremented each tick when lit |
| `cookTime` | int | Current cooking progress (increments each tick) |
| `totalCookTime` | int | Ticks needed per item (affected by Speed augment) |
| `recipeType` | RecipeType | SMELTING, SMOKING, or BLASTING |
| `inventory[4]` | ItemStack | Green augment slot (Speed or Fuel) |
| `inventory[5]` | ItemStack | Blue augment (mode selector, `currentAugment[2]`) |

Furnace tick logic (simplified):
```
if furnaceBurnTime > 0: cookTime++
if cookTime >= totalCookTime → smelt(recipe), autoIO()
if furnaceBurnTime <= 0 AND fuel present:
  furnaceBurnTime = getBurnTime(fuel) * getCookTime() / 200
  fuel.shrink(1)
```

### Factory mode key fields
| Field | Type | Notes |
|-------|------|-------|
| `factoryCookTime[6]` | int[] | Current progress per slot (independent) |
| `factoryTotalCookTime[6]` | int[] | Required ticks per slot |
| `usedRF[6]` | double[] | RF consumed so far for current item |
| `energy` | FEnergyStorage | Internal RF buffer, drained per tick |

Factory tick per slot:
```
if cookTime[i] > 0:
  cookTime[i]++
  usedRF[i] += recipeRF / totalCookTime[i]
  energy -= recipeRF / totalCookTime[i]
  if cookTime[i] >= totalCookTime[i]:
    smelt → autoIO → reset cookTime[i] = 0
```

### Generator mode key fields
| Field | Type | Notes |
|-------|------|-------|
| `generatorBurn` | double | Remaining RF*20 from current fuel piece |
| `generatorRecentRecipeRF` | int | Original `generatorBurn` value |
| `gottenRF` | double | RF already generated from current fuel |
| `getGeneration()` | int | RF per tick (varies by furnace tier: iron=10, gold=20, diamond=40, etc.) |
| `getCapacity()` | int | Max RF buffer |

Generator tick:
```
if energy < capacity:
  if generatorBurn <= 0 AND fuel in slot 6:
    burn 1 fuel → generatorBurn = getGeneratorBurn()
  if generatorBurn > 0:
    energy += getGeneration()
    generatorBurn -= getGeneration() / 20.0
  energyOut()  // push RF to neighbors
```

### Augments
| Augment | Slot | Effect |
|---------|------|--------|
| Green Speed | slot 4 | `getCookTime()` returns halved value |
| Green Fuel | slot 4 | Burn duration doubled (`baseBurn * cookTime / 200` with Fuel augment check) |
| Blue Factory | slot 5 | Sets `currentAugment[2] = 1` |
| Blue Generator | slot 5 | Sets `currentAugment[2] = 2` |
| Red Blasting | slot 1 outer | Changes `recipeType` to BLASTING |
| Red Smoking | slot 1 outer | Changes `recipeType` to SMOKING |

---

## SimulationEngine (pure math, JUnit tested)

### Methods
| Method | Signature |
|--------|-----------|
| `simulateGeneratorOnly` | `(totalFuelItems, burnTicksPerFuel, rfPerTick, capacity, currentRf, elapsedTicks) → SimulationResult` |
| `simulateFactoryOnly` | `(totalItems, outputSpace, maxRfPerItem, rfPerTickConsumption, maxCookTime, currentRf, elapsedTicks) → SimulationResult` |
| `simulateNetwork` | `(NetworkResources, elapsedTicks) → SimulationResult` (requires FurnaceNetwork — no JUnit) |
| `simulate` | `(fuel, burnTicks, rfPerTick, items, outSpace, rfPerItem, rfPerTick, cookTime, genCap, genRf, factCap, factRf, elapsed) → SimulationResult` |

### SimulationResult fields
| Field | Type | Meaning |
|-------|------|---------|
| `fuelToBurn` | int | Items of fuel consumed |
| `itemsToSmelt` | int | Items successfully smelted |
| `rfForFactory` | int | RF used for smelting |
| `rfForGenerators` | int | Excess RF stored in generator buffers |
| `rfForFactoryStorage` | int | Excess RF stored in factory buffers |
| `effectiveTicks` | long | Actual ticks simulated |

### RF routing logic
1. **Max RF from fuel:** `min(fuel × burnTicks, elapsed) × rfPerTick`
2. **Max RF to consume:** `min(items × cookTime, elapsed) × rfPerTickConsumption`
3. **Effective RF:** `min(fuelRF + genBufferRF + factoryBufferRF, neededRF)`
4. **Distribution:** smelting first → gen storage → factory storage

---

## Config

File: `KeepSmeltingConfig.java` (COMMON type only, server-side).

| Key | Type | Default | Range |
|-----|------|---------|-------|
| `catchupEnabled` | boolean | true | — |
| `maxCatchupTicks` | long | 24000 (1 MC day) | 1–192000 |
| `minDeltaThreshold` | int | 20 (1 second) | 1–72000 |
| `debugMode` | enum | OFF | OFF, CHAT, LOG |
| `timeMode` | enum | REALTIME | REALTIME, GAMETIME |

---

## Mixins

| Mixin | Target | Inject |
|-------|--------|--------|
| `FurnaceTickMixin` | `AbstractFurnaceBlockEntity` | `serverTick` @ HEAD |
| `IFurnaceAccessor` | `AbstractFurnaceBlockEntity` | Accessors + Invokers |
| `IronFurnaceTickMixin` | `BlockIronFurnaceTileBase` (`@Pseudo`) | `tick` @ HEAD |
| `IronFurnaceAccessor` | `BlockIronFurnaceTileBase` (`@Pseudo`) | Accessors |
