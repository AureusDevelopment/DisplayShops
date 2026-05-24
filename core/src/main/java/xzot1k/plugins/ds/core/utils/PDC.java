package xzot1k.plugins.ds.core.utils;

import org.bukkit.NamespacedKey;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataHolder;
import org.bukkit.persistence.PersistentDataType;
import xzot1k.plugins.ds.DisplayShops;

import java.util.List;
import java.util.Objects;
import java.util.Set;

@SuppressWarnings("ALL")
public class PDC {
    private final PersistentDataContainer pdc;

    public <T extends PersistentDataHolder> PDC(T t) {
        Objects.requireNonNull(t, "PersistentDataHolder cannot be null!");
        this.pdc = t.getPersistentDataContainer();
    }

    public PDC(PersistentDataContainer pdc) {
        Objects.requireNonNull(pdc, "PersistentDataContainer cannot be null!");
        this.pdc = pdc;
    }

    public void setString(String key, String data) {
        pdc.set(new NamespacedKey(DisplayShops.getPluginInstance(), key), PersistentDataType.STRING, data);
    }

    public void setBoolean(String key, boolean data) {
        pdc.set(new NamespacedKey(DisplayShops.getPluginInstance(), key), PersistentDataType.BOOLEAN, data);
    }

    public void setByte(String key, byte data) {
        pdc.set(new NamespacedKey(DisplayShops.getPluginInstance(), key), PersistentDataType.BYTE, data);
    }

    public void setByteArray(String key, byte[] data) {
        pdc.set(new NamespacedKey(DisplayShops.getPluginInstance(), key), PersistentDataType.BYTE_ARRAY, data);
    }

    public void setInt(String key, int data) {
        pdc.set(new NamespacedKey(DisplayShops.getPluginInstance(), key), PersistentDataType.INTEGER, data);
    }

    public void setIntArray(String key, int[] data) {
        pdc.set(new NamespacedKey(DisplayShops.getPluginInstance(), key), PersistentDataType.INTEGER_ARRAY, data);
    }

    public void setLong(String key, long data) {
        pdc.set(new NamespacedKey(DisplayShops.getPluginInstance(), key), PersistentDataType.LONG, data);
    }

    public void setLongArray(String key, long[] data) {
        pdc.set(new NamespacedKey(DisplayShops.getPluginInstance(), key), PersistentDataType.LONG_ARRAY, data);
    }

    public void setDouble(String key, double data) {
        pdc.set(new NamespacedKey(DisplayShops.getPluginInstance(), key), PersistentDataType.DOUBLE, data);
    }

    public void setFloat(String key, float data) {
        pdc.set(new NamespacedKey(DisplayShops.getPluginInstance(), key), PersistentDataType.FLOAT, data);
    }

    public void setShort(String key, short data) {
        pdc.set(new NamespacedKey(DisplayShops.getPluginInstance(), key), PersistentDataType.SHORT, data);
    }

    public void setTagContainer(String key, PersistentDataContainer data) {
        pdc.set(new NamespacedKey(DisplayShops.getPluginInstance(), key), PersistentDataType.TAG_CONTAINER, data);
    }

    public String getString(String key) {
        return pdc.get(new NamespacedKey(DisplayShops.getPluginInstance(), key), PersistentDataType.STRING);
    }

    public boolean getBoolean(String key) {
        return pdc.get(new NamespacedKey(DisplayShops.getPluginInstance(), key), PersistentDataType.BOOLEAN);
    }

    public byte getByte(String key) {
        return pdc.get(new NamespacedKey(DisplayShops.getPluginInstance(), key), PersistentDataType.BYTE);
    }

    public byte[] getByteArray(String key) {
        return pdc.get(new NamespacedKey(DisplayShops.getPluginInstance(), key), PersistentDataType.BYTE_ARRAY);
    }

    public int getInt(String key) {
        return pdc.get(new NamespacedKey(DisplayShops.getPluginInstance(), key), PersistentDataType.INTEGER);
    }

    public int[] getIntArray(String key) {
        return pdc.get(new NamespacedKey(DisplayShops.getPluginInstance(), key), PersistentDataType.INTEGER_ARRAY);
    }

