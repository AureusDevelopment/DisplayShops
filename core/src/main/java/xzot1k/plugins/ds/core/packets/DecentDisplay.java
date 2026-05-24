package xzot1k.plugins.ds.core.packets;

import eu.decentsoftware.holograms.api.DHAPI;
import eu.decentsoftware.holograms.api.holograms.Hologram;
import eu.decentsoftware.holograms.api.holograms.HologramLine;
import eu.decentsoftware.holograms.api.holograms.HologramPage;
import eu.decentsoftware.holograms.api.holograms.objects.HologramObject;
import eu.decentsoftware.holograms.api.utils.items.HologramItem;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import xzot1k.plugins.ds.DisplayShops;
import xzot1k.plugins.ds.api.objects.Shop;

import java.util.*;

public class DecentDisplay {

    public final Shop shop;

    public Hologram hologram;

    public DecentDisplay(Shop shop) {
        this.shop = shop;
    }

    public static void ClearAllEntities() {
        for (Map.Entry<UUID, DecentDisplay> entry : DisplayShops.getPluginInstance().getDisplayManager().getDHDisplays().entrySet()) {
            DecentDisplay display = entry.getValue();
            display.Clear();
        }
    }

    public void Clear() {
        if (getHologram() != null) {getHologram().delete();} else {
            Hologram foundHologram = DHAPI.getHologram("ds-" + getShop().getShopId());
            if (foundHologram != null) {foundHologram.delete();}
        }
    }

    public void update(World world, List<String> generatedText, float itemScale, double itemOffsetX, double itemOffsetY, double itemOffsetZ, double[] offsets) {
        if (getShop() == null) return;

        Location baseLocation = getShop().getBaseLocation().asBukkitLocation();
        if (baseLocation == null) return;

        if (getHologram() == null) {
            Hologram foundHologram = DHAPI.getHologram("ds-" + getShop().getShopId());
            if (foundHologram != null) {setHologram(foundHologram);} else {
                setHologram(DHAPI.createHologram("ds-" + getShop().getShopId(), baseLocation));
            }
        }

        updateItem(world, baseLocation, itemScale, itemOffsetX, itemOffsetY, itemOffsetZ, offsets);
        // updateGlass(world, baseLocation, offsets);
        updateLines(world, baseLocation, generatedText, offsets);
    }

    public void delete() {
        DisplayShops.getPluginInstance().getDisplayManager().getShopDisplays().remove(shop.getOwnerUniqueId());
        Clear();
    }

    private void updateItem(World world, Location baseLocation, float scale, double x, double y, double z, double[] appearanceOffsets) {
        // final float yaw = baseLocation.getYaw();
        final ItemStack item = (shop.getShopItem() != null ? shop.getShopItem() : Display.barrier);
      /*  final boolean isBlock = item.getType().isBlock() && item.getType() != Material.BARRIER,
                allowsSpinning = DisplayShops.getPluginInstance().getConfig().getBoolean("allow-item-spinning");*/

        //float addYaw = 0f, addPitch = 0f;
        Location destination = baseLocation.clone();

       /* if (isBlock) {destination.add(0.5, 0.4, 0.5);}

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

        }*/

        HologramPage page = getHologram().getPages().first();
        HologramLine hologramLine = page.getLines().stream().filter(line -> line.getItem() != null).findFirst().orElse(null);

        if (hologramLine != null) {
            hologramLine.setItem(new HologramItem("#ICON:" + HologramItem.fromItemStack(item).getContent()));
        } else {
            HologramLine hologram = DHAPI.addHologramLine(getHologram(), item);
        }


       /* if (getItemHolder() == null || getItemHolder().isDead() || !getItemHolder().isValid()) {
            baseLocation.setYaw(addYaw);

            Location newLocation = baseLocation.clone().add(0.5 + x + (appearanceOffsets != null ? appearanceOffsets[0] : 0),
                    1.4 + y + (appearanceOffsets != null ? appearanceOffsets[1] : 0), 0.5 + z + (appearanceOffsets != null ? appearanceOffsets[2] : 0));

            for (ItemDisplay itemDisplay : newLocation.getNearbyEntitiesByType(ItemDisplay.class, 1)) {
                if (!itemDisplay.getPersistentDataContainer().has(key)) {continue;}

                String value = itemDisplay.getPersistentDataContainer().get(key, PersistentDataType.STRING);
                if (value == null || !value.equals("item")) {continue;}

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

                if (getEntityIds().contains(entity.getUniqueId())) {getEntityIds().add(entity.getUniqueId());}

                Matrix4f mat = new Matrix4f().scale(scale);
                entity.setTransformationMatrix(mat);

                if (DisplayShops.getPluginInstance().getConfig().getBoolean("allow-item-spinning")) {rotateDisplay(entity, mat, scale, 5);}
            });
        } else {((ItemDisplay) getItemHolder()).setItemStack(item);}*/

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

    private void updateGlass(World world, Location baseLocation, double[] appearanceOffsets) {
        // return if glass is supposed to be hidden
     /*   if (DisplayShops.getPluginInstance().getConfig().getBoolean("hide-glass")) return;

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
    */
    }

    public List<String> generateText() {
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

        List<String> lines = new ArrayList<>();

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

                    lines.add(DisplayShops.getPluginInstance().getManager().color((prefix + descriptionLine + suffix)));
                    // y += 0.3;
                }
                continue;
            }

