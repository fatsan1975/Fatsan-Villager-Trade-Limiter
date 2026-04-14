package com.fatsan.villagertrade.listener;

import com.fatsan.villagertrade.VillagerTradeLimiter;
import com.fatsan.villagertrade.config.PluginConfig;
import org.bukkit.entity.Villager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.VillagerCareerChangeEvent;

public final class EmploymentLockListener implements Listener {

    private final VillagerTradeLimiter plugin;

    public EmploymentLockListener(VillagerTradeLimiter plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onVillagerCareerChange(VillagerCareerChangeEvent event) {
        if (event.getReason() != VillagerCareerChangeEvent.ChangeReason.EMPLOYED) {
            return;
        }

        Villager villager = event.getEntity();
        PluginConfig.EmploymentLock employmentLock = plugin.getPluginConfig().employmentLock();
        if (!employmentLock.enabled()) {
            return;
        }

        if (!plugin.isFeatureActiveIn(villager.getWorld())) {
            return;
        }

        if (employmentLock.excludedProfessions().contains(event.getProfession())) {
            return;
        }

        if (!employmentLock.applyToCured() && plugin.getDataStore().isCured(villager)) {
            return;
        }

        villager.getScheduler().run(plugin, scheduledTask -> {
            if (!villager.isValid() || villager.isDead()) {
                return;
            }

            if (!plugin.isFeatureActiveIn(villager.getWorld())) {
                return;
            }

            if (employmentLock.excludedProfessions().contains(villager.getProfession())) {
                return;
            }

            if (villager.getVillagerExperience() < employmentLock.lockXp()) {
                villager.setVillagerExperience(employmentLock.lockXp());
            }

            plugin.getDataStore().markEmploymentLocked(villager);
            plugin.debug("Applied employment lock to villager " + villager.getUniqueId()
                    + " with profession " + villager.getProfession()
                    + " at xp " + villager.getVillagerExperience());
        }, null);
    }
}
