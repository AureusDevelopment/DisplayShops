/*
 * Copyright (c) 2021 XZot1K, All rights reserved.
 */

package xzot1k.plugins.ds.api.objects;

public class DRegion implements Region {

    private LocationClone pointOne, pointTwo;

    public LocationClone getPointOne() {
        return pointOne;
    }

    public void setPointOne(LocationClone pointOne) {
        this.pointOne = pointOne;
    }

    public LocationClone getPointTwo() {
        return pointTwo;
    }

    public void setPointTwo(LocationClone pointTwo) {
        this.pointTwo = pointTwo;
    }

}
