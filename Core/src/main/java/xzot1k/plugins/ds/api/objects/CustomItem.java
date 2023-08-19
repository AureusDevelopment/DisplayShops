/*
 * Copyright (c) 2021 XZot1K, All rights reserved.
 */

package xzot1k.plugins.ds.api.objects;

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import xzot1k.plugins.ds.DisplayShops;

import java.lang.reflect.Field;
import java.util.*;

public class CustomItem {

    private final DisplayShops pluginInstance;
    boolean isNew, papiHere;
    private ItemStack itemStack;
    private Shop shop;
    private int unitCount, unitItemMaxStack;

    public CustomItem(DisplayShops pluginInstance, String materialName, int durability, int amount) {
        this.pluginInstance = pluginInstance;
        constructorHelper(materialName, durability, amount);
    }

    public CustomItem(DisplayShops pluginInstance, String materialName, int durability, int amount, Shop shop, int unitItemMaxStack, int unitCount) {
        this.pluginInstance = pluginInstance;
        this.shop = shop;
        this.unitCount = unitCount;
        this.unitItemMaxStack = unitItemMaxStack;
        constructorHelper(materialName, durability, amount);
    }

    public CustomItem(String materialName, int amount) {
        this.pluginInstance = DisplayShops.getPluginInstance();
        constructorHelper(materialName, 0, amount);
    }

    public CustomItem(String materialName, int durability, int amount) {
        this.pluginInstance = DisplayShops.getPluginInstance();
        constructorHelper(materialName, durability, amount);
    }

    public CustomItem(String materialName, int durability, int amount, Shop shop, int unitItemMaxStack, int unitCount) {
        this.pluginInstance = DisplayShops.getPluginInstance();
        this.shop = shop;
        this.unitCount = unitCount;
        this.unitItemMaxStack = unitItemMaxStack;
        constructorHelper(materialName, durability, amount);
    }

    public CustomItem(ItemStack itemStack, Shop shop, int unitItemMaxStack, int unitCount) {
        this.pluginInstance = DisplayShops.getPluginInstance();
        this.isNew = (Math.floor(getPluginInstance().getServerVersion()) > 1_12);
        this.papiHere = (getPluginInstance().getPapiHelper() != null);
        this.itemStack = itemStack;
        this.shop = shop;
        this.unitCount = unitCount;
        this.unitItemMaxStack = unitItemMaxStack;
    }

