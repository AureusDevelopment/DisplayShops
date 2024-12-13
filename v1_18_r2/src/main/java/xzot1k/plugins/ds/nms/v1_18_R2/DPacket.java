/*
 * Copyright (c) 2021 XZot1K, All rights reserved.
 */

package xzot1k.plugins.ds.nms.v1_18_R2;

import com.mojang.datafixers.util.Pair;
import io.netty.buffer.Unpooled;
import net.md_5.bungee.api.ChatColor;
import net.minecraft.network.PacketDataSerializer;
import net.minecraft.network.chat.IChatBaseComponent;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.*;
import net.minecraft.network.syncher.DataWatcherRegistry;
import net.minecraft.network.syncher.DataWatcherSerializer;
import net.minecraft.server.network.PlayerConnection;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EnumItemSlot;
import org.bukkit.Material;
import org.bukkit.craftbukkit.v1_18_R2.entity.CraftPlayer;
import org.bukkit.craftbukkit.v1_18_R2.inventory.CraftItemStack;
import org.bukkit.craftbukkit.v1_18_R2.util.CraftChatMessage;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import xzot1k.plugins.ds.DisplayShops;
import xzot1k.plugins.ds.api.handlers.DisplayPacket;
import xzot1k.plugins.ds.api.objects.Appearance;
import xzot1k.plugins.ds.api.objects.Shop;

import java.lang.reflect.Field;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

public class DPacket implements DisplayPacket {

    private static final AtomicInteger ENTITY_ID_COUNTER_FIELD = getAI();
    private static final Supplier<Integer> idGenerator = setGenerator();

