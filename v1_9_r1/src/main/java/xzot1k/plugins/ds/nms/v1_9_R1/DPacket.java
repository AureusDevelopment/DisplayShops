/*
 * Copyright (c) 2021 XZot1K, All rights reserved.
 */

package xzot1k.plugins.ds.nms.v1_9_R1;

import net.minecraft.server.v1_9_R1.*;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.craftbukkit.v1_9_R1.CraftWorld;
import org.bukkit.craftbukkit.v1_9_R1.entity.CraftPlayer;
import org.bukkit.craftbukkit.v1_9_R1.inventory.CraftItemStack;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import xzot1k.plugins.ds.DisplayShops;
import xzot1k.plugins.ds.api.handlers.DisplayPacket;
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
        Double[] offsets = this.getPluginInstance().getManager().getBaseBlockOffsets(shop);
        double offsetX = offsets[0];
        double offsetY = offsets[1];
        double offsetZ = offsets[2];
        PlayerConnection playerConnection = ((CraftPlayer) player).getHandle().playerConnection;
        NBTTagCompound compoundTag = new NBTTagCompound();
        compoundTag.setBoolean("Marker", true);
        compoundTag.setBoolean("PersistenceRequired", true);
        compoundTag.setBoolean("NoGravity", true);
        compoundTag.setBoolean("Gravity", false);
        compoundTag.setBoolean("Invulnerable", true);
        if (!this.getPluginInstance().getConfig().getBoolean("hide-glass")) {
            double x = shop.getBaseLocation().getX() + offsetX;
            double y = shop.getBaseLocation().getY() + offsetY;
            double z = shop.getBaseLocation().getZ() + offsetZ;
            this.createStand(playerConnection, compoundTag, player.getWorld(), x, y, z, "", true);
        }
        ItemStack item = shop.getShopItem() != null ? shop.getShopItem().clone()
                : this.getPluginInstance().getConfig().getBoolean("empty-shop-item") ? new ItemStack(Material.BARRIER) : null;
        if (item != null) {
            if (this.getPluginInstance().getConfig().getBoolean("force-single-stack")) {
                item.setAmount(1);
            }
            net.minecraft.server.v1_9_R1.ItemStack itemStack = CraftItemStack.asNMSCopy(item);
            EntityItem entityItem = new EntityItem(((CraftWorld) player.getWorld()).getHandle(), shop.getBaseLocation().getX() + offsetX,
                    shop.getBaseLocation().getY() + 1.325 + offsetY, shop.getBaseLocation().getZ() + offsetZ, itemStack);
            this.getEntityIds().add(entityItem.getId());
            entityItem.setItemStack(itemStack);
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

        final boolean hidePriceLine = getPluginInstance().getConfig().getBoolean("price-disabled-hide");
        final int wordCount = getPluginInstance().getConfig().getInt("description-word-line-limit");
        double x = (shop.getBaseLocation().getX() + offsetX), y = (shop.getBaseLocation().getY() + (1.9 + offsetY)), z = (shop.getBaseLocation().getZ() + offsetZ);
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

                List<String> descriptionLines = getPluginInstance().getManager().wrapString(shop.getDescription(), wordCount);
                Collections.reverse(descriptionLines);
                for (String descriptionLine : descriptionLines) {
                    createStand(playerConnection, compoundTag, player.getWorld(), x, y, z, (prefix + descriptionLine + suffix), false);
                    y += 0.3;
                }

                continue;
            }

            createStand(playerConnection, compoundTag, player.getWorld(), x, y, z, getPluginInstance().getManager().applyShopBasedPlaceholders(line, shop), false);
            y += 0.3;
        }
    }

    private void createStand(@NotNull PlayerConnection playerConnection, @NotNull NBTTagCompound compoundTag, @NotNull World world,
                             double x, double y, double z, String name, boolean glassHead) {
        EntityArmorStand stand = new EntityArmorStand(((CraftWorld) world).getHandle(), x, y, z);
        this.getEntityIds().add(stand.getId());
        stand.setCustomNameVisible(!glassHead);
        stand.setPositionRotation(0.0, 0.0, 0.0, 0.0f, 0.0f);
        stand.setHeadPose(new Vector3f(0.0f, 0.0f, 0.0f));
        stand.setLocation(x, y, z, 0.0f, 0.0f);
        stand.setMarker(true);
        stand.setGravity(true);
        stand.setInvisible(true);
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
            PacketPlayOutEntityEquipment packet = new PacketPlayOutEntityEquipment(stand.getId(),
                    EnumItemSlot.HEAD, CraftItemStack.asNMSCopy(new org.bukkit.inventory.ItemStack(Material.GLASS)));
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