    private void constructorHelper(String materialName, int durability, int amount) {
        this.isNew = (Math.floor(getPluginInstance().getServerVersion()) > 1_12);
        this.papiHere = (getPluginInstance().getPapiHelper() != null);

        if (materialName == null || materialName.isEmpty()) {
            itemStack = new ItemStack(Material.STONE, amount);
            return;
        }

        String replacedMaterial = materialName.toUpperCase().replace(" ", "_").replace("-", "_");
        if (materialName.toUpperCase().startsWith("HEAD") && materialName.contains(":")) {
            final String[] materialArgs = replacedMaterial.split(":");
            if (getPluginInstance().getHeadDatabaseAPI() != null && !getPluginInstance().getManager().isNotNumeric(materialArgs[1])) {
                itemStack = getPluginInstance().getHeadDatabaseAPI().getItemHead(materialArgs[1]);
                itemStack.setAmount(amount);
            } else {
                Material mat = Material.getMaterial(isNew ? "PLAYER_HEAD" : "SKULL_ITEM");
                mat = (mat != null ? mat : Material.STONE);
                itemStack = new ItemStack(mat, amount, (isNew ? 0 : (short) 3));

                SkullMeta skullMeta = (SkullMeta) itemStack.getItemMeta();
                if (skullMeta != null && materialArgs[1] != null && !materialArgs[1].equalsIgnoreCase("")) {
                    if (isNew()) {
                        OfflinePlayer player = getPluginInstance().getServer().getOfflinePlayer(materialArgs[1]);
                        skullMeta.setOwningPlayer(player);
                    } else skullMeta.setOwner(materialArgs[1]);
                    itemStack.setItemMeta(skullMeta);
                }
            }
        } else if (materialName.toUpperCase().startsWith("TEXTURE") && materialName.contains(":")) {
            final String[] materialArgs = replacedMaterial.split(":");
            Material mat = Material.getMaterial(isNew ? "PLAYER_HEAD" : "SKULL_ITEM");
            mat = (mat != null ? mat : Material.STONE);
            itemStack = new ItemStack(mat, amount, (isNew ? 0 : (short) 3));

            SkullMeta skullMeta = (SkullMeta) itemStack.getItemMeta();
            if (skullMeta != null && materialArgs[1] != null && !materialArgs[1].equalsIgnoreCase("")) {

                GameProfile profile = new GameProfile(UUID.randomUUID(), "");
                profile.getProperties().put("textures", new Property("textures", materialArgs[1]));

                Field profileField;
                try {
                    profileField = skullMeta.getClass().getDeclaredField("profile");
                    profileField.setAccessible(true);
                    profileField.set(skullMeta, profile);
                } catch (IllegalArgumentException | IllegalAccessException | NoSuchFieldException | SecurityException e) {
                    e.printStackTrace();
                }

                itemStack.setItemMeta(skullMeta);
            }

        } else {
            String itemMat;
            if (materialName.contains(":")) {
                final String[] materialArgs = materialName.split(":");
                if (materialArgs.length > 2) itemMat = (materialArgs[0] + ":" + materialArgs[1]);
                else itemMat = materialArgs[0];
            } else itemMat = materialName;

            Material material = Material.getMaterial(itemMat);
            if (material == null) {
                if (getPluginInstance().isItemAdderInstalled()) {
                    if (dev.lone.itemsadder.api.CustomStack.isInRegistry(itemMat)) {
                        dev.lone.itemsadder.api.CustomStack customStack = dev.lone.itemsadder.api.CustomStack.getInstance(itemMat);
                        if (customStack != null) itemStack = customStack.getItemStack();
                    } else itemStack = new ItemStack(Material.STONE);
                    itemStack.setAmount(amount);
                }
            } else itemStack = new ItemStack(material, amount, (short) durability);
        }
    }

    public CustomItem setDisplayName(Player player, String displayName) {
        ItemMeta itemMeta = get().getItemMeta();
        if (itemMeta != null && displayName != null && !displayName.isEmpty()) {
            displayName = (papiHere && player != null) ? getPluginInstance().getPapiHelper().replace(player, displayName) : displayName;
            itemMeta.setDisplayName(getPluginInstance().getManager().color(displayName));
            get().setItemMeta(itemMeta);
        }
        return this;
    }

    public CustomItem setDisplayName(@Nullable Player player, @Nullable Shop shop, @NotNull String displayName, @Nullable String... extraPlaceHolders) {
        setDisplayName(player, replaceExtraPlaceholders(((shop != null)
                ? getPluginInstance().getManager().applyShopBasedPlaceholders(displayName, shop, unitCount, unitItemMaxStack)
                : displayName), extraPlaceHolders));
        return this;
    }

    public void setLore(Player player, @Nullable String[] extraPlaceHolders, String... lines) {
        ItemMeta itemMeta = get().getItemMeta();
        if (itemMeta == null) return;
        itemMeta.setLore(new ArrayList<String>() {{
            final boolean useVault = getPluginInstance().getConfig().getBoolean("use-vault");
            for (int i = -1; ++i < lines.length; ) {
                String line = papiHere ? getPluginInstance().getPapiHelper().replace(player, lines[i]) : lines[i];
                if (!line.contains("{no-vault}") || (!useVault && line.contains("{no-vault}"))) {
                    if (line.contains("{description}")) {
                        if (shop.getDescription() == null || shop.getDescription().isEmpty()) continue;

                        final int wordCount = getPluginInstance().getConfig().getInt("description-word-line-limit");
                        List<String> descriptionLines = getPluginInstance().getManager().wrapString(shop.getDescription(), wordCount);
                        Collections.reverse(descriptionLines);

                        for (int j = -1; ++j < descriptionLines.size(); ) {
                            final String descLine = descriptionLines.get(j);
                            add(getPluginInstance().getManager().color(descLine));
                        }

                        continue;
                    }

                    add(getPluginInstance().getManager().color(replaceExtraPlaceholders(((shop != null)
                            ? getPluginInstance().getManager().applyShopBasedPlaceholders(line, shop, unitCount, unitItemMaxStack)
                            : line), extraPlaceHolders)));
                }
            }
        }});
        get().setItemMeta(itemMeta);
    }