    private static AtomicInteger getAI() {
        try {
            for (Field field : Entity.class.getDeclaredFields()) {
                if (field.getType() == AtomicInteger.class) {
                    field.setAccessible(true);
                    return ((AtomicInteger) field.get(null));
                }
            }
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
        return new AtomicInteger();
    }

    private static Supplier<Integer> setGenerator() {
        return Objects.requireNonNull(ENTITY_ID_COUNTER_FIELD)::incrementAndGet;
    }

    private final Collection<Integer> entityIds = new ArrayList<>();
    private DisplayShops pluginInstance;
    private net.minecraft.world.item.ItemStack itemStack;

    public DPacket(DisplayShops pluginInstance, Player player, Shop shop, boolean showHolograms) {
        if (player == null || !player.isOnline() || shop == null) return;
        setPluginInstance(pluginInstance);

        final PlayerConnection playerConnection = getPlayerConnection(player);
        if (playerConnection == null) return;

        Appearance appearance = Appearance.getAppearance(shop.getAppearanceId());
        if (appearance == null) return;

        final double[] offsets = appearance.getOffset();
        final double offsetX = offsets[0], offsetY = offsets[1], offsetZ = offsets[2];

        double x = (shop.getBaseLocation().getX() + 0.5 + offsetX),
                y = (shop.getBaseLocation().getY() - 0.3 + offsetY),
                z = (shop.getBaseLocation().getZ() + 0.5 + offsetZ);

        if (!getPluginInstance().getConfig().getBoolean("hide-glass")) {
            createStand(playerConnection, x, y, z, "", true);
        }

        ItemStack item = (shop.getShopItem() != null) ? shop.getShopItem().clone() : (getPluginInstance().getConfig().getBoolean("empty-shop-item")
                ? new ItemStack(Material.BARRIER) : null);
        if (item != null) {
            if (getPluginInstance().getConfig().getBoolean("force-single-stack")) item.setAmount(1);

            if (item.getType() != Material.AIR) {
                itemStack = CraftItemStack.asNMSCopy(item);
                if (itemStack != null) {
                    //<editor-fold desc="Item Packet">
                    final int id = idGenerator.get();
                    getEntityIds().add(id);

                    PacketDataSerializer pds = buildSerializer(id, true, x, (y + 1.325) + offsetY, z);

                    final PacketPlayOutSpawnEntity itemPacket = new PacketPlayOutSpawnEntity(pds);
                    sendPacket(playerConnection, itemPacket);

                    final DataWatcherSerializer<net.minecraft.world.item.ItemStack> ITEM_STACK_SERIALIZER = DataWatcherRegistry.g;
                    final DataWatcherSerializer<Byte> BYTE_SERIALIZER = DataWatcherRegistry.a;

                    PacketDataSerializer metaData = new PacketDataSerializer(Unpooled.buffer());
                    metaData.d(id);
                    metaData.writeByte(8); // key index

                    int serializerTypeID = DataWatcherRegistry.b(ITEM_STACK_SERIALIZER);
                    if (serializerTypeID < 0) return;

                    metaData.d(serializerTypeID); // key serializer type id
                    ITEM_STACK_SERIALIZER.a(metaData, itemStack);
                    metaData.d(0xFF);

                    PacketPlayOutEntityMetadata md = new PacketPlayOutEntityMetadata(metaData);
                    sendPacket(playerConnection, md);
                    //</editor-fold>

                    //<editor-fold desc="Vehicle Mount Packets">
                    final int vehicleId = idGenerator.get();
                    getEntityIds().add(vehicleId);
                    PacketDataSerializer vehicleData = buildSerializer(vehicleId, false, x, (y + 1.325) + offsetY, z);
                    final PacketPlayOutSpawnEntityLiving vehiclePacket = new PacketPlayOutSpawnEntityLiving(vehicleData);
                    sendPacket(playerConnection, vehiclePacket);

                    PacketDataSerializer vehiclePDS = new PacketDataSerializer(Unpooled.buffer());
                    vehiclePDS.d(vehicleId);

                    // invisibility
                    vehiclePDS.writeByte(0); // key index
                    serializerTypeID = DataWatcherRegistry.b(BYTE_SERIALIZER);
                    if (serializerTypeID < 0) return;

                    vehiclePDS.d(serializerTypeID); // key serializer type id
                    BYTE_SERIALIZER.a(vehiclePDS, (byte) 0x20);

                    // small, no gravity, no base-plate marker, etc.
                    vehiclePDS.writeByte(15); // key index
                    serializerTypeID = DataWatcherRegistry.b(BYTE_SERIALIZER);
                    if (serializerTypeID < 0) return;

                    vehiclePDS.d(serializerTypeID); // key serializer type id
                    BYTE_SERIALIZER.a(vehiclePDS, (byte) (0x01 | 0x02 | 0x08 | 0x10));
                    vehiclePDS.d(0xFF);

                    PacketPlayOutEntityMetadata vehicleMD = new PacketPlayOutEntityMetadata(vehiclePDS);
                    sendPacket(playerConnection, vehicleMD);


                    PacketDataSerializer mountData = new PacketDataSerializer(Unpooled.buffer());
                    mountData.d(vehicleId);
                    mountData.d(1);
                    mountData.d(id);

                    PacketPlayOutMount mountPacket = new PacketPlayOutMount(mountData);
                    sendPacket(playerConnection, mountPacket);
                    //</editor-fold>
                }
            }
        }

        if (!showHolograms) return;

        List<String> hologramFormat;
        if (shop.getShopItem() != null) {
            if (shop.getOwnerUniqueId() == null)
                hologramFormat = getPluginInstance().getConfig().getStringList("admin-shop-format");
            else hologramFormat = getPluginInstance().getConfig().getStringList("valid-item-format");
        } else {
            if (shop.getOwnerUniqueId() == null)
                hologramFormat = getPluginInstance().getConfig().getStringList("admin-invalid-item-format");
            else hologramFormat = getPluginInstance().getConfig().getStringList("invalid-item-format");
        }

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
                    createStand(playerConnection, x, y, z, (prefix + descriptionLine + suffix), false);
                    y += 0.3;
                }
                continue;
            }

