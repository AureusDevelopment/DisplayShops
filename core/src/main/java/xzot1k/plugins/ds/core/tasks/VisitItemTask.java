package xzot1k.plugins.ds.core.tasks;

import net.md_5.bungee.api.ChatColor;
import org.apache.commons.lang3.StringUtils;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.NotNull;
import xzot1k.plugins.ds.DisplayShops;
import xzot1k.plugins.ds.api.objects.LocationClone;
import xzot1k.plugins.ds.api.objects.Menu;
import xzot1k.plugins.ds.api.objects.Shop;

import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;

public class VisitItemTask extends BukkitRunnable {
    private final DisplayShops INSTANCE;
    private final boolean forceUse;
    private final Queue<UUID> rebuildQueue;
    private boolean showAdminShops;
    private String adminType, playerType, shopNameFormat;
    private List<String> loreFormat;
    private double visitCost;
    private boolean runFirstTime = true;

    public VisitItemTask(@NotNull DisplayShops pluginInstance) {
        this.INSTANCE = pluginInstance;
        rebuildQueue = new ConcurrentLinkedQueue<>();

        Menu visitMenu = INSTANCE.getMenu("visit");
        if (visitMenu != null) {
            FileConfiguration visitConfig = visitMenu.getConfiguration();
            showAdminShops = visitConfig.getBoolean("show-admin-shops");
            visitCost = visitConfig.getDouble("visit-charge");
            adminType = visitConfig.getString("visit-icon.type-admin");
            playerType = visitConfig.getString("visit-icon.type-normal");
            shopNameFormat = visitConfig.getString("visit-icon.name");
            loreFormat = visitConfig.getStringList("visit-icon.lore");
        }

        forceUse = INSTANCE.getConfig().getBoolean("shop-currency-item.force-use");
    }

    @Override
    public void run() {
        if (runFirstTime) {
            for (Shop shop : INSTANCE.getManager().getShopMap().values()) {
                if (shop.getBaseLocation() != null && shop.getShopItem() != null) {
                    if ((showAdminShops && shop.isAdminShop()) || !shop.isAdminShop())
                        shop.setVisitIcon(buildItem(shop));
                    rebuildQueue.remove(shop.getShopId());
                }
            }

            runFirstTime = false;
        }

        while (!getRebuildQueue().isEmpty()) {
            final UUID shopId = getRebuildQueue().remove();

            final Shop shop = DisplayShops.getPluginInstance().getManager().getShopById(shopId);
            if (shop == null) continue;

            if (shop.getBaseLocation() == null || (!showAdminShops && shop.isAdminShop()) || shop.getShopItem() == null)
                continue;
            shop.setVisitIcon(buildItem(shop));
        }

        //  INSTANCE.getManager().sort((o1, o2) -> (o1.getValue().getType().name().compareToIgnoreCase(o2.getValue().getType().name())));
    }

