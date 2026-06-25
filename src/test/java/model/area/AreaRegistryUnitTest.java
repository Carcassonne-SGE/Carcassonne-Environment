package model.area;

import model.bits.AreaLayoutBit;
import model.enums.AreaType;
import model.enums.EdgeType;
import model.tile.TileSpec;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AreaRegistryUnitTest {

    @Test
    void constructorBuildsAreasForDeckAndStartTile() {
        TileSpec deckTile = tile(0, false, false, false, EdgeType.FIELD, EdgeType.FIELD, EdgeType.FIELD, EdgeType.FIELD);
        TileSpec startTile = tile(1, true, true, false, EdgeType.CASTLE, EdgeType.FIELD, EdgeType.FIELD, EdgeType.FIELD);

        AreaRegistry registry = new AreaRegistry(new TileSpec[]{deckTile}, startTile);

        assertEquals(26, registry.areas.length);
        assertEquals(AreaType.FIELD, Area.getAreaTypeTyped(registry.getArea(0, 0)));
        assertEquals(AreaType.CASTLE, Area.getAreaTypeTyped(registry.getArea(1, 0)));
        assertEquals(AreaType.MONASTERY, Area.getAreaTypeTyped(registry.getMonasteryArea(1)));
    }

    @Test
    void constructorRejectsInvalidDeckTileId() {
        TileSpec invalidDeckTile = tile(2, false, false, false, EdgeType.FIELD, EdgeType.FIELD, EdgeType.FIELD, EdgeType.FIELD);
        TileSpec startTile = tile(1, false, false, false, EdgeType.FIELD, EdgeType.FIELD, EdgeType.FIELD, EdgeType.FIELD);

        assertThrows(IllegalArgumentException.class, () -> new AreaRegistry(new TileSpec[]{invalidDeckTile}, startTile));
    }

    @Test
    void constructorRejectsInvalidStartTileId() {
        TileSpec deckTile = tile(0, false, false, false, EdgeType.FIELD, EdgeType.FIELD, EdgeType.FIELD, EdgeType.FIELD);
        TileSpec invalidStartTile = tile(2, false, false, false, EdgeType.FIELD, EdgeType.FIELD, EdgeType.FIELD, EdgeType.FIELD);

        assertThrows(IllegalArgumentException.class, () -> new AreaRegistry(new TileSpec[]{deckTile}, invalidStartTile));
    }

    @Test
    void findRepresentativeByBitsReturnsCurrentRepresentative() {
        AreaRegistry registry = createRegistryForGlobalMerges();
        registry.merge(0, 3);

        long representative = registry.findRepresentative(registry.getArea(0, 3));

        assertEquals(Area.getGlobalAreaId(registry.getArea(0, 0)), Area.getGlobalAreaId(representative));
    }

    @Test
    void findRepresentativeByBitsReturnsAreaWhenAlreadyRepresentative() {
        AreaRegistry registry = createRegistryForAccessors();
        long representative = registry.getArea(1, 12);

        assertEquals(representative, registry.findRepresentative(representative));
    }

    @Test
    void findRepresentativeByGlobalIdFindsRootThroughChain() {
        AreaRegistry registry = createRegistryForGlobalMerges();
        registry.merge(0, 3);
        registry.merge(0, 6);

        long representative = registry.findRepresentative(6);

        assertEquals(Area.getGlobalAreaId(registry.getArea(0, 0)), Area.getGlobalAreaId(representative));
        assertEquals(0, AreaLayoutBit.getParentGlobalAreaId(registry.areas[6]));
    }

    @Test
    void findRepresentativeByGlobalIdReturnsAreaWhenAlreadyRepresentative() {
        AreaRegistry registry = createRegistryForAccessors();

        assertEquals(registry.getArea(1, 12), registry.findRepresentative(25));
    }

    @Test
    void findRepresentativeByGlobalIdHalvesLongerPaths() {
        AreaRegistry registry = createRegistryForAccessors();
        registry.areas[0] = Area.CreateArea(AreaType.FIELD, 0, 0, false);
        registry.areas[1] = AreaLayoutBit.setParentGlobalAreaId(Area.CreateArea(AreaType.FIELD, 1, 0, false), 0);
        registry.areas[2] = AreaLayoutBit.setParentGlobalAreaId(Area.CreateArea(AreaType.FIELD, 2, 0, false), 1);

        long representative = registry.findRepresentative(2);

        assertEquals(0, Area.getGlobalAreaId(representative));
        assertEquals(0, AreaLayoutBit.getParentGlobalAreaId(registry.areas[2]));
    }

    @Test
    void mergeBalancesAndUpdatesPointsAndCompleteness() {
        AreaRegistry registry = createRegistryForGlobalMerges();
        int areaA = registry.getGlobalAreaId(0, 0);
        int areaB = registry.getGlobalAreaId(0, 3);
        long beforeRep = registry.findRepresentative(areaA);

        registry.merge(areaA, areaB);

        long representative = registry.findRepresentative(areaA);
        assertEquals(2, AreaLayoutBit.getStoredPoints(representative));
        assertEquals(4, AreaLayoutBit.getOpenEdgesCounter(representative));
        assertEquals(6, AreaLayoutBit.getSize(representative));
        assertFalse(Area.isCompleted(representative));
        assertEquals(Area.getGlobalAreaId(beforeRep), AreaLayoutBit.getParentGlobalAreaId(registry.getArea(0, 3)));
    }

    @Test
    void mergeSwapsRepresentativesWhenSecondAreaIsLarger() {
        AreaRegistry registry = createRegistryForAccessors();
        registry.areas[0] = AreaLayoutBit.setSize(Area.CreateArea(AreaType.CASTLE, 0, 0, false), 1);
        registry.areas[13] = AreaLayoutBit.setSize(Area.CreateArea(AreaType.CASTLE, 0, 1, true), 4);

        registry.merge(0, 13);

        long representative = registry.findRepresentative(0);
        assertEquals(13, Area.getGlobalAreaId(representative));
        assertEquals(5, AreaLayoutBit.getSize(representative));
        assertEquals(3, AreaLayoutBit.getStoredPoints(representative));
        assertEquals(13, AreaLayoutBit.getParentGlobalAreaId(registry.areas[0]));
    }

    @Test
    void mergeOfSameAreaOnlyUpdatesCompletenessOnce() {
        AreaRegistry registry = createRegistryForGlobalMerges();
        int areaA = registry.getGlobalAreaId(0, 0);
        int areaB = registry.getGlobalAreaId(0, 3);
        registry.merge(areaA, areaB);

        registry.merge(areaA, areaB);

        long representative = registry.findRepresentative(areaA);
        assertEquals(2, AreaLayoutBit.getOpenEdgesCounter(representative));
        assertEquals(2, AreaLayoutBit.getStoredPoints(representative));
        assertEquals(6, AreaLayoutBit.getSize(representative));
    }

    @Test
    void mergeThrowsForInvalidGlobalIds() {
        AreaRegistry registry = createRegistryForAccessors();

        assertThrows(ArrayIndexOutOfBoundsException.class, () -> registry.merge(-1, 0));
        assertThrows(ArrayIndexOutOfBoundsException.class, () -> registry.merge(0, 26));
    }

    @Test
    void findLocalRepresentativeReturnsRootAndCompressesPath() {
        long[] localAreas = new long[13];
        localAreas[0] = Area.CreateArea(AreaType.FIELD, 0, 0, false);
        localAreas[1] = AreaLayoutBit.setParentGlobalAreaId(Area.CreateArea(AreaType.FIELD, 1, 0, false), 0);
        localAreas[2] = AreaLayoutBit.setParentGlobalAreaId(Area.CreateArea(AreaType.FIELD, 2, 0, false), 1);

        long representative = AreaRegistry.findLocalRepresentative(localAreas, localAreas[2]);

        assertEquals(0, Area.getGlobalAreaId(representative));
        assertEquals(0, AreaLayoutBit.getParentGlobalAreaId(localAreas[2]));
    }

    @Test
    void findLocalRepresentativeReturnsAreaWhenAlreadyRepresentative() {
        long[] localAreas = new long[13];
        localAreas[4] = Area.CreateArea(AreaType.FIELD, 4, 0, false);

        assertEquals(localAreas[4], AreaRegistry.findLocalRepresentative(localAreas, localAreas[4]));
    }

    @Test
    void localMergeMergesDifferentAreasWithoutAddingPoints() {
        long[] localAreas = new long[13];
        localAreas[0] = Area.CreateArea(AreaType.CASTLE, 0, 0, false);
        localAreas[1] = Area.CreateArea(AreaType.CASTLE, 1, 0, true);

        AreaRegistry.localMerge(localAreas, localAreas[0], localAreas[1]);

        assertEquals(0, AreaLayoutBit.getParentGlobalAreaId(localAreas[1]));
        assertEquals(1, AreaLayoutBit.getStoredPoints(localAreas[0]));
        assertEquals(2, AreaLayoutBit.getSize(localAreas[0]));
    }

    @Test
    void localMergeSwapsRepresentativesWhenSecondAreaIsLarger() {
        long[] localAreas = new long[13];
        localAreas[0] = AreaLayoutBit.setSize(Area.CreateArea(AreaType.FIELD, 0, 0, false), 1);
        localAreas[1] = AreaLayoutBit.setSize(Area.CreateArea(AreaType.FIELD, 1, 0, false), 3);

        AreaRegistry.localMerge(localAreas, localAreas[0], localAreas[1]);

        assertEquals(1, Area.getGlobalAreaId(AreaRegistry.findLocalRepresentative(localAreas, localAreas[0])));
        assertEquals(1, AreaLayoutBit.getParentGlobalAreaId(localAreas[0]));
        assertEquals(4, AreaLayoutBit.getSize(localAreas[1]));
    }

    @Test
    void localMergeLeavesSameAreaUntouched() {
        long[] localAreas = new long[13];
        localAreas[0] = Area.CreateArea(AreaType.FIELD, 0, 0, false);
        long before = localAreas[0];

        AreaRegistry.localMerge(localAreas, localAreas[0], localAreas[0]);

        assertEquals(before, localAreas[0]);
    }

    @Test
    void deepCopyClonesInternalAreaArray() {
        AreaRegistry registry = createRegistryForAccessors();

        AreaRegistry copy = registry.deepCopy();
        copy.areas[0] = AreaLayoutBit.setStoredPoints(copy.areas[0], 7);

        assertNotSame(registry.areas, copy.areas);
        assertNotEquals(copy.areas[0], registry.areas[0]);
    }

    @Test
    void getAreaReturnsAreaAtGlobalIndex() {
        AreaRegistry registry = createRegistryForAccessors();

        assertEquals(registry.areas[13], registry.getArea(1, 0));
    }

    @Test
    void getAreaValidatesTileAndAreaBounds() {
        AreaRegistry registry = createRegistryForAccessors();

        assertThrows(IllegalArgumentException.class, () -> registry.getArea(-1, 0));
        assertThrows(IllegalArgumentException.class, () -> registry.getArea(2, 0));
        assertThrows(IllegalArgumentException.class, () -> registry.getArea(0, -1));
        assertThrows(IllegalArgumentException.class, () -> registry.getArea(0, 13));
    }

    @Test
    void getGlobalAreaIdValidatesBounds() {
        AreaRegistry registry = createRegistryForAccessors();

        assertEquals(16, registry.getGlobalAreaId(1, 3));
        assertThrows(IllegalArgumentException.class, () -> registry.getGlobalAreaId(-1, 0));
        assertThrows(IllegalArgumentException.class, () -> registry.getGlobalAreaId(2, 0));
        assertThrows(IllegalArgumentException.class, () -> registry.getGlobalAreaId(0, -1));
        assertThrows(IllegalArgumentException.class, () -> registry.getGlobalAreaId(0, 13));
    }

    @Test
    void getEdgeAreaWithoutRotationRejectsMonasteryIndex() {
        AreaRegistry registry = createRegistryForAccessors();

        assertEquals(registry.getArea(0, 11), registry.getEdgeArea(0, 11));
        assertThrows(IllegalArgumentException.class, () -> registry.getEdgeArea(0, 12));
    }

    @Test
    void getMonasteryAreaReturnsIndexTwelve() {
        AreaRegistry registry = createRegistryForAccessors();

        assertEquals(registry.getArea(1, 12), registry.getMonasteryArea(1));
    }

    @Test
    void getMonasteryAreaValidatesTileBounds() {
        AreaRegistry registry = createRegistryForAccessors();

        assertThrows(IllegalArgumentException.class, () -> registry.getMonasteryArea(-1));
        assertThrows(IllegalArgumentException.class, () -> registry.getMonasteryArea(2));
    }

    @Test
    void getEdgeAreaWithRotationMapsRotatedLocalId() {
        AreaRegistry registry = createRegistryForAccessors();

        assertEquals(registry.getArea(1, 4), registry.getEdgeArea(1, 1, 1));
    }

    @Test
    void getEdgeAreaWithRotationRejectsNegativeRotationResult() {
        AreaRegistry registry = createRegistryForAccessors();

        assertThrows(IllegalArgumentException.class, () -> registry.getEdgeArea(1, 0, -1));
    }

    @Test
    void getEdgeAreaWithRotationWrapsLargeRotations() {
        AreaRegistry registry = createRegistryForAccessors();

        assertEquals(registry.getArea(1, 4), registry.getEdgeArea(1, 1, 5));
    }

    @Test
    void getAreaRotHandlesMonasteryAndRotatedEdgeAreas() {
        AreaRegistry registry = createRegistryForAccessors();

        assertEquals(registry.getMonasteryArea(1), registry.getAreaRot(1, 12, 3));
        assertEquals(registry.getArea(1, 7), registry.getAreaRot(1, 1, 2));
    }

    @Test
    void getAreaRotTreatsOutOfRangeLocalIdsAsEdgeAreas() {
        AreaRegistry registry = createRegistryForAccessors();

        assertEquals(registry.getArea(1, 1), registry.getAreaRot(1, 13, 0));
    }

    @Test
    void isSameAreaUsesRepresentatives() {
        AreaRegistry registry = createRegistryForGlobalMerges();
        registry.merge(0, 3);

        assertTrue(registry.isSameArea(registry.getArea(0, 0), registry.getArea(0, 3)));
        assertFalse(registry.isSameArea(registry.getArea(0, 0), registry.getArea(0, 6)));
    }

    @Test
    void isSameAreaIsTrueForSelfAndTransitiveMerges() {
        AreaRegistry registry = createRegistryForAccessors();
        registry.areas[0] = Area.CreateArea(AreaType.FIELD, 0, 0, false);
        registry.areas[1] = Area.CreateArea(AreaType.FIELD, 1, 0, false);
        registry.areas[2] = Area.CreateArea(AreaType.FIELD, 2, 0, false);
        AreaRegistry.localMerge(registry.areas, registry.areas[0], registry.areas[1]);
        AreaRegistry.localMerge(registry.areas, registry.areas[1], registry.areas[2]);

        assertTrue(registry.isSameArea(registry.areas[0], registry.areas[0]));
        assertTrue(registry.isSameArea(registry.areas[0], registry.areas[2]));
    }

    @Test
    void uncheckedGlobalAndLocalIdHelpersUseFixedStride() {
        assertEquals(31, AreaRegistry.getGlobalAreaIdUnchecked(2, 5));
        assertEquals(5, AreaRegistry.getLocalAreaIdUnchecked(31, 2));
    }

    private static AreaRegistry createRegistryForGlobalMerges() {
        TileSpec deckTile = tile(0, false, false, false, EdgeType.CASTLE, EdgeType.CASTLE, EdgeType.CASTLE, EdgeType.FIELD);
        TileSpec startTile = tile(1, false, false, false, EdgeType.FIELD, EdgeType.FIELD, EdgeType.FIELD, EdgeType.FIELD);
        return new AreaRegistry(new TileSpec[]{deckTile}, startTile);
    }

    private static AreaRegistry createRegistryForAccessors() {
        TileSpec deckTile = tile(0, false, false, false, EdgeType.FIELD, EdgeType.ROAD0, EdgeType.FIELD, EdgeType.CASTLE);
        TileSpec startTile = tile(1, false, true, false, EdgeType.CASTLE, EdgeType.FIELD, EdgeType.ROAD1, EdgeType.FIELD);
        return new AreaRegistry(new TileSpec[]{deckTile}, startTile);
    }

    private static TileSpec tile(int tileId, boolean hasBlazon, boolean isMonastery, boolean connectCastles,
                                 EdgeType left, EdgeType top, EdgeType right, EdgeType bottom) {
        return new TileSpec(new EdgeType[]{left, top, right, bottom}, hasBlazon, isMonastery, connectCastles, tileId, tileId);
    }
}
