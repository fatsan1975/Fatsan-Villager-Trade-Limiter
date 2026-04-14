package com.fatsan.villagertrade.config;

import org.bukkit.World;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Villager;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

public record PluginConfig(
        General general,
        WorldFilter worldFilter,
        EmploymentLock employmentLock,
        RareTrades rareTrades,
        Discounts discounts,
        Balance balance
) {

    public static PluginConfig load(JavaPlugin plugin) {
        FileConfiguration config = plugin.getConfig();
        Logger logger = plugin.getLogger();

        return new PluginConfig(
                new General(
                        config.getBoolean("general.enabled", true),
                        config.getBoolean("general.debug", false)
                ),
                parseWorldFilter(config.getConfigurationSection("worlds")),
                parseEmploymentLock(config.getConfigurationSection("employment-lock"), logger),
                parseRareTrades(config.getConfigurationSection("rare-trades"), logger),
                parseDiscounts(config.getConfigurationSection("discounts"), logger),
                parseBalance(config.getConfigurationSection("balance"), logger)
        );
    }

    public record General(boolean enabled, boolean debug) {}

    public record WorldFilter(Mode mode, Set<String> worlds) {
        public boolean allows(World world) {
            return allows(world.getName());
        }

        public boolean allows(String worldName) {
            String normalized = normalize(worldName);
            return switch (mode) {
                case WHITELIST -> worlds.contains(normalized);
                case BLACKLIST -> !worlds.contains(normalized);
            };
        }
    }

    public enum Mode {
        WHITELIST,
        BLACKLIST
    }

    public record EmploymentLock(
            boolean enabled,
            int lockXp,
            Set<Villager.Profession> excludedProfessions,
            boolean applyToCured
    ) {}

    public record RareTrades(
            boolean enabled,
            FallbackAction fallbackAction,
            double fallbackPriceMultiplier,
            Set<Villager.Profession> targetProfessions,
            Map<String, EnchantmentRule> enchantmentRules
    ) {
        public boolean appliesTo(Villager villager) {
            return targetProfessions.isEmpty() || targetProfessions.contains(villager.getProfession());
        }
    }

    public enum FallbackAction {
        REMOVE,
        INCREASE_PRICE,
        DOWNGRADE_LEVEL
    }

    public record EnchantmentRule(
            double blockChance,
            Map<Integer, Double> levelChances
    ) {
        public double chanceForLevel(int level) {
            return levelChances.getOrDefault(level, blockChance);
        }
    }

    public record Discounts(
            boolean enabled,
            double discountMultiplier,
            double heroOfTheVillageMultiplier,
            double cureMultiplier,
            int minPrice,
            Map<Villager.Profession, Integer> professionMinPrices
    ) {
        public int minPriceFor(Villager.Profession profession) {
            return professionMinPrices.getOrDefault(profession, minPrice);
        }
    }

    public record Balance(
            boolean enabled,
            Map<Villager.Profession, Double> professionPriceMultipliers,
            int enchantedBookMinPrice,
            double demandScalingMultiplier,
            MaxUses maxUses,
            RestockPenalty restockPenalty,
            MasterTradeScarcity masterTradeScarcity
    ) {}

    public record MaxUses(
            boolean enabled,
            int enchantedBooks,
            int treasureEnchants
    ) {}

    public record RestockPenalty(
            boolean enabled,
            double restoreFraction
    ) {}

    public record MasterTradeScarcity(
            boolean enabled,
            double removalChance
    ) {}

    private static WorldFilter parseWorldFilter(ConfigurationSection section) {
        if (section == null) {
            return new WorldFilter(Mode.BLACKLIST, Set.of());
        }

        Mode mode = parseEnum(
                section.getString("mode", "BLACKLIST"),
                Mode.class,
                Mode.BLACKLIST,
                null
        );

        Set<String> worlds = new HashSet<>();
        for (String world : section.getStringList("list")) {
            worlds.add(normalize(world));
        }

        return new WorldFilter(mode, Set.copyOf(worlds));
    }

    private static EmploymentLock parseEmploymentLock(ConfigurationSection section, Logger logger) {
        if (section == null) {
            return new EmploymentLock(false, 1, Set.of(), true);
        }

        return new EmploymentLock(
                section.getBoolean("enabled", true),
                clamp(section.getInt("lock-xp", 1), 1, 100),
                parseProfessions(section.getStringList("excluded-professions"), logger),
                section.getBoolean("apply-to-cured", true)
        );
    }

    private static RareTrades parseRareTrades(ConfigurationSection section, Logger logger) {
        if (section == null) {
            return new RareTrades(false, FallbackAction.INCREASE_PRICE, 3.0D, Set.of(), Map.of());
        }

        Map<String, EnchantmentRule> rules = new HashMap<>();
        ConfigurationSection ruleSection = section.getConfigurationSection("enchantment-nerfs");
        if (ruleSection != null) {
            for (String key : ruleSection.getKeys(false)) {
                ConfigurationSection enchantmentSection = ruleSection.getConfigurationSection(key);
                if (enchantmentSection == null) {
                    continue;
                }

                Map<Integer, Double> levelChances = new HashMap<>();
                ConfigurationSection levelSection = enchantmentSection.getConfigurationSection("level-penalties");
                if (levelSection != null) {
                    for (String levelKey : levelSection.getKeys(false)) {
                        try {
                            int level = Integer.parseInt(levelKey);
                            levelChances.put(level, clamp(enchantmentSection.getDouble("level-penalties." + levelKey), 0.0D, 1.0D));
                        } catch (NumberFormatException exception) {
                            logger.warning("Ignoring invalid enchantment level '" + levelKey + "' in rare-trades.enchantment-nerfs." + key);
                        }
                    }
                }

                rules.put(
                        normalizeKey(key),
                        new EnchantmentRule(
                                clamp(enchantmentSection.getDouble("block-chance", 0.0D), 0.0D, 1.0D),
                                Map.copyOf(levelChances)
                        )
                );
            }
        }

        return new RareTrades(
                section.getBoolean("enabled", true),
                parseEnum(section.getString("fallback-action", "INCREASE_PRICE"), FallbackAction.class, FallbackAction.INCREASE_PRICE, logger),
                clamp(section.getDouble("fallback-price-multiplier", 3.0D), 1.0D, 16.0D),
                parseProfessions(section.getStringList("target-professions"), logger),
                Map.copyOf(rules)
        );
    }

    private static Discounts parseDiscounts(ConfigurationSection section, Logger logger) {
        if (section == null) {
            return new Discounts(false, 1.0D, 1.0D, 1.0D, 1, Map.of());
        }

        Map<Villager.Profession, Integer> professionMinimums = new HashMap<>();
        ConfigurationSection minimumSection = section.getConfigurationSection("profession-min-prices");
        if (minimumSection != null) {
            for (String key : minimumSection.getKeys(false)) {
                Villager.Profession profession = parseProfession(key, logger);
                if (profession != null) {
                    professionMinimums.put(profession, clamp(minimumSection.getInt(key, 1), 1, 64));
                }
            }
        }

        return new Discounts(
                section.getBoolean("enabled", true),
                clamp(section.getDouble("discount-multiplier", 0.5D), 0.0D, 1.0D),
                clamp(section.getDouble("hero-of-the-village-multiplier", 0.4D), 0.0D, 1.0D),
                clamp(section.getDouble("cure-multiplier", 0.25D), 0.0D, 1.0D),
                clamp(section.getInt("min-price", 1), 1, 64),
                Map.copyOf(professionMinimums)
        );
    }

    private static Balance parseBalance(ConfigurationSection section, Logger logger) {
        if (section == null) {
            return new Balance(
                    false,
                    Map.of(),
                    5,
                    1.0D,
                    new MaxUses(false, 8, 4),
                    new RestockPenalty(false, 1.0D),
                    new MasterTradeScarcity(false, 0.0D)
            );
        }

        Map<Villager.Profession, Double> professionPriceMultipliers = new HashMap<>();
        ConfigurationSection multiplierSection = section.getConfigurationSection("profession-price-multipliers");
        if (multiplierSection != null) {
            for (String key : multiplierSection.getKeys(false)) {
                Villager.Profession profession = parseProfession(key, logger);
                if (profession != null) {
                    professionPriceMultipliers.put(profession, clamp(multiplierSection.getDouble(key, 1.0D), 0.1D, 16.0D));
                }
            }
        }

        ConfigurationSection maxUsesSection = section.getConfigurationSection("max-uses");
        MaxUses maxUses = new MaxUses(
                maxUsesSection != null && maxUsesSection.getBoolean("enabled", false),
                maxUsesSection != null ? clamp(maxUsesSection.getInt("enchanted-books", 8), 1, 64) : 8,
                maxUsesSection != null ? clamp(maxUsesSection.getInt("treasure-enchants", 4), 1, 64) : 4
        );

        ConfigurationSection restockSection = section.getConfigurationSection("restock-penalty");
        RestockPenalty restockPenalty = new RestockPenalty(
                restockSection != null && restockSection.getBoolean("enabled", false),
                restockSection != null ? clamp(restockSection.getDouble("restore-fraction", 0.75D), 0.05D, 1.0D) : 1.0D
        );

        ConfigurationSection scarcitySection = section.getConfigurationSection("master-trade-scarcity");
        MasterTradeScarcity masterTradeScarcity = new MasterTradeScarcity(
                scarcitySection != null && scarcitySection.getBoolean("enabled", false),
                scarcitySection != null ? clamp(scarcitySection.getDouble("removal-chance", 0.30D), 0.0D, 1.0D) : 0.0D
        );

        return new Balance(
                section.getBoolean("enabled", true),
                Map.copyOf(professionPriceMultipliers),
                clamp(section.getInt("enchanted-book-min-price", 5), 1, 64),
                clamp(section.getDouble("demand-scaling-multiplier", 1.5D), 0.1D, 8.0D),
                maxUses,
                restockPenalty,
                masterTradeScarcity
        );
    }

    private static Set<Villager.Profession> parseProfessions(Iterable<String> rawValues, Logger logger) {
        Set<Villager.Profession> professions = new HashSet<>();
        for (String rawValue : rawValues) {
            Villager.Profession profession = parseProfession(rawValue, logger);
            if (profession != null) {
                professions.add(profession);
            }
        }
        return Set.copyOf(professions);
    }

    private static Villager.Profession parseProfession(String rawValue, Logger logger) {
        if (rawValue == null || rawValue.isBlank()) {
            return null;
        }

        String normalized = rawValue.trim().toLowerCase(Locale.ROOT);
        NamespacedKey key = NamespacedKey.fromString(normalized.contains(":") ? normalized : "minecraft:" + normalized);
        if (key == null) {
            if (logger != null) {
                logger.warning("Ignoring invalid villager profession key '" + rawValue + "'.");
            }
            return null;
        }

        Villager.Profession profession = Registry.VILLAGER_PROFESSION.get(key);
        if (profession == null && logger != null) {
            logger.warning("Ignoring unknown villager profession '" + rawValue + "'.");
        }
        return profession;
    }

    private static <T extends Enum<T>> T parseEnum(String rawValue, Class<T> type, T fallback, Logger logger) {
        if (rawValue == null || rawValue.isBlank()) {
            return fallback;
        }

        try {
            return Enum.valueOf(type, rawValue.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            if (logger != null) {
                logger.warning("Ignoring invalid value '" + rawValue + "' for " + type.getSimpleName() + ". Falling back to " + fallback + ".");
            }
            return fallback;
        }
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    private static String normalize(String rawValue) {
        return rawValue == null ? "" : rawValue.trim().toLowerCase(Locale.ROOT);
    }

    public static String normalizeKey(String rawKey) {
        String normalized = normalize(rawKey);
        if (normalized.startsWith("minecraft:")) {
            return normalized.substring("minecraft:".length());
        }
        return normalized;
    }
}