    public CustomItem setLore(Player player, List<String> lines, @Nullable String... extraPlaceHolders) {
        setLore(player, extraPlaceHolders, Arrays.copyOf(lines.toArray(), lines.size(), String[].class));
        return this;
    }

    private String replaceExtraPlaceholders(@NotNull String text, @Nullable String[] extraPlaceHolders) {
        if (extraPlaceHolders == null || extraPlaceHolders.length == 0) return text;
        String newText = text;
        for (int i = -1; ++i < extraPlaceHolders.length; ) {
            final String placeholderString = extraPlaceHolders[i];
            if (placeholderString == null || !placeholderString.contains(":")) continue;

            final String[] args = placeholderString.split(":");
            newText = newText.replace(args[0], args[1]);
        }
        return newText;
    }

    public CustomItem refreshPlaceholders(@NotNull Player player, @NotNull String menuName, String buttonAction) {
        final Menu menu = getPluginInstance().getMenu(menuName);
        if (menu == null) return this;

        final String name = menu.getConfiguration().getString("buttons." + buttonAction + ".name");
        if (name != null && !name.isEmpty()) setDisplayName(player, shop, name);

        final List<String> lore = menu.getConfiguration().getStringList("buttons." + buttonAction + ".lore");
        if (!lore.isEmpty()) setLore(player, lore);

        return this;
    }

    public CustomItem setEnchanted(boolean enchanted) {
        ItemMeta itemMeta = get().getItemMeta();
        if (itemMeta != null) {
            if (!enchanted) {
                itemMeta.getItemFlags().add(ItemFlag.HIDE_ENCHANTS);
                itemMeta.addEnchant(Enchantment.DURABILITY, 10, true);
            } else {
                itemMeta.getItemFlags().remove(ItemFlag.HIDE_ENCHANTS);
                itemMeta.removeEnchant(Enchantment.DURABILITY);
            }
        }

        return this;
    }

    public CustomItem setEnchantments(List<String> enchants) {
        ItemMeta itemMeta = get().getItemMeta();
        if (itemMeta != null && !enchants.isEmpty()) {
            for (int i = -1; ++i < enchants.size(); ) {
                final String line = enchants.get(i);
                if (line.contains(":")) {
                    final String[] enchantLine = line.split(":");
                    Enchantment enchantment = Enchantment.getByName(enchantLine[0]);
                    if (enchantment == null || getPluginInstance().getManager().isNotNumeric(enchantLine[1])) continue;
                    itemMeta.addEnchant(enchantment, Integer.parseInt(enchantLine[1]), true);
                } else {
                    Enchantment enchantment = Enchantment.getByName(line);
                    if (enchantment == null) continue;
                    itemMeta.addEnchant(enchantment, 0, true);
                }
                get().setItemMeta(itemMeta);
            }
        }
        return this;
    }

    public CustomItem setItemFlags(List<String> itemFlags) {
        ItemMeta itemMeta = get().getItemMeta();
        if (itemMeta != null && itemFlags != null && !itemFlags.isEmpty()) {
            String[] flagNames = {"HIDE_POTION_EFFECTS", "HIDE_DYE", "HIDE_ATTRIBUTES", "HIDE_UNBREAKABLE"};
            for (int i = -1; ++i < flagNames.length; ) {
                final String flagName = flagNames[i];
                try {
                    final ItemFlag flag = ItemFlag.valueOf(flagName);
                    itemMeta.addItemFlags(flag);
                } catch (Exception ignored) {}
            }
            get().setItemMeta(itemMeta);
        }
        return this;
    }

    public CustomItem setModelData(int modelData) {
        ItemMeta itemMeta = get().getItemMeta();
        if (itemMeta != null && modelData > 0 && isNew()) {
            itemMeta.setCustomModelData(modelData);
            get().setItemMeta(itemMeta);
        }
        return this;
    }

    private boolean isNew() {return isNew;}

    private DisplayShops getPluginInstance() {return pluginInstance;}

    public ItemStack get() {return itemStack;}

    private Shop getShop() {return shop;}
}
