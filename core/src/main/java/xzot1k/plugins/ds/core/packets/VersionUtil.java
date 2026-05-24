package xzot1k.plugins.ds.core.packets;

import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;
import xzot1k.plugins.ds.DisplayShops;

import java.util.Arrays;
import java.util.HashMap;

public class VersionUtil implements xzot1k.plugins.ds.api.VersionUtil {
    private static final HashMap<String, NamespacedKey> keyMap = new HashMap<>();
    //private static final String CRAFTBUKKIT_PACKAGE = Bukkit.getServer().getClass().getPackage().getName();
    private final boolean redstoneExists = Arrays.stream(Particle.values()).anyMatch(particle -> particle.name().equals("REDSTONE"));
    //private static Class<?> craftItemClass;
    //private static Method nmsCopyMethod;

    public VersionUtil() {
       /* try {
            craftItemClass = Class.forName(cbClass("inventory.CraftItemStack"));
            nmsCopyMethod = craftItemClass.getDeclaredMethod("asNMSCopy", ItemStack.class);
        }
        catch (Exception e) {e.printStackTrace();}*/
    }

    //public static String cbClass(String clazz) {return CRAFTBUKKIT_PACKAGE + "." + clazz;}

    @Override
    public void sendActionBar(@NotNull Player player, @NotNull String message) {
        player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(DisplayShops.getPluginInstance().getManager().color(message)));
    }

    @Override
    public void displayParticle(@NotNull Player player, @NotNull String particleName, @NotNull Location location, double offsetX, double offsetY, double offsetZ, int speed, int amount) {
        if (location.getWorld() != null) {
            Particle particle = Particle.valueOf(particleName);
            if (particle == (redstoneExists ? Particle.valueOf("REDSTONE") : Particle.valueOf("DUST"))) {
                player.spawnParticle(particle, location, amount, offsetX, offsetY, offsetZ);
            } else
                player.spawnParticle(particle, location, amount, offsetX, offsetY, offsetZ, 0);
        }
    }

    @Override
    public String getNBT(@NotNull ItemStack itemStack, @NotNull String nbtTag) {

        if (DisplayShops.getPluginInstance().isNBTAPIInstalled()) {
            de.tr7zw.nbtapi.NBTItem nbtItem = new de.tr7zw.nbtapi.NBTItem(itemStack);
            return (nbtItem.hasTag(nbtTag) ? nbtItem.getString(nbtTag) : null);
        }

        NamespacedKey key = keyMap.getOrDefault(nbtTag, null);
        if (key == null) {return null;}
        return itemStack.getItemMeta().getPersistentDataContainer().has(key) ? itemStack.getItemMeta().getPersistentDataContainer().get(key, PersistentDataType.STRING) : null;
    }

    @Override
    public ItemStack updateNBT(@NotNull ItemStack itemStack, @NotNull String nbtTag, @NotNull String value) {

        if (DisplayShops.getPluginInstance().isNBTAPIInstalled()) {
            de.tr7zw.nbtapi.NBTItem nbtItem = new de.tr7zw.nbtapi.NBTItem(itemStack);
            nbtItem.setString(nbtTag, value);
            return nbtItem.getItem();
        }

        NamespacedKey key = keyMap.getOrDefault(nbtTag, null);
        if (key == null) {key = keyMap.put(nbtTag, new NamespacedKey(DisplayShops.getPluginInstance(), nbtTag));}
        if (key != null) {
            ItemMeta itemMeta = itemStack.getItemMeta();
            itemMeta.getPersistentDataContainer().set(key, PersistentDataType.STRING, value);
            itemStack.setItemMeta(itemMeta);
        }
        return itemStack;
    }

    @Override
    public String getInventoryName(Object inventory, Object inventoryView) {
        return ((InventoryView) inventoryView).getTitle();
    }

    @Override
    public String getPlayerTopInventoryName(Player player) {
        return player.getOpenInventory().getTitle();
    }

    @Override
    public InventoryType getInventoryType(Object inventoryView) {
        return ((InventoryView) inventoryView).getType();
    }

    @Override
    public void playSound(Player player, String key) {
        player.playSound(player.getLocation(), Registry.SOUNDS.getOrThrow(NamespacedKey.minecraft(key)), 1, 1);
    }


    @Override
    public void playSound(World world, Location location, String key) {
        world.playSound(location, Registry.SOUNDS.getOrThrow(NamespacedKey.minecraft(key)), 1, 1);
    }

}