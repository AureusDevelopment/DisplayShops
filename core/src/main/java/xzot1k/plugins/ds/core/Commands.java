/*
 * Copyright (c) 2021 XZot1K, All rights reserved.
 */

package xzot1k.plugins.ds.core;



import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.ItemTag;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.chat.hover.content.Item;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Entity;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.Player;
import org.bukkit.entity.TextDisplay;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import xzot1k.plugins.ds.DisplayShops;
import xzot1k.plugins.ds.api.enums.EconomyCallType;
import xzot1k.plugins.ds.api.enums.EditType;
import xzot1k.plugins.ds.api.events.EconomyCallEvent;
import xzot1k.plugins.ds.api.events.ShopEditEvent;
import xzot1k.plugins.ds.api.objects.*;
import xzot1k.plugins.ds.core.packets.Display;
import xzot1k.plugins.ds.core.utils.nms_utils.Ref;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.logging.Level;

public class Commands implements CommandExecutor {

    private DisplayShops pluginInstance;

    public Commands(DisplayShops pluginInstance) {
        setPluginInstance(pluginInstance);
    }

    @Override
    public boolean onCommand(@NotNull CommandSender commandSender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (command.getName().equalsIgnoreCase("dsfilter")) {
            if (args.length > 1) {
                StringBuilder sb = new StringBuilder();
                for (int i = -1; ++i < args.length; ) {
                    if (sb.length() > 0) sb.append(" ");
                    sb.append(args[i]);
                }

                runVisit(commandSender, sb.toString(), sb.toString());
                return true;
            } else if (args.length == 1) {
                runVisit(commandSender, args[0], args[0]);
                return true;
            }

            runVisit(commandSender, null, null);
            return true;
        } else if (command.getName().equalsIgnoreCase("displayshops")) {
            if (args.length >= 2 && (args[0].equalsIgnoreCase("addcommand") || args[0].equalsIgnoreCase("addcmd"))) {
                runAddCommand(commandSender, args);
                return true;
            }

            if (args.length > 0 && (args[0].equalsIgnoreCase("visit") || args[0].equalsIgnoreCase("sw"))) {
                if (args.length > 2) {
                    StringBuilder sb = new StringBuilder();
                    for (int i = 0; ++i < args.length; ) {
                        if (sb.length() > 0) sb.append(" ");
                        sb.append(args[i]);
                    }

                    runVisit(commandSender, sb.toString(), sb.toString());
                    return true;
                } else if (args.length == 2) {
                    runVisit(commandSender, args[1], args[1]);
                    return true;
                }
            }

            switch (args.length) {
                case 1:
                    if (args[0].equalsIgnoreCase("block")) {
                        runBlock(commandSender);
                        return true;
                    } else if (args[0].equalsIgnoreCase("unblock")) {
                        runUnBlock(commandSender);
                        return true;
                    } else if (args[0].equalsIgnoreCase("cdb")) {
                        runCDB(commandSender);
                        return true;
                    } else if (args[0].equalsIgnoreCase("cleanup")) {
                        runCleanUp(commandSender);
                        return true;
                    } else if (args[0].equalsIgnoreCase("info")) {
                        if (Ref.serverType() == Ref.ServerType.PAPER) {
                            runInfoAdventure(commandSender);
                        } else {
                            runInfo(commandSender);
                        }
                        return true;
                    } else if (args[0].equalsIgnoreCase("marketregionlist") || args[0].equalsIgnoreCase("mrl")) {
                        runMarketList(commandSender);
                        return true;
                    } else if (args[0].equalsIgnoreCase("selectionmode") || args[0].equalsIgnoreCase("sm")) {
                        runSelectionMode(commandSender);
                        return true;
                    } else if (args[0].equalsIgnoreCase("admin")) {
                        runAdmin(commandSender);
                        return true;
                    } else if (args[0].equalsIgnoreCase("commandmode") || args[0].equalsIgnoreCase("cm")) {
                        runCommandMode(commandSender);
                        return true;
                    } else if (args[0].equalsIgnoreCase("commands")) {
                        runCommands(commandSender);
                        return true;
                    } else if (args[0].equalsIgnoreCase("reload")) {
                        runReload(commandSender);
                        return true;
                    } else if (args[0].equalsIgnoreCase("buy")) {
                        runBuy(commandSender, "1");
                        return true;
                    } else if (args[0].equalsIgnoreCase("delete")) {
                        runDelete(commandSender);
                        return true;
                    } else if (args[0].equalsIgnoreCase("rent") || args[0].equalsIgnoreCase("renew")) {
                        runRent(commandSender, null);
                        return true;
                    } else if (args[0].equalsIgnoreCase("reset")) {
                        runReset(commandSender, null);
                        return true;
                    } else if (args[0].equalsIgnoreCase("time")) {
                        runTime(commandSender, null);
                        return true;
                    } else if (args[0].equalsIgnoreCase("visit")) {
                        runVisit(commandSender, null, null);
                        return true;
                    } else if (args[0].equalsIgnoreCase("advertise")
                            || args[0].equalsIgnoreCase("duyuru") || args[0].equalsIgnoreCase("reklam")) {
                        runAdvertise(commandSender);
                        return true;
                    } else if (args[0].equalsIgnoreCase("notify")) {
                        runNotify(commandSender);
                        return true;
                    } else if (args[0].equalsIgnoreCase("id")) {
                        runId(commandSender);
                        return true;
                    }
                    break;
                case 2:
                    if (args[0].equalsIgnoreCase("buyprice") || args[0].equalsIgnoreCase("bp")) {
                        runBuyPrice(commandSender, args);
                        return true;
                    } else if (args[0].equalsIgnoreCase("sellprice") || args[0].equalsIgnoreCase("sp")) {
                        runSellPrice(commandSender, args);
                        return true;
                    } else if (args[0].equalsIgnoreCase("withdraw")) {
                        runWithdraw(commandSender, args);
                        return true;
                    } else if (args[0].equalsIgnoreCase("deposit")) {
                        runDeposit(commandSender, args);
                        return true;
                    } else if (args[0].equalsIgnoreCase("clear")) {
                        runClear(commandSender, args[1]);
                        return true;
                    } else if (args[0].equalsIgnoreCase("buy")) {
                        runBuy(commandSender, args[1]);
                        return true;
                    } else if (args[0].equalsIgnoreCase("removecommand") || args[0].equalsIgnoreCase("removecmd")) {
                        runRemoveCommand(commandSender, args);
                        return true;
                    } else if (args[0].equalsIgnoreCase("deletemarketregion") || args[0].equalsIgnoreCase("dmr")) {
                        runDeleteMarket(commandSender, args);
                        return true;
                    } else if (args[0].equalsIgnoreCase("createmarketregion") || args[0].equalsIgnoreCase("cmr")) {
                        runCreateMarket(commandSender, args);
                        return true;
                    } else if (args[0].equalsIgnoreCase("owner")) {
                        runOwner(commandSender, args);
                        return true;
                    } else if (args[0].equalsIgnoreCase("stock")) {
                        runStock(commandSender, args);
                        return true;
                    } else if (args[0].equalsIgnoreCase("unlock")) {
                        runAppearanceAccess(commandSender, true, args);
                        return true;
                    } else if (args[0].equalsIgnoreCase("lock")) {
                        runAppearanceAccess(commandSender, false, args);
                        return true;
                    } else if (args[0].equalsIgnoreCase("rent") || args[0].equalsIgnoreCase("renew")) {
                        runRent(commandSender, args[1]);
                        return true;
                    } else if (args[0].equalsIgnoreCase("reset")) {
                        runReset(commandSender, args[1]);
                        return true;
                    } else if (args[0].equalsIgnoreCase("time")) {
                        runTime(commandSender, args[1]);
                        return true;
                    } else if (args[0].equalsIgnoreCase("visit")) {
                        runVisit(commandSender, args[1], null);
                        return true;
                    } else if (args[0].equalsIgnoreCase("cost")) {
                        runCost(commandSender, null, args[1]);
                        return true;
                    } else if (args[0].equalsIgnoreCase("renewcost") || args[0].equalsIgnoreCase("rcost")) {
                        runRenewCost(commandSender, null, args[1]);
                        return true;
                    } else if (args[0].equalsIgnoreCase("kill")) {
                        runKill(commandSender, args);
                        return true;
                    }

                    break;
                case 3:
                    if (args[0].equalsIgnoreCase("balance") || args[0].equalsIgnoreCase("bal")) {
                        runBalance(commandSender, args);
                        return true;
                    } else if (args[0].equalsIgnoreCase("give")) {
                        runGive(commandSender, args);
                        return true;
                    } else if (args[1].equalsIgnoreCase("filter")) {
                        runVisit(commandSender, null, args[2]);
                        return true;
                    } else if (args[0].equalsIgnoreCase("cost")) {
                        runCost(commandSender, args[1], args[2]);
                        return true;
                    } else if (args[0].equalsIgnoreCase("renewcost") || args[0].equalsIgnoreCase("rcost")) {
                        runRenewCost(commandSender, args[1], args[2]);
                        return true;
                    } else if (args[0].equalsIgnoreCase("unlock")) {
                        runAppearanceAccess(commandSender, true, args);
                        return true;
                    } else if (args[0].equalsIgnoreCase("lock")) {
                        runAppearanceAccess(commandSender, false, args);
                        return true;
                    } else if (args[0].equalsIgnoreCase("kill")) {
                        runKill(commandSender, args);
                        return true;
                    }
                    break;
            }

            sendHelp(commandSender);
            return true;
        } else
            return false;
    }

    private void runKill(CommandSender commandSender, String[] args) {
        if (!(commandSender instanceof Player)) {
            String message = getPluginInstance().getLangConfig().getString("must-be-player");
            if (message != null && !message.equalsIgnoreCase(""))
                commandSender.sendMessage(getPluginInstance().getManager().color(message));
            return;
        }

        final Player player = (Player) commandSender;

        String type = "ArmorStand";
        if (args.length >= 2) {type = args[1];}

        int radius = 8;
        if (args.length >= 3) {
            if (getPluginInstance().getManager().isNotNumeric(args[2])) {
                String message = getPluginInstance().getLangConfig().getString("invalid-amount");
                if (message != null && !message.equalsIgnoreCase(""))
                    getPluginInstance().getManager().sendMessage(player, message);
                return;
            }

            radius = Integer.parseInt(args[2]);
        }

        int counter = 0, removedCounter = 0;

        List<Entity> entities = player.getNearbyEntities(radius, radius, radius);
        counter += entities.size();

        for (int i = -1; ++i < entities.size(); ) {
            Entity entity = entities.get(i);

            if (((entity instanceof TextDisplay || entity instanceof ItemDisplay) && type.toLowerCase().contains("both"))
                    || (entity instanceof TextDisplay && (type.toLowerCase().contains("armorstand") || type.toLowerCase().contains("stand")))
                    || (entity instanceof ItemDisplay && (type.toLowerCase().contains("item") || type.toLowerCase().contains("drop")))) {
                entity.remove();
                removedCounter++;
            }
        }

        getPluginInstance().getManager().sendMessage(player, getPluginInstance().getLangConfig().getString("kill-command"),
                ("{count}:" + counter), ("{removed}:" + removedCounter));
    }

