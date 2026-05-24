package xzot1k.plugins.ds.core.gui;


import net.md_5.bungee.api.ChatColor;
import org.apache.commons.lang3.StringUtils;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import xzot1k.plugins.ds.DisplayShops;
import xzot1k.plugins.ds.api.eco.EcoHook;
import xzot1k.plugins.ds.api.enums.InteractionType;
import xzot1k.plugins.ds.api.enums.ShopActionType;
import xzot1k.plugins.ds.api.objects.*;
import xzot1k.plugins.ds.core.utils.nms_utils.Ref;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;

public class BackendMenu extends YamlConfiguration implements Menu {

    private final DisplayShops INSTANCE;
    private final String menuName, fileName;
    private final File file;
    private final HashMap<Integer, String> buttonLocationMap;
    private String title;
    private int size;

    /**
     * Creates a configuration file in the YAML format.
     *
     * @param file the configuration file.
     */
    public BackendMenu(@NotNull File file) {
        this.INSTANCE = DisplayShops.getPluginInstance();
        this.file = file;
        this.fileName = file.getName();
        this.menuName = fileName.toLowerCase().replace(".yml", "");

        this.buttonLocationMap = new HashMap<>();

        try {
            load(file);
            fixup();
            final String title = getString("title");
            setTitle(INSTANCE.getManager().color((title != null && !title.isEmpty()) ? title : ""));
            setSize(getInt("size"));
        } catch (IOException | InvalidConfigurationException e) {
            INSTANCE.getServer().getLogger().warning(e.getMessage());
        }
    }

