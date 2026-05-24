package xzot1k.plugins.ds.core.utils;

import org.bstats.bukkit.Metrics;
import org.bstats.charts.SimplePie;
import org.bstats.charts.SingleLineChart;
import xzot1k.plugins.ds.DisplayShops;

public class MetricsImpl {

    public MetricsImpl(DisplayShops instance) {
        Metrics m = new Metrics(instance, 23070);
        m.addCustomChart(new SingleLineChart("shop_amount", () -> instance.getManager().getShopMap().size()));
        m.addCustomChart(new SimplePie("modern_displays", () -> instance.getDisplayManager() != null ? "true" : "false"));
        m.addCustomChart(new SimplePie("claimable_system", () -> instance.getConfig().getBoolean("claimable-system", false) ? "true" : "false"));
        if (instance.getPapiHelper() != null)
            m.addCustomChart(new SingleLineChart("placeholderapi_requests", () -> DisplayShops.placeholderAPI));
        m.addCustomChart(new SingleLineChart("item_sells", () -> DisplayShops.itemSells));
        m.addCustomChart(new SingleLineChart("item_buys", () -> DisplayShops.itemBuys));
        m.addCustomChart(new SingleLineChart("menu_opens", () -> DisplayShops.menuOpens));
        m.addCustomChart(new SimplePie("sql_saving_system", () -> DisplayShops.isSQL ? "true" : "false"));
        m.addCustomChart(new SimplePie("new_item_saving", () -> instance.isNewItemSaving() ? "true" : "false"));
    }

}