            lines.add(DisplayShops.getPluginInstance().getManager().color((DisplayShops.getPluginInstance().getManager().applyShopBasedPlaceholders(line, shop))));
            // y += 0.3;
        }

        //Collections.reverse(lines);
        return lines;
    }

    private void updateLines(World world, Location baseLocation, List<String> text, double[] appearanceOffsets) {
       /* double x = (0.5 + baseLocation.getX() + (appearanceOffsets != null ? appearanceOffsets[0] : 0)),
                y = (1.8 + baseLocation.getY() + (appearanceOffsets != null ? appearanceOffsets[1] : 0)),
                z = (0.5 + baseLocation.getZ() + (appearanceOffsets != null ? appearanceOffsets[2] : 0));*/

        // Location location = new Location(world, x, y, z);

        HologramPage page = getHologram().getPages().first();

        List<HologramLine> linesToRemove = new ArrayList<HologramLine>() {{
            for (HologramLine line : page.getLines()) {
                if (line.getItem() == null && line.getEntity() == null) {continue;}
                add(line);
            }
        }};
        linesToRemove.forEach(HologramObject::delete);
        linesToRemove.clear();

        text.forEach(line -> {
            HologramLine hLine = DHAPI.insertHologramLine(getHologram(), 0, line);
        });

       /* if (getTextDisplay() == null || !getTextDisplay().isValid() || getTextDisplay().isDead()) {

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
                entity.text(LegacyComponentSerializer.legacySection().deserialize(text));

                //final int longWordCount = DisplayShops.getPluginInstance().getConfig().getInt("description-long-word-wrap");
                //entity.setLineWidth(120);

                entity.setMetadata("DisplayShops-Entity", new FixedMetadataValue(DisplayShops.getPluginInstance(), ""));
                entity.getPersistentDataContainer().set(key, PersistentDataType.STRING, "line");

                if (getEntityIds().contains(entity.getUniqueId())) {getEntityIds().add(entity.getUniqueId());}
            });
        } else {
            ((TextDisplay) getTextDisplay()).text(LegacyComponentSerializer.legacySection().deserialize(text));
            getTextDisplay().teleportAsync(location, PlayerTeleportEvent.TeleportCause.PLUGIN);
        }*/

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
        getHologram().getPages().first().getLines().forEach(line -> {
            if (line.getItem() != null || line.getEntity() != null) {
                if (!line.isVisible(player)) {line.show(player);}
                return;
            }

            if (focused) {
                if (!line.isVisible(player)) {line.show(player);}
            } else {
                if (line.isVisible(player)) {line.hide(player);}
            }
        });
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
        getHologram().getPages().first().getLines().forEach(line -> {
            if (line.getItem() != null || line.getEntity() != null) {
                if (hideAll && line.isVisible(player)) {line.hide(player);}
                return;
            }

            if (line.isVisible(player)) {line.hide(player);}
        });
    }

    // getters & setters
    public Shop getShop() {
        return shop;
    }

    public Hologram getHologram() {
        return hologram;
    }

    public void setHologram(Hologram hologram) {
        this.hologram = hologram;
    }
}