package xzot1k.plugins.ds.api.enums;

import org.jetbrains.annotations.NotNull;

public enum InteractionType {
    SELECT_SALE_ITEM, SELECT_TRADE_ITEM, CLEAR_SALE_ITEM, CLEAR_TRADE_ITEM, AMOUNT_BUY_PRICE,
    AMOUNT_SELL_PRICE, SHOP_ITEM_AMOUNT, AMOUNT_PLAYER_BUY_LIMIT,
    AMOUNT_PLAYER_SELL_LIMIT, AMOUNT_GLOBAL_BUY_LIMIT, AMOUNT_GLOBAL_SELL_LIMIT,
    AMOUNT_STOCK, AMOUNT_BALANCE, DELETE;

    /**
     * @param type The type string.
     * @return The potential matching type.
     */
    public static InteractionType getApproxType(@NotNull String type) {
        for (int i = -1; ++i < values().length; ) {
            final InteractionType it = values()[i];
            if (it.name().contains(type.toUpperCase().replace("-", "_")))
                return it;
        }

        return null;
    }

}
