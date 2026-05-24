/*
 * Copyright (c) 2022 XZot1K, All rights reserved.
 */

package xzot1k.plugins.ds.exceptions;

import org.jetbrains.annotations.NotNull;

public class RayTraceException extends Exception {
    /**
     *
     */
    private static final long serialVersionUID = 1L;

    public RayTraceException(@NotNull String message) {
        super(message);
    }
}