package com.fatsan.villagertrade.util;

import com.fatsan.villagertrade.config.PluginConfig;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.MerchantRecipe;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

public final class EnchantmentUtil {

    private EnchantmentUtil() {}

    public static Map<Enchantment, Integer> getResultEnchantments(MerchantRecipe recipe) {
        ItemStack result = recipe.getResult();
        ItemMeta meta = result.getItemMeta();
        if (meta == null) {
            return Map.of();
        }

        if (result.getType() == Material.ENCHANTED_BOOK && meta instanceof EnchantmentStorageMeta storageMeta) {
            return Collections.unmodifiableMap(new LinkedHashMap<>(storageMeta.getStoredEnchants()));
        }

        if (meta.getEnchants().isEmpty()) {
            return Map.of();
        }

        return Collections.unmodifiableMap(new LinkedHashMap<>(meta.getEnchants()));
    }

    public static boolean isEnchantedTrade(MerchantRecipe recipe) {
        return !getResultEnchantments(recipe).isEmpty();
    }

    public static String lookupKey(Enchantment enchantment) {
        String key = enchantment.getKey().toString().toLowerCase(Locale.ROOT);
        return PluginConfig.normalizeKey(key);
    }

    public static boolean isTreasureLike(Enchantment enchantment) {
        return enchantment.isCursed() || !enchantment.isDiscoverable();
    }

    public static MerchantRecipe downgradeResultEnchantment(MerchantRecipe original, Enchantment enchantment) {
        Map<Enchantment, Integer> enchantments = getResultEnchantments(original);
        Integer currentLevel = enchantments.get(enchantment);
        if (currentLevel == null || currentLevel <= 1) {
            return null;
        }

        ItemStack newResult = original.getResult().clone();
        ItemMeta meta = newResult.getItemMeta();
        if (meta == null) {
            return null;
        }

        if (newResult.getType() == Material.ENCHANTED_BOOK && meta instanceof EnchantmentStorageMeta storageMeta) {
            storageMeta.removeStoredEnchant(enchantment);
            storageMeta.addStoredEnchant(enchantment, currentLevel - 1, true);
            newResult.setItemMeta(storageMeta);
            return MerchantRecipeUtil.copyWithResult(original, newResult);
        }

        meta.removeEnchant(enchantment);
        meta.addEnchant(enchantment, currentLevel - 1, true);
        newResult.setItemMeta(meta);
        return MerchantRecipeUtil.copyWithResult(original, newResult);
    }
}
