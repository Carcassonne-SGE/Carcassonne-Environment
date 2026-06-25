package model.collections;

import model.bits.EdgeConstraintLayoutBit;
import model.bits.FrontierMapEntryLayoutBit;

import java.util.Arrays;

/// FrontierMap
///
/// As the Frontier i define all positions that are adjacent to a currently placed tile. So all
/// possible actions are in the frontier. But not all actions in the frontier are currently possible.
///
/// My original implementation used a naive ArrayList but i saw that this was very slow. The is placement
/// valid function was very very slow. So this class aims to allow for a very fast check if a action is possible
///
/// This Class is basically a hashmap that stores for a position the EdgeConstraints. So that when i
/// do a placement i only need to check if those constraints are satisfied or not.
///
/// Note: Uses as values everywhere int but they are packed using EdgeConstraintLayout
public final class FrontierMap {
    // special values to indicate special cases
    public static final int EMPTY = Integer.MIN_VALUE;
    private static final long EMPTY_SLOT = 0L;
    private static final long DELETED_SLOT = 1L;

    // internal array used for the hashmap. table.length = 2^a for some a. So needs
    // to be power of 2
    private long[] table;

    // table.length - 1 used for very fast % modulo table.length
    private int mask;

    // number of current elements in the hashmap
    private int size;
    // live count + tombstones count
    private int used;
    private int resizeAt;

    /// FrontierMap
    ///
    /// Public constructor that takes the expected number of elements. Number of
    /// elements is not fixed
    /// but can grow if needed
    public FrontierMap(int expectedSize) {
        int slots = 1;
        int need = (expectedSize * 10) / 6 + 1;
        while (slots < need)
            slots <<= 1;
        init(slots);
    }

    /// addConstraint
    ///
    /// If no constraint for that position is present it is created and if already
    /// exists
    /// the position entry is updated
    ///
    /// @param position where the new constraint should be added
    /// @param edge the edgeType or constraint that is added so the value
    /// @param dir at which direction is the constraint added
    public void addConstraint(int position, int edge, int dir) {
        int v = get(position);
        if (v == 0) {
            v = EdgeConstraintLayoutBit.rawPack(0, 0, 0, 0);
        }
        v = EdgeConstraintHelper.setBitsByDir(v, edge + 1, dir);
        put(position, v);
    }

    /// get
    ///
    /// returns the constraints stored for a position
    /// uses linear probing and returns 0 if no entry exists
    public int get(int key) {
        int idx = hash(key);
        while (true) {
            long entry = table[idx];
            // empty means the key was never inserted in this probe chain
            if (entry == EMPTY_SLOT)
                return 0;
            // found the matching entry return its constraints
            if (entry != DELETED_SLOT && FrontierMapEntryLayoutBit.getKey(entry) == key) {
                return FrontierMapEntryLayoutBit.getValue(entry);
            }
            // continue probing
            idx = (idx + 1) & mask;
        }
    }

    /// put
    ///
    /// stores or updates the constraints for a position
    /// uses linear probing and reuses deleted slots if possible
    private void put(int key, int value) {
        // resize if too small
        if (used >= resizeAt) {
            rehash((mask + 1) << 1);
        }

        int idx = hash(key);
        int firstDeleted = -1;
        while (true) {
            long entry = table[idx];

            if (entry == EMPTY_SLOT) {
                // prefer an earlier tombstone if one was seen
                if (firstDeleted != -1) {
                    idx = firstDeleted;
                } else {
                    used++;
                }
                // insert new entry
                table[idx] = FrontierMapEntryLayoutBit.rawPack(key, value);
                size++;
                return;
            }

            if (entry == DELETED_SLOT) {
                // remember first tombstone for possible reuse
                if (firstDeleted == -1)
                    firstDeleted = idx;
            } else if (FrontierMapEntryLayoutBit.getKey(entry) == key) {
                // key already exists just overwrite value
                table[idx] = FrontierMapEntryLayoutBit.rawPack(key, value);
                return;
            }

            // continue probing
            idx = (idx + 1) & mask;
        }
    }

