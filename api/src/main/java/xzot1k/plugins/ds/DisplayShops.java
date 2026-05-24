/*
 * Copyright (c) 2022 XZot1K, All rights reserved.
 */

package xzot1k.plugins.ds;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
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

public class DisplayShops extends JavaPlugin implements DisplayShopsAPI {

    private static DisplayShops pluginInstance;

    public DisplayShops() {
        DisplayShops.pluginInstance = this;
    }

    public static DisplayShops getPluginInstance() {
        return pluginInstance;
    }

    @Override
    public void onEnable() {
    }

    @Override
    public void onDisable() {
    }

    @Override
    public void log(@NotNull Level level, @NotNull String message) {
    }

    @Override
    public void setupTasks() {
    }

    @Override
    public Menu getMenu(@NotNull String name) {
        return null;
    }

    @Override
    public boolean matchesAnyMenu(@NotNull String name) {
        return false;
    }

    @Override
    public boolean matchesMenu(@NotNull String menuName, @NotNull String inventoryName) {
        return false;
    }

    @Override
    public long getBlockedItemId(@NotNull ItemStack itemStack) {
        return -1;
    }

    @Override
    public void reloadConfigs() {
    }

    @Override
    public FileConfiguration getLangConfig() {
        return null;
    }

    @Override
    public void saveDefaultConfigs() {
    }

    @Override
    public void refreshShop(@NotNull Shop shop) {
    }

    @Override
    public DisplayPacket getDisplayPacket(@NotNull Shop shop, @NotNull Player player) {
        return null;
    }

    @Override
    public void killCurrentShopPacket(@NotNull Player player) {
    }

    @Override
    public void clearDisplayPackets(@NotNull Player player) {
    }

    @Override
    public void removeDisplayPacket(@NotNull Shop shop, @NotNull Player player) {
    }

    @Override
    public void updateDisplayPacket(@NotNull Shop shop, @NotNull Player player, @NotNull DisplayPacket displayPacket) {
    }

    @Override
    public void sendDisplayPacket(@NotNull Shop shop, @NotNull Player player, boolean showHolograms) {
    }

    @Override
    public Manager getManager() {
        return null;
    }

    @Override
    public boolean isPrismaInstalled() {
        return false;
    }

    @Override
    public Connection getDatabaseConnection() {
        return null;
    }

    @Override
    public List<UUID> getTeleportingPlayers() {
        return null;
    }

    @Override
    public HashMap<UUID, UUID> getShopMemory() {
        return null;
    }

    @Override
    public EcoHandler getEconomyHandler() {
        return null;
    }

    @Override
    public VersionUtil getVersionUtil() {
        return null;
    }

    @Override
    public File getLoggingFile() {
        return null;
    }

    @Override
    public void writeToLog(@NotNull String text) {

    }

}