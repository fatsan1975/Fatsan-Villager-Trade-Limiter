package com.fatsan.villagertrade.listener;

import com.fatsan.villagertrade.VillagerTradeLimiter;
import org.bukkit.entity.Villager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityTransformEvent;

public final class VillagerStateListener implements Listener {

    private final VillagerTradeLimiter plugin;

    public VillagerStateListener(VillagerTradeLimiter plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onVillagerCured(EntityTransformEvent event) {
        if (event.getTransformReason() != EntityTransformEvent.TransformReason.CURED) {
            return;
        }

        if (!(event.getTransformedEntity() instanceof Villager villager)) {
            return;
        }

        plugin.getDataStore().markCured(villager);
        plugin.debug("Marked villager " + villager.getUniqueId() + " as cured.");
    }
}