    public long getLong(String key) {
        return pdc.get(new NamespacedKey(DisplayShops.getPluginInstance(), key), PersistentDataType.LONG);
    }

    public long[] getLongArray(String key) {
        return pdc.get(new NamespacedKey(DisplayShops.getPluginInstance(), key), PersistentDataType.LONG_ARRAY);
    }

    public double getDouble(String key) {
        return pdc.get(new NamespacedKey(DisplayShops.getPluginInstance(), key), PersistentDataType.DOUBLE);
    }

    public float getFloat(String key) {
        return pdc.get(new NamespacedKey(DisplayShops.getPluginInstance(), key), PersistentDataType.FLOAT);
    }

    public short getShort(String key) {
        return pdc.get(new NamespacedKey(DisplayShops.getPluginInstance(), key), PersistentDataType.SHORT);
    }

    public PersistentDataContainer getTagContainer(String key) {
        return pdc.get(new NamespacedKey(DisplayShops.getPluginInstance(), key), PersistentDataType.TAG_CONTAINER);
    }

    public PersistentDataContainer[] getTagContainerArray(String key) {
        return pdc.get(new NamespacedKey(DisplayShops.getPluginInstance(), key), PersistentDataType.TAG_CONTAINER_ARRAY);
    }

    public boolean has(String key) {
        return pdc.has(new NamespacedKey(DisplayShops.getPluginInstance(), key));
    }

    public boolean hasString(String key) {
        return pdc.has(new NamespacedKey(DisplayShops.getPluginInstance(), key), PersistentDataType.STRING);
    }

    public boolean hasBoolean(String key) {
        return pdc.has(new NamespacedKey(DisplayShops.getPluginInstance(), key), PersistentDataType.BOOLEAN);
    }

    public boolean hasByte(String key) {
        return pdc.has(new NamespacedKey(DisplayShops.getPluginInstance(), key), PersistentDataType.BYTE);
    }

    public boolean hasByteArray(String key) {
        return pdc.has(new NamespacedKey(DisplayShops.getPluginInstance(), key), PersistentDataType.BYTE_ARRAY);
    }

    public boolean hasInt(String key) {
        return pdc.has(new NamespacedKey(DisplayShops.getPluginInstance(), key), PersistentDataType.INTEGER);
    }

    public boolean hasIntArray(String key) {
        return pdc.has(new NamespacedKey(DisplayShops.getPluginInstance(), key), PersistentDataType.INTEGER_ARRAY);
    }

    public boolean hasLong(String key) {
        return pdc.has(new NamespacedKey(DisplayShops.getPluginInstance(), key), PersistentDataType.LONG);
    }

    public boolean hasLongArray(String key) {
        return pdc.has(new NamespacedKey(DisplayShops.getPluginInstance(), key), PersistentDataType.LONG_ARRAY);
    }

    public boolean hasDouble(String key) {
        return pdc.has(new NamespacedKey(DisplayShops.getPluginInstance(), key), PersistentDataType.DOUBLE);
    }

    public boolean hasFloat(String key) {
        return pdc.has(new NamespacedKey(DisplayShops.getPluginInstance(), key), PersistentDataType.FLOAT);
    }

    public boolean hasShort(String key) {
        return pdc.has(new NamespacedKey(DisplayShops.getPluginInstance(), key), PersistentDataType.SHORT);
    }

    public boolean hasTagContainer(String key) {
        return pdc.has(new NamespacedKey(DisplayShops.getPluginInstance(), key), PersistentDataType.TAG_CONTAINER);
    }

    public boolean hasTagContainerArray(String key) {
        return pdc.has(new NamespacedKey(DisplayShops.getPluginInstance(), key), PersistentDataType.TAG_CONTAINER_ARRAY);
    }

    public List<String> getKeys() {
        return pdc.getKeys().stream().map(NamespacedKey::getKey).toList();
    }

    public Set<NamespacedKey> getNamespacedKeys() {
        return pdc.getKeys();
    }

    public void remove(String key) {
        pdc.remove(new NamespacedKey(DisplayShops.getPluginInstance(), key));
    }

}
