/*
 * Copyright (c) 2022 XZot1K, All rights reserved.
 */

package xzot1k.plugins.ds.api;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionType;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import xzot1k.plugins.ds.api.objects.DataPack;
import xzot1k.plugins.ds.api.objects.MarketRegion;
import xzot1k.plugins.ds.api.objects.Shop;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

public interface Manager {

    /**
     * Loads the passed player's data pack. If not found, a new data pack module is created. (CAN RETURN NULL)
     *
     * @param player The player to load the data pack for.
     */
    DataPack loadDataPack(@NotNull Player player);

    /**
     * Attempts to remove any player data from the database when the player's last seen exceeds one year.
     */
    void cleanUpDataPacks();

    /**
     * Ray traces from the provided vectors to obtain a shop from the locations it passes through.
     *
     * @param worldName       The world to ray trace in.
     * @param eyeVector       The eye location of the entity or just the general origin location.
     * @param directionVector The direction at which the vector should go.
     * @param distance        The distance at which the ray should travel.
     * @return The found shop, returns NULL if NOT found.
     */
    Shop getShopRayTraced(@NotNull String worldName, @NotNull Vector eyeVector, @NotNull Vector directionVector, double distance);

    /**
     * Sends a color translated message to the players as either a normal chat message or action bar message.
     *
     * @param player       The player to send the message to.
     * @param message      The message to send (color codes accepted, if the message contains {bar} at the front it will be sent to the action bar).
     * @param placeholders The placeholders in the format <placeholder>:<replacement>.
     */
    void sendMessage(@NotNull Player player, @NotNull String message, @Nullable String... placeholders);

    /**
     * @param text         The text to apply replacements to.
     * @param placeholders The placeholders in the format <placeholder>:<replacement>.
     * @return The text with applied replacements.
     */
    String applyPlaceholders(@Nullable String text, @Nullable String... placeholders);

    /**
     * @param player       The player to associate PlaceholderAPI replacements with.
     * @param text         The text to apply replacements to.
     * @param placeholders The placeholders in the format <placeholder>:<replacement>.
     * @return The text with applied replacements.
     */
    String applyPlaceholders(@NotNull Player player, @Nullable String text, @Nullable String... placeholders);

    /**
     * @param text       The text to apply placeholders to.
     * @param shop       The shop to obtain placeholder values from.
     * @param unitValues The unit count and unit item max stack values.
     * @return The text with placeholders applied.
     */
    String applyShopBasedPlaceholders(@Nullable String text, @NotNull Shop shop, int... unitValues);

    /**
     * Obtains available space for a defined item.
     *
     * @param player    The player who's inventory needs to be checked.
     * @param itemStack The itemstack to check for.
     * @return The total available item amount space.
     */
    int getInventorySpaceForItem(@NotNull Player player, @NotNull ItemStack itemStack);

    /**
     * Retrieve a market region, if the passed location is inside it.
     *
     * @param location The location to check.
     * @return The MarketRegion object found.
     */
    MarketRegion getMarketRegion(@NotNull Location location);

    /**
     * Retrieve a market region by ID, if it exists.
     *
     * @param marketId The id to check for.
     * @return The MarketRegion object.
     */
    MarketRegion getMarketRegion(@NotNull String marketId);

    /**
     * Checks if a market region already exists with the passed id.
     *
     * @param marketId The id to check.
     * @return The result as a boolean value.
     */
    boolean doesMarketRegionExist(@NotNull String marketId);

    /**
     * Gets if a location is too close to a shop based on the distance value in the configuration.
     *
     * @param location The location to check around.
     * @return The result as a boolean.
     */
    boolean isTooClose(@NotNull Location location);

    /**
     * Finds the value a specific placeholder was replaced with given the original format, placeholder, and the ItemStack.
     *
     * @param itemStack   The ItemStack containing the formatted placeholder.
     * @param textFormat  The format with the non-replaced placeholder.
     * @param placeHolder The placeholder to get the value from.
     * @return The value found in the placeholders place (can return NULL).
     */
    String getValueFromPlaceholder(@NotNull ItemStack itemStack, @NotNull Object textFormat, @NotNull String placeHolder);

    /**
     * Gets the name of the item. If the item name is not custom, then either the original or translated name will be used.
     *
     * @param itemStack The item to get the name of.
     * @return The item name.
     */
    String getItemName(@NotNull ItemStack itemStack);