    // command helpers
    private void runBalance(CommandSender commandSender, String[] args) {
        if (!(commandSender instanceof Player)) {
            String message = getPluginInstance().getLangConfig().getString("must-be-player");
            if (message != null && !message.equalsIgnoreCase(""))
                commandSender.sendMessage(getPluginInstance().getManager().color(message));
            return;
        }

        final boolean isWithdraw = (args[1].equalsIgnoreCase("withdraw") || args[1].equalsIgnoreCase("w")),
                isDeposit = (!isWithdraw && (args[1].equalsIgnoreCase("deposit") || args[1].equalsIgnoreCase("d")));

        if (!isWithdraw && !isDeposit) {
            String message = getPluginInstance().getLangConfig().getString("balance-command-usage");
            if (message != null && !message.equalsIgnoreCase(""))
                commandSender.sendMessage(getPluginInstance().getManager().color(message));
            return;
        }

        Player player = (Player) commandSender;
        if (!player.hasPermission("displayshops.bal" + (isWithdraw ? "withdraw" : "deposit"))) {
            String message = getPluginInstance().getLangConfig().getString("no-permission");
            if (message != null && !message.equalsIgnoreCase(""))
                getPluginInstance().getManager().sendMessage(player, message);
            return;
        }

        Block block = player.getTargetBlock(null, 10);
        Shop shop = getPluginInstance().getManager().getShop(block.getLocation());
        if (shop == null) {
            String message = getPluginInstance().getLangConfig().getString("shop-invalid");
            if (message != null && !message.equalsIgnoreCase(""))
                getPluginInstance().getManager().sendMessage(player, message);
            return;
        }

        if (!player.hasPermission("displayshops.admin") && (shop.isAdminShop()
                || (shop.getOwnerUniqueId() != null && !shop.getOwnerUniqueId().toString().equals(player.getUniqueId().toString())))) {
            String message = getPluginInstance().getLangConfig().getString("not-shop-owner");
            if (message != null && !message.equalsIgnoreCase(""))
                getPluginInstance().getManager().sendMessage(player, message);
            return;
        }

        boolean isAll = args[1].equalsIgnoreCase("all");
        if (!isAll && getPluginInstance().getManager().isNotNumeric(args[2].replace(",", "."))) {
            String message = getPluginInstance().getLangConfig().getString("invalid-amount");
            if (message != null && !message.equalsIgnoreCase(""))
                getPluginInstance().getManager().sendMessage(player, message);
            return;
        }

        double amount = (isAll ? (isWithdraw ? shop.getStoredBalance() : getPluginInstance().getEconomyHandler().getBalance(player, shop))
                : Double.parseDouble(args[2].replace(",", ".")));
        if (amount < 0) {
            getPluginInstance().getManager().sendMessage(player, getPluginInstance().getLangConfig().getString("negative-entry"));
            return;
        }

        String tradeItemName = shop.getTradeItemName();
        if (isWithdraw) {
            if ((shop.getStoredBalance() - amount) < 0 && shop.getStoredBalance() != amount) {
                getPluginInstance().getManager().sendMessage(player, Objects.requireNonNull(getPluginInstance().getLangConfig().getString("balance-withdraw-fail"))
                        .replace("{trade-item}", tradeItemName)
                        .replace("{balance}", getPluginInstance().getEconomyHandler().format(shop, shop.getCurrencyType(), shop.getStoredBalance())));
                return;
            }
        } else {
            if (!getPluginInstance().getEconomyHandler().has(player, shop, amount)) {
                getPluginInstance().getManager().sendMessage(player, Objects.requireNonNull(getPluginInstance().getLangConfig().getString("insufficient-funds"))
                        .replace("{trade-item}", tradeItemName)
                        .replace("{raw-price}", getPluginInstance().getEconomyHandler().format(shop, shop.getCurrencyType(), amount))
                        .replace("{price}", getPluginInstance().getEconomyHandler().format(shop, shop.getCurrencyType(), amount)));
                return;
            }
        }

        if (isDeposit && (shop.getStoredBalance() + amount) >= getPluginInstance().getConfig().getLong("max-stored-currency")) {
            getPluginInstance().getManager().sendMessage(player, Objects.requireNonNull(getPluginInstance().getLangConfig().getString("max-stored-currency"))
                    .replace("{trade-item}", tradeItemName)
                    .replace("{amount}", getPluginInstance().getEconomyHandler().format(shop, shop.getCurrencyType(), amount)));
            return;
        }

        // call edit event
        ShopEditEvent shopEditEvent = new ShopEditEvent(player, shop, (isWithdraw ? EditType.WITHDRAW_BALANCE : EditType.DEPOSIT_BALANCE), (isWithdraw ? -amount : amount));
        getPluginInstance().getServer().getPluginManager().callEvent(shopEditEvent);
        if (shopEditEvent.isCancelled()) return;

        final EconomyCallEvent economyCallEvent = EconomyCallEvent.call(player, shop, (isWithdraw ? EconomyCallType.WITHDRAW_BALANCE : EconomyCallType.DEPOSIT_BALANCE),
                getPluginInstance().getConfig().getDouble("prices.balance"));
        if (economyCallEvent.failed()) return;

        if (isWithdraw) getPluginInstance().getEconomyHandler().deposit(player, shop, amount);
        else getPluginInstance().getEconomyHandler().withdraw(player, shop, amount);

        shop.setStoredBalance(shop.getStoredBalance() + (isWithdraw ? -amount : amount));
        shop.updateTimeStamp();
        getPluginInstance().getManager().sendMessage(player, Objects.requireNonNull(getPluginInstance().getLangConfig().getString("balance-"
                        + (isWithdraw ? "withdrawn" : "deposited"))).replace("{trade-item}", tradeItemName)
                .replace("{amount}", getPluginInstance().getEconomyHandler().format(shop, shop.getCurrencyType(), amount)));
    }

    private void runWithdraw(CommandSender commandSender, String[] args) {
        if (!(commandSender instanceof Player)) {
            String message = getPluginInstance().getLangConfig().getString("must-be-player");
            if (message != null && !message.equalsIgnoreCase(""))
                commandSender.sendMessage(getPluginInstance().getManager().color(message));
            return;
        }

        Player player = (Player) commandSender;
        if (!player.hasPermission("displayshops.withdraw")) {
            String message = getPluginInstance().getLangConfig().getString("no-permission");
            if (message != null && !message.equalsIgnoreCase(""))
                getPluginInstance().getManager().sendMessage(player, message);
            return;
        }

        Block block = player.getTargetBlock(null, 10);
        Shop shop = getPluginInstance().getManager().getShop(block.getLocation());
        if (shop == null) {
            String message = getPluginInstance().getLangConfig().getString("shop-invalid");
            if (message != null && !message.equalsIgnoreCase(""))
                getPluginInstance().getManager().sendMessage(player, message);
            return;
        }

        if (!player.hasPermission("displayshops.admin") && (shop.isAdminShop()
                || (shop.getOwnerUniqueId() != null && !shop.getOwnerUniqueId().toString().equals(player.getUniqueId().toString())))) {
            String message = getPluginInstance().getLangConfig().getString("not-shop-owner");
            if (message != null && !message.equalsIgnoreCase(""))
                getPluginInstance().getManager().sendMessage(player, message);
            return;
        }

        if (getPluginInstance().getManager().isNotNumeric(args[1])) {
            String message = getPluginInstance().getLangConfig().getString("invalid-amount");
            if (message != null && !message.equalsIgnoreCase(""))
                getPluginInstance().getManager().sendMessage(player, message);
            return;
        }

        int amount = Math.max(1, Integer.parseInt(args[1]));
        String message;
        if (amount > shop.getStock()) {
            message = getPluginInstance().getLangConfig().getString("stock-withdraw-fail");
            if (message != null && !message.equalsIgnoreCase(""))
                getPluginInstance().getManager().sendMessage(player, message
                        .replace("{amount}", getPluginInstance().getManager().formatNumber(amount, false)));
            return;
        }

        final int availableSpace = Math.min(getPluginInstance().getManager().getInventorySpaceForItem(player,
                shop.getShopItem()), (36 * shop.getShopItem().getMaxStackSize()));
        amount = Math.min(amount, availableSpace);

        if (player.getInventory().firstEmpty() == -1) {
            message = getPluginInstance().getLangConfig().getString("insufficient-space");
            if (message != null && !message.equalsIgnoreCase(""))
                getPluginInstance().getManager().sendMessage(player, message
                        .replace("{space}", getPluginInstance().getManager().formatNumber(availableSpace, false)));
            return;
        }

        // call edit event
        ShopEditEvent shopEditEvent = new ShopEditEvent(player, shop, EditType.WITHDRAW_STOCK, amount);
        getPluginInstance().getServer().getPluginManager().callEvent(shopEditEvent);
        if (shopEditEvent.isCancelled()) return;

        final EconomyCallEvent economyCallEvent = EconomyCallEvent.call(player, shop, EconomyCallType.EDIT_ACTION,
                getPluginInstance().getConfig().getDouble("prices.withdraw-stock"));
        if (economyCallEvent.failed()) return;

        shop.setStock(shop.getStock() - amount);
        shop.updateTimeStamp();

        final ItemStack itemStack = shop.getShopItem().clone();
        final double finalAmount = amount;
        getPluginInstance().getServer().getScheduler().runTask(getPluginInstance(), () ->
                getPluginInstance().getManager().giveItemStacks(player, itemStack, (int) finalAmount));

        message = getPluginInstance().getLangConfig().getString("withdrawn-stock");
        if (message != null && !message.equalsIgnoreCase(""))
            getPluginInstance().getManager().sendMessage(player, message
                    .replace("{amount}", getPluginInstance().getManager().formatNumber(amount, false)));
        getPluginInstance().runEventCommands("shop-withdraw", player);
    }

    private void runDeposit(CommandSender commandSender, String[] args) {
        if (!(commandSender instanceof Player)) {
            String message = getPluginInstance().getLangConfig().getString("must-be-player");
            if (message != null && !message.equalsIgnoreCase(""))
                commandSender.sendMessage(getPluginInstance().getManager().color(message));
            return;
        }

        Player player = (Player) commandSender;
        if (!player.hasPermission("displayshops.deposit")) {
            String message = getPluginInstance().getLangConfig().getString("no-permission");
            if (message != null && !message.equalsIgnoreCase(""))
                getPluginInstance().getManager().sendMessage(player, message);
            return;
        }

        Block block = player.getTargetBlock(null, 10);
        Shop shop = getPluginInstance().getManager().getShop(block.getLocation());
        if (shop == null) {
            String message = getPluginInstance().getLangConfig().getString("shop-invalid");
            if (message != null && !message.equalsIgnoreCase(""))
                getPluginInstance().getManager().sendMessage(player, message);
            return;
        }

        if (!player.hasPermission("displayshops.admin") && (shop.isAdminShop()
                || (shop.getOwnerUniqueId() != null && !shop.getOwnerUniqueId().toString().equals(player.getUniqueId().toString())))) {
            String message = getPluginInstance().getLangConfig().getString("not-shop-owner");
            if (message != null && !message.equalsIgnoreCase(""))
                getPluginInstance().getManager().sendMessage(player, message);
            return;
        }

        if (getPluginInstance().getManager().isNotNumeric(args[1])) {
            String message = getPluginInstance().getLangConfig().getString("invalid-amount");
            if (message != null && !message.equalsIgnoreCase(""))
                getPluginInstance().getManager().sendMessage(player, message);
            return;
        }

        int amount = Math.max(1, Integer.parseInt(args[1]));
        String message;
        final int maxStock = shop.getMaxStock();
        int totalItemCount = getPluginInstance().getManager().getItemAmount(player.getInventory(), shop.getShopItem());
        if (totalItemCount <= 0 || totalItemCount < amount) {
            message = getPluginInstance().getLangConfig().getString("insufficient-items");
            if (message != null && !message.equalsIgnoreCase(""))
                getPluginInstance().getManager().sendMessage(player, message);
            return;
        }

        int difference = (maxStock - shop.getStock()), amountToRemove = (difference > 0 && difference >= amount ? amount : Math.max(difference, 0));
        if (amountToRemove == 0 || shop.getStock() >= maxStock) {
            message = getPluginInstance().getLangConfig().getString("stock-deposit-fail");
            if (message != null && !message.equalsIgnoreCase(""))
                getPluginInstance().getManager().sendMessage(player, message
                        .replace("{amount}", getPluginInstance().getManager().formatNumber(amount, false)));
            return;
        }

        // call edit event
        ShopEditEvent shopEditEvent = new ShopEditEvent(player, shop, EditType.DEPOSIT_STOCK, amountToRemove);
        getPluginInstance().getServer().getPluginManager().callEvent(shopEditEvent);
        if (shopEditEvent.isCancelled()) return;

        final EconomyCallEvent economyCallEvent = EconomyCallEvent.call(player, shop, EconomyCallType.EDIT_ACTION,
                getPluginInstance().getConfig().getDouble("prices.deposit-stock"));
        if (economyCallEvent.failed()) return;

        getPluginInstance().getManager().removeItem(player.getInventory(), shop.getShopItem(), amountToRemove);
        shop.setStock(shop.getStock() + amountToRemove);
        shop.updateTimeStamp();

        message = getPluginInstance().getLangConfig().getString("deposited-stock");
        if (message != null && !message.equalsIgnoreCase(""))
            getPluginInstance().getManager().sendMessage(player, message
                    .replace("{amount}", getPluginInstance().getManager().formatNumber(amountToRemove, false)));
        getPluginInstance().runEventCommands("shop-deposit", player);
    }

