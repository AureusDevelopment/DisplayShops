/*
 * Copyright (c) 2022 XZot1K, All rights reserved.
 */

package xzot1k.plugins.ds.api.handlers;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public interface ParticleHandler {
    void displayParticle(@NotNull Player player, @NotNull String particleName, @NotNull Location location, double offsetX, double offsetY, double offsetZ, int speed, int amount);
}