package com.fatsan.villagertrade.command;

import com.fatsan.villagertrade.VillagerTradeLimiter;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class VtlCommand implements CommandExecutor, TabCompleter {

    private final VillagerTradeLimiter plugin;

    public VtlCommand(VillagerTradeLimiter plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0 || args[0].equalsIgnoreCase("info")) {
            if (!sender.hasPermission("vtl.info")) {
                sender.sendMessage("You do not have permission to view plugin info.");
                return true;
            }

            sender.sendMessage("VillagerTradeLimiter v" + plugin.getDescription().getVersion()
                    + " | config-enabled=" + plugin.getPluginConfig().general().enabled()
                    + " | debug=" + plugin.isDebugEnabled()
                    + " | folia-detected=" + plugin.isFoliaDetected());
            return true;
        }

        if (args[0].equalsIgnoreCase("reload")) {
            if (!sender.hasPermission("vtl.reload")) {
                sender.sendMessage("You do not have permission to reload this plugin.");
                return true;
            }

            plugin.reloadPlugin();
            sender.sendMessage("VillagerTradeLimiter configuration reloaded.");
            return true;
        }

        if (args[0].equalsIgnoreCase("debug")) {
            if (!sender.hasPermission("vtl.debug")) {
                sender.sendMessage("You do not have permission to change debug mode.");
                return true;
            }

            boolean newValue = args.length < 2
                    ? !plugin.isDebugEnabled()
                    : switch (args[1].toLowerCase(Locale.ROOT)) {
                        case "on", "true", "enable" -> true;
                        case "off", "false", "disable" -> false;
                        default -> !plugin.isDebugEnabled();
                    };

            plugin.setDebugEnabled(newValue);
            sender.sendMessage("VillagerTradeLimiter debug mode is now " + (newValue ? "enabled" : "disabled") + ".");
            return true;
        }

        sender.sendMessage("Usage: /" + label + " <info|reload|debug>");
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();
        if (args.length == 1) {
            addIfMatching(completions, args[0], "info");
            addIfMatching(completions, args[0], "reload");
            addIfMatching(completions, args[0], "debug");
        } else if (args.length == 2 && args[0].equalsIgnoreCase("debug")) {
            addIfMatching(completions, args[1], "on");
            addIfMatching(completions, args[1], "off");
        }
        return completions;
    }

    private static void addIfMatching(List<String> completions, String input, String candidate) {
        if (candidate.startsWith(input.toLowerCase(Locale.ROOT))) {
            completions.add(candidate);
        }
    }
}
