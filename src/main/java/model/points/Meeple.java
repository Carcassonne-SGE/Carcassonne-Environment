package model.points;

import model.bits.MeepleLayoutBit;

/// Meeple
///
/// helper functions for the packed meeple representation
/// used to create, clear and inspect meeple bits
public final class Meeple {
    private Meeple() {
    }

    /// createMeeple
    ///
    /// creates one free meeple with the given player and local meeple id
    public static int createMeeple(int playerId, int localMeepleId, int meeplesPerPlayer) {
        int globalMeepleId = playerId * meeplesPerPlayer + localMeepleId;
        return MeepleLayoutBit.pack(-1, globalMeepleId, 0, false);
    }

    /// isFree
    ///
    /// returns true if the meeple is currently not placed on any area and this
    /// is the case iff globalAreaId of that meeple == -1
    public static boolean isFree(int meeple) {
        return MeepleLayoutBit.getGlobalAreaId(meeple) == -1;
    }

    /// clear
    ///
    /// clears placement and temporary scoring state of a meeple
    public static int clear(int meeple) {
        int cleared = MeepleLayoutBit.rawSetGlobalAreaId(meeple, -1);
        cleared = MeepleLayoutBit.rawSetCount(cleared, 0);
        return MeepleLayoutBit.rawSetReadyToCollect(cleared, false);
    }

    /// getPlayerIdFromGlobal
    ///
    /// returns the owning player id for a global meeple id
    public static int getPlayerIdFromGlobal(int globalMeepleId, int meeplesPerPlayer) {
        return globalMeepleId / meeplesPerPlayer;
    }
}
