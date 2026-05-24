/*
 * Copyright (c) 2021 XZot1K, All rights reserved.
 */

package xzot1k.plugins.ds.api.objects;

import org.bukkit.Location;
import org.bukkit.World;
import org.jetbrains.annotations.NotNull;
import xzot1k.plugins.ds.DisplayShops;

public class LClone implements LocationClone {

    private DisplayShops pluginInstance;
    private String worldName;
    private double x, y, z, yaw, pitch;

    public LClone(Location location) {
        setPluginInstance(DisplayShops.getPluginInstance());
        setWorldName(location.getWorld() == null ? "" : location.getWorld().getName());
        setX(location.getX());
        setY(location.getY());
        setZ(location.getZ());
        setYaw(location.getYaw());
        setPitch(location.getPitch());
    }

    public LClone(String worldName, double x, double y, double z, double yaw, double pitch) {
        setPluginInstance(DisplayShops.getPluginInstance());
        setWorldName(worldName);
        setX(x);
        setY(y);
        setZ(z);
        setYaw(yaw);
        setPitch(pitch);
    }

    /**
     * Checks if the passed location is similar to this location (Block Coords).
     *
     * @param location Location passed.
     * @return The result in true or false format.
     */
    public boolean isSame(@NotNull Location location) {
        return location.getWorld() != null && location.getWorld().getName().equals(getWorldName())
                && location.getBlockX() == (int) getX() && location.getBlockY() == (int) getY() && location.getBlockZ() == (int) getZ();
    }

    /**
     * Checks if the passed location is similar to this location (Doesn't check yaw and pitch).
     *
     * @param location Location passed.
     * @return The result in true or false format.
     */
    public boolean isSameNormal(@NotNull Location location) {
        return location.getWorld() != null && location.getWorld().getName().equals(getWorldName())
                && location.getX() == getX() && location.getY() == getY() && location.getZ() == getZ();
    }

    /**
     * Gets if the two location clones are identical
     *
     * @param location The location to compare to.
     * @return Whether they are identical.
     */
    public boolean isSame(@NotNull LocationClone location) {
        return location.getWorldName() != null && location.getWorldName().equals(getWorldName())
                && location.getX() == (int) getX() && location.getY() == (int) getY() && location.getZ() == (int) getZ();
    }

    /**
     * Gets all the location clone as a bukkit location.
     *
     * @return The bukkit location.
     */
    public Location asBukkitLocation() {
        World world = getPluginInstance().getServer().getWorld(getWorldName());
        if (world == null) return null;
        return new Location(world, getX(), getY(), getZ(), (float) getYaw(), (float) getPitch());
    }

    public double distance(@NotNull LocationClone location, boolean checkYAxis) {
        final double highX = Math.max(getX(), location.getX()), highY = Math.max(getY(), location.getY()), highZ = Math.max(getZ(), location.getZ()),
                lowX = Math.min(getX(), location.getX()), lowY = Math.min(getY(), location.getY()), lowZ = Math.min(getZ(), location.getZ());
        return Math.sqrt(Math.pow((highX - lowX), 2) + (checkYAxis ? Math.pow(highY - lowY, 2) : 0) + Math.pow((highZ - lowZ), 2));
    }

    public double distance(@NotNull Location location, boolean checkYAxis) {
        final double highX = Math.max(getX(), location.getX()), highY = Math.max(getY(), location.getY()), highZ = Math.max(getZ(), location.getZ()),
                lowX = Math.min(getX(), location.getX()), lowY = Math.min(getY(), location.getY()), lowZ = Math.min(getZ(), location.getZ());
        return Math.sqrt(Math.pow((highX - lowX), 2) + (checkYAxis ? Math.pow(highY - lowY, 2) : 0) + Math.pow((highZ - lowZ), 2));
    }

    public double distanceBlock(@NotNull Location location) {
        final double highX = Math.max(getX(), location.getX()), highZ = Math.max(getZ(), location.getZ()),
                lowX = Math.min(getX(), location.getX()), lowZ = Math.min(getZ(), location.getZ());
        return Math.sqrt(Math.pow((highX - lowX), 2) + Math.pow((highZ - lowZ), 2));
    }

    /**
     * Returns the location's data in a single line string.
     *
     * @return The location string.
     */
    @Override
    public String toString() {
        return getWorldName().replace("\"", "\\\"").replace("'", "\\'")
                + "," + getX() + "," + getY() + "," + getZ() + "," + getYaw() + "," + getPitch();
    }

    private DisplayShops getPluginInstance() {
        return pluginInstance;
    }

    private void setPluginInstance(DisplayShops pluginInstance) {
        this.pluginInstance = pluginInstance;
    }

    public String getWorldName() {
        return worldName;
    }

    public void setWorldName(@NotNull String worldName) {
        this.worldName = worldName;
    }

    public double getX() {
        return x;
    }

    public void setX(double x) {
        this.x = x;
    }

    public double getY() {
        return y;
    }

    public void setY(double y) {
        this.y = y;
    }

    public double getZ() {
        return z;
    }

    public void setZ(double z) {
        this.z = z;
    }

    public double getYaw() {
        return yaw;
    }

    public void setYaw(double yaw) {
        this.yaw = yaw;
    }

    public double getPitch() {
        return pitch;
    }

    public void setPitch(double pitch) {
        this.pitch = pitch;
    }

}