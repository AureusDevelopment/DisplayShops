/*
 * Copyright (c) 2021 XZot1K, All rights reserved.
 */

package xzot1k.plugins.ds.api.objects;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import xzot1k.plugins.ds.DisplayShops;
import xzot1k.plugins.ds.api.enums.InteractionType;

import java.util.*;

public class DDataPack implements DataPack {
    private final HashMap<String, Long> cooldownMap;
    private final HashMap<UUID, Long[]> transactionLimitMap;
    private DisplayShops pluginInstance;
    private HashMap<Integer, List<ItemStack>> pageMap;
    private LinkedHashMap<String, Boolean> appearanceDataMap;
    private int currentPage;
    private boolean inSelectionMode, transactionNotify;
    private InteractionType interactionType;
    private Object interactionValue, longTermInteractionValue;
    private Shop selectedShop;
    private Region selectedRegion;

    public DDataPack(DisplayShops pluginInstance, @Nullable String appearanceData) {
        this.setPluginInstance(pluginInstance);
        this.transactionLimitMap = new HashMap<>();
        this.cooldownMap = new HashMap<>();
        setTransactionNotify(true);
        setPageMap(new LinkedHashMap<>());
        setAppearanceDataMap(new LinkedHashMap<>());
        loadAppearanceData(appearanceData);
    }

    /**
     * Updates the transaction limit counter for the shop.
     *
     * @param shop   The shop to update.
     * @param isBuy  Whether the transaction was buy or sell.
     * @param amount The amount to set.
     */
    public void updateCurrentTransactionLimitCounter(@NotNull Shop shop, boolean isBuy, long amount) {
        final Long[] counters = getTransactionLimitMap().getOrDefault(shop.getShopId(), new Long[]{0L, 0L});

        if (isBuy) counters[0] = amount;
        else counters[1] = amount;

        getTransactionLimitMap().put(shop.getShopId(), counters);
    }

    /**
     * Gets the current transaction limit counter under the shop.
     *
     * @param shop  The shop to check for.
     * @param isBuy Whether the transaction was buy or sell.
     * @return The current counter for the shop.
     */
    public long getCurrentTransactionCounter(Shop shop, boolean isBuy) {
        return getTransactionLimitMap().getOrDefault(shop.getShopId(), new Long[]{0L, 0L})[isBuy ? 0 : 1];
    }

    /**
     * Checks if the transaction limit was reached.
     *
     * @param shop     The shop to check against.
     * @param isBuy    Whether the transaction was buy or sell.
     * @param isGlobal Whether the limited needs to be global or player specific.
     * @return Whether the limit was met.
     */
    public boolean hasMetTransactionLimit(Shop shop, boolean isBuy, boolean isGlobal) {
        final long counter = getCurrentTransactionCounter(shop, isBuy),
                buyLimit = (isGlobal ? shop.getGlobalBuyLimit() : shop.getPlayerBuyLimit()),
                sellLimit = (isGlobal ? shop.getGlobalSellLimit() : shop.getPlayerSellLimit());

        return isBuy ? (buyLimit > -1 && counter >= buyLimit) : (sellLimit > -1 && counter >= sellLimit);
    }

    /**
     * Gets the excess amount of units before the limit is met.
     *
     * @param shop     The shop to check against.
     * @param isBuy    Whether the transaction was buy or sell.
     * @param isGlobal Whether the limited needs to be global or player specific.
     * @param amount   the unit count requested
     * @return The excess amount of units available before limit is met
     */
    @Override
    public long getTransactionLimitExcess(Shop shop, boolean isBuy, boolean isGlobal, long amount) {
        final long counter = getCurrentTransactionCounter(shop, isBuy),
                buyLimit = (isGlobal ? shop.getGlobalBuyLimit() : shop.getPlayerBuyLimit()),
                sellLimit = (isGlobal ? shop.getGlobalSellLimit() : shop.getPlayerSellLimit());

        return isBuy ? (buyLimit < 0 ? amount : Math.min(counter + amount, buyLimit)) : (sellLimit < 0 ? amount : Math.min(counter + amount, sellLimit));
    }

    @Override
    public void updateCooldown(@NotNull String cooldownId) {
        this.getCooldownMap().put(cooldownId, System.currentTimeMillis());
    }

    @Override
    public boolean isOnCooldown(@NotNull Player player, @NotNull String cooldownId, int cooldown) {
        return this.getCooldown(player, cooldownId, cooldown) > 0;
    }

    @Override
    public int getCooldown(@NotNull Player player, @NotNull String cooldownId, int cooldown) {
        if (!player.hasPermission("displayshops.cdbypass")) {
            int cd = Math.max(cooldown, 0);
            return (int) ((getCooldownMap().containsKey(cooldownId) ? getCooldownMap().get(cooldownId)
                    : (long) cd) / 1000L + (long) cooldown - System.currentTimeMillis() / 1000L);
        }
        return 0;
    }

