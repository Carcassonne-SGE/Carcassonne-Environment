package model.points;

import model.area.Area;
import model.area.AreaRegistry;
import model.bits.MeepleLayoutBit;
import model.enums.EdgeType;
import model.tile.TileSpec;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MeepleRegistryUnitTest {

    @Test
    void constructorCreatesAllMeeplesAndGettersExposeThem() {
        MeepleRegistry registry = new MeepleRegistry(2, 3);

        assertEquals(3, registry.getMeeplesPerPlayerCount());
        assertEquals(6, registry.getMeeples().length);
        assertEquals(0, MeepleLayoutBit.getGlobalMeepleId(registry.getMeepleRaw(0, 0)));
        assertEquals(2, MeepleLayoutBit.getGlobalMeepleId(registry.getMeepleRaw(0, 2)));
        assertEquals(3, MeepleLayoutBit.getGlobalMeepleId(registry.getMeepleRaw(1, 0)));
        assertEquals(registry.getMeepleRaw(1, 1), registry.getMeepleRawByGlobalId(4));
    }

    @Test
    void findFreeMeepleReturnsMeepleOrZeroWhenNoneRemain() {
        MeepleRegistry registry = new MeepleRegistry(1, 1);
        int freeMeeple = registry.findFreeMeeple(0);

        assertTrue(freeMeeple != 0);

        registry.place(freeMeeple, 5);

        assertEquals(0, registry.findFreeMeeple(0));
    }

    @Test
    void countFreeMeeplesTracksPlacedAndClearedMeeples() {
        MeepleRegistry registry = new MeepleRegistry(1, 2);
        int firstMeeple = registry.getMeepleRaw(0, 0);

        assertEquals(2, registry.countFreeMeeples(0));

        registry.place(firstMeeple, 4);
        assertEquals(1, registry.countFreeMeeples(0));

        registry.clearMeepleByGlobalId(MeepleLayoutBit.getGlobalMeepleId(firstMeeple));
        assertEquals(2, registry.countFreeMeeples(0));
    }

    @Test
    void hasPlayerMeepleOnMatchesAreaByCurrentImplementation() {
        MeepleRegistry registry = new MeepleRegistry(1, 2);
        AreaRegistry areas = createIndependentAreaRegistry();

        assertTrue(registry.hasPlayerMeepleOn(0, areas.getArea(0, 0)));
        assertFalse(registry.hasPlayerMeepleOn(0, areas.getArea(0, 5)));
    }

    @Test
    void findMeeplesOnAreaCountCountsMeeplesByRepresentative() {
        MeepleRegistry registry = new MeepleRegistry(2, 2);
        AreaRegistry areas = createIndependentAreaRegistry();
        int first = registry.getMeepleRaw(0, 0);
        int second = registry.getMeepleRaw(0, 1);

        registry.place(first, Area.getGlobalAreaId(areas.getArea(0, 0)));
        registry.place(second, Area.getGlobalAreaId(areas.getArea(0, 3)));
        areas.merge(0, 3);

        assertEquals(2, registry.findMeeplesOnAreaCount(areas, areas.findRepresentative(0), 0));
        assertEquals(0, registry.findMeeplesOnAreaCount(areas, areas.findRepresentative(0), 1));
    }

    @Test
    void findMeeplePlayerIdOnExactAreaReturnsOwnerOrMinusOne() {
        MeepleRegistry registry = new MeepleRegistry(2, 2);
        AreaRegistry areas = createIndependentAreaRegistry();
        int playerOneMeeple = registry.getMeepleRaw(1, 0);
        int targetAreaId = Area.getGlobalAreaId(areas.getArea(0, 3));

        registry.place(playerOneMeeple, targetAreaId);

        assertEquals(1, registry.findMeeplePlayerIdOnExactArea(targetAreaId, 2));
        assertEquals(-1, registry.findMeeplePlayerIdOnExactArea(Area.getGlobalAreaId(areas.getArea(1, 3)), 2));
    }

    @Test
    void deepCopyCreatesIndependentMeepleArray() {
        MeepleRegistry registry = new MeepleRegistry(1, 2);
        MeepleRegistry copy = registry.deepCopy();

        copy.place(copy.getMeepleRaw(0, 0), 8);

        assertNotSame(registry.getMeeples(), copy.getMeeples());
        assertEquals(-1, MeepleLayoutBit.getGlobalAreaId(registry.getMeepleRaw(0, 0)));
        assertEquals(8, MeepleLayoutBit.getGlobalAreaId(copy.getMeepleRaw(0, 0)));
    }

    @Test
    void placeAndClearMeepleByGlobalIdUpdateStoredMeeple() {
        MeepleRegistry registry = new MeepleRegistry(1, 1);
        int meeple = registry.getMeepleRaw(0, 0);

        registry.place(meeple, 11);
        assertEquals(11, MeepleLayoutBit.getGlobalAreaId(registry.getMeepleRawByGlobalId(0)));

        registry.clearMeepleByGlobalId(0);
        assertEquals(-1, MeepleLayoutBit.getGlobalAreaId(registry.getMeepleRawByGlobalId(0)));
        assertEquals(0, MeepleLayoutBit.getCount(registry.getMeepleRawByGlobalId(0)));
    }

    @Test
    void getMeeplesReturnsInternalArrayReference() {
        MeepleRegistry registry = new MeepleRegistry(1, 1);
        int[] raw = registry.getMeeples();
        raw[0] = MeepleLayoutBit.rawSetGlobalAreaId(raw[0], 12);

        assertArrayEquals(raw, registry.getMeeples());
        assertEquals(12, MeepleLayoutBit.getGlobalAreaId(registry.getMeepleRawByGlobalId(0)));
    }

    private static AreaRegistry createIndependentAreaRegistry() {
        TileSpec deckTile = tile(0, EdgeType.CASTLE, EdgeType.CASTLE, EdgeType.FIELD, EdgeType.FIELD);
        TileSpec startTile = tile(1, EdgeType.FIELD, EdgeType.FIELD, EdgeType.CASTLE, EdgeType.CASTLE);
        return new AreaRegistry(new TileSpec[]{deckTile}, startTile);
    }

    private static TileSpec tile(int tileId, EdgeType left, EdgeType top, EdgeType right, EdgeType bottom) {
        return new TileSpec(new EdgeType[]{left, top, right, bottom}, false, false, false, tileId, tileId);
    }
}
