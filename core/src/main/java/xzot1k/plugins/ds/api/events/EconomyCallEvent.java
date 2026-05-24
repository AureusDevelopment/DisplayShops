/*
 * Copyright (c) 2021 XZot1K, All rights reserved.
 */

package xzot1k.plugins.ds.api.events;

import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import xzot1k.plugins.ds.DisplayShops;
import xzot1k.plugins.ds.api.enums.EconomyCallType;
import xzot1k.plugins.ds.api.objects.Shop;

public class EconomyCallEvent extends Event implements Cancellable, ECEvent {

    private static final HandlerList handlers = new HandlerList();
    private final DisplayShops INSTANCE;
    private Player player;
    private double amount, tax;
    private boolean cancelled, playerHasEnough, willSucceed, performedCurrencyTransfer, chargedPlayer;
    private Shop shop;
    private EconomyCallType economyCallType;
    private String errorMessage;

    public EconomyCallEvent(@NotNull Player player, @NotNull EconomyCallType economyCallType, @Nullable Shop shop, double amount) {
        this.INSTANCE = DisplayShops.getPluginInstance();
        setShop(shop);
        setPlayer(player);
        setAmount(amount);
        setEconomyCallType(economyCallType);
        setPerformedCurrencyTransfer(false);
        setChargedPlayer(false);
        setTax(INSTANCE.getConfig().getDouble("transaction-tax"));

        if (getShop() != null && !getShop().isAdminShop()) {
            if (getEconomyCallType() == EconomyCallType.WITHDRAW_BALANCE) {
                final boolean canShopAfford = (getShop().getStoredBalance() >= getAmount());

                if (getShop().getCurrencyType().equals("item-for-item")) {
                    int totalSpace = INSTANCE.getManager().getInventorySpaceForItem(player, (getShop().getTradeItem() != null
                            ? getShop().getTradeItem() : INSTANCE.getManager().defaultCurrencyItem));
                    if (getAmount() > totalSpace) setAmount(totalSpace);
                    setPlayerHasEnough(totalSpace >= (getAmount()));
                    setWillSucceed(playerHasEnough() && canShopAfford);

                    if (!playerHasEnough) setErrorMessage(INSTANCE.getLangConfig().getString("insufficient-space"));
                    if (!canShopAfford) setErrorMessage(INSTANCE.getLangConfig().getString("owner-insufficient-funds"));
                    return;
                }

                setPlayerHasEnough(true);
                setWillSucceed(playerHasEnough() && canShopAfford);
                if (!canShopAfford) setErrorMessage(INSTANCE.getLangConfig().getString("owner-insufficient-funds"));
                return;
            } else if (getEconomyCallType() == EconomyCallType.DEPOSIT_BALANCE) {
                if (getShop().getCurrencyType().equals("item-for-item")) {
                    final int currentBalance = INSTANCE.getEconomyHandler().getItemForItemBalance(player, getShop());
                    setPlayerHasEnough(currentBalance >= amount);
                    setWillSucceed(playerHasEnough());
                    if (!playerHasEnough()) {
                        final String message = INSTANCE.getLangConfig().getString("insufficient-funds");
                        if (message != null && !message.isEmpty()) setErrorMessage(message
                                .replace("{price}", INSTANCE.getEconomyHandler().format(getShop(), getShop().getCurrencyType(), getAmount())));
                    }
                    return;
                }

                final boolean doesNotMeetMax = ((getShop().getStoredBalance() + getAmount()) < INSTANCE.getConfig().getLong("max-stored-currency"));
                setPlayerHasEnough(INSTANCE.getEconomyHandler().has(getPlayer(), getShop(), getTaxedAmount()));
                setWillSucceed(playerHasEnough() && doesNotMeetMax);
                if (!doesNotMeetMax) setErrorMessage(INSTANCE.getLangConfig().getString("max-stored-currency"));
                if (!playerHasEnough()) {
                    final String message = INSTANCE.getLangConfig().getString("insufficient-funds");
                    if (message != null && !message.isEmpty()) setErrorMessage(message
                            .replace("{price}", INSTANCE.getEconomyHandler().format(getShop(), getShop().getCurrencyType(), getAmount())));
                }
                return;
            } else if (getEconomyCallType() == EconomyCallType.CREATION_ITEM_COMMAND) {
                setPlayerHasEnough(getPlayer().hasPermission("displayshops.bypass") || INSTANCE.getEconomyHandler().has(getPlayer(), getShop(), getTaxedAmount(), economyCallType));
                return;
            }
        }

        final boolean isSell = (getEconomyCallType() == EconomyCallType.SELL);
        setPlayerHasEnough(getPlayer().hasPermission("displayshops.bypass") || INSTANCE.getEconomyHandler().has(getPlayer(), getShop(),
                (isSell ? (getAmount() / Math.max(1, getShop().getSellPrice(true))) : getTaxedAmount()), economyCallType));

        if (isSell && getShop() != null && !getShop().isAdminShop()) {
            final boolean useOwnerSyncing = INSTANCE.getConfig().getBoolean("sync-owner-balance");
            final OfflinePlayer shopOwner = INSTANCE.getServer().getOfflinePlayer(getShop().getOwnerUniqueId());
            setWillSucceed(playerHasEnough() && (getShop().isAdminShop() || (useOwnerSyncing ? INSTANCE.getEconomyHandler().has(shopOwner, getShop(), getTaxedAmount())
                    : (getShop().getStoredBalance() >= getTaxedAmount()))));
            return;
        }

        setWillSucceed(playerHasEnough());
    }

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
        final DisplayShops instance = DisplayShops.getPluginInstance();

