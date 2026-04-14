# VillagerTradeLimiter

VillagerTradeLimiter is a Folia-safe villager trade balancing plugin for survival servers running Folia 1.21.11 or Paper 1.21.11. It targets the main villager abuse cases without touching villager breeding.

## Features

- Early employment lock using real vanilla job-loss semantics, not a cosmetic fake.
- Per-enchantment trade nerfs with level-based chances.
- Safe trade mutation through `VillagerAcquireTradeEvent`.
- Discount control for Hero of the Village and cure-driven reputation.
- Optional profession price multipliers, enchanted-trade minimums, master-trade scarcity, max-use overrides, and restock penalties.
- Persistent cured-villager and employment-lock markers via PDC.
- `folia-supported: true` and entity-scheduler usage for cross-platform behavior.

## Commands

- `/vtl info` shows status, debug state, and Folia detection.
- `/vtl reload` reloads `config.yml`.
- `/vtl debug [on|off]` toggles debug logging.

## Permissions

- `vtl.admin` grants everything.
- `vtl.info` allows `/vtl info`.
- `vtl.reload` allows `/vtl reload`.
- `vtl.debug` allows `/vtl debug`.

## Config Overview

- `employment-lock` controls the early profession lock.
- `rare-trades` controls per-enchantment nerfs and fallback behavior.
- `discounts` controls positive discount scaling and minimum emerald floors.
- `balance` controls optional extra balancing like price multipliers, demand scaling, max uses, restock penalty, and master-trade scarcity.
- `worlds` allows blacklist or whitelist scoping.

## Version Support

- Primary target: Folia 1.21.11 and Paper 1.21.11.
- Best-effort support: 1.21.x where the same Paper API and merchant behavior remain compatible.
- The plugin intentionally avoids direct NMS hooks in its runtime code to keep that compatibility realistic.

## Paper / Folia Notes

- The plugin only touches villager state from event threads that already own that entity, or from the villager entity scheduler.
- Merchant discount changes are applied during `InventoryOpenEvent`, before offers are sent to the client.
- No async world or entity access is used.

## Known Limitations

- Cure-aware discount scaling depends on the villager reputation data Paper exposes. It is accurate for the current target, but villagers cured before this plugin was installed are not retroactively tagged in PDC.
- If another plugin rewrites villager offers after this plugin in the same event phase, whichever plugin runs last will win for that offer.
- Extensive gameplay validation still requires live in-game interaction; see `TESTING.md`.
