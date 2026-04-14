package com.fatsan.villagertrade.storage;

import org.bukkit.NamespacedKey;
import org.bukkit.entity.Villager;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

public final class VillagerDataStore {

    private final NamespacedKey employmentLockedKey;
    private final NamespacedKey curedVillagerKey;
    private final NamespacedKey cureCountKey;

    public VillagerDataStore(JavaPlugin plugin) {
        this.employmentLockedKey = new NamespacedKey(plugin, "employment_locked");
        this.curedVillagerKey = new NamespacedKey(plugin, "cured_villager");
        this.cureCountKey = new NamespacedKey(plugin, "cure_count");
    }

    public boolean isEmploymentLocked(Villager villager) {
        return villager.getPersistentDataContainer().has(employmentLockedKey, PersistentDataType.BYTE);
    }

    public void markEmploymentLocked(Villager villager) {
        villager.getPersistentDataContainer().set(employmentLockedKey, PersistentDataType.BYTE, (byte) 1);
    }

    public boolean isCured(Villager villager) {
        return villager.getPersistentDataContainer().has(curedVillagerKey, PersistentDataType.BYTE);
    }

    public void markCured(Villager villager) {
        PersistentDataContainer container = villager.getPersistentDataContainer();
        container.set(curedVillagerKey, PersistentDataType.BYTE, (byte) 1);
        container.set(cureCountKey, PersistentDataType.INTEGER, getCureCount(villager) + 1);
    }

    public int getCureCount(Villager villager) {
        Integer stored = villager.getPersistentDataContainer().get(cureCountKey, PersistentDataType.INTEGER);
        return stored == null ? 0 : stored;
    }
}