        final EconomyCallEvent economyCallEvent = new EconomyCallEvent(player, economyCallType, shop, price);
        DisplayShops.getPluginInstance().getServer().getPluginManager().callEvent(economyCallEvent);

        if (economyCallEvent.isCancelled()) {
            if (price <= 0) economyCallEvent.setWillSucceed(true);
            return economyCallEvent;
        } else if (price <= 0) {
            economyCallEvent.setWillSucceed(true);
            return economyCallEvent;
        }

        final boolean isSellType = (economyCallType == EconomyCallType.SELL);
        if (isSellType && shop != null && !shop.isAdminShop() && economyCallEvent.playerHasEnough() && !economyCallEvent.willSucceed()) {
            final String message = instance.getLangConfig().getString("owner-insufficient-funds");
            if (message != null && !message.equalsIgnoreCase("")) instance.getManager().sendMessage(player, message);
            return economyCallEvent;
        }

        final boolean canNotBypass = !player.hasPermission("displayshops.bypass");
        if (canNotBypass) {
            if (!economyCallEvent.playerHasEnough()) {
                player.closeInventory();

                final String message = instance.getLangConfig().getString("insufficient-funds");
                if (message != null && !message.equalsIgnoreCase(""))
                    instance.getManager().sendMessage(player, message.replace("{price}", ((shop != null) ? (instance.getEconomyHandler().format(shop,
                            shop.getCurrencyType(), (!isSellType ? economyCallEvent.getTaxedAmount() : shop.getShopItemAmount()), economyCallType))
                            : instance.getManager().formatNumber(economyCallEvent.getTaxedAmount(), true))));
                return economyCallEvent;
            }
        }

