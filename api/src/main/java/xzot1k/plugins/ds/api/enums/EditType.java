/*
 * Copyright (c) 2022 XZot1K, All rights reserved.
 */

package xzot1k.plugins.ds.api.enums;

public enum EditType {
    OPEN_EDIT_MENU(false, false), QUICK_WITHDRAW(true, false),
    QUICK_DEPOSIT(true, false), DEPOSIT_STOCK(true, false),
    WITHDRAW_STOCK(true, false), STOCK_SET(true, false),
    DEPOSIT_BALANCE(true, false), WITHDRAW_BALANCE(true, false),
    BUY_PRICE(true, false), SELL_PRICE(true, false),
    SHOP_ITEM_AMOUNT(true, false), PLAYER_BUY_LIMIT(true, false),
    PLAYER_SELL_LIMIT(true, false), GLOBAL_BUY_LIMIT(true, false),
    GLOBAL_SELL_LIMIT(true, false), APPEARANCE_CHANGE(false, true),
    ASSISTANT_ADD(false, true), ASSISTANT_REMOVE(false, true),
    DESCRIPTION_CHANGE(false, true), CLEAR_LIMITS(false, false),
    DELETE(false, false), CLEAR_SALE_ITEM(false, true),
    CLEAR_TRADE_ITEM(false, true), CURRENCY_CHANGE(false, true),
    COMMAND_ADD(false, true), COMMAND_REMOVE(true, false);

    private final boolean hasAmount, hasStringValue;

    EditType(boolean hasAmount, boolean hasStringValue) {
        this.hasAmount = hasAmount;
        this.hasStringValue = hasStringValue;
    }

    public boolean hasAmount() {
        return hasAmount;
    }

    public boolean hasStringValue() {
        return hasStringValue;
    }
}