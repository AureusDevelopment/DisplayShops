package xzot1k.plugins.ds.core.packets;

import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.*;
import org.bukkit.entity.Entity;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.Player;
import org.bukkit.entity.TextDisplay;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitTask;
import org.jetbrains.annotations.NotNull;
import org.joml.Matrix4f;
import xzot1k.plugins.ds.DisplayShops;
import xzot1k.plugins.ds.api.objects.Shop;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.logging.Level;

public class Display {

    public static NamespacedKey key = new NamespacedKey(DisplayShops.getPluginInstance(), "DisplayShops-Entity");
    public static final ItemStack barrier = new ItemStack(Material.BARRIER);

    private static final LegacyComponentSerializer SECTION = LegacyComponentSerializer.legacySection();

    private final Shop shop;
    private Entity textDisplay, itemDisplay, blockDisplay;
    private final List<UUID> entityIds = new ArrayList<>();

    public Display(@NotNull Shop shop) {
        this.shop = shop;
    }

    public void Clear() {
        World world = DisplayShops.getPluginInstance().getServer().getWorld(shop.getBaseLocation().getWorldName());

        DisplayShops.getPluginInstance().getServer().getScheduler().runTask(DisplayShops.getPluginInstance(), () -> {
            if (getItemHolder() != null) {getItemHolder().remove();}
            if (getGlass() != null) {getGlass().remove();}
            if (getTextDisplay() != null) {getTextDisplay().remove();}

            if (world != null) {
                world.getEntities().stream().filter(entity -> getEntityIds().contains(entity.getUniqueId())).forEach(Entity::remove);
            }

            getEntityIds().clear();
        });
    }

    public void update(World world, String generatedText, float itemScale, double itemOffsetX, double itemOffsetY, double itemOffsetZ, double[] offsets) {
        if (getShop() == null) return;

        Location baseLocation = getShop().getBaseLocation().asBukkitLocation();
        if (baseLocation == null) return;

        updateItem(world, baseLocation, itemScale, itemOffsetX, itemOffsetY, itemOffsetZ, offsets);
        updateGlass(world, baseLocation, offsets);
        updateLines(world, baseLocation, generatedText, offsets);
    }

    public void delete() {
        if(shop.getOwnerUniqueId()==null){
            DisplayShops.getPluginInstance().log(Level.WARNING,"DisplayShop with null owner detected! Location: "+shop.getBaseLocation().asBukkitLocation().toString());
            return;
        }
        DisplayShops.getPluginInstance().getDisplayManager().getShopDisplays().remove(shop.getOwnerUniqueId());
        Clear();
    }