    @SuppressWarnings("unchecked")
    private void fixup() {
        // This fixes the edit.yml for the new values added
        if (getMenuName().equals("edit")) {
            ConfigurationSection buttonSection = getConfiguration().getConfigurationSection("buttons");
            if (buttonSection != null) {

                // This will rename the unit-amount item to shop-item-amount
                if (buttonSection.contains("unit-amount")) {
                    ConfigurationSection oldSection = buttonSection.getConfigurationSection("unit-amount");
                    if (oldSection != null) {
                        ConfigurationSection newSection = buttonSection.createSection("shop-item-amount");
                        for (String key : oldSection.getKeys(false)) {
                            final Object value = oldSection.get(key);
                            newSection.set(key, value);
                        }

                        buttonSection.set("unit-amount", null);
                        save();
                    }
                }
            }

            if (getConfiguration().contains("item-change") && !getConfiguration().contains("sale-item-change")
                    && !getConfiguration().contains("trade-item-change")) {
                addSectionFromJarConfig("menus/edit.yml", "sale-item-change");
                addSectionFromJarConfig("menus/edit.yml", "trade-item-change");
            }
        }

        if (getMenuName().equals("transaction")) {
            ConfigurationSection buttonSection = getConfiguration().getConfigurationSection("buttons");
            if (buttonSection != null) {
                if (buttonSection.contains("unit-increase-more")) {
                    ConfigurationSection oldSection = buttonSection.getConfigurationSection("unit-increase-more");
                    if (oldSection != null) {
                        ConfigurationSection newSection = buttonSection.createSection("unit-increase-16");
                        for (String key : oldSection.getKeys(false)) {
                            if (key.contains("amount")) {
                                newSection.set(key, "16");
                                continue;
                            }

                            final Object value = oldSection.get(key);
                            if (value instanceof String) {
                                newSection.set(key, ((String) value).replace("{unit-increment}", "16"));
                                continue;
                            } else if (value instanceof List) {
                                List<String> list = ((List<String>) value);
                                for (int i = -1; ++i < list.size(); ) {
                                    String line = list.get(i);
                                    list.set(i, line.replace("{unit-increment}", "16"));
                                }
                            }
                            newSection.set(key, value);
                        }

                        buttonSection.set("unit-increase-more", null);
                        save();
                    }
                }

                if (buttonSection.contains("unit-decrease-more")) {
                    ConfigurationSection oldSection = buttonSection.getConfigurationSection("unit-decrease-more");
                    if (oldSection != null) {
                        ConfigurationSection newSection = buttonSection.createSection("unit-decrease-16");
                        for (String key : oldSection.getKeys(false)) {
                            if (key.contains("amount")) {
                                newSection.set(key, "16");
                                continue;
                            }

                            final Object value = oldSection.get(key);
                            if (value instanceof String) {
                                newSection.set(key, ((String) value).replace("{unit-increment}", "16"));
                                continue;
                            } else if (value instanceof List) {
                                List<String> list = ((List<String>) value);
                                for (int i = -1; ++i < list.size(); ) {
                                    String line = list.get(i);
                                    list.set(i, line.replace("{unit-increment}", "16"));
                                }
                            }
                            newSection.set(key, value);
                        }

                        buttonSection.set("unit-decrease-more", null);
                        save();
                    }
                }
            }
        }

        if (getMenuName().equals("appearance")) {
            ConfigurationSection baseSection = getConfigurationSection("");
            if (baseSection != null) {
                if (!baseSection.contains("permission-unlock-mode")) baseSection.set("permission-unlock-mode", false);

                if (!baseSection.contains("default-appearance")) {
                    String defaultAppearance = "Default";

                    ConfigurationSection appearancesSection = baseSection.getConfigurationSection("appearances");
                    if (appearancesSection != null && !appearancesSection.contains(defaultAppearance))
                        defaultAppearance = baseSection.getKeys(false).stream().findFirst().orElse("Default");

                    baseSection.set("default-appearance", defaultAppearance);
                }
            }
        }

        for (Map.Entry<String, Object> entry : getValues(true).entrySet()) {
            final String key = entry.getKey().toLowerCase();
            if (key.endsWith("material")) {

                final String mat = String.valueOf(entry.getValue());
                if (!mat.isEmpty() && !isValidMaterial(mat)) {

                    if (mat.toUpperCase().startsWith("HEAD")) continue; // skip custom heads

                    if (Ref.isOlderThan(13)&&!Ref.isMajorNewerThan(26)) {
                        // wool
                        if (mat.equalsIgnoreCase("LIME_WOOL")) getConfiguration().set(entry.getKey(), "WOOL:5");
                        else if (mat.equalsIgnoreCase("RED_WOOL")) getConfiguration().set(entry.getKey(), "WOOL:14");
                        else if (mat.equalsIgnoreCase("ORANGE_WOOL")) getConfiguration().set(entry.getKey(), "WOOL:1");
                        else if (mat.toUpperCase().contains("_WOOL")) getConfiguration().set(entry.getKey(), "WOOL");
                            // dye
                        else if (mat.equalsIgnoreCase("LIME_DYE"))
                            getConfiguration().set(entry.getKey(), "INK_SACK:10");
                        else if (mat.equalsIgnoreCase("RED_DYE")) getConfiguration().set(entry.getKey(), "INK_SACK:1");
                        else if (mat.equalsIgnoreCase("ORANGE_DYE"))
                            getConfiguration().set(entry.getKey(), "INK_SACK:14");
                        else if (mat.toUpperCase().contains("_DYE")) getConfiguration().set(entry.getKey(), "INK_SACK");
                            // minecart
                        else if (mat.equalsIgnoreCase("CHEST_MINECART"))
                            getConfiguration().set(entry.getKey(), "STORAGE_MINECART");
                            // sign
                        else if (mat.toUpperCase().contains("_SIGN")) getConfiguration().set(entry.getKey(), "SIGN");
                            // end portal frame
                        else if (mat.equalsIgnoreCase("END_PORTAL_FRAME"))
                            getConfiguration().set(entry.getKey(), "ENDER_PORTAL_FRAME");
                            // player head
                        else if (mat.equalsIgnoreCase("PLAYER_HEAD"))
                            getConfiguration().set(entry.getKey(), "SKULL_ITEM");
                            // stained-glass
                        else if (mat.equalsIgnoreCase("BLACK_STAINED_GLASS_PANE"))
                            getConfiguration().set(entry.getKey(), "STAINED_GLASS_PANE:15");
                        else if (mat.toUpperCase().contains("STAINED_GLASS_PANE"))
                            getConfiguration().set(entry.getKey(), "STAINED_GLASS_PANE:15");

                    } else if (Ref.isNewerThan(12)|| Ref.isMajorNewerThan(26)) {

                        // wool
                        if (mat.equalsIgnoreCase("WOOL:5")) getConfiguration().set(entry.getKey(), "LIME_WOOL");
                        else if (mat.equalsIgnoreCase("WOOL:14")) getConfiguration().set(entry.getKey(), "RED_WOOL");
                        else if (mat.equalsIgnoreCase("WOOL:1")) getConfiguration().set(entry.getKey(), "ORANGE_WOOL");
                        else if (mat.toUpperCase().contains("WOOL"))
                            getConfiguration().set(entry.getKey(), "WHITE_WOOL");
                            // dye
                        else if (mat.equalsIgnoreCase("INK_SACK:10"))
                            getConfiguration().set(entry.getKey(), "LIME_DYE");
                        else if (mat.equalsIgnoreCase("INK_SACK:1")) getConfiguration().set(entry.getKey(), "RED_DYE");
                        else if (mat.equalsIgnoreCase("INK_SACK:14"))
                            getConfiguration().set(entry.getKey(), "ORANGE_DYE");
                        else if (mat.toUpperCase().contains("INK_SACK")) getConfiguration().set(entry.getKey(), "_DYE");
                            // minecart
                        else if (mat.equalsIgnoreCase("STORAGE_MINECART"))
                            getConfiguration().set(entry.getKey(), "CHEST_MINECART");
                            // sign
                        else if ((Ref.isNewerThan(13)|| Ref.isMajorNewerThan(26)) && mat.equalsIgnoreCase("SIGN"))
                            getConfiguration().set(entry.getKey(), "OAK_SIGN");
                            // end portal frame
                        else if (mat.equalsIgnoreCase("ENDER_PORTAL_FRAME"))
                            getConfiguration().set(entry.getKey(), "END_PORTAL_FRAME");
                            // player head
                        else if (mat.equalsIgnoreCase("SKULL_ITEM"))
                            getConfiguration().set(entry.getKey(), "PLAYER_HEAD");
                            // stained-glass
                        else if (mat.equalsIgnoreCase("STAINED_GLASS_PANE:15"))
                            getConfiguration().set(entry.getKey(), "BLACK_STAINED_GLASS_PANE");
                        else if (mat.toUpperCase().contains("STAINED_GLASS_PANE"))
                            getConfiguration().set(entry.getKey(), "BLACK_STAINED_GLASS_PANE");
                    }
                }
            }
        }

        try {
            getConfiguration().save(getFile());
            reload();
        } catch (IOException ignored) {
        }
    }

