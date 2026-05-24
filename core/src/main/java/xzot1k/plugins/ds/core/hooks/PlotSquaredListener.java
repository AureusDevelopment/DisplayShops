/*
 * Copyright (c) 2021 XZot1K, All rights reserved.
 */

package xzot1k.plugins.ds.core.hooks;

import com.google.common.eventbus.Subscribe;
import com.plotsquared.core.PlotAPI;
import com.plotsquared.core.events.PlotChangeOwnerEvent;
import com.plotsquared.core.events.PlotClearEvent;
import com.plotsquared.core.events.PlotDeleteEvent;
import com.plotsquared.core.plot.Plot;
import org.bukkit.event.Listener;
import xzot1k.plugins.ds.DisplayShops;
import xzot1k.plugins.ds.api.objects.Shop;

import java.util.ArrayList;
import java.util.List;

public class PlotSquaredListener implements Listener {

    private DisplayShops pluginInstance;

    public PlotSquaredListener(DisplayShops pluginInstance) {
        setPluginInstance(pluginInstance);

        final PlotAPI plotAPI = new PlotAPI();
        plotAPI.registerListener(this);
    }

    @Subscribe
    public void onPlotOwnerChange(PlotChangeOwnerEvent e) {
        List<Shop> shopList = new ArrayList<>(getPluginInstance().getManager().getShopMap().values());
        for (Shop shop : shopList) {
            if (shop == null || (shop != null && (shop.getBaseLocation() == null || (shop.getBaseLocation() != null
                    && !e.getPlot().getWorldName().equalsIgnoreCase(shop.getBaseLocation().getWorldName()))))) continue;

            final org.bukkit.Location location = shop.getBaseLocation().asBukkitLocation();
            if (!e.getPlot().getArea().contains(location.getChunk().getX(), location.getChunk().getZ())) continue;

            shop.setOwnerUniqueId(e.getNewOwner());
        }
    }

    @Subscribe
    public void onPlotRemove(PlotDeleteEvent e) {
        removeShops(e.getPlot());
    }

    @Subscribe
    public void onPlotClear(PlotClearEvent e) {
        removeShops(e.getPlot());
    }

    private void removeShops(Plot plot) {
        List<Shop> shopList = new ArrayList<>(getPluginInstance().getManager().getShopMap().values());
        for (Shop shop : shopList) {
            if (shop == null || (shop != null && (shop.getBaseLocation() == null || (shop.getBaseLocation() != null
                    && !plot.getWorldName().equalsIgnoreCase(shop.getBaseLocation().getWorldName()))))) continue;
            final org.bukkit.Location location = shop.getBaseLocation().asBukkitLocation();
            if (!plot.getArea().contains(location.getChunk().getX(), location.getChunk().getZ())) continue;
            shop.purge(null, true);
        }
    }

    private DisplayShops getPluginInstance() {
        return pluginInstance;
    }

    private void setPluginInstance(DisplayShops pluginInstance) {
        this.pluginInstance = pluginInstance;
    }
}