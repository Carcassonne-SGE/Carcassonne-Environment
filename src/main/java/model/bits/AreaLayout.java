package model.bits;

import annotation.BitField;
import annotation.BitPacked;
import annotation.PackedStorage;

/// AreaLayout
///
/// bit layout for one area in the AreaRegistry
/// stores scoring, union-find and meeple related area data
@BitPacked(storage = PackedStorage.LONG, useValidBit = true)
public final class AreaLayout {

    @BitField(bits = 2)
    int areaType;

    @BitField(bits = 4)
    int localAreaId;

    @BitField(bits = 9)
    int tileId;

    @BitField(bits = 8)
    int storedPoints;

    @BitField(bits = 10)
    int openEdgesCounter;

    @BitField(bits = 6)
    int maxMeeples;

    @BitField(bits = 8)
    int size;

    @BitField(bits = 13)
    int parentGlobalAreaId;

    private AreaLayout() {
    }
}
