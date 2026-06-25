package model.collections;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SeenFlagMapUnitTest {

    @Test
    void notSeenAndSetReturnsTrueThenFalseForSamePosition() {
        SeenFlagMap seenFlagMap = new SeenFlagMap();
        seenFlagMap.ensureSeen(10);

        assertTrue(seenFlagMap.notSeenAndSet(3));
        assertFalse(seenFlagMap.notSeenAndSet(3));
    }

    @Test
    void ensureSeenClearsExistingFlagsAndKeepsLargeCapacityUsable() {
        SeenFlagMap seenFlagMap = new SeenFlagMap();
        seenFlagMap.ensureSeen(65);
        seenFlagMap.notSeenAndSet(0);
        seenFlagMap.notSeenAndSet(64);

        seenFlagMap.ensureSeen(1);

        assertTrue(seenFlagMap.notSeenAndSet(0));
        assertTrue(seenFlagMap.notSeenAndSet(64));
    }

    @Test
    void ensureSeenAllowsFlagsAcrossWordBoundaries() {
        SeenFlagMap seenFlagMap = new SeenFlagMap();
        seenFlagMap.ensureSeen(130);

        assertTrue(seenFlagMap.notSeenAndSet(63));
        assertTrue(seenFlagMap.notSeenAndSet(64));
        assertTrue(seenFlagMap.notSeenAndSet(129));
        assertFalse(seenFlagMap.notSeenAndSet(63));
        assertFalse(seenFlagMap.notSeenAndSet(64));
        assertFalse(seenFlagMap.notSeenAndSet(129));
    }
}
