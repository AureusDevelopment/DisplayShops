package xzot1k.plugins.ds.core.hooks;

import com.nexomc.nexo.api.events.NexoItemsLoadedEvent;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import xzot1k.plugins.ds.DisplayShops;

public class NexoHook implements Listener {

    private final DisplayShops INSTANCE;

    public NexoHook(DisplayShops instance) {
        this.INSTANCE = instance;
    }

    @EventHandler
    public void NexoLoadEvent(NexoItemsLoadedEvent e) {
        INSTANCE.getListeners().creationItem = INSTANCE.getManager().buildShopCreationItem(null, 1);
    }


}
