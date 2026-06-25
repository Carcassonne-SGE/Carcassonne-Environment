package model.collections;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ActionManagerMapUnitTest {

    @Test
    void addStoresMappingsAndGetFindsExistingValues() {
        ActionManagerMap map = new ActionManagerMap();
        map.add(4, 9);
        map.add(7, 2);

        assertEquals(9, map.get(4));
        assertEquals(2, map.get(7));
        assertEquals(-1, map.get(3));
    }

    @Test
    void containsValueChecksStoredValuesOnly() {
        ActionManagerMap map = new ActionManagerMap();
        map.add(1, 5);
        map.add(2, 8);

        assertTrue(map.containsValue(5));
        assertTrue(map.containsValue(8));
        assertFalse(map.containsValue(9));
    }

    @Test
    void clearResetsSizeAndMakesLookupsMiss() {
        ActionManagerMap map = new ActionManagerMap();
        map.add(3, 6);
        map.add(4, 7);

        map.clear();

        assertEquals(0, map.size());
        assertEquals(-1, map.get(3));
        assertFalse(map.containsValue(6));
    }

    @Test
    void keyAtAndValueAtExposeStoredOrder() {
        ActionManagerMap map = new ActionManagerMap();
        map.add(10, 1);
        map.add(11, 12);

        assertEquals(10, map.keyAt(0));
        assertEquals(1, map.valueAt(0));
        assertEquals(11, map.keyAt(1));
        assertEquals(12, map.valueAt(1));
    }
}
