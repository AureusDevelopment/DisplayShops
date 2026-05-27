/*
 * Copyright (c) 2021 XZot1K, All rights reserved.
 */

package xzot1k.plugins.ds.core.tasks;

import com.nexomc.nexo.api.NexoItems;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.joml.Matrix4f;
import xzot1k.plugins.ds.DisplayShops;
import xzot1k.plugins.ds.api.objects.Appearance;
import xzot1k.plugins.ds.api.objects.Shop;
import xzot1k.plugins.ds.core.packets.Display;
import xzot1k.plugins.ds.core.utils.nms_utils.Ref;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;

public class VisualTask extends BukkitRunnable {

    private final boolean isItemSpinning;
    private final HashMap<UUID, Float> map = new HashMap<>();
    private DisplayShops pluginInstance;
    private boolean alwaysDisplay, pause = false;
    private LinkedList<UUID> shopsToRefresh, playersToRefresh;

    public VisualTask(DisplayShops pluginInstance) {
        setPluginInstance(pluginInstance);
        setShopsToRefresh(new LinkedList<>());
        setPlayersToRefresh(new LinkedList<>());
        setAlwaysDisplay(getPluginInstance().getConfig().getBoolean("always-display"));
        isItemSpinning = DisplayShops.getPluginInstance().getConfig().getBoolean("allow-item-spinning");
    }

