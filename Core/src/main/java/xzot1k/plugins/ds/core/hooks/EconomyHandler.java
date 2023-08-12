package xzot1k.plugins.ds.core.hooks;

import com.willfp.ecobits.currencies.CurrencyUtils;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import xzot1k.plugins.ds.DisplayShops;
import xzot1k.plugins.ds.api.eco.EcoHandler;
import xzot1k.plugins.ds.api.eco.EcoHook;
import xzot1k.plugins.ds.api.enums.EconomyCallType;
import xzot1k.plugins.ds.api.objects.DataPack;
import xzot1k.plugins.ds.api.objects.Shop;

import java.io.File;
import java.math.BigDecimal;
import java.util.*;

public class EconomyHandler implements EcoHandler {

    private final DisplayShops INSTANCE;
    private final HashMap<String, EcoHook> economyRegistry;
    private Economy vaultEconomy;

    public EconomyHandler(@NotNull DisplayShops instance) {
        this.INSTANCE = instance;
        this.economyRegistry = new HashMap<>();

        reset();
    }

    /**
     * Reloads all EcoHooks and the EconomyHandler in general.
     */
    public void reset() {
        setupItemForItem();
        setupVaultEconomy();
        setupPlayerPoints();
        setupEcoBits();
    }

    // pre-made registration
    private void setupItemForItem() {
        EcoHook ecoHook = new EcoHook() {
            @Override
            public String getSingularName() {return "item-for-item";}

            @Override
            public String getPluralName() {return "item-for-item";}

            @Override
            public boolean deposit(@NotNull UUID playerUniqueId, double amount) {
                final Player player = INSTANCE.getServer().getPlayer(playerUniqueId);
                if (player == null) return false;

                final DataPack dataPack = INSTANCE.getManager().getDataPack(player);
                final Shop shop = dataPack.getSelectedShop();

                return EconomyHandler.this.deposit(player, shop, amount);
            }

            @Override
            public boolean deposit(@NotNull OfflinePlayer player, double amount) {
                if (player.getPlayer() == null) return false;

                final DataPack dataPack = INSTANCE.getManager().getDataPack(player.getPlayer());
                final Shop shop = dataPack.getSelectedShop();

                return EconomyHandler.this.deposit(player, shop, amount);
            }

            @Override
            public boolean withdraw(@NotNull UUID playerUniqueId, double amount) {
                final Player player = INSTANCE.getServer().getPlayer(playerUniqueId);
                if (player == null) return false;

                final DataPack dataPack = INSTANCE.getManager().getDataPack(player);
                final Shop shop = dataPack.getSelectedShop();

                return EconomyHandler.this.withdraw(player, shop, amount);
            }

            @Override
            public boolean withdraw(@NotNull OfflinePlayer player, double amount) {
                if (player.getPlayer() == null) return false;

                final DataPack dataPack = INSTANCE.getManager().getDataPack(player.getPlayer());
                final Shop shop = dataPack.getSelectedShop();

                return EconomyHandler.this.withdraw(player, shop, amount);
            }

            @Override
            public double getBalance(@NotNull UUID playerUniqueId) {
                final Player player = INSTANCE.getServer().getPlayer(playerUniqueId);
                if (player == null) return 0;

                final DataPack dataPack = INSTANCE.getManager().getDataPack(player);
                final Shop shop = dataPack.getSelectedShop();

                return EconomyHandler.this.getBalance(player, shop);
            }

            @Override
            public double getBalance(@NotNull OfflinePlayer player) {
                if (player.getPlayer() == null) return 0;

                final DataPack dataPack = INSTANCE.getManager().getDataPack(player.getPlayer());
                final Shop shop = dataPack.getSelectedShop();

                return EconomyHandler.this.getBalance(player, shop);
            }
        };

        getEconomyRegistry().put("item-for-item", ecoHook);
        loadExtraData("item-for-item", ecoHook);
    }