    private void runSellPrice(CommandSender commandSender, String[] args) {
        if (!(commandSender instanceof Player)) {
            String message = getPluginInstance().getLangConfig().getString("must-be-player");
            if (message != null && !message.equalsIgnoreCase(""))
                commandSender.sendMessage(getPluginInstance().getManager().color(message));
            return;
        }

        Player player = (Player) commandSender;
        if (!player.hasPermission("displayshops.sellprice")) {
            String message = getPluginInstance().getLangConfig().getString("no-permission");
            if (message != null && !message.equalsIgnoreCase(""))
                getPluginInstance().getManager().sendMessage(player, message);
            return;
        }

        Block block = player.getTargetBlock(null, 10);
        Shop shop = getPluginInstance().getManager().getShop(block.getLocation());
        if (shop == null) {
            String message = getPluginInstance().getLangConfig().getString("shop-invalid");
            if (message != null && !message.equalsIgnoreCase(""))
                getPluginInstance().getManager().sendMessage(player, message);
            return;
        }

        if (!player.hasPermission("displayshops.admin") && (shop.isAdminShop()
                || (shop.getOwnerUniqueId() != null && !shop.getOwnerUniqueId().toString().equals(player.getUniqueId().toString())))) {
            String message = getPluginInstance().getLangConfig().getString("not-shop-owner");
            if (message != null && !message.equalsIgnoreCase(""))
                getPluginInstance().getManager().sendMessage(player, message);
            return;
        }

        if (getPluginInstance().getManager().isNotNumeric(args[1])) {
            String message = getPluginInstance().getLangConfig().getString("invalid-amount");
            if (message != null && !message.equalsIgnoreCase(""))
                getPluginInstance().getManager().sendMessage(player, message);
            return;
        }

        final double price = Math.max(-1, Double.parseDouble(args[1]));
        if (price > -1) {
            double foundSellPriceMax = (shop.getShopItem() != null ? getPluginInstance().getManager().getMaterialMaxPrice(shop.getShopItem(), false) : 0),
                    foundSellPriceMin = shop.getShopItem() != null ? getPluginInstance().getManager().getMaterialMinPrice(shop.getShopItem(), false) : 0;
            if (price < (foundSellPriceMin * shop.getShopItemAmount()) || price > (foundSellPriceMax * shop.getShopItemAmount())) {
                getPluginInstance().getManager().sendMessage(player, getPluginInstance().getLangConfig().getString("sell-price-limit"));
                return;
            }

            if (price > shop.getBuyPrice(false) && shop.getBuyPrice(false) >= 0) {
                String message = getPluginInstance().getLangConfig().getString("sell-price-greater");
                if (message != null && !message.equalsIgnoreCase(""))
                    getPluginInstance().getManager().sendMessage(player, message);
                return;
            }
        }

        // call edit event
        ShopEditEvent shopEditEvent = new ShopEditEvent(player, shop, EditType.SELL_PRICE, price);
        getPluginInstance().getServer().getPluginManager().callEvent(shopEditEvent);
        if (shopEditEvent.isCancelled()) return;

        final EconomyCallEvent economyCallEvent = EconomyCallEvent.call(player, shop, EconomyCallType.EDIT_ACTION,
                getPluginInstance().getConfig().getDouble("prices.sale-item-change"));
        if (economyCallEvent.failed()) return;

        if (shop.getSellPrice(false) < 0) shop.setSellPrice(Math.min(price, shop.getBuyPrice(false)));
        else shop.setSellPrice(price);

        shop.updateTimeStamp();
        shop.save(true);

        getPluginInstance().getInSightTask().refreshShop(shop);

        String message;
        if (price == -1) message = getPluginInstance().getLangConfig().getString("selling-disabled");
        else message = getPluginInstance().getLangConfig().getString("sell-price-set");
        getPluginInstance().getManager().sendMessage(player, Objects.requireNonNull(message)
                .replace("{price}", getPluginInstance().getEconomyHandler().format(shop, shop.getCurrencyType(), price)));
        getPluginInstance().runEventCommands("shop-sell-price", player);
    }

    private void runBuyPrice(CommandSender commandSender, String[] args) {
        if (!(commandSender instanceof Player)) {
            String message = getPluginInstance().getLangConfig().getString("must-be-player");
            if (message != null && !message.equalsIgnoreCase(""))
                commandSender.sendMessage(getPluginInstance().getManager().color(message));
            return;
        }

        Player player = (Player) commandSender;
        if (!player.hasPermission("displayshops.buyprice")) {
            String message = getPluginInstance().getLangConfig().getString("no-permission");
            if (message != null && !message.equalsIgnoreCase(""))
                getPluginInstance().getManager().sendMessage(player, message);
            return;
        }

        Block block = player.getTargetBlock(null, 10);
        Shop shop = getPluginInstance().getManager().getShop(block.getLocation());
        if (shop == null) {
            String message = getPluginInstance().getLangConfig().getString("shop-invalid");
            if (message != null && !message.equalsIgnoreCase(""))
                getPluginInstance().getManager().sendMessage(player, message);
            return;
        }

        if (!player.hasPermission("displayshops.admin") && (shop.isAdminShop()
                || (shop.getOwnerUniqueId() != null && !shop.getOwnerUniqueId().toString().equals(player.getUniqueId().toString())))) {
            String message = getPluginInstance().getLangConfig().getString("not-shop-owner");
            if (message != null && !message.equalsIgnoreCase(""))
                getPluginInstance().getManager().sendMessage(player, message);
            return;
        }

        if (getPluginInstance().getManager().isNotNumeric(args[1])) {
            String message = getPluginInstance().getLangConfig().getString("invalid-amount");
            if (message != null && !message.equalsIgnoreCase(""))
                getPluginInstance().getManager().sendMessage(player, message);
            return;
        }

        final double price = Math.max(-1, Double.parseDouble(args[1]));

        if (price > -1) {
            double foundBuyPriceMax = (shop.getShopItem() != null ? getPluginInstance().getManager().getMaterialMaxPrice(shop.getShopItem(), true) : 0),
                    foundBuyPriceMin = shop.getShopItem() != null ? getPluginInstance().getManager().getMaterialMinPrice(shop.getShopItem(), true) : 0;
            if (price < (foundBuyPriceMin * shop.getShopItemAmount()) || price > (foundBuyPriceMax * shop.getShopItemAmount())) {
                getPluginInstance().getManager().sendMessage(player, getPluginInstance().getLangConfig().getString("buy-price-limit"));
                return;
            }

            if (price < shop.getSellPrice(false) && shop.getSellPrice(false) >= 0) {
                getPluginInstance().getManager().sendMessage(player, getPluginInstance().getLangConfig().getString("buy-price-less"));
                return;
            }
        }

        // call edit event
        ShopEditEvent shopEditEvent = new ShopEditEvent(player, shop, EditType.BUY_PRICE, price);
        getPluginInstance().getServer().getPluginManager().callEvent(shopEditEvent);
        if (shopEditEvent.isCancelled()) return;

        final EconomyCallEvent economyCallEvent = EconomyCallEvent.call(player, shop, EconomyCallType.EDIT_ACTION,
                getPluginInstance().getConfig().getDouble("prices.buy-price"));
        if (economyCallEvent.failed()) return;

        if (shop.getBuyPrice(false) < 0) shop.setBuyPrice(Math.max(price, shop.getBuyPrice(false)));
        else shop.setBuyPrice(price);

        shop.updateTimeStamp();
        shop.save(true);

        getPluginInstance().getInSightTask().refreshShop(shop);

        String message;
        if (price == -1) message = getPluginInstance().getLangConfig().getString("buying-disabled");
        else message = getPluginInstance().getLangConfig().getString("buy-price-set");
        getPluginInstance().getManager().sendMessage(player, Objects.requireNonNull(message)
                .replace("{price}", getPluginInstance().getEconomyHandler().format(shop, shop.getCurrencyType(), price)));
        getPluginInstance().runEventCommands("shop-buy-price", player);
    }

    private void runNotify(CommandSender commandSender) {
        if (!(commandSender instanceof Player)) {
            String message = getPluginInstance().getLangConfig().getString("must-be-player");
            if (message != null && !message.equalsIgnoreCase(""))
                commandSender.sendMessage(getPluginInstance().getManager().color(message));
            return;
        }

        final Player player = (Player) commandSender;
        if (!player.hasPermission("displayshops.notify")) {
            getPluginInstance().getManager().sendMessage(player, getPluginInstance().getLangConfig().getString("no-permission"));
            return;
        }

        final DataPack dataPack = getPluginInstance().getManager().getDataPack(player);
        dataPack.setTransactionNotify(!dataPack.isTransactionNotify());
        getPluginInstance().getManager().sendMessage(player, getPluginInstance().getLangConfig().getString("notify-toggle-"
                + (dataPack.isTransactionNotify() ? "on" : "off")));
    }

    private void runBlock(CommandSender commandSender) {
        if (!(commandSender instanceof Player)) {
            String message = getPluginInstance().getLangConfig().getString("must-be-player");
            if (message != null && !message.equalsIgnoreCase(""))
                commandSender.sendMessage(getPluginInstance().getManager().color(message));
            return;
        }

        final Player player = (Player) commandSender;
        if (!player.hasPermission("displayshops.block")) {
            getPluginInstance().getManager().sendMessage(player, getPluginInstance().getLangConfig().getString("no-permission"));
            return;
        }

        final ItemStack handItem = (Ref.isNewerThan(8) ||Ref.isMajorNewerThan(26)) ? player.getInventory().getItemInMainHand() : player.getInventory().getItemInHand();
        if (handItem == null || handItem.getType().name().contains("AIR")) {
            getPluginInstance().getManager().sendMessage(player, getPluginInstance().getLangConfig().getString("no-hand-item"));
            return;
        }

        File file = new File(getPluginInstance().getDataFolder(), "blocked-items.yml");
        FileConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        try {
            long id = 0;
            ConfigurationSection cs = yaml.getConfigurationSection("");
            if (cs != null) for (String key : cs.getKeys(false)) if (Long.parseLong(key) == id) id++;
            yaml.set(String.valueOf(id), getPluginInstance().toString(handItem));
            yaml.save(file);
        } catch (IOException e) {
            e.printStackTrace();
        }
        getPluginInstance().getManager().sendMessage(player, getPluginInstance().getLangConfig().getString("item-blocked"));
    }

    private void runUnBlock(CommandSender commandSender) {
        if (!(commandSender instanceof Player)) {
            String message = getPluginInstance().getLangConfig().getString("must-be-player");
            if (message != null && !message.equalsIgnoreCase(""))
                commandSender.sendMessage(getPluginInstance().getManager().color(message));
            return;
        }

        final Player player = (Player) commandSender;
        if (!player.hasPermission("displayshops.block")) {
            getPluginInstance().getManager().sendMessage(player, getPluginInstance().getLangConfig().getString("no-permission"));
            return;
        }

        final ItemStack handItem = (Ref.isNewerThan(8)|| Ref.isMajorNewerThan(26))
                ? player.getInventory().getItemInMainHand() : player.getInventory().getItemInHand();
        if (handItem == null || handItem.getType().name().contains("AIR")) {
            getPluginInstance().getManager().sendMessage(player, getPluginInstance().getLangConfig().getString("no-hand-item"));
            return;
        }

        final long foundId = getPluginInstance().getBlockedItemId(handItem);
        if (foundId < 0) {
            getPluginInstance().getManager().sendMessage(player, getPluginInstance().getLangConfig().getString("item-not-blocked"));
            return;
        }

        File file = new File(getPluginInstance().getDataFolder(), "blocked-items.yml");
        FileConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        try {
            yaml.set(String.valueOf(foundId), getPluginInstance().toString(handItem));
            yaml.save(file);
        } catch (IOException e) {
            e.printStackTrace();
        }
        getPluginInstance().getManager().sendMessage(player, getPluginInstance().getLangConfig().getString("item-unblocked"));
    }