    private ItemStack buildItem(@NotNull Shop shop) {
        final LocationClone location = shop.getBaseLocation();

        ItemStack shopItemClone = shop.getShopItem().clone();
        shopItemClone.setAmount(1);

        ItemStack itemStack = INSTANCE.updateNBT(shopItemClone, "currentShop-id", shop.getShopId().toString());
        ItemMeta itemMeta = itemStack.getItemMeta();
        if (itemMeta != null) {
            itemMeta.addItemFlags(ItemFlag.values());
            final String itemName = (shop.getShopItem() != null ? INSTANCE.getManager().getItemName(shop.getShopItem()) : ""),
                    tradeItemName = (shop.getCurrencyType().equals("item-for-item") ? ((!forceUse && shop.getTradeItem() != null)
                                                                                       ? INSTANCE.getManager().getItemName(shop.getTradeItem())
                                                                                       : INSTANCE.getManager().getItemName(INSTANCE.getManager().buildShopCurrencyItem(1))) : "");

            itemMeta.setDisplayName(INSTANCE.getManager().color(Objects.requireNonNull(shopNameFormat).replace("{item}", itemName).replace("{trade}", tradeItemName)));
            itemMeta.setLore(new ArrayList<String>() {{
                OfflinePlayer offlinePlayer = (shop.getOwnerUniqueId() == null ? null : INSTANCE.getServer().getOfflinePlayer(shop.getOwnerUniqueId()));
                final String colorCode = INSTANCE.getConfig().getString("default-description-color");
                for (int i = -1; ++i < loreFormat.size(); ) {
                    final String line = loreFormat.get(i);
                    if ((line.contains("{owner}") && shop.getOwnerUniqueId() == null)
                            || (line.contains("{buy}") && !line.contains("{sell}") && shop.getBuyPrice(false) <= 0)
                            || (line.contains("{sell}") && !line.contains("{buy}") && shop.getSellPrice(false) <= 0))
                        continue;

                    if (line.equalsIgnoreCase("{description}")) {
                        if ((shop.getDescription() == null || shop.getDescription().isEmpty())) {
                            add("");
                            continue;
                        }

                        add("");
                        List<String> descriptionLines = INSTANCE.getManager().wrapString(shop.getDescription());
                        for (int j = -1; ++j < descriptionLines.size(); ) {
                            String newLine = INSTANCE.getManager().color(descriptionLines.get(j));
                            add(newLine.contains(ChatColor.COLOR_CHAR + "") ? newLine : (INSTANCE.getManager().color(colorCode + newLine)));
                        }
                        add("");
                        continue;
                    }

                    if (line.toLowerCase().contains("{enchants}")) {
                        if (shop.getShopItem() == null) continue;

                        if (shop.getShopItem().getType() == Material.ENCHANTED_BOOK) {
                            EnchantmentStorageMeta bookMeta = (EnchantmentStorageMeta) shop.getShopItem().getItemMeta();
                            if (bookMeta != null)
                                for (Map.Entry<Enchantment, Integer> entry : bookMeta.getStoredEnchants().entrySet())
                                    add(INSTANCE.getManager().color(line.replace("{enchants}", (INSTANCE.getManager().getTranslatedName(entry.getKey())
                                            + " " + INSTANCE.getManager().getRomanNumeral(entry.getValue())))));
                        } else if (!shop.getShopItem().getEnchantments().isEmpty()) {
                            for (Map.Entry<Enchantment, Integer> entry : shop.getShopItem().getEnchantments().entrySet()) {
                                add(INSTANCE.getManager().color(line.replace("{enchants}", (INSTANCE.getManager().getTranslatedName(entry.getKey()) + " "
                                        + INSTANCE.getManager().getRomanNumeral(entry.getValue())))));
                            }
                        }

                        if (shop.getShopItem().getType().name().contains("POTION")) {
                            PotionMeta potionMeta = (PotionMeta) shop.getShopItem().getItemMeta();
                            if (potionMeta != null) {
                                final String translatedName = INSTANCE.getManager().getTranslatedName(potionMeta.getBasePotionData().getType());
                                if (!translatedName.equalsIgnoreCase("Uncraftable"))
                                    add(INSTANCE.getManager().color(line.replace("{enchants}", (translatedName))));
                                for (PotionEffect potionEffect : potionMeta.getCustomEffects())
                                    add(INSTANCE.getManager().color(line.replace("{enchants}",
                                            (StringUtils.capitalize(potionEffect.getType().getName().toLowerCase().replace("_", " "))
                                                    + " " + INSTANCE.getManager().getRomanNumeral(potionEffect.getAmplifier() + 1)
                                                    + " " + (potionEffect.getDuration() / 20) + "s"))));
                            }
                            continue;
                        }

                        continue;
                    }

                    add(INSTANCE.getManager().color(line
                            .replace("{owner}", ((shop.getOwnerUniqueId() != null && offlinePlayer != null && offlinePlayer.getName() != null) ? offlinePlayer.getName() : ""))
                            .replace("{balance}", ((shop.isAdminShop() && shop.getStoredBalance() <= 0) ? "\u221E" : INSTANCE.getEconomyHandler().format(shop,
                                    shop.getCurrencyType(), shop.getStoredBalance())))
                            .replace("{stock}", ((shop.isAdminShop() && shop.getStock() < 0) ? "\u221E" : INSTANCE.getManager().formatNumber(shop.getStock(), false)))
                            .replace("{description}", ((shop.getDescription() != null && !shop.getDescription().isEmpty()) ? shop.getDescription() : ""))
                            .replace("{world}", location.getWorldName())
                            .replace("{x}", INSTANCE.getManager().formatNumber((int) location.getX(), false))
                            .replace("{y}", INSTANCE.getManager().formatNumber((int) location.getY(), false))
                            .replace("{z}", INSTANCE.getManager().formatNumber((int) location.getZ(), false))
                            .replace("{buy}", INSTANCE.getEconomyHandler().format(shop, shop.getCurrencyType(), shop.getBuyPrice(shop.canDynamicPriceChange())))
                            .replace("{sell}", INSTANCE.getEconomyHandler().format(shop, shop.getCurrencyType(), shop.getSellPrice(shop.canDynamicPriceChange())))
                            .replace("{cost}", INSTANCE.getEconomyHandler().format(shop, shop.getCurrencyType(), visitCost))
                            .replace("{type}", (Objects.requireNonNull(shop.isAdminShop() ? adminType : playerType)))
                            .replace("{amount}", INSTANCE.getManager().formatNumber(shop.getShopItemAmount(), false))
                            .replace("{item}", itemName).replace("{trade}", tradeItemName)));
                }
            }});
            itemStack.setItemMeta(itemMeta);
        }

        return INSTANCE.updateNBT(itemStack, "shop-id", shop.getShopId().toString());
    }

    public Queue<UUID> getRebuildQueue() {
        return rebuildQueue;
    }
}