        if (economyCallEvent.willSucceed()) economyCallEvent.performCurrencyTransfer(canNotBypass);
        else if (economyCallEvent.getErrorMessage() != null)
            DisplayShops.getPluginInstance().getManager().sendMessage(player, economyCallEvent.getErrorMessage());
        return economyCallEvent;
    }

    /**
     * Takes and gives currency from the players accordingly.
     *
     * @parm canNotBypass determines whether the investor will be charged for the transaction or not (If the type is NOT sell).
     */
    public void performCurrencyTransfer(boolean canNotBypass) {
        if (performedCurrencyTransfer()) return;
        setPerformedCurrencyTransfer(true);

        switch (getEconomyCallType()) {
            case WITHDRAW_BALANCE: {
                INSTANCE.getEconomyHandler().deposit(getPlayer(), getShop(), getAmount(), getEconomyCallType());
                getShop().setStoredBalance(Math.max((getShop().getStoredBalance() - getAmount()), 0));
                shop.updateTimeStamp();
                shop.save(true);
                return;
            }
            case DEPOSIT_BALANCE: {
                INSTANCE.getEconomyHandler().withdraw(getPlayer(), getShop(), getAmount(), getEconomyCallType());
                getShop().setStoredBalance(Math.max((getShop().getStoredBalance() + getAmount()), 0));
                shop.updateTimeStamp();
                shop.save(true);
                return;
            }
            default: {
                break;
            }
        }

        if (getEconomyCallType() == EconomyCallType.SELL) {
            if (getShop() != null && !getShop().isAdminShop()) {
                if (INSTANCE.getConfig().getBoolean("sync-owner-balance")) {
                    final OfflinePlayer shopOwner = INSTANCE.getServer().getOfflinePlayer(getShop().getOwnerUniqueId());
                    INSTANCE.getEconomyHandler().withdraw(shopOwner, getShop(), getTaxedAmount(), economyCallType);
                } else getShop().setStoredBalance(Math.max((getShop().getStoredBalance() - getTaxedAmount()), 0));
            }

            INSTANCE.getEconomyHandler().deposit(getPlayer(), getShop(), getAmount(), getEconomyCallType());
            return;
        }

        if (!hasChargedPlayer() && canNotBypass) {
            INSTANCE.getEconomyHandler().withdraw(getPlayer(), getShop(), getTaxedAmount(), getEconomyCallType());
            setChargedPlayer(true);
        }

        if ((getEconomyCallType() != EconomyCallType.EDIT_ACTION && getEconomyCallType() != EconomyCallType.APPEARANCE
                && getEconomyCallType() != EconomyCallType.RENT && getEconomyCallType() != EconomyCallType.RENT_RENEW)
                && getShop() != null && !getShop().isAdminShop()) {
            if (INSTANCE.getConfig().getBoolean("sync-owner-balance")) {
                final OfflinePlayer shopOwner = INSTANCE.getServer().getOfflinePlayer(getShop().getOwnerUniqueId());
                INSTANCE.getEconomyHandler().deposit(shopOwner, getShop(), getAmount(), getEconomyCallType());
                return;
            }

            getShop().setStoredBalance(Math.min(INSTANCE.getConfig().getDouble("max-stored-currency"), (getShop().getStoredBalance() + getAmount())));
        }
    }

    /**
     * Reverses all transactions and returns all modified currency balanced to their original state before it was taken/given.
     */
    public void reverseCurrencyTransfer() {
        if (!performedCurrencyTransfer()) return;

        if (getEconomyCallType() == EconomyCallType.SELL) {
            INSTANCE.getEconomyHandler().withdraw(getPlayer(), getShop(), getAmount(), economyCallType);

            if (!getShop().isAdminShop()) {
                if (INSTANCE.getConfig().getBoolean("sync-owner-balance")) {
                    final OfflinePlayer shopOwner = INSTANCE.getServer().getOfflinePlayer(getShop().getOwnerUniqueId());
                    INSTANCE.getEconomyHandler().deposit(shopOwner, getShop(), getTaxedAmount(), economyCallType);
                    return;
                }

                getShop().setStoredBalance(Math.min(INSTANCE.getConfig().getDouble("max-stored-currency"),
                        (getShop().getStoredBalance() + getTaxedAmount())));
            }
            return;
        }

        if (hasChargedPlayer() && !getPlayer().hasPermission("displayshops.bypass"))
            INSTANCE.getEconomyHandler().deposit(getPlayer(), getShop(), getTaxedAmount());

        if (!getShop().isAdminShop()) {
            if (INSTANCE.getConfig().getBoolean("sync-owner-balance")) {
                final OfflinePlayer shopOwner = INSTANCE.getServer().getOfflinePlayer(getShop().getOwnerUniqueId());
                INSTANCE.getEconomyHandler().withdraw(shopOwner, getShop(), getAmount());
                return;
            }

            getShop().setStoredBalance(Math.max(0, (getShop().getStoredBalance() - getAmount())));
        }
    }

    /**
     * @return Whether the event's checks failed or not.
     */
    public boolean failed() {
        return (isCancelled() || !willSucceed());
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

    @Override
    public @NotNull HandlerList getHandlers() {
        return handlers;
    }

    // getters & setters

    public Player getPlayer() {
        return player;
    }

    public void setPlayer(@NotNull Player player) {
        this.player = player;
    }

    public double getRawAmount() {
        return amount;
    }

    public double getAmount() {
        return amount;
    }

    public void setAmount(double amount) {
        this.amount = amount;
    }

    public double getTaxedAmount() {
        return (getAmount() + (getAmount() * getTax()));
    }

    public EconomyCallType getEconomyCallType() {
        return economyCallType;
    }

    private void setEconomyCallType(EconomyCallType economyCallType) {
        this.economyCallType = economyCallType;
    }

    public Shop getShop() {
        return shop;
    }

    private void setShop(Shop shop) {
        this.shop = shop;
    }

    public double getTax() {
        return tax;
    }

    public void setTax(double tax) {
        this.tax = tax;
    }

    /**
     * Tells if the transaction will succeed based on if the investor and producer can both complete it.
     *
     * @return If the transaction succeeded.
     */
    public boolean willSucceed() {
        return willSucceed;
    }

    public void setWillSucceed(boolean willSucceed) {
        this.willSucceed = willSucceed;
    }

    public boolean performedCurrencyTransfer() {
        return performedCurrencyTransfer;
    }

    public void setPerformedCurrencyTransfer(boolean performedCurrencyTransfer) {
        this.performedCurrencyTransfer = performedCurrencyTransfer;
    }

    public boolean playerHasEnough() {
        return playerHasEnough;
    }

    public void setPlayerHasEnough(boolean playerHasEnough) {
        this.playerHasEnough = playerHasEnough;
    }

    public boolean hasChargedPlayer() {
        return chargedPlayer;
    }

    public void setChargedPlayer(boolean chargedPlayer) {
        this.chargedPlayer = chargedPlayer;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(@Nullable String message) {
        this.errorMessage = message;
    }

}