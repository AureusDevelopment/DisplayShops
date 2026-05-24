package xzot1k.plugins.ds.api.objects;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

public abstract class Appearance implements Comparable<Appearance> {
    private static final List<Appearance> appearances = new ArrayList<>();
    private String id, material, permission;
    private List<String> requirement;
    private double price;
    private double[] offset;

    public Appearance(@NotNull String id, @NotNull String material, double price, @Nullable String permission,
                      double @Nullable [] offset, @Nullable String... requirement) {
        setId(id);
        setMaterial(material);
        setPrice(price);
        setPermission(permission);
        setRequirement(new ArrayList<String>() {{Collections.addAll(this, requirement);}});
        setOffset(offset);
    }

    public static void registerAppearance(@NotNull Appearance appearance) {
        if (!doesAppearanceExist(appearance.getId())) getAppearances().add(appearance);
    }

    public static Appearance getAppearance(@Nullable String id) {
        if (id == null || id.isEmpty()) return null;
        return getAppearances().parallelStream().filter(appearance -> appearance.getId().equalsIgnoreCase(id)).findFirst().orElse(null);
    }

    public static String findAppearance(@NotNull String materialString) {
        Optional<Appearance> appearanceIdOptional = getAppearances().parallelStream().filter(appearance ->
                (materialString.equalsIgnoreCase(appearance.getMaterial()))).findFirst();
        return appearanceIdOptional.map(Appearance::getId).orElse(null);
    }

    public static boolean doesAppearanceExist(@NotNull String id) {
        return (getAppearance(id) != null);
    }

    public static List<Appearance> getAppearances() {
        return appearances;
    }

    public void apply(@NotNull Shop shop, @Nullable Player player) {
    }

    public ItemStack build(@NotNull Player player, @NotNull Shop shop) {
        return null;
    }

    public String getId() {
        return id;
    }

    public void setId(@NotNull String id) {
        this.id = id;
    }

    public String getMaterial() {
        return material;
    }

    public void setMaterial(@NotNull String material) {
        this.material = material;
    }

    public double getPrice() {
        return price;
    }

    public void setPrice(double price) {
        this.price = price;
    }

    public String getPermission() {
        return permission;
    }

    public void setPermission(@Nullable String permission) {
        this.permission = permission;
    }

    public List<String> getRequirement() {
        return requirement;
    }

    public void setRequirement(@NotNull List<String> requirement) {
        this.requirement = requirement;
    }

    public double[] getOffset() {
        return (offset == null ? new double[]{0.5, -0.25, 0.5} : offset);
    }

    public void setOffset(double[] offset) {
        this.offset = offset;
    }
}