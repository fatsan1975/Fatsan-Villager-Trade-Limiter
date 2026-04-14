package com.fatsan.villagertrade;

import com.fatsan.villagertrade.command.VtlCommand;
import com.fatsan.villagertrade.config.PluginConfig;
import com.fatsan.villagertrade.listener.DiscountControlListener;
import com.fatsan.villagertrade.listener.EmploymentLockListener;
import com.fatsan.villagertrade.listener.TradeBalancerListener;
import com.fatsan.villagertrade.listener.VillagerStateListener;
import com.fatsan.villagertrade.service.DiscountService;
import com.fatsan.villagertrade.service.TradeBalancerService;
import com.fatsan.villagertrade.storage.VillagerDataStore;
import org.bukkit.World;
import org.bukkit.command.PluginCommand;
import org.bukkit.event.HandlerList;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

public final class VillagerTradeLimiter extends JavaPlugin {

    private volatile PluginConfig pluginConfig;
    private volatile boolean debugEnabled;
    private VillagerDataStore dataStore;
    private TradeBalancerService tradeBalancerService;
    private DiscountService discountService;
    private boolean foliaDetected;

    @Override
    public void onEnable() {
        this.foliaDetected = detectFolia();

        saveDefaultConfig();

        this.dataStore = new VillagerDataStore(this);
        this.tradeBalancerService = new TradeBalancerService(this);
        this.discountService = new DiscountService(this);
        reloadPluginConfiguration();

        registerCommand();
        registerListeners();

        getLogger().info("Enabled v" + getDescription().getVersion()
                + " for " + (foliaDetected ? "Folia/Paper" : "Paper")
                + " targeting 1.21.11 with best-effort 1.21.x support.");
    }

    @Override
    public void onDisable() {
        HandlerList.unregisterAll(this);
    }

    public void reloadPlugin() {
        reloadConfig();
        reloadPluginConfiguration();
        HandlerList.unregisterAll(this);
        registerListeners();
        getLogger().info("VillagerTradeLimiter configuration reloaded.");
    }

    private void reloadPluginConfiguration() {
        this.pluginConfig = PluginConfig.load(this);
        this.debugEnabled = this.pluginConfig.general().debug();
    }

    private void registerCommand() {
        PluginCommand command = getCommand("vtl");
        if (command == null) {
            getLogger().warning("Command /vtl is missing from plugin.yml.");
            return;
        }

        VtlCommand handler = new VtlCommand(this);
        command.setExecutor(handler);
        command.setTabCompleter(handler);
    }

    private void registerListeners() {
        if (!pluginConfig.general().enabled()) {
            getLogger().info("VillagerTradeLimiter is disabled by config.");
            return;
        }

        PluginManager pluginManager = getServer().getPluginManager();
        pluginManager.registerEvents(new VillagerStateListener(this), this);
        pluginManager.registerEvents(new EmploymentLockListener(this), this);
        pluginManager.registerEvents(new TradeBalancerListener(this), this);
        pluginManager.registerEvents(new DiscountControlListener(this), this);
    }

    private boolean detectFolia() {
        try {
            Class.forName("io.papermc.paper.threadedregions.RegionizedServer");
            return true;
        } catch (ClassNotFoundException ignored) {
            return false;
        }
    }

    public boolean isFeatureActiveIn(World world) {
        return pluginConfig.general().enabled() && pluginConfig.worldFilter().allows(world);
    }

    public void debug(String message) {
        if (debugEnabled) {
            getLogger().info("[debug] " + message);
        }
    }

    public PluginConfig getPluginConfig() {
        return pluginConfig;
    }

    public VillagerDataStore getDataStore() {
        return dataStore;
    }

    public TradeBalancerService getTradeBalancerService() {
        return tradeBalancerService;
    }

    public DiscountService getDiscountService() {
        return discountService;
    }

    public boolean isFoliaDetected() {
        return foliaDetected;
    }

    public void setDebugEnabled(boolean debugEnabled) {
        this.debugEnabled = debugEnabled;
    }

    public boolean isDebugEnabled() {
        return debugEnabled;
    }
}
