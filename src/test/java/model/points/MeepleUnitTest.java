package model.points;

import model.bits.MeepleLayoutBit;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MeepleUnitTest {

    @Test
    void createMeepleInitializesFreeMeepleWithDerivedGlobalId() {
        int meeple = Meeple.createMeeple(2, 3, 7);

        assertEquals(-1, MeepleLayoutBit.getGlobalAreaId(meeple));
        assertEquals(17, MeepleLayoutBit.getGlobalMeepleId(meeple));
        assertEquals(0, MeepleLayoutBit.getCount(meeple));
        assertFalse(MeepleLayoutBit.getReadyToCollect(meeple));
        assertTrue(Meeple.isFree(meeple));
    }

    @Test
    void isFreeReturnsFalseForPlacedMeeple() {
        int meeple = Meeple.createMeeple(0, 1, 5);
        meeple = MeepleLayoutBit.rawSetGlobalAreaId(meeple, 9);

        assertFalse(Meeple.isFree(meeple));
    }

    @Test
    void clearResetsPlacementCountAndCollectState() {
        int meeple = MeepleLayoutBit.pack(10, 4, 3, true);

        int cleared = Meeple.clear(meeple);

        assertEquals(-1, MeepleLayoutBit.getGlobalAreaId(cleared));
        assertEquals(4, MeepleLayoutBit.getGlobalMeepleId(cleared));
        assertEquals(0, MeepleLayoutBit.getCount(cleared));
        assertFalse(MeepleLayoutBit.getReadyToCollect(cleared));
        assertTrue(Meeple.isFree(cleared));
    }

    @Test
    void getPlayerIdFromGlobalUsesMeeplesPerPlayerStride() {
        assertEquals(0, Meeple.getPlayerIdFromGlobal(2, 7));
        assertEquals(1, Meeple.getPlayerIdFromGlobal(7, 7));
        assertEquals(3, Meeple.getPlayerIdFromGlobal(22, 7));
    }
}