    private void updateItem(World world, Location baseLocation, float scale, double x, double y, double z, double[] appearanceOffsets) {
        final float yaw = baseLocation.getYaw();
        final ItemStack item = (shop.getShopItem() != null ? shop.getShopItem() : barrier);
        final boolean isBlock = item.getType().isBlock() && item.getType() != Material.BARRIER,
                allowsSpinning = DisplayShops.getPluginInstance().getConfig().getBoolean("allow-item-spinning");

        float addYaw = 0f, addPitch = 0f;
        Location destination = baseLocation.clone();

        if (isBlock) {destination.add(0.5, 0.4, 0.5);}

        if ((yaw > 145 && yaw <= 180) || (yaw < -145 && yaw >= -180)) { // north

            if (!isBlock) destination.add(0.5, 0, allowsSpinning ? 0.5 : 0);
            addYaw = 0f; // opposite direction
            //System.out.println(item.getType() + " | NORTH");

        } else if (yaw <= 45 && yaw >= -45) { // south

            if (!isBlock) destination.add(0.5, 0, allowsSpinning ? 0.5 : 0);
            addYaw = 180f; // opposite direction
            //System.out.println(item.getType() + " | SOUTH");

        } else if (yaw > 45 && yaw < 145) { // west

            if (!isBlock) destination.add(allowsSpinning ? 0.5 : 0, 0, 0.5);
            addYaw = -90f; // opposite direction

            //System.out.println(item.getType() + " | WEST");

        } else if (yaw < -45 && yaw >= -145) { // east

            if (!isBlock) destination.add(allowsSpinning ? 0.5 : 0, 0, 0.5);
            addYaw = 90f; // opposite direction
            //System.out.println(item.getType() + " | EAST");

        }

        if (getItemHolder() == null || getItemHolder().isDead() || !getItemHolder().isValid()) {
            baseLocation.setYaw(addYaw);

            Location newLocation = baseLocation.clone().add(0.5 + x + (appearanceOffsets != null ? appearanceOffsets[0] : 0),
                    1.4 + y + (appearanceOffsets != null ? appearanceOffsets[1] : 0), 0.5 + z + (appearanceOffsets != null ? appearanceOffsets[2] : 0));

            for (ItemDisplay itemDisplay : newLocation.getNearbyEntitiesByType(ItemDisplay.class, 1)) {
                if (!itemDisplay.getPersistentDataContainer().has(key)) {
                    continue;
                }

                String value = itemDisplay.getPersistentDataContainer().get(key, PersistentDataType.STRING);
                if (value == null || !value.equals("item")) {
                    continue;
                }

                itemDisplay.remove();
                getEntityIds().remove(itemDisplay.getUniqueId());
            }

            itemDisplay = world.spawn(newLocation, ItemDisplay.class, entity -> {
                entity.setItemStack(item);
                entity.setInvulnerable(true);
                entity.setPersistent(true);
                entity.setGravity(false);
                entity.setNoPhysics(true);
                entity.setSilent(true);
                entity.setVisibleByDefault(true);
                entity.setViewRange(0.2f);

                entity.setMetadata("DisplayShops-Entity", new FixedMetadataValue(DisplayShops.getPluginInstance(), ""));
                entity.getPersistentDataContainer().set(key, PersistentDataType.STRING, "item");

                if (getEntityIds().contains(entity.getUniqueId())) {
                    getEntityIds().add(entity.getUniqueId());
                }
                entity.setTransformationMatrix(new Matrix4f().scale(scale));


            });
        } else {
            ((ItemDisplay) getItemHolder()).setItemStack(item);
        }

        //getItemHolder().setRotation(baseLocation.getYaw() + addYaw, baseLocation.getPitch() + addPitch);

        /*if (getItemHolder().getEquipment() != null) {
            getItemHolder().getEquipment().setHelmet(item, true);
            getItemHolder().addEquipmentLock(EquipmentSlot.HEAD, ArmorStand.LockType.REMOVING_OR_CHANGING);
        }*/

        /*if (getItem() == null || getItem().isDead()) {
            item = world.spawn(destination, Item.class);
            getItem().setItemStack(shop.getShopItem());
            //getItem().teleport(destination); // ensure the item goes where it needs to
        } else {getItem().setItemStack(shop.getShopItem());}

        getItem().setMetadata("DisplayShops-Entity", new FixedMetadataValue(DisplayShops.getPluginInstance(), ""));
        getItem().getPersistentDataContainer().set(key, PersistentDataType.STRING, "item");

        // Set properties to make it not tick
        getItem().setGravity(false);
        getItem().setPickupDelay(Integer.MAX_VALUE); // Prevent pickup
        getItem().setPersistent(true);
        getItem().setUnlimitedLifetime(true);
        getItem().setVisualFire(false);
        getItem().setInvulnerable(true);

        getItemHolder().addPassenger(getItem()); // add item as passenger*/
    }

    private BukkitTask displayTask;

/*    private void rotateDisplay(ItemDisplay display, Matrix4f mat, float scale, int duration) {
        if(displayTask!=null&& !displayTask.isCancelled()){
            return;
        }
        displayTask=new BukkitRunnable() {
            final float rotationIncrement = (float) Math.toRadians(5); // Rotate 5 degrees per tick
            float currentAngle = 0F; // Array to hold current angle
            @Override
            public void run() {
                if (display == null || display.isDead() || !display.isValid()) { // display was removed from the world, abort task
                    cancel();
                    return;
                }

                currentAngle += rotationIncrement; // Increment the angle
                if (currentAngle >= Math.toRadians(360)) {
                    currentAngle -= (float) Math.toRadians(360); // Reset the angle if it completes a full rotation
                }

                ItemStack itemStack = display.getItemStack();
                if (itemStack != null) {
                    if (itemStack.getType().name().contains("SHIELD")) {return;}
                }

                // Update the transformation matrix with the new rotation
                display.setTransformationMatrix(mat.identity().scale(scale).rotateY(currentAngle));
                display.setInterpolationDelay(0); // no delay to the interpolation
                display.setInterpolationDuration(duration); // set the duration of the interpolated rotation
            }
        }.runTaskTimer(DisplayShops.getPluginInstance(), 10, duration);
    }*/