    public void addSectionFromJarConfig(@NotNull String configPathInJar, @NotNull String sectionName) {
        ConfigurationSection newSection = getConfiguration().createSection(sectionName);
        final FileConfiguration jarConfigCopy = INSTANCE.getConfigFromJar(configPathInJar);
        if (jarConfigCopy != null) {
            ConfigurationSection section = jarConfigCopy.getConfigurationSection(sectionName);
            if (section != null) {
                for (String key : section.getKeys(true)) {
                    final Object value = section.get(key);
                    newSection.set(key, value);
                }
            }

            save();
        }
    }

    private boolean isValidMaterial(@Nullable String material) {
        if (material == null || material.isEmpty()) return false;
        Material mat = Material.matchMaterial(material);
        return (mat != null);
    }

    private String predictCorrectValue(@NotNull Class<? extends Enum<?>> enumeration, @NotNull String value) {
        String currentWinner = ((enumeration.getEnumConstants().length > 0) ? enumeration.getEnumConstants()[0].name() : "");
        int currentDistance = 0;

        for (Enum<?> operation : enumeration.getEnumConstants()) {
            final int newDistance = StringUtils.getLevenshteinDistance(operation.name(), value);
            if (newDistance > currentDistance) {
                currentWinner = operation.name();
                currentDistance = newDistance;
            }
        }

        return currentWinner;
    }

    private String stitchSearchText(@Nullable String... searchText) {
        String stitchedSearchText = ((searchText != null && searchText.length == 1) ? searchText[0] : null);

        if (searchText != null && searchText.length > 1) {
            final StringBuilder sb = new StringBuilder();

            for (int i = -1; ++i < searchText.length; ) {
                final String arg = searchText[i];

                if (sb.length() > 0) sb.append(" ");
                sb.append(arg);
            }

            stitchedSearchText = sb.toString();
        }

        return ((stitchedSearchText != null && !stitchedSearchText.isEmpty()) ? stitchedSearchText : null);
    }

    @Override
    public boolean matches(@Nullable String title) {
        if ((title == null || title.isEmpty()) && (getTitle() == null || getTitle().isEmpty())) {return true;}
        if (title == null || title.isEmpty() || getTitle() == null || getTitle().isEmpty()) {return false;}
        return ChatColor.stripColor(getTitle()).equals(ChatColor.stripColor(title));
    }

    /**
     * Gets the location of the button being looked for.
     *
     * @param slot The slot to look for.
     * @return The name of the button found.
     */
    @Override
    public String getButtonName(int slot) {
        return getButtonLocationMap().getOrDefault(slot, null);
    }

