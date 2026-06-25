package model.collections;

/// ActionManagerMap
///
/// small temporary map used during possible action calculation and other parts
/// maps neighbour area representatives to local area representatives
public class ActionManagerMap {
    private static final int KEY_MASK = 0x1FFF;
    private static final int VALUE_SHIFT = 13;

    private final int[] entries = new int[12];
    private int size;

    /// clear
    ///
    /// removes all stored mappings
    public void clear() {
        size = 0;
    }

    /// add
    ///
    /// stores one key value mapping
    ///
    /// @param key source key
    /// @param value mapped value
    public void add(int key, int value) {
        entries[size++] = key | (value << VALUE_SHIFT);
    }

    /// get
    ///
    /// returns the mapped value for a key or -1 if not found
    ///
    /// @param key key to search for
    /// @return mapped value or -1
    public int get(int key) {
        // simple linear search
        for (int i = 0; i < size; i++) {
            int e = entries[i];
            if ((e & KEY_MASK) == key) {
                return e >>> VALUE_SHIFT;
            }
        }
        return -1;
    }

    /// containsValue
    ///
    /// @param value you want to check if it is in the map
    /// @return true if there exists some key with that value
    public boolean containsValue(int value){
        for(int i = 0; i < size; i++){
            int e = entries[i];
            if((e >>> VALUE_SHIFT) == value){
                return true;
            }
        }
        return false;
    }

    /// size
    ///
    /// returns the number of stored mappings
    ///
    /// @return number of entries
    public int size() {
        return size;
    }

    /// keyAt
    ///
    /// returns the key at an internal index
    ///
    /// @param i internal index
    /// @return stored key
    public int keyAt(int i) {
        return entries[i] & KEY_MASK;
    }

    /// valueAt
    ///
    /// returns the value at an internal index
    ///
    /// @param i internal index
    /// @return stored value
    public int valueAt(int i) {
        return entries[i] >>> VALUE_SHIFT;
    }
}
