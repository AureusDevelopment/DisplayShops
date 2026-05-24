/*
 * Copyright (c) 2022 XZot1K, All rights reserved.
 */

package xzot1k.plugins.ds.api.handlers;

import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public interface ActionBarHandler {
    void sendActionBar(@NotNull Player player, @NotNull String message);
}