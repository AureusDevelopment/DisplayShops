package xzot1k.plugins.ds.api.objects;

import com.nexomc.nexo.api.NexoBlocks;
import com.nexomc.nexo.api.NexoFurniture;
import com.nexomc.nexo.api.NexoItems;
import com.nexomc.nexo.items.ItemBuilder;

import org.bukkit.Axis;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.Directional;
import org.bukkit.block.data.Orientable;
import org.bukkit.block.data.Rotatable;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import xzot1k.plugins.ds.DisplayShops;
import xzot1k.plugins.ds.api.enums.Direction;
import xzot1k.plugins.ds.api.enums.EconomyCallType;
import xzot1k.plugins.ds.core.utils.nms_utils.Ref;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

public class DAppearance extends Appearance {
    private final DisplayShops INSTANCE = DisplayShops.getPluginInstance();

    public DAppearance(@NotNull String id, @NotNull String material, double price, @Nullable String permission,
                       double @Nullable [] offset, @Nullable String... requirement) {
        super(id, material, price, permission, offset, requirement);
        registerAppearance(this);
    }

    public static void loadAppearances() {
        Menu menu = DisplayShops.getPluginInstance().getMenu("appearance");
        if (menu == null) return;

        Object appearanceObject = menu.getConfiguration().get("appearances");
        if (appearanceObject == null) {
            DisplayShops.getPluginInstance().log(Level.WARNING, "No appearances section was found in the \""
                    + menu.getConfiguration().getName() + "\" file.");
            return;
        }

        // convert from old format
        List<?> appearanceList;
        if (appearanceObject instanceof List && (appearanceList = ((List<?>) appearanceObject)).parallelStream().noneMatch((o -> !(o instanceof String)))) {
            menu.getConfiguration().set("appearances", null);
            appearanceObject = menu.getConfiguration().createSection("appearances");

            final ConfigurationSection appearanceSection = (ConfigurationSection) appearanceObject;
            for (int i = -1; ++i < appearanceList.size(); ) {
                final String appearance = (String) appearanceList.get(i);

                // Old Format: 4 args
                // "END_PORTAL_FRAME:-1:50.0:&7Requires the &edisplayshops.bbm.END_PORTAL_FRAME &7permission."

                String material, id;
                double price = 0;
                List<String> requirement = new ArrayList<>();

                if (appearance.contains(":")) {
                    final String[] args = appearance.split(":");
                    material = ((id = args[0]) + ":" + args[1]);
                    if (!DisplayShops.getPluginInstance().getManager().isNotNumeric(args[2]))
                        price = Double.parseDouble(args[2]);
                    if (args.length >= 4) requirement.add(args[3]);
                } else {
                    material = appearance;
                    id = appearance;
                }

                appearanceSection.set((id + ".material"), material);
                appearanceSection.set((id + ".price"), price);
                appearanceSection.set((id + ".permission"), ("displayshops.bbm." + id));
                appearanceSection.set((id + ".requirement"), requirement);
                appearanceSection.set((id + ".offset"), "0.5,-0." + (material.contains("PORTAL_FRAME") ? "4" : "2") + ",0.5");
            }

            menu.save();
        }

        // load new format
        long counter = 0, failed = 0;
        final ConfigurationSection appearanceSection = (ConfigurationSection) appearanceObject;
        for (String id : appearanceSection.getKeys(true)) {
            final ConfigurationSection appearanceOptions = appearanceSection.getConfigurationSection(id);
            if (appearanceOptions == null) continue;

            String material = appearanceOptions.getString("material");
            if (material == null || material.isEmpty()) {
                failed++;
                DisplayShops.getPluginInstance().log(Level.WARNING, "The appearance \"" + id
                        + "\" was unable to load due to a missing/invalid material value.");
                continue;
            }

            double[] offset = null;
            String offsetString = appearanceOptions.getString("offset");
            if (offsetString != null && offsetString.contains(",")) {
                String[] args = offsetString.split(",");
                if (args.length >= 3) {
                    offset = new double[3];
                    if (!DisplayShops.getPluginInstance().getManager().isNotRawNumeric(args[0]))
                        offset[0] = Double.parseDouble(args[0]);
                    if (!DisplayShops.getPluginInstance().getManager().isNotRawNumeric(args[1]))
                        offset[1] = Double.parseDouble(args[1]);
                    if (!DisplayShops.getPluginInstance().getManager().isNotRawNumeric(args[2]))
                        offset[2] = Double.parseDouble(args[2]);
                }
            }

            new DAppearance(id, material, appearanceOptions.getDouble("price"), appearanceOptions.getString("permission"),
                    offset, appearanceOptions.getStringList("requirement").toArray(new String[]{}));
            counter++;
        }

        DisplayShops.getPluginInstance().log(Level.INFO, counter + " shop appearances were loaded into memory."
                + (failed > 0 ? " " + failed + " appearances failed to load." : ""));
    }

