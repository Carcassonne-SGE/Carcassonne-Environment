package model.area;

import model.bits.AreaLayoutBit;
import model.enums.AreaType;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AreaUnitTest {

    @Test
    void getAreaTypeTypedReturnsDecodedAreaType() {
        long area = Area.CreateArea(AreaType.CASTLE, 0, 2, false);
        long area1 = Area.CreateArea(AreaType.FIELD, 0, 2, false);
        long area2 = Area.CreateArea(AreaType.ROAD, 0, 2, false);

        assertEquals(AreaType.CASTLE, Area.getAreaTypeTyped(area));
        assertEquals(AreaType.FIELD, Area.getAreaTypeTyped(area1));
        assertEquals(AreaType.ROAD, Area.getAreaTypeTyped(area2));
    }

    @Test
    void getGlobalAreaIdUsesTileAndLocalId() {
        long area = Area.CreateArea(AreaType.ROAD, 5, 3, false);

        assertEquals(44, Area.getGlobalAreaId(area));
    }

    @Test
    void createAreaInitializesRegularAreaState() {
        long area = Area.CreateArea(AreaType.CASTLE, 4, 2, true);

        assertEquals(AreaType.CASTLE.getValue(), AreaLayoutBit.getAreaType(area));
        assertEquals(4, AreaLayoutBit.getLocalAreaId(area));
        assertEquals(2, AreaLayoutBit.getTileId(area));
        assertEquals(2, AreaLayoutBit.getStoredPoints(area));
        assertEquals(1, AreaLayoutBit.getOpenEdgesCounter(area));
        assertEquals(0, AreaLayoutBit.getMaxMeeples(area));
        assertEquals(1, AreaLayoutBit.getSize(area));
        assertEquals(30, AreaLayoutBit.getParentGlobalAreaId(area));
    }

    @Test
    void createAreaInitializesMonasteryState() {
        long area = Area.CreateArea(AreaType.MONASTERY, 12, 1, false);

        assertEquals(1, AreaLayoutBit.getStoredPoints(area));
        assertEquals(8, AreaLayoutBit.getOpenEdgesCounter(area));
        assertEquals(25, AreaLayoutBit.getParentGlobalAreaId(area));
    }

    @Test
    void createAreaRejectsInvalidLocalIds() {
        assertThrows(IllegalArgumentException.class, () -> Area.CreateArea(AreaType.FIELD, -1, 0, false));
        assertThrows(IllegalArgumentException.class, () -> Area.CreateArea(AreaType.FIELD, 13, 0, false));
        assertThrows(IllegalArgumentException.class, () -> Area.CreateArea(AreaType.MONASTERY, 11, 0, false));
    }

    @Test
    void mergeCombinesStateAndCanMergePoints() {
        long[] areas = new long[26];
        long a = Area.CreateArea(AreaType.CASTLE, 0, 0, false);
        a = AreaLayoutBit.setMaxMeeples(a, 1);
        long b = Area.CreateArea(AreaType.CASTLE, 1, 0, true);
        b = AreaLayoutBit.setMaxMeeples(b, 3);
        areas[0] = a;
        areas[1] = b;

        Area.merge(areas, 0, 1, true);

        assertEquals(3, AreaLayoutBit.getStoredPoints(areas[0]));
        assertEquals(2, AreaLayoutBit.getOpenEdgesCounter(areas[0]));
        assertEquals(2, AreaLayoutBit.getSize(areas[0]));
        assertEquals(3, AreaLayoutBit.getMaxMeeples(areas[0]));
        assertEquals(0, AreaLayoutBit.getParentGlobalAreaId(areas[1]));
    }


    @Test
    void updateCompletenessSubtractsOneForMonasteries() {
        long[] areas = new long[26];
        areas[25] = Area.CreateArea(AreaType.MONASTERY, 12, 1, false);
        Area.updateCompleteness(areas, areas[25]);
        assertEquals(7, AreaLayoutBit.getOpenEdgesCounter(areas[25]));
    }

    @Test
    void updateCompletenessWrapsFieldCounterByTwo() {
        long[] areas = new long[13];
        areas[0] = Area.CreateArea(AreaType.FIELD, 0, 0, false);

        Area.updateCompleteness(areas, areas[0]);

        assertEquals(1023, AreaLayoutBit.getOpenEdgesCounter(areas[0]));
    }

    @Test
    void updateCompletenessWrapsRoadCounterByTwo() {
        long[] areas = new long[13];
        areas[0] = Area.CreateArea(AreaType.ROAD, 0, 0, false);

        Area.updateCompleteness(areas, areas[0]);

        assertEquals(1023, AreaLayoutBit.getOpenEdgesCounter(areas[0]));
    }

    @Test
    void updateCompletenessWrapsCastleCounterByTwo() {
        long[] areas = new long[13];
        areas[0] = Area.CreateArea(AreaType.CASTLE, 0, 0, false);

        Area.updateCompleteness(areas, areas[0]);

        assertEquals(1023, AreaLayoutBit.getOpenEdgesCounter(areas[0]));
    }

    @Test
    void getStoredPointsReturnsFieldPointsDuringGame() {
        long area = Area.CreateArea(AreaType.FIELD, 0, 0, false);

        assertEquals(0, Area.getStoredPoints(area, false));
    }

    @Test
    void getStoredPointsReturnsFieldPointsAfterGame() {
        long area = Area.CreateArea(AreaType.FIELD, 0, 0, false);

        assertEquals(0, Area.getStoredPoints(area, true));
    }

    @Test
    void getStoredPointsDoublesCastlePointsDuringGame() {
        long area = Area.CreateArea(AreaType.CASTLE, 0, 0, true);

        assertEquals(4, Area.getStoredPoints(area, false));
    }

    @Test
    void getStoredPointsReturnsCastlePointsAfterGame() {
        long area = Area.CreateArea(AreaType.CASTLE, 0, 0, true);

        assertEquals(2, Area.getStoredPoints(area, true));
    }

    @Test
    void getStoredPointsReturnsRoadPointsDuringGame() {
        long area = Area.CreateArea(AreaType.ROAD, 0, 0, false);

        assertEquals(1, Area.getStoredPoints(area, false));
    }

    @Test
    void getStoredPointsReturnsRoadPointsAfterGame() {
        long area = Area.CreateArea(AreaType.ROAD, 0, 0, false);

        assertEquals(1, Area.getStoredPoints(area, true));
    }

    @Test
    void getStoredPointsCalculatesMonasteryPointsDuringGame() {
        long area = Area.CreateArea(AreaType.MONASTERY, 12, 0, false);
        area = AreaLayoutBit.addOpenEdgesCounter(area, -3);

        assertEquals(4, Area.getStoredPoints(area, false));
    }

    @Test
    void getStoredPointsCalculatesMonasteryPointsAfterGame() {
        long area = Area.CreateArea(AreaType.MONASTERY, 12, 0, false);
        area = AreaLayoutBit.addOpenEdgesCounter(area, -5);

        assertEquals(6, Area.getStoredPoints(area, true));
    }


    @Test
    void isCompletedChecksOpenEdgesCounter() {
        long complete = AreaLayoutBit.setOpenEdgesCounter(Area.CreateArea(AreaType.FIELD, 0, 0, false), 0);
        long incomplete = Area.CreateArea(AreaType.FIELD, 1, 0, false);

        assertTrue(Area.isCompleted(complete));
        assertFalse(Area.isCompleted(incomplete));
    }

    @Test
    void isRepresentativeChecksParentAgainstGlobalId() {
        long representative = Area.CreateArea(AreaType.FIELD, 2, 0, false);
        long child = AreaLayoutBit.setParentGlobalAreaId(Area.CreateArea(AreaType.FIELD, 3, 0, false), 2);

        assertTrue(Area.isRepresentative(representative));
        assertFalse(Area.isRepresentative(child));
    }

    @Test
    void hasMeepleChecksMaxMeeples() {
        long empty = Area.CreateArea(AreaType.FIELD, 0, 0, false);
        long occupied = AreaLayoutBit.setMaxMeeples(empty, 2);

        assertFalse(Area.hasMeeple(empty));
        assertTrue(Area.hasMeeple(occupied));
    }

    @Test
    void equalsUsesGlobalAreaIdOnly() {
        long area = Area.CreateArea(AreaType.FIELD, 4, 2, false);
        long sameGlobalIdDifferentBits = AreaLayoutBit.setStoredPoints(area, 7);
        long differentArea = Area.CreateArea(AreaType.FIELD, 5, 2, false);

        assertTrue(Area.equals(area, sameGlobalIdDifferentBits));
        assertFalse(Area.equals(area, differentArea));
    }
}