    public void setupVaultEconomy() {
        if (INSTANCE.getServer().getPluginManager().getPlugin("Vault") == null || !INSTANCE.getConfig().getBoolean("use-vault")) return;

        RegisteredServiceProvider<Economy> rsp = INSTANCE.getServer().getServicesManager().getRegistration(Economy.class);
        if (rsp == null) return;

        this.vaultEconomy = rsp.getProvider();

        EcoHook ecoHook = new EcoHook() {
            @Override
            public String getSingularName() {
                return ((getVaultEconomy().currencyNameSingular() == null
                        || getVaultEconomy().currencyNameSingular().isEmpty()) ? getVaultEconomy().getName() : getVaultEconomy().currencyNameSingular());
            }

            @Override
            public String getPluralName() {
                return ((getVaultEconomy().currencyNamePlural() == null
                        || getVaultEconomy().currencyNamePlural().isEmpty()) ? getVaultEconomy().getName() : getVaultEconomy().currencyNamePlural());
            }

            @Override
            public boolean deposit(@NotNull UUID playerUniqueId, double amount) {
                final OfflinePlayer offlinePlayer = INSTANCE.getServer().getOfflinePlayer(playerUniqueId);
                return getVaultEconomy().depositPlayer(offlinePlayer, amount).transactionSuccess();
            }

            @Override
            public boolean deposit(@NotNull OfflinePlayer player, double amount) {return getVaultEconomy().depositPlayer(player, amount).transactionSuccess();}

            @Override
            public boolean withdraw(@NotNull UUID playerUniqueId, double amount) {
                final OfflinePlayer offlinePlayer = INSTANCE.getServer().getOfflinePlayer(playerUniqueId);
                return getVaultEconomy().withdrawPlayer(offlinePlayer, amount).transactionSuccess();
            }

            @Override
            public boolean withdraw(@NotNull OfflinePlayer player, double amount) {return getVaultEconomy().withdrawPlayer(player, amount).transactionSuccess();}

            @Override
            public double getBalance(@NotNull UUID playerUniqueId) {
                final OfflinePlayer offlinePlayer = INSTANCE.getServer().getOfflinePlayer(playerUniqueId);
                return getVaultEconomy().getBalance(offlinePlayer);
            }

            @Override
            public double getBalance(@NotNull OfflinePlayer player) {return getVaultEconomy().getBalance(player);}
        };

        getEconomyRegistry().put("Vault", ecoHook);
        loadExtraData(getVaultEconomy().getName(), ecoHook);
    }

    private void setupPlayerPoints() {
        Plugin ppPlugin = INSTANCE.getServer().getPluginManager().getPlugin("PlayerPoints");
        if (ppPlugin == null || ppPlugin.getConfig().getBoolean("vault")) return;

        final String locale = ppPlugin.getConfig().getString("locale");
        File localeFile = new File(ppPlugin.getDataFolder().getPath(), "/locale/" + locale + ".yml");
        if (localeFile.exists()) {
            FileConfiguration config = YamlConfiguration.loadConfiguration(localeFile);
            final String singular = config.getString("currency-singular"),
                    plural = config.getString("currency-plural");

            EcoHook ecoHook = new EcoHook() {
                @Override
                public String getSingularName() {return singular;}

                @Override
                public String getPluralName() {return plural;}

                @Override
                public boolean deposit(@NotNull UUID playerUniqueId, double amount) {
                    return org.black_ixx.playerpoints.PlayerPoints.getInstance().getAPI().give(playerUniqueId, Math.max(0, (int) amount));
                }

                @Override
                public boolean deposit(@NotNull OfflinePlayer player, double amount) {return deposit(player.getUniqueId(), amount);}

                @Override
                public boolean withdraw(@NotNull UUID playerUniqueId, double amount) {
                    return org.black_ixx.playerpoints.PlayerPoints.getInstance().getAPI().take(playerUniqueId, Math.max(0, (int) amount));
                }

                @Override
                public boolean withdraw(@NotNull OfflinePlayer player, double amount) {return withdraw(player.getUniqueId(), amount);}

                @Override
                public double getBalance(@NotNull UUID playerUniqueId) {return org.black_ixx.playerpoints.PlayerPoints.getInstance().getAPI().look(playerUniqueId);}

                @Override
                public double getBalance(@NotNull OfflinePlayer player) {return getBalance(player.getUniqueId());}
            };

            final String id = org.black_ixx.playerpoints.PlayerPoints.getInstance().getName();
            getEconomyRegistry().put(id, ecoHook);
            loadExtraData(id, ecoHook);
        }
    }