    @Override
    public ItemStack build(@NotNull Player player, @NotNull Shop shop) {
        ItemStack itemStack = null;
        if (getMaterial() != null && getMaterial().contains(":")) {
            final String[] args = getMaterial().split(":");
            Material mat = Material.getMaterial(args[0]);
            if (mat != null) {
                int durability = Integer.parseInt(args[1]);
                itemStack = new ItemStack(mat, 1, (byte) (Math.max(durability, 0)));
            } else if (INSTANCE.isItemAdderInstalled()) {
                dev.lone.itemsadder.api.CustomBlock customBlock = dev.lone.itemsadder.api.CustomBlock.getInstance(args[0]);
                if (customBlock != null) itemStack = customBlock.getItemStack();
            } else if (INSTANCE.isNexoInstalled()) {
                if (NexoItems.exists(args[0])) {
                    ItemBuilder builder = NexoItems.itemFromId(args[0]);
                    if (builder != null)
                        itemStack = builder.build();
                }
            }
        } else {
            Material mat = Material.getMaterial(getMaterial());
            if (mat != null) itemStack = new ItemStack(mat);
            else if (INSTANCE.isItemAdderInstalled()) {
                dev.lone.itemsadder.api.CustomBlock customBlock = dev.lone.itemsadder.api.CustomBlock.getInstance(getMaterial());
                if (customBlock != null) {
                    itemStack = customBlock.getItemStack();
                }

            } else if (INSTANCE.isNexoInstalled()) {
                ItemBuilder builder = NexoItems.itemFromId(getMaterial());
                if (builder != null) {
                    itemStack = builder.build();
                }
            }
        }

        if (itemStack == null) return null;

        ItemMeta itemMeta = itemStack.getItemMeta();
        final Menu menu = INSTANCE.getMenu("appearance");
        if (itemMeta == null || menu == null)
            return INSTANCE.updateNBT(itemStack, "ds-bbm", shop.getShopId().toString());

        if (shop.getAppearanceId() != null && shop.getAppearanceId().equalsIgnoreCase(getId())) {
            String selectedName = menu.getConfiguration().getString("selected-format.name");
            final List<String> selectedLore = menu.getConfiguration().getStringList("selected-format.lore");

            if (selectedName != null) {
                selectedName = selectedName.replace("{material}", getId().replace("_", " "));
                itemMeta.setDisplayName(INSTANCE.getManager().color(selectedName));
            }
            itemMeta.setLore(new ArrayList<String>() {{
                for (int i = -1; ++i < selectedLore.size(); )
                    add(INSTANCE.getManager().color(selectedLore.get(i)));
            }});

            if (menu.getConfiguration().getBoolean("selected-format.enchanted")) {
                try {
                    Enchantment enchantment = Enchantment.getByName("DURABILITY");
                    if (enchantment == null) enchantment = Enchantment.UNBREAKING;

                    itemMeta.addEnchant(enchantment, 0, true);
                    itemMeta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
                } catch (Exception e) {
                    e.printStackTrace();
                    INSTANCE.log(Level.WARNING, "Failed to hide the enchantments on the current base-block item"
                            + " for the selection GUI. Please disable this option for your version.");
                }
            }

            itemStack.setItemMeta(itemMeta);
        } else {
            final String unlockedName = menu.getConfiguration().getString("unlocked-format.name"),
                    lockedName = menu.getConfiguration().getString("locked-format.name");
            final List<String> unlockedLore = menu.getConfiguration().getStringList("unlocked-format.lore"),
                    lockedLore = menu.getConfiguration().getStringList("locked-format.lore");

            final DataPack dataPack = INSTANCE.getManager().getDataPack(player);
            final boolean isUnlocked = dataPack.hasUnlockedAppearance(player, getId());
            String name = menu.getConfiguration().getString((isUnlocked ? "unlocked" : "locked") + "-format.name");
            if (name != null) {
                name = name.replace("{material}", getId().replace("_", " "));
                String newName = (isUnlocked ? unlockedName : lockedName);
                if (newName != null)
                    itemMeta.setDisplayName(INSTANCE.getManager().color(newName.replace("{material}", name)));
                itemMeta.setLore(new ArrayList<String>() {{
                    List<String> newLore = (isUnlocked ? unlockedLore : lockedLore);
                    for (int i = -1; ++i < newLore.size(); ) {
                        String line = newLore.get(i);
                        if (!line.equalsIgnoreCase("{requirement}")) {
                            if (line.contains("price}") && getPrice() <= 0) continue; // skip no price
                            add(INSTANCE.getManager().color(INSTANCE.papiText(player, line
                                    .replace("{price}", INSTANCE.getEconomyHandler().format(shop, shop.getCurrencyType(), getPrice(), EconomyCallType.APPEARANCE))
                                    .replace("{raw-price}", INSTANCE.getEconomyHandler().format(shop, shop.getCurrencyType(), getPrice(), EconomyCallType.APPEARANCE))
                                    .replace("{permission}", (getPermission() != null ? getPermission() : ""))
                                    .replace("{id}", (getId() != null ? getId() : "")))));
                        } else {
                            if (getRequirement() != null && !getRequirement().isEmpty()) {
                                for (int j = -1; ++j < getRequirement().size(); ) {
                                    final String rLine = getRequirement().get(j);
                                    if (rLine != null) {
                                        if (line.contains("price}") && getPrice() <= 0) continue; // skip no price
                                        add(INSTANCE.getManager().color(INSTANCE.papiText(player, rLine
                                                .replace("{price}", INSTANCE.getEconomyHandler().format(shop, shop.getCurrencyType(), getPrice(), EconomyCallType.APPEARANCE))
                                                .replace("{raw-price}", INSTANCE.getEconomyHandler().format(shop, shop.getCurrencyType(), getPrice(), EconomyCallType.APPEARANCE))
                                                .replace("{permission}", (getPermission() != null ? getPermission() : ""))
                                                .replace("{id}", (getId() != null ? getId() : "")))));
                                    }
                                }
                            }
                        }
                    }
                }});
                itemStack.setItemMeta(itemMeta);
            }
        }

        return INSTANCE.updateNBT(INSTANCE.updateNBT(itemStack, "ds-bbm", shop.getShopId().toString()), "ds-appearance", getId());
    }


