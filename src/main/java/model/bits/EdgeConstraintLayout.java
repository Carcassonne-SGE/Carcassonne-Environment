package model.bits;

import annotation.BitField;
import annotation.BitPacked;
import annotation.PackedStorage;

/// EdgeConstraintLayout
///
/// bit layout for frontier placement constraints
/// stores the required edge type for each of the four directions
@BitPacked(storage = PackedStorage.INT, useValidBit = true)
public final class EdgeConstraintLayout {

    @BitField(bits = 3)
    int left;

    @BitField(bits = 3)
    int top;

    @BitField(bits = 3)
    int right;

    @BitField(bits = 3)
    int bottom;

    private EdgeConstraintLayout() {
    }
}
