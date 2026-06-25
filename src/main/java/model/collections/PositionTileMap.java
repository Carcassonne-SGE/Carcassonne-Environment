package model.collections;

import model.bits.PositionTileMapLayoutBit;

import java.util.Arrays;
import java.util.Iterator;

/// PositionTileMap
///
/// small hashmap from packed board positions to long values
/// used to store placed tiles by their position key in the state of the game
public final class PositionTileMap implements Iterable<Long> {
    private final int mask;
    private final int maxElements;
    private final long[] table; // 0L = empty
    private int size;

    /// PositionTileMap
    ///
    /// creates the internal hashmap with enough slots for the expected number of elements
    public PositionTileMap(int maxElements) {
        this.maxElements = maxElements;

        int slots = 1;
        int need = (maxElements * 10) / 6 + 1; // load ~0.6
        while (slots < need) slots <<= 1;

        this.mask = slots - 1;
        this.table = new long[slots];
        this.size = 0;
    }

    private PositionTileMap(int maxElements, int mask, long[] table, int size) {
        this.maxElements = maxElements;
        this.mask = mask;
        this.table = table;
        this.size = size;
    }

    /// mix
    ///
    /// hashes a packed position key to spread entries over the table
    private static int mix(int x) {
        x ^= (x >>> 16);
        x *= 0x7feb352d;
        x ^= (x >>> 15);
        x *= 0x846ca68b;
        x ^= (x >>> 16);
        return x;
    }

    /// get
    ///
    /// returns the value stored for a position
    /// uses linear probing and returns 0 if no entry exists
    public long get(int key) {
        int idx = mix(key) & mask;
        while (true) {
            long e = table[idx];
            // empty means key was not inserted in this probe chain
            if (e == 0L) return 0L;
            // found matching entry return stored value
            if (PositionTileMapLayoutBit.getKey(e) == key) return PositionTileMapLayoutBit.getValue(e);
            // continue probing
            idx = (idx + 1) & mask;
        }
    }

    /// put
    ///
    /// stores or updates the value for a position
    /// uses linear probing
    public void put(int key, long value) {
        int idx = mix(key) & mask;
        while (true) {
            long e = table[idx];
            if (e == 0L) {
                // insert new entry
                table[idx] = PositionTileMapLayoutBit.rawPack(key, value);
                size++;
                return;
            }
            if (PositionTileMapLayoutBit.getKey(e) == key) {
                // key already exists overwrite value
                table[idx] = PositionTileMapLayoutBit.rawPack(key, value);
                return;
            }
            // continue probing
            idx = (idx + 1) & mask;
        }
    }

    /// clear
    ///
    /// removes all stored entries
    public void clear() {
        // fast overwrite of all fields
        Arrays.fill(table, 0L);
        size = 0;
    }

    /// size
    ///
    /// returns the number of stored entries
    public int size() {
        return size;
    }

    /// deepCopy
    ///
    /// returns an independent copy of this map
    public PositionTileMap deepCopy() {
        return new PositionTileMap(maxElements, mask, table.clone(), size);
    }

    @Override
    public Iterator<Long> iterator() {
        return new Iterator<>() {
            private int idx = 0;

            @Override
            public boolean hasNext() {
                while (idx < table.length && table[idx] == 0L) idx++;
                return idx < table.length;
            }

            @Override
            public Long next() {
                if (!hasNext()) throw new java.util.NoSuchElementException();
                return table[idx++];
            }
        };
    }
}
