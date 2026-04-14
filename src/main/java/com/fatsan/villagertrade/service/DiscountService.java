package com.fatsan.villagertrade.service;

import com.destroystokyo.paper.entity.villager.Reputation;
import com.fatsan.villagertrade.VillagerTradeLimiter;
import com.fatsan.villagertrade.config.PluginConfig;
import com.fatsan.villagertrade.util.DiscountMath;
import com.fatsan.villagertrade.util.MerchantRecipeUtil;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.entity.Villager;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.MerchantRecipe;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.List;

public final class DiscountService {

    private final VillagerTradeLimiter plugin;

    public DiscountService(VillagerTradeLimiter plugin) {
        this.plugin = plugin;
    }

    public void applyDiscounts(Player player, Villager villager) {
        PluginConfig.Discounts discounts = plugin.getPluginConfig().discounts();
        if (!discounts.enabled()) {
            return;
        }

        for (int index = 0; index < villager.getRecipeCount(); index++) {
            MerchantRecipe recipe = villager.getRecipe(index);
            if (recipe == null) {
                continue;
            }

            applyDiscountsToRecipe(player, villager, recipe, discounts);
        }
    }

    public MerchantRecipe adjustedTrade(Player player, Villager villager, MerchantRecipe original) {
        PluginConfig.Discounts discounts = plugin.getPluginConfig().discounts();
        MerchantRecipe adjusted = MerchantRecipeUtil.copy(original);
        applyDiscountsToRecipe(player, villager, adjusted, discounts);
        return adjusted;
    }

    public void applyDiscountsToRecipe(Player player, Villager villager, MerchantRecipe recipe) {
        // Villager#getRecipe returns a live CraftMerchantRecipe wrapper, so mutate the
        // offer in place instead of replacing it and risking stock-state churn.
        applyDiscountsToRecipe(player, villager, recipe, plugin.getPluginConfig().discounts());
    }

    private void applyDiscountsToRecipe(Player player, Villager villager, MerchantRecipe recipe, PluginConfig.Discounts discounts) {
        int specialPrice = calculateSpecialPrice(player, villager, recipe, discounts);
        recipe.setSpecialPrice(specialPrice);
        enforceMinimumPrice(villager, recipe, discounts);
    }

    private int calculateSpecialPrice(Player player, Villager villager, MerchantRecipe recipe, PluginConfig.Discounts discounts) {
        Reputation reputation = villager.getReputation(player.getUniqueId());

        double scaledWeightedReputation =
                DiscountMath.weightedNegative(reputation)
                        + (DiscountMath.weightedTrading(reputation) * discounts.discountMultiplier())
                        + (DiscountMath.weightedCure(reputation) * discounts.discountMultiplier() * discounts.cureMultiplier());

        int reputationContribution = -(int) Math.floor(scaledWeightedReputation * recipe.getPriceMultiplier());
        int heroContribution = calculateHeroContribution(player, recipe, discounts);

        return reputationContribution + heroContribution;
    }

    private int calculateHeroContribution(Player player, MerchantRecipe recipe, PluginConfig.Discounts discounts) {
        PotionEffect effect = player.getPotionEffect(PotionEffectType.HERO_OF_THE_VILLAGE);
        if (effect == null) {
            return 0;
        }

        List<ItemStack> ingredients = recipe.getIngredients();
        if (ingredients.isEmpty()) {
            return 0;
        }

        int baseCount = ingredients.get(0).getAmount();
        double vanillaFactor = 0.3D + (0.0625D * effect.getAmplifier());
        int vanillaMagnitude = Math.max((int) Math.floor(vanillaFactor * baseCount), 1);
        int adjustedMagnitude = (int) Math.floor(vanillaMagnitude
                * discounts.discountMultiplier()
                * discounts.heroOfTheVillageMultiplier());

        return -Math.max(adjustedMagnitude, 0);
    }

    private void enforceMinimumPrice(Villager villager, MerchantRecipe recipe, PluginConfig.Discounts discounts) {
        List<ItemStack> ingredients = recipe.getIngredients();
        if (ingredients.isEmpty()) {
            return;
        }

        ItemStack firstIngredient = ingredients.get(0);
        if (firstIngredient.getType() != Material.EMERALD) {
            return;
        }

        int minimumPrice = discounts.minPriceFor(villager.getProfession());
        int baseAmount = firstIngredient.getAmount();
        int requiredSpecialPrice = minimumPrice - baseAmount - MerchantRecipeUtil.getDemandAdjustment(recipe);
        if (recipe.getSpecialPrice() < requiredSpecialPrice) {
            recipe.setSpecialPrice(requiredSpecialPrice);
        }
    }
}
