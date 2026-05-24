/*
 * Copyright (c) 2021 XZot1K, All rights reserved.
 */

package xzot1k.plugins.ds.core.tasks;

import org.apache.commons.lang3.StringUtils;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.NotNull;
import xzot1k.plugins.ds.DisplayShops;
import xzot1k.plugins.ds.api.eco.EcoHook;
import xzot1k.plugins.ds.api.enums.EconomyCallType;
import xzot1k.plugins.ds.api.objects.DataPack;
import xzot1k.plugins.ds.api.objects.MarketRegion;
import xzot1k.plugins.ds.api.objects.Shop;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.*;
import java.util.logging.Level;

public class ManagementTask extends BukkitRunnable {

    private final boolean dynamicPricesEnabled;
    private DisplayShops pluginInstance;
    private FileConfiguration recoveryConfig;
    private File recoveryFile;
    private int autoSaveCounter, mysqlDumpCounter;
    private int autoSaveInterval, dynamicResetDuration, mysqlDumpInterval;

    public ManagementTask(DisplayShops pluginInstance) {
        setPluginInstance(pluginInstance);
        setAutoSaveCounter(0);
        setMysqlDumpCounter(0);
        setAutoSaveInterval(getPluginInstance().getConfig().getInt("auto-save-interval"));
        setMysqlDumpInterval(getPluginInstance().getConfig().getInt("mysql.dump-interval"));
        setDynamicResetDuration(getPluginInstance().getConfig().getInt("dynamic-price-change"));
        this.dynamicPricesEnabled = getPluginInstance().getConfig().getBoolean("dynamic-prices");

        reloadRecoveryConfig();
    }

    @Override
    public void run() {
        setAutoSaveCounter(getAutoSaveCounter() + 1);
        final boolean isSaveTime = (getAutoSaveCounter() >= getAutoSaveInterval());
        if (isSaveTime) setAutoSaveCounter(0);

        final List<Shop> shopList = new ArrayList<>(getPluginInstance().getManager().getShopMap().values());
        for (Shop shop : shopList) {
            if (shop == null) continue;

            if (isDynamicPricesEnabled() && shop.canDynamicPriceChange()) {
                boolean wasUpdated = false;
                if (shop.isReadyForDynamicReset(getDynamicResetDuration(), EconomyCallType.BUY)) {
                    shop.setDynamicBuyCounter(0);
                    wasUpdated = true;
                }
                if (shop.isReadyForDynamicReset(getDynamicResetDuration(), EconomyCallType.SELL)) {
                    shop.setDynamicSellCounter(0);
                    wasUpdated = true;
                }

                if (wasUpdated) getPluginInstance().refreshShop(shop);
            }

            if (isSaveTime) shop.save(false);
        }

        for (MarketRegion marketRegion : getPluginInstance().getManager().getMarketRegions()) {
            int timeUntilExpire;
            if (marketRegion != null && marketRegion.getRenter() != null && marketRegion.getRentedTimeStamp() != null && !marketRegion.getRentedTimeStamp().isEmpty())
                if ((timeUntilExpire = marketRegion.timeUntilExpire()) <= 0) marketRegion.reset();
                else if (getPluginInstance().getConfig().getIntegerList("rent-notify-intervals").contains(timeUntilExpire)) {
                    getPluginInstance().getServer().getScheduler().runTask(getPluginInstance(), () -> {
                        Player renter = getPluginInstance().getServer().getPlayer(marketRegion.getRenter());
                        if (renter != null)
                            getPluginInstance().getManager().sendMessage(renter, Objects.requireNonNull(getPluginInstance().getLangConfig().getString("rent-notification"))
                                    .replace("{id}", StringUtils.capitalize(marketRegion.getMarketId())).replace("{time}", marketRegion.formattedTimeUntilExpire()));
                    });
                }
        }

        if (isSaveTime) {
            getPluginInstance().getManager().saveMarketRegions();
            final Set<Map.Entry<UUID, DataPack>> dataPacks = new HashSet<>(getPluginInstance().getManager().getDataPackMap().entrySet());
            try {
                Statement statement = getPluginInstance().getDatabaseConnection().createStatement();
                for (Map.Entry<UUID, DataPack> dataPackEntry : dataPacks) {
                    if (dataPackEntry == null) continue;
                    getPluginInstance().getManager().saveDataPack(statement, dataPackEntry.getKey(), dataPackEntry.getValue(), false, false);
                    OfflinePlayer player = getPluginInstance().getServer().getPlayer(dataPackEntry.getKey());
                    if (player == null || !player.isOnline())
                        getPluginInstance().getManager().getDataPackMap().remove(dataPackEntry.getKey());
                }

                statement.close();
            } catch (SQLException e) {
                getPluginInstance().log(Level.WARNING, "There was an issue saving due to an SQL issue.");
            }
        }

        final String host = getPluginInstance().getConfig().getString("mysql.host");
        if (host != null && !host.isEmpty()) {
            setMysqlDumpCounter(getMysqlDumpCounter() + 1);
            final boolean isDumpTime = (getMysqlDumpCounter() >= getMysqlDumpInterval());
            if (isDumpTime) {
                try {
                    getPluginInstance().exportMySQLDatabase();
                } catch (IOException ignored) {}
                setMysqlDumpCounter(0);
            }
        }

        for (Player player : getPluginInstance().getServer().getOnlinePlayers()) {
            if (player == null || !player.isOnline()) return;
            handleRecovery(player);
        }
    }

