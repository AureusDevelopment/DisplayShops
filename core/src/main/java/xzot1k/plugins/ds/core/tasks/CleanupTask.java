/*
 * Copyright (c) 2021 XZot1K, All rights reserved.
 */

/*
 * Decompiled with CFR 0.150.
 *
 * Could not load the following classes:
 *  org.bukkit.Location
 *  org.bukkit.scheduler.BukkitRunnable
 */
package xzot1k.plugins.ds.core.tasks;

import org.bukkit.Location;
import org.bukkit.scheduler.BukkitRunnable;
import xzot1k.plugins.ds.DisplayShops;
import xzot1k.plugins.ds.api.objects.Shop;

import java.util.ArrayList;

public class CleanupTask
        extends BukkitRunnable {
    private DisplayShops pluginInstance;
    private int counter;
    private int delay;

    public CleanupTask(DisplayShops pluginInstance) {
        this.setPluginInstance(pluginInstance);
        this.setDelay(this.getPluginInstance().getConfig().getInt("base-block-sync-delay"));
    }

    public void run() {
        if (this.getDelay() < 0) {
            this.getPluginInstance().setCleanupTask(null);
            this.cancel();
            return;
        }
        if (this.getCounter() >= this.getDelay()) {
            this.setCounter(0);
            ArrayList<Shop> shopList = new ArrayList<Shop>(this.getPluginInstance().getManager().getShopMap().values());
            int i = -1;
            while (++i < shopList.size()) {
                Shop shop = shopList.get(i);
                if (shop == null || shop != null && shop.getBaseLocation() == null) {
                    this.getPluginInstance().getManager().getShopMap().remove(shop.getShopId());
                    shop.delete(true);
                    continue;
                }
                Location baseBlock = shop.getBaseLocation().asBukkitLocation();
                if (!baseBlock.getWorld().isChunkLoaded(baseBlock.getBlockX() << 4, baseBlock.getBlockZ() << 4) || baseBlock.getBlock() != null && !baseBlock.getBlock().getType().name().contains("AIR"))
                    continue;
                this.getPluginInstance().getManager().getShopMap().remove(shop.getShopId());
                shop.delete(true);
            }
            return;
        }
        this.setCounter(this.getCounter() + 1);
    }

    private DisplayShops getPluginInstance() {
        return this.pluginInstance;
    }

    private void setPluginInstance(DisplayShops pluginInstance) {
        this.pluginInstance = pluginInstance;
    }

    public int getCounter() {
        return this.counter;
    }

    private void setCounter(int counter) {
        this.counter = counter;
    }

    public int getDelay() {
        return this.delay;
    }

    private void setDelay(int delay) {
        this.delay = delay;
    }
}