    private void setupEcoBits() {
        Plugin ecoBitsPlugin = INSTANCE.getServer().getPluginManager().getPlugin("EcoBits");
        if (ecoBitsPlugin == null) return;

        for (int i = -1; ++i < com.willfp.ecobits.currencies.Currencies.values().size(); ) {
            final com.willfp.ecobits.currencies.Currency currency = com.willfp.ecobits.currencies.Currencies.values().get(i);
            if (currency.isRegisteredWithVault()) continue;

            EcoHook ecoHook = new EcoHook() {
                @Override
                public String getSingularName() {return currency.getName();}

                @Override
                public String getPluralName() {return currency.getName();}

                @Override
                public boolean deposit(@NotNull UUID playerUniqueId, double amount) {
                    try {
                        final OfflinePlayer offlinePlayer = INSTANCE.getServer().getOfflinePlayer(playerUniqueId);
                        CurrencyUtils.adjustBalance(offlinePlayer, currency, BigDecimal.valueOf(amount));
                        return true;
                    } catch (Exception ignored) {return false;}
                }

                @Override
                public boolean deposit(@NotNull OfflinePlayer player, double amount) {
                    try {
                        CurrencyUtils.adjustBalance(player, currency, BigDecimal.valueOf(amount));
                        return true;
                    } catch (Exception ignored) {return false;}
                }

                @Override
                public boolean withdraw(@NotNull UUID playerUniqueId, double amount) {
                    final OfflinePlayer offlinePlayer = INSTANCE.getServer().getOfflinePlayer(playerUniqueId);
                    if (CurrencyUtils.getBalance(offlinePlayer, currency).doubleValue() >= amount) {
                        CurrencyUtils.adjustBalance(offlinePlayer, currency, BigDecimal.valueOf((amount >= 0 ? -amount : amount)));
                        return true;
                    } else return false;
                }

                @Override
                public boolean withdraw(@NotNull OfflinePlayer player, double amount) {
                    if (CurrencyUtils.getBalance(player, currency).doubleValue() >= amount) {
                        CurrencyUtils.adjustBalance(player, currency, BigDecimal.valueOf((amount >= 0 ? -amount : amount)));
                        return true;
                    } else return false;
                }

                @Override
                public double getBalance(@NotNull UUID playerUniqueId) {
                    final OfflinePlayer offlinePlayer = INSTANCE.getServer().getOfflinePlayer(playerUniqueId);
                    return CurrencyUtils.getBalance(offlinePlayer, currency).doubleValue();
                }

                @Override
                public double getBalance(@NotNull OfflinePlayer player) {return CurrencyUtils.getBalance(player, currency).doubleValue();}
            };

            getEconomyRegistry().put(currency.getId(), ecoHook);
            loadExtraData(currency.getId(), ecoHook);
        }
    }

    // helper methods
    public EcoHook getEcoHook(@NotNull String currencyType) {
        Map.Entry<String, EcoHook> optionalEcoHook = getEconomyRegistry().entrySet().parallelStream().filter(entry ->
                entry.getKey().equalsIgnoreCase(currencyType)).findAny().orElse(null);
        return ((optionalEcoHook != null) ? optionalEcoHook.getValue() : null);
    }

    /**
     * Checks to see if the player can use the currency.
     *
     * @param player       The player to check.
     * @param currencyName The name of the currency.
     * @return Whether the player can use the currency.
     */
    public boolean canUseCurrency(@NotNull Player player, @NotNull String currencyName) {
        if (player.hasPermission("displayshops.currency.*") || player.hasPermission("displayshops.currency." + currencyName)) return true;
        final List<String> blacklistedCurrencies = INSTANCE.getConfig().getStringList("currency-blacklist");
        for (int i = -1; ++i < blacklistedCurrencies.size(); ) {
            final String blacklistedCurrency = blacklistedCurrencies.get(i);
            if (blacklistedCurrency.equalsIgnoreCase(currencyName)) return false;
        }
        return true;
    }

    /**
     * Gets the next currency in the list starting from the shop's currently selected currency.
     *
     * @param player The player to check permissions for.
     * @param shop   The shop to obtain the selected currency.
     * @return The next available currency for selection.
     */
    public String determineNextCurrencyCycle(@NotNull Player player, @NotNull Shop shop) {
        if (getEconomyRegistry().keySet().size() == 1) return "item-for-item";

        final List<String> currencies = new ArrayList<>(getEconomyRegistry().keySet());
        int nextIndex = 0;
        for (int i = -1; ++i < currencies.size(); ) {
            final String currency = currencies.get(i);
            if (currency.equalsIgnoreCase(shop.getCurrencyType())) {
                if ((nextIndex + 1) < currencies.size()) nextIndex++;
                break;
            }
        }

        String nextCurrency = currencies.get(nextIndex);
        while (nextCurrency.equals(shop.getCurrencyType()) || !canUseCurrency(player, nextCurrency)) {
            nextCurrency = currencies.get(nextIndex);
            if ((nextIndex + 1) >= currencies.size()) nextIndex = 0;
            else nextIndex++;
        }

        return nextCurrency;
    }

