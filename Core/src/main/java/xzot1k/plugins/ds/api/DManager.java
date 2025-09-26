/*
 * Copyright (c) 2021 XZot1K, All rights reserved.
 */

package xzot1k.plugins.ds.api;

import me.devtec.shared.Ref;
import net.md_5.bungee.api.ChatColor;
import org.apache.commons.lang.WordUtils;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.permissions.PermissionAttachmentInfo;
import org.bukkit.potion.PotionType;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import xzot1k.plugins.ds.DisplayShops;
import xzot1k.plugins.ds.api.eco.EcoHook;
import xzot1k.plugins.ds.api.objects.*;

import java.io.File;
import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.DecimalFormat;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DManager implements Manager {

    private DisplayShops pluginInstance;

    private ConcurrentHashMap<String, Shop> shopMap;
    private List<MarketRegion> marketRegions;
    private final Pattern hexPattern, uuidPattern;
    private ConcurrentHashMap<UUID, DataPack> dataPackMap;
    public ItemStack defaultCurrencyItem;

    public DManager(DisplayShops pluginInstance) {
        setPluginInstance(pluginInstance);
        setShopMap(new ConcurrentHashMap<>());
        setDataPackMap(new ConcurrentHashMap<>());
        setMarketRegions(new ArrayList<>());
        hexPattern = Pattern.compile("#[a-fA-F\\d]{6}");
        uuidPattern = Pattern.compile("^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$");
    }

    /**
     * Loads the passed player's data pack. If not found, a new data pack module is created. (CAN RETURN NULL)
     *
     * @param player The player to load the data pack for.
     */
    public DataPack loadDataPack(@NotNull Player player) {
        DataPack dataPack = null;
        try (PreparedStatement statement = getPluginInstance().getDatabaseConnection().prepareStatement(
                "SELECT * FROM player_data WHERE uuid = '" + player.getUniqueId() + "';");
             ResultSet resultSet = statement.executeQuery()) {
            if (resultSet.next()) {
                dataPack = new DDataPack(getPluginInstance(), resultSet.getString("appearance_data"));

                final String cooldownLine = resultSet.getString("cooldowns");
                String[] cdArgs = cooldownLine.split(",");

                final String transactionLimitLine = resultSet.getString("transaction_limits");
                if (transactionLimitLine != null && !transactionLimitLine.isEmpty()) {
                    if (transactionLimitLine.contains(",")) {
                        String[] tlArgs = transactionLimitLine.split(",");
                        try {
                            if (tlArgs.length > 0) for (int i = -1; ++i < tlArgs.length; ) {
                                final String[] tlLineArgs = tlArgs[i].split(":"), tlInnerArgs = tlLineArgs[1].split(";");

                                String uuid = tlLineArgs[0];
                                if (uuid == null || uuid.isEmpty()) continue;
                                final UUID id = UUID.fromString(uuid);

                                Shop shop = getShopById(id);
                                if (shop == null) continue;
                                dataPack.updateCurrentTransactionLimitCounter(shop, true, Long.parseLong(tlInnerArgs[0]));
                                dataPack.updateCurrentTransactionLimitCounter(shop, false, Long.parseLong(tlInnerArgs[1]));
                            }
                        } catch (ArrayIndexOutOfBoundsException ignored) {
                        }
                    } else {
                        final String[] args = transactionLimitLine.split(":");
                        final String[] innerArgs = args[1].split(";");
                        final UUID id = UUID.fromString(args[0]);
                        final Shop shop = getShopById(id);
                        if (shop != null) {
                            dataPack.updateCurrentTransactionLimitCounter(shop, true, Long.parseLong(innerArgs[0]));
                            dataPack.updateCurrentTransactionLimitCounter(shop, false, Long.parseLong(innerArgs[1]));
                        }
                    }
                }

                try {
                    if (cdArgs.length > 0) for (int i = -1; ++i < cdArgs.length; ) {
                        final String[] cdLineArgs = cdArgs[i].split(":");
                        dataPack.getCooldownMap().put(cdLineArgs[0], Long.parseLong(cdLineArgs[1]));
                    }
                } catch (ArrayIndexOutOfBoundsException ignored) {
                }

                String notify = resultSet.getString("notify");
                if (notify != null && !notify.isEmpty())
                    dataPack.setTransactionNotify(notify.equalsIgnoreCase("1"));
            }

            resultSet.close();
            statement.close();

            if (dataPack == null) dataPack = new DDataPack(getPluginInstance(), null);
            getDataPackMap().put(player.getUniqueId(), dataPack);
            return dataPack;
        } catch (SQLException e) {
            e.printStackTrace();
            getPluginInstance().log(Level.WARNING, "There was an issue loading existing data for \"" + player.getName() + "\".");
        }

        if (dataPack == null) dataPack = new DDataPack(getPluginInstance(), null);
        getDataPackMap().put(player.getUniqueId(), dataPack);
        return dataPack;
    }

    /**
     * Attempts to remove any player data from the database when the player's last seen exceeds one year.
     */
    public void cleanUpDataPacks() {
        try (PreparedStatement statement = getPluginInstance().getDatabaseConnection().prepareStatement("SELECT * FROM player_data;");
             ResultSet resultSet = statement.executeQuery()) {
            while (resultSet.next()) {
                String uuidString = resultSet.getString("uuid");
                if (uuidString == null || uuidString.isEmpty()) {
                    Statement deleteStatement = getPluginInstance().getDatabaseConnection().createStatement();
                    deleteStatement.executeUpdate("DELETE FROM player_data where uuid = '" + uuidString + "';");
                    deleteStatement.close();
                    getPluginInstance().log(Level.WARNING, "[CLEANING] A data pack was removed due to its invalid UUID.");
                    continue;
                }

                UUID playerId = UUID.fromString(resultSet.getString("uuid"));
                Statement deleteStatement = getPluginInstance().getDatabaseConnection().createStatement();
                deleteStatement.executeUpdate("DELETE FROM player_data where uuid = '" + uuidString + "';");
                deleteStatement.close();
                getPluginInstance().log(Level.WARNING, "[CLEANING] A data pack was removed due to its untranslatable UUID.");

                OfflinePlayer offlinePlayer = getPluginInstance().getServer().getOfflinePlayer(playerId);
                deleteStatement = getPluginInstance().getDatabaseConnection().createStatement();
                deleteStatement.executeUpdate("DELETE FROM player_data where uuid = '" + uuidString + "';");
                getPluginInstance().log(Level.WARNING, "[CLEANING] The data pack for the player \"" + playerId
                        + "\" was removed due to the offline player being invalid.");

                if (!offlinePlayer.hasPlayedBefore() || TimeUnit.MILLISECONDS.toDays(System.currentTimeMillis() - offlinePlayer.getLastPlayed()) >= 365) {
                    deleteStatement = getPluginInstance().getDatabaseConnection().createStatement();
                    deleteStatement.executeUpdate("DELETE FROM player_data where uuid = '" + uuidString + "';");
                    deleteStatement.close();
                    getPluginInstance().log(Level.WARNING, "[CLEANING] The data pack for the player \""
                            + playerId + "\" (" + offlinePlayer.getName() + ") was removed due to last seen exceeding 365 days (One Year).");
                }
            }

        } catch (SQLException e) {
            e.printStackTrace();
            getPluginInstance().log(Level.WARNING, "This is just a warning stating that a market region has failed to load.");
        }
    }

    /**
     * Saves the player's data pack to the database.
     *
     * @param statement      The database statement to use.
     * @param playerUniqueId The player's UUID for the data pack to save under.
     * @param dataPack       The data pack itself.
     * @param async          Whether it should be saved on the main thread or not.
     * @param closeStatement Closes the database statement when task is complete.
     */
    public void saveDataPack(Statement statement, UUID playerUniqueId, DataPack dataPack, boolean async, boolean closeStatement) {
        if (async)
            getPluginInstance().getServer().getScheduler().runTaskAsynchronously(getPluginInstance(), () ->
                    saveDataPack(statement, playerUniqueId, dataPack, closeStatement));
        else saveDataPack(statement, playerUniqueId, dataPack, closeStatement);
    }

    private synchronized void saveDataPack(Statement statement, UUID playerUniqueId, DataPack dataPack, boolean closeStatement) {
        if (playerUniqueId == null) return;
        try {
            if (statement == null) statement = getPluginInstance().getDatabaseConnection().createStatement();

            final String host = getPluginInstance().getConfig().getString("mysql.host"), syntax;
            if (host == null || host.isEmpty())
                syntax = "INSERT OR REPLACE INTO player_data(uuid, appearance_data, cooldowns, transaction_limits, notify) VALUES('"
                        + playerUniqueId + "', '" + dataPack.getAppearanceData() + "', '" + dataPack.cooldownsToString() + "', '"
                        + dataPack.transactionLimitsToString() + "', '" + (dataPack.isTransactionNotify() ? 1 : 0) + "');";
            else
                syntax = "INSERT INTO player_data(uuid, appearance_data, cooldowns, transaction_limits, notify) VALUES( '"
                        + playerUniqueId + "', '" + dataPack.getAppearanceData() + "', '" + dataPack.cooldownsToString() + "', '"
                        + dataPack.transactionLimitsToString() + "', '" + (dataPack.isTransactionNotify() ? 1 : 0)
                        + "') ON DUPLICATE KEY UPDATE uuid = '" + playerUniqueId + "', appearance_data = '"
                        + dataPack.getAppearanceData() + "', cooldowns = '" + dataPack.cooldownsToString()
                        + "', transaction_limits = '" + dataPack.transactionLimitsToString() + "', notify = '"
                        + (dataPack.isTransactionNotify() ? 1 : 0) + "';";

            statement.executeUpdate(syntax);
            if (closeStatement) statement.close();
        } catch (SQLException e) {
            e.printStackTrace();
            getPluginInstance().log(Level.WARNING, "There was an issue saving the data pack for '"
                    + playerUniqueId + "' to the database (" + e.getMessage() + ").");
        }
    }

    /**
     * Obtains the designated data pack for the passed player.
     *
     * @param player The player to obtain the data pack for.
     * @return The data pack object (Cooldowns, interactions, unlocks, etc.). (Should never return NULL)
     */
    public DataPack getDataPack(Player player) {
        DataPack dataPack = getDataPackMap().getOrDefault(player.getUniqueId(), null);
        if (dataPack == null) dataPack = loadDataPack(player);
        return dataPack;
    }

    /**
     * Ray traces from the provided vectors to obtain a shop from the locations it passes through.
     *
     * @param worldName       The world to ray trace in.
     * @param eyeVector       The eye location of the entity or just the general origin location.
     * @param directionVector The direction at which the vector should go.
     * @param distance        The distance at which the ray should travel.
     * @return The found shop, returns NULL if NOT found.
     */
    public Shop getShopRayTraced(@NotNull String worldName, @NotNull Vector eyeVector, @NotNull Vector directionVector, double distance) {
        // Shop[] shopList = getShopMap().values().toArray(new Shop[0]);
        for (int i = -1; ++i < distance; ) {
            final Vector newPositionVector = eyeVector.clone().add(directionVector.clone().multiply(i));
            final String locationToString = worldName + "," + newPositionVector.getBlockX() + "," + newPositionVector.getBlockY() + "," + newPositionVector.getBlockZ();

            Shop shop = getShopMap().getOrDefault(locationToString, null);
            if (shop != null) return shop;

           /* for (int j = -1; ++j < shopList.length; ) {
                final Shop shop = shopList[j];
                if (shop == null || shop.getBaseLocation() == null || !shop.getBaseLocation().getWorldName().equals(worldName)
                        || !((shop.getBaseLocation().getX() + 1.2) > newPositionVector.getX()
                        && (shop.getBaseLocation().getX() - 0.2) < newPositionVector.getX())
                        || !((shop.getBaseLocation().getY() + 2) > newPositionVector.getY()
                        && (shop.getBaseLocation().getY() - 0.2) < newPositionVector.getY())
                        || !((shop.getBaseLocation().getZ() + 1.2) > newPositionVector.getZ()
                        && (shop.getBaseLocation().getZ() - 0.2) < newPositionVector.getZ()))
                    continue;
                return shop;
            }*/
        }
        return null;
    }

    /**
     * Sends a color translated message to the players as either a normal chat message or action bar message.
     *
     * @param player       The player to send the message to.
     * @param message      The message to send (color codes accepted, if the message contains {bar} at the front it will be sent to the action bar).
     * @param placeholders The placeholders in the format <placeholder>:<replacement>.
     */
    public void sendMessage(@NotNull Player player, @Nullable String message, @Nullable String... placeholders) {
        if (message == null || message.isEmpty()) return;

        final String replacedMessage = applyPlaceholders(player, message, placeholders);
        if (!message.toLowerCase().startsWith("{bar}")) player.sendMessage(color(replacedMessage));
        else getPluginInstance().sendActionBar(player, replacedMessage.substring(5));
    }

    @Override
    public String applyPlaceholders(@Nullable String text, @Nullable String... placeholders) {
        if (text == null || text.isEmpty() || placeholders == null || placeholders.length == 0) return text;

        for (int i = -1; ++i < placeholders.length; ) {
            final String placeholder = placeholders[i];
            if (placeholder == null || !placeholder.contains(":")) continue;

            final String[] args = placeholder.split(":");
            if (args.length >= 2) text = text.replace(args[0], args[1]);
            else if (args.length == 1) text = text.replace(args[0], "");
        }

        return text;
    }

    @Override
    public String applyPlaceholders(@NotNull Player player, @Nullable String text, @Nullable String... placeholders) {
        if (text == null || text.isEmpty()) return text;
        return applyPlaceholders(getPluginInstance().papiText(player, text), placeholders);
    }

    /**
     * @param text       The text to apply placeholders to.
     * @param shop       The shop to obtain placeholder values from.
     * @param unitValues The unit count and unit item max stack values.
     * @return The text with placeholders applied.
     */
    public String applyShopBasedPlaceholders(@Nullable String text, @NotNull Shop shop, int... unitValues) {
        final boolean unitValuesProvided = (unitValues != null && unitValues.length >= 2);
        final int //unitItemMaxStack = (unitValuesProvided ? unitValues[1] : 0),
                unitCount = (unitValuesProvided ? unitValues[0] : 1);

        final double tax = getPluginInstance().getConfig().getDouble("transaction-tax"),
                buyPrice = shop.getBuyPrice(true),
                sellPrice = shop.getSellPrice(true),
                beforeBuyPrice = (buyPrice * unitCount),
                calculatedBuyPrice = (beforeBuyPrice + (beforeBuyPrice * tax)),
                calculatedSellPrice = (sellPrice * unitCount);

        String ownerName = ((shop.getOwnerUniqueId() != null) ?
                getPluginInstance().getServer().getOfflinePlayer(shop.getOwnerUniqueId()).getName() : ""),
                disabled = getPluginInstance().getLangConfig().getString("disabled");
        if (disabled == null) disabled = "";

        EcoHook ecoHook = getPluginInstance().getEconomyHandler().getEcoHook(shop.getCurrencyType());
        return applyPlaceholders(text, ("{no-vault}:"), ("{assistant-count}:" + shop.getAssistants().size()),
                ("{base-buy-price}:" + (buyPrice >= 0 ? getPluginInstance().getEconomyHandler().format(shop, shop.getCurrencyType(), buyPrice) : disabled)),
                ("{buy-price}:" + (buyPrice >= 0 ? getPluginInstance().getEconomyHandler().format(shop, shop.getCurrencyType(), calculatedBuyPrice) : disabled)),
                ("{sell-price}:" + (sellPrice >= 0 ? getPluginInstance().getEconomyHandler().format(shop, shop.getCurrencyType(), calculatedSellPrice) : disabled)),
                ("{balance}:" + ((shop.isAdminShop() && shop.getStoredBalance() <= 0) ? "∞"
                        : getPluginInstance().getEconomyHandler().format(shop, shop.getCurrencyType(), shop.getStoredBalance()))),
                ("{stock}:" + (shop.getStock() <= -1 ? "∞" : getPluginInstance().getManager().formatNumber(shop.getStock(), false))),
                ("{global-buy-counter}:" + getPluginInstance().getManager().formatNumber(shop.getGlobalBuyCounter(), false)),
                ("{global-sell-counter}:" + getPluginInstance().getManager().formatNumber(shop.getGlobalSellCounter(), false)),
                ("{global-buy-limit}:" + (shop.getGlobalBuyLimit() >= 0 ? getPluginInstance().getManager().formatNumber(shop.getGlobalBuyLimit(), false) : disabled)),
                ("{global-sell-limit}:" + (shop.getGlobalSellLimit() >= 0 ? getPluginInstance().getManager().formatNumber(shop.getGlobalSellLimit(), false) : disabled)),
                ("{player-buy-limit}:" + (shop.getPlayerBuyLimit() >= 0 ? getPluginInstance().getManager().formatNumber(shop.getPlayerBuyLimit(), false) : disabled)),
                ("{player-sell-limit}:" + (shop.getPlayerSellLimit() >= 0 ? getPluginInstance().getManager().formatNumber(shop.getPlayerSellLimit(), false) : disabled)),
                ("{item}:" + (shop.getShopItem() != null ? getPluginInstance().getManager().getItemName(shop.getShopItem()) : "")),
                ("{trade-item}:" + shop.getTradeItemName()),
                ("{shop-item-amount}:" + getPluginInstance().getManager().formatNumber(shop.getShopItemAmount(), false)),
                ("{amount}:" + getPluginInstance().getManager().formatNumber(shop.getShopItemAmount(), false)),
                ("{unit-count}:" + getPluginInstance().getManager().formatNumber(unitCount, false)),
                ("{item-count}:" + getPluginInstance().getManager().formatNumber((shop.getShopItemAmount() * unitCount), false)),
                ("{owner}:" + ((ownerName == null) ? "---" : ownerName)),
                ("{symbol}:" + (ecoHook != null ? ecoHook.getSymbol() : "")),
                ("{currency-name}:" + (ecoHook != null ? ecoHook.getName() : "")));
    }

    /**
     * Retrieve a market region, if the passed location is inside it.
     *
     * @param location The location to check.
     * @return The MarketRegion object found.
     */
    public MarketRegion getMarketRegion(@NotNull Location location) {
        if (getMarketRegions().isEmpty()) return null;
        for (int i = -1; ++i < getMarketRegions().size(); ) {
            MarketRegion marketRegion = getMarketRegions().get(i);
            if (marketRegion.isInRegion(location)) return marketRegion;
        }
        return null;
    }

    /**
     * Retrieve a market region by Id, if it exists.
     *
     * @param marketId The id to check for.
     * @return The MarketRegion object.
     */
    public MarketRegion getMarketRegion(@NotNull String marketId) {
        if (getMarketRegions().isEmpty()) return null;
        for (int i = -1; ++i < getMarketRegions().size(); ) {
            MarketRegion marketRegion = getMarketRegions().get(i);
            if (marketRegion.getMarketId().equalsIgnoreCase(marketId)) return marketRegion;
        }
        return null;
    }

    /**
     * Checks if a market region already exists with the passed id.
     *
     * @param marketId The id to check.
     * @return The result as a boolean value.
     */
    public boolean doesMarketRegionExist(@NotNull String marketId) {
        if (getMarketRegions().isEmpty()) return false;
        for (int i = -1; ++i < getMarketRegions().size(); ) {
            MarketRegion marketRegion = getMarketRegions().get(i);
            if (marketRegion.getMarketId().equalsIgnoreCase(marketId)) return true;
        }

        return false;
    }

    /**
     * Gets if a location is too close to a shop based on the distance value in the configuration.
     *
     * @param location The location to check around.
     * @return The result as a boolean.
     */
    public boolean isTooClose(@NotNull Location location) {
        if (location.getWorld() == null) return false;
        double distance = getPluginInstance().getConfig().getDouble("required-shop-distance");

        for (Shop shop : getShopMap().values()) {
            if (shop.getBaseLocation() != null && shop.getBaseLocation().getWorldName().equals(location.getWorld().getName())
                    && shop.getBaseLocation().distance(location, true) <= distance)
                return true;
        }

        return false;
    }

    /**
     * Finds the value a specific placeholder was replaced with given the original format, placeholder, and the ItemStack.
     *
     * @param itemStack   The ItemStack containing the formatted placeholder.
     * @param textFormat  The format with the non-replaced placeholder.
     * @param placeHolder The placeholder to get the value from.
     * @return The value found in the placeholders place (can return NULL).
     */
    public String getValueFromPlaceholder(@NotNull ItemStack itemStack, @NotNull Object textFormat, @NotNull String placeHolder) {
        final ItemMeta itemMeta = itemStack.getItemMeta();
        if (itemMeta == null) return null;

        if (textFormat instanceof String) { // displayName
            final String format = (String) textFormat;
            final String nFormat = ChatColor.stripColor(DisplayShops.getPluginInstance().getManager().color(format));
            if (nFormat.contains("{type}")) {
                String currentType = ChatColor.stripColor(itemMeta.getDisplayName());
                String[] args = nFormat.split(placeHolder.replace("{", "\\{"));
                if (args.length > 0) for (int i = -1; ++i < args.length; )
                    currentType = currentType.replace(args[0], "");

                if (!currentType.isEmpty()) return currentType;
            }
        } else if (textFormat instanceof List<?>) { // lore
            final List<?> formatList = (List<?>) textFormat;

            String lineWithPlaceholder = null;
            for (int i = -1; ++i < formatList.size(); ) {
                final Object cLine = formatList.get(i);
                if (cLine instanceof String) {
                    final String line = (String) cLine;
                    if (line.contains(placeHolder.replace("{", "\\{"))) {
                        lineWithPlaceholder = line;
                        break;
                    }
                }
            }

            if (lineWithPlaceholder == null) return null;

            final String format = ChatColor.stripColor(DisplayShops.getPluginInstance().getManager().color(lineWithPlaceholder));
            if (format.contains("{search-text}")) {
                String currentType = ChatColor.stripColor(itemMeta.getDisplayName());
                String[] args = lineWithPlaceholder.split(placeHolder);
                if (args.length > 0)
                    for (int i = -1; ++i < args.length; ) {
                        currentType = currentType.replace(args[0], "");
                    }
            }
        }

        return null;
    }

    /**
     * Gets the name of the item. If the item name is not custom, then either the original or translated name will be used.
     *
     * @param itemStack The item to get the name of.
     * @return The item name.
     */
    public String getItemName(@NotNull ItemStack itemStack) {
        if (itemStack.hasItemMeta() && itemStack.getItemMeta() != null && itemStack.getItemMeta().hasDisplayName())
            return itemStack.getItemMeta().getDisplayName();//.replace("\"", "\\\"").replace("'", "\\'");
        else
            return Ref.isNewerThan(12) ? getPluginInstance().getManager().getTranslatedName(itemStack.getType(), itemStack.getDurability())
                    : getPluginInstance().getManager().getTranslatedName(itemStack.getType());
        //.replace("\"", "\\\"").replace("'", "\\'");
    }

    /**
     * Obtains any translation created for the passed material found in the "lang.yml".
     *
     * @param material The material to obtain the translation for.
     * @param data     The durability/data value used in older versions (Optional, defaults to 0).
     * @return The translated version.
     */
    public String getTranslatedName(Material material, int... data) {
        final int durability = ((data.length > 0) ? data[0] : 0);
        ConfigurationSection cs = getPluginInstance().getLangConfig().getConfigurationSection("translated-material-names");
        if (cs != null) {
            Collection<String> keys = cs.getKeys(false);
            if (!keys.isEmpty())
                for (String key : keys) {
                    if (key.contains(":")) {
                        String[] args = key.split(":");
                        if (args[0].toUpperCase().replace(" ", "_").replace("-", "_")
                                .equalsIgnoreCase(material.name()) && String.valueOf(durability).equals(args[1]))
                            return cs.getString(key);
                    } else if (key.contains(",")) {
                        String[] args = key.split(",");
                        if (args[0].toUpperCase().replace(" ", "_").replace("-", "_")
                                .equalsIgnoreCase(material.name()) && String.valueOf(durability).equals(args[1]))
                            return cs.getString(key);
                    }

                    if (key.toUpperCase().replace(" ", "_").replace("-", "_").equalsIgnoreCase(material.name()))
                        return cs.getString(key);
                }
        }

        return WordUtils.capitalize(material.name().toLowerCase().replace("_", " "));
    }

    /**
     * Obtains any translation created for the passed enchantment found in the "lang.yml".
     *
     * @param enchantment The enchantment to obtain the translation for.
     * @return The translated version.
     */
    public String getTranslatedName(@NotNull Enchantment enchantment) {
        final boolean isNew = (Ref.isNewerThan(12));
        ConfigurationSection cs = getPluginInstance().getLangConfig().getConfigurationSection("translated-enchantment-names");
        if (cs != null) {
            Collection<String> keys = cs.getKeys(false);
            if (!keys.isEmpty())
                for (String key : keys) {
                    if (key.toUpperCase().replace(" ", "_").replace("-", "_").equalsIgnoreCase(enchantment.getName())) {
                        return cs.getString(key);
                    }
                }
        }

        return WordUtils.capitalize((isNew ? enchantment.getKey().getKey() : enchantment.getName()).toLowerCase().replace("_", " "));
    }

    /**
     * Obtains any translation created for the passed potion type found in the "translation.yml".
     *
     * @param potionType The potion type to obtain the translation for.
     * @return The translated version.
     */
    public String getTranslatedName(@NotNull PotionType potionType) {
        ConfigurationSection cs = getPluginInstance().getLangConfig().getConfigurationSection("translated-potion-names");
        if (cs != null) {
            Collection<String> keys = cs.getKeys(false);
            if (!keys.isEmpty())
                for (String key : keys) {
                    if (key.toUpperCase().replace(" ", "_").replace("-", "_").equalsIgnoreCase(potionType.name()))
                        return cs.getString(key);
                }
        }

        return WordUtils.capitalize(potionType.name().toLowerCase().replace("_", " "));
    }

    /**
     * Gets the names of the enchantments and formats them into a neat format.
     *
     * @param itemStack The item to get the enchants from.
     * @return The new formatted line.
     */
    public String getEnchantmentLine(@NotNull ItemStack itemStack) {
        StringBuilder enchantLine = new StringBuilder();

        int totalEnchantments, currentCount = 0, cutCount = getPluginInstance().getConfig().getInt("enchantment-cut-count");
        Set<Map.Entry<Enchantment, Integer>> enchantEntries;
        if (itemStack.getItemMeta() instanceof EnchantmentStorageMeta) {
            EnchantmentStorageMeta bookMeta = (EnchantmentStorageMeta) itemStack.getItemMeta();
            enchantEntries = bookMeta.getStoredEnchants().entrySet();
            totalEnchantments = bookMeta.getStoredEnchants().size();
        } else {
            enchantEntries = itemStack.getEnchantments().entrySet();
            totalEnchantments = itemStack.getEnchantments().size();
        }

        if (totalEnchantments > 0) for (Map.Entry<Enchantment, Integer> enchantEntry : enchantEntries) {
            if (enchantLine.length() > 0) enchantLine.append(", ");
            if (currentCount >= cutCount) break;
            enchantLine.append(color(getTranslatedName(enchantEntry.getKey()) + " " + getRomanNumeral(enchantEntry.getValue())));
            currentCount++;
        }

        final int remainder = (totalEnchantments - currentCount);
        return enchantLine + (remainder > 0 ? Objects.requireNonNull(getPluginInstance().getConfig().getString("enchantment-description-format"))
                .replace("{remainder}", getPluginInstance().getManager().formatNumber(remainder, false)) : "");
    }

    /**
     * Gets the names of the potion effects and formats them into a neat format.
     *
     * @param itemStack The item to get the potion effects from.
     * @return The new formatted line.
     */
    public String getPotionLine(@NotNull ItemStack itemStack) {
        if (!(itemStack.getItemMeta() instanceof PotionMeta)) return "";
        PotionMeta potionMeta = (PotionMeta) itemStack.getItemMeta();
        final int totalEffects = potionMeta.getCustomEffects().size();
        final String extended = getPluginInstance().getConfig().getString("potion-description-extended"),
                upgraded = getPluginInstance().getConfig().getString("potion-description-upgraded");

        if (getPluginInstance().getDisplayManager() != null) {
            boolean isExtended = false, isUpgraded = false;
            String potionName = Objects.requireNonNull(potionMeta.getBasePotionType()).name();

            if (potionName.startsWith("LONG")) {isExtended = true;}
            if (potionName.startsWith("STRONG")) {isUpgraded = true;}

            PotionType type = PotionType.valueOf(potionName.replace("LONG_", "").replace("STRONG_", ""));

            return (color(getTranslatedName(Objects.requireNonNull(type)) + (isExtended
                    ? (" " + extended) : "") + (isUpgraded ? (" " + upgraded) : ""))
                    + (totalEffects > 0 ? (" " + getPluginInstance().getConfig().getString("potion-description-format"))
                    .replace("{remainder}", getPluginInstance().getManager().formatNumber(totalEffects, false)) : ""));
        }

        return (color(getTranslatedName(Objects.requireNonNull(potionMeta.getBasePotionData()).getType()) + (potionMeta.getBasePotionData().isExtended()
                ? (" " + extended) : "") + (potionMeta.getBasePotionData().isUpgraded() ? (" " + upgraded) : ""))
                + (totalEffects > 0 ? (" " + getPluginInstance().getConfig().getString("potion-description-format"))
                .replace("{remainder}", getPluginInstance().getManager().formatNumber(totalEffects, false)) : ""));
    }

    /**
     * Gets roman numeral value from 1 - 10.
     *
     * @param number The number between 1 and 10.
     * @return The roman numeral.
     */
    public String getRomanNumeral(int number) {
        switch (number) {
            case 2:
                return "II";
            case 3:
                return "III";
            case 4:
                return "IV";
            case 5:
                return "V";
            case 6:
                return "VI";
            case 7:
                return "VII";
            case 8:
                return "VIII";
            case 9:
                return "IX";
            case 10:
                return "X";
            default:
                return "I";
        }
    }

    /**
     * Colors the text passed.
     *
     * @param message The message to translate.
     * @return The colored text.
     */
    public String color(@NotNull String message) {
        if (message.isEmpty()) return message;
        if (Ref.isNewerThan(15)) {
            Matcher matcher = hexPattern.matcher(message);
            while (matcher.find()) {
                final ChatColor hexColor = ChatColor.of(matcher.group());
                final String before = message.substring(0, matcher.start()), after = message.substring(matcher.end());
                matcher = hexPattern.matcher(message = (before + hexColor + after));
            }
        }
        return ChatColor.translateAlternateColorCodes('&', message);
    }

    /**
     * Formats a given number based on the locale.
     *
     * @param value     The numerical value.
     * @param isDecimal Whether the number contains a decimal.
     * @return The formatted number as a string.
     */
    public String formatNumber(double value, boolean isDecimal) {
        String formatted;
        final int decimalPlaces = getPluginInstance().getConfig().getInt("minimum-fraction-digits");
        final boolean useUKFormatting = getPluginInstance().getConfig().getBoolean("use-uk-format");
        if (decimalPlaces > 0 && isDecimal) formatted = String.format(("%,." + decimalPlaces + "f"), value);
        else formatted = String.format("%,.0f", value);

        formatted = formatted.replace("\\s", "").replace("_", "");

        return (getPluginInstance().getConfig().getBoolean("short-number-format")
                ? format((long) Double.parseDouble(formatted.replace(",", "")), useUKFormatting)
                : (useUKFormatting ? formatted.replace(".", "_COMMA_").replace(",", "_PERIOD_")
                .replace("_PERIOD_", ".").replace("_COMMA_", ",") : formatted));
    }

    public String format(long value, boolean useUKFormatting) {
        String[] arr = {"", "K", "M", "B", "T", "Q", "E"};
        int index = 0;
        while ((value / 1000) >= 1) {
            value = value / 1000;
            index++;
        }

        DecimalFormat decimalFormat = new DecimalFormat("#.##");
        String format = String.format("%s%s", decimalFormat.format(value), arr[index]);
        return (useUKFormatting ? format.replace(".", ",") : format);
    }

    /**
     * Gets the max buy or sell price of a selected material.
     *
     * @param itemStack The item stack to check for.
     * @param isBuy     Whether it is buy or sell.
     * @return The found maximum.
     */
    public double getMaterialMaxPrice(@NotNull ItemStack itemStack, boolean isBuy) {
        ConfigurationSection maxSection = getPluginInstance().getConfig().getConfigurationSection("max-material-prices");
        if (maxSection != null) for (String keyName : maxSection.getKeys(false)) {
            if (keyName.toUpperCase().replace(" ", "_").replace("-", "_").equals(itemStack.getType().name())
                    || (Objects.requireNonNull(itemStack.getItemMeta()).hasDisplayName() && color(keyName).equals(itemStack.getItemMeta().getDisplayName())))
                return (isBuy ? new BigDecimal(Objects.requireNonNull(getPluginInstance().getConfig().getString("max-material-prices." + keyName +
                        ".buy"))).doubleValue()
                        : new BigDecimal(Objects.requireNonNull(getPluginInstance().getConfig().getString("max-material-prices." + keyName + ".sell"
                ))).doubleValue());
        }
        return (isBuy ? new BigDecimal(Objects.requireNonNull(getPluginInstance().getConfig().getString("buy-price-limit"))).doubleValue()
                : new BigDecimal(Objects.requireNonNull(getPluginInstance().getConfig().getString("sell-price-limit"))).doubleValue());
    }

    /**
     * Gets the min buy or sell price of a selected material.
     *
     * @param itemStack The item stack to check for.
     * @param isBuy     Whether it is buy or sell.
     * @return The found minimum.
     */
    public double getMaterialMinPrice(@NotNull ItemStack itemStack, boolean isBuy) {
        ConfigurationSection maxSection = getPluginInstance().getConfig().getConfigurationSection("min-material-prices");
        if (maxSection != null) for (String keyName : maxSection.getKeys(false))
            if (keyName.toUpperCase().replace(" ", "_").replace("-", "_").equals(itemStack.getType().name())
                    || (Objects.requireNonNull(itemStack.getItemMeta()).hasDisplayName() && color(keyName).equals(itemStack.getItemMeta().getDisplayName())))
                return (isBuy ? new BigDecimal(Objects.requireNonNull(getPluginInstance().getConfig().getString("min-material-prices." + keyName +
                        ".buy"))).doubleValue()
                        : new BigDecimal(Objects.requireNonNull(getPluginInstance().getConfig().getString("min-material-prices." + keyName + ".sell"
                ))).doubleValue());
        return -1;
    }

    /**
     * Checks if a shop already exists with the passed id.
     *
     * @param shopId The id to check for.
     * @return Whether the id exists in true or false format.
     */
    public boolean doesShopIdExist(@NotNull UUID shopId) {
        for (Shop shop : getShopMap().values()) {
            if (shop.getShopId().toString().equals(shopId.toString())) return true;
        }
        return false;
    }

    /**
     * Generates a new ID suited for a new display shop.
     *
     * @return a long value for the ID.
     */
    public UUID generateNewId() {
        UUID generatedId;

        do {
            generatedId = UUID.randomUUID();
        } while (doesShopIdExist(generatedId));

        return generatedId;
    }

    /**
     * Attempts to get a shop object from the shop map by its ID.
     *
     * @param shopId The ID to get the shop from
     * @return the shop object. (Can return NULL if the object does not exist)
     */
    public Shop getShopById(@NotNull UUID shopId) {
        for (Shop shop : getShopMap().values()) {
            if (shop.getShopId().toString().equals(shopId.toString())) return shop;
        }
        return null;
    }

    /**
     * Get a shop from the passed chest if possible.
     *
     * @param location Chest location.
     * @return The shop if found.
     */
    public Shop getShop(@NotNull Location location) {
        final String locationAsString = getLocationAsString(location);
        return getShopMap().getOrDefault(locationAsString, null);
    }

    public String getLocationAsString(@NotNull Location location) {
        return Objects.requireNonNull(location.getWorld()).getName() + "," + location.getBlockX() + "," + location.getBlockY() + "," + location.getBlockZ();
    }

    /**
     * Creates a shop at the passed block owned by the passed player.
     *
     * @param player              The player to use as the owner.
     * @param block               The block to create the shop at.
     * @param shopItemAmount      The amount used for the shop item.
     * @param doCreationEffects   Whether to play the effects and sounds.
     * @param sendCreationMessage Whether to send the creation message.
     * @return The shop object.
     */
    public Shop createShop(@NotNull Player player, @NotNull Block block, int shopItemAmount, boolean doCreationEffects, boolean sendCreationMessage) {
        Menu appearanceMenu = getPluginInstance().getMenu("appearance");
        DShop shop = new DShop(getPluginInstance().getManager().generateNewId(), player.getUniqueId(), block.getLocation(), shopItemAmount,
                (appearanceMenu != null ? appearanceMenu.getConfiguration().getString("default-appearance") : "Default"));
        shop.register();
        shop.updateBaseBlockDirection(player);

        if (doCreationEffects) {
            String soundString = getPluginInstance().getConfig().getString("immersion-section.shop-creation-sound");
            if (soundString != null && !soundString.equalsIgnoreCase(""))
                block.getWorld().playSound(block.getLocation(),
                        Sound.valueOf(soundString), 1, 1);

            String particleString = getPluginInstance().getConfig().getString("immersion-section.shop-creation-particle");
            if (particleString != null && !particleString.equalsIgnoreCase(""))
                getPluginInstance().displayParticle(player, particleString.toUpperCase()
                                .replace(" ", "_").replace("-", "_"),
                        block.getLocation().add(0.5, 0.5, 0.5), 0.5, 0.5, 0.5, 0, 12);
        }

        if (sendCreationMessage) {
            String message = getPluginInstance().getLangConfig().getString("shop-created");
            if (message != null && !message.equalsIgnoreCase(""))
                getPluginInstance().getManager().sendMessage(player, message);
        }

        return shop;
    }

    /**
     * Attempts to load all available shops from the database.
     *
     * @param isAsync Whether the task is ran on the main thread.
     * @param cleanUp Deletes all shops that have invalid data (Example: No longer existing worlds).
     */
    public void loadShops(boolean isAsync, boolean cleanUp) {

        List<String> shopsToDelete = new ArrayList<>();
        long loadedShops = 0, current = 0, shopCount = 0, shopCountPercentage = 0;

        // begins loading from SQLite.
        try (PreparedStatement statement = getPluginInstance().getDatabaseConnection().prepareStatement("SELECT Count(*) FROM shops;")) {
            try (ResultSet resultSet = statement.executeQuery()) {
                shopCount = (resultSet.next() ? resultSet.getInt(1) : 0);
                shopCountPercentage = ((long) (shopCount * 0.15));
            } catch (SQLException ex) {
                getPluginInstance().log(Level.WARNING, "Error counting shops from database (" + ex.getMessage() + ").");
            }
        } catch (SQLException e) {
            getPluginInstance().log(Level.WARNING, "An error occurred during shop load time (" + e.getMessage() + ").");
        }

        Menu appearanceMenu = getPluginInstance().getMenu("appearance");
        String defaultAppearance = appearanceMenu.getConfiguration().getString("default-appearance");
        try (PreparedStatement preparedStatement = getPluginInstance().getDatabaseConnection().prepareStatement("SELECT * FROM shops;");
             ResultSet resultSet = preparedStatement.executeQuery()) {
            while (resultSet.next()) {
                try {
                    UUID shopId;
                    final String shopIdString = resultSet.getString("id");
                    if (shopIdString == null || shopIdString.equalsIgnoreCase("")) {
                        if (cleanUp) {
                            PreparedStatement deleteStatement = getPluginInstance().getDatabaseConnection()
                                    .prepareStatement("DELETE FROM shops where id = '" + shopIdString + "';");
                            deleteStatement.executeUpdate();
                            getPluginInstance().log(Level.INFO, "[CLEANING] A shop was removed due to invalid id.");
                            deleteStatement.close();
                            continue;
                        }

                        shopId = getPluginInstance().getManager().generateNewId();
                        getPluginInstance().log(Level.INFO, "Provided an existing shop with the new id \"" + shopId + "\".");
                    } else shopId = UUID.fromString(shopIdString);

                    UUID ownerId = null;
                    final String ownerIdString = resultSet.getString("owner");
                    if (ownerIdString != null && !ownerIdString.isEmpty()) ownerId = UUID.fromString(ownerIdString);

                    final int cleanInactiveTime = getPluginInstance().getConfig().getInt("clean-inactive-duration");
                    boolean claimable = false;
                    if (cleanInactiveTime >= 0 && ownerId != null) {
                        OfflinePlayer offlinePlayer = getPluginInstance().getServer().getOfflinePlayer(ownerId);
                        final long lastOnline = (((System.currentTimeMillis() - offlinePlayer.getLastSeen()) / 1000));
                        if (lastOnline >= cleanInactiveTime) {
                            if (getPluginInstance().getConfig().getBoolean("claimable-system", false)) {
                                claimable = true;
                            } else {
                                shopsToDelete.add(shopIdString);
                                continue;
                            }
                        }
                    }

                    final String baseLocationString = resultSet.getString((!getPluginInstance().hasColumn(resultSet, "location") ? "base_" : "") + "location");
                    String appearance;
                    LClone baseLocation;
                    if (baseLocationString.contains(":")) {
                        final String[] locationStringArgsOuter = baseLocationString.split(":"),
                                locationStringArgs = locationStringArgsOuter[locationStringArgsOuter.length - 1].replace("/0", "").split(",");

                        baseLocation = new LClone(locationStringArgs[0], Double.parseDouble(locationStringArgs[1]),
                                Double.parseDouble(locationStringArgs[2]), Double.parseDouble(locationStringArgs[3]),
                                Float.parseFloat(locationStringArgs[4]), Float.parseFloat(locationStringArgs[5]));

                        appearance = Appearance.findAppearance(locationStringArgsOuter[0].replace(",", ":"));
                    } else {
                        final String[] locationStringArgs = baseLocationString.split(",");
                        baseLocation = new LClone(locationStringArgs[0], Double.parseDouble(locationStringArgs[1]),
                                Double.parseDouble(locationStringArgs[2]), Double.parseDouble(locationStringArgs[3]),
                                Float.parseFloat(locationStringArgs[4]), Float.parseFloat(locationStringArgs[5]));

                        appearance = resultSet.getString("appearance");
                    }

                    World world = getPluginInstance().getServer().getWorld(baseLocation.getWorldName());
                    if (world == null) {
                        if (cleanUp) {
                            Statement deleteStatement = getPluginInstance().getDatabaseConnection().createStatement();
                            deleteStatement.executeUpdate("DELETE FROM shops where id = '" + shopIdString + "';");
                            getPluginInstance().log(Level.INFO, "[CLEANING] The shop \"" + shopIdString
                                    + "\" was removed due to invalid world \"" + baseLocation.getWorldName() + "\".");
                            continue;
                        }

                        getPluginInstance().log(Level.INFO,
                                "The shop \"" + shopIdString + "\" has an invalid world \"" + baseLocation.getWorldName() + "\". Skipping...");
                        continue;
                    }

                    final String shopItemString = resultSet.getString("shop_item"),
                            tradeItemString = resultSet.getString("trade_item");

                    ItemStack shopItem, tradeItem;
                    shopItem = tradeItem = null;

                    if (shopItemString != null && !shopItemString.equalsIgnoreCase("")) {
                        shopItem = getPluginInstance().toItem(shopItemString);
                    }

                    if (tradeItemString != null && !tradeItemString.equalsIgnoreCase("")) {
                        tradeItem = getPluginInstance().toItem(tradeItemString);
                    }

                    if (appearance == null || appearance.isEmpty() || Appearance.getAppearance(appearance) == null)
                        appearance = (defaultAppearance != null ? defaultAppearance : "Default");

                    DShop shop = new DShop(shopId, ownerId, shopItem, baseLocation, resultSet.getInt("shop_item_amount"), appearance);
                    shop.setTradeItem(tradeItem);
                    shop.setDescription(resultSet.getString("description"));
                    shop.setBuyPrice(resultSet.getDouble("buy_price"));
                    shop.setSellPrice(resultSet.getDouble("sell_price"));
                    shop.setStoredBalance(resultSet.getDouble("balance"));

                    // get limits
                    String limitsString = resultSet.getString("limits");
                    String[] limitArgs = limitsString.split(";");

                    // update limit values
                    shop.setGlobalBuyLimit(Integer.parseInt(limitArgs[0]));
                    shop.setGlobalBuyCounter(Integer.parseInt(limitArgs[1]));
                    shop.setGlobalSellLimit(Integer.parseInt(limitArgs[2]));
                    shop.setGlobalSellCounter(Integer.parseInt(limitArgs[3]));
                    shop.setPlayerBuyLimit(Integer.parseInt(limitArgs[4]));
                    shop.setPlayerSellLimit(Integer.parseInt(limitArgs[5]));

                    String changeStamp = resultSet.getString("change_time_stamp");
                    if (changeStamp != null && !changeStamp.contains("."))
                        shop.setChangeTimeStamp(Long.parseLong(changeStamp));

                    shop.setCommandOnlyMode(resultSet.getBoolean("command_only_mode"));
                    shop.setStock(resultSet.getInt("stock"));


                    shop.setClaimable(claimable);
                    if (shop.isClaimable()) {
                        if ((shop.getStock() == 0 && shop.getStoredBalance() == 0) || shop.getShopItem() == null) {
                            shopsToDelete.add(shopIdString);
                            continue;
                        }
                    }

                    final String assistantsLine = resultSet.getString("assistants");
                    if (assistantsLine != null && !assistantsLine.isEmpty())
                        if (assistantsLine.contains(";")) {
                            String[] assistantArgs = assistantsLine.split(";");
                            for (String playerUniqueId : assistantArgs)
                                shop.getAssistants().add(UUID.fromString(playerUniqueId));
                        } else shop.getAssistants().add(UUID.fromString(assistantsLine));

                    final String commandsLine = resultSet.getString("commands");
                    if (commandsLine != null && !commandsLine.equalsIgnoreCase(""))
                        if (commandsLine.contains(";")) {
                            final String[] commandsSplit = commandsLine.split(";");
                            for (String command : commandsSplit) shop.getCommands().add(command);
                        } else shop.getCommands().add(commandsLine);

                    String extraDataLine = resultSet.getString("extra_data");
                    if (extraDataLine != null && !extraDataLine.isEmpty())
                        if (extraDataLine.contains(";")) {
                            final String[] extraDataArgs = extraDataLine.split(";");

                            shop.setDynamicPriceChange(Boolean.parseBoolean(extraDataArgs[0]));

                            if (extraDataArgs.length > 3) shop.setCurrencyType(extraDataArgs[3]);
                            shop.checkCurrencyType(null);

                            if (shop.getCurrencyType().equalsIgnoreCase("item-for-item")
                                    && getPluginInstance().getConfig().getBoolean("shop-currency-item.force-use")) {
                                shop.setTradeItem(getPluginInstance().getManager().defaultCurrencyItem.clone());
                            }

                            if (extraDataArgs.length > 2 && extraDataLine.contains(":")) {
                                String[] buySplit = extraDataArgs[1].split(":"), sellSplit = extraDataArgs[2].split(":");

                                String lastBuyStamp = buySplit[0];
                                if (lastBuyStamp != null && changeStamp != null && !changeStamp.contains("."))
                                    shop.setLastBuyTimeStamp(Long.parseLong(lastBuyStamp));
                                shop.setDynamicBuyCounter(Integer.parseInt(buySplit[1]));

                                String lastSellStamp = sellSplit[0];
                                if (lastSellStamp != null && changeStamp != null && !changeStamp.contains("."))
                                    shop.setLastSellTimeStamp(Long.parseLong(lastSellStamp));
                                shop.setDynamicSellCounter(Integer.parseInt(sellSplit[1]));
                            }

                        } else shop.setDynamicPriceChange(Boolean.parseBoolean(extraDataLine));

                    shop.register();
                    loadedShops++;

                    if (getPluginInstance().getConfig().getBoolean("fix-above-blocks")) {
                        if (isAsync) {
                            getPluginInstance().getServer().getScheduler().runTask(getPluginInstance(), () -> fixAboveBlock(shop));
                        } else fixAboveBlock(shop);
                    }

                    current++;
                    if ((shopCountPercentage <= 0 || current % shopCountPercentage == 0 || current == shopCount))
                        getPluginInstance().log(Level.INFO, "Loading shops " + current + "/" + shopCount
                                + " (" + Math.min(100, (int) (((double) current / (double) shopCount) * 100)) + "%)...");
                } catch (Exception e) {
                    getPluginInstance().log(Level.WARNING, "The shop '" + resultSet.getString("id") + "' failed to load(" + e.getMessage() + ").");
                }
            }
        } catch (SQLException ex) {
            getPluginInstance().log(Level.WARNING, "Error retrieving shops from database (" + ex.getMessage() + ").");
        }

        if (!shopsToDelete.isEmpty()) {
            for (String id : shopsToDelete) {
                try (PreparedStatement statement = getPluginInstance().getDatabaseConnection().prepareStatement("DELETE FROM shops WHERE id = '" + id + "';")) {
                    statement.executeUpdate();
                } catch (SQLException e) {
                    getPluginInstance().log(Level.WARNING, "An error occurred during shop load time (" + e.getMessage() + ").");
                }
            }
        }

        if (loadedShops <= 0) getPluginInstance().log(Level.INFO, "No shops were found.");
    }

    private void fixAboveBlock(DShop shop) {
        final Location location = shop.getBaseLocation().asBukkitLocation();
        if (location == null) return;

        final BlockState blockState = location.getBlock().getRelative(BlockFace.UP).getState();
        blockState.setType(Material.AIR);
        blockState.update(true, false);
    }

    /**
     * Checks to see if the material is blocked or not.
     *
     * @param material The material to check for.
     * @return The result.
     */
    public boolean isBlockedMaterial(@NotNull Material material) {
        List<String> materialList = getPluginInstance().getConfig().getStringList("blocked-material-list");
        for (int i = -1; ++i < materialList.size(); ) {
            if (material.name().equalsIgnoreCase(materialList.get(i)
                    .replace(" ", "_").replace("-", "_"))) return true;
        }

        return false;
    }

    /**
     * Saves all market regions to the database.
     */
    public synchronized void saveMarketRegions() {
        getPluginInstance().getManager().getMarketRegions().parallelStream().forEach(marketRegion -> marketRegion.save(false));
    }

    /**
     * Loads all market regions into the memory.
     *
     * @param cleanUp Deletes all market regions that have invalid data (Example: No longer existing worlds).
     */
    public void loadMarketRegions(boolean cleanUp) {
        long startTime = System.currentTimeMillis();
        int loadedRegions = 0, failedToLoadRegions = 0;

        File file = new File(getPluginInstance().getDataFolder(), "/market-regions.yml");
        FileConfiguration yaml = YamlConfiguration.loadConfiguration(file);

        ConfigurationSection marketIdsSection = yaml.getConfigurationSection("");
        if (marketIdsSection != null)
            for (String marketId : marketIdsSection.getKeys(false)) {
                DRegion region = new DRegion();

                String worldOne = yaml.getString(marketId + ".point-one.world"), worldTwo = yaml.getString(marketId + ".point-two.world");
                if (worldOne == null || worldOne.equalsIgnoreCase("") || worldTwo == null || worldTwo.equalsIgnoreCase(""))
                    continue;

                region.setPointOne(new LClone(worldOne, yaml.getDouble(marketId + ".point-one.x"),
                        yaml.getDouble(marketId + ".point-one.y"), yaml.getDouble(marketId + ".point-one.z"), 0, 0));
                region.setPointTwo(new LClone(worldTwo, yaml.getDouble(marketId + ".point-two.x"),
                        yaml.getDouble(marketId + ".point-two.y"), yaml.getDouble(marketId + ".point-two.z"), 0, 0));


                getMarketRegions().add(new MRegion(getPluginInstance(), marketId, region));
                loadedRegions++;
            }

        file.renameTo(new File(getPluginInstance().getDataFolder(), "/old-mr.yml"));

        try (PreparedStatement statement = getPluginInstance().getDatabaseConnection().prepareStatement("SELECT * FROM market_regions;");
             ResultSet resultSet = statement.executeQuery()) {
            while (resultSet.next()) {
                final String regionId = resultSet.getString("id");
                if (regionId == null || regionId.equalsIgnoreCase("")) {

                    if (cleanUp) {
                        Statement deleteStatement = getPluginInstance().getDatabaseConnection().createStatement();
                        deleteStatement.executeUpdate("DELETE FROM market_regions where id = '" + regionId + "';");
                        getPluginInstance().log(Level.WARNING, "[CLEANING] A market region was removed due an invalid id.");
                    }

                    continue;
                }

                if (doesMarketRegionExist(regionId)) continue;
                final String pointOneString = resultSet.getString("point_one"),
                        pointTwoString = resultSet.getString("point_two");
                if (pointOneString.contains(",") && pointTwoString.contains(",")) {
                    final String[] pointOneArgs = pointOneString.split(","), pointTwoArgs = pointTwoString.split(",");
                    final LClone pointOne = new LClone(pointOneArgs[0], Double.parseDouble(pointOneArgs[1]), Double.parseDouble(pointOneArgs[2]),
                            Double.parseDouble(pointOneArgs[3]), Float.parseFloat(pointOneArgs[4]), Float.parseFloat(pointOneArgs[5])),
                            pointTwo = new LClone(pointTwoArgs[0], Double.parseDouble(pointTwoArgs[1]), Double.parseDouble(pointTwoArgs[2]),
                                    Double.parseDouble(pointTwoArgs[3]), Float.parseFloat(pointTwoArgs[4]), Float.parseFloat(pointTwoArgs[5]));

                    if (getPluginInstance().getServer().getWorld(pointOne.getWorldName()) == null
                            || getPluginInstance().getServer().getWorld(pointTwo.getWorldName()) == null) {
                        if (cleanUp) {
                            Statement deleteStatement = getPluginInstance().getDatabaseConnection().createStatement();
                            deleteStatement.executeUpdate("DELETE FROM market_regions where id = '" + regionId + "';");
                            getPluginInstance().log(Level.WARNING, "[CLEANING] The market region \"" + regionId
                                    + "\" was removed due an invalid world that possibly no longer exists.");
                        }

                        continue;
                    }

                    if (pointOne.getWorldName() == null || pointOne.getWorldName().equalsIgnoreCase("")
                            || pointTwo.getWorldName() == null || pointTwo.getWorldName().equalsIgnoreCase("")
                            || !pointOne.getWorldName().equalsIgnoreCase(pointTwo.getWorldName())) {

                        if (cleanUp) {
                            Statement deleteStatement = getPluginInstance().getDatabaseConnection().createStatement();
                            deleteStatement.executeUpdate("DELETE FROM market_regions where id = '" + regionId + "';");
                            getPluginInstance().log(Level.WARNING, "[CLEANING] The market region \"" + regionId
                                    + "\" was removed due to mismatching region point worlds.");
                        }

                        continue;
                    }

                    DRegion region = new DRegion();
                    region.setPointOne(pointOne);
                    region.setPointTwo(pointTwo);

                    MRegion marketRegion = new MRegion(getPluginInstance(), regionId, region);

                    String renterUniqueId = resultSet.getString("renter");
                    if (renterUniqueId != null && !renterUniqueId.isEmpty()) {
                        final UUID renter = UUID.fromString(renterUniqueId);
                        marketRegion.setRenter(renter);
                    }

                    marketRegion.setExtendedDuration(resultSet.getInt("extended_duration"));
                    marketRegion.setRentedTimeStamp(resultSet.getString("rent_time_stamp"));

                    final String extraDataLine = resultSet.getString("extra_data");
                    if (extraDataLine != null && !extraDataLine.isEmpty()) {
                        final String[] args = extraDataLine.split(",");
                        marketRegion.setCost(Double.parseDouble(args[0]));
                        marketRegion.setRenewCost(Double.parseDouble(args[1]));
                    }

                    getPluginInstance().getManager().getMarketRegions().add(marketRegion);
                    loadedRegions++;
                }
            }
        } catch (SQLException e) {
            failedToLoadRegions += 1;
            e.printStackTrace();
            getPluginInstance().log(Level.WARNING,
                    "This is just a warning stating that a market region has failed to load.");
        }

        if (loadedRegions > 0 || failedToLoadRegions > 0)
            getPluginInstance().log(Level.INFO, loadedRegions + " " + ((loadedRegions == 1)
                    ? "market region was" : "market regions were") + " loaded and " + failedToLoadRegions
                    + " " + ((failedToLoadRegions == 1) ? "market region" : "market regions")
                    + " failed to load. (Took " + (System.currentTimeMillis() - startTime) + "ms)");
    }

    /**
     * Obtains the default base material without checking attached durability.
     *
     * @return The material type.
     */
    public Material getBaseBlockType() {
        final String material = getPluginInstance().getConfig().getString("shop-block-material");
        if (material != null && !material.isEmpty()) {
            if (material.contains(":"))
                return Material.getMaterial(material.split(":")[0].toUpperCase()
                        .replace(" ", "_").replace("-", "_"));
            else return Material.getMaterial(material.toUpperCase().replace(" ", "_").replace("-", "_"));
        }
        return null;
    }

    /**
     * Returns a list of all shops owned by the player.
     *
     * @param player The player to check for.
     * @return The list of shops the player owns.
     */
    public List<Shop> getPlayerShops(@NotNull Player player) {
        List<Shop> shopList = new ArrayList<>();
        for (Shop shop : getShopMap().values())
            if (shop.getOwnerUniqueId() != null && shop.getOwnerUniqueId().toString().equalsIgnoreCase(player.getUniqueId().toString()))
                shopList.add(shop);

        return shopList;
    }

    /**
     * Gets if the player has reached the maximum amount of shops they can have.
     *
     * @param player The player to check.
     * @return Whether the player has exceeded their limit or not.
     */
    public boolean exceededShopLimit(Player player) {
        if (player.hasPermission("displayshops.limit.*")) return false;
        final List<Shop> shopList = getPlayerShops(player);
        final int shopLimit = getShopLimit(player);
        return (shopList.size() >= shopLimit);
    }

    /**
     * Gets the shop limit of the passed player.
     *
     * @param player The player to check for.
     * @return The limit they have on shops.
     */
    public int getShopLimit(Player player) {
        if (player.hasPermission("displayshops.limit.*")) return -1;
        int shopLimit = getPluginInstance().getConfig().getInt("default-shop-limit");
        for (PermissionAttachmentInfo permissionAttachmentInfo : player.getEffectivePermissions()) {
            if (permissionAttachmentInfo.getPermission().toLowerCase().startsWith("displayshops.limit.")) {
                String intValue = permissionAttachmentInfo.getPermission().toLowerCase().replace("displayshops.limit.", "");
                if (isNotNumeric(intValue)) continue;

                int tempValue = Integer.parseInt(permissionAttachmentInfo.getPermission().toLowerCase().replace("displayshops.limit.", ""));
                if (tempValue > shopLimit) shopLimit = tempValue;
            }
        }

        return shopLimit;
    }

    /**
     * Returns a list of all regions rented by the player.
     *
     * @param player The player to check for.
     * @return The list of rented regions.
     */
    public List<MarketRegion> getMarketRegions(@NotNull Player player) {
        List<MarketRegion> regionList = new ArrayList<>();
        for (MarketRegion marketRegion : getMarketRegions())
            if (marketRegion.getRenter() != null && marketRegion.getRenter().toString()
                    .equalsIgnoreCase(player.getUniqueId().toString())) regionList.add(marketRegion);

        return regionList;
    }

    /**
     * Gets if the player has reached the maximum amount of rented regions they can have.
     *
     * @param player The player to check.
     * @return Whether the player has exceeded their limit or not.
     */
    public boolean exceededMarketRegionLimit(Player player) {
        if (player.hasPermission("displayshops.rlimit.*")) return false;
        final List<MarketRegion> regionList = getMarketRegions(player);
        final int regionLimit = getMarketRegionLimit(player);
        return (regionList.size() >= regionLimit);
    }

    /**
     * Gets the rented region limit of the passed player.
     *
     * @param player The player to check for.
     * @return The limit they have on rented regions.
     */
    public int getMarketRegionLimit(Player player) {
        if (player.hasPermission("displayshops.rlimit.*")) return -1;
        int regionLimit = getPluginInstance().getConfig().getInt("default-region-limit");
        for (PermissionAttachmentInfo permissionAttachmentInfo : player.getEffectivePermissions()) {
            if (permissionAttachmentInfo.getPermission().toLowerCase().startsWith("displayshops.rlimit.")) {
                String intValue = permissionAttachmentInfo.getPermission().toLowerCase().replace("displayshops.rlimit.", "");
                if (isNotNumeric(intValue)) continue;

                int tempValue = Integer.parseInt(permissionAttachmentInfo.getPermission().toLowerCase().replace("displayshops.rlimit.", ""));
                if (tempValue > regionLimit) regionLimit = tempValue;
            }
        }

        return regionLimit;
    }

    /**
     * Gets the player specific shop promotion item modifier.
     *
     * @param player The player to check.
     * @return The modifier for multiplication.
     */
    public double getPromotionPriceModifier(Player player) {
        double modifier = 1;
        for (PermissionAttachmentInfo permissionAttachmentInfo : player.getEffectivePermissions()) {
            if (permissionAttachmentInfo.getPermission().toLowerCase().startsWith("displayshops.pm.")) {
                String intValue = permissionAttachmentInfo.getPermission().toLowerCase().replace("displayshops.pm.", "");
                if (isNotNumeric(intValue)) continue;

                double tempValue = Double.parseDouble(permissionAttachmentInfo.getPermission().toLowerCase().replace("displayshops.pm.", ""));
                if (tempValue > modifier) modifier = tempValue;
            }
        }

        return modifier;
    }

    /**
     * Obtains available space for a defined item.
     *
     * @param player    The player whose inventory needs to be checked.
     * @param itemStack The itemstack to check for.
     * @return The total available item amount space.
     */
    public int getInventorySpaceForItem(@NotNull Player player, @NotNull ItemStack itemStack) {
        int availableSpace = 0;

        if (Ref.isNewerThan(8)) {
            ItemStack[] contents = player.getInventory().getStorageContents();
            for (int i = -1; ++i < contents.length; ) {
                final ItemStack item = contents[i];
                if (item == null || item.getType().name().contains("AIR")) {
                    availableSpace += itemStack.getType().getMaxStackSize();
                    continue;
                }

                if (itemStack.isSimilar(item))
                    availableSpace += Math.max(0, (itemStack.getType().getMaxStackSize() - item.getAmount()));
            }
            return availableSpace;
        }

        for (int i = 8; ++i < Math.min(player.getInventory().getSize(), 44); ) {
            final ItemStack item = player.getInventory().getItem(i);
            if (item == null || item.getType().name().contains("AIR"))
                availableSpace += itemStack.getType().getMaxStackSize();
            else if (itemStack.isSimilar(item))
                availableSpace += Math.max(0, (itemStack.getType().getMaxStackSize() - item.getAmount()));
        }

        return availableSpace;
    }

    /**
     * Gives the passed item stack to the player the passed amount of times in the form of stacks.
     *
     * @param player           The player to give the item to.
     * @param itemStackToClone The item stack to make the stacks from.
     * @param amount           The amount of the items to give to the player (NOT stacks).
     */
    public void giveItemStacks(@NotNull Player player, @NotNull ItemStack itemStackToClone, int amount) {
        if (amount <= 0) return;

        ItemStack clone = itemStackToClone.clone();
        clone.setAmount(itemStackToClone.getAmount() * amount);
        FixedMetadataValue fixedMetadataValue = new FixedMetadataValue(getPluginInstance(), player.getUniqueId().toString());

        if (itemStackToClone.getType().getMaxStackSize() == 1) {
            ItemStack itemStack = itemStackToClone.clone();
            itemStack.setAmount(1);
            for (int i = -1; ++i < amount; ) {
                if (player.getInventory().firstEmpty() == -1) {
                    Item item = player.getWorld().dropItemNaturally(player.getLocation(), itemStack);
                    item.setMetadata("shop-owners", fixedMetadataValue);
                } else player.getInventory().addItem(itemStack);
            }
            return;
        }

        int stacks = (amount / itemStackToClone.getType().getMaxStackSize()),
                remainder = (amount % itemStackToClone.getType().getMaxStackSize());
        if (stacks <= 0 && remainder == 0) return;

        if (stacks > 0) {
            ItemStack itemStack = itemStackToClone.clone();
            for (int i = -1; ++i < stacks; ) {
                itemStack.setAmount(itemStack.getType().getMaxStackSize());
                if (player.getInventory().firstEmpty() == -1) {
                    Item item = player.getWorld().dropItemNaturally(player.getLocation(), itemStack);
                    item.setMetadata("shop-owners", fixedMetadataValue);
                } else player.getInventory().addItem(itemStack);
            }
        }

        if (remainder > 0) {
            ItemStack itemStack = itemStackToClone.clone();
            itemStack.setAmount(remainder);
            if (player.getInventory().firstEmpty() == -1) {
                Item item = player.getWorld().dropItemNaturally(player.getLocation(), itemStack);
                item.setMetadata("shop-owners", fixedMetadataValue);
            } else player.getInventory().addItem(itemStack);
        }
    }

    /**
     * Remove a certain amount of a certain similar item.
     *
     * @param inventory The inventory to remove from.
     * @param itemStack The item to check for.
     * @param amount    The amount to remove.
     * @return Whether an item was removed or not.
     */
    public boolean removeItem(@NotNull Inventory inventory, @NotNull ItemStack itemStack, int amount) {
        int left = amount;
        boolean removedItems = false;

        for (int i = -1; ++i < inventory.getSize(); ) {
            ItemStack is = inventory.getItem(i);
            if (is == null) continue;

            if (isSimilar(is, itemStack)) {
                if (left >= is.getAmount()) {
                    inventory.clear(i);
                    left -= is.getAmount();
                } else {
                    if (left <= 0) break;
                    is.setAmount(is.getAmount() - left);
                    left = 0;
                }

                removedItems = true;
            }
        }
        return removedItems;
    }

    public boolean hasBlockedNBT(@NotNull ItemStack itemStack) {
        if (itemStack.getType() == Material.AIR || itemStack.getAmount() == 0) return false;

        // TODO fix
       /* try {
            // ensure blocked nbt is not on the item
            de.tr7zw.changeme.nbtapi.NBTItem nbtItem = new de.tr7zw.changeme.nbtapi.NBTItem(itemStack);
            List<String> blockedNbtList = getPluginInstance().getConfig().getStringList("blocked-nbt-list");
            return blockedNbtList.stream().anyMatch(nbtItem::hasKey);
        } catch (Exception ignored) {}*/
        return false;
    }

    /**
     * Get amount of similar items.
     *
     * @param inventory The inventory to check.
     * @param itemStack The item to check for.
     * @return The total amount.
     */
    public int getItemAmount(@NotNull Inventory inventory, @NotNull ItemStack itemStack) {
        int amount = 0;

        for (int i = -1; ++i < inventory.getSize(); ) {
            final ItemStack is = inventory.getItem(i);
            if (is == null) continue;
            if (isSimilar(is, itemStack)) amount += is.getAmount();
        }

        return amount;
    }

    /**
     * Checks if two items are identical using string format.
     *
     * @param itemOne The first item.
     * @param itemTwo The second item.
     * @return Whether the items are identical.
     */
    public boolean isSimilar(ItemStack itemOne, @NotNull ItemStack itemTwo) {
        return itemOne.isSimilar(itemTwo);
    }

    /**
     * See if a string is NOT a numerical value.
     *
     * @param string The string to check.
     * @return Whether it is numerical or not.
     */
    public boolean isNotNumeric(@NotNull String string) {
        if (string.isEmpty()) return true;

        final char[] chars = string.toCharArray();
        if (chars.length == 1 && !Character.isDigit(chars[0])) return true;

        final boolean onlyWholeNumbers = getPluginInstance().getConfig().getBoolean("whole-number-entries");
        for (int i = -1; ++i < string.length(); ) {
            final char c = chars[i];
            if (onlyWholeNumbers && (c == '.' || c == ',')) return true;
            if (!Character.isDigit(c) && (c != '.' && c != ',') && !((i == 0 && c == '-'))) return true;
        }

        return false;
    }

    /**
     * See if a string is NOT a numerical value.
     *
     * @param string The string to check.
     * @return Whether it is numerical or not.
     */
    public boolean isNotRawNumeric(@NotNull String string) {
        if (string.isEmpty()) return true;

        final char[] chars = string.toCharArray();
        if (chars.length == 1 && !Character.isDigit(chars[0])) return true;

        for (int i = -1; ++i < string.length(); ) {
            final char c = chars[i];
            if (!Character.isDigit(c) && (c != '.' && c != ',') && !((i == 0 && c == '-'))) return true;
        }

        return false;
    }

    /**
     * Wraps a string into multiple lines based on a word count.
     *
     * @param text The long string to wrap.
     * @return wraps the string to multiple lines
     */

    public List<String> wrapString(@NotNull String text) {
        List<String> lines = new ArrayList<>();
        int longWordCount = getPluginInstance().getConfig().getInt("description-long-word-wrap");

        String[] words = text.split("\\s+"); // split by any whitespace
        StringBuilder currentLine = new StringBuilder();
        int wordCounter = 0;

        for (String word : words) {
            if (wordCounter >= longWordCount) {
                lines.add(currentLine.toString().trim());
                currentLine.setLength(0);
                wordCounter = 0;
            }

            currentLine.append(word).append(" ");
            wordCounter++;
        }

        if (!currentLine.toString().isEmpty()) {
            lines.add(currentLine.toString().trim());
        }

        return lines;
    }

    /**
     * Builds and returns the shop creation item.
     *
     * @param player The player to check for what base-block to use.
     * @param amount The amount of the item stack.
     * @return The shop creation physical item stack.
     */
    public ItemStack buildShopCreationItem(Player player, int amount) {
        Menu appearanceMenu = getPluginInstance().getMenu("appearance");
        if (appearanceMenu == null) return null;

        Appearance appearance = Appearance.getAppearance(appearanceMenu.getConfiguration().getString("default-appearance"));
        if (appearance == null) {
            getPluginInstance().log(Level.WARNING, "The default appearance in the \"" + appearanceMenu.getFileName() + "\" is invalid.");
            return null;
        }

        final String materialName = appearance.getMaterial();
        final boolean isEnchanted = getPluginInstance().getConfig().getBoolean("shop-creation-item.enchanted");

        CustomItem item;
        if (materialName != null) {
            if (materialName.contains(":")) {
                String[] args = materialName.split(":");

                if (args.length == 2) {
                    item = new CustomItem(getPluginInstance(), args[0], Integer.parseInt(args[1]), amount)
                            .setDisplayName(player, getPluginInstance().getConfig().getString("shop-creation-item.display-name"))
                            .setLore(player, getPluginInstance().getConfig().getStringList("shop-creation-item.lore"))
                            .setEnchantments(getPluginInstance().getConfig().getStringList("shop-creation-item.enchantments"))
                            .setItemFlags(getPluginInstance().getConfig().getStringList("shop-creation-item.flags"))
                            .setModelData(getPluginInstance().getConfig().getInt("shop-creation-item.model-data"));

                    if (isEnchanted) item.setEnchanted(true);
                    return getPluginInstance().updateNBT(item.get(), "DisplayShops", "Creation Item");
                }
            }

            item = new CustomItem(getPluginInstance(), materialName, 0, amount)
                    .setDisplayName(player, getPluginInstance().getConfig().getString("shop-creation-item.display-name"))
                    .setLore(player, getPluginInstance().getConfig().getStringList("shop-creation-item.lore"))
                    .setEnchantments(getPluginInstance().getConfig().getStringList("shop-creation-item.enchantments"))
                    .setItemFlags(getPluginInstance().getConfig().getStringList("shop-creation-item.flags"))
                    .setModelData(getPluginInstance().getConfig().getInt("shop-creation-item.model-data"));

            if (isEnchanted) item.setEnchanted(true);
            return getPluginInstance().updateNBT(item.get(), "DisplayShops", "Creation Item");
        }

        return null;
    }

    /**
     * Builds and returns the shop currency item.
     *
     * @param amount The amount of the item stack.
     * @return The shop currency physical item stack.
     */
    public ItemStack buildShopCurrencyItem(int amount) {
        final String materialName = getPluginInstance().getConfig().getString("shop-currency-item.material");
        if (materialName != null && !materialName.isEmpty()) {
            if (materialName.contains(":")) {
                String[] args = materialName.split(":");
                return new CustomItem(getPluginInstance(), args[0], Integer.parseInt(args[1]), amount)
                        .setDisplayName(null, getPluginInstance().getConfig().getString("shop-currency-item.display-name"))
                        .setLore(null, getPluginInstance().getConfig().getStringList("shop-currency-item.lore"))
                        .setEnchantments(getPluginInstance().getConfig().getStringList("shop-currency-item.enchantments"))
                        .setItemFlags(getPluginInstance().getConfig().getStringList("shop-currency-item.flags"))
                        .setModelData(getPluginInstance().getConfig().getInt("shop-currency-item.model-data")).get();
            } else
                return new CustomItem(getPluginInstance(), materialName, 0, amount)
                        .setDisplayName(null, getPluginInstance().getConfig().getString("shop-currency-item.display-name"))
                        .setLore(null, getPluginInstance().getConfig().getStringList("shop-currency-item.lore"))
                        .setEnchantments(getPluginInstance().getConfig().getStringList("shop-currency-item.enchantments"))
                        .setItemFlags(getPluginInstance().getConfig().getStringList("shop-currency-item.flags"))
                        .setModelData(getPluginInstance().getConfig().getInt("shop-currency-item.model-data")).get();
        }
        return null;
    }

    /**
     * Checks if the passed world is in the world blacklist.
     *
     * @param world The world to check for.
     * @return Whether the world is blocked or not.
     */
    public boolean isBlockedWorld(@NotNull World world) {
        List<String> worldList = getPluginInstance().getConfig().getStringList("blocked-worlds");
        for (int i = -1; ++i < worldList.size(); )
            if (worldList.get(i).equalsIgnoreCase(world.getName())) return true;
        return false;
    }

    // getters & setters
    private DisplayShops getPluginInstance() {
        return pluginInstance;
    }

    private void setPluginInstance(DisplayShops pluginInstance) {
        this.pluginInstance = pluginInstance;
    }

    public ConcurrentHashMap<String, Shop> getShopMap() {
        return shopMap;
    }

    private void setShopMap(ConcurrentHashMap<String, Shop> shopMap) {
        this.shopMap = shopMap;
    }

    public List<MarketRegion> getMarketRegions() {
        return marketRegions;
    }

    private void setMarketRegions(List<MarketRegion> marketRegions) {
        this.marketRegions = marketRegions;
    }

    public ConcurrentHashMap<UUID, DataPack> getDataPackMap() {
        return dataPackMap;
    }

    private void setDataPackMap(ConcurrentHashMap<UUID, DataPack> dataPackMap) {
        this.dataPackMap = dataPackMap;
    }

    public Pattern getUUIDPattern() {
        return uuidPattern;
    }
}