    @Override
    public boolean canUnlockAppearance(@NotNull Player player, @NotNull String appearanceId) {
        Menu menu = getPluginInstance().getMenu("appearance");
        if (menu == null) return false;
        Appearance appearance = Appearance.getAppearance(appearanceId);
        if (appearance != null) {
            return (appearance.getPermission() == null || appearance.getPermission().isEmpty()
                    || player.hasPermission(appearance.getPermission()) || player.hasPermission("displayshops.bbm.*")
                    || player.hasPermission("displayshops.appearance.*"));
        } else return false;
    }

    @Override
    public boolean hasUnlockedAppearance(@NotNull Player player, @NotNull String appearanceId) {
        Menu menu = getPluginInstance().getMenu("appearance");
        if (menu != null && menu.getConfiguration().getBoolean("permission-unlock-mode"))
            return canUnlockAppearance(player, appearanceId);
        return getAppearanceDataMap().getOrDefault(appearanceId, false);
    }

    @Override
    public void updateAppearance(@NotNull String appearanceId, boolean unlocked) {
        getAppearanceDataMap().put(appearanceId, unlocked);
    }

    @Override
    public void updateUnlocks(boolean unlockAll) {
        final Menu menu = getPluginInstance().getMenu("appearance");
        if (menu == null) return;

        ConfigurationSection appearanceSection = menu.getConfiguration().getConfigurationSection("appearances");
        if (appearanceSection != null) {
            for (String appearanceId : appearanceSection.getKeys(false)) {
                ConfigurationSection appearanceSettings = menu.getConfiguration().getConfigurationSection(appearanceId);
                if (appearanceSettings == null) continue;
                getAppearanceDataMap().putIfAbsent(appearanceId, unlockAll);
            }
        }
    }

