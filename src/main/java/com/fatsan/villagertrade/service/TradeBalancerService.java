package com.fatsan.villagertrade.service;

import com.fatsan.villagertrade.VillagerTradeLimiter;
import com.fatsan.villagertrade.config.PluginConfig;
import com.fatsan.villagertrade.util.EnchantmentUtil;
import com.fatsan.villagertrade.util.MerchantRecipeUtil;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Villager;
import org.bukkit.inventory.MerchantRecipe;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

public final class TradeBalancerService {

    private final VillagerTradeLimiter plugin;

    public TradeBalancerService(VillagerTradeLimiter plugin) {
        this.plugin = plugin;
    }

    public MerchantRecipe adjustAcquiredTrade(Villager villager, MerchantRecipe original) {
        PluginConfig config = plugin.getPluginConfig();
        MerchantRecipe recipe = MerchantRecipeUtil.copy(original);

        if (config.balance().enabled() && shouldRemoveMasterTrade(villager, config.balance())) {
            plugin.debug("Removed master trade from villager " + villager.getUniqueId());
            return null;
        }

        if (config.rareTrades().enabled() && config.rareTrades().appliesTo(villager)) {
            NerfCandidate candidate = pickTriggeredNerf(recipe, config.rareTrades());
            if (candidate != null) {
                recipe = applyNerf(recipe, candidate, config.rareTrades());
                if (recipe == null) {
                    plugin.debug("Removed trade for " + candidate.enchantment().getKey() + " level " + candidate.level());
                    return null;
                }
            }
        }

        if (config.balance().enabled()) {
            recipe = applyBalanceAdjustments(villager, recipe, config.balance());
        }

        return recipe;
    }

    public void applyRestockPenalty(MerchantRecipe recipe, PluginConfig.RestockPenalty restockPenalty) {
        if (!restockPenalty.enabled()) {
            return;
        }

        int maxUses = recipe.getMaxUses();
        int currentUses = recipe.getUses();
        int targetAvailableUses = Math.max(0, (int) Math.ceil(maxUses * restockPenalty.restoreFraction()));
        int targetUses = Math.max(0, maxUses - targetAvailableUses);

        if (targetUses < currentUses) {
            recipe.setUses(targetUses);
        }
    }

    private boolean shouldRemoveMasterTrade(Villager villager, PluginConfig.Balance balance) {
        return balance.masterTradeScarcity().enabled()
                && villager.getVillagerLevel() >= 5
                && ThreadLocalRandom.current().nextDouble() < balance.masterTradeScarcity().removalChance();
    }

    private MerchantRecipe applyBalanceAdjustments(Villager villager, MerchantRecipe recipe, PluginConfig.Balance balance) {
        Double professionMultiplier = balance.professionPriceMultipliers().get(villager.getProfession());
        if (professionMultiplier != null && professionMultiplier > 0.0D && professionMultiplier != 1.0D) {
            recipe = MerchantRecipeUtil.multiplyFirstIngredient(recipe, professionMultiplier);
        }

        if (EnchantmentUtil.isEnchantedTrade(recipe)) {
            recipe = MerchantRecipeUtil.enforceMinimumFirstIngredient(recipe, balance.enchantedBookMinPrice());
        }

        if (balance.demandScalingMultiplier() != 1.0D) {
            recipe.setPriceMultiplier((float) (recipe.getPriceMultiplier() * balance.demandScalingMultiplier()));
        }

        if (balance.maxUses().enabled()) {
            applyMaxUses(recipe, balance.maxUses());
        }

        return recipe;
    }

    private void applyMaxUses(MerchantRecipe recipe, PluginConfig.MaxUses maxUses) {
        if (!EnchantmentUtil.isEnchantedTrade(recipe)) {
            return;
        }

        boolean treasureLike = EnchantmentUtil.getResultEnchantments(recipe).keySet().stream()
                .anyMatch(EnchantmentUtil::isTreasureLike);

        int target = treasureLike ? maxUses.treasureEnchants() : maxUses.enchantedBooks();
        recipe.setMaxUses(Math.min(recipe.getMaxUses(), target));
    }

    private NerfCandidate pickTriggeredNerf(MerchantRecipe recipe, PluginConfig.RareTrades rareTrades) {
        Map<Enchantment, Integer> enchantments = EnchantmentUtil.getResultEnchantments(recipe);
        if (enchantments.isEmpty()) {
            return null;
        }

        List<NerfCandidate> triggered = new ArrayList<>();
        ThreadLocalRandom random = ThreadLocalRandom.current();

        for (Map.Entry<Enchantment, Integer> entry : enchantments.entrySet()) {
            String key = EnchantmentUtil.lookupKey(entry.getKey());
            PluginConfig.EnchantmentRule rule = rareTrades.enchantmentRules().get(key);
            if (rule == null) {
                continue;
            }

            double chance = rule.chanceForLevel(entry.getValue());
            if (chance <= 0.0D) {
                continue;
            }

            if (random.nextDouble() < chance) {
                triggered.add(new NerfCandidate(entry.getKey(), entry.getValue(), chance));
            }
        }

        return triggered.stream()
                .max(Comparator.comparingDouble(NerfCandidate::chance).thenComparingInt(NerfCandidate::level))
                .orElse(null);
    }

    private MerchantRecipe applyNerf(MerchantRecipe recipe, NerfCandidate candidate, PluginConfig.RareTrades rareTrades) {
        return switch (rareTrades.fallbackAction()) {
            case REMOVE -> null;
            case INCREASE_PRICE -> MerchantRecipeUtil.multiplyFirstIngredient(recipe, rareTrades.fallbackPriceMultiplier());
            case DOWNGRADE_LEVEL -> {
                MerchantRecipe downgraded = EnchantmentUtil.downgradeResultEnchantment(recipe, candidate.enchantment());
                yield downgraded != null ? downgraded : MerchantRecipeUtil.multiplyFirstIngredient(recipe, rareTrades.fallbackPriceMultiplier());
            }
        };
    }

    private record NerfCandidate(Enchantment enchantment, int level, double chance) {}
}
