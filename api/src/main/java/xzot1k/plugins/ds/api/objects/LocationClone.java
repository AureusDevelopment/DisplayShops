/*
 * Copyright (c) 2022 XZot1K, All rights reserved.
 */

package xzot1k.plugins.ds.api.objects;

import org.bukkit.Location;
import org.jetbrains.annotations.NotNull;

public interface LocationClone {

    /**
     * Checks if the passed location is similar to this location (Block Coords).
     *
     * @param location Location passed.
     * @return The result in true or false format.
     */
    boolean isSame(@NotNull Location location);

    /**
     * Checks if the passed location is similar to this location (Doesn't check yaw and pitch).
     *
     * @param location Location passed.
     * @return The result in true or false format.
     */
    boolean isSameNormal(@NotNull Location location);

    /**
     * Gets if the two location clones are identical
     *
     * @param location The location to compare to.
     * @return Whether they are identical.
     */
    boolean isSame(@NotNull LocationClone location);

    /**
     * Gets all the location clone as a bukkit location.
     *
     * @return The bukkit location.
     */
    Location asBukkitLocation();

    double distance(@NotNull LocationClone location, boolean checkYAxis);

    double distance(@NotNull Location location, boolean checkYAxis);

    double distanceBlock(@NotNull Location location);

    /**
     * Returns the location's data in a single line string.
     *
     * @return The location string.
     */
    @Override
    String toString();

    String getWorldName();

    void setWorldName(@NotNull String worldName);

    double getX();

    void setX(double x);

    double getY();

    void setY(double y);

    double getZ();

    void setZ(double z);

    double getYaw();

    void setYaw(double yaw);

    double getPitch();

    void setPitch(double pitch);

}