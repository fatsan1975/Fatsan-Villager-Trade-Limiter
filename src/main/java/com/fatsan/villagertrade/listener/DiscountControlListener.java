package com.fatsan.villagertrade.listener;

import com.fatsan.villagertrade.VillagerTradeLimiter;
import io.papermc.paper.event.player.PlayerTradeEvent;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Villager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.inventory.TradeSelectEvent;
import org.bukkit.inventory.view.MerchantView;

public final class DiscountControlListener implements Listener {

    private final VillagerTradeLimiter plugin;

    public DiscountControlListener(VillagerTradeLimiter plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onInventoryOpen(InventoryOpenEvent event) {
        if (!(event.getView() instanceof MerchantView merchantView)) {
            return;
        }

        HumanEntity humanEntity = merchantView.getPlayer();
        if (!(humanEntity instanceof Player player)) {
            return;
        }

        if (!(merchantView.getMerchant() instanceof Villager villager)) {
            return;
        }

        if (!plugin.isFeatureActiveIn(villager.getWorld())) {
            return;
        }

        if (!plugin.getPluginConfig().discounts().enabled()) {
            return;
        }

        plugin.getDiscountService().applyDiscounts(player, villager);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onTradeSelect(TradeSelectEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }

        if (!(event.getMerchant() instanceof Villager villager)) {
            return;
        }

        if (!plugin.isFeatureActiveIn(villager.getWorld())) {
            return;
        }

        if (!plugin.getPluginConfig().discounts().enabled()) {
            return;
        }

        if (event.getIndex() < 0 || event.getIndex() >= villager.getRecipeCount()) {
            return;
        }

        plugin.getDiscountService().applyDiscountsToRecipe(player, villager, villager.getRecipe(event.getIndex()));
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPlayerTrade(PlayerTradeEvent event) {
        if (!(event.getMerchant() instanceof Villager villager)) {
            return;
        }

        if (!plugin.isFeatureActiveIn(villager.getWorld())) {
            return;
        }

        if (!plugin.getPluginConfig().discounts().enabled()) {
            return;
        }

        event.setTrade(plugin.getDiscountService().adjustedTrade(event.getPlayer(), villager, event.getTrade()));
    }
}