    private void runAdvertise(CommandSender commandSender) {
        if (!(commandSender instanceof Player)) {
            String message = getPluginInstance().getLangConfig().getString("must-be-player");
            if (message != null && !message.equalsIgnoreCase(""))
                commandSender.sendMessage(getPluginInstance().getManager().color(message));
            return;
        }

        final Player player = (Player) commandSender;
        if (!player.hasPermission("displayshops.advertise")) {
            getPluginInstance().getManager().sendMessage(player, getPluginInstance().getLangConfig().getString("no-permission"));
            return;
        }

        final Block block = player.getTargetBlock(null, 10);
        final Shop shop = getPluginInstance().getManager().getShop(block.getLocation());
        if (shop == null) {
            String message = getPluginInstance().getLangConfig().getString("shop-invalid");
            if (message != null && !message.equalsIgnoreCase(""))
                getPluginInstance().getManager().sendMessage(player, message);
            return;
        }

        final DataPack dataPack = getPluginInstance().getManager().getDataPack(player);
        final int cooldown = dataPack.getCooldown(player, "advertise", getPluginInstance().getConfig().getInt("shop-advertise-cooldown"));
        if (cooldown > 0) {
            String message = getPluginInstance().getLangConfig().getString("cooldown");
            if (message != null && !message.equalsIgnoreCase(""))
                getPluginInstance().getManager().sendMessage(player, message.replace("{cooldown}", String.valueOf(cooldown)));
            return;
        }

        final double amount = getPluginInstance().getConfig().getDouble("shop-advertise-cost");
        if (amount > 0) {
            final EconomyCallEvent economyCallEvent = EconomyCallEvent.call(player, shop, EconomyCallType.ADVERTISE, amount);
            if (economyCallEvent.failed()) {
                dataPack.resetEditData();
                return;
            }
        }

        String message = getPluginInstance().getLangConfig().getString("shop-advertisement.message");
        if (message != null && !message.isEmpty()) {

            final String notApplicable = getPluginInstance().getLangConfig().getString("not-applicable");
            final boolean naNotEmpty = (notApplicable != null && !notApplicable.isEmpty());

            final double buyPrice = shop.getBuyPrice(shop.canDynamicPriceChange()),
                    sellPrice = shop.getSellPrice(shop.canDynamicPriceChange());

            final String buyReplacement = ((buyPrice < 0 && naNotEmpty) ? notApplicable : getPluginInstance().getEconomyHandler().format(shop, shop.getCurrencyType(), buyPrice)),
                    sellReplacement = ((sellPrice < 0 && naNotEmpty) ? notApplicable : getPluginInstance().getEconomyHandler().format(shop, shop.getCurrencyType(), sellPrice)),
                    itemName = getPluginInstance().getManager().getItemName(shop.getShopItem());
            message = message.replace("{buy}", buyReplacement).replace("{sell}", sellReplacement);
            message = getPluginInstance().getManager().color(message.replace("{player}", player.getName()).replace("{item}", itemName)
                    .replace("{buy}", buyReplacement).replace("{sell}", sellReplacement));

            HoverEvent hoverEvent = null;
            if (shop.getShopItem() != null) {
                final ItemTag itemTag = ItemTag.ofNbt(shop.getShopItem().getItemMeta() == null ? null : ((Ref.isNewerThan(17) || Ref.isMajorNewerThan(26))
                                                                                                         ? shop.getShopItem().getItemMeta().getAsString() : shop.getShopItem().getItemMeta().toString()));
                hoverEvent = new HoverEvent(HoverEvent.Action.SHOW_ITEM, new Item(shop.getShopItem().getType().getKey().toString(),
                        Math.min(shop.getShopItemAmount(), shop.getShopItem().getType().getMaxStackSize()), itemTag));
            }

            TextComponent component = null;
            if (message.contains("\\n")) {
                String[] args = message.split("\\\\n");
                for (int i = -1; ++i < args.length; ) {
                    final String line = args[i];
                    if (component == null) {
                        component = new TextComponent(TextComponent.fromLegacyText("\n" + line));
                        if (hoverEvent != null) component.setHoverEvent(hoverEvent);
                    } else {
                        TextComponent extra = new TextComponent(TextComponent.fromLegacyText("\n" + line));
                        if (hoverEvent != null) extra.setHoverEvent(hoverEvent);
                        component.addExtra(extra);
                    }
                }
            } else {
                component = new TextComponent(message);
                if (hoverEvent != null) component.setHoverEvent(hoverEvent);
            }

            final ClickEvent clickEvent = new ClickEvent(ClickEvent.Action.RUN_COMMAND, ("/displayshops visit " + shop.getShopId()));
            String visitClickable = getPluginInstance().getLangConfig().getString("shop-advertisement.visit-clickable");
            if (visitClickable != null && !visitClickable.isEmpty()) {
                visitClickable = getPluginInstance().getManager().color(visitClickable);
                TextComponent visitClickableComponent = null;
                if (visitClickable.contains("\\n")) {
                    String[] args = visitClickable.split("\\\\n");
                    for (int i = -1; ++i < args.length; ) {
                        final String line = args[i];
                        if (visitClickableComponent == null) {
                            visitClickableComponent = new TextComponent("\n" + line);
                            visitClickableComponent.setClickEvent(clickEvent);
                        } else {
                            TextComponent extra = new TextComponent(TextComponent.fromLegacyText("\n" + line));
                            extra.setClickEvent(clickEvent);
                            visitClickableComponent.addExtra(extra);
                        }
                    }
                } else {
                    visitClickableComponent = new TextComponent(visitClickable);
                    visitClickableComponent.setClickEvent(clickEvent);
                }

                if (component != null) component.addExtra(visitClickableComponent);
            } else if (component != null) component.setClickEvent(clickEvent);

            if (component != null) {
                final TextComponent finalComponent = component;
                getPluginInstance().getServer().getOnlinePlayers().parallelStream().forEach(p -> p.spigot().sendMessage(finalComponent));
            }
        }

        dataPack.updateCooldown("advertise");
    }

