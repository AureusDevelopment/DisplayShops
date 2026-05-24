/*
 * Copyright (c) 2022 XZot1K, All rights reserved.
 */
package xzot1k.plugins.ds.api.events;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

public class ShopCreationEvent extends Event implements Cancellable {
    private static final HandlerList handlers = new HandlerList();
    private Player player;
    private boolean cancelled, cancelBlockPlaceEvent;
    private Location location;

    public ShopCreationEvent(@NotNull Player player, @NotNull Location location) {
        setPlayer(player);
        setLocation(location);
        setCancelled(false);
        setCancelBlockPlaceEvent(false);
    }

    public static HandlerList getHandlerList() {
        return handlers;
    }

    // getters & setters
    @Override
    public boolean isCancelled() {
        return cancelled;
    }

    @Override
    public void setCancelled(boolean cancelled) {
        this.cancelled = cancelled;
    }

    public Player getPlayer() {
        return player;
    }

    private void setPlayer(@NotNull Player player) {
        this.player = player;
    }

    @Override
    public @NotNull HandlerList getHandlers() {
        return handlers;
    }

    public Location getLocation() {
        return location;
    }

    public void setLocation(@NotNull Location location) {
        this.location = location;
    }

    public boolean isCancelBlockPlaceEvent() {
        return cancelBlockPlaceEvent;
    }

    public void setCancelBlockPlaceEvent(boolean cancelBlockPlaceEvent) {
        this.cancelBlockPlaceEvent = cancelBlockPlaceEvent;
    }
}