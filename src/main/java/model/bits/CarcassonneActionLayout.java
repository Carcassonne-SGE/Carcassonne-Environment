package model.bits;

import annotation.BitField;
import annotation.BitPacked;
import annotation.PackedStorage;

/// CarcassonneActionLayout
///
/// bit layout for one action in the SGE api
/// stores either a player placement action or a env draw action
@BitPacked(storage = PackedStorage.INT, useValidBit = true)
public final class CarcassonneActionLayout {

    @BitField(bits = 8, valueOffset = 128)
    int x;

    @BitField(bits = 8, valueOffset = 128)
    int y;

    @BitField(bits = 2)
    int rotation;

    @BitField(bits = 4, valueOffset = 1)
    int areaId;

    @BitField(bits = 8)
    int tileId;

    @BitField(bits = 1)
    boolean isAction;

    private CarcassonneActionLayout() {
    }
}
