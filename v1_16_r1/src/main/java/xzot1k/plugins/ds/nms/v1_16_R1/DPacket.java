/*
 * Copyright (c) 2021 XZot1K, All rights reserved.
 */

package xzot1k.plugins.ds.nms.v1_16_R1;

import com.mojang.datafixers.util.Pair;
import io.netty.buffer.Unpooled;
import net.md_5.bungee.api.ChatColor;
import net.minecraft.server.v1_16_R1.*;
import org.bukkit.Material;
import org.bukkit.craftbukkit.v1_16_R1.entity.CraftPlayer;
import org.bukkit.craftbukkit.v1_16_R1.inventory.CraftItemStack;
import org.bukkit.craftbukkit.v1_16_R1.util.CraftChatMessage;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import xzot1k.plugins.ds.DisplayShops;
import xzot1k.plugins.ds.api.handlers.DisplayPacket;
import xzot1k.plugins.ds.api.objects.Appearance;
import xzot1k.plugins.ds.api.objects.Shop;

import java.io.IOException;
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

    private net.minecraft.server.v1_16_R1.ItemStack itemStack;

    private final Collection<Integer> entityIds = new ArrayList<>();

    private DisplayShops pluginInstance;

    public DPacket(@NotNull DisplayShops pluginInstance, @NotNull Player player, @NotNull Shop shop, boolean showHolograms) {
        if (!player.isOnline()) return;

        this.setPluginInstance(pluginInstance);

        Appearance appearance = Appearance.getAppearance(shop.getAppearanceId());
        if (appearance == null) return;

        final double[] offsets = appearance.getOffset();
        final double offsetX = offsets[0], offsetY = offsets[1], offsetZ = offsets[2];

        double x = (shop.getBaseLocation().getX() + 0.5 + offsetX),
                y = (shop.getBaseLocation().getY() - 0.3 + offsetY),
                z = (shop.getBaseLocation().getZ() + 0.5 + offsetZ);

        PlayerConnection playerConnection = ((CraftPlayer) player).getHandle().playerConnection;
        if (!this.getPluginInstance().getConfig().getBoolean("hide-glass")) {
            this.createStand(playerConnection, x, y, z, "", true);
        }
        ItemStack item = shop.getShopItem() != null ? shop.getShopItem().clone()
                : this.getPluginInstance().getConfig().getBoolean("empty-shop-item")
                ? new ItemStack(Material.BARRIER) : null;
        if (item != null) {
            if (getPluginInstance().getConfig().getBoolean("force-single-stack")) item.setAmount(1);

            if (item.getType() != Material.AIR) {
                itemStack = CraftItemStack.asNMSCopy(item);
                if (itemStack != null) {
                    itemStack = CraftItemStack.asNMSCopy(item);
                    if (itemStack != null) {
                        //<editor-fold desc="Item Packet">
                        final int id = idGenerator.get();
                        getEntityIds().add(id);

                        PacketDataSerializer pds = buildSerializer(id, true, x, (y + 1.325) + offsetY, z);

                        final PacketPlayOutSpawnEntity itemPacket = new PacketPlayOutSpawnEntity();
                        try {
                            itemPacket.a(pds);
                        } catch (IOException e) {e.printStackTrace();}
                        sendPacket(playerConnection, itemPacket);

                        final DataWatcherSerializer<net.minecraft.server.v1_16_R1.ItemStack> ITEM_STACK_SERIALIZER = DataWatcherRegistry.g;
                        final DataWatcherSerializer<Byte> BYTE_SERIALIZER = DataWatcherRegistry.a;

                        PacketDataSerializer metaData = new PacketDataSerializer(Unpooled.buffer());
                        metaData.d(id);
                        metaData.writeByte(7); // key index

                        int serializerTypeID = DataWatcherRegistry.b(ITEM_STACK_SERIALIZER);
                        if (serializerTypeID < 0) return;

                        metaData.d(serializerTypeID); // key serializer type id
                        ITEM_STACK_SERIALIZER.a(metaData, itemStack);
                        metaData.d(0xFF);

                        PacketPlayOutEntityMetadata md = new PacketPlayOutEntityMetadata();
                        try {
                            md.a(metaData);
                        } catch (IOException e) {e.printStackTrace();}
                        sendPacket(playerConnection, md);
                        //</editor-fold>

                        //<editor-fold desc="Vehicle Mount Packets">
                        final int vehicleId = createStand(playerConnection, x, (y + 1.325), z, "", false);

                        PacketDataSerializer mountData = new PacketDataSerializer(Unpooled.buffer());
                        mountData.d(vehicleId);
                        mountData.d(1);
                        mountData.d(id);

                        PacketPlayOutMount mountPacket = new PacketPlayOutMount();
                        try {
                            mountPacket.a(mountData);
                        } catch (IOException e) {e.printStackTrace();}
                        sendPacket(playerConnection, mountPacket);
                        //</editor-fold>
                    }
                }
            }
        }
        if (!showHolograms) {
            return;
        }
        List<String> hologramFormat = shop.getShopItem() != null ? (shop.getOwnerUniqueId() == null
                ? this.getPluginInstance().getConfig().getStringList("admin-shop-format") :
                this.getPluginInstance().getConfig().getStringList("valid-item-format")) : (shop.getOwnerUniqueId() == null
                ? this.getPluginInstance().getConfig().getStringList("admin-invalid-item" +
                "-format") : this.getPluginInstance().getConfig().getStringList("invalid-item-format"));

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

    private PacketDataSerializer buildSerializer(int id, boolean isItem, double x, double y, double z) {
        PacketDataSerializer pds = new PacketDataSerializer(Unpooled.buffer());

        pds.d(id);
        pds.a(UUID.randomUUID());
        pds.d(isItem ? 37 : 1); // (item is 37, armor stand is 1, and slime is 75)

        // Position
        pds.writeDouble(x);
        pds.writeDouble(y);
        pds.writeDouble(z);

        // Rotation
        pds.writeByte(0);
        pds.writeByte(0);

        // Object data
        pds.writeInt(isItem ? 1 : 0);

        // Velocity
        pds.writeShort(0);
        pds.writeShort(0);
        pds.writeShort(0);

        return pds;
    }

    private int createStand(@NotNull PlayerConnection playerConnection, double x, double y, double z, @NotNull String name, boolean glassHead) {
        final int id = idGenerator.get();
        getEntityIds().add(id);

        final PacketDataSerializer pds = buildSerializer(id, false, x, y, z);
        final PacketPlayOutSpawnEntityLiving spawnPacket = new PacketPlayOutSpawnEntityLiving();
        try {
            spawnPacket.a(pds);
        } catch (IOException e) {e.printStackTrace();}
        sendPacket(playerConnection, spawnPacket);

        final DataWatcherSerializer<Byte> BYTE_SERIALIZER = DataWatcherRegistry.a;
        final DataWatcherSerializer<Boolean> BOOLEAN_SERIALIZER = DataWatcherRegistry.i;
        final DataWatcherSerializer<Optional<IChatBaseComponent>> OPTIONAL_CHAT_COMPONENT_SERIALIZER = DataWatcherRegistry.f;

        if (glassHead) {
            itemStack = CraftItemStack.asNMSCopy(new ItemStack(Material.GLASS));
            if (itemStack != null) {
                List<Pair<EnumItemSlot, net.minecraft.server.v1_16_R1.ItemStack>> list = new ArrayList<>();
                list.add(new Pair<>(EnumItemSlot.HEAD /*HEAD or HELMET*/, itemStack));
                PacketPlayOutEntityEquipment packet = new PacketPlayOutEntityEquipment(id, list);
                sendPacket(playerConnection, packet);
            }
        }

        PacketDataSerializer metaData = new PacketDataSerializer(Unpooled.buffer());
        metaData.d(id);

        // invisibility
        metaData.writeByte(0); // entity status index (BYTE_SERIALIZER)
        int serializerTypeID = DataWatcherRegistry.b(BYTE_SERIALIZER);
        if (serializerTypeID >= 0) {
            metaData.d(serializerTypeID);
            BYTE_SERIALIZER.a(metaData, (byte) 0x20);
        }

        // small, no gravity, no base-plate marker, etc.
        metaData.writeByte(14); // armor stand status index (BYTE_SERIALIZER)
        serializerTypeID = DataWatcherRegistry.b(BYTE_SERIALIZER);
        if (serializerTypeID >= 0) {
            metaData.d(serializerTypeID);
            BYTE_SERIALIZER.a(metaData, (glassHead ? (byte) (0x02 | 0x08 | 0x10) : (byte) (0x01 | 0x02 | 0x08 | 0x10)));
        }

        if (!name.isEmpty()) {
            name = name.substring(0, Math.min(name.length(), 5000));

            // set custom name
            metaData.writeByte(2); // custom name index (OPTIONAL_CHAT_COMPONENT_SERIALIZER)
            serializerTypeID = DataWatcherRegistry.b(OPTIONAL_CHAT_COMPONENT_SERIALIZER);
            if (serializerTypeID >= 0) {
                metaData.d(serializerTypeID);
                OPTIONAL_CHAT_COMPONENT_SERIALIZER.a(metaData, Optional.of(CraftChatMessage
                        .fromString(DisplayShops.getPluginInstance().getManager().color(name), false)[0]));
            }

            // set name visibility
            metaData.writeByte(3); // custom name visibility index (BOOLEAN_SERIALIZER)
            serializerTypeID = DataWatcherRegistry.b(BOOLEAN_SERIALIZER);
            if (serializerTypeID >= 0) {
                metaData.d(serializerTypeID);
                BOOLEAN_SERIALIZER.a(metaData, true);
            }
        }

        metaData.d(0xFF);

        PacketPlayOutEntityMetadata md = new PacketPlayOutEntityMetadata();
        try {
            md.a(metaData);
        } catch (IOException e) {e.printStackTrace();}
        sendPacket(playerConnection, md);

        return id;
    }

    @Override
    public void hide(@NotNull Player player) {
        PlayerConnection playerConnection = ((CraftPlayer) player).getHandle().playerConnection;
        for (int entityId : this.getEntityIds()) {
            PacketPlayOutEntityDestroy standPacket = new PacketPlayOutEntityDestroy(entityId);
            playerConnection.sendPacket(standPacket);
        }
    }

    public void sendPacket(@NotNull PlayerConnection playerConnection, @NotNull Packet<?> packet) {
        playerConnection.a().sendPacket(packet);
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