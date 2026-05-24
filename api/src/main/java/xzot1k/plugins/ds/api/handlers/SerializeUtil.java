/*
 * Copyright (c) 2022 XZot1K, All rights reserved.
 */

package xzot1k.plugins.ds.api.handlers;

import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

public interface SerializeUtil {

    /**
     * Convert an item to a string including all NBT.
     *
     * @param itemStack The item to serialize.
     * @return The serialized string.
     */
    String toString(@NotNull ItemStack itemStack);

    /**
     * Convert a string back into an item including all NBT.
     *
     * @param itemString The serialized item string.
     * @return The itemstack object.
     */
    ItemStack toItem(@NotNull String itemString);

    /**
     * Convert an item to a JSON string format for in-game messages.
     *
     * @param itemStack The item to convert.
     * @return The JSON string (can return 'NULL').
     */
    String toJSON(@NotNull ItemStack itemStack);

    /**
     * @param itemStack The item to obtain NBT from.
     * @param nbtTag    The NBT tag.
     * @return The value associated to the NBT tag on the item.
     */
    String getNBT(@NotNull ItemStack itemStack, @NotNull String nbtTag);

    /**
     * @param itemStack The item to update NBT on.
     * @param nbtTag    The NBT tag to update.
     * @param value     The value to associate with the NBT tag.
     * @return The finalized item.
     */
    ItemStack updateNBT(@NotNull ItemStack itemStack, @NotNull String nbtTag, @NotNull String value);

}