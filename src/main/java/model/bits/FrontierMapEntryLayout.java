package model.bits;

import annotation.BitField;
import annotation.BitPacked;
import annotation.PackedStorage;

/// FrontierMapEntryLayout
///
/// bit layout for one FrontierMap slot entry
/// stores the packed board position key and the packed edge constraints value
@BitPacked(storage = PackedStorage.LONG, useValidBit = true)
public final class FrontierMapEntryLayout {

    @BitField(bits = 20)
    int key;

    @BitField(bits = 13)
    int value;

    private FrontierMapEntryLayout() {
    }
}
