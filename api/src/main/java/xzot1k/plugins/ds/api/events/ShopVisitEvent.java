/*
 * Copyright (c) 2022 XZot1K, All rights reserved.
 */

package xzot1k.plugins.ds.api.events;

import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;
import xzot1k.plugins.ds.api.objects.Shop;

public class ShopVisitEvent extends Event implements Cancellable {
    private static final HandlerList handlers = new HandlerList();
    private Player player;
    private Shop shop;
    private double chargeAmount;
    private boolean cancelled;

    public ShopVisitEvent(@NotNull Player player, @NotNull Shop shop, double chargeAmount) {
        setPlayer(player);
        setShop(shop);
        setChargeAmount(chargeAmount);
        setCancelled(false);
    }

    public static HandlerList getHandlerList() {
        return handlers;
    }

    @Override
    public @NotNull HandlerList getHandlers() {
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

    public double getChargeAmount() {
        return chargeAmount;
    }

    public void setChargeAmount(double chargeAmount) {
        this.chargeAmount = chargeAmount;
    }

    public Shop getShop() {
        return shop;
    }

    private void setShop(@NotNull Shop shop) {
        this.shop = shop;
    }
}