    public void handleRecovery(@NotNull Player player) {
        final ConfigurationSection playerSection = recoveryConfig.getConfigurationSection(player.getUniqueId().toString());
        if (playerSection == null) return;

        for (String shopId : playerSection.getKeys(false)) {
            if (playerSection.contains(shopId + ".shop-item")) {
                ItemStack itemStack = playerSection.getItemStack(shopId + ".shop-item.data");
                if (itemStack == null) itemStack = getPluginInstance().getManager().buildShopCurrencyItem(1);

                final ItemStack finalItemStack = itemStack;
                getPluginInstance().getServer().getScheduler().runTask(getPluginInstance(), () ->
                        getPluginInstance().getManager().giveItemStacks(player, finalItemStack, playerSection.getInt(shopId + ".shop-item.amount")));
            }

            if (playerSection.contains(shopId + ".currency")) {
                final String currencyType = playerSection.getString(shopId + ".currency.type");
                if (currencyType != null) {
                    if (currencyType.equals("item-for-item")) {
                        ItemStack itemStack = playerSection.getItemStack(shopId + ".currency.data");
                        if (itemStack == null || getPluginInstance().getConfig().getBoolean("shop-currency-item.force-use"))
                            itemStack = getPluginInstance().getManager().buildShopCurrencyItem(1);

                        final ItemStack finalItemStack = itemStack;
                        getPluginInstance().getServer().getScheduler().runTask(getPluginInstance(), () ->
                                getPluginInstance().getManager().giveItemStacks(player, finalItemStack, playerSection.getInt(shopId + ".currency.amount")));
                    } else {
                        getPluginInstance().getServer().getScheduler().runTask(getPluginInstance(), () -> {
                            EcoHook ecoHook = getPluginInstance().getEconomyHandler().getEcoHook(currencyType);
                            if (ecoHook != null)
                                ecoHook.deposit(player, playerSection.getDouble(shopId + ".currency.amount"));
                        });
                    }
                }
            }
        }

        clearRecovery(player);
    }

    private void clearRecovery(@NotNull Player player) {
        try {
            recoveryConfig.set(player.getUniqueId().toString(), null);
            recoveryConfig.save(recoveryFile);
        } catch (IOException ignored) {
            getPluginInstance().log(Level.WARNING, "Failed to clear the recovery data of \"" + player.getName() + "\".");
        }
    }

