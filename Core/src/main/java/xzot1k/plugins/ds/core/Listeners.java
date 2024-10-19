/*
 * Copyright (c) 2021 XZot1K, All rights reserved.
 */

package xzot1k.plugins.ds.core;

import me.devtec.shared.Ref;
import org.apache.commons.lang.WordUtils;
import org.bukkit.Axis;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.block.data.Directional;
import org.bukkit.block.data.Orientable;
import org.bukkit.block.data.Rotatable;
import org.bukkit.entity.*;
import org.bukkit.event.Cancellable;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.*;
import org.bukkit.event.entity.EntityChangeBlockEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.player.*;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.metadata.MetadataValue;
import org.jetbrains.annotations.NotNull;
import xzot1k.plugins.ds.DisplayShops;
import xzot1k.plugins.ds.api.enums.Direction;
import xzot1k.plugins.ds.api.enums.EconomyCallType;
import xzot1k.plugins.ds.api.enums.EditType;
import xzot1k.plugins.ds.api.enums.ItemType;
import xzot1k.plugins.ds.api.events.*;
import xzot1k.plugins.ds.api.objects.*;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public class Listeners implements Listener {

    private DisplayShops pluginInstance;
    public ItemStack creationItem;

    public Listeners(DisplayShops pluginInstance) {
        setPluginInstance(pluginInstance);
        creationItem = getPluginInstance().getManager().buildShopCreationItem(null, 1);
    }

    @EventHandler
    public void onPickup(PlayerPickupItemEvent e) {
        handlePickup(e, e.getItem(), e.getPlayer());
    }

    private void handlePickup(Cancellable e, Item item, Entity entity) {
        if (item.hasMetadata("shop-owner")) {
            for (MetadataValue value : item.getMetadata("shop-owner"))
                if (value.asString().equals(entity.getUniqueId().toString())) {
                    e.setCancelled(true);
                    break;
                }
        }
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
    public void onPlaceCheck(BlockPlaceEvent e) {

        // TODO fix ItemsAdder
        // System.out.println(e.getItemInHand());
        // System.out.println(creationItem);

        final boolean cancelEvent = shopCreationWorks(e.getPlayer(), e.getBlock(), e.getItemInHand());
        if (cancelEvent) e.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onLava(PlayerBucketEmptyEvent e) {
        if (e.getBucket().name().contains("LAVA")) {
            final Location loc = e.getBlockClicked().getLocation();
            for (Shop shop : getPluginInstance().getManager().getShopMap().values()) {
                if (shop.getBaseLocation().getY() <= e.getBlockClicked().getY() && shop.getBaseLocation().distance(loc, true) < 2) {
                    e.setCancelled(true);
                    break;
                }
            }
        }
    }

    @SuppressWarnings("deprecation")
    @EventHandler(priority = EventPriority.LOWEST)
    public void onInteract(PlayerInteractEvent e) {
        if (e.getClickedBlock() == null) {
            return;
        }
        boolean block = false;
        if (e.getAction() == Action.RIGHT_CLICK_BLOCK && e.getItem() != null && e.getItem().getType().name().contains("SIGN")) {
            if (e.getPlayer().isSneaking()) {
                return;
            }
            block = true;
        }

        final boolean isOffhandVersion = Ref.isNewerThan(8);
        if (isOffhandVersion && Ref.isNewerThan(11) && e.getHand() != EquipmentSlot.HAND)
            return;

        final DataPack dataPack = getPluginInstance().getManager().getDataPack(e.getPlayer());
        if (dataPack == null) return;
        final ItemStack handItem = (isOffhandVersion ? e.getPlayer().getInventory().getItemInMainHand() : e.getPlayer().getItemInHand());

        if (dataPack.isInSelectionMode()) {
            if (isOffhandVersion && Ref.isNewerThan(12) && e.getHand() == EquipmentSlot.OFF_HAND) {
                e.setCancelled(true);
                return;
            }

            if (e.getAction() == Action.LEFT_CLICK_BLOCK && e.getClickedBlock() != null) {
                e.setCancelled(true);
                if (dataPack.getSelectedRegion() != null)
                    dataPack.getSelectedRegion().setPointOne(new LClone(e.getClickedBlock().getLocation()));
                else {
                    DRegion regionSelection = new DRegion();
                    regionSelection.setPointOne(new LClone(e.getClickedBlock().getLocation()));
                    dataPack.setSelectedRegion(regionSelection);
                }

                String message = getPluginInstance().getLangConfig().getString("selection-one-set");
                if (message != null && !message.equalsIgnoreCase(""))
                    getPluginInstance().getManager().sendMessage(e.getPlayer(), message);
                return;
            } else if (e.getAction() == Action.RIGHT_CLICK_BLOCK && e.getClickedBlock() != null) {
                e.setCancelled(true);

                if (dataPack.getSelectedRegion() != null)
                    dataPack.getSelectedRegion().setPointTwo(new LClone(e.getClickedBlock().getLocation()));
                else {
                    Region regionSelection = new DRegion();
                    regionSelection.setPointTwo(new LClone(e.getClickedBlock().getLocation()));
                    dataPack.setSelectedRegion(regionSelection);
                }

                String message = getPluginInstance().getLangConfig().getString("selection-two-set");
                if (message != null && !message.equalsIgnoreCase(""))
                    getPluginInstance().getManager().sendMessage(e.getPlayer(), message);
                return;
            }
        }

        final Shop shop = getPluginInstance().getManager().getShop(e.getClickedBlock().getLocation());
        if (shop == null) return;
        if (block) {
            e.setCancelled(true);
            return;
        }

        e.setCancelled(true);

        final MarketRegion marketRegion = getPluginInstance().getManager().getMarketRegion(e.getClickedBlock().getLocation());
        if (marketRegion != null && marketRegion.getRenter() == null) {
            getPluginInstance().getManager().sendMessage(e.getPlayer(), Objects.requireNonNull(getPluginInstance().getLangConfig().getString(
                            "rentable-interact"))
                    .replace("{id}", marketRegion.getMarketId()));
            return;
        }

        // ensure blocked nbt is not on the item
        if (getPluginInstance().getManager().hasBlockedNBT(handItem)) {
            String message = getPluginInstance().getLangConfig().getString("set-item-blocked");
            if (message != null && !message.equalsIgnoreCase(""))
                getPluginInstance().getManager().sendMessage(e.getPlayer(), message);
            return;
        }

        final boolean editPrevention = getPluginInstance().getConfig().getBoolean("editor-prevention"),
                emptyShopEdit = getPluginInstance().getConfig().getBoolean("empty-shop-edit"),
                quickActions = getPluginInstance().getConfig().getBoolean("quick-actions");
        if (e.getAction() == Action.RIGHT_CLICK_BLOCK) {
            if (handItem.getType() == Material.LAVA_BUCKET) getPluginInstance().getInSightTask().refreshShop(shop);

            if (e.getClickedBlock() != null) {
                final String blockType = e.getClickedBlock().getType().name();
                if (Ref.isNewerThan(13)) {
                    if (e.getClickedBlock() instanceof org.bukkit.block.Container) {
                        getPluginInstance().getServer().getScheduler().runTaskLater(getPluginInstance(), () -> {
                            e.getPlayer().closeInventory();
                            e.getPlayer().getOpenInventory().close();
                        }, 1);
                    }
                } else if (blockType.contains("CHEST") || blockType.contains("DISPENSER")
                        || blockType.contains("DROPPER") || blockType.contains("HOPPER")
                        || blockType.contains("FURNACE") || blockType.contains("BREWING_STAND")
                        || blockType.contains("JUKEBOX") || blockType.contains("BEACON")) {
                    getPluginInstance().getServer().getScheduler().runTaskLater(getPluginInstance(), () -> {
                        e.getPlayer().closeInventory();
                        e.getPlayer().getOpenInventory().close();
                    }, 1);
                }
            }

            final boolean canEdit = shop.canEdit(e.getPlayer());
            if (e.getPlayer().isSneaking() && canEdit) {
                if (getPluginInstance().getConfig().getBoolean("block-creative") && e.getPlayer().getGameMode() == GameMode.CREATIVE) {
                    String message = getPluginInstance().getLangConfig().getString("creative-blocked");
                    if (message != null && !message.equalsIgnoreCase(""))
                        getPluginInstance().getManager().sendMessage(e.getPlayer(), message);
                    return;
                }

                if (shop.getShopItem() == null) {
                    setShopItem(e.getPlayer(), handItem, shop);
                    return;
                }

                if (!quickActions) return;

                if (shop.isAdminShop() && shop.getStock() < 0) {
                    String message = getPluginInstance().getLangConfig().getString("shop-infinite-stock");
                    if (message != null && !message.equalsIgnoreCase(""))
                        getPluginInstance().getManager().sendMessage(e.getPlayer(), message);
                    return;
                }

                if (shop.getStock() <= 0) {
                    String message = getPluginInstance().getLangConfig().getString("shop-low-stock");
                    if (message != null && !message.equalsIgnoreCase(""))
                        getPluginInstance().getManager().sendMessage(e.getPlayer(), message);
                    return;
                }

                if (e.getPlayer().getInventory().firstEmpty() == -1) {
                    String message = getPluginInstance().getLangConfig().getString("insufficient-space");
                    if (message != null && !message.equalsIgnoreCase(""))
                        getPluginInstance().getManager().sendMessage(e.getPlayer(), message.replace("{space}", "0"));
                    return;
                }

                if (editPrevention) {
                    if (shop.getCurrentEditor() != null && !shop.getCurrentEditor().toString().equals(e.getPlayer().getUniqueId().toString())) {
                        if (getPluginInstance().getServer().getOfflinePlayer(shop.getCurrentEditor()).isOnline()) {
                            e.setCancelled(true);
                            String message = getPluginInstance().getLangConfig().getString("shop-under-edit");
                            if (message != null && !message.equalsIgnoreCase(""))
                                getPluginInstance().getManager().sendMessage(e.getPlayer(), message);
                            return;
                        }
                        shop.setCurrentEditor(null);
                    }
                }

                if (editPrevention) shop.setCurrentEditor(e.getPlayer().getUniqueId());
                dataPack.setSelectedShop(shop);

                String message = getPluginInstance().getLangConfig().getString("quick-withdraw");
                final int itemAmount = Math.min(shop.getShopItem().getMaxStackSize(), shop.getShopItemAmount());
                ShopEditEvent shopEditEvent = new ShopEditEvent(e.getPlayer(), shop, EditType.QUICK_WITHDRAW);
                if (shop.getStock() >= itemAmount) {
                    shopEditEvent.setAmount(itemAmount);
                    getPluginInstance().getServer().getPluginManager().callEvent(shopEditEvent);
                    if (shopEditEvent.isCancelled()) return;

                    shop.setStock(shop.getStock() - itemAmount);

                    ItemStack cloneItem = shop.getShopItem().clone();
                    cloneItem.setAmount(itemAmount);

                    if (e.getPlayer().getInventory().firstEmpty() == -1)
                        e.getPlayer().getWorld().dropItemNaturally(e.getPlayer().getLocation(), cloneItem);
                    else e.getPlayer().getInventory().addItem(cloneItem);

                    if (message != null && !message.equalsIgnoreCase(""))
                        getPluginInstance().getManager().sendMessage(e.getPlayer(), message
                                .replace("{amount}", getPluginInstance().getManager().formatNumber(shop.getShopItemAmount(), false)));
                } else {
                    shopEditEvent.setAmount(shop.getStock());
                    getPluginInstance().getServer().getPluginManager().callEvent(shopEditEvent);
                    if (shopEditEvent.isCancelled()) return;

                    ItemStack shopItemClone = shop.getShopItem().clone();
                    shopItemClone.setAmount(shop.getStock());
                    shop.setStock(0);
                    if (e.getPlayer().getInventory().firstEmpty() == -1)
                        e.getPlayer().getWorld().dropItemNaturally(e.getPlayer().getLocation(), shopItemClone);
                    else e.getPlayer().getInventory().addItem(shopItemClone);
                    if (message != null && !message.equalsIgnoreCase(""))
                        getPluginInstance().getManager().sendMessage(e.getPlayer(), message
                                .replace("{amount}", getPluginInstance().getManager().formatNumber(shopItemClone.getAmount(), false)));
                }

                getPluginInstance().runEventCommands("shop-withdraw", e.getPlayer());
                dataPack.resetEditData();
                getPluginInstance().getInSightTask().refreshShop(shop);
            } else {
                if (shop.getShopItem() == null && !emptyShopEdit) {
                    String message = getPluginInstance().getLangConfig().getString("shop-invalid-item");
                    if (message != null && !message.equalsIgnoreCase(""))
                        getPluginInstance().getManager().sendMessage(e.getPlayer(), message);
                    return;
                }

                if (!shop.isAdminShop() && !getPluginInstance().getConfig().getBoolean("offline-mode")) {
                    Player player = getPluginInstance().getServer().getPlayer(shop.getOwnerUniqueId());
                    if (player == null || !player.isOnline()) {
                        String message = getPluginInstance().getLangConfig().getString("owner-offline");
                        if (message != null && !message.equalsIgnoreCase(""))
                            getPluginInstance().getManager().sendMessage(e.getPlayer(), message);
                        return;
                    }
                }

                if (editPrevention) {
                    if (shop.getCurrentEditor() != null && !shop.getCurrentEditor().toString().equals(e.getPlayer().getUniqueId().toString())) {
                        if (getPluginInstance().getServer().getOfflinePlayer(shop.getCurrentEditor()).isOnline()) {
                            e.setCancelled(true);
                            String message = getPluginInstance().getLangConfig().getString("shop-under-edit");
                            if (message != null && !message.equalsIgnoreCase(""))
                                getPluginInstance().getManager().sendMessage(e.getPlayer(), message);
                            return;
                        }
                        shop.setCurrentEditor(null);
                    }
                }

                if (!shop.isAdminShop() && (shop.getOwnerUniqueId().toString().equals(e.getPlayer().getUniqueId().toString())
                        || shop.getAssistants().contains(e.getPlayer().getUniqueId()))) {
                    ShopEditEvent shopEditEvent = new ShopEditEvent(e.getPlayer(), shop, EditType.OPEN_EDIT_MENU);
                    getPluginInstance().getServer().getPluginManager().callEvent(shopEditEvent);
                    if (shopEditEvent.isCancelled()) return;

                    if (editPrevention) shop.setCurrentEditor(e.getPlayer().getUniqueId());
                    dataPack.setSelectedShop(shop);
                    /*System.out.println("A");
                    if (pluginInstance.isGeyserInstalled()) {
                        GeyserConnection conn = pluginInstance.geyserHook.getPlayer(e.getPlayer());
                        if (conn != null) {
                            System.out.println("SEND_FORM_1");
                            conn.sendForm(CustomForm.builder().title("TestTitle").toggle("test_toggle", true).closedOrInvalidResultHandler(() -> System.out.println("Test_1")).resultHandler((a, b) -> System.out.println(a.content() + " | " + b.responseType())).build());
                            getPluginInstance().runEventCommands("shop-edit", e.getPlayer());
                            return;
                        }
                    }*/
                    final Menu editMenu = getPluginInstance().getMenu("edit");
                    if (editMenu != null) editMenu.build(e.getPlayer());

                    getPluginInstance().runEventCommands("shop-edit", e.getPlayer());
                    return;
                }

                if (emptyShopEdit && shop.getShopItem() == null) {
                    String message = getPluginInstance().getLangConfig().getString("empty-shop");
                    if (message != null && !message.equalsIgnoreCase(""))
                        getPluginInstance().getManager().sendMessage(e.getPlayer(), message);
                    return;
                }

                ShopTransactionEvent shopTransactionEvent = new ShopTransactionEvent(e.getPlayer(), shop);
                getPluginInstance().getServer().getPluginManager().callEvent(shopTransactionEvent);
                if (shopTransactionEvent.isCancelled()) return;

                dataPack.setSelectedShop(shop);
                /*System.out.println("B");
                if (pluginInstance.isGeyserInstalled()) {
                    GeyserConnection conn = pluginInstance.geyserHook.getPlayer(e.getPlayer());
                    if (conn != null) {
                        System.out.println("SEND_FORM_2");
                        FloodgateApi api = FloodgateApi.getInstance();
                        api.getPlayer(e.getPlayer().getUniqueId()).sendForm(CustomForm.builder().title("TestTitle").toggle("test_toggle", true).closedOrInvalidResultHandler(() -> System.out.println("Test_1")).resultHandler((a, b) -> System.out.println(a.content() + " | " + b.responseType())));
                        getPluginInstance().runEventCommands("shop-edit", e.getPlayer());
                        return;
                    }
                }*/
                final Menu editMenu = getPluginInstance().getMenu("transaction");
                if (editMenu != null) editMenu.build(e.getPlayer());

                getPluginInstance().runEventCommands("shop-open", e.getPlayer());
            }
        } else if (e.getAction() == Action.LEFT_CLICK_BLOCK) {
            if (quickActions && e.getPlayer().isSneaking() && shop.canEdit(e.getPlayer())) {
                if (getPluginInstance().getConfig().getBoolean("block-creative") && e.getPlayer().getGameMode() == GameMode.CREATIVE) {
                    String message = getPluginInstance().getLangConfig().getString("creative-blocked");
                    if (message != null && !message.equalsIgnoreCase(""))
                        getPluginInstance().getManager().sendMessage(e.getPlayer(), message);
                    return;
                }

                if (shop.getShopItem() == null && marketRegion == null) {
                    if (!e.getPlayer().hasPermission("displayshops.delete")) {
                        dataPack.resetEditData();
                        String message = getPluginInstance().getLangConfig().getString("no-permission");
                        if (message != null && !message.equalsIgnoreCase(""))
                            getPluginInstance().getManager().sendMessage(e.getPlayer(), message);
                        return;
                    }

                    if (!e.getPlayer().hasPermission("displayshops.admin") && shop.getOwnerUniqueId() != null
                            && !shop.getOwnerUniqueId().toString().equals(e.getPlayer().getUniqueId().toString())) {
                        dataPack.resetEditData();
                        String message = getPluginInstance().getLangConfig().getString("not-shop-owner");
                        if (message != null && !message.equalsIgnoreCase(""))
                            getPluginInstance().getManager().sendMessage(e.getPlayer(), message);
                        return;
                    }

                    shop.purge(e.getPlayer(), true);
                    return;
                }

                ItemStack itemInHand;
                if (isOffhandVersion) itemInHand = e.getPlayer().getInventory().getItemInMainHand();
                else itemInHand = e.getPlayer().getItemInHand();

                // ensure blocked nbt is not on the item
                if (getPluginInstance().getManager().hasBlockedNBT(handItem)) {
                    String message = getPluginInstance().getLangConfig().getString("set-item-blocked");
                    if (message != null && !message.equalsIgnoreCase(""))
                        getPluginInstance().getManager().sendMessage(e.getPlayer(), message);
                    return;
                }

                if (getPluginInstance().getManager().isSimilar(itemInHand, shop.getShopItem())) {
                    final int maxStock = shop.getMaxStock(), newTotalStock = (shop.getStock() + itemInHand.getAmount()),
                            remainderInHand = (newTotalStock - maxStock);

                    if (shop.isAdminShop() && shop.getStock() < 0) {
                        String message = getPluginInstance().getLangConfig().getString("shop-infinite-stock");
                        if (message != null && !message.equalsIgnoreCase(""))
                            getPluginInstance().getManager().sendMessage(e.getPlayer(), message);
                        return;
                    }

                    if (shop.getStock() >= maxStock) {
                        String message = getPluginInstance().getLangConfig().getString("shop-max-stock");
                        if (message != null && !message.equalsIgnoreCase(""))
                            getPluginInstance().getManager().sendMessage(e.getPlayer(),
                                    message.replace("{max}", getPluginInstance().getManager().formatNumber(maxStock, false)));
                        return;
                    }

                    ShopEditEvent shopEditEvent = new ShopEditEvent(e.getPlayer(), shop, EditType.QUICK_DEPOSIT,
                            (itemInHand.getAmount() - remainderInHand));
                    getPluginInstance().getServer().getPluginManager().callEvent(shopEditEvent);
                    if (shopEditEvent.isCancelled()) return;

                    String message = getPluginInstance().getLangConfig().getString("quick-deposit");
                    if (newTotalStock > maxStock && remainderInHand > 0) {
                        itemInHand.setAmount(remainderInHand);
                        shop.setStock(maxStock);
                        if (message != null && !message.equalsIgnoreCase(""))
                            getPluginInstance().getManager().sendMessage(e.getPlayer(), message.replace("{amount}",
                                    getPluginInstance().getManager().formatNumber((itemInHand.getAmount() - remainderInHand), false)));
                    } else {
                        if (message != null && !message.equalsIgnoreCase(""))
                            getPluginInstance().getManager().sendMessage(e.getPlayer(), message.replace("{amount}",
                                    getPluginInstance().getManager().formatNumber(itemInHand.getAmount(), false)));
                        shop.setStock(newTotalStock);
                        if (remainderInHand <= 0) {
                            if (isOffhandVersion) e.getPlayer().getInventory().setItemInMainHand(null);
                            else e.getPlayer().setItemInHand(null);
                        } else itemInHand.setAmount(remainderInHand);
                    }

                    getPluginInstance().runEventCommands("shop-deposit", e.getPlayer());
                    dataPack.resetEditData();
                    getPluginInstance().getInSightTask().refreshShop(shop);
                    return;
                }
            }

            if (shop.getShopItem() == null && !emptyShopEdit) {
                String message = getPluginInstance().getLangConfig().getString("shop-invalid-item");
                if (message != null && !message.equalsIgnoreCase(""))
                    getPluginInstance().getManager().sendMessage(e.getPlayer(), message);
                return;
            }

            final boolean canEdit = shop.canEdit(e.getPlayer());
            if (canEdit) {
                if (getPluginInstance().getConfig().getBoolean("block-creative") && e.getPlayer().getGameMode() == GameMode.CREATIVE) {
                    String message = getPluginInstance().getLangConfig().getString("creative-blocked");
                    if (message != null && !message.equalsIgnoreCase(""))
                        getPluginInstance().getManager().sendMessage(e.getPlayer(), message);
                    return;
                }

                if (editPrevention) {
                    if (shop.getCurrentEditor() != null && !shop.getCurrentEditor().toString().equals(e.getPlayer().getUniqueId().toString())) {
                        if (getPluginInstance().getServer().getOfflinePlayer(shop.getCurrentEditor()).isOnline()) {
                            e.setCancelled(true);
                            String message = getPluginInstance().getLangConfig().getString("shop-under-edit");
                            if (message != null && !message.equalsIgnoreCase(""))
                                getPluginInstance().getManager().sendMessage(e.getPlayer(), message);
                            return;
                        }
                        shop.setCurrentEditor(null);
                    }
                }

                ShopEditEvent shopEditEvent = new ShopEditEvent(e.getPlayer(), shop, EditType.OPEN_EDIT_MENU);
                getPluginInstance().getServer().getPluginManager().callEvent(shopEditEvent);
                if (shopEditEvent.isCancelled()) return;

                if (editPrevention) shop.setCurrentEditor(e.getPlayer().getUniqueId());
                dataPack.setSelectedShop(shop);

                final Menu editMenu = getPluginInstance().getMenu("edit");
                if (editMenu != null)
                    editMenu.build(e.getPlayer());

                getPluginInstance().runEventCommands("shop-edit", e.getPlayer());
            } else {
                System.out.println("B");
                if (!shop.isAdminShop() && !getPluginInstance().getConfig().getBoolean("offline-mode")) {
                    Player player = getPluginInstance().getServer().getPlayer(shop.getOwnerUniqueId());
                    if (player == null || !player.isOnline()) {
                        String message = getPluginInstance().getLangConfig().getString("owner-offline");
                        if (message != null && !message.equalsIgnoreCase(""))
                            getPluginInstance().getManager().sendMessage(e.getPlayer(), message);
                        return;
                    }
                }

                if (editPrevention) {
                    if (shop.getCurrentEditor() != null && !shop.getCurrentEditor().toString().equals(e.getPlayer().getUniqueId().toString())) {
                        if (getPluginInstance().getServer().getOfflinePlayer(shop.getCurrentEditor()).isOnline()) {
                            e.setCancelled(true);
                            String message = getPluginInstance().getLangConfig().getString("shop-under-edit");
                            if (message != null && !message.equalsIgnoreCase(""))
                                getPluginInstance().getManager().sendMessage(e.getPlayer(), message);
                            return;
                        }
                        shop.setCurrentEditor(null);
                    }
                }

                if (emptyShopEdit && shop.getShopItem() == null) {
                    String message = getPluginInstance().getLangConfig().getString("empty-shop");
                    if (message != null && !message.equalsIgnoreCase(""))
                        getPluginInstance().getManager().sendMessage(e.getPlayer(), message);
                    return;
                }

                ShopTransactionEvent shopTransactionEvent = new ShopTransactionEvent(e.getPlayer(), shop);
                getPluginInstance().getServer().getPluginManager().callEvent(shopTransactionEvent);
                if (shopTransactionEvent.isCancelled()) return;

                dataPack.setSelectedShop(shop);

                final Menu editMenu = getPluginInstance().getMenu("transaction");
                if (editMenu != null) editMenu.build(e.getPlayer());

                getPluginInstance().runEventCommands("shop-open", e.getPlayer());
            }
        }
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
    public void onEdit(ShopEditEvent e) {
        getPluginInstance().getServer().getScheduler().runTaskAsynchronously(getPluginInstance(), () ->
        {
            StringBuilder data = new StringBuilder();
            switch (e.getEditType()) {
                case CLEAR_TRADE_ITEM:
                case CLEAR_SALE_ITEM: {
                    data.append("\n\n").append(e.getValue());
                    break;
                }
                case WITHDRAW_STOCK:
                case DEPOSIT_STOCK: {
                    data.append("\n\n").append(getPluginInstance().toString(e.getShop().getShopItem()));
                    break;
                }
                case ASSISTANT_REMOVE:
                case ASSISTANT_ADD: {
                    data.append(" (Assistant Id: ").append(e.getValue()).append(")");
                    break;
                }
                case CURRENCY_CHANGE: {
                    data.append(" (New Currency: ").append(e.getValue()).append(")");
                    break;
                }
                case SELL_PRICE:
                case BUY_PRICE:
                case GLOBAL_BUY_LIMIT:
                case GLOBAL_SELL_LIMIT:
                case PLAYER_BUY_LIMIT:
                case PLAYER_SELL_LIMIT:
                case DEPOSIT_BALANCE:
                case WITHDRAW_BALANCE:
                case QUICK_DEPOSIT:
                case QUICK_WITHDRAW:
                case STOCK_SET:
                case SHOP_ITEM_AMOUNT: {
                    data.append(" (Amount: ").append(e.getAmount()).append(")");
                    break;
                }
                case COMMAND_ADD:
                case COMMAND_REMOVE: {
                    data.append(" (Command: ").append(e.getValue()).append(")");
                    break;
                }
                case APPEARANCE_CHANGE: {
                    data.append(" (New Appearance: ").append(e.getValue()).append(")");
                    break;
                }
                case DESCRIPTION_CHANGE: {
                    data.append(" (New Description: ").append(e.getValue()).append(")");
                    break;
                }
                default: {return;}
            }

            getPluginInstance().writeToLog("[" + getPluginInstance().getDateFormat().format(new Date(System.currentTimeMillis())) + "] "
                    + e.getPlayer().getName() + " performed the \""
                    + WordUtils.capitalize(e.getEditType().name().toLowerCase().replace("_", " "))
                    + "\" action on the shop '" + e.getShop().getShopId().toString()
                    + "' at (World: " + e.getShop().getBaseLocation().getWorldName()
                    + " X: " + e.getShop().getBaseLocation().getX() + " Y: " + e.getShop().getBaseLocation().getY() + " Z: "
                    + e.getShop().getBaseLocation().getZ() + ")." + data);
        });
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
    public void onCreate(ShopCreationEvent e) {
        getPluginInstance().getServer().getScheduler().runTaskAsynchronously(getPluginInstance(), () ->
                getPluginInstance().writeToLog("[" + getPluginInstance().getDateFormat().format(new Date(System.currentTimeMillis())) + "] "
                        + e.getPlayer().getName() + " created a shop at (World: " + Objects.requireNonNull(e.getLocation().getWorld()).getName()
                        + " X: " + e.getLocation().getBlockX() + " Y: " + e.getLocation().getBlockY() + " Z: " + e.getLocation().getBlockZ() + ")."));
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
    public void onDelete(ShopDeletionEvent e) {
        getPluginInstance().getServer().getScheduler().runTaskAsynchronously(getPluginInstance(), () ->
                getPluginInstance().writeToLog("[" + getPluginInstance().getDateFormat().format(new Date(System.currentTimeMillis())) + "] "
                        + e.getPlayer().getName() + " a shop at (World: " + Objects.requireNonNull(e.getLocation().getWorld()).getName()
                        + " X: " + e.getLocation().getBlockX() + " Y: " + e.getLocation().getBlockY() + " Z: " + e.getLocation().getBlockZ() + ")."));
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
    public void onEconomyCall(EconomyCallEvent e) {
        if (!e.willSucceed()) return;
        StringBuilder stringBuilder = new StringBuilder();

        String value = (e.getShop() != null ? getPluginInstance().getEconomyHandler().format(e.getShop(), (e.getShop() != null ?
                e.getShop().getCurrencyType()
                : getPluginInstance().getEconomyHandler().getDefaultCurrency()), e.getAmount())
                : getPluginInstance().getEconomyHandler().format(null, getPluginInstance().getEconomyHandler().getDefaultCurrency(), e.getAmount()));

        stringBuilder.append("[").append(getPluginInstance().getDateFormat().format(new Date(System.currentTimeMillis()))).append("] ")
                .append("Action: ").append(e.getEconomyCallType().name()).append(" | Cost: ").append(value).append(" --- performed by ").append(e.getPlayer().getName());

        if (e.getShop() != null) {
            stringBuilder.append(" performed the shop '")
                    .append(e.getShop().getShopId().toString()).append("' (World: ").append(e.getShop().getBaseLocation().getWorldName())
                    .append(" X: ").append(e.getShop().getBaseLocation().getX()).append(" Y: ").append(e.getShop().getBaseLocation().getY())
                    .append(" Z: ").append(e.getShop().getBaseLocation().getZ()).append(")");

            if (e.getEconomyCallType() == EconomyCallType.BUY || e.getEconomyCallType() == EconomyCallType.SELL
                    || e.getEconomyCallType() == EconomyCallType.EDIT_ACTION) {
                final int unitCount = (int) (e.getRawAmount() / e.getShop().getSellPrice(true));
                stringBuilder.append(" (Estimated Unit Count: ").append(getPluginInstance().getManager().formatNumber(unitCount, false)).append(")");
            }
        }

        if (stringBuilder.length() > 0)
            getPluginInstance().getServer().getScheduler().runTaskAsynchronously(getPluginInstance(), () ->
                    getPluginInstance().writeToLog(stringBuilder.toString()));
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onEntityBlockChange(EntityChangeBlockEvent e) {
        if (e.getEntity().getType() == EntityType.FALLING_BLOCK) return;

        if (getPluginInstance().getManager().getShop(e.getBlock().getLocation()) != null)
            e.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onExplode(EntityExplodeEvent e) {
        if (getPluginInstance().getConfig().getBoolean("explosive-protection.check")) {
            e.blockList().stream().parallel().forEach(block -> {
                final Shop shop = getPluginInstance().getManager().getShop(block.getLocation());
                if (shop != null) shop.delete(true);
            }); return;
        }

        if (getPluginInstance().getConfig().getBoolean("explosive-protection.alternative-method")) {
            Optional<Block> blockSearch = e.blockList().stream().parallel().filter(block ->
                    (getPluginInstance().getManager().getShop(block.getLocation()) != null)).findAny();
            blockSearch.ifPresent(block -> e.setCancelled(true));
        } else e.blockList().removeIf(block -> (getPluginInstance().getManager().getShop(block.getLocation()) != null));
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPistonExtend(BlockPistonExtendEvent e) {
        if (!getPluginInstance().getConfig().getBoolean("piston-protection.check")) return;
        handlePistons(e, e.getBlocks());
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPistonRetract(BlockPistonRetractEvent e) {
        if (!getPluginInstance().getConfig().getBoolean("piston-protection.check")) return;
        handlePistons(e, e.getBlocks());
    }

    private void handlePistons(@NotNull BlockPistonEvent e, @NotNull List<Block> blocks) {
        final boolean altPistonCheck = getPluginInstance().getConfig().getBoolean("piston-protection.alternative-method");
        for (Block block : blocks) {
            Shop shop = getPluginInstance().getManager().getShop(block.getLocation());
            if (shop == null) continue;

            if (altPistonCheck) {
                final boolean yDiff = (e.getBlock().getY() != shop.getBaseLocation().getY()),
                        xDiff = (e.getBlock().getX() != shop.getBaseLocation().getX()),
                        zDiff = (e.getBlock().getZ() != shop.getBaseLocation().getZ());

                final double changeInX = (Math.max(e.getBlock().getX(), shop.getBaseLocation().getX()) - Math.min(e.getBlock().getX(),
                        shop.getBaseLocation().getX())),
                        changeInY = (Math.max(e.getBlock().getY(), shop.getBaseLocation().getY()) - Math.min(e.getBlock().getY(),
                                shop.getBaseLocation().getY())),
                        changeInZ = (Math.max(e.getBlock().getZ(), shop.getBaseLocation().getZ()) - Math.min(e.getBlock().getZ(),
                                shop.getBaseLocation().getZ()));

                if ((!xDiff && zDiff && changeInZ < 12) || (!zDiff && xDiff && changeInX < 12) || (yDiff && changeInY < 12 && !xDiff && !zDiff)) {
                    e.setCancelled(true);
                    break;
                }
            } else if (shop.getBaseLocation().isSame(block.getLocation())) {

                e.setCancelled(true);
                break;
            }
        }
    }

   /* @EventHandler(ignoreCancelled = true)
    public void onHopper(InventoryMoveItemEvent e) {
        if (e.getDestination().getHolder() == null || !(e.getDestination().getHolder() instanceof Container)
                || !(e.getSource().getHolder() instanceof Container)) return;

        final Container container = (Container) e.getDestination().getHolder();
        final Container hopper = (Container) e.getDestination().getHolder();

        final Shop shop = getPluginInstance().getManager().getShop(container.getLocation());
        if (shop != null) {
            e.setCancelled(true);
            hopper.getBlock().breakNaturally();
        }
    }*/

    @EventHandler
    public void onCraft(CraftItemEvent e) {
        if (!(e.getWhoClicked() instanceof Player) || !getPluginInstance().getConfig().getBoolean("shop-creation-item.craftable") || e.getCurrentItem() == null)
            return;

        final Player player = (Player) e.getWhoClicked();
        if (e.getCurrentItem().getItemMeta() != null && e.getCurrentItem().getItemMeta().hasDisplayName()) {
            final String creationName = getPluginInstance().getConfig().getString("shop-creation-item.display-name");
            if (creationName != null && !creationName.isEmpty() && e.getCurrentItem().getItemMeta().getDisplayName().equals(creationName)) {
                if (!player.hasPermission("displayshops.craft")) {
                    e.setCancelled(true);
                    String message = getPluginInstance().getLangConfig().getString("no-permission");
                    if (message != null && !message.equalsIgnoreCase(""))
                        getPluginInstance().getManager().sendMessage(player, message);
                    return;
                }

                e.getInventory().setItem(e.getSlot(), getPluginInstance().getManager().buildShopCreationItem(player, 1));
            }
        }
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        if (!getPluginInstance().getManager().getDataPackMap().containsKey(e.getPlayer().getUniqueId()))
            getPluginInstance().getServer().getScheduler().runTaskAsynchronously(getPluginInstance(), () -> {
                DataPack dataPack = getPluginInstance().getManager().loadDataPack(e.getPlayer());
                dataPack.resetEditData();
            });
        else getPluginInstance().getManager().getDataPack(e.getPlayer()).resetEditData();
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        final DataPack dataPack = getPluginInstance().getManager().getDataPack(e.getPlayer());
        getPluginInstance().getManager().saveDataPack(null, e.getPlayer().getUniqueId(), dataPack, true, true);
        dataPack.resetEditData();
        getPluginInstance().getDisplayPacketMap().remove(e.getPlayer().getUniqueId());
        getPluginInstance().getShopMemory().remove(e.getPlayer().getUniqueId());
        getPluginInstance().getTeleportingPlayers().remove(e.getPlayer().getUniqueId());
    }

    @EventHandler
    public void onMove(PlayerMoveEvent e) {
        if (e.getTo() != null && (e.getTo().getBlockX() != e.getFrom().getBlockX() || e.getTo().getBlockY() != e.getFrom().getBlockY()
                || e.getTo().getBlockZ() != e.getFrom().getBlockZ()))
            if (getPluginInstance().getTeleportingPlayers().contains(e.getPlayer().getUniqueId())) {
                getPluginInstance().getTeleportingPlayers().remove(e.getPlayer().getUniqueId());
                String message = getPluginInstance().getLangConfig().getString("shop-visit-fail");
                if (message != null && !message.equalsIgnoreCase(""))
                    getPluginInstance().getManager().sendMessage(e.getPlayer(), message);
            }
    }

    @EventHandler
    public void onDamage(EntityDamageByEntityEvent e) {
        if (e.getEntity() instanceof Player) {
            Player victim = (Player) e.getEntity();
            if (getPluginInstance().getTeleportingPlayers().contains(victim.getUniqueId())) {
                getPluginInstance().getTeleportingPlayers().remove(victim.getUniqueId());
                String message = getPluginInstance().getLangConfig().getString("shop-visit-fail");
                if (message != null && !message.equalsIgnoreCase(""))
                    getPluginInstance().getManager().sendMessage(victim, message);
            }
        }

        if (e.getDamager() instanceof Player) {
            Player attacker = (Player) e.getDamager();
            if (getPluginInstance().getTeleportingPlayers().contains(attacker.getUniqueId())) {
                getPluginInstance().getTeleportingPlayers().remove(attacker.getUniqueId());
                String message = getPluginInstance().getLangConfig().getString("shop-visit-fail");
                if (message != null && !message.equalsIgnoreCase(""))
                    getPluginInstance().getManager().sendMessage(attacker, message);
            }
        } else if (e.getEntity() instanceof Projectile) {
            Projectile projectile = (Projectile) e.getDamager();
            if (projectile.getShooter() instanceof Player) {
                Player attacker = (Player) projectile.getShooter();
                if (getPluginInstance().getTeleportingPlayers().contains(attacker.getUniqueId())) {
                    getPluginInstance().getTeleportingPlayers().remove(attacker.getUniqueId());
                    String message = getPluginInstance().getLangConfig().getString("shop-visit-fail");
                    if (message != null && !message.equalsIgnoreCase(""))
                        getPluginInstance().getManager().sendMessage(attacker, message);
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onRespawn(PlayerRespawnEvent e) {
        if (getPluginInstance().getTeleportingPlayers().contains(e.getPlayer().getUniqueId())) {
            getPluginInstance().getTeleportingPlayers().remove(e.getPlayer().getUniqueId());
            String message = getPluginInstance().getLangConfig().getString("shop-visit-fail");
            if (message != null && !message.equalsIgnoreCase(""))
                getPluginInstance().getManager().sendMessage(e.getPlayer(), message);
        }

        getPluginInstance().getServer().getScheduler().runTaskAsynchronously(getPluginInstance(), () -> {
            getPluginInstance().clearDisplayPackets(e.getPlayer());
            getPluginInstance().killCurrentShopPacket(e.getPlayer());
        });
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onTeleport(PlayerTeleportEvent e) {
        if (getPluginInstance().getTeleportingPlayers().contains(e.getPlayer().getUniqueId())) {
            getPluginInstance().getTeleportingPlayers().remove(e.getPlayer().getUniqueId());
            String message = getPluginInstance().getLangConfig().getString("shop-visit-fail");
            if (message != null && !message.equalsIgnoreCase(""))
                getPluginInstance().getManager().sendMessage(e.getPlayer(), message);
        }

        getPluginInstance().getInSightTask().refreshShops(e.getPlayer());
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onWorldChange(PlayerChangedWorldEvent e) {
        if (getPluginInstance().getTeleportingPlayers().contains(e.getPlayer().getUniqueId())) {
            getPluginInstance().getTeleportingPlayers().remove(e.getPlayer().getUniqueId());
            String message = getPluginInstance().getLangConfig().getString("shop-visit-fail");
            if (message != null && !message.equalsIgnoreCase(""))
                getPluginInstance().getManager().sendMessage(e.getPlayer(), message);
        }

        getPluginInstance().getInSightTask().refreshShops(e.getPlayer());
    }

    // helper methods
    private boolean isTooClose(@NotNull Player player, @NotNull Block block, @NotNull ItemStack itemStack, boolean removeItem) {
        if (getPluginInstance().getManager().isTooClose(block.getLocation())) {
            String message = getPluginInstance().getLangConfig().getString("too-close");
            if (message != null && !message.equalsIgnoreCase(""))
                getPluginInstance().getManager().sendMessage(player, message);
            return true;
        }

        if (getPluginInstance().getManager().exceededShopLimit(player)) {
            String message = getPluginInstance().getLangConfig().getString("max-shops");
            if (message != null && !message.equalsIgnoreCase(""))
                getPluginInstance().getManager().sendMessage(player, message);
            return true;
        }

        if (removeItem) {
            if (itemStack.getAmount() > 1) itemStack.setAmount(itemStack.getAmount() - 1);
            else getPluginInstance().getManager().removeItem(player.getInventory(), itemStack, 1);
        }

        return false;
    }

    private void setShopItem(@NotNull Player player, @NotNull ItemStack handItem, @NotNull Shop shop) {
        ShopItemSetEvent shopItemSetEvent = new ShopItemSetEvent(player, shop, ItemType.SHOP, handItem);
        getPluginInstance().getServer().getPluginManager().callEvent(shopItemSetEvent);
        if (shopItemSetEvent.isCancelled()) return;

        if (handItem.getType() == Material.AIR) {
            String message = getPluginInstance().getLangConfig().getString("set-item-invalid");
            if (message != null && !message.equalsIgnoreCase(""))
                getPluginInstance().getManager().sendMessage(player, message);
            return;
        }

        ItemStack handItemClone = handItem.clone();

        boolean forceSingleStack = getPluginInstance().getConfig().getBoolean("force-single-stack");
        if (forceSingleStack) handItemClone.setAmount(1);

        if (isBlockedItem(handItem)) {
            String message = getPluginInstance().getLangConfig().getString("set-item-blocked");
            if (message != null && !message.equalsIgnoreCase(""))
                getPluginInstance().getManager().sendMessage(player, message);
            return;
        }

        if (getPluginInstance().getBlockedItemId(handItem) >= 0) {
            String message = getPluginInstance().getLangConfig().getString("set-item-blocked");
            if (message != null && !message.equalsIgnoreCase(""))
                getPluginInstance().getManager().sendMessage(player, message);
            return;
        }

        shop.updateBaseBlockDirection(player);
        shop.setShopItem(handItemClone);
        shop.setShopItemAmount(shop.getShopItem().getAmount());

        final double minBuyPrice = getPluginInstance().getManager().getMaterialMinPrice(handItem, true),
                maxBuyPrice = getPluginInstance().getManager().getMaterialMaxPrice(handItem, true),
                minSellPrice = getPluginInstance().getManager().getMaterialMinPrice(handItem, false),
                maxSellPrice = getPluginInstance().getManager().getMaterialMaxPrice(handItem, false),
                buyPrice = shop.getBuyPrice(false), sellPrice = shop.getSellPrice(false);

        if (buyPrice < minBuyPrice) shop.setBuyPrice(minBuyPrice);
        else if (buyPrice > maxBuyPrice) shop.setBuyPrice(maxBuyPrice);

        if (sellPrice < minSellPrice) shop.setSellPrice(minSellPrice);
        else if (sellPrice > maxSellPrice) shop.setSellPrice(maxSellPrice);

        if (shop.getStock() >= 0) shop.setStock(shop.getShopItem().getAmount());

        if (forceSingleStack && handItem.getAmount() > 1) {
            handItem.setAmount(handItem.getAmount() - 1);
            if (Ref.isNewerThan(8))
                player.getInventory().setItemInMainHand(handItem);
            else player.setItemInHand(handItem);
        } else {
            if (Ref.isNewerThan(8))
                player.getInventory().setItemInMainHand(null);
            else player.setItemInHand(null);
        }

        String description = "";

        if (getPluginInstance().getConfig().getBoolean("enchantment-description-set") && shop.getShopItem() != null
                && (!shop.getShopItem().getEnchantments().isEmpty() || shop.getShopItem().getItemMeta() instanceof EnchantmentStorageMeta))
            description = description + getPluginInstance().getManager().getEnchantmentLine(shop.getShopItem());
        else if (getPluginInstance().getConfig().getBoolean("potion-description-set") && shop.getShopItem() != null
                && shop.getShopItem().getItemMeta() instanceof PotionMeta)
            description = description + getPluginInstance().getManager().getPotionLine(shop.getShopItem());

        shop.setDescription(description);

        final String message = getPluginInstance().getLangConfig().getString("shop-item-set");
        if (message != null && !message.equalsIgnoreCase(""))
            getPluginInstance().getManager().sendMessage(player, message);


        getPluginInstance().getInSightTask().refreshShop(shop);
        getPluginInstance().runEventCommands("shop-item-set", player);
    }

    public boolean isBlockedItem(ItemStack itemStack) {
        List<String> materialNames = getPluginInstance().getConfig().getStringList("blocked-item-list");
        for (int i = -1; ++i < materialNames.size(); ) {
            String materialLine = materialNames.get(i);
            if (materialLine.contains(":")) {
                String[] splitMaterialLine = materialLine.split(":");
                if (splitMaterialLine[0].replace(" ", "_").replace("-", "_").equalsIgnoreCase(itemStack.getType().name()) && splitMaterialLine[1].equalsIgnoreCase(String.valueOf(itemStack.getDurability())))
                    return true;
            } else if (materialLine.replace(" ", "_").replace("-", "_").equalsIgnoreCase(itemStack.getType().name()))
                return true;
        }

        return false;
    }

    private void setBlock(Block block, Material material, BlockFace blockFace) {
        block.setType(material);

        org.bukkit.block.data.BlockData blockData = block.getBlockData();
        if (blockData instanceof Directional) {
            ((Directional) blockData).setFacing(blockFace);
            block.setBlockData(blockData);
        }

        if (blockData instanceof Orientable) {
            ((Orientable) blockData).setAxis(Axis.valueOf(convertBlockFaceToAxis(blockFace)));
            block.setBlockData(blockData);
        }

        if (blockData instanceof Rotatable) {
            ((Rotatable) blockData).setRotation(blockFace);
            block.setBlockData(blockData);
        }
    }

    private String convertBlockFaceToAxis(BlockFace face) {
        switch (face) {
            case NORTH:
            case SOUTH:
                return "Z";
            case UP:
            case DOWN:
                return "Y";
            case EAST:
            case WEST:
            default:
                return "X";
        }
    }

    private byte oppositeDirectionByte(Direction direction) {
        for (int i = -1; ++i < Direction.values().length; )
            if (direction == Direction.values()[i]) return (byte) i;
        return 4;
    }

    public boolean isCreationItem(@NotNull ItemStack itemStack) {
        final String nbtResult = getPluginInstance().getNBT(itemStack, "DisplayShops");
        return (nbtResult != null && nbtResult.equals("Creation Item"));
    }

    public boolean shopCreationWorks(@NotNull Player player, @NotNull Block block, @NotNull ItemStack itemStack) {

        if (!isCreationItem(itemStack)) {
           /* if(getPluginInstance().isItemAdderInstalled()) {
                dev.lone.itemsadder.api.CustomBlock customBlock = dev.lone.itemsadder.api.CustomBlock.byItemStack(itemStack);
            }*/
            return false;
        }

        if (!player.hasPermission("displayshops.create")) {
            String message = getPluginInstance().getLangConfig().getString("no-permission");
            if (message != null && !message.equalsIgnoreCase(""))
                getPluginInstance().getManager().sendMessage(player, message);
            return true;
        }

        Block blockRelativeOne = block.getRelative(BlockFace.UP), blockRelativeTwo = blockRelativeOne.getRelative(BlockFace.UP);
        if (!blockRelativeOne.getType().name().contains("AIR") || !blockRelativeTwo.getType().name().contains("AIR")) {
            String message = getPluginInstance().getLangConfig().getString("relative-unsafe-world");
            if (message != null && !message.equalsIgnoreCase(""))
                getPluginInstance().getManager().sendMessage(player, message);
            return true;
        }

        if (getPluginInstance().getManager().isBlockedWorld(block.getWorld())) {
            String message = getPluginInstance().getLangConfig().getString("blocked-world");
            if (message != null && !message.equalsIgnoreCase(""))
                getPluginInstance().getManager().sendMessage(player, message);
            return true;
        }

        if (getPluginInstance().isTownyInstalled()) {
            final boolean shopPlotOnly = getPluginInstance().getConfig().getBoolean("towny-shop-plots-only");
            if (shopPlotOnly && (!com.palmergames.bukkit.towny.utils.ShopPlotUtil.isShopPlot(block.getLocation())
                    || !com.palmergames.bukkit.towny.utils.ShopPlotUtil.doesPlayerHaveAbilityToEditShopPlot(player, block.getLocation()))) {
                String message = this.getPluginInstance().getLangConfig().getString("towny-no-access");
                if (message != null && !message.equalsIgnoreCase(""))
                    getPluginInstance().getManager().sendMessage(player, message);
                return true;
            }
        }

        ShopCreationEvent shopCreationEvent = new ShopCreationEvent(player, block.getLocation());
        getPluginInstance().getServer().getPluginManager().callEvent(shopCreationEvent);
        if (shopCreationEvent.isCancelBlockPlaceEvent()) return true;
        if (shopCreationEvent.isCancelled()) return false;

        Shop shopCheck = getPluginInstance().getManager().getShop(block.getLocation());
        if (shopCheck != null || isTooClose(player, block, itemStack, false)) return true;

        if (getPluginInstance().getManager().isBlockedMaterial(block.getRelative(BlockFace.DOWN).getType())) {
            if (player.getInventory().firstEmpty() == -1)
                player.getWorld().dropItemNaturally(player.getLocation(), getPluginInstance().getManager().buildShopCreationItem(player, 1));
            else player.getInventory().addItem(getPluginInstance().getManager().buildShopCreationItem(player, 1));
            return true;
        }

        MarketRegion marketRegion = getPluginInstance().getManager().getMarketRegion(block.getLocation());
        if (marketRegion != null) {
            getPluginInstance().getServer().getScheduler().runTaskLater(getPluginInstance(), () -> {
                if (Ref.isOlderThan(11)) {
                    block.setType(Material.AIR);
                    block.setType(creationItem.getType());
                    block.setBlockData(getPluginInstance().getServer().createBlockData(creationItem.getType()));
                    setBlock(block, creationItem.getType(), (creationItem.getType().name().contains("SHULKER")
                            ? BlockFace.UP : BlockFace.valueOf(Direction.getYaw(player).name())));

                    final BlockState blockState = block.getRelative(BlockFace.UP).getState();
                    blockState.setType(Material.AIR);
                    blockState.update(true, false);
                } else {
                    block.setType(Material.AIR);
                    block.setType(creationItem.getType());

                    if (block instanceof Directional) try {
                        Method method = Block.class.getMethod("setData", Byte.class, Boolean.class);
                        method.invoke(block, oppositeDirectionByte(Direction.getYaw(player)), true);
                    } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException ignored) {}
                    else try {
                        Method method = Block.class.getMethod("setData", Byte.class, Boolean.class);
                        method.invoke(block, (byte) creationItem.getDurability(), true);
                    } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException ignored) {}

                    final BlockState blockState = block.getRelative(BlockFace.UP).getState();
                    blockState.setType(Material.AIR);
                    blockState.update(true, false);
                }
            }, 10);
        } else block.getRelative(BlockFace.UP).breakNaturally();

        getPluginInstance().getManager().createShop(player, block, 1, true, true);
        return false;
    }

    // getters & setters
    private DisplayShops getPluginInstance() {
        return pluginInstance;
    }

    private void setPluginInstance(DisplayShops pluginInstance) {
        this.pluginInstance = pluginInstance;
    }

}