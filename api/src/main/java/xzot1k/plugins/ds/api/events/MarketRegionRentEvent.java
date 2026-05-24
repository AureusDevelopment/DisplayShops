/*
 * Copyright (c) 2022 XZot1K, All rights reserved.
 */
package xzot1k.plugins.ds.api.events;

import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;
import xzot1k.plugins.ds.api.objects.MarketRegion;

public class MarketRegionRentEvent extends Event implements Cancellable {
    private static final HandlerList handlers = new HandlerList();
    private final Player player;
    private final MarketRegion marketRegion;
    private boolean cancelled, renewal;

    public MarketRegionRentEvent(@NotNull Player player, @NotNull MarketRegion marketRegion, boolean renewal) {
        this.player = player;
        this.marketRegion = marketRegion;
        setCancelled(false);
        setRenew(renewal);
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

    @Override
    public @NotNull HandlerList getHandlers() {
        return handlers;
    }

    public boolean isRenewal() {
        return renewal;
    }

    public void setRenew(boolean renewal) {
        this.renewal = renewal;
    }

    public MarketRegion getMarketRegion() {
        return marketRegion;
    }
}