    /**
     * Obtains any translation created for the passed material found in the "lang.yml".
     *
     * @param material The material to obtain the translation for.
     * @param data     The durability/data value used in older versions (Optional, defaults to 0).
     * @return The translated version.
     */
    String getTranslatedName(Material material, int... data);

    /**
     * Obtains any translation created for the passed enchantment found in the "lang.yml".
     *
     * @param enchantment The enchantment to obtain the translation for.
     * @return The translated version.
     */
    String getTranslatedName(@NotNull Enchantment enchantment);

    /**
     * Obtains any translation created for the passed potion type found in the "translation.yml".
     *
     * @param potionType The potion type to obtain the translation for.
     * @return The translated version.
     */
    String getTranslatedName(@NotNull PotionType potionType);

    /**
     * Gets the names of the enchantments and formats them into a neat format.
     *
     * @param itemStack The item to get the enchants from.
     * @return The new formatted line.
     */
    String getEnchantmentLine(@NotNull ItemStack itemStack);

    /**
     * Gets the names of the potion effects and formats them into a neat format.
     *
     * @param itemStack The item to get the potion effects from.
     * @return The new formatted line.
     */
    String getPotionLine(@NotNull ItemStack itemStack);

    /**
     * Gets roman numeral value from 1 - 10.
     *
     * @param number The number between 1 and 10.
     * @return The roman numeral.
     */
    String getRomanNumeral(int number);

    /**
     * Colors the text passed.
     *
     * @param message The message to translate.
     * @return The colored text.
     */
    String color(@NotNull String message);

    /**
     * Formats a given number based on the locale.
     *
     * @param value     The numerical value.
     * @param isDecimal Whether the number contains a decimal.
     * @return The formatted number as a string.
     */
    String formatNumber(double value, boolean isDecimal);

    /**
     * Gets the max buy or sell price of a selected material.
     *
     * @param itemStack The item stack to check for.
     * @param isBuy     Whether it is buy or sell.
     * @return The found maximum.
     */
    double getMaterialMaxPrice(@NotNull ItemStack itemStack, boolean isBuy);

    /**
     * Gets the min buy or sell price of a selected material.
     *
     * @param itemStack The item stack to check for.
     * @param isBuy     Whether it is buy or sell.
     * @return The found minimum.
     */
    double getMaterialMinPrice(@NotNull ItemStack itemStack, boolean isBuy);

    /**
     * Checks if a shop already exists with the passed id.
     *
     * @param shopId The id to check for.
     * @return Whether the id exists in true or false format.
     */
    boolean doesShopIdExist(@NotNull UUID shopId);

    /**
     * Generates a new ID suited for a new display shop.
     *
     * @return a long value for the ID.
     */
    UUID generateNewId();

    /**
     * Attempts to get a shop object from the shop map by its ID.
     *
     * @param shopId The ID to get the shop from
     * @return the shop object.
     */
    Shop getShopById(@NotNull UUID shopId);

    /**
     * Get a shop from the passed chest if possible.
     *
     * @param location Chest location.
     * @return The shop if found.
     */
    Shop getShop(@NotNull Location location);

    /**
     * Creates a shop at the passed block owned by the passed player.
     *
     * @param player              The player to use as the owner.
     * @param block               The block to create the shop at.
     * @param shopItemAmount      The amount used for the shop item.
     * @param doCreationEffects   Whether to play the effects and sounds.
     * @param sendCreationMessage Whether to send the creation message.
     * @return The shop object.
     */
    Shop createShop(@NotNull Player player, @NotNull Block block, int shopItemAmount, boolean doCreationEffects, boolean sendCreationMessage);

    /**
     * Attempts to load all available shops from the database.
     *
     * @param isAsync Whether the task is ran on the main thread.
     * @param cleanUp Deletes all shops that have invalid data (Example: No longer existing worlds).
     */
    void loadShops(boolean isAsync, boolean cleanUp);

    /**
     * Checks to see if the material is blocked or not.
     *
     * @param material The material to check for.
     * @return The result.
     */
    boolean isBlockedMaterial(@NotNull Material material);

    /**
     * Saves all market regions to the database.
     */
    void saveMarketRegions();

    /**
     * Loads all market regions into the memory.
     *
     * @param cleanUp Deletes all market regions that have invalid data (Example: No longer existing worlds).
     */
    void loadMarketRegions(boolean cleanUp);

    /**
     * Obtains the default base material without checking attached durability.
     *
     * @return The material type.
     */
    Material getBaseBlockType();

