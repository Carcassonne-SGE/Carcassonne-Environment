package model.collections;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;

class PositionTileMapUnitTest {

    @Test
    void getReturnsZeroForMissingKey() {
        PositionTileMap map = new PositionTileMap(2);

        assertEquals(0L, map.get(42));
        assertEquals(0, map.size());
    }

    @Test
    void putStoresAndUpdatesValues() {
        PositionTileMap map = new PositionTileMap(2);
        map.put(7, 101L);
        map.put(8, 202L);
        map.put(7, 303L);

        assertEquals(303L, map.get(7));
        assertEquals(202L, map.get(8));
        assertEquals(2, map.size());
    }

    @Test
    void clearRemovesAllEntriesAndResetsSize() {
        PositionTileMap map = new PositionTileMap(1);
        map.put(1, 10L);
        map.put(2, 20L);

        map.clear();

        assertEquals(0, map.size());
        assertEquals(0L, map.get(1));
        assertEquals(0L, map.get(2));
    }

    @Test
    void deepCopyCreatesIndependentMap() {
        PositionTileMap map = new PositionTileMap(2);
        map.put(4, 40L);

        PositionTileMap copy = map.deepCopy();
        copy.put(4, 99L);
        copy.put(5, 50L);

        assertNotSame(map, copy);
        assertEquals(40L, map.get(4));
        assertEquals(0L, map.get(5));
        assertEquals(99L, copy.get(4));
        assertEquals(50L, copy.get(5));
    }

    @Test
    void constructorWithZeroCapacityStillAllowsInsertions() {
        PositionTileMap map = new PositionTileMap(0);
        map.put(9, 900L);

        assertEquals(1, map.size());
        assertEquals(900L, map.get(9));
    }
}
