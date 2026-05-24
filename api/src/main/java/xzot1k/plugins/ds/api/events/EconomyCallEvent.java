/*
 * Copyright (c) 2022 XZot1K, All rights reserved.
 */

package xzot1k.plugins.ds.api.events;

import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import xzot1k.plugins.ds.api.enums.EconomyCallType;
import xzot1k.plugins.ds.api.objects.Shop;

public class EconomyCallEvent extends Event implements Cancellable, ECEvent {
    private static final HandlerList handlers = new HandlerList();

    public static HandlerList getHandlerList() {
        return handlers;
    }

    /**
     * Initiates a withdrawal/deposit transaction directed at a player for a specific economy call type based on a passed shop.
     *
     * @param player          The player who initiated the economy call event.
     * @param shop            The shop in use.
     * @param economyCallType The economy type being processed Buy, Sell, etc.
     * @param price           The price in use.
     * @return the economy call event
     */
    public static EconomyCallEvent call(@NotNull Player player, @Nullable Shop shop, @NotNull EconomyCallType economyCallType, double price) {
        return null;
    }

    /**
     * Takes and gives currency from the players accordingly.
     *
     * @parm canNotBypass determines whether the investor will be charged for the transaction or not (If the type is NOT sell).
     */
    @Override
    public void performCurrencyTransfer(boolean canNotBypass) {
    }


    /**
     * Reverses all transactions and returns all modified currency balanced to their original state before it was taken/given.
     */
    @Override
    public void reverseCurrencyTransfer() {
    }

    @Override
    public boolean isCancelled() {
        return false;
    }

    @Override
    public void setCancelled(boolean b) {
    }

    @Override
    public Player getPlayer() {
        return null;
    }

    @Override
    public void setPlayer(@NotNull Player player) {
    }

    @Override
    public double getRawAmount() {
        return 0;
    }

    @Override
    public double getAmount() {
        return 0;
    }

    @Override
    public void setAmount(double amount) {
    }

    @Override
    public EconomyCallType getEconomyCallType() {
        return null;
    }

    @Override
    public @NotNull HandlerList getHandlers() {
        return handlers;
    }

    @Override
    public Shop getShop() {
        return null;
    }

    @Override
    public double getTax() {
        return 0;
    }

    @Override
    public void setTax(double tax) {
    }

    /**
     * Tells if the transaction will succeed based on if the investor and producer can both complete it.
     *
     * @return If the transaction succeeded.
     */
    @Override
    public boolean willSucceed() {
        return false;
    }

    @Override
    public void setWillSucceed(boolean willSucceed) {
    }

    @Override
    public boolean performedCurrencyTransfer() {
        return false;
    }

    @Override
    public void setPerformedCurrencyTransfer(boolean performedCurrencyTransfer) {
    }

    @Override
    public boolean playerHasEnough() {
        return false;
    }

    @Override
    public void setPlayerHasEnough(boolean playerHasEnough) {
    }

    @Override
    public boolean hasChargedPlayer() {
        return false;
    }

    @Override
    public void setChargedPlayer(boolean chargedPlayer) {
    }

    /**
     * @return Whether the event's checks failed or not.
     */
    @Override
    public boolean failed() {
        return false;
    }

    @Override
    public String getErrorMessage() {
        return null;
    }

    @Override
    public void setErrorMessage(@Nullable String message) {
    }

}