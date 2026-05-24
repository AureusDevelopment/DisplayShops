package xzot1k.plugins.ds.core.hooks;

import dev.lone.itemsadder.api.Events.ItemsAdderLoadDataEvent;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.jetbrains.annotations.NotNull;
import xzot1k.plugins.ds.DisplayShops;

public class ItemsAdderHandler implements Listener {

    private final DisplayShops INSTANCE;

    public ItemsAdderHandler(@NotNull DisplayShops instance) {
        this.INSTANCE = instance;
    }

    @EventHandler
    public void onLoad(ItemsAdderLoadDataEvent e) {
        // update creation item
        INSTANCE.getListeners().creationItem = INSTANCE.getManager().buildShopCreationItem(null, 1);
    }

}
