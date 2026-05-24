package xzot1k.plugins.ds.nms.v1_20_R3;

import com.mojang.datafixers.util.Pair;
import io.netty.buffer.Unpooled;
import net.md_5.bungee.api.ChatColor;
import net.minecraft.core.Rotations;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.*;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Pose;
import org.bukkit.Material;
import org.bukkit.craftbukkit.v1_20_R3.entity.CraftPlayer;
import org.bukkit.craftbukkit.v1_20_R3.inventory.CraftItemStack;
import org.bukkit.craftbukkit.v1_20_R3.util.CraftChatMessage;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import xzot1k.plugins.ds.DisplayShops;
import xzot1k.plugins.ds.api.handlers.DisplayPacket;
import xzot1k.plugins.ds.api.objects.Appearance;
import xzot1k.plugins.ds.api.objects.Shop;

import java.util.*;

public class DPacket implements DisplayPacket {

    private final Collection<Integer> entityIds = new ArrayList<>();
    private DisplayShops pluginInstance;
    private net.minecraft.world.item.ItemStack itemStack;

    public DPacket(@NotNull DisplayShops pluginInstance, @NotNull Player player, @NotNull Shop shop, boolean showHolograms) {
        setPluginInstance(pluginInstance);
        if (!player.isOnline()) return;

        final ServerGamePacketListenerImpl playerConnection = getPlayerConnection(player);
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

                        final int id = Entity.nextEntityId();
                        getEntityIds().add(id);

                        FriendlyByteBuf pds = buildSerializer(id, true, x, (y + 1.325) + offsetY, z);

                        final ClientboundAddEntityPacket itemPacket = new ClientboundAddEntityPacket(pds);

                        sendPacket(playerConnection, itemPacket);

                        List<SynchedEntityData.DataValue<?>> list = new ArrayList<>(getDefaultEntityMetadata(false, null));

                        list.add(SynchedEntityData.DataValue.create(new EntityDataAccessor<>(8, EntityDataSerializers.ITEM_STACK), itemStack));

                        ClientboundSetEntityDataPacket md = new ClientboundSetEntityDataPacket(id, list);
                        sendPacket(playerConnection, md);


                        final int vehicleId = Entity.nextEntityId();
                        getEntityIds().add(vehicleId);
                        FriendlyByteBuf vehicleData = buildSerializer(vehicleId, false, x, (y + 1.325) + offsetY, z);
                        ClientboundAddEntityPacket vehiclePacket = new ClientboundAddEntityPacket(vehicleData);

                        sendPacket(playerConnection, vehiclePacket);

                        List<SynchedEntityData.DataValue<?>> list2 = new ArrayList<>(getArmorStandEntityMetadata(false, null));

                        ClientboundSetEntityDataPacket vehicleMD = new ClientboundSetEntityDataPacket(vehicleId, list2);
                        sendPacket(playerConnection, vehicleMD);

                        FriendlyByteBuf mountData = new FriendlyByteBuf(Unpooled.buffer());
                        mountData.writeVarInt(vehicleId);
                        mountData.writeVarIntArray(new int[]{id});

                        ClientboundSetPassengersPacket mountPacket = new ClientboundSetPassengersPacket(mountData);
                        sendPacket(playerConnection, mountPacket);
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

    private static List<SynchedEntityData.DataValue<?>> getArmorStandEntityMetadata(boolean glassHead, String name) {
        List<SynchedEntityData.DataValue<?>> list = new ArrayList<>(getDefaultEntityMetadata(true, name));
        byte b = (byte) (glassHead ? (0x02 | 0x08 | 0x010) : (0x01 | 0x02 | 0x08 | 0x10));
        list.add(SynchedEntityData.DataValue.create(new EntityDataAccessor<>(15, EntityDataSerializers.BYTE), b));
        var emptyRotation = new Rotations(0.0F, 0.0F, 0.0F);
        list.add(SynchedEntityData.DataValue.create(new EntityDataAccessor<>(16, EntityDataSerializers.ROTATIONS), emptyRotation));
        list.add(SynchedEntityData.DataValue.create(new EntityDataAccessor<>(17, EntityDataSerializers.ROTATIONS), emptyRotation));

        list.add(SynchedEntityData.DataValue.create(new EntityDataAccessor<>(18, EntityDataSerializers.ROTATIONS), new Rotations(-10.0F, 0.0F, -10.0F)));
        list.add(SynchedEntityData.DataValue.create(new EntityDataAccessor<>(19, EntityDataSerializers.ROTATIONS), new Rotations(-15.0F, 0.0F, 10.0F)));
        list.add(SynchedEntityData.DataValue.create(new EntityDataAccessor<>(20, EntityDataSerializers.ROTATIONS), new Rotations(-1.0F, 0.0F, -1.0F)));
        list.add(SynchedEntityData.DataValue.create(new EntityDataAccessor<>(21, EntityDataSerializers.ROTATIONS), new Rotations(1.0F, 0.0F, 1.0F)));
        return list;
    }

    private static List<SynchedEntityData.DataValue<?>> getDefaultEntityMetadata(boolean invisible, String name) {
        Optional<Component> nameComponent = Optional.ofNullable(name)
                .map(it -> it.substring(0, Math.min(it.length(), 5000)))
                .filter(it -> !it.isEmpty())
                .map(it -> DisplayShops.getPluginInstance().getManager().color(it))
                .map(it -> CraftChatMessage.fromString(it, false, true)[0]);


        List<SynchedEntityData.DataValue<?>> list = new ArrayList<>();
        list.add(SynchedEntityData.DataValue.create(new EntityDataAccessor<>(0, EntityDataSerializers.BYTE), invisible ? 0x20 : (byte) 0));
        list.add(SynchedEntityData.DataValue.create(new EntityDataAccessor<>(1, EntityDataSerializers.INT), 300));
        list.add(SynchedEntityData.DataValue.create(new EntityDataAccessor<>(2, EntityDataSerializers.OPTIONAL_COMPONENT), nameComponent));
        list.add(SynchedEntityData.DataValue.create(new EntityDataAccessor<>(3, EntityDataSerializers.BOOLEAN), nameComponent.isPresent()));
        list.add(SynchedEntityData.DataValue.create(new EntityDataAccessor<>(4, EntityDataSerializers.BOOLEAN), false));
        list.add(SynchedEntityData.DataValue.create(new EntityDataAccessor<>(5, EntityDataSerializers.BOOLEAN), false));
        list.add(SynchedEntityData.DataValue.create(new EntityDataAccessor<>(6, EntityDataSerializers.POSE), Pose.STANDING));
        list.add(SynchedEntityData.DataValue.create(new EntityDataAccessor<>(7, EntityDataSerializers.INT), 0));
        return list;
    }

    private ServerGamePacketListenerImpl getPlayerConnection(@NotNull Player player) {
        return ((CraftPlayer) player).getHandle().connection;
    }

    private FriendlyByteBuf buildSerializer(int id, boolean isItem, double x, double y, double z) {
        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());

        buf.writeVarInt(id);
        buf.writeUUID(UUID.randomUUID());
        buf.writeId(BuiltInRegistries.ENTITY_TYPE, isItem ? EntityType.ITEM : EntityType.ARMOR_STAND);

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

    private void createStand(@NotNull ServerGamePacketListenerImpl playerConnection, double x, double y, double z, @NotNull String name, boolean glassHead) {
        final int id = Entity.nextEntityId();
        getEntityIds().add(id);

        final FriendlyByteBuf pds = buildSerializer(id, false, x, y, z);

        final ClientboundAddEntityPacket spawnPacket = new ClientboundAddEntityPacket(pds);

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

        ClientboundSetEntityDataPacket md = new ClientboundSetEntityDataPacket(id, getArmorStandEntityMetadata(glassHead, name));
        sendPacket(playerConnection, md);
    }

    public void hide(@NotNull Player player) {
        if (getEntityIds() != null && !getEntityIds().isEmpty()) {
            final ServerGamePacketListenerImpl playerConnection = getPlayerConnection(player);
            for (int entityId : getEntityIds()) {
                ClientboundRemoveEntitiesPacket standPacket = new ClientboundRemoveEntitiesPacket(entityId);
                sendPacket(playerConnection, standPacket);
            }
        }
    }

    public void sendPacket(@NotNull ServerGamePacketListenerImpl playerConnection, @NotNull Packet<?> packet) {
        playerConnection.connection.send(packet);
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