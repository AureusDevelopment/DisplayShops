/*
 * Copyright (c) 2022 XZot1K, All rights reserved.
 */

package xzot1k.plugins.ds.api.enums;

import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public enum Direction {
    NORTH, EAST, SOUTH, WEST;

    public static Direction getYaw(@NotNull Player player) {
        return values()[Math.round(player.getLocation().getYaw() / 90f) & 0x3];
    }
}