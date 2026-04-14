# Implementation Notes

## Chosen Event Hooks

- `VillagerCareerChangeEvent` with `EMPLOYED` for employment lock.
- `EntityTransformEvent` with `CURED` for persistent cured-villager tracking.
- `VillagerAcquireTradeEvent` for initial recipe mutation and rare-trade nerfs.
- `VillagerReplenishTradeEvent` for partial restock behavior.
- `InventoryOpenEvent`, `TradeSelectEvent`, and `PlayerTradeEvent` for discount control without client desync.

## Employment-Lock Strategy

The plugin gives a newly employed villager a tiny amount of villager XP on its entity scheduler the tick after employment. This is based on current 1.21.11 server behavior: vanilla only allows job loss while villager XP is `0` and villager level is `1` or lower. Because the XP field is saved by vanilla, the lock persists across restarts.

## Discount Strategy

The discount controller does not guess from the current displayed price alone. It rebuilds the positive discount contribution from:

- Paper villager reputation data for trade reputation and cure-style positive gossip
- the current Hero of the Village effect using the target-version vanilla formula

It then writes the adjusted `specialPrice` during merchant open and re-applies it on trade select / trade execution as a safety net.

## Why The Design Is Folia-Safe

- No repeating polling tasks.
- No async access to entities, worlds, inventories, or villager offers.
- Employment lock uses `Entity#getScheduler()` so delayed mutation runs on the villager's owning region thread.
- Trade and discount logic only runs inside synchronous Paper/Folia event callbacks that already own the relevant merchant/player context.