    /**
     * Builds and opens the menu using a defined search text alongside the player.
     *
     * @param player     The player to base properties around.
     * @param searchText The searched text, if applicable.
     */
    public void build(@NotNull Player player, @Nullable String... searchText) {
        final DataPack dataPack = INSTANCE.getManager().getDataPack(player);
        final Shop shop = dataPack.getSelectedShop();

        if (dataPack.getPageMap() != null) dataPack.getPageMap().clear();

        // check and update appearance, if incorrectly set
        if (shop != null && getMenuName().contains("appearance")) {
            if (shop.getBaseLocation() != null) {
                final Location baseLocation = shop.getBaseLocation().asBukkitLocation();
                if (shop.getAppearanceId() != null && !shop.getAppearanceId().isEmpty()) {
                    Appearance appearance = Appearance.getAppearance(shop.getAppearanceId());
                    if (!appearance.getMaterial().contains(baseLocation.getBlock().getType().name())) {
                        String newAppearanceId = Appearance.findAppearance(baseLocation.getBlock().getType().name());
                        if (newAppearanceId != null) {
                            Appearance newAppearance = Appearance.getAppearance(newAppearanceId);
                            if (newAppearance != null) newAppearance.apply(shop, player);
                            else appearance.apply(shop, player);
                        } else appearance.apply(shop, player);
                    }
                }
            }
        }

        final Inventory inventory = ((getSize() <= 5) ? INSTANCE.getServer().createInventory(null, InventoryType.HOPPER, getTitle())
                : INSTANCE.getServer().createInventory(null, getSize(), getTitle()));
        final String stitchedSearchText = stitchSearchText(searchText);

        CompletableFuture.runAsync(() -> {
            ArrayList<Integer> emptySlots = new ArrayList<>(getIntegerList("empty-slots"));
            buildButtons(player, inventory, emptySlots);

            if (getMenuName().contains("amount-selector") && shop != null) {
                double finalAmount = 0;
                if (dataPack.getInteractionValue() != null) {
                    finalAmount = (double) dataPack.getInteractionValue();
                    dataPack.setInteractionValue(null);
                }

                final int amountSlot = getInt("buttons.amount.slot");
                final ItemStack amountItem = inventory.getItem(amountSlot);
                if (amountItem != null && amountItem.getItemMeta() != null) {
                    final ItemMeta itemMeta = amountItem.getItemMeta();

                    if (finalAmount == 0) {
                        if (dataPack.getInteractionType() == InteractionType.AMOUNT_BUY_PRICE) {
                            finalAmount = shop.getBuyPrice(false);
                        } else if (dataPack.getInteractionType() == InteractionType.AMOUNT_SELL_PRICE) {
                            finalAmount = shop.getSellPrice(false);
                        } else if (dataPack.getInteractionType() == InteractionType.AMOUNT_STOCK) {
                            finalAmount = shop.getStock();
                        } else if (dataPack.getInteractionType() == InteractionType.AMOUNT_BALANCE) {
                            finalAmount = shop.getStoredBalance();
                        } else if (dataPack.getInteractionType() == InteractionType.SHOP_ITEM_AMOUNT) {
                            finalAmount = shop.getShopItemAmount();
                        } else if (dataPack.getInteractionType() == InteractionType.AMOUNT_PLAYER_BUY_LIMIT) {
                            finalAmount = shop.getPlayerBuyLimit();
                        } else if (dataPack.getInteractionType() == InteractionType.AMOUNT_PLAYER_SELL_LIMIT) {
                            finalAmount = shop.getPlayerSellLimit();
                        } else if (dataPack.getInteractionType() == InteractionType.AMOUNT_GLOBAL_BUY_LIMIT) {
                            finalAmount = shop.getGlobalBuyLimit();
                        } else if (dataPack.getInteractionType() == InteractionType.AMOUNT_GLOBAL_SELL_LIMIT) {
                            finalAmount = shop.getGlobalSellLimit();
                        }
                    }

                    final String name = getString("buttons.amount.name");
                    if (name != null) {
                        String disabled = INSTANCE.getLangConfig().getString("disabled");
                        if (disabled == null) disabled = "";

                        final boolean isDecimal = (dataPack.getInteractionType().name().contains("PRICE") || dataPack.getInteractionType() == InteractionType.AMOUNT_BALANCE),
                                isLimit = dataPack.getInteractionType().name().contains("LIMIT");
                        if (!isDecimal)
                            amountItem.setAmount((int) Math.max(1, Math.min(finalAmount, amountItem.getType().getMaxStackSize())));
                        itemMeta.setDisplayName(INSTANCE.getManager().color(name
                                .replace("{amount}", (!isLimit ? (isDecimal ? INSTANCE.getEconomyHandler().format(shop, shop.getCurrencyType(), finalAmount)
                                                                  : INSTANCE.getManager().formatNumber(finalAmount, false))
                                        : ((finalAmount < 0) ? disabled : INSTANCE.getManager().formatNumber(finalAmount, false))))));
                    }

                    amountItem.setAmount((int) Math.min(amountItem.getType().getMaxStackSize(), Math.max(1, finalAmount)));
                    amountItem.setItemMeta(itemMeta);
                    inventory.setItem(amountSlot, INSTANCE.updateNBT(amountItem, "ds-amount", String.valueOf(finalAmount)));
                }
            }

            // fill empty slots. If defined, fill defined slots
            fillEmptySlots(inventory, emptySlots);

            if (shop != null) {
                if (getMenuName().contains("edit")) {
                    final int saleSlot = getInt("sale-item-slot"), tradeSlot = getInt("trade-item-slot");

                    if (saleSlot >= 0 && saleSlot < inventory.getSize()) {
                        if (shop.getShopItem() != null) {
                            final ItemStack clonedItem = dataPack.getSelectedShop().getShopItem().clone();
                            clonedItem.setAmount(dataPack.getSelectedShop().getShopItemAmount());
                            inventory.setItem(saleSlot, clonedItem);
                        } else inventory.setItem(saleSlot, null);
                    }

                    if (shouldShowTradeContent(shop) && tradeSlot >= 0 && tradeSlot < inventory.getSize())
                        inventory.setItem(tradeSlot, dataPack.getSelectedShop().getCurrencyItem().clone());

                    final int currencyTypeSlot = getConfiguration().getInt("buttons.currency-type.slot");
                    if (getConfiguration().contains("buttons.currency-type") && currencyTypeSlot > -1) {
                        final EcoHook ecoHook = INSTANCE.getEconomyHandler().getEcoHook(shop.getCurrencyType());
                        if (ecoHook != null) updateButton(player, inventory, currencyTypeSlot,
                                shop, null, ("{type}:" + (shop.getCurrencyType().equals("item-for-item") ? shop.getTradeItemName() : ecoHook.getName())));
                    }
                } else if (getMenuName().contains("transaction")) {
                    ItemStack previewItem = dataPack.getSelectedShop().getShopItem().clone();
                    if (dataPack.getSelectedShop().getCurrencyType().equals("item-for-item")) {
                        ItemMeta itemMeta = previewItem.getItemMeta();
                        if (itemMeta != null) {
                            List<String> lore = itemMeta.getLore() == null ? new ArrayList<>() : new ArrayList<>(itemMeta.getLore()),
                                    previewLore = getStringList("trade-item-lore");
                            for (int i = -1; ++i < previewLore.size(); )
                                lore.add(INSTANCE.getManager().color(previewLore.get(i)));
                            itemMeta.setLore(lore);
                            previewItem.setItemMeta(itemMeta);
                        }
                    }

                    final int previewSlot = getInt("preview-slot");
                    if (previewSlot >= 0 && previewSlot < inventory.getSize()) {
                        previewItem.setAmount(Math.min(dataPack.getSelectedShop().getShopItemAmount(), previewItem.getMaxStackSize()));
                        inventory.setItem(previewSlot, previewItem);
                    }

                    // ((int) (unitCountItem.getType().getMaxStackSize() * 0.25))

                    if (getConfiguration().contains("buttons.unit")) {
                        final int unitSlot = getInt("buttons.unit.slot");
                        if (unitSlot >= 0 && unitSlot < inventory.getSize()) {
                            ItemStack unitItem = inventory.getItem(unitSlot);
                            if (unitItem != null) {
                                if (getConfiguration().contains("buttons.unit-increase-more")) {
                                    final int unitMoreSlot = getInt("buttons.unit-increase-more.slot");
                                    if (unitMoreSlot >= 0 && unitMoreSlot < inventory.getSize()) {
                                        ItemStack itemStack = inventory.getItem(unitMoreSlot);
                                        if (itemStack != null)
                                            itemStack.setAmount((int) (unitItem.getType().getMaxStackSize() * 0.25));
                                    }
                                }

                                if (getConfiguration().contains("buttons.unit-decrease-more")) {
                                    final int unitMoreSlot = getInt("buttons.unit-decrease-more.slot");
                                    if (unitMoreSlot >= 0 && unitMoreSlot < inventory.getSize()) {
                                        ItemStack itemStack = inventory.getItem(unitMoreSlot);
                                        if (itemStack != null)
                                            itemStack.setAmount((int) (unitItem.getType().getMaxStackSize() * 0.25));
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }).thenRun(() -> {
            if (getMenuName().contains("appearance") || getMenuName().contains("assistants")) {
                refreshPageButtons(player, dataPack, inventory, null);
                loadPages(player, dataPack, shop, stitchedSearchText, null, inventory);
            } else if (getMenuName().contains("visit")) {
                final String bothType = getString("visit-types.both");
                updateButton(player, inventory, getConfiguration().getInt("buttons.type.slot"), shop, null, ("{type}:" + bothType));

                refreshPageButtons(player, dataPack, inventory, null);
                loadPages(player, dataPack, null, stitchedSearchText, null, inventory);
            }
        });

        player.openInventory(inventory);
    }

    public void updateButton(@NotNull Player player, @NotNull Inventory inventory, int slot, @Nullable Shop shop,
                             @Nullable List<Integer> emptySlots, @Nullable String... extraPlaceHolders) {
        ConfigurationSection mainSection = getConfigurationSection("buttons");
        if (mainSection == null) return;
        buildButton(mainSection, getButtonName(slot), player, inventory, shop, emptySlots, extraPlaceHolders);
    }

    private void updatePageButtons(@NotNull Player player, @NotNull Inventory inventory, @NotNull DataPack dataPack,
                                   @Nullable Shop shop, @Nullable List<Integer> emptySlots) {
        ConfigurationSection mainSection = getConfigurationSection("buttons");
        if (mainSection != null) {
            if (dataPack.hasNextPage()) {
                buildButton(mainSection, "next", player, inventory, shop, emptySlots);
            } else fillSlot(inventory, mainSection.getInt("next.slot"));

            if (dataPack.hasPreviousPage()) {
                buildButton(mainSection, "previous", player, inventory, shop, emptySlots);
            } else fillSlot(inventory, mainSection.getInt("previous.slot"));
        }
    }

    private void buildButtons(@NotNull Player player, @NotNull Inventory inventory, @Nullable List<Integer> emptySlots) {
        ConfigurationSection mainSection = getConfigurationSection("buttons");
        if (mainSection != null) {
            final Collection<String> buttonActions = mainSection.getKeys(false);
            if (!buttonActions.isEmpty()) {
                final DDataPack dataPack = (DDataPack) INSTANCE.getManager().getDataPack(player);
                final Shop shop = dataPack.getSelectedShop();

                final boolean isPageMenu = (getMenuName().contains("appearance")
                        || getMenuName().contains("visit") || getMenuName().contains("assistants"));

                buttonActions.parallelStream().forEach(buttonAction -> {
                    // checks whether to add the next and/or previous buttons for page menus
                    if ((isPageMenu && (buttonAction.equals("next") && !dataPack.hasNextPage() || buttonAction.equals("previous") && !dataPack.hasPreviousPage()))
                            || (shop != null && !shouldShowTradeContent(shop) && buttonAction.contains("trade")) || !mainSection.contains(buttonAction)
                            /* || (buttonAction.equals("custom-amount") && INSTANCE.isGeyserInstalled()
                            && org.geysermc.geyser.api.GeyserApi.api().isBedrockPlayer(player.getUniqueId()))*/) return;
                    buildButton(mainSection, buttonAction, player, inventory, shop, emptySlots);
                });
            }
        }
    }

    public void buildButton(@NotNull ConfigurationSection mainSection, @NotNull String buttonAction, @NotNull Player player,
                            @NotNull Inventory inventory, @Nullable Shop shop, @Nullable List<Integer> emptySlots, @Nullable String... extraPlaceHolders) {
        final int slot = (mainSection.contains(buttonAction + ".slot") ? mainSection.getInt(buttonAction + ".slot") : 0);

        if (slot < 0 || slot >= inventory.getSize()) return;
        else inventory.setItem(slot, null);

        getButtonLocationMap().put(slot, buttonAction);

        if (emptySlots == null || !emptySlots.contains(slot)) {
            final String materialName = (mainSection.contains(buttonAction + ".material") ? mainSection.getString(buttonAction + ".material") : "STONE");
            final int amount = (mainSection.contains(buttonAction + ".amount") ? mainSection.getInt(buttonAction + ".amount") : 1),
                    durability = (mainSection.contains(buttonAction + ".durability") ? mainSection.getInt(buttonAction + ".durability") : 1);

            final CustomItem item = (shop != null ? new CustomItem(materialName, amount, durability, shop,
                    (shop.getShopItem() != null ? shop.getShopItem().getMaxStackSize() : 1), 1)
                    : new CustomItem(materialName, amount, durability));

            if (item.get().getType() == Material.STONE && (materialName == null || !materialName.contains("STONE"))) {
                INSTANCE.log(Level.WARNING, "The button \"" + buttonAction + "\" from the \"" + getMenuName() + "\" menu failed to build right.");
                return;
            }

            final String name = mainSection.getString(buttonAction + ".name");
            if (name != null && !name.isEmpty())
                item.setDisplayName(null, shop, name, extraPlaceHolders);

            final List<String> lore = mainSection.getStringList(buttonAction + ".lore");
            if (!lore.isEmpty()) item.setLore(null, lore, extraPlaceHolders);

            final int customModelData = (mainSection.contains(buttonAction + ".custom-model-data")
                    ? mainSection.getInt(buttonAction + ".custom-model-data") : 0);
            item.setModelData(customModelData);

            final List<String> enchants = mainSection.getStringList(buttonAction + ".enchants");
            if (!enchants.isEmpty()) item.setEnchantments(enchants);

            final List<String> flags = mainSection.getStringList(buttonAction + ".flags");
            if (!flags.isEmpty()) item.setItemFlags(flags);

            final ItemStack itemStack = item.get();
            itemStack.setAmount(amount);

            inventory.setItem(slot, itemStack);
        }
    }

    @Override
    public void switchPage(@NotNull Inventory inventory, @NotNull Player player, int page) {
        List<Integer> emptySlots = getIntegerList("empty-slots");
        emptySlots.parallelStream().forEach(slot -> {
            if (slot > -1 && slot < inventory.getSize()) inventory.setItem(slot, null);
        });

        final DDataPack dataPack = (DDataPack) INSTANCE.getManager().getDataPack(player);
        if (dataPack.getPageMap() != null && !dataPack.getPageMap().isEmpty()) {
            final Shop shop = dataPack.getSelectedShop();

            if (dataPack.getPageMap().containsKey(page)) {
                final List<ItemStack> buttons = dataPack.getPageMap().get(page);
                for (int i = -1; ++i < buttons.size(); ) {
                    final ItemStack button = buttons.get(i);
                    inventory.addItem(button);
                }
            }

            dataPack.setCurrentPage(page);
            updatePageButtons(player, inventory, dataPack, shop, emptySlots);
        }
    }

    private void clearEmptySlots(@NotNull Inventory inventory) {
        List<Integer> emptySlots = getIntegerList("empty-slots");
        emptySlots.parallelStream().forEach(slot -> {
            if (slot > -1 && slot < inventory.getSize()) inventory.setItem(slot, null);
        });
    }

    @SuppressWarnings("UnnecessaryUnicodeEscape")
    @Override
    public void loadPages(@NotNull Player player, @NotNull DataPack dataPack, @Nullable Shop shop, @Nullable String searchText,
                          @Nullable ItemStack typeItem, @NotNull Inventory inventory) {
        switch (getMenuName()) {
            case "visit": {
                final boolean showAdminShop = getBoolean("show-admin-shops");

                ShopActionType actionType = (typeItem != null ? ShopActionType.getTypeFromItem(typeItem, this) : null);
                if (actionType != null) actionType = actionType.getNext();

                AtomicInteger counter = new AtomicInteger();
                final ShopActionType finalActionType = actionType;

                INSTANCE.getServer().getScheduler().runTaskAsynchronously(INSTANCE, () -> {
                    final HashMap<Integer, List<ItemStack>> pageMap = new HashMap<>();
                    int currentPage = 1;
                    List<ItemStack> pageContents = new ArrayList<>();

                    OfflinePlayer offlinePlayer = null;
                    if (searchText != null && !searchText.isEmpty()) {
                        OfflinePlayer op = INSTANCE.getServer().getOfflinePlayer(searchText);
                        if (op.hasPlayedBefore()) offlinePlayer = op;
                    }

                    for (Shop currentShop : INSTANCE.getManager().getShopMap().values()) {
                        if (currentShop == null || currentShop.getBaseLocation() == null || currentShop.getShopItem() == null
                                || currentShop.getStock() == 0 || (currentShop.isAdminShop() ? !showAdminShop : (currentShop.getStock() < currentShop.getShopItemAmount()))
                                || (finalActionType != null && !finalActionType.passesCheck(currentShop))) continue;

                        if (searchText != null && !searchText.isEmpty()) {
                            if (offlinePlayer != null) {
                                if (currentShop.getOwnerUniqueId() == null || !currentShop.getOwnerUniqueId().toString().equals(offlinePlayer.getUniqueId().toString()))
                                    continue;

                                OfflinePlayer op = INSTANCE.getServer().getOfflinePlayer(currentShop.getOwnerUniqueId());
                                if (op.hasPlayedBefore() && op.getName() != null && !op.getName().equalsIgnoreCase(offlinePlayer.getName()))
                                    continue;
                            } else if (currentShop.getShopItem().getItemMeta() != null
                                    && !ChatColor.stripColor(currentShop.getShopItem().getItemMeta().getDisplayName().toLowerCase()).contains(searchText.toLowerCase())
                                    && !currentShop.getShopItem().getType().name().toLowerCase().replace("_", " ").contains(searchText.toLowerCase())
                                    && !currentShop.getDescription().toLowerCase().contains(searchText.toLowerCase())
                                    && !(currentShop.getBuyPrice(false) + " " + currentShop.getSellPrice(false)).contains(searchText))
                                continue;

                            counter.getAndIncrement();
                        }

                        if (pageContents.size() >= (getSize() - 9)) {
                            pageMap.put(currentPage, new ArrayList<>(pageContents));
                            pageContents.clear();
                            currentPage += 1;
                        }

                        if (currentShop.getVisitIcon() != null) pageContents.add(currentShop.getVisitIcon());
                    }

                    if (!pageContents.isEmpty()) pageMap.put(currentPage, new ArrayList<>(pageContents));
                    dataPack.setPageMap(pageMap);

                    if (player.isOnline()) {
                        if (dataPack.getPageMap() != null && !dataPack.getPageMap().isEmpty()) {
                            refreshPageButtons(player, dataPack, inventory, null);

                            if (searchText != null && !searchText.isEmpty()) {
                                final int searchSlot = getConfiguration().getInt("buttons.search.slot");
                                updateButton(player, inventory, searchSlot, null, null, ("{search-text}:" + searchText));

                                ItemStack searchItem = inventory.getItem(searchSlot);
                                if (searchItem != null)
                                    inventory.setItem(searchSlot, INSTANCE.updateNBT(searchItem, "ds-search", searchText));

                                final String message = INSTANCE.getLangConfig().getString((counter.get() > 0) ? "visit-filter-count" : "visit-filter-none");
                                if (message != null && !message.equalsIgnoreCase(""))
                                    INSTANCE.getManager().sendMessage(player, message
                                            .replace("{count}", INSTANCE.getManager().formatNumber(counter.get(), false))
                                            .replace("{filter}", searchText));
                            }
                        } else clearEmptySlots(inventory);
                    }
                });
                break;
            }
            case "assistants": {
                if (shop != null) {
                    final String activeColor = getString("active-color"),
                            inActiveColor = getString("inactive-color");
                    final List<String> loreFormat = getStringList("head-lore");

                    INSTANCE.getServer().getScheduler().runTaskAsynchronously(INSTANCE, () -> {
                        final HashMap<Integer, List<ItemStack>> pageMap = new HashMap<>();
                        int currentPage = 1;
                        List<ItemStack> pageContents = new ArrayList<>();

                        List<UUID> assistants = new ArrayList<>(shop.getAssistants());
                        for (int i = -1; ++i < assistants.size(); ) {
                            final UUID uuid = assistants.get(i);
                            OfflinePlayer offlinePlayer = INSTANCE.getServer().getOfflinePlayer(uuid);
                            if (!offlinePlayer.hasPlayedBefore()) continue;

                            if (searchText != null && !searchText.isEmpty()
                                    && !(offlinePlayer.getName() != null && offlinePlayer.getName().toLowerCase().startsWith(searchText.toLowerCase()))
                                    && !offlinePlayer.getUniqueId().toString().startsWith(searchText)) continue;

                            final CustomItem item = new CustomItem(("HEAD:" + uuid), 0, 1)
                                    .setDisplayName(player, shop, (shop.getAssistants().contains(offlinePlayer.getUniqueId())
                                            ? (activeColor + offlinePlayer.getName()) : (inActiveColor + offlinePlayer.getName())))
                                    .setLore(null, loreFormat, ("{player}:" + offlinePlayer.getName()));

                            if (pageContents.size() >= (getSize() - 9)) {
                                pageMap.put(currentPage, new ArrayList<>(pageContents));
                                pageContents.clear();
                                currentPage += 1;
                            }

                            pageContents.add(INSTANCE.updateNBT(item.get(), "uuid", uuid.toString()));
                        }

                        if (!pageContents.isEmpty()) pageMap.put(currentPage, new ArrayList<>(pageContents));
                        dataPack.setPageMap(pageMap);

                        if (player.isOnline()) {
                            if (dataPack.getPageMap() != null && !dataPack.getPageMap().isEmpty()) {
                                refreshPageButtons(player, dataPack, inventory, null);
                                if (searchText != null) {
                                    final int searchSlot = getConfiguration().getInt("buttons.search.slot");
                                    updateButton(player, inventory, searchSlot, null, null, ("{search-text}:" + searchText));

                                    ItemStack searchItem = inventory.getItem(searchSlot);
                                    if (searchItem != null)
                                        inventory.setItem(searchSlot, INSTANCE.updateNBT(searchItem, "ds-search", searchText));
                                }
                            } else clearEmptySlots(inventory);
                        }
                    });
                }
                break;
            }
            case "appearance": {
                if (shop != null) {
                    INSTANCE.getServer().getScheduler().runTaskAsynchronously(INSTANCE, () -> {
                        final HashMap<Integer, List<ItemStack>> pageMap = new HashMap<>();
                        int currentPage = 1;
                        List<ItemStack> pageContents = new ArrayList<>();

                        final List<Appearance> appearances = new ArrayList<>(Appearance.getAppearances());
                        if (getBoolean("sort-alphabetically"))
                            Collections.sort(appearances); // sort appearances alphabetically

                        for (int i = -1; ++i < appearances.size(); ) {
                            Appearance appearance = appearances.get(i);
                            if (appearance == null || (searchText != null && !searchText.isEmpty()
                                    && !appearance.getId().toLowerCase().contains(searchText.toLowerCase()))
                                    && !appearance.getMaterial().toLowerCase().contains(searchText.toLowerCase()))
                                continue;

                            if (pageContents.size() >= (getSize() - 9)) {
                                pageMap.put(currentPage, new ArrayList<>(pageContents));
                                pageContents.clear();
                                currentPage += 1;
                            }

                            ItemStack itemStack = appearance.build(player, shop);
                            if (itemStack != null) pageContents.add(itemStack);
                        }

                        if (!pageContents.isEmpty()) pageMap.put(currentPage, new ArrayList<>(pageContents));
                        dataPack.setPageMap(pageMap);

                        if (player.isOnline()) {
                            if (dataPack.getPageMap() != null && !dataPack.getPageMap().isEmpty()) {
                                refreshPageButtons(player, dataPack, inventory, null);
                                if (searchText != null) {
                                    final int searchSlot = getConfiguration().getInt("buttons.search.slot");
                                    updateButton(player, inventory, searchSlot, null, null, ("{search-text}:" + searchText));

                                    ItemStack searchItem = inventory.getItem(searchSlot);
                                    if (searchItem != null)
                                        inventory.setItem(searchSlot, INSTANCE.updateNBT(searchItem, "ds-search", searchText));
                                }
                            } else clearEmptySlots(inventory);
                        }
                    });
                    break;
                }
            }
            case "log": {
                if (shop != null) {
                    /*INSTANCE.getServer().getScheduler().runTaskAsynchronously(INSTANCE, () -> {
                        final HashMap<Integer, List<ItemStack>> pageMap = new HashMap<>();
                        int currentPage = 1;
                        List<ItemStack> pageContents = new ArrayList<>();


                    });*/
                }
                break;
            }
            default: {
                break;
            }
        }

        dataPack.setCurrentPage(1);
    }

    private void refreshPageButtons(@NotNull Player player, @NotNull DataPack dataPack, @NotNull Inventory inventory, @Nullable Shop shop) {
        if (getMenuName().contains("visit")) {
            updateButton(player, inventory, getConfiguration().getInt("buttons.search.slot"),
                    shop, null, ("{search-text}:" + INSTANCE.getLangConfig().getString("not-applicable")));
            switchPage(inventory, player, dataPack.getCurrentPage());
        } else if (getMenuName().contains("appearance") || getMenuName().contains("assistants")) {
            updateButton(player, inventory, getConfiguration().getInt("buttons.search.slot"),
                    shop, null, ("{search-text}:" + INSTANCE.getLangConfig().getString("not-applicable")));
            switchPage(inventory, player, dataPack.getCurrentPage());
        }
    }

    /**
     * Compares an itemstack to the menu's filler item.
     *
     * @param itemStack The item to compare.
     * @return Whether the itemstack is similar to the filler item.
     */
    public boolean isFillerItem(@NotNull ItemStack itemStack) {
        return itemStack.isSimilar(new CustomItem(getString("filler-material"), 0, 1)
                .setDisplayName(null, null, "&6").get());
    }

    private void fillEmptySlots(@NotNull Inventory inventory, @Nullable List<Integer> emptySlots) {
        final String mat = getString("filler-material");
        if (mat == null || mat.toUpperCase().contains("AIR")) return;

        final CustomItem fillItem = new CustomItem(mat, 0, 1)
                .setDisplayName(null, null, "&6")
                .setModelData(getInt("filler-model-data"));

        for (int i = -1; ++i < inventory.getSize(); ) {
            final ItemStack itemStack = inventory.getItem(i);
            if ((itemStack != null && !itemStack.getType().name().contains("AIR"))
                    || (emptySlots != null && emptySlots.contains(i))) continue;
            inventory.setItem(i, fillItem.get());
        }
    }

    private void fillSlot(@NotNull Inventory inventory, int slot) {
        if (slot >= 0 && slot < inventory.getSize()) {
            final String mat = getString("filler-material");
            if (mat == null || mat.toUpperCase().contains("AIR")) return;
            inventory.setItem(slot, new CustomItem(mat, 0, 1)
                    .setDisplayName(null, null, "&6")
                    .setModelData(getInt("filler-model-data"))
                    .get());
        }
    }

    public int getButtonSlot(@NotNull String name) {
        for (Map.Entry<Integer, String> entry : getButtonLocationMap().entrySet())
            if (entry.getValue().equals(name)) return entry.getKey();
        return -1;
    }

    /**
     * Saves the configuration file to the disk.
     */
    @Override
    public void save() {
        if (file == null) return;

        try {
            save(file);
        } catch (IOException e) {
            e.printStackTrace();
            INSTANCE.getServer().getLogger().warning(e.getMessage());
        }
    }

    /**
     * Reloads the configuration file from disk.
     */
    @Override
    public void reload() {
        try {
            load(file);
        } catch (IOException | InvalidConfigurationException e) {
            e.printStackTrace();
            INSTANCE.getServer().getLogger().warning(e.getMessage());
        }

        loadConfiguration(file);
    }

    @Override
    public FileConfiguration getConfiguration() {
        return this;
    }

    /**
     * @return The name of the configuration file.
     */
    @Override
    public String getFileName() {
        return fileName;
    }

    /**
     * @return The name of the menu.
     */
    @Override
    public String getMenuName() {
        return menuName;
    }

    /**
     * @return The configuration file associated to the menu.
     */
    @Override
    public File getFile() {
        return file;
    }

    /**
     * @return The map of buttons alongside their placement for the menu.
     */
    @Override
    public HashMap<Integer, String> getButtonLocationMap() {
        return buttonLocationMap;
    }

    /**
     * @return The size of the menu.
     */
    public int getSize() {
        return size;
    }

    /**
     * Sets the size of the menu for all future builds.
     *
     * @param size The new menu size.
     */
    public void setSize(int size) {
        if (size <= 5) this.size = 5;
        else if ((size % 9) == 0) this.size = Math.min(size, 54);
        else this.size = 54;
    }

    /**
     * @return The current color translated title of the menu.
     */
    public String getTitle() {
        return title;
    }

    /**
     * Sets the title of the menu for all future builds.
     *
     * @param title The new title for the menu.
     */
    public void setTitle(@NotNull String title) {
        this.title = title;
    }

    public boolean shouldShowTradeContent(@NotNull Shop shop) {
        return (getMenuName().equals("edit") && shop.getCurrencyType().equalsIgnoreCase("item-for-item"));
    }

}