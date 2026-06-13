package model.points;

import model.area.Area;
import model.area.AreaRegistry;
import model.bits.AreaLayoutBit;
import model.bits.MeepleLayoutBit;
import model.enums.EdgeType;
import model.exceptions.MeepleAlreadyOnTileExcpetion;
import model.exceptions.NoMeepleLeftException;
import model.tile.TileSpec;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PlayerUnitTest {

    @Test
    void checkMeeplePlacementAllowsFreePlacement() {
        AreaRegistry areas = createIndependentAreaRegistry();
        MeepleRegistry meeples = new MeepleRegistry(2, 2);

        assertDoesNotThrow(() -> Player.checkMeeplePlacement(areas, meeples, 0, areas.getArea(0, 0)));
    }

    @Test
    void checkMeeplePlacementRejectsAreaThatAlreadyHasMeeple() {
        AreaRegistry areas = createIndependentAreaRegistry();
        MeepleRegistry meeples = new MeepleRegistry(2, 2);

        Player.placeMeeple(areas, meeples, 0, areas.getArea(0, 0));

        assertThrows(MeepleAlreadyOnTileExcpetion.class,
                () -> Player.checkMeeplePlacement(areas, meeples, 1, areas.getArea(0, 0)));
    }

    @Test
    void checkMeeplePlacementRejectsPlayersWithoutFreeMeeples() {
        AreaRegistry areas = createIndependentAreaRegistry();
        MeepleRegistry meeples = new MeepleRegistry(1, 1);
        int meeple = meeples.getMeepleRaw(0, 0);
        meeples.place(meeple, Area.getGlobalAreaId(areas.getArea(0, 0)));

        assertThrows(NoMeepleLeftException.class,
                () -> Player.checkMeeplePlacement(areas, meeples, 0, areas.getArea(0, 3)));
    }

    @Test
    void placeMeeplePlacesMeepleAndUpdatesRepresentativeMaxMeeples() {
        AreaRegistry areas = createIndependentAreaRegistry();
        MeepleRegistry meeples = new MeepleRegistry(2, 2);

        Player.placeMeeple(areas, meeples, 1, areas.getArea(0, 0));

        int placedMeeple = meeples.getMeepleRaw(1, 0);
        long representative = areas.findRepresentative(areas.getArea(0, 0));
        assertEquals(Area.getGlobalAreaId(areas.getArea(0, 0)), MeepleLayoutBit.getGlobalAreaId(placedMeeple));
        assertEquals(1, meeples.findMeeplesOnAreaCount(areas, representative, 1));
        assertEquals(1, AreaLayoutBit.getMaxMeeples(representative));
    }

    @Test
    void placeMeepleUsesPlacementChecks() {
        AreaRegistry areas = createIndependentAreaRegistry();
        MeepleRegistry meeples = new MeepleRegistry(2, 1);
        Player.placeMeeple(areas, meeples, 0, areas.getArea(0, 0));

        assertThrows(MeepleAlreadyOnTileExcpetion.class,
                () -> Player.placeMeeple(areas, meeples, 1, areas.getArea(0, 0)));
    }

    @Test
    void getPointsReadsOnePlayersTwelveBitSlice() {
        long playerPoints = 0L;
        playerPoints = Player.addPoints(playerPoints, 0, 25);
        playerPoints = Player.addPoints(playerPoints, 1, 300);

        assertEquals(25, Player.getPoints(playerPoints, 0));
        assertEquals(300, Player.getPoints(playerPoints, 1));
    }

    @Test
    void addPointsAddsWithoutAffectingOtherPlayersAndWrapsAtTwelveBits() {
        long playerPoints = 0L;
        playerPoints = Player.addPoints(playerPoints, 0, 4095);
        playerPoints = Player.addPoints(playerPoints, 1, 17);

        long updated = Player.addPoints(playerPoints, 0, 2);

        assertEquals(1, Player.getPoints(updated, 0));
        assertEquals(17, Player.getPoints(updated, 1));
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
