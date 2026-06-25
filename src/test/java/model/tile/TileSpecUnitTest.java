package model.tile;

import model.area.Area;
import model.area.AreaRegistry;
import model.bits.AreaLayoutBit;
import model.enums.AreaType;
import model.enums.Direction;
import model.enums.EdgeType;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TileSpecUnitTest {

    @Test
    void constructorRejectsNullWrongLengthAndNullEntries() {
        assertThrows(IllegalArgumentException.class, () -> new TileSpec(null, false, false, false, 0, 0));
        assertThrows(IllegalArgumentException.class,
                () -> new TileSpec(new EdgeType[]{EdgeType.FIELD, EdgeType.FIELD, EdgeType.FIELD}, false, false, false, 0, 0));
        assertThrows(IllegalArgumentException.class,
                () -> new TileSpec(new EdgeType[]{EdgeType.FIELD, null, EdgeType.FIELD, EdgeType.FIELD}, false, false, false, 0, 0));
    }

    @Test
    void calculateAreasMergesAllFieldAreasIntoOneRepresentative() {
        TileSpec tileSpec = new TileSpec(new EdgeType[]{EdgeType.FIELD, EdgeType.FIELD, EdgeType.FIELD, EdgeType.FIELD}, false, false, false, 0, 7);

        long[] areas = tileSpec.calculateAreas();
        long representative = AreaRegistry.findLocalRepresentative(areas, areas[0]);

        for (int i = 0; i < 12; i++) {
            assertEquals(AreaType.FIELD, Area.getAreaTypeTyped(areas[i]));
            assertEquals(Area.getGlobalAreaId(representative), Area.getGlobalAreaId(AreaRegistry.findLocalRepresentative(areas, areas[i])));
        }
        assertEquals(0L, areas[12]);
    }

    @Test
    void calculateAreasCreatesMonasteryAreaWhenConfigured() {
        TileSpec tileSpec = new TileSpec(new EdgeType[]{EdgeType.FIELD, EdgeType.FIELD, EdgeType.FIELD, EdgeType.FIELD}, false, true, false, 0, 7);

        long[] areas = tileSpec.calculateAreas();

        assertEquals(AreaType.MONASTERY, Area.getAreaTypeTyped(areas[12]));
        assertEquals(8, AreaLayoutBit.getOpenEdgesCounter(areas[12]));
    }

    @Test
    void calculateAreasConnectsCastleAreasWhenConfigured() {
        TileSpec tileSpec = new TileSpec(new EdgeType[]{EdgeType.CASTLE, EdgeType.CASTLE, EdgeType.CASTLE, EdgeType.CASTLE}, true, false, true, 0, 7);

        long[] areas = tileSpec.calculateAreas();
        long representative = AreaRegistry.findLocalRepresentative(areas, areas[0]);

        for (int i = 0; i < 12; i++) {
            assertEquals(AreaType.CASTLE, Area.getAreaTypeTyped(areas[i]));
            assertEquals(Area.getGlobalAreaId(representative), Area.getGlobalAreaId(AreaRegistry.findLocalRepresentative(areas, areas[i])));
        }
    }

    @Test
    void getEdgeTypeRespectsRotation() {
        TileSpec tileSpec = new TileSpec(new EdgeType[]{EdgeType.FIELD, EdgeType.CASTLE, EdgeType.ROAD0, EdgeType.FIELD}, false, false, false, 2, 9);

        assertEquals(EdgeType.FIELD, tileSpec.getEdgeType(Direction.LEFT, 0));
        assertEquals(EdgeType.CASTLE, tileSpec.getEdgeType(Direction.LEFT, 1));
        assertEquals(EdgeType.ROAD0, tileSpec.getEdgeType(Direction.LEFT, 2));
        assertEquals(EdgeType.FIELD, tileSpec.getEdgeType(Direction.TOP, 3));
    }

    @Test
    void accessorsExposeConfiguredValues() {
        TileSpec tileSpec = new TileSpec(new EdgeType[]{EdgeType.FIELD, EdgeType.CASTLE, EdgeType.ROAD0, EdgeType.FIELD}, true, true, true, 5, 11);

        assertTrue(tileSpec.hasBlazon());
        assertTrue(tileSpec.isMonastery());
        assertTrue(tileSpec.isConnectCastles());
        assertEquals(5, tileSpec.getTileId());
        assertEquals(11, tileSpec.getGraphicsId());
    }

    @Test
    void deepCopyCreatesIndependentEqualTileSpec() {
        TileSpec tileSpec = new TileSpec(new EdgeType[]{EdgeType.FIELD, EdgeType.CASTLE, EdgeType.ROAD0, EdgeType.FIELD}, true, true, false, 5, 11);

        TileSpec copy = tileSpec.deepCopy();

        assertNotSame(tileSpec, copy);
        assertEquals(tileSpec, copy);
        assertEquals(tileSpec.getTileTemplate(), copy.getTileTemplate());
        assertEquals(tileSpec.getDifferentRotationsCount(), copy.getDifferentRotationsCount());
    }

    @Test
    void equalsAndHashCodeUseTileIdOnly() {
        TileSpec a = new TileSpec(new EdgeType[]{EdgeType.FIELD, EdgeType.FIELD, EdgeType.FIELD, EdgeType.FIELD}, false, false, false, 3, 1);
        TileSpec sameIdDifferentShape = new TileSpec(new EdgeType[]{EdgeType.CASTLE, EdgeType.ROAD0, EdgeType.FIELD, EdgeType.FIELD}, true, true, true, 3, 9);
        TileSpec otherId = new TileSpec(new EdgeType[]{EdgeType.FIELD, EdgeType.FIELD, EdgeType.FIELD, EdgeType.FIELD}, false, false, false, 4, 1);

        assertEquals(a, sameIdDifferentShape);
        assertEquals(a.hashCode(), sameIdDifferentShape.hashCode());
        assertFalse(a.equals(otherId));
        assertFalse(a.equals("tile"));
    }

    @Test
    void getDifferentRotationsCountDistinguishesSymmetryCases() {
        TileSpec oneRotation = new TileSpec(new EdgeType[]{EdgeType.FIELD, EdgeType.FIELD, EdgeType.FIELD, EdgeType.FIELD}, false, false, false, 0, 0);
        TileSpec twoRotations = new TileSpec(new EdgeType[]{EdgeType.FIELD, EdgeType.CASTLE, EdgeType.FIELD, EdgeType.CASTLE}, false, false, false, 1, 0);
        TileSpec fourRotations = new TileSpec(new EdgeType[]{EdgeType.FIELD, EdgeType.CASTLE, EdgeType.ROAD0, EdgeType.FIELD}, false, false, false, 2, 0);

        assertEquals(1, oneRotation.getDifferentRotationsCount());
        assertEquals(2, twoRotations.getDifferentRotationsCount());
        assertEquals(4, fourRotations.getDifferentRotationsCount());
    }

    @Test
    void getTileTemplateUsesSimpleEdgeEncoding() {
        TileSpec tileSpec = new TileSpec(new EdgeType[]{EdgeType.FIELD, EdgeType.ROAD0, EdgeType.CASTLE, EdgeType.ROAD1}, false, false, false, 0, 0);

        assertEquals((2 << 2) | (1 << 4) | (2 << 6), tileSpec.getTileTemplate());
    }
}
