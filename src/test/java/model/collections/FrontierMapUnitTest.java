package model.collections;

import model.bits.EdgeConstraintLayoutBit;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FrontierMapUnitTest {

    @Test
    void addConstraintCreatesAndAccumulatesConstraintsPerDirection() {
        FrontierMap map = new FrontierMap(1);

        map.addConstraint(12, 2, 0);
        map.addConstraint(12, 1, 1);
        map.addConstraint(12, 0, 2);
        map.addConstraint(12, 2, 3);

        int value = map.get(12);
        assertEquals(3, EdgeConstraintLayoutBit.getLeft(value));
        assertEquals(2, EdgeConstraintLayoutBit.getTop(value));
        assertEquals(1, EdgeConstraintLayoutBit.getRight(value));
        assertEquals(3, EdgeConstraintLayoutBit.getBottom(value));
        assertEquals(1, map.size());
    }

    @Test
    void getReturnsZeroForMissingPosition() {
        FrontierMap map = new FrontierMap(1);

        assertEquals(0, map.get(5));
    }

    @Test
    void removeDeletesExistingEntryAndReturnsWhetherItWasPresent() {
        FrontierMap map = new FrontierMap(1);
        map.addConstraint(7, 1, 0);

        assertTrue(map.remove(7));
        assertFalse(map.remove(7));
        assertEquals(0, map.get(7));
        assertEquals(0, map.size());
    }

    @Test
    void slotCountGrowsWhenMapRehashes() {
        FrontierMap map = new FrontierMap(1);
        int initialSlots = map.slotCount();

        map.addConstraint(1, 0, 0);
        map.addConstraint(2, 1, 0);

        assertTrue(map.slotCount() > initialSlots);
        assertEquals(1, EdgeConstraintLayoutBit.getLeft(map.get(1)));
        assertEquals(2, EdgeConstraintLayoutBit.getLeft(map.get(2)));
    }

    @Test
    void getNextIndexSkipsEmptyAndDeletedSlots() {
        FrontierMap map = new FrontierMap(2);
        map.addConstraint(1, 0, 0);
        map.addConstraint(2, 1, 1);
        map.remove(1);

        int first = map.getNextIndex(0);
        int second = map.getNextIndex(first + 1);

        assertTrue(first >= 0);
        assertEquals(2, map.getPositionAt(first));
        assertEquals(FrontierMap.EMPTY, second);
    }

    @Test
    void getValueAtAndGetPositionAtReturnEmptyForInvalidOrUnusedSlots() {
        FrontierMap map = new FrontierMap(1);
        map.addConstraint(9, 2, 3);
        int usedIndex = map.getNextIndex(0);

        assertEquals(9, map.getPositionAt(usedIndex));
        assertEquals(map.get(9), map.getValueAt(usedIndex));
        assertEquals(FrontierMap.EMPTY, map.getPositionAt(-1));
        assertEquals(FrontierMap.EMPTY, map.getValueAt(-1));
        assertEquals(FrontierMap.EMPTY, map.getPositionAt(map.slotCount()));
        assertEquals(FrontierMap.EMPTY, map.getValueAt(map.slotCount()));
    }

    @Test
    void deepCopyCreatesIndependentFrontierMap() {
        FrontierMap map = new FrontierMap(2);
        map.addConstraint(3, 1, 0);

        FrontierMap copy = map.deepCopy();
        copy.addConstraint(4, 2, 1);
        copy.remove(3);

        assertNotSame(map, copy);
        assertTrue(map.get(3) != 0);
        assertEquals(0, map.get(4));
        assertEquals(0, copy.get(3));
        assertTrue(copy.get(4) != 0);
    }

    @Test
    void addConstraintReusesPositionInsteadOfGrowingSize() {
        FrontierMap map = new FrontierMap(1);
        map.addConstraint(5, 0, 0);
        map.addConstraint(5, 1, 1);

        assertEquals(1, map.size());
        assertEquals(1, EdgeConstraintLayoutBit.getLeft(map.get(5)));
        assertEquals(2, EdgeConstraintLayoutBit.getTop(map.get(5)));
    }
}