    @Override
    public String getAppearanceData() {
        final StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, Boolean> entry : getAppearanceDataMap().entrySet()) {
            if (sb.length() > 0)
                sb.append(",");
            sb.append(entry.getKey()).append("/").append(entry.getValue() ? 1 : 0);
        }
        return sb.toString();
    }

    private void loadAppearanceDataLine(@NotNull String appearanceDataLine) {
        if (appearanceDataLine.contains("/")) {
            String[] data = appearanceDataLine.split("/");
            String appearanceId = data[0];
            if (appearanceId.contains(":")) appearanceId = Appearance.findAppearance(appearanceId);
            if (appearanceId == null || appearanceId.isEmpty()) return;

            Appearance appearance = Appearance.getAppearance(appearanceId);
            if (appearance != null) getAppearanceDataMap().put(appearanceId, data[1].equals("1"));
        } else {
            if (appearanceDataLine.contains(":")) appearanceDataLine = Appearance.findAppearance(appearanceDataLine);
            if (appearanceDataLine == null || appearanceDataLine.isEmpty()) return;

            Appearance appearance = Appearance.getAppearance(appearanceDataLine);
            if (appearance != null) {
                Menu menu = getPluginInstance().getMenu("appearance");
                if (menu != null) {
                    getAppearanceDataMap().put(appearanceDataLine, (!appearance.getId().equalsIgnoreCase(menu.getConfiguration().getString("default-appearance"))));
                    return;
                }

                getAppearanceDataMap().put(appearanceDataLine, false);
            }
        }
    }

    @Override
    public void loadAppearanceData(@Nullable String appearanceData) {
        Menu menu = getPluginInstance().getMenu("appearance");
        if (menu != null && menu.getConfiguration().getBoolean("permission-unlock-mode")) return;

        if (appearanceData == null || appearanceData.isEmpty()) {
            updateUnlocks(false);
            return;
        }

        if (!appearanceData.contains(",")) {
            loadAppearanceDataLine(appearanceData);
            return;
        }

        String[] savedAppearances = appearanceData.split(",");
        for (int i = -1; ++i < savedAppearances.length; )
            loadAppearanceDataLine(savedAppearances[i]);
    }

    @Override
    public String cooldownsToString() {
        StringBuilder cdLine = new StringBuilder();
        int counter = this.getCooldownMap().size();
        for (Map.Entry<String, Long> cdEntry : this.getCooldownMap().entrySet()) {
            cdLine.append(cdEntry.getKey()).append(":").append(cdEntry.getValue());
            if (--counter <= 0) continue;
            cdLine.append(",");
        }
        return cdLine.toString();
    }

    @Override
    public String transactionLimitsToString() {
        StringBuilder cdLine = new StringBuilder();
        int counter = this.getTransactionLimitMap().size();
        for (Map.Entry<UUID, Long[]> cdEntry : this.getTransactionLimitMap().entrySet()) {
            cdLine.append(cdEntry.getKey()).append(":").append(cdEntry.getValue()[0])
                    .append(";").append(cdEntry.getValue()[1]);
            if (--counter <= 0) continue;
            cdLine.append(",");
        }
        return cdLine.toString();
    }

    @Override
    public void resetEditData() {
        if (getSelectedShop() != null) getSelectedShop().setCurrentEditor(null);
        setSelectedShop(null);
        setInteractionType(null);
        //setLongTermInteractionValue(null);
    }

    @Override
    public LinkedHashMap<String, Boolean> getAppearanceDataMap() {
        return this.appearanceDataMap;
    }

    @Override
    public void setAppearanceDataMap(LinkedHashMap<String, Boolean> appearanceDataMap) {
        this.appearanceDataMap = appearanceDataMap;
    }

    private DisplayShops getPluginInstance() {
        return this.pluginInstance;
    }

    private void setPluginInstance(DisplayShops pluginInstance) {
        this.pluginInstance = pluginInstance;
    }

    @Override
    public Shop getSelectedShop() {
        return this.selectedShop;
    }

    @Override
    public void setSelectedShop(Shop selectedShop) {
        this.selectedShop = selectedShop;
    }

    @Override
    public Region getSelectedRegion() {
        return this.selectedRegion;
    }

    @Override
    public void setSelectedRegion(Region selectedRegion) {
        this.selectedRegion = selectedRegion;
    }

    @Override
    public boolean isInSelectionMode() {
        return this.inSelectionMode;
    }

    @Override
    public void setInSelectionMode(boolean inSelectionMode) {
        this.inSelectionMode = inSelectionMode;
    }

    @Override
    public HashMap<String, Long> getCooldownMap() {
        return this.cooldownMap;
    }

    private HashMap<UUID, Long[]> getTransactionLimitMap() {
        return transactionLimitMap;
    }

    /**
     * Gets the current page the player is on from the page map.
     *
     * @return The current page.
     */
    @Override
    public int getCurrentPage() {
        return currentPage;
    }

    /**
     * Sets the current page the player is on from the page map.
     */
    @Override
    public void setCurrentPage(int currentPage) {
        this.currentPage = currentPage;
    }

    /**
     * Gets the page map for the player if it exists.
     *
     * @return The map itself.
     */
    @Override
    public HashMap<Integer, List<ItemStack>> getPageMap() {
        return pageMap;
    }

    /**
     * Sets the page map for the player.
     *
     * @param pageMap The map to set it as.
     */
    @Override
    public void setPageMap(@Nullable HashMap<Integer, List<ItemStack>> pageMap) {
        this.pageMap = pageMap;
    }

    /**
     * @return Whether the player has a next page in their current menu interaction.
     */
    @Override
    public boolean hasNextPage() {
        return getPageMap().containsKey(getCurrentPage() + 1);
    }

    /**
     * @return Whether the player has a previous page in their current menu interaction.
     */
    @Override
    public boolean hasPreviousPage() {
        return getPageMap().containsKey(getCurrentPage() - 1);
    }

    @Override
    public boolean isTransactionNotify() {
        return transactionNotify;
    }

    @Override
    public void setTransactionNotify(boolean transactionNotify) {
        this.transactionNotify = transactionNotify;
    }

    /**
     * @return The current interaction type the player is performing.
     */
    @Override
    public InteractionType getInteractionType() {
        return interactionType;
    }

    /**
     * Sets the interaction type the player is currently performing.
     *
     * @param interactionType The new interaction type to update with.
     */
    @Override
    public void setInteractionType(@Nullable InteractionType interactionType) {
        this.interactionType = interactionType;
    }

    /**
     * Gets the value attached to the current interaction performed by the player.
     *
     * @return The current value attached to the interaction.
     */
    @Override
    public Object getInteractionValue() {
        return interactionValue;
    }

    /**
     * Sets the value attached to the current interaction performed by the player.
     *
     * @param interactionValue The new value to set as the attached interaction value.
     */
    @Override
    public void setInteractionValue(@Nullable Object interactionValue) {
        this.interactionValue = interactionValue;
    }

    /**
     * Gets the value attached to the current interaction performed by the player, intended for retrieval way later in the process.
     *
     * @return The current value attached to the interaction, intended for retrieval way later in the process.
     */
    @Override
    public Object getLongTermInteractionValue() {
        return longTermInteractionValue;
    }

    /**
     * Sets the value attached to the current interaction performed by the player, intended for retrieval way later in the process.
     *
     * @param longTermInteractionValue The new value to set as the attached interaction value, intended for retrieval way later in the process.
     */
    @Override
    public void setLongTermInteractionValue(@Nullable Object longTermInteractionValue) {
        this.longTermInteractionValue = longTermInteractionValue;
    }

}