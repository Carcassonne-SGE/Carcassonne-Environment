package model.bits;

import annotation.BitField;
import annotation.BitPacked;
import annotation.PackedStorage;

/// MeepleLayout
///
/// bit layout for one meeple in the MeepleRegistry
/// stores placement, identity and temporary scoring state
@BitPacked(storage = PackedStorage.INT, useValidBit = true)
public final class MeepleLayout {

    @BitField(bits = 14, valueOffset = 1)
    int globalAreaId;

    @BitField(bits = 7)
    int globalMeepleId;

    @BitField(bits = 4)
    int count;

    @BitField(bits = 1)
    boolean readyToCollect;

    private MeepleLayout() {
    }
}
