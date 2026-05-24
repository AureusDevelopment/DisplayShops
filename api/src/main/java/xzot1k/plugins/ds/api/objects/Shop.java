/*
 * Copyright (c) 2022 XZot1K, All rights reserved.
 */

package xzot1k.plugins.ds.api.objects;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import xzot1k.plugins.ds.api.enums.EconomyCallType;

import java.util.List;
import java.util.UUID;

public interface Shop {

    /**
     * Kills the shop's display packets entirely.
     *
     * @param player The player to send the destroy packets to.
     */
    void kill(@NotNull Player player);

    /**
     * Kills the shop's display packets entirely for ALL players.
     */
    void killAll();

    /**
     * Shows the shop display to the player.
     *
     * @param player        The player to show the display to.
     * @param showHolograms Whether the hologram lines are shown or not.
     */
    void display(@NotNull Player player, boolean showHolograms);

    /**
     * Checks if player is in range of this shop.
     *
     * @param player   The player to check.
     * @param distance The distance between the player and the shop.
     * @return Whether the player is in range or not.
     */
    boolean isInRange(@NotNull Player player, double distance);

    /**
     * Returns the shop's entire stored balance to the owner. If the owner does NOT exist, currency is sent to the passed player.
     */
    void returnBalance();

    /**
     * Obtains the passed shop's max stock based on owner permissions or administrator bypasses.
     *
     * @return The obtained max stock (defaults to configuration value or max possible integer, if the shop is admin).
     */
    int getMaxStock();

    /**
     * Drops the shop's entire stock onto the ground after calculating stacks.
     */
    void dropStock();

    /**
     * Updates the direction to where the player is looking (for the shop item placement)
     *
     * @param player The player to grab info from
     */
    void updateBaseBlockDirection(@NotNull Player player);

    /**
     * Gets the name of the shop's trade currency item (if NULL or "force-use" is enabled in the config.yml, the name of the default currency item will be used).
     *
     * @return The shop's currency itemstack name.
     */
    String getTradeItemName();

    boolean isClaimable();

    void setClaimable(boolean value);

    /**
     * Saves the shop to the database.
     *
     * @param async Whether it should be saved on the main thread or not.
     */
    void save(boolean async);

    /**
     * Runs all commands given to the shop.
     *
     * @param player the player used inside the commands.
     * @param amount The amount for the {amount} placeholder.
     */
    void runCommands(@NotNull Player player, int amount);

    /**
     * Attempts to teleport the passed player to this shop.
     *
     * @param player The player to teleport.
     * @param charge Whether the player should be charged.
     */
    void visit(@NotNull Player player, boolean charge);

    /**
     * Deletes the shop from the database if found.
     *
     * @param async Whether the shop should be deleted on the main thread or not.
     */
    void delete(boolean async);

    /**
     * Registers the shop into the manager.
     */
    void register();

    /**
     * Unregisters the shop from the manager.
     */
    void unRegister();

    /**
     * Gets if a shop is an admin shop or not.
     *
     * @return Whether the shop is admin or not.
     */
    boolean isAdminShop();

    /**
     * Sets the owner to null to initiate admin shop mode.
     */
    void makeAdminShop();

    /**
     * Updates the shop change time stamp (Used for the purge system).
     */
    void updateTimeStamp();

    /**
     * Updates the shop buy or sell time stamp (Used for the dynamic price changing system).
     * (Note: Only use BUY or SELL as the economy call type. Defaults to BUY if invalid or improper.)
     */
    void updateTransactionTimeStamp(@NotNull EconomyCallType economyCallType);

    /**
     * Checks if the shop is ready to be purged.
     *
     * @param purgeDuration The duration a shop must be untouched.
     * @return Whether it is ready.
     */
    boolean isReadyForPurge(int purgeDuration);

    /**
     * Checks if the shop is ready to have its dynamic price counter reset for either BUY or SELL.
     *
     * @param resetDuration   The duration a shop must go without a transaction in the BUY or SELL field.
     * @param economyCallType The type to check readiness for (Note: ONLY use BUY or SELL, by default BUY will be used).
     * @return Whether it is ready.
     */
    boolean isReadyForDynamicReset(int resetDuration, @NotNull EconomyCallType economyCallType);

    /**
     * Deletes the shop's data, deletes the base-block, drops the stock, destroys the display packet, and unregisters the object.
     *
     * @param player If provided, some additional effects will be displayed.
     * @param async  Whether the file deletion is async or not.
     */
    void purge(@Nullable Player player, boolean async);

    /**
     * Gets the dynamic price that will be added/subtracted to the buy/sell price.
     *
     * @param originalPrice The original price.
     * @param isBuy         Determines whether the result will use buy or sell values.
     * @return The dynamic value that will
     */
    double getDynamicPriceValue(double originalPrice, boolean isBuy);

    /**
     * Checks to see if the player is able to edit the shop.
     *
     * @param player The player to check access for.
     * @return Whether the player can or can NOT edit the shop.
     */
    boolean canEdit(@NotNull Player player);