    /**
     * Returns a list of all shops owned by the player.
     *
     * @param player The player to check for.
     * @return The list of shops the player owns.
     */
    List<Shop> getPlayerShops(@NotNull Player player);

    /**
     * Gets if the player has reached the maximum amount of warps they can have.
     *
     * @param player The player to check.
     * @return Whether the player has exceeded their limit or not.
     */
    boolean exceededShopLimit(@NotNull Player player);

    /**
     * Gets the shop limit of the passed player.
     *
     * @param player The player to check for.
     * @return The limit they have on shops.
     */
    int getShopLimit(@NotNull Player player);

    /**
     * Returns a list of all regions rented by the player.
     *
     * @param player The player to check for.
     * @return The list of rented regions.
     */
    List<MarketRegion> getMarketRegions(@NotNull Player player);

    /**
     * Gets if the player has reached the maximum amount of rented regions they can have.
     *
     * @param player The player to check.
     * @return Whether the player has exceeded their limit or not.
     */
    boolean exceededMarketRegionLimit(@NotNull Player player);

    /**
     * Gets the rented region limit of the passed player.
     *
     * @param player The player to check for.
     * @return The limit they have on rented regions.
     */
    int getMarketRegionLimit(@NotNull Player player);

    /**
     * Gets the player specific shop promotion item modifier.
     *
     * @param player The player to check.
     * @return The modifier for multiplication.
     */
    double getPromotionPriceModifier(Player player);

    /**
     * Gives the passed item stack to the player the passed amount of times in the form of stacks.
     *
     * @param player           The player to give the item to.
     * @param itemStackToClone The item stack to make the stacks from.
     * @param amount           The amount of the items to give to the player (NOT stacks).
     */
    void giveItemStacks(@NotNull Player player, @NotNull ItemStack itemStackToClone, int amount);

    /**
     * Remove a certain amount of a certain similar item.
     *
     * @param inventory The inventory to remove from.
     * @param itemStack The item to check for.
     * @param amount    The amount to remove.
     * @return Whether an item was removed or not.
     */
    boolean removeItem(@NotNull Inventory inventory, @NotNull ItemStack itemStack, int amount);

    /**
     * @param itemStack itemstack to check NBT on
     * @return Whether the itemstack contains blocked NBT data
     */
    boolean hasBlockedNBT(@NotNull ItemStack itemStack);

    /**
     * Get amount of similar items.
     *
     * @param inventory The inventory to check.
     * @param itemStack The item to check for.
     * @return The total amount.
     */
    int getItemAmount(@NotNull Inventory inventory, @NotNull ItemStack itemStack);

    /**
     * Checks if two items are identical using string format.
     *
     * @param itemOne The first item.
     * @param itemTwo The second item.
     * @return Whether the items are identical.
     */
    boolean isSimilar(@NotNull ItemStack itemOne, @NotNull ItemStack itemTwo);

    /**
     * See if a string is NOT a numerical value.
     *
     * @param string The string to check.
     * @return Whether it is numerical or not.
     */
    boolean isNotNumeric(@NotNull String string);

    /**
     * See if a string is NOT a numerical value.
     *
     * @param string The string to check.
     * @return Whether it is numerical or not.
     */
    boolean isNotRawNumeric(@NotNull String string);

    /**
     * Wraps a string into multiple lines based on a word count.
     *
     * @param text The long string to wrap.
     * @return wraps the string to multiple lines
     */
    List<String> wrapString(@NotNull String text);

    /**
     * Builds and returns the shop creation item.
     *
     * @param player The player to check for what base-block to use.
     * @param amount The amount of the item stack.
     * @return The shop creation physical item stack.
     */
    ItemStack buildShopCreationItem(@Nullable Player player, int amount);

    /**
     * Builds and returns the shop currency item.
     *
     * @param amount The amount of the item stack.
     * @return The shop currency physical item stack.
     */
    ItemStack buildShopCurrencyItem(int amount);

    /**
     * Checks if the passed world is in the world blacklist.
     *
     * @param world The world to check for.
     * @return Whether the world is blocked or not.
     */
    boolean isBlockedWorld(@NotNull World world);

    // getters & setters
    ConcurrentHashMap<String, Shop> getShopMap();

    List<MarketRegion> getMarketRegions();

    ConcurrentHashMap<UUID, DataPack> getDataPackMap();

    Pattern getUUIDPattern();
}