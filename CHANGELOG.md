# Changelog

## v0.2.0

### New Features
- **Hopper pipeline simulation** — offline catchup now simulates the full barrel→hopper→furnace→hopper→barrel chain, respecting throughput bottlenecks and hopper directions
- **Unified commands** — `simulate` and `test` now work with any furnace type, not just Iron Furnaces
- **Vanilla furnace support** — catchup and testing for regular furnaces, smokers, and blast furnaces

### Improvements
- **More accurate catchup** — resource counting across multiple hoppers and containers, output push through the full chain
- **Better test scenarios** — vanilla furnace tests place hoppers in realistic configurations

## v0.1.1

### Fixes
- No longer crashes without Iron Furnaces
- JAR filename now includes version
