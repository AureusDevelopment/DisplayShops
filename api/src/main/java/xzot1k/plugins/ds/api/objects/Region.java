/*
 * Copyright (c) 2022 XZot1K, All rights reserved.
 */

package xzot1k.plugins.ds.api.objects;

import org.jetbrains.annotations.NotNull;

public interface Region {

    LocationClone getPointOne();

    void setPointOne(@NotNull LocationClone pointOne);

    LocationClone getPointTwo();

    void setPointTwo(@NotNull LocationClone pointTwo);

}