package com.fatsan.villagertrade.listener;

import com.fatsan.villagertrade.VillagerTradeLimiter;
import com.fatsan.villagertrade.config.PluginConfig;
import org.bukkit.entity.AbstractVillager;
import org.bukkit.entity.Villager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.VillagerAcquireTradeEvent;
import org.bukkit.event.entity.VillagerReplenishTradeEvent;
import org.bukkit.inventory.MerchantRecipe;

public final class TradeBalancerListener implements Listener {

    private final VillagerTradeLimiter plugin;

    public TradeBalancerListener(VillagerTradeLimiter plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onVillagerAcquireTrade(VillagerAcquireTradeEvent event) {
        AbstractVillager entity = event.getEntity();
        if (!(entity instanceof Villager villager)) {
            return;
        }

        if (!plugin.isFeatureActiveIn(villager.getWorld())) {
            return;
        }

        MerchantRecipe adjusted = plugin.getTradeBalancerService().adjustAcquiredTrade(villager, event.getRecipe());
        if (adjusted == null) {
            event.setCancelled(true);
            return;
        }

        event.setRecipe(adjusted);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onVillagerReplenishTrade(VillagerReplenishTradeEvent event) {
        AbstractVillager entity = event.getEntity();
        if (!(entity instanceof Villager villager)) {
            return;
        }

        if (!plugin.isFeatureActiveIn(villager.getWorld())) {
            return;
        }

        PluginConfig.RestockPenalty restockPenalty = plugin.getPluginConfig().balance().restockPenalty();
        if (!restockPenalty.enabled() || restockPenalty.restoreFraction() >= 1.0D) {
            return;
        }

        MerchantRecipe recipe = event.getRecipe();
        int beforeUses = recipe.getUses();
        plugin.getTradeBalancerService().applyRestockPenalty(recipe, restockPenalty);
        event.setCancelled(true);

        plugin.debug("Adjusted villager restock for " + villager.getUniqueId()
                + " from uses=" + beforeUses
                + " to uses=" + recipe.getUses());
    }
}