    @Override
    public void apply(@NotNull Shop shop, @Nullable Player player) {
        if (shop.getBaseLocation() == null) return;
        final Location baseBlockLocation = shop.getBaseLocation().asBukkitLocation();
        final Block block = baseBlockLocation.getBlock();

        shop.setAppearanceId(getId());

        String materialName = getMaterial();
        int durability = 0;
        if (materialName.contains(":")) {
            String[] args = materialName.split(":");
            materialName = args[0];
            if (!DisplayShops.getPluginInstance().getManager().isNotNumeric(args[1]))
                durability = Integer.parseInt(args[1]);
        }

        Material material = Material.getMaterial(materialName);
        if (material != null) {
            if (DisplayShops.getPluginInstance().isItemAdderInstalled())
                dev.lone.itemsadder.api.CustomBlock.remove(baseBlockLocation);
            if (DisplayShops.getPluginInstance().isNexoInstalled()) {
                NexoBlocks.remove(baseBlockLocation);
                NexoFurniture.remove(baseBlockLocation);
            }
            block.setType(Material.AIR);
            block.setType(material);

            if (player != null) {
                final boolean isOld = Ref.isOlderThan(13) && !Ref.isMajorNewerThan(26);
                if (isOld) try {
                    @SuppressWarnings("JavaReflectionMemberAccess") Method method = Block.class.getMethod("setData", byte.class);
                    method.invoke(baseBlockLocation.getBlock(), ((byte) (durability < 0 ? oppositeDirectionByte(Direction.getYaw(player)) : durability)));
                } catch (Exception ignored) {
                }
                else {
                    block.setBlockData(INSTANCE.getServer().createBlockData(material));
                    setBlock(block, material, (material.name().contains("SHULKER") ? BlockFace.UP : BlockFace.valueOf(Direction.getYaw(player).name())));
                }
            }
        } else if (DisplayShops.getPluginInstance().isItemAdderInstalled()) {
            dev.lone.itemsadder.api.CustomBlock customBlock = dev.lone.itemsadder.api.CustomBlock.getInstance(materialName);
            if (customBlock != null) {
                block.setType(Material.AIR);
                customBlock.place(baseBlockLocation);
            }
        } else if (DisplayShops.getPluginInstance().isNexoInstalled()) {
            if (NexoBlocks.isCustomBlock(materialName)) {
                block.setType(Material.AIR);
                NexoBlocks.remove(baseBlockLocation);
                NexoBlocks.place(materialName, baseBlockLocation);
            } else if (NexoFurniture.isFurniture(materialName)) {
                block.setType(Material.AIR);
                NexoFurniture.remove(baseBlockLocation);
                float yaw = player == null ? 0f : player.getLocation().getYaw();
                float correctedYaw = ((yaw + 180f) % 360f + 360f) % 360f;
                BlockFace facing = player == null ? BlockFace.SELF : player.getFacing();

                NexoFurniture.place(materialName, baseBlockLocation, correctedYaw, facing);
            }
        }

        if (player != null) {
            String changeSound = INSTANCE.getConfig().getString("immersion-section.shop-bbm-sound");
            if (changeSound != null && !changeSound.equalsIgnoreCase("")) {
                INSTANCE.getVersionUtil().playSound(player, changeSound.toUpperCase().replace(" ", "_")
                        .replace("-", "_"));
            }

            String changeEffect = INSTANCE.getConfig().getString("immersion-section.shop-bbm-particle");
            if (changeEffect != null && !changeEffect.equalsIgnoreCase(""))
                INSTANCE.displayParticle(player, changeEffect.toUpperCase().replace(" ", "_").replace("-", "_"),
                        baseBlockLocation.add(0.5, 0.5, 0.5), 0.5, 0.5, 0.5, 0, 12);
        }
    }

    private String convertBlockFaceToAxis(BlockFace face) {
        switch (face) {
            case NORTH:
            case SOUTH:
                return "Z";
            case UP:
            case DOWN:
                return "Y";
            case EAST:
            case WEST:
            default:
                return "X";
        }
    }

    private void setBlock(Block block, Material material, BlockFace blockFace) {
        block.setType(material);

        org.bukkit.block.data.BlockData blockData = block.getBlockData();
        if (blockData instanceof Directional) {
            ((Directional) blockData).setFacing(blockFace);
            block.setBlockData(blockData);
        }

        if (blockData instanceof Orientable) {
            ((Orientable) blockData).setAxis(Axis.valueOf(convertBlockFaceToAxis(blockFace)));
            block.setBlockData(blockData);
        }

        if (blockData instanceof Rotatable) {
            ((Rotatable) blockData).setRotation(blockFace);
            block.setBlockData(blockData);
        }
    }

    private byte oppositeDirectionByte(Direction direction) {
        for (int i = -1; ++i < Direction.values().length; )
            if (direction == Direction.values()[i]) return (byte) i;
        return 4;
    }

    @Override
    public int compareTo(@NotNull Appearance appearance) {
        return (getId().compareTo(appearance.getId()));
    }
}