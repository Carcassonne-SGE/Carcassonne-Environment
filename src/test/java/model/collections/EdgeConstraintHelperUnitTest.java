package model.collections;

import model.bits.EdgeConstraintLayoutBit;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EdgeConstraintHelperUnitTest {

    @Test
    void setBitsByDirWritesEachDirection() {
        int bits = 0;
        bits = EdgeConstraintHelper.setBitsByDir(bits, 1, 0);
        bits = EdgeConstraintHelper.setBitsByDir(bits, 2, 1);
        bits = EdgeConstraintHelper.setBitsByDir(bits, 3, 2);
        bits = EdgeConstraintHelper.setBitsByDir(bits, 4, 3);

        assertEquals(1, EdgeConstraintLayoutBit.getLeft(bits));
        assertEquals(2, EdgeConstraintLayoutBit.getTop(bits));
        assertEquals(3, EdgeConstraintLayoutBit.getRight(bits));
        assertEquals(4, EdgeConstraintLayoutBit.getBottom(bits));
    }

    @Test
    void setBitsByDirWrapsDirectionByMask() {
        int bits = EdgeConstraintHelper.setBitsByDir(0, 5, 5);

        assertEquals(5, EdgeConstraintLayoutBit.getTop(bits));
    }

    @Test
    void matchesRequiredNonZeroAcceptsMatchingAndUnspecifiedConstraints() {
        int required = EdgeConstraintLayoutBit.rawPack(1, 0, 3, 0);
        int candidate = EdgeConstraintLayoutBit.rawPack(1, 5, 3, 2);

        assertTrue(EdgeConstraintHelper.matchesRequiredNonZero(required, candidate));
    }

    @Test
    void matchesRequiredNonZeroRejectsMismatchOnRequiredEdge() {
        int required = EdgeConstraintLayoutBit.rawPack(1, 0, 3, 0);
        int candidate = EdgeConstraintLayoutBit.rawPack(2, 5, 3, 2);

        assertFalse(EdgeConstraintHelper.matchesRequiredNonZero(required, candidate));
    }

    @Test
    void encodeTileEdgesOffsetsEveryTwoBitValueByOne() {
        int tileEdges = 0 | (1 << 2) | (2 << 4) | (3 << 6);

        int encoded = EdgeConstraintHelper.encodeTileEdges(tileEdges);

        assertEquals(1, EdgeConstraintLayoutBit.getLeft(encoded));
        assertEquals(2, EdgeConstraintLayoutBit.getTop(encoded));
        assertEquals(3, EdgeConstraintLayoutBit.getRight(encoded));
        assertEquals(4, EdgeConstraintLayoutBit.getBottom(encoded));
    }
}
