/*
 * Copyright (c) 2022 XZot1K, All rights reserved.
 */

package xzot1k.plugins.ds.api.events;

import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;
import xzot1k.plugins.ds.api.enums.EditType;
import xzot1k.plugins.ds.api.objects.Shop;

public class ShopEditEvent extends Event implements Cancellable {
    private static final HandlerList handlers = new HandlerList();
    private final Player player;
    private Shop shop;
    private EditType editType;
    private double amount;
    private String value;
    private boolean cancelled;

    public ShopEditEvent(@NotNull Player player, @NotNull Shop shop, @NotNull EditType editType) {
        this.player = player;
        setShop(shop);
        setEditType(editType);
        setAmount(amount);
        setValue(null);
        setCancelled(false);
    }

    public ShopEditEvent(@NotNull Player player, @NotNull Shop shop, @NotNull EditType editType, double amount) {
        this.player = player;
        setShop(shop);
        setEditType(editType);
        setAmount(amount);
        setValue(null);
        setCancelled(false);
    }

    public ShopEditEvent(@NotNull Player player, @NotNull Shop shop, @NotNull EditType editType, String value) {
        this.player = player;
        setShop(shop);
        setEditType(editType);
        setAmount(0);
        setValue(value);
        setCancelled(false);
    }

    // getters & setters
    public static HandlerList getHandlerList() {
        return handlers;
    }

    @Override
    public @NotNull HandlerList getHandlers() {
        return handlers;
    }

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

    public Shop getShop() {
        return shop;
    }

    public void setShop(Shop shop) {
        this.shop = shop;
    }

    public EditType getEditType() {
        return editType;
    }

    public void setEditType(EditType editType) {
        this.editType = editType;
    }

    public double getAmount() {
        return amount;
    }

    public void setAmount(double amount) {
        this.amount = amount;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }
}