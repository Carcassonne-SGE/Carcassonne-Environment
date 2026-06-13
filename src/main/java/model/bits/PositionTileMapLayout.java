package model.bits;

import annotation.BitField;
import annotation.BitPacked;
import annotation.PackedStorage;

/// PositionTileMapLayout
///
/// bit layout for one PositionTileMap entry
/// stores a packed position key together with the mapped long value
@BitPacked(storage = PackedStorage.LONG, useValidBit = true)
public final class PositionTileMapLayout {

    @BitField(bits = 20)
    int key;

    @BitField(bits = 43)
    long value;

    private PositionTileMapLayout() {
    }
}