    /**
     * Checks to see if the editor is actually editing the shop, if not the current editor data is cleared.
     */
    void checkCurrentEditor(@Nullable Player player);

    // getters & setters
    ItemStack getShopItem();

    void setShopItem(@Nullable ItemStack shopItem);

    /**
     * Gets the shop's unit buy price.
     *
     * @param useDynamicPricing Whether to apply dynamic price values to the base value or not.
     * @return The total result.
     */
    double getBuyPrice(boolean useDynamicPricing);

    void setBuyPrice(double buyPrice);

    /**
     * Gets the shop's unit sell price.
     *
     * @param useDynamicPricing Whether to apply dynamic price values to the base value or not.
     * @return The total result.
     */
    double getSellPrice(boolean useDynamicPricing);

    void setSellPrice(double sellPrice);

    UUID getOwnerUniqueId();

    void setOwnerUniqueId(@Nullable UUID ownerUniqueId);

    LocationClone getBaseLocation();

    void setBaseLocation(@NotNull LocationClone baseLocation);

    int getStock();

    void setStock(int stock);

    UUID getShopId();

    void setShopId(@NotNull UUID shopId);

    /**
     * @return The global counter for how many times the shop has been bought from.
     */
    int getGlobalBuyCounter();

    /**
     * @param buyCounter The new amount to set the global buy counter to.
     */
    void setGlobalBuyCounter(int buyCounter);

    /**
     * @return The global counter for how many times the shop has been sold to.
     */
    int getGlobalSellCounter();

    /**
     * @param sellCounter The new amount to set the global sell counter to.
     */
    void setGlobalSellCounter(int sellCounter);

    /**
     * @return The per-player buy limit for the shop.
     */
    int getPlayerBuyLimit();

    /**
     * @param playerBuyLimit The new per-player buy limit for the shop.
     */
    void setPlayerBuyLimit(int playerBuyLimit);

    /**
     * @return The per-player sell limit for the shop.
     */
    int getPlayerSellLimit();

    /**
     * @param playerSellLimit The new per-player sell limit for the shop.
     */
    void setPlayerSellLimit(int playerSellLimit);

    /**
     * @return The per-shop global buy limit for the shop.
     */
    int getGlobalBuyLimit();

    /**
     * @param globalBuyLimit The new per-shop global buy limit for the shop.
     */
    void setGlobalBuyLimit(int globalBuyLimit);

    /**
     * @return The per-shop global sell limit for the shop.
     */
    int getGlobalSellLimit();

    /**
     * @param globalSellLimit The new per-shop global sell limit for the shop.
     */
    void setGlobalSellLimit(int globalSellLimit);

    /**
     * Obtains the balance of the shop which is used to purchase and store currency collected from invetors.
     *
     * @return The balance being held (currency).
     */
    double getStoredBalance();

    /**
     * Sets the balance of a shop. This balance is used to purchase and store currency collected from investors.
     *
     * @param amount The amount to set as the stored balance.
     */
    void setStoredBalance(double amount);

    boolean isCommandOnlyMode();

    void setCommandOnlyMode(boolean commandOnlyMode);

    List<String> getCommands();

    void setCommands(@NotNull List<String> commands);

    List<UUID> getAssistants();

    int getShopItemAmount();

    void setShopItemAmount(int tradeItemAmount);

    ItemStack getTradeItem();

    void setTradeItem(@Nullable ItemStack tradeItem);

    long getChangeTimeStamp();

    void setChangeTimeStamp(long changeTimeStamp);

    long getLastBuyTimeStamp();

    void setLastBuyTimeStamp(long buyTimeStamp);

    long getLastSellTimeStamp();

    void setLastSellTimeStamp(long sellTimeStamp);

    String getDescription();

    void setDescription(@Nullable String description);

    boolean canDynamicPriceChange();

    void setDynamicPriceChange(boolean dynamicPriceChange);

    int getDynamicBuyCounter();

    void setDynamicBuyCounter(int dynamicPriceCounter);

    int getDynamicSellCounter();

    void setDynamicSellCounter(int dynamicPriceCounter);

    /**
     * Gets  the player unique ID of whom is currently editing the shop.
     *
     * @return The current editor's unique ID.
     */
    UUID getCurrentEditor();

    /**
     * Sets the current editor.
     *
     * @param currentEditor The unique ID of the player editing.
     */
    void setCurrentEditor(@Nullable UUID currentEditor);

    /**
     * Resets the shop entirely. The time stamp is also updated.
     */
    void reset();

    String getCurrencyType();

    void setCurrencyType(@NotNull String currencyType);

    /**
     * @return Gets the item used for Item-For-Item trading.
     */
    ItemStack getCurrencyItem();

    String getAppearanceId();

    void setAppearanceId(String appearanceId);

    ItemStack getVisitIcon();

    void setVisitIcon(ItemStack visitIcon);
}