package xzot1k.plugins.ds.nms.v1_21_R8;

import io.papermc.paper.registry.RegistryAccess;
import io.papermc.paper.registry.RegistryKey;
import net.kyori.adventure.key.Key;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.component.CustomData;
import org.bukkit.*;
import org.bukkit.craftbukkit.inventory.CraftItemStack;
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
        player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(DisplayShops.getPluginInstance().getManager().color(message)));
    }

    @Override
    public void displayParticle(@NotNull Player player, @NotNull String particleName, @NotNull Location location,
                                double offsetX, double offsetY, double offsetZ, int speed, int amount) {
        if (location.getWorld() != null) {
            Particle particle = Particle.valueOf(particleName);
            if (particle == Particle.DUST)
                player.spawnParticle(particle, location, amount, offsetX, offsetY, offsetZ);
            else player.spawnParticle(particle, location, amount, offsetX, offsetY, offsetZ, 0);
        }
    }

    @Override
    public String getNBT(@NotNull ItemStack itemStack, @NotNull String nbtTag) {
        final net.minecraft.world.item.ItemStack item = CraftItemStack.asNMSCopy(itemStack);
        if (item.isEmpty()) {
            return null;
        }
        CustomData data = item.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY);
        try {
            return data.copyTag().getString(nbtTag).orElse(null);
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    public ItemStack updateNBT(@NotNull ItemStack itemStack, @NotNull String nbtTag, @NotNull String value) {
        final net.minecraft.world.item.ItemStack item = CraftItemStack.asNMSCopy(itemStack);
        if (item.isEmpty()) {
            return CraftItemStack.asBukkitCopy(item);
        }
        CustomData d = item.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY);
        item.set(DataComponents.CUSTOM_DATA, d.update(a -> {
            a.putString(nbtTag, value);
        }));
        return CraftItemStack.asBukkitCopy(item);
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

    private final Registry<Sound> REGISTRY = RegistryAccess.registryAccess().getRegistry(RegistryKey.SOUND_EVENT);

    @Override
    public void playSound(Player player, String key) {
        Sound s;
        key = key.toLowerCase();
        try {
            s = REGISTRY.get(Key.key(key));
        } catch (Exception e) {
            LOGGER.warning("Detected OLD or invalid sound format! Sound Key: " + key + " | Error: " + e.getMessage());
            return;
        }
        if (s != null)
            player.playSound(player.getLocation(), s, 1, 1);
    }

    @Override
    public void playSound(World world, Location location, String key) {
        Sound s;
        key = key.toLowerCase();
        try {
            s = REGISTRY.get(Key.key(key));
        } catch (Exception e) {
            LOGGER.warning("Detected OLD or invalid sound format! Sound: " + key + " | Error: " + e.getMessage());
            return;
        }
        if (s != null)
            world.playSound(location, s, 1, 1);
    }
}
