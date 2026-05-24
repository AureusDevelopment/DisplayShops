/*
 * Copyright (c) 2022 XZot1K, All rights reserved.
 */

package xzot1k.plugins.ds.api.objects;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import xzot1k.plugins.ds.api.enums.InteractionType;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;

public interface DataPack {

    /**
     * Updates the transaction limit counter for the shop.
     *
     * @param shop   The shop to update.
     * @param isBuy  Whether the transaction was buy or sell.
     * @param amount The amount to set.
     */
    void updateCurrentTransactionLimitCounter(@NotNull Shop shop, boolean isBuy, long amount);

    /**
     * Gets the current transaction limit counter under the shop.
     *
     * @param shop  The shop to check for.
     * @param isBuy Whether the transaction was buy or sell.
     * @return The current counter for the shop.
     */
    long getCurrentTransactionCounter(Shop shop, boolean isBuy);

    /**
     * Checks if the transaction limit was reached.
     *
     * @param shop     The shop to check against.
     * @param isBuy    Whether the transaction was buy or sell.
     * @param isGlobal Whether the limited needs to be global or player specific.
     * @return Whether the limit was met.
     */
    boolean hasMetTransactionLimit(Shop shop, boolean isBuy, boolean isGlobal);

    /**
     * Gets the excess amount of units before the limit is met.
     *
     * @param shop     The shop to check against.
     * @param isBuy    Whether the transaction was buy or sell.
     * @param isGlobal Whether the limited needs to be global or player specific.
     * @param amount   the unit count requested
     * @return The excess amount of units available before limit is met
     */
    long getTransactionLimitExcess(Shop shop, boolean isBuy, boolean isGlobal, long amount);

    /**
     * Updates the cooldown id for the player.
     *
     * @param cooldownId The cooldown id to update.
     */
    void updateCooldown(@NotNull String cooldownId);

    /**
     * Checks if the passed player has a cooldown at the specified id based on the base cooldown value.
     *
     * @param player     The player to check.
     * @param cooldownId The cooldown id.
     * @param cooldown   The cooldown's amount (normally the configuration value).
     * @return Whether the cooldown is active (above zero).
     */
    boolean isOnCooldown(@NotNull Player player, @NotNull String cooldownId, int cooldown);

    /**
     * Gets the cooldown left for the player at a specified id.
     *
     * @param player     The player to get the cooldown from.
     * @param cooldownId The cooldown id.
     * @param cooldown   The cooldown value (Usually from configuration).
     * @return The cooldown time remaining.
     */
    int getCooldown(@NotNull Player player, @NotNull String cooldownId, int cooldown);

    /**
     * Checks to see if the player can unlock the appearance.
     *
     * @param player       The player to check permissions of.
     * @param appearanceId The appearance ID.
     * @return Whether the appearance is unlocked.
     */
    boolean canUnlockAppearance(@NotNull Player player, @NotNull String appearanceId);

    /**
     * Checks to see if the player has unlocked the appearance.
     *
     * @param player       The player to check permissions of.
     * @param appearanceId The appearance ID.
     * @return Whether the appearance is unlocked.
     */
    boolean hasUnlockedAppearance(@NotNull Player player, @NotNull String appearanceId);

    /**
     * Unlocks the appearance.
     *
     * @param appearanceId The material followed by a colon and the durability.
     * @param unlocked     Updates the appearance status to be unlocked or locked.
     */
    void updateAppearance(@NotNull String appearanceId, boolean unlocked);

    String getAppearanceData();

    void loadAppearanceData(@Nullable String appearanceData);

    /**
     * Updates all base-block data to either locked aside the default or unlock all.
     *
     * @param unlockAll Whether to unlock all materials.
     */
    void updateUnlocks(boolean unlockAll);

    /**
     * Obtains a full string of cooldowns applied to the player.
     *
     * @return The string full of cooldowns.
     */
    String cooldownsToString();

    /**
     * Obtains a full string of transaction limits applied to the player.
     *
     * @return The string full of transaction limits.
     */
    String transactionLimitsToString();

    /**
     * Resets all type of editing a player is doing to a selected shop.
     */
    void resetEditData();

    // getters & setters
    LinkedHashMap<String, Boolean> getAppearanceDataMap();

    void setAppearanceDataMap(@Nullable LinkedHashMap<String, Boolean> appearanceDataMap);

    /**
     * Gets the shop object the player is currently tethered to from editing or any similar actions.
     *
     * @return The shop object. (Returns NULL if there is no shop selected for editing or similar actions)
     */
    Shop getSelectedShop();

    /**
     * Sets the shop object the player is currently editing or any similar actions.
     *
     * @param selectedShop The shop object to set as.
     */
    void setSelectedShop(@Nullable Shop selectedShop);

    /**
     * Obtains the selected region of a player, if there is one.
     *
     * @return The region object. (Returns NULL if no region is selected)
     */
    Region getSelectedRegion();

    /**
     * Sets the selected region stored for the player.
     *
     * @param selectedRegion The new region object.
     */
    void setSelectedRegion(@Nullable Region selectedRegion);

    /**
     * Checks if the player is currently in region selection mode.
     *
     * @return Whether the player is in selection mode.
     */
    boolean isInSelectionMode();

    /**
     * Sets if the player is in selection mode or not.
     *
     * @param inSelectionMode Whether the player should be in selection mode or not.
     */
    void setInSelectionMode(boolean inSelectionMode);

    /**
     * Gets the player cooldown map for the player.
     *
     * @return The map itself.
     */
    HashMap<String, Long> getCooldownMap();

    /**
     * Gets the current page the player is on from the page map.
     *
     * @return The current page.
     */
    int getCurrentPage();

    /**
     * Sets the current page the player is on from the page map.
     */
    void setCurrentPage(int currentPage);

    /**
     * Gets the page map for the player if it exists.
     *
     * @return The map itself.
     */
    HashMap<Integer, List<ItemStack>> getPageMap();

    /**
     * Sets the page map for the player.
     *
     * @param pageMap The map to set it as.
     */
    void setPageMap(@Nullable HashMap<Integer, List<ItemStack>> pageMap);

    /**
     * @return Whether the player has a next page in their current menu interaction.
     */
    boolean hasNextPage();

    /**
     * @return Whether the player has a previous page in their current menu interaction.
     */
    boolean hasPreviousPage();

    /**
     * Determines if player will be notified .
     */
    boolean isTransactionNotify();

    void setTransactionNotify(boolean transactionNotify);

    /**
     * @return The current interaction type the player is performing.
     */
    InteractionType getInteractionType();

    /**
     * Sets the interaction type the player is currently performing.
     *
     * @param interactionType The new interaction type to update with.
     */
    void setInteractionType(@Nullable InteractionType interactionType);

    /**
     * Gets the value attached to the current interaction performed by the player.
     *
     * @return The current value attached to the interaction.
     */
    Object getInteractionValue();

    /**
     * Sets the value attached to the current interaction performed by the player.
     *
     * @param interactionValue The new value to set as the attached interaction value.
     */
    void setInteractionValue(@Nullable Object interactionValue);

    /**
     * Gets the value attached to the current interaction performed by the player, intended for retrieval way later in the process.
     *
     * @return The current value attached to the interaction, intended for retrieval way later in the process.
     */
    Object getLongTermInteractionValue();

    /**
     * Sets the value attached to the current interaction performed by the player, intended for retrieval way later in the process.
     *
     * @param interactionValue The new value to set as the attached interaction value, intended for retrieval way later in the process.
     */
    void setLongTermInteractionValue(@Nullable Object interactionValue);

}