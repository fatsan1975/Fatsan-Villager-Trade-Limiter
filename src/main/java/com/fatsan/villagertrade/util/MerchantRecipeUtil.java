package com.fatsan.villagertrade.util;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.MerchantRecipe;

import java.util.ArrayList;
import java.util.List;

public final class MerchantRecipeUtil {

    private MerchantRecipeUtil() {}

    public static MerchantRecipe copy(MerchantRecipe original) {
        return new MerchantRecipe(original);
    }

    public static MerchantRecipe copyWithResult(MerchantRecipe original, ItemStack newResult) {
        MerchantRecipe copy = new MerchantRecipe(
                newResult,
                original.getUses(),
                original.getMaxUses(),
                original.hasExperienceReward(),
                original.getVillagerExperience(),
                original.getPriceMultiplier(),
                original.getDemand(),
                original.getSpecialPrice(),
                original.shouldIgnoreDiscounts()
        );
        copy.setIngredients(original.getIngredients());
        return copy;
    }

    public static MerchantRecipe multiplyFirstIngredient(MerchantRecipe original, double multiplier) {
        List<ItemStack> ingredients = new ArrayList<>(original.getIngredients());
        if (ingredients.isEmpty()) {
            return copy(original);
        }

        ItemStack first = ingredients.get(0).clone();
        int newAmount = (int) Math.ceil(first.getAmount() * multiplier);
        first.setAmount(clamp(newAmount, 1, first.getMaxStackSize()));
        ingredients.set(0, first);

        return copyWithIngredients(original, ingredients);
    }

    public static MerchantRecipe enforceMinimumFirstIngredient(MerchantRecipe original, int minimumAmount) {
        List<ItemStack> ingredients = new ArrayList<>(original.getIngredients());
        if (ingredients.isEmpty()) {
            return copy(original);
        }

        ItemStack first = ingredients.get(0).clone();
        if (first.getAmount() >= minimumAmount) {
            return copy(original);
        }

        first.setAmount(clamp(minimumAmount, 1, first.getMaxStackSize()));
        ingredients.set(0, first);
        return copyWithIngredients(original, ingredients);
    }

    public static MerchantRecipe copyWithIngredients(MerchantRecipe original, List<ItemStack> ingredients) {
        MerchantRecipe copy = new MerchantRecipe(
                original.getResult().clone(),
                original.getUses(),
                original.getMaxUses(),
                original.hasExperienceReward(),
                original.getVillagerExperience(),
                original.getPriceMultiplier(),
                original.getDemand(),
                original.getSpecialPrice(),
                original.shouldIgnoreDiscounts()
        );
        copy.setIngredients(ingredients);
        return copy;
    }

    public static boolean hasFirstEmeraldIngredient(MerchantRecipe recipe) {
        List<ItemStack> ingredients = recipe.getIngredients();
        return !ingredients.isEmpty() && ingredients.get(0).getType() == Material.EMERALD;
    }

    public static int getDemandAdjustment(MerchantRecipe recipe) {
        return Math.max(0, (int) Math.floor(recipe.getDemand() * recipe.getPriceMultiplier()));
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }
}