    private void updateGlass(World world, Location baseLocation, double[] appearanceOffsets) {
        // return if glass is supposed to be hidden
        if (DisplayShops.getPluginInstance().getConfig().getBoolean("hide-glass")) return;

        double x = (0.5 + baseLocation.getX() + (appearanceOffsets != null ? appearanceOffsets[0] : 0)),
                y = (1.4 + baseLocation.getY() + (appearanceOffsets != null ? appearanceOffsets[1] : 0)),
                z = (0.5 + baseLocation.getZ() + (appearanceOffsets != null ? appearanceOffsets[2] : 0));

        Location newLocation = new Location(world, x, y, z);

        if (getGlass() == null || getGlass().isDead() || !getGlass().isValid()) {

            for (ItemDisplay itemDisplay : newLocation.getNearbyEntitiesByType(ItemDisplay.class, 1)) {
                if (!itemDisplay.getPersistentDataContainer().has(key)) {continue;}

                String value = itemDisplay.getPersistentDataContainer().get(key, PersistentDataType.STRING);
                if (value == null || !value.equals("glass")) {continue;}

                itemDisplay.remove();
                getEntityIds().remove(itemDisplay.getUniqueId());
            }

            blockDisplay = world.spawn(newLocation, ItemDisplay.class, entity -> {
                entity.setInvulnerable(true);
                entity.setGravity(false);
                entity.setVisibleByDefault(true);
                entity.setPersistent(true);
                entity.setItemStack(new ItemStack(Material.GLASS));
                entity.setTransformationMatrix(new Matrix4f().scale((float) DisplayShops.getPluginInstance().getConfig().getDouble("display-glass-scale")));
                entity.setViewRange(0.2f);

                entity.getPersistentDataContainer().set(key, PersistentDataType.STRING, "glass");
                entity.setMetadata("DisplayShops-Entity", new FixedMetadataValue(DisplayShops.getPluginInstance(), ""));

                if (getEntityIds().contains(entity.getUniqueId())) {getEntityIds().add(entity.getUniqueId());}
            });
        } else {getGlass().teleportAsync(newLocation, PlayerTeleportEvent.TeleportCause.PLUGIN);}
    }

    public String generateText() {
        List<String> hologramFormat;
        if (shop.getShopItem() != null) {
            if (shop.getOwnerUniqueId() == null)
                hologramFormat = DisplayShops.getPluginInstance().getConfig().getStringList("admin-shop-format");
            else hologramFormat = DisplayShops.getPluginInstance().getConfig().getStringList("valid-item-format");
        } else {
            if (shop.getOwnerUniqueId() == null)
                hologramFormat = DisplayShops.getPluginInstance().getConfig().getStringList("admin-invalid-item-format");
            else hologramFormat = DisplayShops.getPluginInstance().getConfig().getStringList("invalid-item-format");
        }

        final String colorCode = DisplayShops.getPluginInstance().getConfig().getString("default-description-color");
        final boolean hidePriceLine = DisplayShops.getPluginInstance().getConfig().getBoolean("price-disabled-hide");

        StringBuilder text = new StringBuilder();

        for (int i = -1; ++i < hologramFormat.size(); ) {
            final String line = hologramFormat.get(i);

            if ((hidePriceLine && ((line.contains("buy-price") && shop.getBuyPrice(true) < 0)
                    || (line.contains("sell-price") && shop.getSellPrice(true) < 0)))
                    || ((line.contains("{description}") && (shop.getDescription() == null || shop.getDescription().equalsIgnoreCase(""))))) {
                continue;
            }

            if (line.contains("{description}") && !(shop.getDescription() == null || shop.getDescription().equalsIgnoreCase(""))) {
                final String[] otherContents = line.split("\\{description}");
                final String prefix = (otherContents.length >= 1 ? otherContents[0] : ""),
                        suffix = (otherContents.length >= 2 ? otherContents[1] : "");

                List<String> descriptionLines = DisplayShops.getPluginInstance().getManager().wrapString(shop.getDescription());
                Collections.reverse(descriptionLines);

                for (int j = -1; ++j < descriptionLines.size(); ) {
                    String descriptionLine = DisplayShops.getPluginInstance().getManager().color(descriptionLines.get(j));
                    descriptionLine = (descriptionLine.contains(ChatColor.COLOR_CHAR + "") ? descriptionLine : (DisplayShops.getPluginInstance().getManager().color(colorCode + descriptionLine)));

                    // lineEntity.text(LegacyComponentSerializer.legacySection().deserialize(
                    if (text.length() > 0) {text.append("\n");}

                    text.append(DisplayShops.getPluginInstance().getManager().color((prefix + descriptionLine + suffix)));
                    // y += 0.3;
                }
                continue;
            }

            if (text.length() > 0) {text.append("\n");}
            text.append(DisplayShops.getPluginInstance().getManager().color((DisplayShops.getPluginInstance().getManager().applyShopBasedPlaceholders(line, shop))));
            // y += 0.3;
        }

        return DisplayShops.getPluginInstance().getManager().color(text.toString());
    }