    public void reloadRecoveryConfig() {
        if (recoveryFile == null) {
            recoveryFile = new File(getPluginInstance().getDataFolder(), "recovery.yml");
            recoveryConfig = YamlConfiguration.loadConfiguration(recoveryFile);

            try {
                recoveryConfig.save(recoveryFile);
            } catch (IOException e) {e.printStackTrace();}
        } else recoveryConfig = YamlConfiguration.loadConfiguration(recoveryFile);
    }

    public void createRecovery(@NotNull UUID playerUniqueId, @NotNull Shop shop) {
        try {
            int existingShopItemAmount = 0, existingCurrencyAmount = 0;
            if (recoveryConfig.contains(playerUniqueId + "." + shop.getShopId().toString() + ".shop-item"))
                existingShopItemAmount = recoveryConfig.getInt(playerUniqueId + "." + shop.getShopId().toString() + ".shop-item.amount");

            if (recoveryConfig.contains(playerUniqueId + "." + shop.getShopId().toString() + ".currency.amount"))
                existingCurrencyAmount = recoveryConfig.getInt(playerUniqueId + "." + shop.getShopId().toString() + ".currency.amount");

            if (shop.getStock() > 0 && shop.getShopItem() != null) {
                recoveryConfig.set((playerUniqueId + "." + shop.getShopId() + ".shop-item.data"), shop.getShopItem());
                recoveryConfig.set((playerUniqueId + "." + shop.getShopId() + ".shop-item.amount"), (existingShopItemAmount + shop.getStock()));
            }

            if (shop.getCurrencyType() != null && shop.getStoredBalance() > 0) {
                recoveryConfig.set((playerUniqueId + "." + shop.getShopId() + ".currency.type"), shop.getCurrencyType());
                if (shop.getCurrencyType().equals("item-for-item"))
                    recoveryConfig.set((playerUniqueId + "." + shop.getShopId() + ".currency.data"),
                            ((shop.getTradeItem() != null) ? shop.getTradeItem() : getPluginInstance().getManager().defaultCurrencyItem));
                recoveryConfig.set((playerUniqueId + "." + shop.getShopId() + ".currency.amount"), (existingCurrencyAmount + shop.getStoredBalance()));
            }

            recoveryConfig.save(recoveryFile);
        } catch (IOException ignored) {
            getPluginInstance().log(Level.WARNING, "Failed to create the recovery data for \"" + playerUniqueId + "\".");
        }

    }

    // getters & setters
    private DisplayShops getPluginInstance() {
        return pluginInstance;
    }

    private void setPluginInstance(DisplayShops pluginInstance) {
        this.pluginInstance = pluginInstance;
    }

    private int getAutoSaveInterval() {
        return autoSaveInterval;
    }

    private void setAutoSaveInterval(int autoSaveInterval) {
        this.autoSaveInterval = autoSaveInterval;
    }

    private int getDynamicResetDuration() {
        return dynamicResetDuration;
    }

    private void setDynamicResetDuration(int dynamicResetDuration) {
        this.dynamicResetDuration = dynamicResetDuration;
    }

    public boolean isDynamicPricesEnabled() {
        return dynamicPricesEnabled;
    }

    public int getAutoSaveCounter() {
        return autoSaveCounter;
    }

    public void setAutoSaveCounter(int autoSaveCounter) {
        this.autoSaveCounter = autoSaveCounter;
    }

    public int getMysqlDumpCounter() {
        return mysqlDumpCounter;
    }

    public void setMysqlDumpCounter(int mysqlDumpCounter) {
        this.mysqlDumpCounter = mysqlDumpCounter;
    }

    public int getMysqlDumpInterval() {
        return mysqlDumpInterval;
    }

    public void setMysqlDumpInterval(int mysqlDumpInterval) {
        this.mysqlDumpInterval = mysqlDumpInterval;
    }
}