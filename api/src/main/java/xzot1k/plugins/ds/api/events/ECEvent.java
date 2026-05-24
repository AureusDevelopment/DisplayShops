/*
 * Copyright (c) 2022 XZot1K, All rights reserved.
 */

package xzot1k.plugins.ds.api.events;

import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import xzot1k.plugins.ds.api.enums.EconomyCallType;
import xzot1k.plugins.ds.api.objects.Shop;

public interface ECEvent {
    /**
     * Takes and gives currency from the players accordingly.
     *
     * @param chargeInvestor determines whether the investor will be charged for the transaction or not (If the type is NOT sell).
     */
    void performCurrencyTransfer(boolean chargeInvestor);

    /**
     * Reverses all transactions and returns all modified currency balanced to their original state before it was taken/given.
     */
    void reverseCurrencyTransfer();

    // getters & setters
    boolean isCancelled();

    void setCancelled(boolean cancelled);

    /**
     * @return The player who initiated the economy call event.
     */
    Player getPlayer();

    /**
     * Sets the player who initiated the economy call event.
     *
     * @param player The player who initiated the event.
     */
    void setPlayer(@NotNull Player player);

    /**
     * @return The base amount of currency (without tax applied).
     */
    double getRawAmount();

    /**
     * @return The amount of currency the transaction is handling (Tax, defined in the config.yml, is applied automatically).
     */
    double getAmount();

    /**
     * Sets the amount of currency the transaction is handling (Does NOT include tax).
     *
     * @param amount The raw amount of currency.
     */
    void setAmount(double amount);

    EconomyCallType getEconomyCallType();

    HandlerList getHandlers();

    Shop getShop();

    double getTax();

    void setTax(double tax);

    /**
     * Tells if the transaction will succeed based on if the investor and producer can both complete it.
     *
     * @return If the transaction succeeded.
     */
    boolean willSucceed();

    void setWillSucceed(boolean willSucceed);

    boolean performedCurrencyTransfer();

    void setPerformedCurrencyTransfer(boolean performedCurrencyTransfer);

    boolean playerHasEnough();

    void setPlayerHasEnough(boolean playerHasEnough);

    boolean hasChargedPlayer();

    void setChargedPlayer(boolean chargedPlayer);

    /**
     * @return Whether the event's checks failed or not.
     */
    boolean failed();

    String getErrorMessage();

    void setErrorMessage(@Nullable String message);

}