    /// remove
    ///
    /// removes the constraints for a position
    /// returns true iff an entry was found and deleted
    public boolean remove(int key) {
        int idx = hash(key);
        while (true) {
            long entry = table[idx];
            // empty means key does not exist
            if (entry == EMPTY_SLOT)
                return false;
            if (entry != DELETED_SLOT && FrontierMapEntryLayoutBit.getKey(entry) == key) {
                // keep probe chains intact by writing a tombstone
                table[idx] = DELETED_SLOT;
                size--;
                return true;
            }
            // continue probing
            idx = (idx + 1) & mask;
        }
    }

    /// size
    ///
    /// @return number of elements in the FrontierMap
    public int size() {
        return size;
    }

    /// slotCount
    /// Note that slotCount is the number of elements in the array in the underlying
    /// hashmap
    /// @return the current size of the hashmap long array
    /// Note: again tradeoff between data encapsulation and performance
    public int slotCount() {
        return table.length;
    }

    /// getNextIndex
    ///
    /// takes an index and iterates from there until a nonempty slot is found
    /// @param startIndex from where is the search started
    /// @return if such a position as found return that index if not return
    /// EMPTY(public const of that class)
    public int getNextIndex(int startIndex) {
        int idx = Math.max(0, startIndex);
        for (; idx < table.length; idx++) {
            long entry = table[idx];
            if (entry != EMPTY_SLOT && entry != DELETED_SLOT) {
                return idx;
            }
        }
        return EMPTY;
    }

    /// getValueAt
    ///
    /// returns a value at an index but checks if it is in range on filled
    /// value means here the constraints
    /// Note: not an optimal solution to hide the internal state but tradeoff
    /// between speed and data encapsulation
    public int getValueAt(int index) {
        if (index < 0 || index >= table.length) {
            return EMPTY;
        }
        long entry = table[index];
        if (entry == EMPTY_SLOT || entry == DELETED_SLOT) {
            return EMPTY;
        }
        return FrontierMapEntryLayoutBit.getValue(entry);
    }

    /// getPositionAt
    /// @param index checks for validity of the index so not out of range if out of
    /// range or something return EMPTY
    /// @return returns the key at an index which means at an index what position is
    /// there
    /// Note: not an optimal solution to hide the internal state but tradeoff
    /// between speed and data encapsulation
    public int getPositionAt(int index) {
        if (index < 0 || index >= table.length) {
            return EMPTY;
        }
        long entry = table[index];
        if (entry == EMPTY_SLOT || entry == DELETED_SLOT) {
            return EMPTY;
        }
        return FrontierMapEntryLayoutBit.getKey(entry);
    }

    /// Private DeepCopy constructor allows for setting of private fields
    private FrontierMap(long[] table, int mask, int size, int used, int resizeAt) {
        this.table = table;
        this.mask = mask;
        this.size = size;
        this.used = used;
        this.resizeAt = resizeAt;
    }

    /// public deepCopy method used to get a independent instance
    public FrontierMap deepCopy() {
        return new FrontierMap(table.clone(), mask, size, used, resizeAt);
    }

    /// init
    ///
    /// creates a long map for the hashmap and sets all values to the empty_slot
    /// sets mask size and used
    private void init(int slots) {
        table = new long[slots];
        Arrays.fill(table, EMPTY_SLOT);
        mask = slots - 1;
        size = 0;
        used = 0;
        resizeAt = (slots * 6) / 10;
    }

    /// hash
    ///
    /// maps an key to an index in the array
    private int hash(int x) {
        x ^= (x >>> 16);
        x *= 0x7feb352d;
        x ^= (x >>> 15);
        x *= 0x846ca68b;
        x ^= (x >>> 16);
        return x & mask;
    }

    /// rehash
    ///
    /// internal helper function that creates a new hashmap long[] which might be
    /// large
    /// and inserts them again. Which cleans the Deleted_slot and adjust to larger
    /// size
    private void rehash(int newSlots) {
        long[] old = table;
        init(newSlots);

        for (long entry : old) {
            if (entry != EMPTY_SLOT && entry != DELETED_SLOT) {
                put(FrontierMapEntryLayoutBit.getKey(entry), FrontierMapEntryLayoutBit.getValue(entry));
            }
        }
    }
}
