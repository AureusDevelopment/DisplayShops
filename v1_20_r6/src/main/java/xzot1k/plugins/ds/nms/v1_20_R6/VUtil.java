package xzot1k.plugins.ds.nms.v1_20_R6;

import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.item.component.CustomData;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.craftbukkit.v1_20_R4.inventory.CraftItemStack;
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
        if (item.e()) {
            return null;
        }
        CustomData data = item.a(DataComponents.b);
        if (data != null) {
            return data.c().l(nbtTag);
        }
        return null;
    }

    @Override
    public ItemStack updateNBT(@NotNull ItemStack itemStack, @NotNull String nbtTag, @NotNull String value) {
        final net.minecraft.world.item.ItemStack item = CraftItemStack.asNMSCopy(itemStack);
        if (item.e()) {
            return CraftItemStack.asBukkitCopy(item);
        }
        CustomData d = item.a(DataComponents.b);
        NBTTagCompound nbt = new NBTTagCompound();
        if (d != null) {
            nbt = d.c();
        }
        nbt.a(nbtTag, value);
        item.b(DataComponents.b, CustomData.a(nbt));
        return CraftItemStack.asBukkitCopy(item);
    }

}