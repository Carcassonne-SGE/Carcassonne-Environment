package model.collections;

import model.bits.EdgeConstraintLayoutBit;

/// EdgeConstraintHelper
///
/// helper functions for packed edge constraints
/// used by frontier and placement validation code
public final class EdgeConstraintHelper {
    private EdgeConstraintHelper() {
    }

    /// setBitsByDir
    ///
    /// writes one edge constraint into the packed value for a direction
    public static int setBitsByDir(int bits, int edge, int dir) {
        // there are 4 rotations & 3 is quick % 4 to loop around
        return switch (dir & 3) {
            case 0 -> EdgeConstraintLayoutBit.rawSetLeft(bits, edge);
            case 1 -> EdgeConstraintLayoutBit.rawSetTop(bits, edge);
            case 2 -> EdgeConstraintLayoutBit.rawSetRight(bits, edge);
            default -> EdgeConstraintLayoutBit.rawSetBottom(bits, edge);
        };
    }

    /// matchesRequiredNonZero
    ///
    /// checks if all non zero constraints of a are matched by b
    public static boolean matchesRequiredNonZero(int a, int b) {
        int left = EdgeConstraintLayoutBit.getLeft(a);
        if (left != 0 && left != EdgeConstraintLayoutBit.getLeft(b)) return false;

        int top = EdgeConstraintLayoutBit.getTop(a);
        if (top != 0 && top != EdgeConstraintLayoutBit.getTop(b)) return false;

        int right = EdgeConstraintLayoutBit.getRight(a);
        if (right != 0 && right != EdgeConstraintLayoutBit.getRight(b)) return false;

        int bottom = EdgeConstraintLayoutBit.getBottom(a);
        return bottom == 0 || bottom == EdgeConstraintLayoutBit.getBottom(b);
    }

    /// encodeTileEdges
    ///
    /// converts packed tile edges into the frontier constraint encoding
    public static int encodeTileEdges(int tileEdges) {
        int left = (tileEdges & 0x3) + 1;
        int top = ((tileEdges >>> 2) & 0x3) + 1;
        int right = ((tileEdges >>> 4) & 0x3) + 1;
        int bottom = ((tileEdges >>> 6) & 0x3) + 1;
        return EdgeConstraintLayoutBit.rawPack(left, top, right, bottom);
    }
}
