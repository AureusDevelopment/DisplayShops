package xzot1k.plugins.ds.nms.v1_8_R3;

import net.minecraft.server.v1_8_R3.*;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.craftbukkit.v1_8_R3.entity.CraftPlayer;
import org.bukkit.craftbukkit.v1_8_R3.inventory.CraftItemStack;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import xzot1k.plugins.ds.DisplayShops;
import xzot1k.plugins.ds.api.VersionUtil;

public class VUtil implements VersionUtil {


    @Override
    public void sendActionBar(@NotNull Player player, @NotNull String message) {
        IChatBaseComponent iChatBaseComponent = IChatBaseComponent.ChatSerializer.a("{\"text\": \""
                + DisplayShops.getPluginInstance().getManager().color(message) + "\"}");
        PacketPlayOutChat bar = new PacketPlayOutChat(iChatBaseComponent, (byte) 2);
        ((CraftPlayer) player).getHandle().playerConnection.sendPacket(bar);
    }

    @Override
    public void displayParticle(@NotNull Player player, @NotNull String particleName, @NotNull Location location,
                                double offsetX, double offsetY, double offsetZ, int speed, int amount) {
        if (location.getWorld() != null) {
            PacketPlayOutWorldParticles packet = new PacketPlayOutWorldParticles(EnumParticle.valueOf(particleName),
                    true, (float) location.getX(), (float) location.getY(), (float) location.getZ(), (float) offsetX,
                    (float) offsetY, (float) offsetZ, speed, amount);
            ((CraftPlayer) player).getHandle().playerConnection.sendPacket(packet);
        }
    }

    @Override
    public String getNBT(@NotNull ItemStack itemStack, @NotNull String nbtTag) {
        final net.minecraft.server.v1_8_R3.ItemStack item = CraftItemStack.asNMSCopy(itemStack);
        final NBTTagCompound tag = (item.getTag() == null ? new NBTTagCompound() : item.getTag());
        return tag.getString(nbtTag);
    }

    @Override
    public ItemStack updateNBT(@NotNull ItemStack itemStack, @NotNull String nbtTag, @NotNull String value) {
        final net.minecraft.server.v1_8_R3.ItemStack item = CraftItemStack.asNMSCopy(itemStack);
        final NBTTagCompound tag = (item.getTag() == null ? new NBTTagCompound() : item.getTag());
        tag.setString(nbtTag, value);
        item.save(tag);

        return CraftItemStack.asBukkitCopy(item);
    }

    @Override
    public String getInventoryName(Object inventory, Object inventoryView) {
        return ((InventoryView) inventoryView).getTitle();
    }

    @Override
    public String getPlayerTopInventoryName(Player player) {
        return player.getOpenInventory().getTopInventory().getTitle();
    }

    @Override
    public InventoryType getInventoryType(Object inventoryView) {
        return ((InventoryView) inventoryView).getType();
    }

    @Override
    public void playSound(Player player, String key) {
        player.playSound(player.getLocation(), Sound.valueOf(key), 1, 1);
    }

    @Override
    public void playSound(World world, Location location, String key) {
        world.playSound(location, Sound.valueOf(key), 1, 1);
    }


}
