package model.bits;

import annotation.BitField;
import annotation.BitPacked;
import annotation.PackedStorage;

/// PositionLayout
///
/// bit layout for one board position
/// used as compact x y key in frontier and tile maps
@BitPacked(storage = PackedStorage.INT)
public final class PositionLayout {

    @BitField(bits = 10, valueOffset = 512)
    int x;

    @BitField(bits = 10, valueOffset = 512)
    int y;

    private PositionLayout() {
    }
}
