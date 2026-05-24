/*
 * Copyright (c) 2021 XZot1K, All rights reserved.
 */

package xzot1k.plugins.ds.core;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import xzot1k.plugins.ds.DisplayShops;
import xzot1k.plugins.ds.api.objects.Appearance;
import xzot1k.plugins.ds.api.objects.MarketRegion;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class TabCompleter implements org.bukkit.command.TabCompleter {

    private DisplayShops pluginInstance;

    public TabCompleter(DisplayShops pluginInstance) {
        setPluginInstance(pluginInstance);
    }

    @Override
    public List<String> onTabComplete(CommandSender commandSender, Command command, String label, String[] args) {
        if (command.getName().equalsIgnoreCase("displayshops")) {
            List<String> contents = new ArrayList<>();
            switch (args.length) {
                case 1: {
                    if (commandSender.hasPermission("displayshops.bbmaccess"))
                        if ("lock".startsWith(args[0].toLowerCase())) contents.add("lock");
                        else if ("unlock".startsWith(args[0].toLowerCase())) contents.add("unlock");

                    if (commandSender.hasPermission("displayshops.give") && "give".startsWith(args[0].toLowerCase()))
                        contents.add("give");

                    if (commandSender.hasPermission("displayshops.commands")) {
                        if ("addcommand".startsWith(args[0].toLowerCase()))
                            contents.add("addcommand");
                        if ("removecommand".startsWith(args[0].toLowerCase()))
                            contents.add("removecommand");
                        if ("commands".startsWith(args[0].toLowerCase()))
                            contents.add("commands");
                        if ("commandmode".startsWith(args[0].toLowerCase()))
                            contents.add("commandmode");
                    }

                    if (commandSender.hasPermission("displayshops.advertise") && "advertise".startsWith(args[0].toLowerCase()))
                        contents.add("advertise");

                    if (commandSender.hasPermission("displayshops.dmr") && "deletemarketregion".startsWith(args[0].toLowerCase()))
                        contents.add("deletemarketregion");

                    if (commandSender.hasPermission("displayshops.cmr") && "createmarketregion".startsWith(args[0].toLowerCase()))
                        contents.add("createmarketregion");

                    if (commandSender.hasPermission("displayshops.owner") && "owner".startsWith(args[0].toLowerCase()))
                        contents.add("owner");

                    if (commandSender.hasPermission("displayshops.stock") && "stock".startsWith(args[0].toLowerCase()))
                        contents.add("stock");

                    if (commandSender.hasPermission("displayshops.info") && "info".startsWith(args[0].toLowerCase()))
                        contents.add("info");

                    if (commandSender.hasPermission("displayshops.delete") && "delete".startsWith(args[0].toLowerCase()))
                        contents.add("delete");

                    if (commandSender.hasPermission("displayshops.buy") && "buy".startsWith(args[0].toLowerCase()))
                        contents.add("buy");

                    if (commandSender.hasPermission("displayshops.admin") && "admin".startsWith(args[0].toLowerCase()))
                        contents.add("admin");

                    if (commandSender.hasPermission("displayshops.sm") && "selectionmode".startsWith(args[0].toLowerCase()))
                        contents.add("selectionmode");

                    if (commandSender.hasPermission("displayshops.mrl") && "marketregionlist".startsWith(args[0].toLowerCase()))
                        contents.add("marketregionlist");

                    if (commandSender.hasPermission("displayshops.visit") && "visit".startsWith(args[0].toLowerCase()))
                        contents.add("visit");

                    if (commandSender.hasPermission("displayshops.copy") && "copy".startsWith(args[0].toLowerCase()))
                        contents.add("copy");

                    if (commandSender.hasPermission("displayshops.buyprice") && "buyprice".startsWith(args[0].toLowerCase()))
                        contents.add("buyprice");

                    if (commandSender.hasPermission("displayshops.sellprice") && "sellprice".startsWith(args[0].toLowerCase()))
                        contents.add("sellprice");

                    if ((commandSender.hasPermission("displayshops.balwithdraw") || commandSender.hasPermission("displayshops.baldeposit"))
                            && "balance".startsWith(args[0].toLowerCase())) contents.add("balance");

                    if (commandSender.hasPermission("displayshops.cost") && "cost".startsWith(args[0].toLowerCase()))
                        contents.add("cost");

                    if (commandSender.hasPermission("displayshops.rcost")) {
                        if ("renewcost".startsWith(args[0].toLowerCase())) contents.add("renewcost");
                        else if ("rcost".startsWith(args[0].toLowerCase())) contents.add("rcost");
                    }
                    break;
                }
                case 2: {
                    if ((args[0].equalsIgnoreCase("bal") || args[0].equalsIgnoreCase("balance"))) {
                        if (commandSender.hasPermission("displayshops.balwithdraw") && "withdraw".startsWith(args[1].toLowerCase()))
                            contents.add("withdraw");
                        else if (commandSender.hasPermission("displayshops.baldeposit") && "deposit".startsWith(args[1].toLowerCase()))
                            contents.add("deposit");
                    }

                    if ((commandSender.hasPermission("displayshops.give") && args[0].equalsIgnoreCase("give"))
                            || ((commandSender.hasPermission("displayshops.bbmaccess") || commandSender.hasPermission("displayshops.appearance"))
                            && (args[0].equalsIgnoreCase("lock") || args[0].equalsIgnoreCase("unlock")))) {
                        for (Player player : getPluginInstance().getServer().getOnlinePlayers())
                            if (player.getName().toLowerCase().startsWith(args[1].toLowerCase()))
                                contents.add(player.getName());
                    }

                    if ((commandSender.hasPermission("displayshops.cost") && "cost".startsWith(args[0].toLowerCase()))
                            || (commandSender.hasPermission("displayshops.rcost") && ("renewcost".startsWith(args[0].toLowerCase())
                            || "rcost".startsWith(args[0].toLowerCase()))))
                        for (int i = -1; ++i < getPluginInstance().getManager().getMarketRegions().size(); ) {
                            final MarketRegion region = getPluginInstance().getManager().getMarketRegions().get(i);
                            if (region.getMarketId().toLowerCase().startsWith(args[1].toLowerCase()))
                                contents.add(region.getMarketId());
                        }

                    break;
                }
                case 3: {
                    if (((commandSender.hasPermission("displayshops.bbmaccess") || commandSender.hasPermission("displayshops.appearance"))
                            && (args[0].equalsIgnoreCase("lock") || args[0].equalsIgnoreCase("unlock"))))
                        Appearance.getAppearances().parallelStream().filter(appearance -> (appearance.getId().toLowerCase().startsWith(args[2].toLowerCase())))
                                .forEach(appearance -> contents.add(appearance.getId()));
                    break;
                }
            }

            if (!contents.isEmpty()) Collections.sort(contents);
            return contents;
        }

        return null;
    }

    // getters & setters
    private DisplayShops getPluginInstance() {
        return pluginInstance;
    }

    private void setPluginInstance(DisplayShops pluginInstance) {
        this.pluginInstance = pluginInstance;
    }
}