            createStand(playerConnection, x, y, z, getPluginInstance().getManager().applyShopBasedPlaceholders(line, shop), false);
            y += 0.3;
        }
    }

    private PlayerConnection getPlayerConnection(@NotNull Player player) {
        return ((CraftPlayer) player).getHandle().b;
    }

    private PacketDataSerializer buildSerializer(int id, boolean isItem, double x, double y, double z) {
        PacketDataSerializer pds = new PacketDataSerializer(Unpooled.buffer());

        pds.d(id);
        pds.a(UUID.randomUUID());
        pds.d(isItem ? 41 : 1);
        // Position
        pds.writeDouble(x);
        pds.writeDouble(y);
        pds.writeDouble(z);

        // Rotation
        pds.writeByte(0);
        pds.writeByte(0);

        // Object data (item is 44, armor stand is 2, and slime is 83)
        pds.writeInt(isItem ? 1 : 0);

        // Velocity
        pds.writeShort(0);
        pds.writeShort(0);
        pds.writeShort(0);

        return pds;
    }

    private void createStand(@NotNull PlayerConnection playerConnection, double x, double y, double z, @NotNull String name, boolean glassHead) {
        final int id = idGenerator.get();
        getEntityIds().add(id);

        final PacketDataSerializer pds = buildSerializer(id, false, x, y, z);
        final PacketPlayOutSpawnEntityLiving spawnPacket = new PacketPlayOutSpawnEntityLiving(pds);
        sendPacket(playerConnection, spawnPacket);

        final DataWatcherSerializer<Byte> BYTE_SERIALIZER = DataWatcherRegistry.a;
        final DataWatcherSerializer<Boolean> BOOLEAN_SERIALIZER = DataWatcherRegistry.i;
        final DataWatcherSerializer<Optional<IChatBaseComponent>> OPTIONAL_CHAT_COMPONENT_SERIALIZER = DataWatcherRegistry.f;

        if (glassHead) {
            itemStack = CraftItemStack.asNMSCopy(new ItemStack(Material.GLASS));
            if (itemStack != null) {
                List<Pair<EnumItemSlot, net.minecraft.world.item.ItemStack>> list = new ArrayList<>();
                list.add(new Pair<>(EnumItemSlot.f /*HEAD or HELMET*/, itemStack));
                PacketPlayOutEntityEquipment packet = new PacketPlayOutEntityEquipment(id, list);
                sendPacket(playerConnection, packet);
            }
        }

        PacketDataSerializer metaData = new PacketDataSerializer(Unpooled.buffer());
        metaData.d(id);

        // invisibility
        metaData.writeByte(0); // key index
        int serializerTypeID = DataWatcherRegistry.b(BYTE_SERIALIZER);
        if (serializerTypeID < 0) return;

        metaData.d(serializerTypeID); // key serializer type id
        BYTE_SERIALIZER.a(metaData, (byte) 0x20);

        // small, no gravity, no base-plate marker, etc.
        metaData.writeByte(15); // key index
        serializerTypeID = DataWatcherRegistry.b(BYTE_SERIALIZER);
        if (serializerTypeID < 0) return;

        metaData.d(serializerTypeID); // key serializer type id
        BYTE_SERIALIZER.a(metaData, (glassHead ? (byte) (0x02 | 0x08 | 0x10) : (byte) (0x01 | 0x02 | 0x08 | 0x10)));

        if (!name.isEmpty()) {
            name = name.substring(0, Math.min(name.length(), 5000));

            // set custom name
            metaData.writeByte(2); // key index
            serializerTypeID = DataWatcherRegistry.b(OPTIONAL_CHAT_COMPONENT_SERIALIZER);
            if (serializerTypeID < 0) return;

            metaData.d(serializerTypeID); // key serializer type id
            OPTIONAL_CHAT_COMPONENT_SERIALIZER.a(metaData, Optional.of(CraftChatMessage
                    .fromString(DisplayShops.getPluginInstance().getManager().color(name),
                            false, true)[0]));

            // set name visibility
            metaData.writeByte(3); // key index
            serializerTypeID = DataWatcherRegistry.b(BOOLEAN_SERIALIZER);
            if (serializerTypeID < 0) return;

            metaData.d(serializerTypeID); // key serializer type id
            BOOLEAN_SERIALIZER.a(metaData, true);
        }

        metaData.d(0xFF);

        PacketPlayOutEntityMetadata md = new PacketPlayOutEntityMetadata(metaData);
        sendPacket(playerConnection, md);
    }

    public void hide(@NotNull Player player) {
        if (getEntityIds() == null || getEntityIds().isEmpty()) return;

        for (int entityId : getEntityIds()) {
            final PacketPlayOutEntityDestroy standPacket = new PacketPlayOutEntityDestroy(entityId);
            sendPacket(getPlayerConnection(player), standPacket);
        }
    }

    public void sendPacket(@NotNull PlayerConnection playerConnection, @NotNull Packet<?> packet) {
        playerConnection.a /*NetworkManager*/.a(packet); // sendPacket()
    }

    private DisplayShops getPluginInstance() {
        return pluginInstance;
    }

    private void setPluginInstance(DisplayShops pluginInstance) {
        this.pluginInstance = pluginInstance;
    }

    public Collection<Integer> getEntityIds() {
        return entityIds;
    }

}