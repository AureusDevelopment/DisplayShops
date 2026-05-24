package xzot1k.plugins.ds.api.enums;

import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import xzot1k.plugins.ds.DisplayShops;
import xzot1k.plugins.ds.api.eco.EcoHook;
import xzot1k.plugins.ds.api.objects.Menu;
import xzot1k.plugins.ds.api.objects.Shop;

public enum ShopActionType {

    BUY, SELL, BOTH;

    /**
     * Gets the current ShopActionType from the item in the visit menu.
     *
     * @param itemStack The item to decipher.
     * @param visitMenu The global visit menu.
     * @return The current type (returns BOTH, if null).
     */
    public static ShopActionType getTypeFromItem(@NotNull ItemStack itemStack, @NotNull Menu visitMenu) {
        final String nameFormat = visitMenu.getConfiguration().getString("buttons.type.name");
        if (nameFormat != null) {

            String current = DisplayShops.getPluginInstance().getManager().getValueFromPlaceholder(itemStack, nameFormat, "{type}");
            if (current != null) {
                current = current.toUpperCase().replace(" ", "_");
                for (int i = -1; ++i < values().length; ) {
                    final ShopActionType type = values()[i];
                    if (type.getName(visitMenu).equalsIgnoreCase(current)) return type;
                }
            }
        }
        return BOTH;
    }

    public ShopActionType[] getAllowedTypes() {
        if (this == ShopActionType.BOTH) return new ShopActionType[]{BUY, SELL};
        return new ShopActionType[]{this};
    }

    public String getName(@NotNull Menu visitMenu) {
        return visitMenu.getConfiguration().getString("visit-types." + name().toLowerCase().replace("_", "-"));
    }

    public ShopActionType getNext() {
        for (int i = -1; ++i < values().length; ) {
            final ShopActionType type = values()[i];
            if (type == this) return (((i + 1) >= values().length) ? values()[0] : values()[i + 1]);
        }
        return BOTH;
    }

    public boolean passesCheck(@NotNull Shop shop) {
        final double buyPrice = shop.getBuyPrice(shop.canDynamicPriceChange()),
                sellPrice = shop.getSellPrice(shop.canDynamicPriceChange());

        boolean balanceCheck = true;
        if (!shop.isAdminShop()) {
            balanceCheck = (shop.getStoredBalance() >= sellPrice);
            if (DisplayShops.getPluginInstance().getConfig().getBoolean("sync-owner-balance")) {
                EcoHook ecoHook = DisplayShops.getPluginInstance().getEconomyHandler().getEcoHook(shop.getCurrencyType());
                if (ecoHook != null) balanceCheck = (ecoHook.getBalance(shop.getOwnerUniqueId()) >= sellPrice);
            }
        }

        switch (this) {
            case BUY: {
                return (buyPrice >= 0 && sellPrice < 0 && shop.getStock() > 0 && shop.getStock() >= shop.getShopItemAmount());
            }
            case SELL: {
                return (sellPrice >= 0 && buyPrice < 0 && shop.getStock() < shop.getMaxStock() && balanceCheck);
            }
            default: {
                return (buyPrice >= 0 && sellPrice >= 0 && shop.getStock() > 0 && shop.getStock() >= shop.getShopItemAmount()
                        && shop.getStock() < shop.getMaxStock() && balanceCheck);
            }
        }
    }
}
