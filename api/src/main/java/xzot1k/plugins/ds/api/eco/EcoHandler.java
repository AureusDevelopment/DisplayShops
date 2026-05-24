package xzot1k.plugins.ds.api.eco;

import net.milkbowl.vault.economy.Economy;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import xzot1k.plugins.ds.api.enums.EconomyCallType;
import xzot1k.plugins.ds.api.objects.Shop;

import java.util.HashMap;

public interface EcoHandler {

    Economy getVaultEconomy();

    HashMap<String, EcoHook> getEconomyRegistry();

    /**
     * Reloads all EcoHooks and the EconomyHandler in general.
     */
    void reset();

    EcoHook getEcoHook(@NotNull String currencyType);

    /**
     * Checks to see if the player can use the currency.
     *
     * @param player       The player to check.
     * @param currencyName The name of the currency.
     * @return Whether the player can use the currency.
     */
    boolean canUseCurrency(@NotNull Player player, @NotNull String currencyName);

    /**
     * Gets the next currency in the list starting from the shop's currently selected currency.
     *
     * @param player The player to check permissions for.
     * @param shop   The shop to obtain the selected currency.
     * @return The next available currency for selection.
     */
    String determineNextCurrencyCycle(@NotNull Player player, @NotNull Shop shop);

    /**
     * Gets the default economy currency from configuration.
     *
     * @return The default currency found in the configuration.
     */
    String getDefaultCurrency();

    /**
     * Gets the balance the player has based on a shop (determines currency item automatically).
     *
     * @param player          The player to check.
     * @param shop            The shop to obtain information from.
     * @param economyCallType If provided "SELL", the shop's item will be checked instead of the currency item.
     * @return The determined amount the player has (returns -1 if the shop uses the wrong currency or another issue occurs).
     */
    int getItemForItemBalance(@NotNull OfflinePlayer player, @NotNull Shop shop, @Nullable EconomyCallType... economyCallType);

    /**
     * Obtains the currency balance of the passed player.
     *
     * @param player          The player to get the balance of.
     * @param shop            The shop to base currency on (NULL will retrieve the default economy from configuration).
     * @param economyCallType If provided "SELL", the shop's item will be checked instead of the currency item.
     * @return The found player balance amount.
     */
    double getBalance(@NotNull OfflinePlayer player, @Nullable Shop shop, @Nullable EconomyCallType... economyCallType);

    /**
     * Withdraw currency from the player's balance (If a shop is provided, the shop's selected currency will be used).
     *
     * @param player          The player to withdraw currency from.
     * @param shop            The shop to base currency on.
     * @param amount          The amount to withdraw.
     * @param economyCallType If provided "SELL", the shop's item will be checked instead of the currency item.
     * @return Whether the withdrawal was successful.
     */
    boolean withdraw(@NotNull OfflinePlayer player, @Nullable Shop shop, double amount, @Nullable EconomyCallType... economyCallType);

    /**
     * Deposits currency into the player's balance (If a shop is provided, the shop's selected currency will be used).
     *
     * @param player          The player to deposit currency to.
     * @param shop            The shop to base currency on.
     * @param amount          The amount to deposit.
     * @param economyCallType If provided "SELL", the shop's item will be checked instead of the currency item.
     * @return Whether the deposit was successful.
     */
    boolean deposit(@NotNull OfflinePlayer player, @Nullable Shop shop, double amount, @Nullable EconomyCallType... economyCallType);

    /**
     * Check if the player has enough of a currency (If a shop is provided, the shop's selected currency will be used).
     *
     * @param player          The player to check the balance against.
     * @param shop            The shop to base currency on.
     * @param amount          The amount to check for.
     * @param economyCallType If provided "SELL", the shop's item will be checked instead of the currency item.
     * @return Whether the player has enough currency.
     */
    boolean has(@NotNull OfflinePlayer player, @Nullable Shop shop, double amount, @Nullable EconomyCallType... economyCallType);

    /**
     * Formats an amount as a currency string.
     *
     * @param shop            The shop to receive additional data from (OPTIONAL - Can be Null).
     * @param currencyType    The currency to base specific properties off of while formatting.
     * @param amount          The amount to format.
     * @param economyCallType If provided "SELL", the shop's item will be checked instead of the currency item.
     * @return The formatted currency string.
     */
    String format(@Nullable Shop shop, @NotNull String currencyType, double amount, @Nullable EconomyCallType... economyCallType);

    /**
     * @param currencyType The currency to get the value from.
     * @return The decimal placement value.
     */
    int getCurrencyDecimalPlacement(@NotNull String currencyType);

    void loadExtraData(@NotNull String currencyType, @NotNull EcoHook ecoHook);
}