    private void loadExtraData(@NotNull String currencyType, @NotNull EcoHook ecoHook) {
        ConfigurationSection currencySettingsSection = INSTANCE.getConfig().getConfigurationSection("currency-settings");
        if (currencySettingsSection == null) return;

        for (String currency : currencySettingsSection.getKeys(false)) {
            if (!currency.equalsIgnoreCase(currencyType)) continue;

            ConfigurationSection currencySection = currencySettingsSection.getConfigurationSection(currency);
            if (currencySection == null) continue;

            if (currencySection.contains("name")) {
                final String newName = currencySection.getString("name");
                if (newName != null) ecoHook.setAltName(newName);
            }

            if (currencySection.contains("symbol")) {
                final String newName = currencySection.getString("symbol");
                if (newName != null) ecoHook.setSymbol(newName);
            }

            if (currencySection.contains("decimal-placement")) ecoHook.setDecimalPlacement(currencySection.getInt("decimal-placement"));
            break;
        }
    }

    /**
     * Gets the balance the player has based on a shop (determines currency item automatically).
     *
     * @param player The player to check.
     * @param shop   The shop to obtain information from.
     * @param economyCallType If provided "SELL", the shop's item will be checked instead of the currency item.
     * @return The determined amount the player has (returns -1 if the shop uses the wrong currency or another issue occurs).
     */
    public int getItemForItemBalance(@NotNull OfflinePlayer player, @NotNull Shop shop, @Nullable EconomyCallType... economyCallType) {
        if (shop.getCurrencyType().equals("item-for-item") && player.getPlayer() != null) {
            if (economyCallType != null && economyCallType.length > 0 && economyCallType[0] == EconomyCallType.SELL)
                return INSTANCE.getManager().getItemAmount(player.getPlayer().getInventory(), shop.getShopItem());

            final ItemStack currencyItem = (INSTANCE.getConfig().getBoolean("shop-currency-item.force-use") || shop.getTradeItem() == null) ?
                    INSTANCE.getManager().defaultCurrencyItem : shop.getTradeItem();
            return INSTANCE.getManager().getItemAmount(player.getPlayer().getInventory(), currencyItem);
        }
        return -1;
    }

    /**
     * Check if the player has enough of a currency (If a shop is provided, the shop's selected currency will be used).
     *
     * @param player The player to check the balance against.
     * @param shop   The shop to base currency on.
     * @param amount The amount to check for.
     * @param economyCallType If provided "SELL", the shop's item will be checked instead of the currency item.
     * @return Whether the player has enough currency.
     */
    public boolean has(@NotNull OfflinePlayer player, @Nullable Shop shop, double amount, @Nullable EconomyCallType... economyCallType) {
        if (shop != null) {
            final int itemForItemBalance = getItemForItemBalance(player, shop, economyCallType);
            if (itemForItemBalance > -1) return (itemForItemBalance >= amount);
        }

        final EcoHook ecoHook = INSTANCE.getEconomyHandler().getEcoHook(shop != null ? shop.getCurrencyType() : getDefaultCurrency());
        if (ecoHook != null) return (ecoHook.getBalance(player.getUniqueId()) >= amount);
        else return false;
    }

    /**
     * Deposits currency into the player's balance (If a shop is provided, the shop's selected currency will be used).
     *
     * @param player The player to deposit currency to.
     * @param shop   The shop to base currency on.
     * @param amount The amount to deposit.
     * @param economyCallType If provided "SELL", the shop's item will be checked instead of the currency item.
     * @return Whether the deposit was successful.
     */
    public boolean deposit(@NotNull OfflinePlayer player, @Nullable Shop shop, double amount, @Nullable EconomyCallType... economyCallType) {
        if (shop != null && shop.getCurrencyType().equals("item-for-item")) {
            if (player.getPlayer() == null) return false;
            final ItemStack currencyItem = ((economyCallType != null && economyCallType.length > 0 && economyCallType[0] == EconomyCallType.SELL) ? shop.getShopItem()
                    : (INSTANCE.getConfig().getBoolean("shop-currency-item.force-use") ? INSTANCE.getManager().defaultCurrencyItem
                    : (shop.getTradeItem() == null ? INSTANCE.getManager().defaultCurrencyItem : shop.getTradeItem())));
            INSTANCE.getManager().giveItemStacks(player.getPlayer(), currencyItem, (int) amount);
            return true;
        }

        final EcoHook ecoHook = INSTANCE.getEconomyHandler().getEcoHook(shop != null ? shop.getCurrencyType() : getDefaultCurrency());
        if (ecoHook != null) return ecoHook.deposit(player.getUniqueId(), amount);
        else return false;
    }

