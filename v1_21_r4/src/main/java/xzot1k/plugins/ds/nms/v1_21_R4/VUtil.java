package xzot1k.plugins.ds.nms.v1_21_R4;

import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.component.CustomData;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.craftbukkit.inventory.CraftItemStack;
import org.bukkit.entity.Player;
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
            if (particle == Particle.DUST) {
                player.spawnParticle(particle, location, amount, offsetX, offsetY, offsetZ);
            } else player.spawnParticle(particle, location, amount, offsetX, offsetY, offsetZ, 0);
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
            return data.copyTag().getString(nbtTag);
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
        CompoundTag nbt = new CompoundTag();
        nbt.putString(nbtTag, value);
        item.set(DataComponents.CUSTOM_DATA, CustomData.of(nbt));
        return CraftItemStack.asBukkitCopy(item);
    }

}