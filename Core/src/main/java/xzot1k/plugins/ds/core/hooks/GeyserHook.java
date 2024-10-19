/*
package xzot1k.plugins.ds.core.hooks;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.geysermc.cumulus.form.CustomForm;
import org.geysermc.event.subscribe.Subscribe;
import org.geysermc.geyser.api.GeyserApi;
import org.geysermc.geyser.api.connection.GeyserConnection;
import org.geysermc.geyser.api.event.EventRegistrar;
import org.geysermc.geyser.api.event.lifecycle.GeyserPostInitializeEvent;
import org.jetbrains.annotations.NotNull;
import xzot1k.plugins.ds.DisplayShops;

import javax.annotation.Nullable;
import java.util.UUID;
import java.util.logging.Level;

public class GeyserHook implements EventRegistrar {

    private final DisplayShops main;
    private final GeyserApi geyser;

    public GeyserHook(@NotNull DisplayShops main){
        this.main=main;
        geyser = GeyserApi.api();
        geyser.eventBus().subscribe(this,GeyserPostInitializeEvent.class,this::onGeyser);
    }


    @Nullable
    public GeyserConnection getPlayer(@NotNull UUID uuid){
        return geyser.connectionByUuid(uuid);
    }

    @Nullable
    public GeyserConnection getPlayer(@NotNull String player){
        Player p = Bukkit.getPlayer(player);
        if(p==null)
            return null;
        return getPlayer(p.getUniqueId());
    }

    @Nullable
    public GeyserConnection getPlayer(@NotNull Player player){
        return getPlayer(player.getUniqueId());
    }

    @Subscribe
    public void onGeyser(GeyserPostInitializeEvent e){
        main.log(Level.SEVERE,"Geyser initialized!");
    }
}
*/
