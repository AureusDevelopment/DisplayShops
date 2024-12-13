/*
 * Copyright (c) 2021 XZot1K, All rights reserved.
 */

package xzot1k.plugins.ds.nms.v1_21_R2;

import com.mojang.datafixers.util.Pair;
import io.netty.buffer.Unpooled;
import net.md_5.bungee.api.ChatColor;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.*;
import net.minecraft.network.syncher.EntityDataSerializer;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.server.network.ServerPlayerConnection;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import org.bukkit.Material;
import org.bukkit.craftbukkit.CraftRegistry;
import org.bukkit.craftbukkit.entity.CraftPlayer;
import org.bukkit.craftbukkit.inventory.CraftItemStack;
import org.bukkit.craftbukkit.util.CraftChatMessage;
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
    private final DisplayShops INSTANCE;
    private final Collection<Integer> entityIds = new ArrayList<>();
    private net.minecraft.world.item.ItemStack itemStack;

    public DPacket(@NotNull DisplayShops instance, @NotNull Player player, @NotNull Shop shop, boolean showHolograms) {
        this.INSTANCE = instance;
        if (!player.isOnline()) return;

        final ServerPlayerConnection playerConnection = getPlayerConnection(player);
        if (playerConnection == null) return;

        Appearance appearance = Appearance.getAppearance(shop.getAppearanceId());
        if (appearance == null) return;

        final double[] offsets = appearance.getOffset();
        final double offsetX = offsets[0], offsetY = offsets[1], offsetZ = offsets[2];

        double x = (shop.getBaseLocation().getX() + 0.5 + offsetX),
                y = (shop.getBaseLocation().getY() - 0.3 + offsetY),
                z = (shop.getBaseLocation().getZ() + 0.5 + offsetZ);

        if (!INSTANCE.getConfig().getBoolean("hide-glass")) {
            createStand(playerConnection, x, y, z, "", true);
        }

        ItemStack item = (shop.getShopItem() != null) ? shop.getShopItem().clone() : (INSTANCE.getConfig().getBoolean("empty-shop-item")
                ? new ItemStack(Material.BARRIER) : null);
        if (item != null) {
            if (INSTANCE.getConfig().getBoolean("force-single-stack")) item.setAmount(1);

            if (item.getType() != Material.AIR) {
                itemStack = CraftItemStack.asNMSCopy(item);
                if (itemStack != null) {
                    //<editor-fold desc="Item Packet">
                    final int id = idGenerator.get();
                    getEntityIds().add(id);

                    RegistryFriendlyByteBuf bd = buildSerializer(id, true, x, (y + 1.325), z);
                    ClientboundAddEntityPacket itemPacket = ClientboundAddEntityPacket.STREAM_CODEC.decode(bd);
                    sendPacket(playerConnection, itemPacket);

                    RegistryFriendlyByteBuf bb = new RegistryFriendlyByteBuf(Unpooled.buffer(), CraftRegistry.getMinecraftRegistry());
                    bb.writeVarInt(id);

                    writeEntry(bb, 8, itemStack); // add itemstack data

                    bb.writeVarInt(0xFF);
                    ClientboundSetEntityDataPacket md = ClientboundSetEntityDataPacket.STREAM_CODEC.decode(bb);
                    sendPacket(playerConnection, md);
                    //</editor-fold>

                    //<editor-fold desc="Vehicle Mount Packets">
                    final int vehicleId = idGenerator.get();
                    getEntityIds().add(vehicleId);
                    RegistryFriendlyByteBuf vehicleData = buildSerializer(vehicleId, false, x, (y + 1.325), z);

                    ClientboundAddEntityPacket vehiclePacket = ClientboundAddEntityPacket.STREAM_CODEC.decode(vehicleData);
                    sendPacket(playerConnection, vehiclePacket);

                    RegistryFriendlyByteBuf vehiclePDS = new RegistryFriendlyByteBuf(Unpooled.buffer(), CraftRegistry.getMinecraftRegistry());
                    vehiclePDS.writeVarInt(vehicleId);

                    writeEntry(vehiclePDS, 0, (byte) 0x20); // invisibility
                    writeEntry(vehiclePDS, 15, (byte) (0x01 | 0x02 | 0x08 | 0x10));  // small, no gravity, no base-plate marker, etc.

                    vehiclePDS.writeVarInt(0xFF);

                    ClientboundSetEntityDataPacket vehicleMD = ClientboundSetEntityDataPacket.STREAM_CODEC.decode(vehiclePDS);
                    sendPacket(playerConnection, vehicleMD);

                    RegistryFriendlyByteBuf mountData = new RegistryFriendlyByteBuf(Unpooled.buffer(), CraftRegistry.getMinecraftRegistry());
                    mountData.writeVarInt(vehicleId);
                    mountData.writeVarInt(1);
                    mountData.writeVarInt(id);

                    ClientboundSetPassengersPacket mountPacket = ClientboundSetPassengersPacket.STREAM_CODEC.decode(mountData);
                    sendPacket(playerConnection, mountPacket);
                    //</editor-fold>
                }
            }
        }

        if (!showHolograms) return;

        List<String> hologramFormat;
        if (shop.getShopItem() != null) {
            if (shop.getOwnerUniqueId() == null)
                hologramFormat = INSTANCE.getConfig().getStringList("admin-shop-format");
            else hologramFormat = INSTANCE.getConfig().getStringList("valid-item-format");
        } else {
            if (shop.getOwnerUniqueId() == null)
                hologramFormat = INSTANCE.getConfig().getStringList("admin-invalid-item-format");
            else hologramFormat = INSTANCE.getConfig().getStringList("invalid-item-format");
        }

        final String colorCode = INSTANCE.getConfig().getString("default-description-color");
        final boolean hidePriceLine = INSTANCE.getConfig().getBoolean("price-disabled-hide");
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

                List<String> descriptionLines = INSTANCE.getManager().wrapString(shop.getDescription());
                Collections.reverse(descriptionLines);
                for (int j = -1; ++j < descriptionLines.size(); ) {
                    String descriptionLine = INSTANCE.getManager().color(descriptionLines.get(j));
                    descriptionLine = (descriptionLine.contains(ChatColor.COLOR_CHAR + "") ? descriptionLine : (INSTANCE.getManager().color(colorCode + descriptionLine)));
                    createStand(playerConnection, x, y, z, (prefix + descriptionLine + suffix), false);
                    y += 0.3;
                }
                continue;
            }

            createStand(playerConnection, x, y, z, INSTANCE.getManager().applyShopBasedPlaceholders(line, shop), false);
            y += 0.3;
        }
    }

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

    private ServerPlayerConnection getPlayerConnection(@NotNull Player player) {
        return ((CraftPlayer) player).getHandle().connection;
    }

    private RegistryFriendlyByteBuf buildSerializer(int id, boolean isItem, double x, double y, double z) {
        RegistryFriendlyByteBuf pds = new RegistryFriendlyByteBuf(Unpooled.buffer(), CraftRegistry.getMinecraftRegistry());

        pds.writeVarInt(id);
        pds.writeUUID(UUID.randomUUID());
        ByteBufCodecs.registry(Registries.ENTITY_TYPE).encode(pds, isItem ? EntityType.ITEM : EntityType.ARMOR_STAND);

        // Position
        pds.writeDouble(x); //X
        pds.writeDouble(y); //Y
        pds.writeDouble(z); //Z

        // Rotation
        pds.writeByte(0); //PITCH
        pds.writeByte(0); //YAW
        pds.writeByte(0); //HEAD_YAW

        // Object Data
        pds.writeInt(isItem ? 1 : 0);

        // Velocity
        pds.writeShort(0);
        pds.writeShort(0);
        pds.writeShort(0);

        return pds;
    }

    private void createStand(@NotNull ServerPlayerConnection playerConnection, double x, double y, double z,
                             @NotNull String name, boolean glassHead) {
        final int id = idGenerator.get();
        getEntityIds().add(id);

        final RegistryFriendlyByteBuf pds = buildSerializer(id, false, x, y, z);
        final ClientboundAddEntityPacket spawnPacket = ClientboundAddEntityPacket.STREAM_CODEC.decode(pds);

        sendPacket(playerConnection, spawnPacket);

        if (glassHead) {
            itemStack = CraftItemStack.asNMSCopy(new ItemStack(Material.GLASS));
            if (itemStack != null) {
                List<Pair<EquipmentSlot, net.minecraft.world.item.ItemStack>> list = new ArrayList<>();
                list.add(new Pair<>(EquipmentSlot.HEAD, itemStack));
                ClientboundSetEquipmentPacket packet = new ClientboundSetEquipmentPacket(id, list);
                sendPacket(playerConnection, packet);
            }
        }

        RegistryFriendlyByteBuf metaData = new RegistryFriendlyByteBuf(Unpooled.buffer(), CraftRegistry.getMinecraftRegistry());
        metaData.writeVarInt(id);

        writeEntry(metaData, 0, (byte) 0x20); // invisibility
        writeEntry(metaData, 15, (glassHead ? (byte) (0x02 | 0x08 | 0x10) : (byte) (0x01 | 0x02 | 0x08 | 0x10)));  // small, no gravity, no base-plate marker, etc.

        if (!name.isEmpty()) {
            name = name.substring(0, Math.min(name.length(), 5000)); // set name limit

            // set custom name
            writeEntry(metaData, 2, Optional.of(CraftChatMessage.fromString(DisplayShops.getPluginInstance().getManager().color(name), false, true)[0])); // set name
            writeEntry(metaData, 3, true); // set name visibility
        }

        metaData.writeByte(0xFF);
        ClientboundSetEntityDataPacket md = ClientboundSetEntityDataPacket.STREAM_CODEC.decode(metaData);
        /*        ClientboundSetEntityDataPacket*/
        sendPacket(playerConnection, md);
    }

    @SuppressWarnings("unchecked")
    private void writeEntry(@NotNull RegistryFriendlyByteBuf serializer, int id, Object value) { // data watcher entry
        serializer.writeByte(id);

        EntityDataSerializer<net.minecraft.world.item.ItemStack> ITEM_SERIALIZER = EntityDataSerializers.ITEM_STACK;
        EntityDataSerializer<Byte> BYTE_SERIALIZER = EntityDataSerializers.BYTE;
        EntityDataSerializer<Boolean> BOOLEAN_SERIALIZER = EntityDataSerializers.BOOLEAN;
        EntityDataSerializer<Optional<Component>> COMPONENT_SERIALIZER = EntityDataSerializers.OPTIONAL_COMPONENT;

        EntityDataSerializer<?> watcherRegistry = (id == 2 ? COMPONENT_SERIALIZER : id == 3 ? BOOLEAN_SERIALIZER : id == 8 ? ITEM_SERIALIZER : BYTE_SERIALIZER);
        int serializerTypeID = EntityDataSerializers.getSerializedId(watcherRegistry);

        if (serializerTypeID >= 0) {
            serializer.writeVarInt(serializerTypeID);

            if (id == 2) {
                ComponentSerialization.OPTIONAL_STREAM_CODEC.encode(serializer, (Optional<Component>) value);
            } else if (id == 3) {
                ByteBufCodecs.BOOL.encode(serializer, (Boolean) value);
            } else if (id == 8) {
                net.minecraft.world.item.ItemStack.STREAM_CODEC.encode(serializer, (net.minecraft.world.item.ItemStack) value);
            } else {
                ByteBufCodecs.BYTE.encode(serializer, (Byte) value);
            }
        }
    }

    public void hide(@NotNull Player player) {
        if (getEntityIds() != null && !getEntityIds().isEmpty()) {
            final ServerPlayerConnection playerConnection = getPlayerConnection(player);
            for (int entityId : getEntityIds()) {
                ClientboundRemoveEntitiesPacket standPacket = new ClientboundRemoveEntitiesPacket(entityId);
                sendPacket(playerConnection, standPacket);
            }
        }
    }

    public void sendPacket(@NotNull ServerPlayerConnection playerConnection, @NotNull Packet<?> packet) {
        playerConnection.send(packet);
    }

    public Collection<Integer> getEntityIds() {
        return entityIds;
    }
}