    private void runId(CommandSender commandSender) {
        if (!(commandSender instanceof Player)) {
            String message = getPluginInstance().getLangConfig().getString("must-be-player");
            if (message != null && !message.equalsIgnoreCase(""))
                commandSender.sendMessage(getPluginInstance().getManager().color(message));
            return;
        }

        final Player player = (Player) commandSender;
        if (!player.hasPermission("displayshops.id")) {
            getPluginInstance().getManager().sendMessage(player, getPluginInstance().getLangConfig().getString("no-permission"));
            return;
        }

        final Block block = player.getTargetBlock(null, 10);
        final Shop shop = getPluginInstance().getManager().getShop(block.getLocation());
        if (shop == null) {
            String message = getPluginInstance().getLangConfig().getString("shop-invalid");
            if (message != null && !message.equalsIgnoreCase(""))
                getPluginInstance().getManager().sendMessage(player, message);
            return;
        }

        String message = getPluginInstance().getLangConfig().getString("shop-id");
        if (message != null && !message.isEmpty()) {
            TextComponent textComponent = new TextComponent(getPluginInstance().getManager().color(message.replace("{id}", shop.getShopId().toString())));
            if (shop.getShopItem() != null) {

                final ItemTag itemTag = ItemTag.ofNbt(shop.getShopItem().getItemMeta() == null ? null :
                        ((Ref.isNewerThan(17)|| Ref.isMajorNewerThan(26)) ? shop.getShopItem().getItemMeta().getAsString()
                         : shop.getShopItem().getItemMeta().toString()));

                textComponent.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_ITEM, new Item(shop.getShopItem().getType().getKey().toString(),
                        Math.min(shop.getShopItemAmount(), shop.getShopItem().getType().getMaxStackSize()), itemTag)));
            }
            textComponent.setClickEvent(new ClickEvent(ClickEvent.Action.COPY_TO_CLIPBOARD, shop.getShopId().toString()));
            player.spigot().sendMessage(textComponent);
        }
    }

    private void runTime(CommandSender commandSender, String marketRegionId) {
        if (!(commandSender instanceof Player)) {
            String message = getPluginInstance().getLangConfig().getString("must-be-player");
            if (message != null && !message.equalsIgnoreCase(""))
                commandSender.sendMessage(getPluginInstance().getManager().color(message));
            return;
        }

        final Player player = (Player) commandSender;
        if (!player.hasPermission("displayshops.rent")) {
            getPluginInstance().getManager().sendMessage(player, getPluginInstance().getLangConfig().getString("no-permission"));
            return;
        }

        MarketRegion marketRegion = ((marketRegionId != null && !marketRegionId.isEmpty()) ? getPluginInstance().getManager().getMarketRegion(marketRegionId)
                : getPluginInstance().getManager().getMarketRegion(player.getLocation()));
        if (marketRegion == null) {
            getPluginInstance().getManager().sendMessage(player, Objects.requireNonNull(getPluginInstance().getLangConfig().getString("region-invalid"))
                    .replace("{id}", (marketRegionId != null ? marketRegionId : "<invalid-id>")));
            return;
        }

        if (marketRegion.getRenter() != null && marketRegion.timeUntilExpire() > 0) {
            if (!marketRegion.getRenter().toString().equals(player.getUniqueId().toString()) && !player.hasPermission("displayshops.rentadmin")) {
                getPluginInstance().getManager().sendMessage(player, Objects.requireNonNull(getPluginInstance().getLangConfig().getString("not-the-renter"))
                        .replace("{id}", marketRegion.getMarketId()));
                return;
            }

            double cost = getPluginInstance().getConfig().getDouble("rent-renew-cost");
            getPluginInstance().getManager().sendMessage(player, Objects.requireNonNull(getPluginInstance().getLangConfig().getString("rent-time"))
                    .replace("{cost}", getPluginInstance().getEconomyHandler().format(null, getPluginInstance().getEconomyHandler().getDefaultCurrency(), cost))
                    .replace("{id}", marketRegion.getMarketId()).replace("{duration}", marketRegion.formattedTimeUntilExpire()));
            return;
        }

        getPluginInstance().getManager().sendMessage(player, Objects.requireNonNull(getPluginInstance().getLangConfig().getString("region-rent-invalid"))
                .replace("{id}", marketRegion.getMarketId()));
    }

    private void runRent(CommandSender commandSender, String marketRegionId) {
        if (!(commandSender instanceof Player)) {
            String message = getPluginInstance().getLangConfig().getString("must-be-player");
            if (message != null && !message.equalsIgnoreCase(""))
                commandSender.sendMessage(getPluginInstance().getManager().color(message));
            return;
        }

        final Player player = (Player) commandSender;
        if (!player.hasPermission("displayshops.rent")) {
            getPluginInstance().getManager().sendMessage(player, getPluginInstance().getLangConfig().getString("no-permission"));
            return;
        }

        MarketRegion marketRegion = ((marketRegionId != null && !marketRegionId.isEmpty()) ? getPluginInstance().getManager().getMarketRegion(marketRegionId)
                : getPluginInstance().getManager().getMarketRegion(player.getLocation()));
        if (marketRegion == null) {
            getPluginInstance().getManager().sendMessage(player, Objects.requireNonNull(getPluginInstance().getLangConfig().getString("region-invalid"))
                    .replace("{id}", (marketRegionId != null ? marketRegionId : "<invalid-id>")));
            return;
        }

        if (marketRegion.getRenter() != null && marketRegion.timeUntilExpire() > 0) {
            if (!marketRegion.getRenter().toString().equals(player.getUniqueId().toString()) && !player.hasPermission("displayshops.rentadmin")) {
                getPluginInstance().getManager().sendMessage(player, Objects.requireNonNull(getPluginInstance().getLangConfig().getString("not-the-renter"))
                        .replace("{id}", marketRegion.getMarketId()));
                return;
            }

            if (marketRegion.extendRent(player)) {
                double cost = getPluginInstance().getConfig().getDouble("rent-renew-cost");
                getPluginInstance().getManager().sendMessage(player, Objects.requireNonNull(getPluginInstance().getLangConfig().getString("rent-extended"))
                        .replace("{cost}", getPluginInstance().getEconomyHandler().format(null, getPluginInstance().getEconomyHandler().getDefaultCurrency(), cost))
                        .replace("{id}", marketRegion.getMarketId()).replace("{duration}", marketRegion.formattedTimeUntilExpire()));
            }
            return;
        }

        if (getPluginInstance().getManager().exceededMarketRegionLimit(player)) {
            getPluginInstance().getManager().sendMessage(player, getPluginInstance().getLangConfig().getString("region-limit-exceeded"));
            return;
        }

        if (marketRegion.rent(player)) {
            double cost = getPluginInstance().getConfig().getDouble("rent-cost");
            getPluginInstance().getManager().sendMessage(player, Objects.requireNonNull(getPluginInstance().getLangConfig().getString("rented"))
                    .replace("{cost}", getPluginInstance().getEconomyHandler().format(null, getPluginInstance().getEconomyHandler().getDefaultCurrency(), cost))
                    .replace("{id}", marketRegion.getMarketId()).replace("{duration}", marketRegion.formattedTimeUntilExpire()));
        }
    }

    private void runReset(CommandSender commandSender, String marketRegionId) {
        if ((marketRegionId == null || marketRegionId.isEmpty()) && !(commandSender instanceof Player)) {
            String message = getPluginInstance().getLangConfig().getString("must-be-player");
            if (message != null && !message.equalsIgnoreCase(""))
                commandSender.sendMessage(getPluginInstance().getManager().color(message));
            return;
        }

        if (!commandSender.hasPermission("displayshops.reset")) {
            String message = getPluginInstance().getLangConfig().getString("no-permission");
            if (message != null && !message.equalsIgnoreCase(""))
                commandSender.sendMessage(getPluginInstance().getManager().color(message));
            return;
        }

        if (marketRegionId == null || marketRegionId.isEmpty()) {
            final Player player = (Player) commandSender;
            final MarketRegion marketRegion = getPluginInstance().getManager().getMarketRegion(player.getLocation());
            if (marketRegion == null) {
                getPluginInstance().getManager().sendMessage(player, getPluginInstance().getLangConfig().getString("region-location-fail"));
                return;
            }

            marketRegion.reset();

            final String message = getPluginInstance().getLangConfig().getString("market-region-reset");
            if (message != null)
                getPluginInstance().getManager().sendMessage(player, message.replace("{id}", marketRegion.getMarketId()));
            return;
        }

        final MarketRegion marketRegion = getPluginInstance().getManager().getMarketRegion(marketRegionId);
        marketRegion.reset();
        String message = getPluginInstance().getLangConfig().getString("market-region-reset");
        if (message != null && !message.equalsIgnoreCase(""))
            commandSender.sendMessage(getPluginInstance().getManager().color(message.replace("{id}", marketRegion.getMarketId())));
    }

    private void runClear(CommandSender commandSender, String worldName) {
        if (!commandSender.hasPermission("displayshops.clear")) {
            String message = getPluginInstance().getLangConfig().getString("no-permission");
            if (message != null && !message.equalsIgnoreCase(""))
                if (commandSender instanceof Player)
                    getPluginInstance().getManager().sendMessage((Player) commandSender, message);
                else commandSender.sendMessage(getPluginInstance().getManager().color(message));
            return;
        }

        World world = getPluginInstance().getServer().getWorld(worldName);
        if (world == null) {
            String message = getPluginInstance().getLangConfig().getString("world-invalid");
            if (message != null && !message.equalsIgnoreCase(""))
                if (commandSender instanceof Player)
                    getPluginInstance().getManager().sendMessage((Player) commandSender, message.replace("{world}", worldName));
                else
                    commandSender.sendMessage(getPluginInstance().getManager().color(message.replace("{world}", worldName)));
            return;
        }


        getPluginInstance().getServer().getScheduler().runTaskAsynchronously(getPluginInstance(), () -> {
            List<Shop> shopList = new ArrayList<>(getPluginInstance().getManager().getShopMap().values());
            for (Shop shop : shopList) {
                if (shop == null || shop.getBaseLocation() == null || !shop.getBaseLocation().getWorldName().equalsIgnoreCase(worldName))
                    continue;
                try {
                    shop.delete(false);
                    for (Player player : getPluginInstance().getServer().getOnlinePlayers()) {
                        if (player == null || !player.isOnline() || player.getLocation().getWorld() == null
                                || !player.getLocation().getWorld().getName().equalsIgnoreCase(worldName))
                            continue;
                        shop.kill(player);
                        if (getPluginInstance().getShopMemory().containsKey(player.getUniqueId())) {
                            final UUID shopId = getPluginInstance().getShopMemory().get(player.getUniqueId());
                            if (shopId.toString().equals(shop.getShopId().toString()))
                                getPluginInstance().getShopMemory().remove(player.getUniqueId());
                        }
                    }
                } catch (Exception ignored) {
                }
            }
        });
        String message = getPluginInstance().getLangConfig().getString("world-cleared");
        if (message != null && !message.equalsIgnoreCase(""))
            if (commandSender instanceof Player)
                getPluginInstance().getManager().sendMessage((Player) commandSender, message.replace("{world}", world.getName()));
            else
                commandSender.sendMessage(getPluginInstance().getManager().color(message.replace("{world}", world.getName())));
    }

    private void runAppearanceAccess(CommandSender commandSender, boolean isUnlock, String[] args) {
        boolean isPlayer = (commandSender instanceof Player);
        if (!commandSender.hasPermission("displayshops.bbmaccess") && !commandSender.hasPermission("displayshops.appearance")) {
            String message = getPluginInstance().getLangConfig().getString("no-permission");
            if (message != null && !message.equalsIgnoreCase(""))
                if (isPlayer) getPluginInstance().getManager().sendMessage((Player) commandSender, message);
                else commandSender.sendMessage(getPluginInstance().getManager().color(message));
            return;
        }

        final Player player = getPluginInstance().getServer().getPlayer(args[1]);
        if (player == null) {
            String message = getPluginInstance().getLangConfig().getString("player-invalid");
            if (message != null && !message.equalsIgnoreCase(""))
                if (isPlayer) getPluginInstance().getManager().sendMessage((Player) commandSender, message);
                else commandSender.sendMessage(getPluginInstance().getManager().color(message));
            return;
        }

        if (args.length == 2) {
            final DataPack dataPack = getPluginInstance().getManager().getDataPack(player);
            dataPack.updateUnlocks(isUnlock);

            String message = getPluginInstance().getLangConfig().getString("appearance-all-admin");
            if (message != null && !message.equalsIgnoreCase("")) {
                String status = getPluginInstance().getLangConfig().getString(isUnlock ? "unlocked" : "locked");
                if (status != null) message = message.replace("{status}", status);
                if (isPlayer)
                    getPluginInstance().getManager().sendMessage((Player) commandSender, message.replace("{player}", player.getName()));
                else
                    commandSender.sendMessage(getPluginInstance().getManager().color(message.replace("{player}", player.getName())));
            }
        } else {
            Appearance appearance = Appearance.getAppearance(args[2]);
            if (appearance == null) {
                String message = getPluginInstance().getLangConfig().getString("invalid-appearance");
                if (message != null && !message.equalsIgnoreCase("")) {
                    message = message.replace("{appearances}", new ArrayList<String>() {{
                        Appearance.getAppearances().parallelStream().forEach(appearance -> add(appearance.getId()));
                    }}.toString());
                    if (isPlayer) getPluginInstance().getManager().sendMessage((Player) commandSender, message);
                    else commandSender.sendMessage(getPluginInstance().getManager().color(message));
                }
                return;
            }

            final DataPack dataPack = getPluginInstance().getManager().getDataPack(player);
            dataPack.updateAppearance(appearance.getId(), !dataPack.hasUnlockedAppearance(player, appearance.getId()));

            String message = getPluginInstance().getLangConfig().getString("appearance-access-admin");
            if (message != null && !message.equalsIgnoreCase("")) {
                String status = getPluginInstance().getLangConfig().getString(isUnlock ? "unlocked" : "locked");
                if (status != null) message = message.replace("{status}", status);
                if (isPlayer)
                    getPluginInstance().getManager().sendMessage((Player) commandSender, message.replace("{player}", player.getName()));
                else
                    commandSender.sendMessage(getPluginInstance().getManager().color(message.replace("{player}", player.getName())));
            }
        }
    }

    private void runAddCommand(CommandSender commandSender, String[] args) {
        if (!(commandSender instanceof Player)) {
            String message = getPluginInstance().getLangConfig().getString("must-be-player");
            if (message != null && !message.equalsIgnoreCase(""))
                commandSender.sendMessage(getPluginInstance().getManager().color(message));
            return;
        }

        Player player = (Player) commandSender;
        if (!player.hasPermission("displayshops.commands")) {
            String message = getPluginInstance().getLangConfig().getString("no-permission");
            if (message != null && !message.equalsIgnoreCase(""))
                getPluginInstance().getManager().sendMessage(player, message);
            return;
        }

        Block block = player.getTargetBlock(null, 10);
        Shop shop = getPluginInstance().getManager().getShop(block.getLocation());
        if (shop == null) {
            String message = getPluginInstance().getLangConfig().getString("shop-invalid");
            if (message != null && !message.equalsIgnoreCase(""))
                getPluginInstance().getManager().sendMessage(player, message);
            return;
        }

        if (!shop.isAdminShop()) {
            String message = getPluginInstance().getLangConfig().getString("shop-not-admin");
            if (message != null && !message.equalsIgnoreCase(""))
                getPluginInstance().getManager().sendMessage(player, message);
            return;
        }

        StringBuilder enteredCommand = new StringBuilder(args[1]);
        for (int i = 1; ++i < args.length; ) enteredCommand.append(" ").append(args[i]);

        // call edit event
        ShopEditEvent shopEditEvent = new ShopEditEvent(player, shop, EditType.COMMAND_ADD, enteredCommand.toString());
        getPluginInstance().getServer().getPluginManager().callEvent(shopEditEvent);
        if (shopEditEvent.isCancelled()) return;

        shop.getCommands().add(enteredCommand.toString());

        if (!shop.getCommands().isEmpty() && shop.getSellPrice(false) >= 0) {
            shop.setSellPrice(-1);
            getPluginInstance().getInSightTask().refreshShop(shop);
        }

        String fixedCommand = enteredCommand.toString().replaceAll("(?i):PLAYER", "").replaceAll("(?i):CONSOLE", "");
        getPluginInstance().getManager().sendMessage(player, Objects.requireNonNull(getPluginInstance().getLangConfig().getString("command-added"))
                .replace("{command}", fixedCommand));
    }

    private void runRemoveCommand(CommandSender commandSender, String[] args) {
        if (!(commandSender instanceof Player)) {
            String message = getPluginInstance().getLangConfig().getString("must-be-player");
            if (message != null && !message.equalsIgnoreCase(""))
                commandSender.sendMessage(getPluginInstance().getManager().color(message));
            return;
        }

        Player player = (Player) commandSender;
        if (!player.hasPermission("displayshops.commands")) {
            String message = getPluginInstance().getLangConfig().getString("no-permission");
            if (message != null && !message.equalsIgnoreCase(""))
                getPluginInstance().getManager().sendMessage(player, message);
            return;
        }

        Block block = player.getTargetBlock(null, 10);
        Shop shop = getPluginInstance().getManager().getShop(block.getLocation());
        if (shop == null) {
            String message = getPluginInstance().getLangConfig().getString("shop-invalid");
            if (message != null && !message.equalsIgnoreCase(""))
                getPluginInstance().getManager().sendMessage(player, message);
            return;
        }

        String message = getPluginInstance().getLangConfig().getString("invalid-command-index");
        if (getPluginInstance().getManager().isNotNumeric(args[1])) {
            if (message != null && !message.equalsIgnoreCase(""))
                getPluginInstance().getManager().sendMessage(player, message);
            return;
        }

        int index = Integer.parseInt(args[1]);
        if (index < 0 || index > (shop.getCommands().size() - 1)) {
            if (message != null && !message.equalsIgnoreCase(""))
                getPluginInstance().getManager().sendMessage(player, message);
            return;
        }

        // call edit event
        ShopEditEvent shopEditEvent = new ShopEditEvent(player, shop, EditType.COMMAND_REMOVE, index);
        getPluginInstance().getServer().getPluginManager().callEvent(shopEditEvent);
        if (shopEditEvent.isCancelled()) return;

        shop.getCommands().remove(index);
        if (!shop.getCommands().isEmpty() && shop.getSellPrice(false) >= 0) {
            shop.setSellPrice(-1);
            getPluginInstance().getInSightTask().refreshShop(shop);
        }

        String completeMessage = getPluginInstance().getLangConfig().getString("command-removed");
        if (completeMessage != null && !completeMessage.equalsIgnoreCase(""))
            getPluginInstance().getManager().sendMessage(player, completeMessage.replace("{index}",
                    getPluginInstance().getManager().formatNumber(index, false)));
    }

    private void runCommands(CommandSender commandSender) {
        if (!(commandSender instanceof Player)) {
            String message = getPluginInstance().getLangConfig().getString("must-be-player");
            if (message != null && !message.equalsIgnoreCase(""))
                commandSender.sendMessage(getPluginInstance().getManager().color(message));
            return;
        }

        Player player = (Player) commandSender;
        if (!player.hasPermission("displayshops.commands")) {
            String message = getPluginInstance().getLangConfig().getString("no-permission");
            if (message != null && !message.equalsIgnoreCase(""))
                getPluginInstance().getManager().sendMessage(player, message);
            return;
        }

        Block block = player.getTargetBlock(null, 10);
        Shop shop = getPluginInstance().getManager().getShop(block.getLocation());
        if (shop == null) {
            String message = getPluginInstance().getLangConfig().getString("shop-invalid");
            if (message != null && !message.equalsIgnoreCase(""))
                getPluginInstance().getManager().sendMessage(player, message);
            return;
        }

        StringBuilder sb = new StringBuilder();
        for (int i = -1; ++i < shop.getCommands().size(); ) {
            String commandLine = shop.getCommands().get(i);
            sb.append("(").append(i).append(") ").append(commandLine);
            if (i != (shop.getCommands().size() - 1)) sb.append(", ");
        }

        String message = getPluginInstance().getLangConfig().getString("shop-commands");
        if (message != null && !message.equalsIgnoreCase(""))
            getPluginInstance().getManager().sendMessage(player, message.replace("{commands}", sb.toString()));
    }

    private void runCommandMode(CommandSender commandSender) {
        if (!(commandSender instanceof Player)) {
            String message = getPluginInstance().getLangConfig().getString("must-be-player");
            if (message != null && !message.equalsIgnoreCase(""))
                commandSender.sendMessage(getPluginInstance().getManager().color(message));
            return;
        }

        Player player = (Player) commandSender;
        if (!player.hasPermission("displayshops.commands")) {
            String message = getPluginInstance().getLangConfig().getString("no-permission");
            if (message != null && !message.equalsIgnoreCase(""))
                getPluginInstance().getManager().sendMessage(player, message);
            return;
        }

        Block block = player.getTargetBlock(null, 10);
        Shop shop = getPluginInstance().getManager().getShop(block.getLocation());
        if (shop == null) {
            String message = getPluginInstance().getLangConfig().getString("shop-invalid");
            if (message != null && !message.equalsIgnoreCase(""))
                getPluginInstance().getManager().sendMessage(player, message);
            return;
        }

        shop.setCommandOnlyMode(!shop.isCommandOnlyMode());
        String message = getPluginInstance().getLangConfig().getString("command-only");
        if (message != null && !message.equalsIgnoreCase(""))
            getPluginInstance().getManager().sendMessage(player, message.replace("{status}", shop.isCommandOnlyMode() ? "True" : "False"));
    }

    private void runCDB(CommandSender commandSender) {

        if (!commandSender.hasPermission("displayshops.cdb")) {
            String message = getPluginInstance().getLangConfig().getString("no-permission");
            if (message != null && !message.equalsIgnoreCase(""))
                commandSender.sendMessage(getPluginInstance().getManager().color(message));
            return;
        }

        // cancel tasks
        if (getPluginInstance().getManagementTask() != null) getPluginInstance().getManagementTask().cancel();
        if (getPluginInstance().getInSightTask() != null) getPluginInstance().getInSightTask().cancel();
        if (getPluginInstance().getCleanupTask() != null) getPluginInstance().getCleanupTask().cancel();

        // reload configs
        getPluginInstance().reloadConfigs();
        getPluginInstance().loadMenus();

        // reload global variables
        getPluginInstance().getListeners().creationItem = getPluginInstance().getManager().buildShopCreationItem(null, 1);
        getPluginInstance().getManager().defaultCurrencyItem = getPluginInstance().getManager().buildShopCurrencyItem(1);

        // handle market regions
        getPluginInstance().getServer().getScheduler().runTaskAsynchronously(getPluginInstance(), () -> {
            getPluginInstance().getManager().saveMarketRegions();
            getPluginInstance().getManager().getMarketRegions().clear();
            getPluginInstance().getManager().loadMarketRegions(true);
            getPluginInstance().getManager().cleanUpDataPacks();

            List<Shop> shopList = new ArrayList<>(getPluginInstance().getManager().getShopMap().values());
            for (Shop shop : shopList) {
                if (getPluginInstance().getManager().getShopMap().containsKey(shop.getBaseLocation().toString())) {
                    for (Player p : getPluginInstance().getServer().getOnlinePlayers())
                        shop.kill(p);
                    shop.save(false);
                }
            }

            getPluginInstance().getManager().getShopMap().clear();
            getPluginInstance().getManager().loadShops(true, true);
        });

        // restart tasks
        getPluginInstance().setupTasks();

        // reset recipe
        getPluginInstance().setupRecipe();

        String message = getPluginInstance().getLangConfig().getString("clean-database");
        if (message != null && !message.equalsIgnoreCase(""))
            commandSender.sendMessage(getPluginInstance().getManager().color(message));
    }

    private void runCleanUp(CommandSender commandSender) {
        if (!commandSender.hasPermission("displayshops.cleanup")) {
            String message = getPluginInstance().getLangConfig().getString("no-permission");
            if (message != null && !message.equalsIgnoreCase(""))
                commandSender.sendMessage(getPluginInstance().getManager().color(message));
            return;
        }

        if (DisplayShops.getPluginInstance().getDisplayManager() != null) {
            DisplayShops.getPluginInstance().getInSightTask().setPaused(true);

            DisplayShops.ClearAllEntities();

            for (Map.Entry<UUID, Display> entry : new ArrayList<>(DisplayShops.getPluginInstance().getDisplayManager().getShopDisplays().entrySet())) {
                entry.getValue().delete();
            }

            DisplayShops.getPluginInstance().getInSightTask().setPaused(false);
        }

        final int cleanInactiveTime = getPluginInstance().getConfig().getInt("clean-inactive-duration");

        int cleanCount = 0;
        final List<Shop> shopList = new ArrayList<>(getPluginInstance().getManager().getShopMap().values());
        for (Shop shop : shopList) {
            if (shop == null || shop.getBaseLocation() == null) {
                if (shop != null) {
                    shop.killAll();
                    shop.delete(true);
                    shop.unRegister();
                    cleanCount++;
                }
                continue;
            }

            if (cleanInactiveTime >= 0 && shop.getOwnerUniqueId() != null) {
                OfflinePlayer offlinePlayer = getPluginInstance().getServer().getOfflinePlayer(shop.getOwnerUniqueId());
                final long lastOnline = (((System.currentTimeMillis() - offlinePlayer.getLastPlayed()) / 1000) % 60);

                if (lastOnline >= cleanInactiveTime) {
                    shop.killAll();
                    shop.delete(true);
                    shop.unRegister();
                    cleanCount++;
                    continue;
                }
            }

            if (shop.getBaseLocation() != null) {
                World world = getPluginInstance().getServer().getWorld(shop.getBaseLocation().getWorldName());
                if (world == null) {
                    getPluginInstance().log(Level.WARNING, "The shop '" + shop.getShopId() + "' has a world named '"
                            + shop.getBaseLocation().getWorldName() + "' which was unable to be found.");
                    continue;
                }

                Location location = shop.getBaseLocation().asBukkitLocation();
                if (location != null) {
                    if (location.getBlock().getType().name().contains("AIR")) {
                        shop.killAll();
                        shop.delete(true);
                        shop.unRegister();
                        cleanCount++;
                        continue;
                    }

                    if (!shop.isAdminShop() && shop.getStock() < 0) shop.setStock(0);
                }
            } else {
                shop.unRegister();
                cleanCount++;
                getPluginInstance().log(Level.WARNING, "The shop '" + shop.getShopId() + "' had no base location and was therefore un-registered.");
            }
        }

        String message = getPluginInstance().getLangConfig().getString("cleanup-complete");
        if (message != null && !message.equalsIgnoreCase(""))
            commandSender.sendMessage(getPluginInstance().getManager().color(message
                    .replace("{count}", getPluginInstance().getManager().formatNumber(cleanCount, false))));
    }


    private void runInfo(CommandSender commandSender) {
        if (!commandSender.hasPermission("displayshops.info")) {
            String message = getPluginInstance().getLangConfig().getString("no-permission");
            if (message != null && !message.equalsIgnoreCase(""))
                commandSender.sendMessage(getPluginInstance().getManager().color(message));
            return;
        }

        commandSender.sendMessage(getPluginInstance().getManager().color("\n&e<&m------------&r&e[ &bDisplayShops &e]&m------------&r&e>\n" +
                "&7Current Plugin Version: " + (getPluginInstance().getDescription().getVersion().toLowerCase().contains("build") ? "&c" : "&a")
                + getPluginInstance().getDescription().getVersion() + "\n" +
                "&7Latest Release Plugin Version: " + (getPluginInstance().getDescription().getVersion().toLowerCase().contains("snapshot") ? "&1" : "&a")
                + getPluginInstance().getDescription().getVersion()/*getPluginInstance().getLatestVersion().getKey()*/ + "\n"
                + "&7Author(s): &b" + String.join(", ", getPluginInstance().getDescription().getAuthors()) + "\n" +
                "&e<&m-------------------------------------&r&e>\n"));
    }

    private void runInfoAdventure(CommandSender commandSender) {
        if (!commandSender.hasPermission("displayshops.info")) {
            String message = getPluginInstance().getLangConfig().getString("no-permission");
            if (message != null && !message.equalsIgnoreCase(""))
                commandSender.sendMessage(getPluginInstance().getManager().color(message));
            return;
        }
        AdventureCommand.runInfoAdventure(commandSender);
    }

    private void runCreateMarket(CommandSender commandSender, String[] args) {
        if (!(commandSender instanceof Player)) {
            String message = getPluginInstance().getLangConfig().getString("must-be-player");
            if (message != null && !message.equalsIgnoreCase(""))
                commandSender.sendMessage(getPluginInstance().getManager().color(message));
            return;
        }

        Player player = (Player) commandSender;
        if (!player.hasPermission("displayshops.cmr")) {
            String message = getPluginInstance().getLangConfig().getString("no-permission");
            if (message != null && !message.equalsIgnoreCase(""))
                getPluginInstance().getManager().sendMessage(player, message);
            return;
        }

        if (getPluginInstance().getManager().doesMarketRegionExist(args[1])) {
            String message = getPluginInstance().getLangConfig().getString("region-exists");
            if (message != null && !message.equalsIgnoreCase(""))
                getPluginInstance().getManager().sendMessage(player, message);
            return;
        }

        final DataPack dataPack = getPluginInstance().getManager().getDataPack(player);
        dataPack.setInSelectionMode(false);
        if (dataPack.getSelectedRegion() == null) {
            String message = getPluginInstance().getLangConfig().getString("no-selection");
            if (message != null && !message.equalsIgnoreCase(""))
                getPluginInstance().getManager().sendMessage(player, message);
            return;
        }

        Region regionSelection = dataPack.getSelectedRegion();
        MarketRegion marketRegion = new MRegion(getPluginInstance(), args[1], regionSelection);
        getPluginInstance().getManager().getMarketRegions().add(marketRegion);
        dataPack.setSelectedRegion(null);

        String message = getPluginInstance().getLangConfig().getString("market-region-created");
        if (message != null && !message.equalsIgnoreCase(""))
            getPluginInstance().getManager().sendMessage(player, message.replace("{id}", marketRegion.getMarketId()));
    }

    private void runDeleteMarket(CommandSender commandSender, String[] args) {
        if (!(commandSender instanceof Player)) {
            String message = getPluginInstance().getLangConfig().getString("must-be-player");
            if (message != null && !message.equalsIgnoreCase(""))
                commandSender.sendMessage(getPluginInstance().getManager().color(message));
            return;
        }

        Player player = (Player) commandSender;
        if (!player.hasPermission("displayshops.dmr")) {
            String message = getPluginInstance().getLangConfig().getString("no-permission");
            if (message != null && !message.equalsIgnoreCase(""))
                getPluginInstance().getManager().sendMessage(player, message);
            return;
        }

        MarketRegion marketRegion = getPluginInstance().getManager().getMarketRegion(args[1]);
        if (marketRegion == null) {
            String message = getPluginInstance().getLangConfig().getString("region-invalid");
            if (message != null && !message.equalsIgnoreCase(""))
                getPluginInstance().getManager().sendMessage(player, message);
            return;
        }

        getPluginInstance().getManager().getMarketRegions().remove(marketRegion);
        marketRegion.delete(true);
        String message = getPluginInstance().getLangConfig().getString("market-region-deleted");
        if (message != null && !message.equalsIgnoreCase(""))
            getPluginInstance().getManager().sendMessage(player, message.replace("{id}", marketRegion.getMarketId()));
    }

    private void runMarketList(CommandSender commandSender) {
        if (!commandSender.hasPermission("displayshops.mrl")) {
            String message = getPluginInstance().getLangConfig().getString("no-permission");
            if (message != null && !message.equalsIgnoreCase(""))
                commandSender.sendMessage(getPluginInstance().getManager().color(message));
            return;
        }

        List<String> regionIds = new ArrayList<>();
        for (MarketRegion marketRegion : getPluginInstance().getManager().getMarketRegions())
            regionIds.add(marketRegion.getMarketId());

        String message = getPluginInstance().getLangConfig().getString("market-region-list");
        if (message != null && !message.equalsIgnoreCase(""))
            commandSender.sendMessage(getPluginInstance().getManager()
                    .color(message.replace("{list}", regionIds.toString())));
    }

    private void runCost(CommandSender commandSender, String region, String cost) {
        if (!(commandSender instanceof Player)) {
            String message = getPluginInstance().getLangConfig().getString("must-be-player");
            if (message != null && !message.equalsIgnoreCase(""))
                commandSender.sendMessage(getPluginInstance().getManager().color(message));
            return;
        }

        Player player = (Player) commandSender;
        if (!player.hasPermission("displayshops.cost")) {
            String message = getPluginInstance().getLangConfig().getString("no-permission");
            if (message != null && !message.equalsIgnoreCase(""))
                getPluginInstance().getManager().sendMessage(player, message);
            return;
        }

        MarketRegion marketRegion = ((region != null && !region.isEmpty()) ? getPluginInstance().getManager().getMarketRegion(region) :
                getPluginInstance().getManager().getMarketRegion(player.getLocation()));
        if (marketRegion == null) {
            String message = getPluginInstance().getLangConfig().getString("region-invalid");
            if (message != null && !message.equalsIgnoreCase(""))
                getPluginInstance().getManager().sendMessage(player, message);
            return;
        }

        if (getPluginInstance().getManager().isNotNumeric(cost)) {
            String message = getPluginInstance().getLangConfig().getString("invalid-amount");
            if (message != null && !message.equalsIgnoreCase(""))
                getPluginInstance().getManager().sendMessage(player, message);
            return;
        }

        marketRegion.setCost(Double.parseDouble(cost));
        marketRegion.save(true);

        String message = getPluginInstance().getLangConfig().getString("market-region-cost");
        if (message != null && !message.equalsIgnoreCase(""))
            getPluginInstance().getManager().sendMessage(player, message
                    .replace("{id}", marketRegion.getMarketId())
                    .replace("{cost}", String.valueOf(marketRegion.getCost())));
    }

    private void runRenewCost(CommandSender commandSender, String region, String cost) {
        if (!(commandSender instanceof Player)) {
            String message = getPluginInstance().getLangConfig().getString("must-be-player");
            if (message != null && !message.equalsIgnoreCase(""))
                commandSender.sendMessage(getPluginInstance().getManager().color(message));
            return;
        }

        Player player = (Player) commandSender;
        if (!player.hasPermission("displayshops.rcost")) {
            String message = getPluginInstance().getLangConfig().getString("no-permission");
            if (message != null && !message.equalsIgnoreCase(""))
                getPluginInstance().getManager().sendMessage(player, message);
            return;
        }

        MarketRegion marketRegion = ((region != null && !region.isEmpty()) ? getPluginInstance().getManager().getMarketRegion(region)
                : getPluginInstance().getManager().getMarketRegion(player.getLocation()));
        if (marketRegion == null) {
            String message = getPluginInstance().getLangConfig().getString("region-invalid");
            if (message != null && !message.equalsIgnoreCase(""))
                getPluginInstance().getManager().sendMessage(player, message);
            return;
        }

        if (getPluginInstance().getManager().isNotNumeric(cost)) {
            String message = getPluginInstance().getLangConfig().getString("invalid-amount");
            if (message != null && !message.equalsIgnoreCase(""))
                getPluginInstance().getManager().sendMessage(player, message);
            return;
        }

        marketRegion.setRenewCost(Double.parseDouble(cost));
        marketRegion.save(true);

        String message = getPluginInstance().getLangConfig().getString("market-region-rcost");
        if (message != null && !message.equalsIgnoreCase(""))
            getPluginInstance().getManager().sendMessage(player, message
                    .replace("{id}", marketRegion.getMarketId())
                    .replace("{cost}", getPluginInstance().getEconomyHandler().format(null, "", marketRegion.getRenewCost())));
    }

    private void runSelectionMode(CommandSender commandSender) {
        if (!(commandSender instanceof Player)) {
            String message = getPluginInstance().getLangConfig().getString("must-be-player");
            if (message != null && !message.equalsIgnoreCase(""))
                commandSender.sendMessage(getPluginInstance().getManager().color(message));
            return;
        }

        Player player = (Player) commandSender;
        if (!player.hasPermission("displayshops.sm")) {
            String message = getPluginInstance().getLangConfig().getString("no-permission");
            if (message != null && !message.equalsIgnoreCase(""))
                getPluginInstance().getManager().sendMessage(player, message);
            return;
        }

        final DataPack dataPack = getPluginInstance().getManager().getDataPack(player);
        dataPack.setInSelectionMode(!dataPack.isInSelectionMode());
        dataPack.setSelectedRegion(null);
        String message = getPluginInstance().getLangConfig().getString("selection-mode-" + (dataPack.isInSelectionMode() ? "enabled" : "disabled"));
        if (message != null && !message.equalsIgnoreCase(""))
            getPluginInstance().getManager().sendMessage(player, message);
    }

    private void runAdmin(CommandSender commandSender) {
        if (!(commandSender instanceof Player)) {
            String message = getPluginInstance().getLangConfig().getString("must-be-player");
            if (message != null && !message.equalsIgnoreCase(""))
                commandSender.sendMessage(getPluginInstance().getManager().color(message));
            return;
        }

        Player player = (Player) commandSender;
        if (!player.hasPermission("displayshops.admin")) {
            String message = getPluginInstance().getLangConfig().getString("no-permission");
            if (message != null && !message.equalsIgnoreCase(""))
                commandSender.sendMessage(getPluginInstance().getManager().color(message));
            return;
        }

        Block block = player.getTargetBlock(null, 10);
        Shop shop = getPluginInstance().getManager().getShop(block.getLocation());
        if (shop == null) {
            String message = getPluginInstance().getLangConfig().getString("shop-invalid");
            if (message != null && !message.equalsIgnoreCase(""))
                getPluginInstance().getManager().sendMessage(player, message);
            return;
        }

        if (shop.isAdminShop()) {
            shop.setOwnerUniqueId(player.getUniqueId());
            shop.setStock(shop.getShopItemAmount());
            getPluginInstance().getInSightTask().refreshShop(shop);

            String message = getPluginInstance().getLangConfig().getString("shop-admin-revert");
            if (message != null && !message.equalsIgnoreCase(""))
                getPluginInstance().getManager().sendMessage(player, message);
            return;
        }

        shop.makeAdminShop();
        getPluginInstance().getInSightTask().refreshShop(shop);

        String message = getPluginInstance().getLangConfig().getString("shop-admin");
        if (message != null && !message.equalsIgnoreCase(""))
            getPluginInstance().getManager().sendMessage(player, message);
    }

    private void runReload(CommandSender commandSender) {
        if (!commandSender.hasPermission("displayshops.reload")) {
            String message = getPluginInstance().getLangConfig().getString("no-permission");
            if (message != null && !message.equalsIgnoreCase(""))
                if (commandSender instanceof Player)
                    getPluginInstance().getManager().sendMessage((Player) commandSender, message);
                else commandSender.sendMessage(getPluginInstance().getManager().color(message));
            return;
        }

        for (UUID uuid : Listeners.openClaimMenu.keySet()) {
            Player p = Bukkit.getPlayer(uuid);
            if (p != null)
                p.closeInventory();
        }
        Listeners.openClaimMenu.clear();

        // cancel tasks and reload configs
        getPluginInstance().cancelTasks();
        getPluginInstance().reloadConfigs();

        // clear displays
        if (getPluginInstance().getDisplayManager() != null) {
            DisplayShops.ClearAllEntities();

            for (Map.Entry<UUID, Display> entry : new ArrayList<>(DisplayShops.getPluginInstance().getDisplayManager().getShopDisplays().entrySet())) {
                entry.getValue().delete();
            }
        }

        // reset EconomyHandler
        getPluginInstance().getEconomyHandler().reset();

        // reload menus
        getPluginInstance().getMenuMap().clear();
        getPluginInstance().loadMenus();

        // reload global variables
        getPluginInstance().getListeners().creationItem = getPluginInstance().getManager().buildShopCreationItem(null, 1);
        getPluginInstance().getMenuListener().updateChangeItem();

        Appearance.getAppearances().clear();
        getPluginInstance().getServer().getScheduler().runTaskAsynchronously(getPluginInstance(), () -> {
            getPluginInstance().getManager().getShopMap().forEach((key, value) -> value.killAll());
            DAppearance.loadAppearances();
        });

        // restart tasks
        getPluginInstance().setupTasks();

        String message = getPluginInstance().getLangConfig().getString("reload");
        if (message != null && !message.equalsIgnoreCase(""))
            commandSender.sendMessage(getPluginInstance().getManager().color(message));
    }

    private void runBuy(CommandSender commandSender, String amountValue) {
        if (!(commandSender instanceof Player)) {
            String message = getPluginInstance().getLangConfig().getString("muse-be-player");
            if (message != null && !message.equalsIgnoreCase(""))
                commandSender.sendMessage(getPluginInstance().getManager().color(message));
            return;
        }

        Player player = (Player) commandSender;
        if (!player.hasPermission("displayshops.buy")) {
            String message = getPluginInstance().getLangConfig().getString("no-permission");
            if (message != null && !message.equalsIgnoreCase(""))
                getPluginInstance().getManager().sendMessage(player, message);
            return;
        }

        if (getPluginInstance().getManager().isNotNumeric(amountValue)) {
            String message = getPluginInstance().getLangConfig().getString("invalid-amount");
            if (message != null && !message.equalsIgnoreCase(""))
                getPluginInstance().getManager().sendMessage(player, message);
            return;
        }

        int amount = Integer.parseInt(amountValue);
        if (amount < 0) {
            getPluginInstance().getManager().sendMessage(player, getPluginInstance().getLangConfig().getString("negative-entry"));
            return;
        }

        final ItemStack itemStack = getPluginInstance().getManager().buildShopCreationItem(player, 1);
        if (amount == 0) amount = 1;
        else if (amount >= itemStack.getType().getMaxStackSize())
            amount = itemStack.getType().getMaxStackSize();

        final double price = (getPluginInstance().getConfig().getDouble("creation-item-price") * amount);

        if (price > 0) {
            final EconomyCallEvent economyCallEvent = EconomyCallEvent.call(player, null, EconomyCallType.CREATION_ITEM_COMMAND,
                    price);
            if (economyCallEvent.failed()) return;
        }

        getPluginInstance().getManager().giveItemStacks(player, itemStack, amount);

        String message = getPluginInstance().getLangConfig().getString("purchased-creation-item");
        if (message != null && !message.equalsIgnoreCase(""))
            getPluginInstance().getManager().sendMessage(player, message
                    .replace("{amount}", getPluginInstance().getManager().formatNumber(amount, false))
                    .replace("{price}", getPluginInstance().getEconomyHandler().format(null, getPluginInstance().getEconomyHandler().getDefaultCurrency(), price)));
    }

    private void runDelete(CommandSender commandSender) {
        if (!(commandSender instanceof Player)) {
            String message = getPluginInstance().getLangConfig().getString("must-be-player");
            if (message != null && !message.equalsIgnoreCase(""))
                commandSender.sendMessage(getPluginInstance().getManager().color(message));
            return;
        }

        Player player = (Player) commandSender;
        if (!player.hasPermission("displayshops.delete")) {
            String message = getPluginInstance().getLangConfig().getString("no-permission");
            if (message != null && !message.equalsIgnoreCase(""))
                getPluginInstance().getManager().sendMessage(player, message);
            return;
        }

        Shop shop = getPluginInstance().getManager().getShopRayTraced(player.getWorld().getName(),
                player.getEyeLocation().toVector(), player.getEyeLocation().getDirection(), 10);
        if (shop == null) {
            String message = getPluginInstance().getLangConfig().getString("shop-invalid");
            if (message != null && !message.equalsIgnoreCase(""))
                getPluginInstance().getManager().sendMessage(player, message);
            return;
        }

        if ((shop.getOwnerUniqueId() == null || (shop.getOwnerUniqueId() != null && !shop.getOwnerUniqueId().toString().equals(player.getUniqueId().toString())))
                && !player.hasPermission("displayshops.admindelete")) {
            String message = getPluginInstance().getLangConfig().getString("shop-delete-fail");
            if (message != null && !message.equalsIgnoreCase(""))
                getPluginInstance().getManager().sendMessage(player, message);
            return;
        }

        // call edit event
        ShopEditEvent shopEditEvent = new ShopEditEvent(player, shop, EditType.DELETE);
        getPluginInstance().getServer().getPluginManager().callEvent(shopEditEvent);
        if (shopEditEvent.isCancelled()) return;

        shop.purge(player, true);
        String message = getPluginInstance().getLangConfig().getString("shop-deleted");
        if (message != null && !message.equalsIgnoreCase(""))
            getPluginInstance().getManager().sendMessage(player, message);
    }

    private void runOwner(CommandSender commandSender, String[] args) {
        if (!(commandSender instanceof Player)) {
            String message = getPluginInstance().getLangConfig().getString("must-be-player");
            if (message != null && !message.equalsIgnoreCase(""))
                commandSender.sendMessage(getPluginInstance().getManager().color(message));
            return;
        }

        Player player = (Player) commandSender;
        if (!player.hasPermission("displayshops.owner")) {
            String message = getPluginInstance().getLangConfig().getString("no-permission");
            if (message != null && !message.equalsIgnoreCase(""))
                getPluginInstance().getManager().sendMessage(player, message);
            return;
        }

        Block block = player.getTargetBlock(null, 10);
        Shop shop = getPluginInstance().getManager().getShop(block.getLocation());
        if (shop == null) {
            String message = getPluginInstance().getLangConfig().getString("shop-invalid");
            if (message != null && !message.equalsIgnoreCase(""))
                getPluginInstance().getManager().sendMessage(player, message);
            return;
        }

        Player enteredPlayer = getPluginInstance().getServer().getPlayer(args[1]);
        if (enteredPlayer == null || !player.isOnline()) {
            String message = getPluginInstance().getLangConfig().getString("player-invalid");
            if (message != null && !message.equalsIgnoreCase(""))
                getPluginInstance().getManager().sendMessage(player, message);
            return;
        }

        shop.setOwnerUniqueId(enteredPlayer.getUniqueId());
        getPluginInstance().getInSightTask().refreshShop(shop);

        String message = getPluginInstance().getLangConfig().getString("shop-set-owner");
        if (message != null && !message.equalsIgnoreCase(""))
            getPluginInstance().getManager().sendMessage(player, message.replace("{player}", enteredPlayer.getName()));
    }

    private void runStock(CommandSender commandSender, String[] args) {
        if (!(commandSender instanceof Player)) {
            String message = getPluginInstance().getLangConfig().getString("must-be-player");
            if (message != null && !message.equalsIgnoreCase(""))
                commandSender.sendMessage(getPluginInstance().getManager().color(message));
            return;
        }

        Player player = (Player) commandSender;
        if (!player.hasPermission("displayshops.stock")) {
            String message = getPluginInstance().getLangConfig().getString("no-permission");
            if (message != null && !message.equalsIgnoreCase(""))
                getPluginInstance().getManager().sendMessage(player, message);
            return;
        }

        Block block = player.getTargetBlock(null, 10);
        Shop shop = getPluginInstance().getManager().getShop(block.getLocation());
        if (shop == null) {
            String message = getPluginInstance().getLangConfig().getString("shop-invalid");
            if (message != null && !message.equalsIgnoreCase(""))
                getPluginInstance().getManager().sendMessage(player, message);
            return;
        }

        int enteredAmount;
        if (getPluginInstance().getManager().isNotNumeric(args[1])) {
            String message = getPluginInstance().getLangConfig().getString("invalid-amount");
            if (message != null && !message.equalsIgnoreCase(""))
                getPluginInstance().getManager().sendMessage(player, message);
            return;
        }
        enteredAmount = Integer.parseInt(args[1]);

        if (enteredAmount > shop.getMaxStock()) {
            String message = getPluginInstance().getLangConfig().getString("invalid-stock");
            if (message != null && !message.equalsIgnoreCase(""))
                getPluginInstance().getManager().sendMessage(player, message);
            return;
        }

        // call edit event
        ShopEditEvent shopEditEvent = new ShopEditEvent(player, shop, EditType.STOCK_SET, enteredAmount);
        getPluginInstance().getServer().getPluginManager().callEvent(shopEditEvent);
        if (shopEditEvent.isCancelled()) return;

        shop.setStock(enteredAmount);
        getPluginInstance().getInSightTask().refreshShop(shop);

        String message = getPluginInstance().getLangConfig().getString("stock-set");
        if (message != null && !message.equalsIgnoreCase(""))
            getPluginInstance().getManager().sendMessage(player, message.replace("{amount}",
                    getPluginInstance().getManager().formatNumber(enteredAmount, false)));
    }

    private void runVisit(CommandSender commandSender, String id, String filter) {
        if (!(commandSender instanceof Player)) {
            String message = getPluginInstance().getLangConfig().getString("must-be-player");
            if (message != null && !message.equalsIgnoreCase(""))
                commandSender.sendMessage(getPluginInstance().getManager().color(message));
            return;
        }

        Player player = (Player) commandSender;
        if (!player.hasPermission("displayshops.visit")) {
            String message = getPluginInstance().getLangConfig().getString("no-permission");
            if (message != null && !message.equalsIgnoreCase(""))
                getPluginInstance().getManager().sendMessage(player, message);
            return;
        }

        if (!getPluginInstance().getConfig().getBoolean("shop-visiting")) {
            String message = getPluginInstance().getLangConfig().getString("shop-visiting-disabled");
            if (message != null && !message.equalsIgnoreCase(""))
                getPluginInstance().getManager().sendMessage(player, message);
            return;
        }

        if (getPluginInstance().getTeleportingPlayers().contains(player.getUniqueId())) {
            String message = getPluginInstance().getLangConfig().getString("visit-in-process");
            if (message != null && !message.equalsIgnoreCase(""))
                getPluginInstance().getManager().sendMessage(player, message);
            return;
        }

        if (id != null && !id.isEmpty()) {
            try {
                Shop shop = getPluginInstance().getManager().getShopById(UUID.fromString(id));
                if (shop != null) {
                    shop.visit(player, true);
                    return;
                }
            } catch (Exception ignored) {
            }
        }

        Menu visitMenu = getPluginInstance().getMenu("visit");
        if (visitMenu != null) visitMenu.build(player, filter);
    }

    private void runGive(CommandSender commandSender, String[] args) {
        if (!commandSender.hasPermission("displayshops.give")) {
            String message = getPluginInstance().getLangConfig().getString("no-permission");
            if (message != null && !message.equalsIgnoreCase(""))
                if (commandSender instanceof Player)
                    getPluginInstance().getManager().sendMessage((Player) commandSender, message);
                else commandSender.sendMessage(getPluginInstance().getManager().color(message));
            return;
        }

        Player player = getPluginInstance().getServer().getPlayer(args[1]);
        if (player == null || !player.isOnline()) {
            String message = getPluginInstance().getLangConfig().getString("player-invalid");
            if (message != null && !message.equalsIgnoreCase(""))
                if (commandSender instanceof Player)
                    getPluginInstance().getManager().sendMessage((Player) commandSender, message);
                else commandSender.sendMessage(getPluginInstance().getManager().color(message));
            return;
        }

        int enteredAmount;
        if (getPluginInstance().getManager().isNotNumeric(args[2])) {
            String message = getPluginInstance().getLangConfig().getString("invalid-amount");
            if (message != null && !message.equalsIgnoreCase(""))
                if (commandSender instanceof Player)
                    getPluginInstance().getManager().sendMessage((Player) commandSender, message);
                else commandSender.sendMessage(getPluginInstance().getManager().color(message));
            return;
        }

        enteredAmount = Integer.parseInt(args[2]);

        ItemStack itemStack = getPluginInstance().getManager().buildShopCreationItem(player, enteredAmount);
        if (player.getInventory().firstEmpty() == -1)
            player.getWorld().dropItemNaturally(player.getLocation(), itemStack);
        else
            player.getInventory().addItem(itemStack);

        String message1 = getPluginInstance().getLangConfig().getString("received-creation-item");
        if (message1 != null && !message1.equalsIgnoreCase(""))
            getPluginInstance().getManager().sendMessage(player, message1
                    .replace("{amount}", getPluginInstance().getManager().formatNumber(enteredAmount, false)));

        if (!commandSender.getName().equalsIgnoreCase(player.getName())) {
            String message2 = getPluginInstance().getLangConfig().getString("given-creation-item");
            if (message2 != null && !message2.equalsIgnoreCase(""))
                if (commandSender instanceof Player)
                    getPluginInstance().getManager().sendMessage((Player) commandSender, message2
                            .replace("{amount}", getPluginInstance().getManager().formatNumber(enteredAmount, false))
                            .replace("{player}", player.getName()));
                else commandSender.sendMessage(getPluginInstance().getManager().color(message2
                        .replace("{amount}", getPluginInstance().getManager().formatNumber(enteredAmount, false))
                        .replace("{player}", player.getName())));
        }
    }

    // other helpers
    private void sendHelp(CommandSender commandSender) {
        //if (!(commandSender instanceof Player)) {
        final String helpMessage = getPluginInstance().getLangConfig().getString(commandSender.hasPermission("displayshops.adminhelp")
                ? "admin-help-message" : "user-help-message");
        if (helpMessage == null || helpMessage.isEmpty()) return;

        final String helpLink = getPluginInstance().getConfig().getString("help-command-link");
        if (commandSender instanceof Player) {
            Player player = (Player) commandSender;

            if (helpLink != null && !helpLink.isEmpty()) {
                TextComponent textComponent = new TextComponent(getPluginInstance().getManager().color(helpMessage));
                textComponent.setClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, helpLink.equalsIgnoreCase("wiki")
                        ? "https://github.com/XZot1K/DisplayShopsAPI/wiki/Commands" : helpLink));
                player.spigot().sendMessage(textComponent);
                return;
            }

            getPluginInstance().getManager().sendMessage(player, helpMessage);
            return;
        }

        commandSender.sendMessage(getPluginInstance().getManager().color(helpMessage));
        if (helpLink != null && !helpLink.isEmpty())
            commandSender.sendMessage(helpLink.equalsIgnoreCase("wiki") ? "https://github.com/XZot1K/DisplayShopsAPI/wiki/Commands" : helpLink);
        //    return;
        //}

       /* ItemStack bookItem = new ItemStack(Material.WRITTEN_BOOK);
        BookMeta bookmeta = (BookMeta) bookItem.getItemMeta();
        if (bookmeta == null) return;

        bookmeta.setAuthor("XZot1K");
        bookmeta.setTitle("Commands");

        ConfigurationSection commandsSection = getPluginInstance().getLangConfig().getConfigurationSection("help-book.commands");
        if (commandsSection == null) return;

        String pageHeader = getPluginInstance().getLangConfig().getString("help-book.page-header");
        int counter = 0;
        StringBuilder sb = new StringBuilder(pageHeader + "\n");
        for (String command : commandsSection.getKeys(false)) {
            sb.append(getPluginInstance().getManager().color("\n" + command + "\n" + commandsSection.getString(command) + "\n"));

            if (counter >= 4) {
                bookmeta.addPage(sb.toString());
                counter = 0;
                sb.setLength(0);
                sb.append(pageHeader).append("\n");
            }

            counter++;
        }

        if (sb.length() > (pageHeader + "\n").length()) bookmeta.addPage(sb.toString());

        bookItem.setItemMeta(bookmeta);
        Player player = (Player) commandSender;
        player.openBook(bookItem);*/
    }

    // getters & setters
    private DisplayShops getPluginInstance() {
        return pluginInstance;
    }

    private void setPluginInstance(DisplayShops pluginInstance) {
        this.pluginInstance = pluginInstance;
    }
}