    @Override
    public void run() {
        if (isPaused() || getPluginInstance().getManager().getShopMap() == null || getPluginInstance().getManager().getShopMap().isEmpty())
            return;

        boolean isNew = getPluginInstance().getDisplayManager() != null;

        for (Shop shop : getPluginInstance().getManager().getShopMap().values()) {
            if (shop == null || shop.getBaseLocation() == null) {continue;}

            // if display manager exists, use new displays
            if (isNew) {
                World world = DisplayShops.getPluginInstance().getServer().getWorld(shop.getBaseLocation().getWorldName());
                if (world == null) {continue;}

                if (!world.isChunkLoaded((int) shop.getBaseLocation().getX() >> 4, (int) shop.getBaseLocation().getZ() >> 4)) {
                    continue;
                }

                Display display = getPluginInstance().getDisplayManager().getDisplay(shop.getShopId());
                if (display == null || shop.getBaseLocation() == null) {continue;}

                final String generateText = shop.isClaimable() ? display.generateTextClaimable() : display.generateText();
                final ItemStack item = (shop.getShopItem() != null ? shop.getShopItem() : Display.barrier);
                float currentScale = 0.5f;
                double x = 0, y = 0, z = 0;

                if (display.getItemHolder() == null || (display.getItemHolder().getType().name().equals("ITEM_DISPLAY")
                        && ((ItemDisplay) display.getItemHolder()).getItemStack() == null
                        || (((ItemDisplay) display.getItemHolder()).getItemStack() != null && !((ItemDisplay) display.getItemHolder()).getItemStack().isSimilar(item)))) {
                    // handle offset
                    List<String> itemOffsets = DisplayShops.getPluginInstance().getConfig().getStringList("item-display-offsets");
                    for (int i = -1; ++i < itemOffsets.size(); ) {
                        String line = itemOffsets.get(i);
                        if (!line.contains(":")) {continue;}

                        String[] args = line.split(":");
                        if (args.length < 2 || !args[1].contains(",")) {continue;}

                        final String material = args[0];
                        boolean matches = false;

                        if (DisplayShops.getPluginInstance().isItemAdderInstalled()) {
                            if (dev.lone.itemsadder.api.CustomStack.isInRegistry(material)) {matches = true;}
                        }
                        if (DisplayShops.getPluginInstance().isNexoInstalled()) {
                            if (NexoItems.exists(material)) {
                                matches = true;
                            }
                        }

                        if (DisplayShops.getPluginInstance().isOraxenInstalled()) {
                            io.th0rgal.oraxen.items.ItemBuilder itemBuilder = io.th0rgal.oraxen.api.OraxenItems.getItemById(material);
                            if (itemBuilder != null) {matches = true;}
                        }

                        if (!matches && !item.getType().name().contains(args[0].toUpperCase().replace(" ", "_").replace("-", "_"))) {
                            continue;
                        }

                        String[] offsets = args[1].split(",");
                        if (offsets.length < 4) {continue;}

                        x = Double.parseDouble(offsets[0]);
                        y = Double.parseDouble(offsets[1]);
                        z = Double.parseDouble(offsets[2]);
                        currentScale += Float.parseFloat(offsets[3]);
                        break;
                    }
                }

                double[] offsets;
                Appearance appearance = Appearance.getAppearance(shop.getAppearanceId());
                if (appearance != null) {offsets = appearance.getOffset();} else {offsets = null;}

                float finalCurrentScale = currentScale;
                double finalX = x, finalY = y, finalZ = z;
                DisplayShops.getPluginInstance().getServer().getScheduler().runTask(DisplayShops.getPluginInstance(), () -> {
                    display.update(world, generateText, finalCurrentScale, finalX, finalY, finalZ, offsets);
                });
                if (isItemSpinning && !shop.isClaimable()) {
                    Matrix4f mat = new Matrix4f().scale(finalCurrentScale);
                    rotateDisplay(display, mat, finalCurrentScale, 5);
                }


                for (Player player : getPluginInstance().getServer().getOnlinePlayers()) {
                    if (player == null || !player.isOnline()) {continue;}
                    if (isAlwaysDisplay()) {
                        double value = (double) DisplayShops.getPluginInstance().getConfig().getInt("always-display-radius", 15) / 2;
                        Location min = shop.getBaseLocation().asBukkitLocation().clone().subtract(value, value, value);
                        Location max = shop.getBaseLocation().asBukkitLocation().clone().add(value, value, value);
                        // AABB containment check (O(1)). The previous implementation iterated every integer
                        // coordinate inside the box and compared each against the player's location, which
                        // is O(width * height * depth) per shop per online player per VisualTask tick. The
                        // X-loop's upper bound was also typoed as max.getBlockY(), so for shops whose X
                        // coordinate differed greatly from their Y coordinate (e.g. shops at large +/- X in
                        // overworld scale maps) the loop iterated thousands–millions of times per shop and
                        // drove the Craft Scheduler async pool to 100% CPU on multiple workers.
                        final int pX = player.getLocation().getBlockX(),
                                  pY = player.getLocation().getBlockY(),
                                  pZ = player.getLocation().getBlockZ();
                        boolean found = pX >= min.getBlockX() && pX <= max.getBlockX()
                                     && pY >= min.getBlockY() && pY <= max.getBlockY()
                                     && pZ >= min.getBlockZ() && pZ <= max.getBlockZ();
                        boolean finalFound = found;
                        DisplayShops.getPluginInstance().getServer().getScheduler().runTask(DisplayShops.getPluginInstance(), () -> {
                            display.show(player, finalFound);
                        });
                        continue;
                    }
                    final Shop foundShopAtLocation = getPluginInstance().getManager().getShopRayTraced(player.getWorld().getName(),
                            player.getEyeLocation().toVector(), player.getEyeLocation().getDirection(), 10/*getViewDistance()*/);

                    boolean isFocused = foundShopAtLocation != null && foundShopAtLocation.getShopId().toString().equals(shop.getShopId().toString());
                    DisplayShops.getPluginInstance().getServer().getScheduler().runTask(DisplayShops.getPluginInstance(), () -> {

                        display.show(player, isFocused);
                    });
                }

                continue;
            }

            if (shop.isClaimable())
                continue;

            for (Player player : getPluginInstance().getServer().getOnlinePlayers()) {
                if (getPlayersToRefresh().contains(player.getUniqueId()) || getShopsToRefresh().contains(shop.getShopId())) {
                    shop.kill(player);
                    getPluginInstance().killCurrentShopPacket(player);
                    getShopsToRefresh().remove(shop.getShopId());
                    getPlayersToRefresh().remove(player.getUniqueId());
                }

                final boolean packetExists = (getPluginInstance().getDisplayPacketMap().containsKey(player.getUniqueId())
                        && getPluginInstance().getDisplayPacketMap().get(player.getUniqueId()).containsKey(shop.getShopId())),
                        tooFarAway = (!shop.getBaseLocation().getWorldName().equalsIgnoreCase(player.getWorld().getName())
                                || shop.getBaseLocation().distance(player.getLocation(), true) > 16);

                if (tooFarAway) {
                    shop.kill(player);
                    continue;
                }

                if (isAlwaysDisplay()) {
                    if (packetExists) continue;

                    getPluginInstance().sendDisplayPacket(shop, player, true);
                    continue;
                }

                if (!packetExists) getPluginInstance().sendDisplayPacket(shop, player, false);

                Shop currentShop = null;
                if (!getPluginInstance().getShopMemory().isEmpty() && getPluginInstance().getShopMemory().containsKey(player.getUniqueId())) {
                    final UUID shopId = getPluginInstance().getShopMemory().getOrDefault(player.getUniqueId(), null);
                    if (shopId != null) currentShop = getPluginInstance().getManager().getShopById(shopId);
                }

                final Shop foundShopAtLocation = getPluginInstance().getManager().getShopRayTraced(player.getWorld().getName(),
                        player.getEyeLocation().toVector(), player.getEyeLocation().getDirection(), 10/*getViewDistance()*/);

                if (foundShopAtLocation == null) {
                    if (currentShop != null)
                        getPluginInstance().sendDisplayPacket(currentShop, player, false);
                    getPluginInstance().getShopMemory().remove(player.getUniqueId());
                    continue;
                }

                if (currentShop != null) {
                    if (!currentShop.getShopId().toString().equals(foundShopAtLocation.getShopId().toString())) {
                        getPluginInstance().sendDisplayPacket(currentShop, player, false);
                        getPluginInstance().getShopMemory().remove(player.getUniqueId());
                    } else continue;
                }

                getPluginInstance().sendDisplayPacket(foundShopAtLocation, player, true);
                getPluginInstance().getShopMemory().put(player.getUniqueId(), foundShopAtLocation.getShopId());
            }
        }
    }

