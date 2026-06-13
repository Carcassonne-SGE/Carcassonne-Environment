package model.collections;

import java.util.Arrays;

public class SeenFlagMap {
    // used as a very long bit array where each bit is a boolean at a index
    private long[] seen = new long[0];

    /// ensureSeen
    ///
    /// seen is a long array where each bit is used as flag that can later be set or checked. Is very memory efficient
    /// this function ensures that the array has at least the capacity needed for the given flagsCount may have more
    /// capacity
    ///
    /// ensures the seen array is empty
    /// @param flagsCount number of flags you want to store (does not equal length of array)
    public void ensureSeen(int flagsCount) {
        // number of longs needed is ceil(flagsCount / 64) = (flagsCount +63)/64 =  (flagsCount +63) >>> 6
        int size = (flagsCount + 63) >>> 6;
        if (seen.length < size) {
            seen = new long[size];
        }
        Arrays.fill(seen, 0);
    }

    /// notSeenAndSet
    ///
    /// returns true if the flag at the position pos is not set to true in the seen array.
    /// Ensures that after the call the pos is flagged.
    ///
    /// pos is not the long[] array index but the logical index, the bit index
     public boolean notSeenAndSet(int pos) {
        int word = pos >>> 6;           // upper bits gives in which array entry the pos is stored
        long mask = 1L << (pos & 63);   // has 1 at the exact pos position (pos & 63) is fast modulo operation to get the lower bits
        long v = seen[word];
        if ((v & mask) != 0L) {
            // the bit was 1 which means it was flagged
            return false;
        }
        seen[word] = v | mask;
        // flag the bit in the word using the mask
        return true;
    }
}