    /**
     * Withdraw currency from the player's balance (If a shop is provided, the shop's selected currency will be used).
     *
     * @param player The player to withdraw currency from.
     * @param shop   The shop to base currency on.
     * @param amount The amount to withdraw.
     * @param economyCallType If provided "SELL", the shop's item will be checked instead of the currency item.
     * @return Whether the withdrawal was successful.
     */
    public boolean withdraw(@NotNull OfflinePlayer player, @Nullable Shop shop, double amount, @Nullable EconomyCallType... economyCallType) {
        if (shop != null && shop.getCurrencyType().equals("item-for-item")) {
            if (player.getPlayer() == null) return false;
            final ItemStack currencyItem = ((economyCallType != null && economyCallType.length > 0 && economyCallType[0] == EconomyCallType.SELL) ? shop.getShopItem()
                    : (INSTANCE.getConfig().getBoolean("shop-currency-item.force-use") ? INSTANCE.getManager().defaultCurrencyItem
                    : (shop.getTradeItem() == null ? INSTANCE.getManager().defaultCurrencyItem : shop.getTradeItem())));
            return INSTANCE.getManager().removeItem(player.getPlayer().getInventory(), currencyItem, (int) amount);
        }

        final EcoHook ecoHook = INSTANCE.getEconomyHandler().getEcoHook(shop != null ? shop.getCurrencyType() : getDefaultCurrency());
        if (ecoHook != null) return ecoHook.withdraw(player.getUniqueId(), amount);
        else return false;
    }

    /**
     * Obtains the currency balance of the passed player.
     *
     * @param player The player to get the balance of.
     * @param shop   The shop to base currency on (NULL will retrieve the default economy from configuration).
     * @param economyCallType If provided "SELL", the shop's item will be checked instead of the currency item.
     * @return The found player balance amount.
     */
    public double getBalance(@NotNull OfflinePlayer player, @Nullable Shop shop, @Nullable EconomyCallType... economyCallType) {
        if (shop != null) {
            final int itemForItemBalance = getItemForItemBalance(player, shop);
            if (itemForItemBalance > -1) return itemForItemBalance;
        }

        final EcoHook ecoHook = INSTANCE.getEconomyHandler().getEcoHook(shop != null ? shop.getCurrencyType() : getDefaultCurrency());
        if (ecoHook != null) return ecoHook.getBalance(player.getUniqueId());
        else return 0;
    }

    /**
     * Formats an amount as a currency string.
     *
     * @param shop            The shop to receive additional data from (OPTIONAL - Can be Null).
     * @param currencyType    The currency to base specific properties off of while formatting.
     * @param amount          The amount to format.
     * @param economyCallType If provided "SELL", the shop's item will be checked instead of the currency item.
     * @return The formatted currency string.
     */
    public String format(@Nullable Shop shop, @NotNull String currencyType, double amount, @Nullable EconomyCallType... economyCallType) {
        if (amount == -1) return INSTANCE.getLangConfig().getString("disabled");

        final EcoHook ecoHook = INSTANCE.getEconomyHandler().getEcoHook(currencyType);
        String currencySymbol = "";
        int decimalPlacement = 2;
        if (ecoHook != null) {
            currencySymbol = ((ecoHook.getSymbol() != null) ? ecoHook.getSymbol() : "");
            decimalPlacement = ecoHook.getDecimalPlacement();
        }

        final boolean useUKFormatting = INSTANCE.getConfig().getBoolean("use-uk-format");
        String formatted = String.format(("%,." + decimalPlacement + "f"), amount)
                .replace("\\s", "").replace("_", "");

        boolean isItemForItem = currencyType.equalsIgnoreCase("item-for-item");

        return (currencySymbol + (INSTANCE.getConfig().getBoolean("short-number-format")
                ? INSTANCE.getManager().format((long) Double.parseDouble(formatted.replace(",", "")), useUKFormatting)
                : (useUKFormatting ? formatted.replace(".", "_COMMA_").replace(",", "_PERIOD_")
                .replace("_PERIOD_", ".").replace("_COMMA_", ",") : formatted))
                + (isItemForItem ? " " + ((shop != null) ? shop.getTradeItemName() : INSTANCE.getManager().getItemName(INSTANCE.getManager().defaultCurrencyItem)) : ""));
    }

    /**
     * Gets the default economy currency from configuration.
     *
     * @return The default currency found in the configuration.
     */
    public String getDefaultCurrency() {
        final String defaultCurrency = INSTANCE.getConfig().getString("default-currency-type");
        if (defaultCurrency != null && !defaultCurrency.isEmpty()) return defaultCurrency;
        return (INSTANCE.getConfig().getBoolean("use-vault") ? "vault" : "item-for-item");
    }

    // getters and setters
    public Economy getVaultEconomy() {return vaultEconomy;}

    public HashMap<String, EcoHook> getEconomyRegistry() {return economyRegistry;}

}