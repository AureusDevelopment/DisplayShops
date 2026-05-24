package xzot1k.plugins.ds.core;

import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import xzot1k.plugins.ds.DisplayShops;
import xzot1k.plugins.ds.api.objects.Shop;
import xzot1k.plugins.ds.core.packets.DecentDisplay;
import xzot1k.plugins.ds.core.packets.Display;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class DisplayManager {

    private final ConcurrentHashMap<UUID, Display> shopDisplays = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<UUID, DecentDisplay> dhShopDisplays = new ConcurrentHashMap<>();

    public DisplayManager() {

    }

    public boolean ShouldSeeDisplay(UUID shopId, @NotNull Player player) {
        final Display display = getShopDisplays().getOrDefault(shopId, null);
        if (display == null) {return false;}
        return display.shouldSee(player);
    }

    public Display getDisplay(UUID shopId) {
        Display display = getShopDisplays().getOrDefault(shopId, null);
        if (display == null) {
            Shop shop = DisplayShops.getPluginInstance().getManager().getShopById(shopId);
            if (shop != null) {getShopDisplays().put(shopId, display = new Display(shop));}
        }
        return display;
    }

    public DecentDisplay getDHDisplay(UUID shopId) {
        DecentDisplay display = getDHDisplays().getOrDefault(shopId, null);
        if (display == null) {
            Shop shop = DisplayShops.getPluginInstance().getManager().getShopById(shopId);
            if (shop != null) {getDHDisplays().put(shopId, display = new DecentDisplay(shop));}
        }
        return display;
    }

    // getters & setters
    public ConcurrentHashMap<UUID, Display> getShopDisplays() {
        return shopDisplays;
    }

    public ConcurrentHashMap<UUID, DecentDisplay> getDHDisplays() {
        return dhShopDisplays;
    }
}