    private void rotateDisplay(Display ddisplay, Matrix4f mat, float scale, int duration) {
        ItemDisplay display = (ItemDisplay) Ref.get(ddisplay, "itemDisplay");
        if (display == null)
            return;
        if (!getPluginInstance().isEnabled())
            return; // Prevent creating tasks if the plugin is disabling (as that would throw exceptions)

        final float rotationIncrement = (float) Math.toRadians(10); // Rotate 10 degrees per tick
        /*float currentAngle = 0F;*/ // Array to hold current angle
        float currentAngle = map.getOrDefault(display.getUniqueId(), 0F);

        if (display.isDead() || !display.isValid()) { // display was removed from the world, abort task
            return;
        }

        currentAngle += rotationIncrement; // Increment the angle
        if (currentAngle >= Math.toRadians(360)) {
            currentAngle -= (float) Math.toRadians(360); // Reset the angle if it completes a full rotation
        }
        map.put(display.getUniqueId(), currentAngle);

        ItemStack itemStack = display.getItemStack();
        if (itemStack != null) {
            if (itemStack.getType().name().contains("SHIELD")) {return;}
        }

        // Update the transformation matrix with the new rotation
        display.setTransformationMatrix(mat.identity().scale(scale).rotateY(currentAngle));
        display.setInterpolationDelay(0); // no delay to the interpolation
        display.setInterpolationDuration(duration); // set the duration of the interpolated rotation
    }

    public void refreshShop(Shop shop) {
        if (getPluginInstance().getDisplayManager() != null) {return;}

        if (shop == null || shop.getShopId() == null) return;
        if (getShopsToRefresh() != null && (getShopsToRefresh().isEmpty() || !getShopsToRefresh().contains(shop.getShopId())))
            getShopsToRefresh().add(shop.getShopId());
    }

    public void refreshShops(Player player) {
        if (player == null || getPluginInstance().getDisplayManager() != null) return;

        if (getPlayersToRefresh() != null && (getPlayersToRefresh().isEmpty() || !getPlayersToRefresh().contains(player.getUniqueId())))
            getPlayersToRefresh().add(player.getUniqueId());
    }

    // getters & setters
    private DisplayShops getPluginInstance() {
        return pluginInstance;
    }

    private void setPluginInstance(DisplayShops pluginInstance) {
        this.pluginInstance = pluginInstance;
    }

    private boolean isAlwaysDisplay() {
        return alwaysDisplay;
    }

    private void setAlwaysDisplay(boolean alwaysDisplay) {
        this.alwaysDisplay = alwaysDisplay;
    }

    private LinkedList<UUID> getShopsToRefresh() {
        return shopsToRefresh;
    }

    private void setShopsToRefresh(LinkedList<UUID> shopsToRefresh) {
        this.shopsToRefresh = shopsToRefresh;
    }

    private LinkedList<UUID> getPlayersToRefresh() {
        return playersToRefresh;
    }

    private void setPlayersToRefresh(LinkedList<UUID> playersToRefresh) {
        this.playersToRefresh = playersToRefresh;
    }

    public boolean isPaused() {
        return pause;
    }

    public void setPaused(boolean pause) {
        this.pause = pause;
    }
}