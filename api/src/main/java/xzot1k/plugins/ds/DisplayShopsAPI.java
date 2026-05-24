/*
 * Copyright (c) 2022 XZot1K, All rights reserved.
 */

package xzot1k.plugins.ds;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import xzot1k.plugins.ds.api.Manager;
import xzot1k.plugins.ds.api.VersionUtil;
import xzot1k.plugins.ds.api.eco.EcoHandler;
import xzot1k.plugins.ds.api.handlers.DisplayPacket;
import xzot1k.plugins.ds.api.objects.Menu;
import xzot1k.plugins.ds.api.objects.Shop;

import java.io.File;
import java.sql.Connection;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;
import java.util.logging.Level;

public interface DisplayShopsAPI {

    /**
     * Logs a message with a level to the console under the DisplayShops title.
     *
     * @param level   Level of the message.
     * @param message The message to log.
     */
    void log(@NotNull Level level, @NotNull String message);

    /**
     * Sets up all tasks; however, it doesn't cancel or stop existing tasks.
     */
    void setupTasks();

    // custom configurations

    /**
     * @param name The name of the menu or an inventory name.
     * @return The menu found that matches.
     */
    Menu getMenu(@NotNull String name);

    /**
     * @param name The inventory name.
     * @return If any menu matches the same title.
     */
    boolean matchesAnyMenu(@NotNull String name);

    /**
     * @param menuName      The name of the menu to check.
     * @param inventoryName The title of the menu to compare.
     * @return Whether the title compares to the menu.
     */
    boolean matchesMenu(@NotNull String menuName, @NotNull String inventoryName);

    /**
     * Gets the id associated to the item in the blocked-items.yml.
     *
     * @param itemStack The item to check the id for.
     * @return The id associated to the item in the blocked-items.yml (returns -1 if invalid).
     */
    long getBlockedItemId(@NotNull ItemStack itemStack);

    /**
     * Reloads all configs associated with DisplayShops.
     */
    void reloadConfigs();

    /**
     * Gets the language file configuration.
     *
     * @return The FileConfiguration found.
     */
    FileConfiguration getLangConfig();

    /**
     * Saves the default configuration files (Doesn't replace existing).
     */
    void saveDefaultConfigs();

    // packet methods

    /**
     * Schedules a general thread-safe refresh for the shop's display.
     *
     * @param shop The shop to refresh the display of.
     */
    void refreshShop(@NotNull Shop shop);

    /**
     * Gets the display packet a player currently has made for a specific shop (Can return NULL).
     *
     * @param shop   The shop to check for.
     * @param player The player to check for.
     * @return the display packet instance for the shop.
     */
    DisplayPacket getDisplayPacket(@NotNull Shop shop, @NotNull Player player);

    /**
     * Kills the current shop packets in view and removes it from memory for the player.
     * (Note: This is ONLY used for the shop the player is currently looking at)
     *
     * @param player The player to kill the packet for.
     */
    void killCurrentShopPacket(@NotNull Player player);

    /**
     * Clears all display packets for a player on file.
     *
     * @param player The player to remove the packets for.
     */
    void clearDisplayPackets(@NotNull Player player);

    /**
     * Un-registers the existing display packet for a specific shop from the player.
     *
     * @param shop   The shop to look for.
     * @param player The player to look for.
     */
    void removeDisplayPacket(@NotNull Shop shop, @NotNull Player player);

    /**
     * Updates the display packet for a specific shop for the player.
     *
     * @param shop          The shop to use.
     * @param player        The player to set it for.
     * @param displayPacket The packet to set for the shop.
     */
    void updateDisplayPacket(@NotNull Shop shop, @NotNull Player player, @NotNull DisplayPacket displayPacket);

    /**
     * Sends the entire display to the player. (NOTE: The 'showHolograms' parameter will be ignored if the 'always-display' is enabled)
     *
     * @param shop          The shop to create the display for.
     * @param player        The player to send the display packets to.
     * @param showHolograms Whether holograms above the glass and item are visible/created.
     */
    void sendDisplayPacket(@NotNull Shop shop, @NotNull Player player, boolean showHolograms);

    // getters & setters

    /**
     * @return Returns the manager where most data and API methods are stored.
     */
    Manager getManager();

    /**
     * @return Server version in the format XXX.X where the decimal digit is the 'R' version.
     */
    /*    double getServerVersion();*/

    /**
     * Checks if paper spigot methods exist.
     *
     * @return Whether paper spigot is detected.
     */
    /*boolean isPaperSpigot();*/

    /**
     * This gets the logging file.
     *
     * @return The file used for plugin logging.
     */
    File getLoggingFile();

    /**
     * Writes to the log file if size does NOT exceed configuration limitations.
     *
     * @param text The text to store on the next available line in the file.
     */
    void writeToLog(@NotNull String text);

    boolean isPrismaInstalled();

    Connection getDatabaseConnection();

    List<UUID> getTeleportingPlayers();

    HashMap<UUID, UUID> getShopMemory();

    EcoHandler getEconomyHandler();

    VersionUtil getVersionUtil();

}