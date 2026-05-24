package xzot1k.plugins.ds.nms.v1_19_R2;

import com.mojang.datafixers.util.Pair;
import io.netty.buffer.Unpooled;
import net.md_5.bungee.api.ChatColor;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.*;
import net.minecraft.network.syncher.EntityDataSerializer;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import org.bukkit.Material;
import org.bukkit.craftbukkit.v1_19_R2.entity.CraftPlayer;
import org.bukkit.craftbukkit.v1_19_R2.inventory.CraftItemStack;
import org.bukkit.craftbukkit.v1_19_R2.util.CraftChatMessage;
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
    private final Collection<Integer> entityIds = new ArrayList<>();
    private DisplayShops pluginInstance;
    private net.minecraft.world.item.ItemStack itemStack;

    public DPacket(@NotNull DisplayShops pluginInstance, @NotNull Player player, @NotNull Shop shop, boolean showHolograms) {
        setPluginInstance(pluginInstance);
        if (!player.isOnline()) return;

        final ServerGamePacketListener playerConnection = getPlayerConnection(player);
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

        ItemStack item = (shop.getShopItem() != null) ? shop.getShopItem().clone()
                : (getPluginInstance().getConfig().getBoolean("empty-shop-item")
                ? new ItemStack(Material.BARRIER) : null);
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

                        FriendlyByteBuf pds = buildSerializer(id, true, x, (y + 1.325) + offsetY, z);

                        final ClientboundAddEntityPacket itemPacket = new ClientboundAddEntityPacket(pds);
                        sendPacket(playerConnection, itemPacket);

                        final EntityDataSerializer<net.minecraft.world.item.ItemStack> ITEM_STACK_SERIALIZER = EntityDataSerializers.ITEM_STACK;
                        final EntityDataSerializer<Byte> BYTE_SERIALIZER = EntityDataSerializers.BYTE;

                        FriendlyByteBuf metaData = new FriendlyByteBuf(Unpooled.buffer());
                        metaData.writeVarInt(id);
                        metaData.writeByte(8); // key index

                        int serializerTypeID = EntityDataSerializers.getSerializedId(ITEM_STACK_SERIALIZER);
                        if (serializerTypeID < 0) return;

                        metaData.writeVarInt(serializerTypeID);
                        ITEM_STACK_SERIALIZER.write(metaData, itemStack);
                        metaData.writeVarInt(0xFF);

                        ClientboundSetEntityDataPacket md = new ClientboundSetEntityDataPacket(metaData);
                        sendPacket(playerConnection, md);
                        //</editor-fold>

                        //<editor-fold desc="Vehicle Mount Packets">
                        final int vehicleId = idGenerator.get();
                        getEntityIds().add(vehicleId);
                        FriendlyByteBuf vehicleData = buildSerializer(vehicleId, false, x, (y + 1.325) + offsetY, z);
                        ClientboundAddEntityPacket vehiclePacket = new ClientboundAddEntityPacket(vehicleData);

                        sendPacket(playerConnection, vehiclePacket);

                        FriendlyByteBuf vehiclePDS = new FriendlyByteBuf(Unpooled.buffer());
                        vehiclePDS.writeVarInt(vehicleId);


                        // invisibility
                        vehiclePDS.writeByte(0); // key index
                        serializerTypeID = EntityDataSerializers.getSerializedId(BYTE_SERIALIZER);
                        if (serializerTypeID < 0) return;

                        vehiclePDS.writeVarInt(serializerTypeID);
                        BYTE_SERIALIZER.write(vehiclePDS, (byte) 0x20);

                        // small, no gravity, no base-plate marker, etc.
                        vehiclePDS.writeByte(15); // key index
                        serializerTypeID = EntityDataSerializers.getSerializedId(BYTE_SERIALIZER);
                        if (serializerTypeID < 0) return;

                        vehiclePDS.writeVarInt(serializerTypeID);
                        BYTE_SERIALIZER.write(vehiclePDS, (byte) (0x01 | 0x02 | 0x08 | 0x10));

                        vehiclePDS.writeVarInt(0xFF);

                        ClientboundSetEntityDataPacket vehicleMD = new ClientboundSetEntityDataPacket(vehiclePDS);

                        sendPacket(playerConnection, vehicleMD);

                        FriendlyByteBuf mountData = new FriendlyByteBuf(Unpooled.buffer());
                        mountData.writeVarInt(vehicleId);
                        mountData.writeVarInt(1);
                        mountData.writeVarInt(id);

                        ClientboundSetPassengersPacket mountPacket = new ClientboundSetPassengersPacket(mountData);

                        sendPacket(playerConnection, mountPacket);
                        //</editor-fold>
                    }
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

    private ServerGamePacketListener getPlayerConnection(@NotNull Player player) {
        return ((CraftPlayer) player).getHandle().connection;
    }

    private FriendlyByteBuf buildSerializer(int id, boolean isItem, double x, double y, double z) {
        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());

        buf.writeVarInt(id);
        buf.writeUUID(UUID.randomUUID());
        buf.writeVarInt(isItem ? 41 : 1);

        buf.writeDouble(x);
        buf.writeDouble(y);
        buf.writeDouble(z);

        buf.writeByte(0);
        buf.writeByte(0);
        buf.writeByte(0);

        buf.writeInt(isItem ? 1 : 0);

        buf.writeShort(0);
        buf.writeShort(0);
        buf.writeShort(0);
        return buf;
    }

    private void createStand(@NotNull ServerGamePacketListener playerConnection, double x, double y, double z, @NotNull String name, boolean glassHead) {
        final int id = idGenerator.get();
        getEntityIds().add(id);

        final FriendlyByteBuf pds = buildSerializer(id, false, x, y, z);

        final ClientboundAddEntityPacket spawnPacket = new ClientboundAddEntityPacket(pds);

        sendPacket(playerConnection, spawnPacket);

        final EntityDataSerializer<Byte> BYTE_SERIALIZER = EntityDataSerializers.BYTE;
        final EntityDataSerializer<Boolean> BOOLEAN_SERIALIZER = EntityDataSerializers.BOOLEAN;
        final EntityDataSerializer<Optional<Component>> OPTIONAL_CHAT_COMPONENT_SERIALIZER = EntityDataSerializers.OPTIONAL_COMPONENT;


        if (glassHead) {
            itemStack = CraftItemStack.asNMSCopy(new ItemStack(Material.GLASS));
            if (itemStack != null) {
                List<Pair<EquipmentSlot, net.minecraft.world.item.ItemStack>> list = new ArrayList<>();
                list.add(new Pair<>(EquipmentSlot.HEAD /*HEAD or HELMET*/, itemStack));
                ClientboundSetEquipmentPacket packet = new ClientboundSetEquipmentPacket(id, list);
                sendPacket(playerConnection, packet);
            }
        }
        FriendlyByteBuf metaData = new FriendlyByteBuf(Unpooled.buffer());
        metaData.writeVarInt(id);


        // invisibility
        metaData.writeByte(0); // key index
        int serializerTypeID = EntityDataSerializers.getSerializedId(BYTE_SERIALIZER);
        if (serializerTypeID < 0) return;

        metaData.writeVarInt(serializerTypeID);
        BYTE_SERIALIZER.write(metaData, (byte) 0x20);

        // small, no gravity, no base-plate marker, etc.
        metaData.writeByte(15); // key index
        serializerTypeID = EntityDataSerializers.getSerializedId(BYTE_SERIALIZER);
        if (serializerTypeID < 0) return;

        metaData.writeVarInt(serializerTypeID);
        BYTE_SERIALIZER.write(metaData, (glassHead ? (byte) (0x02 | 0x08 | 0x10) : (byte) (0x01 | 0x02 | 0x08 | 0x10)));

        if (!name.isEmpty()) {
            name = name.substring(0, Math.min(name.length(), 5000));

            // set custom name
            metaData.writeByte(2); // key index
            serializerTypeID = EntityDataSerializers.getSerializedId(OPTIONAL_CHAT_COMPONENT_SERIALIZER);
            if (serializerTypeID < 0) return;
            metaData.writeVarInt(serializerTypeID);
            OPTIONAL_CHAT_COMPONENT_SERIALIZER.write(metaData, Optional.of(CraftChatMessage.fromString(DisplayShops.getPluginInstance().getManager().color(name), false, true)[0]));


            // set name visibility
            metaData.writeByte(3); // key index
            serializerTypeID = EntityDataSerializers.getSerializedId(BOOLEAN_SERIALIZER);
            if (serializerTypeID < 0) return;

            metaData.writeVarInt(serializerTypeID);
            BOOLEAN_SERIALIZER.write(metaData, true);
        }

        metaData.writeVarInt(0xFF);

        ClientboundSetEntityDataPacket md = new ClientboundSetEntityDataPacket(metaData);
        sendPacket(playerConnection, md);
    }

    public void hide(@NotNull Player player) {
        if (getEntityIds() != null && !getEntityIds().isEmpty()) {
            final ServerGamePacketListener playerConnection = getPlayerConnection(player);
            for (int entityId : getEntityIds()) {
                ClientboundRemoveEntitiesPacket standPacket = new ClientboundRemoveEntitiesPacket(entityId);
                playerConnection.getConnection().send(standPacket);
            }
        }
    }

    public void sendPacket(@NotNull ServerGamePacketListener playerConnection, @NotNull Packet<?> packet) {
        playerConnection.getConnection().send(packet);
    }

    private DisplayShops getPluginInstance() {
        return pluginInstance;
    }

    private void setPluginInstance(@NotNull DisplayShops pluginInstance) {
        this.pluginInstance = pluginInstance;
    }

    public Collection<Integer> getEntityIds() {
        return entityIds;
    }

}