    private String prevText="";

    private void updateLines(World world, Location baseLocation, String text, double[] appearanceOffsets) {
        double x = (0.5 + baseLocation.getX() + (appearanceOffsets != null ? appearanceOffsets[0] : 0)),
                y = (1.8 + baseLocation.getY() + (appearanceOffsets != null ? appearanceOffsets[1] : 0)),
                z = (0.5 + baseLocation.getZ() + (appearanceOffsets != null ? appearanceOffsets[2] : 0));

        Location location = new Location(world, x, y, z);

        if (getTextDisplay() == null || !getTextDisplay().isValid() || getTextDisplay().isDead()) {

            for (TextDisplay textDisplay : location.getNearbyEntitiesByType(TextDisplay.class, 1)) {
                if (!textDisplay.getPersistentDataContainer().has(key)) {continue;}

                String value = textDisplay.getPersistentDataContainer().get(key, PersistentDataType.STRING);
                if (value == null || !value.equals("line")) {continue;}

                textDisplay.remove();
                getEntityIds().remove(textDisplay.getUniqueId());
            }

            textDisplay = world.spawn(location, TextDisplay.class, entity -> {
                // customize the entity!
                //entity.text(Component.text("Some awesome content", NamedTextColor.BLACK));
                entity.setBillboard(org.bukkit.entity.Display.Billboard.VERTICAL); // pivot only around the vertical axis

                int alpha = 60, red = 0, green = 0, blue = 0;
                String colorLine = DisplayShops.getPluginInstance().getConfig().getString("display-line-color");
                if (colorLine != null && colorLine.contains(",")) {
                    String[] colors = colorLine.split(",");
                    if (colors.length >= 4) {
                        alpha = Math.max(0, Math.min(Integer.parseInt(colors[0]), 100));
                        red = Math.max(0, Math.min(Integer.parseInt(colors[1]), 255));
                        green = Math.max(0, Math.min(Integer.parseInt(colors[2]), 255));
                        blue = Math.max(0, Math.min(Integer.parseInt(colors[3]), 255));
                    }
                }

                entity.setBackgroundColor(Color.fromARGB(alpha, red, green, blue)); // make the background red
                entity.setViewRange(0.2f);
                entity.setAlignment(TextDisplay.TextAlignment.CENTER);
                entity.setInvulnerable(true);
                entity.setPersistent(true);
                entity.setNoPhysics(true);
                entity.setGravity(false);
                entity.setInvisible(true);
                entity.setShadowed(false);
                entity.setSilent(true);
                entity.setVisibleByDefault(false);
                DisplayShops.getPluginInstance().getServer().getScheduler().runTaskAsynchronously(DisplayShops.getPluginInstance(), () -> entity.setText(text));

                //final int longWordCount = DisplayShops.getPluginInstance().getConfig().getInt("description-long-word-wrap");
                //entity.setLineWidth(120);

                entity.setMetadata("DisplayShops-Entity", new FixedMetadataValue(DisplayShops.getPluginInstance(), ""));
                entity.getPersistentDataContainer().set(key, PersistentDataType.STRING, "line");

                if (getEntityIds().contains(entity.getUniqueId())) {getEntityIds().add(entity.getUniqueId());}
            });
        } else {
            if(!prevText.equals(text)) {
                ((TextDisplay) getTextDisplay()).text(SECTION.deserialize(text));
                prevText=text;
            }
            getTextDisplay().teleportAsync(location, PlayerTeleportEvent.TeleportCause.PLUGIN);
        }

        // remove all unused
        //getLines().removeIf(stand -> !usedLines.contains(stand.getEntityId()));
        //usedLines.clear();
    }

