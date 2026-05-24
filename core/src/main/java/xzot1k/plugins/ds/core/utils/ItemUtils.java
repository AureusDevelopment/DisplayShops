package xzot1k.plugins.ds.core.utils;

import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import xzot1k.plugins.ds.DisplayShops;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class ItemUtils {


    public static ItemStack getItemStack(@Nullable ConfigurationSection section) {
        if (section == null)
            throw new RuntimeException("Section can't be null!");
        Material m = getMaterial(section.getString("Material", "BEDROCK"));
        if (m == null)
            throw new RuntimeException("Invalid material: " + section.getString("Material", "BEDROCK"));
        String itemName = colorize(section.getString("DisplayName", "&9DefaultItemName"));
        ItemStack item = new ItemStack(m);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(itemName);
        List<String> lore = colorize(section.getStringList("Lore"));
        meta.setLore(lore);
        meta.setCustomModelData(section.getInt("ModelData", 0));
        item.setItemMeta(meta);
        return item;
    }

    @Nullable
    private static Material getMaterial(String material) {
        return Material.matchMaterial(material);
    }

    private static String colorize(String a) {
        return DisplayShops.getPluginInstance().getManager().color(a);
    }

    private static List<String> colorize(Collection<String> collection) {
        List<String> list = new ArrayList<>();
        for (String a : collection) {
            list.add(colorize(a));
        }
        return list;
    }

}
