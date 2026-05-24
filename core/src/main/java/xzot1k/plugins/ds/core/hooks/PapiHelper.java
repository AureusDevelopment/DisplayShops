/*
 * Copyright (c) 2021 XZot1K, All rights reserved.
 */

package xzot1k.plugins.ds.core.hooks;

import me.clip.placeholderapi.PlaceholderAPI;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import xzot1k.plugins.ds.DisplayShops;
import xzot1k.plugins.ds.api.objects.MarketRegion;

public class PapiHelper extends PlaceholderExpansion {

    private DisplayShops pluginInstance;

    public PapiHelper(DisplayShops pluginInstance) {
        setPluginInstance(pluginInstance);
    }

    public String replace(Player player, String text) {
        return PlaceholderAPI.setPlaceholders(player, text);
    }

    public String replace(OfflinePlayer player, String text) {
        return PlaceholderAPI.setPlaceholders(player, text);
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public boolean canRegister() {
        return true;
    }

    @Override
    public String getAuthor() {
        return getPluginInstance().getDescription().getAuthors().toString();
    }

    @Override
    public String getIdentifier() {
        return "DisplayShops";
    }

    @Override
    public String getVersion() {
        return getPluginInstance().getDescription().getVersion();
    }

    @Override
    public String onPlaceholderRequest(Player player, String identifier) {
        if (player == null) return "";
        DisplayShops.placeholderAPI++;
        if (identifier.equalsIgnoreCase("limit"))
            return String.valueOf(getPluginInstance().getManager().getShopLimit(player));
        else if (identifier.equalsIgnoreCase("count"))
            return String.valueOf(getPluginInstance().getManager().getPlayerShops(player).size());
        else if (identifier.toLowerCase().contains("mrowner_")) {
            String[] args = identifier.toLowerCase().split("_");
            if (args.length >= 1) {
                MarketRegion region = getPluginInstance().getManager().getMarketRegion(args[1]);
                if (region != null) {
                    if (region.getRenter() == null) return "---";
                    OfflinePlayer renter = getPluginInstance().getServer().getOfflinePlayer(region.getRenter());
                    if (renter != null) return renter.getName();
                }
            }
        } else if (identifier.toLowerCase().startsWith("mrtime_")) {
            String[] args = identifier.toLowerCase().split("_");
            if (args.length >= 1) {
                MarketRegion region = getPluginInstance().getManager().getMarketRegion(args[1]);
                if (region != null) {
                    if (region.getRenter() == null) return "---";
                    return region.formattedTimeUntilExpire();
                }
            }
        }

        return null;
    }

    private DisplayShops getPluginInstance() {
        return pluginInstance;
    }

    private void setPluginInstance(DisplayShops pluginInstance) {
        this.pluginInstance = pluginInstance;
    }
}
