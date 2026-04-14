# Testing Checklist

## Automated Validation Already Worth Running

- Build the plugin jar with Gradle.
- Start a Paper 1.21.11 server with the plugin installed.
- Start a Folia 1.21.11 server with the plugin installed.
- Confirm plugin enable log, command registration, and zero startup exceptions.

## Manual Gameplay Checklist

1. Fresh unemployed villager gets employed and receives the XP lock.
2. Break and replace the workstation after employment lock and confirm profession does not reset.
3. Generate large librarian samples and compare book frequency with the plugin off vs on.
4. Verify configured rare enchantments appear less often, especially high levels.
5. Open the same villager with and without Hero of the Village and compare price changes.
6. Cure a zombie villager, then compare discounts before and after the cure multiplier.
7. Exhaust trades, wait for restock, and confirm partial-restock behavior when enabled.
8. Restart the server and confirm employment lock and cured markers still behave correctly.
9. Repeat the above on Paper.
10. Repeat the above on Folia.
11. Trade with villagers in separate chunks / regions on Folia and watch for thread-violation logs.
12. Reload config during normal gameplay with `/vtl reload` and confirm new values take effect.

## Extra Things To Watch For

- No console errors.
- No async or Folia thread-ownership violations.
- No merchant UI desync.
- No impossible or zero-cost trades.
- No duplicated or disappearing ingredients/results.
- No villager softlocks where trading stops unexpectedly.