    /**
     * Reveals a display to a player given certain circumstances.
     *
     * @param player  The player to show the display to.
     * @param focused Whether the player is looking directly at the shop
     */
    public void show(@NotNull Player player, boolean focused) {
       /* if (!shouldSee(player)) {
            hide(player, true);
            return;
        }*/

        // for (int i = -1; ++i < lines.size(); ) {
        //     TextDisplay lineEntity = lines.get(i);
        //     if (lineEntity == null || lineEntity.isDead()) {continue;}

        if (getTextDisplay() != null && getTextDisplay().isValid()) {
            if (focused) {
                player.showEntity(DisplayShops.getPluginInstance(), getTextDisplay());
            } else {
                player.hideEntity(DisplayShops.getPluginInstance(), getTextDisplay());
            }
        }
        // }

        /*if (getItem() != null) {
            if (!player.canSee(getItem())) {player.showEntity(DisplayShops.getPluginInstance(), getItem());}
        }*/

        if (getItemHolder() != null) {
            player.showEntity(DisplayShops.getPluginInstance(), getItemHolder());
        }

        if (getGlass() != null) {
            player.showEntity(DisplayShops.getPluginInstance(), getGlass());
        }
    }

    /**
     * @param player The player to run checks with.
     * @return Whether the player should be able to see the display
     */
    public boolean shouldSee(@NotNull Player player) {
        return (shop.getBaseLocation().getWorldName().equalsIgnoreCase(player.getWorld().getName()) &&
                shop.getBaseLocation().distance(player.getLocation(), false) <= DisplayShops.getPluginInstance().getConfig().getInt("view-distance"));
    }

    /**
     * Hides the display from the player given certain circumstances.
     *
     * @param player  The player to hide the display from.
     * @param hideAll Whether to hide everything or not.
     */
    public void hide(@NotNull Player player, boolean hideAll) {
        //for (int i = -1; ++i < lines.size(); ) {
        //TextDisplay lineEntity = lines.get(i);
        //if (lineEntity == null || lineEntity.isDead()) {continue;}

        // if (player.canSee(lineEntity)) {
        if (getTextDisplay() != null && getTextDisplay().isValid())
            player.hideEntity(DisplayShops.getPluginInstance(), getTextDisplay());
        //}
        //}

        if (hideAll) {

            if (getItemHolder() != null && getItemHolder().isValid()) {
                //if (player.canSee(getItemHolder())) {
                player.hideEntity(DisplayShops.getPluginInstance(), getItemHolder());
                //}
            }

            if (getGlass() != null && getGlass().isValid()) {
                //if (player.canSee(getGlass())) {
                player.hideEntity(DisplayShops.getPluginInstance(), getGlass());
                //}
            }
        }
    }

    /*public void ClearLines() {
        for (int i = -1; ++i < getLines().size(); ) {
            TextDisplay lineEntity = getLines().get(i);
            if (lineEntity == null || lineEntity.isDead()) {continue;}

            lineEntity.remove();
        }
    }*/

    /*private TextDisplay CreateStand(@NotNull Location location) {
        if (location.getWorld() == null) {return null;}

        return location.getWorld().spawn(location, TextDisplay.class, entity -> {
            // customize the entity!
            //entity.text(Component.text("Some awesome content", NamedTextColor.BLACK));
            entity.setBillboard(org.bukkit.entity.Display.Billboard.VERTICAL); // pivot only around the vertical axis
            //entity.setBackgroundColor(Color.RED); // make the background red

            entity.setAlignment(TextDisplay.TextAlignment.CENTER);
            entity.setInvulnerable(true);
            //entity.setPersistent(true);
            entity.setNoPhysics(true);
            entity.setGravity(false);
            entity.setInvisible(true);
            entity.setShadowed(false);
            entity.setSilent(true);
            entity.setVisibleByDefault(false);

            entity.setMetadata("DisplayShops-Entity", new FixedMetadataValue(DisplayShops.getPluginInstance(), ""));
            entity.getPersistentDataContainer().set(key, PersistentDataType.STRING, "line");

            // see the Display and TextDisplay Javadoc, there are many more options
        });
    }*/

   /* private TextDisplay getNextUnusedLine() {
        for (int i = -1; ++i < getLines().size(); ) {
            TextDisplay lineEntity = getLines().get(i);
            if (usedLines.contains(lineEntity.getEntityId())) {return lineEntity;}
        }
        return null;
    }*/

    // getters & setters
    //public List<TextDisplay> getLines() {return lines;}

    //public Item getItem() {return item;}

    public Shop getShop() {
        return shop;
    }

    public Entity getGlass() {
        return blockDisplay;
    }

    public Entity getItemHolder() {
        return itemDisplay;
    }

    public Entity getTextDisplay() {
        return textDisplay;
    }

    public List<UUID> getEntityIds() {
        return entityIds;
    }

    //public ItemFrame getItemFrame() {return itemFrame;}
}