package model.bits;

import annotation.BitField;
import annotation.BitPacked;
import annotation.PackedStorage;

/// TileLayout
///
/// bit layout for one placed tile on the board
/// stores rotated edges, tile id, position and rotation state
@BitPacked(storage = PackedStorage.LONG, useValidBit = true)
public final class TileLayout {

    @BitField(bits = 8)
    int edges;

    @BitField(bits = 8)
    int tileId;

    @BitField(bits = 8)
    byte y;

    @BitField(bits = 8)
    byte x;

    @BitField(bits = 2)
    int rotation;

    @BitField(bits = 1)
    boolean monastery;

    private TileLayout() {
    }
}
