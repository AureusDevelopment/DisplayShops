package xzot1k.plugins.ds.api;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.logging.Logger;

public interface VersionUtil {
    Logger LOGGER = Logger.getLogger(VersionUtil.class.getName());

    void sendActionBar(@NotNull Player player, @NotNull String message);

    void displayParticle(@NotNull Player player, @NotNull String particleName, @NotNull Location location,
                         double offsetX, double offsetY, double offsetZ, int speed, int amount);

    /**
     * @param itemStack The item to obtain NBT from.
     * @param nbtTag    The NBT tag.
     * @return The value associated to the NBT tag on the item.
     */
    String getNBT(@NotNull ItemStack itemStack, @NotNull String nbtTag);

    /**
     * @param itemStack The item to update NBT on.
     * @param nbtTag    The NBT tag to update.
     * @param value     The value to associate with the NBT tag.
     * @return The finalized item.
     */
    ItemStack updateNBT(@NotNull ItemStack itemStack, @NotNull String nbtTag, @NotNull String value);

    String getInventoryName(Object inventory, Object inventoryView);

    String getPlayerTopInventoryName(Player player);

    void playSound(Player player, String key);

    void playSound(World world, Location location, String key);

    InventoryType getInventoryType(Object inventoryView);

}
