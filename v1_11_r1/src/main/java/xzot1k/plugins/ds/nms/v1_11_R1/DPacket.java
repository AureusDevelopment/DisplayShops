/*
 * Copyright (c) 2021 XZot1K, All rights reserved.
 */

package xzot1k.plugins.ds.nms.v1_11_R1;

import net.md_5.bungee.api.ChatColor;
import net.minecraft.server.v1_11_R1.*;
import org.bukkit.Material;
import org.bukkit.craftbukkit.v1_11_R1.CraftWorld;
import org.bukkit.craftbukkit.v1_11_R1.entity.CraftPlayer;
import org.bukkit.craftbukkit.v1_11_R1.inventory.CraftItemStack;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import xzot1k.plugins.ds.DisplayShops;
import xzot1k.plugins.ds.api.handlers.DisplayPacket;
import xzot1k.plugins.ds.api.objects.Appearance;
import xzot1k.plugins.ds.api.objects.Shop;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public class DPacket implements DisplayPacket {
    private DisplayShops pluginInstance;
    private final Collection<Integer> entityIds = new ArrayList<>();

    public DPacket(@NotNull DisplayShops pluginInstance, @NotNull Player player, @NotNull Shop shop, boolean showHolograms) {
        if (!player.isOnline() || player.getWorld() == null) return;

        this.setPluginInstance(pluginInstance);

        Appearance appearance = Appearance.getAppearance(shop.getAppearanceId());
        if (appearance == null) return;

        final double[] offsets = appearance.getOffset();
        final double offsetX = offsets[0], offsetY = offsets[1], offsetZ = offsets[2];

        double x = (shop.getBaseLocation().getX() + 0.5 + offsetX),
                y = (shop.getBaseLocation().getY() - 0.3 + offsetY),
                z = (shop.getBaseLocation().getZ() + 0.5 + offsetZ);

        PlayerConnection playerConnection = ((CraftPlayer) player).getHandle().playerConnection;
        NBTTagCompound compoundTag = new NBTTagCompound();
        compoundTag.setBoolean("Marker", true);
        compoundTag.setBoolean("PersistenceRequired", true);
        compoundTag.setBoolean("NoGravity", true);
        compoundTag.setBoolean("Gravity", false);
        compoundTag.setBoolean("Invulnerable", true);
        if (!this.getPluginInstance().getConfig().getBoolean("hide-glass")) {
            this.createStand(playerConnection, compoundTag, player.getWorld(), x, y, z, "", true);
        }
        ItemStack item = shop.getShopItem() != null ? shop.getShopItem().clone()
                : this.getPluginInstance().getConfig().getBoolean("empty-shop-item")
                ? new ItemStack(Material.getMaterial("BARRIER")) : null;
        if (item != null) {
            if (this.getPluginInstance().getConfig().getBoolean("force-single-stack")) {
                item.setAmount(1);
            }
            net.minecraft.server.v1_11_R1.ItemStack itemStack = CraftItemStack.asNMSCopy(item);
            EntityItem entityItem = new EntityItem(((CraftWorld) player.getWorld()).getHandle(), x, y + 1.325, z, itemStack);
            this.getEntityIds().add(entityItem.getId());
            entityItem.setItemStack(itemStack);
            entityItem.setInvulnerable(true);
            entityItem.c(compoundTag);
            entityItem.f(compoundTag);
            entityItem.motX = 0.0;
            entityItem.motY = 0.0;
            entityItem.motZ = 0.0;
            entityItem.pickupDelay = Integer.MAX_VALUE;
            int itemId = entityItem.getId();
            PacketPlayOutEntityDestroy standPacket = new PacketPlayOutEntityDestroy(itemId);
            playerConnection.sendPacket(standPacket);
            PacketPlayOutSpawnEntity itemPacket = new PacketPlayOutSpawnEntity(entityItem, 2, 1);
            PacketPlayOutEntityVelocity itemVelocityPacket = new PacketPlayOutEntityVelocity(entityItem);
            PacketPlayOutEntityMetadata data = new PacketPlayOutEntityMetadata(itemId, entityItem.getDataWatcher(), true);
            playerConnection.sendPacket(itemPacket);
            playerConnection.sendPacket(itemVelocityPacket);
            playerConnection.sendPacket(data);
        }

        if (!showHolograms) return;

        List<String> hologramFormat = shop.getShopItem() != null ? (shop.getOwnerUniqueId() == null
                ? this.getPluginInstance().getConfig().getStringList("admin-shop-format")
                : this.getPluginInstance().getConfig().getStringList("valid-item-format")) : (shop.getOwnerUniqueId() == null
                ? this.getPluginInstance().getConfig().getStringList("admin-invalid-item-format")
                : this.getPluginInstance().getConfig().getStringList("invalid-item-format"));

        final String colorCode = getPluginInstance().getConfig().getString("default-description-color");
        final boolean hidePriceLine = getPluginInstance().getConfig().getBoolean("price-disabled-hide");

        y = (y + 1.9);
        for (int i = hologramFormat.size(); --i >= 0; ) {
            String line = hologramFormat.get(i);

            if ((hidePriceLine && ((line.contains("buy-price") && shop.getBuyPrice(true) < 0)
                    || (line.contains("sell-price") && shop.getSellPrice(true) < 0)))
                    || ((line.contains("{description}") && (shop.getDescription() == null || shop.getDescription().equalsIgnoreCase("")))))
                continue;

            if (line.contains("{description}") && !(shop.getDescription() == null || shop.getDescription().equalsIgnoreCase(""))) {
                final String[] otherContents = line.split("\\{description}");
                final String prefix = (otherContents.length >= 1 ? otherContents[0] : ""),
                        suffix = (otherContents.length >= 2 ? otherContents[1] : "");

                List<String> descriptionLines = getPluginInstance().getManager().wrapString(shop.getDescription());
                Collections.reverse(descriptionLines);
                for (int j = -1; ++j < descriptionLines.size(); ) {
                    String descriptionLine = pluginInstance.getManager().color(descriptionLines.get(j));
                    descriptionLine = (descriptionLine.contains(ChatColor.COLOR_CHAR + "") ? descriptionLine : (pluginInstance.getManager().color(colorCode + descriptionLine)));
                    createStand(playerConnection, compoundTag, player.getWorld(), x, y, z, (prefix + descriptionLine + suffix), false);
                    y += 0.3;
                }
                continue;
            }

            createStand(playerConnection, compoundTag, player.getWorld(), x, y, z, getPluginInstance().getManager().applyShopBasedPlaceholders(line, shop), false);
            y += 0.3;
        }
    }

    private void createStand(@NotNull PlayerConnection playerConnection, @NotNull NBTTagCompound compoundTag, @NotNull org.bukkit.World world,
                             double x, double y, double z, @NotNull String name, boolean glassHead) {
        EntityArmorStand stand = new EntityArmorStand(((CraftWorld) world).getHandle(), x, y, z);
        this.getEntityIds().add(stand.getId());
        stand.setCustomNameVisible(!glassHead);
        stand.setPositionRotation(0.0, 0.0, 0.0, 0.0f, 0.0f);
        stand.setHeadPose(new Vector3f(0.0f, 0.0f, 0.0f));
        stand.setLocation(x, y, z, 0.0f, 0.0f);
        stand.setMarker(true);
        stand.setNoGravity(true);
        stand.setInvulnerable(true);
        stand.setInvisible(true);
        stand.collides = false;
        stand.c(compoundTag);
        stand.f(compoundTag);
        if (!glassHead) {
            stand.setSmall(true);
        }
        stand.getBukkitEntity().setCustomName(getPluginInstance().getManager().color(name));
        PacketPlayOutSpawnEntityLiving standPacket = new PacketPlayOutSpawnEntityLiving(stand);
        playerConnection.sendPacket(standPacket);
        PacketPlayOutEntityMetadata data = new PacketPlayOutEntityMetadata(stand.getId(), stand.getDataWatcher(), true);
        playerConnection.sendPacket(data);
        if (glassHead) {
            PacketPlayOutEntityEquipment packet = new PacketPlayOutEntityEquipment(stand.getId(), EnumItemSlot.HEAD, CraftItemStack.asNMSCopy(new ItemStack(Material.GLASS)));
            playerConnection.sendPacket(packet);
        }
    }

    @Override
    public void hide(@NotNull Player player) {
        PlayerConnection playerConnection = ((CraftPlayer) player).getHandle().playerConnection;
        for (int entityId : this.getEntityIds()) {
            PacketPlayOutEntityDestroy standPacket = new PacketPlayOutEntityDestroy(entityId);
            playerConnection.sendPacket(standPacket);
        }
    }

    private DisplayShops getPluginInstance() {
        return this.pluginInstance;
    }

    private void setPluginInstance(DisplayShops pluginInstance) {
        this.pluginInstance = pluginInstance;
    }

    @Override
    public Collection<Integer> getEntityIds() {
        return this.entityIds;
    }
}