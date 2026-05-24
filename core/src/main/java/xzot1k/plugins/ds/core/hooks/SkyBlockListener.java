/*
 * Copyright (c) 2021 XZot1K, All rights reserved.
 */

package xzot1k.plugins.ds.core.hooks;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import xzot1k.plugins.ds.DisplayShops;
import xzot1k.plugins.ds.api.objects.Shop;

import java.util.ArrayList;
import java.util.Objects;

public class SkyBlockListener implements Listener {

    private final DisplayShops pluginInstance;

    public SkyBlockListener(DisplayShops pluginInstance) {
        this.pluginInstance = pluginInstance;

        if (getPluginInstance().getServer().getPluginManager().isPluginEnabled("ASkyBlock"))
            getPluginInstance().getServer().getPluginManager().registerEvents(new Listener() {

                @EventHandler
                public void onDelete(com.wasteofplastic.askyblock.events.IslandDeleteEvent e) {
                    for (Shop shop : new ArrayList<>(getPluginInstance().getManager().getShopMap().values())) {
                        if (shop == null || !getPluginInstance().getManager().getShopMap().containsKey(shop.getShopId())
                                || shop.getBaseLocation() == null || (shop.getBaseLocation() != null
                                && !shop.getBaseLocation().getWorldName().equalsIgnoreCase(Objects.requireNonNull(e.getLocation().getWorld()).getName())))
                            continue;

                        final com.wasteofplastic.askyblock.Island island = com.wasteofplastic.askyblock.ASkyBlockAPI.getInstance().getIslandAt(e.getLocation());
                        if (island.inIslandSpace((int) shop.getBaseLocation().getX(), (int) shop.getBaseLocation().getZ())) {
                            shop.unRegister();
                            shop.killAll();
                            shop.delete(true);
                            break;
                        }
                    }
                }

                @Override
                public int hashCode() {
                    return super.hashCode();
                }
            }, getPluginInstance());
        else if (getPluginInstance().getServer().getPluginManager().isPluginEnabled("SuperiorSkyblock2"))
            getPluginInstance().getServer().getPluginManager().registerEvents(new Listener() {

                @EventHandler
                public void onDisband(com.bgsoftware.superiorskyblock.api.events.IslandDisbandEvent e) {
                    for (Shop shop : new ArrayList<>(getPluginInstance().getManager().getShopMap().values())) {
                        if (shop == null || !getPluginInstance().getManager().getShopMap().containsKey(shop.getShopId())
                                || shop.getBaseLocation() == null) continue;

                        boolean foundEnv = false;
                        for (World.Environment env : World.Environment.values()) {
                            try {
                                Location location = e.getIsland().getCenter(env);
                                if (location != null && location.getWorld() != null) {
                                    if (shop.getBaseLocation().getWorldName().equalsIgnoreCase(location.getWorld().getName())) {
                                        foundEnv = true;
                                        break;
                                    }
                                }
                            } catch (NullPointerException ignored) {}
                        }

                        if (foundEnv && e.getIsland().isInside(shop.getBaseLocation().asBukkitLocation())) {
                            shop.unRegister();
                            shop.killAll();
                            shop.delete(true);
                            break;
                        }
                    }
                }

                @Override
                public int hashCode() {
                    return super.hashCode();
                }
            }, getPluginInstance());
        else if (getPluginInstance().getServer().getPluginManager().isPluginEnabled("FabledSkyBlock"))
            getPluginInstance().getServer().getPluginManager().registerEvents(new Listener() {

                @EventHandler
                public void onDelete(com.songoda.skyblock.api.event.island.IslandDeleteEvent e) {
                    if (e.getIsland() == null) return;
                    for (Shop shop : new ArrayList<>(getPluginInstance().getManager().getShopMap().values())) {
                        if (e.getIsland() == null) return;
                        if (shop == null || !getPluginInstance().getManager().getShopMap().containsKey(shop.getShopId())
                                || shop.getBaseLocation() == null)
                            continue;

                        final Location location = shop.getBaseLocation().asBukkitLocation();
                        if (!com.songoda.skyblock.api.SkyBlockAPI.getImplementation().getWorldManager().isIslandWorld(location.getWorld()))
                            continue;

                        final com.songoda.skyblock.api.island.Island island = com.songoda.skyblock.api.SkyBlockAPI.getIslandManager().getIslandAtLocation(location);
                        if (island != null && island.getIslandUUID().toString().equals(e.getIsland().getIslandUUID().toString())) {
                            shop.unRegister();
                            shop.killAll();
                            shop.delete(true);
                            break;
                        }
                    }
                }

                @Override
                public int hashCode() {
                    return super.hashCode();
                }
            }, getPluginInstance());
        else if (getPluginInstance().getServer().getPluginManager().isPluginEnabled("IridiumSkyblock"))
            getPluginInstance().getServer().getPluginManager().registerEvents(new Listener() {

                @EventHandler
                public void onDelete(com.iridium.iridiumskyblock.api.IslandDeleteEvent e) {
                    for (Shop shop : new ArrayList<>(getPluginInstance().getManager().getShopMap().values())) {
                        if (shop == null || !getPluginInstance().getManager().getShopMap().containsKey(shop.getShopId())
                                || shop.getBaseLocation() == null || (shop.getBaseLocation() != null
                                && !shop.getBaseLocation().getWorldName().equalsIgnoreCase(Objects.requireNonNull(e.getIsland().getCenter(getPluginInstance()
                                .getServer().getWorld(shop.getBaseLocation().getWorldName())).getWorld()).getName())))
                            continue;

                        if (e.getIsland().isInIsland((int) shop.getBaseLocation().getX(), (int) shop.getBaseLocation().getZ())) {
                            shop.unRegister();
                            shop.killAll();
                            shop.delete(true);
                            break;
                        }
                    }
                }

                @Override
                public int hashCode() {
                    return super.hashCode();
                }
            }, getPluginInstance());
        else if (getPluginInstance().getServer().getPluginManager().isPluginEnabled("BentoBox"))
            getPluginInstance().getServer().getPluginManager().registerEvents(new Listener() {

                @EventHandler
                public void onDelete(world.bentobox.bentobox.api.events.island.IslandDeleteEvent e) {
                    for (Shop shop : new ArrayList<>(getPluginInstance().getManager().getShopMap().values())) {
                        if (shop == null || !getPluginInstance().getManager().getShopMap().containsKey(shop.getShopId())
                                || shop.getBaseLocation() == null || (shop.getBaseLocation() != null
                                && !shop.getBaseLocation().getWorldName().equalsIgnoreCase(Objects.requireNonNull(e.getIsland().getCenter().getWorld()).getName())))
                            continue;

                        if (e.getIsland().inIslandSpace((int) shop.getBaseLocation().getX(), (int) shop.getBaseLocation().getZ())) {
                            shop.unRegister();
                            shop.killAll();
                            shop.delete(true);
                            break;
                        }
                    }
                }

                @Override
                public int hashCode() {
                    return super.hashCode();
                }
            }, getPluginInstance());

    }

    private DisplayShops getPluginInstance() {
        return pluginInstance;
    }

}
