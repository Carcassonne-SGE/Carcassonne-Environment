package model.state;

import model.Configuration.GameConfigBuilder;
import model.Configuration.GameConfiguration;
import model.area.Area;
import model.bits.AreaLayoutBit;
import model.bits.MeepleLayoutBit;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ScoringManagerUnitTest {

    @Test
    void findMaxMeeplesAndFreeMarksRepresentativesAndClearsDuplicates() {
        State state = new State(createStartFieldOnlyConfig());
        long area = state.areaRegistry.findRepresentative(state.areaRegistry.getArea(0, 0));
        state.meepleRegistry.place(state.meepleRegistry.getMeepleRaw(0, 0), 0);
        state.meepleRegistry.place(state.meepleRegistry.getMeepleRaw(0, 1), 1);
        state.meepleRegistry.place(state.meepleRegistry.getMeepleRaw(1, 0), 2);

        int max = ScoringManager.findMaxMeeplesAndFree(state.gameConfig, state.meepleRegistry, state.areaRegistry, area);

        assertEquals(2, max);
        assertEquals(2, MeepleLayoutBit.getCount(state.meepleRegistry.getMeepleRaw(0, 0)));
        assertTrue(MeepleLayoutBit.getReadyToCollect(state.meepleRegistry.getMeepleRaw(0, 0)));
        assertEquals(-1, MeepleLayoutBit.getGlobalAreaId(state.meepleRegistry.getMeepleRaw(0, 1)));
        assertEquals(1, MeepleLayoutBit.getCount(state.meepleRegistry.getMeepleRaw(1, 0)));
        assertTrue(MeepleLayoutBit.getReadyToCollect(state.meepleRegistry.getMeepleRaw(1, 0)));
    }

    @Test
    void collectPlayerPointsAwardsCompletedMonasteryAndFreesMeeple() {
        State state = new State(createMonasteryStartConfig());
        int monasteryTileId = state.gameConfig.startTile().getTileId();
        long monasteryArea = state.areaRegistry.getMonasteryArea(monasteryTileId);
        int repId = Area.getGlobalAreaId(monasteryArea);
        state.areaRegistry.areas[repId] = AreaLayoutBit.setOpenEdgesCounter(monasteryArea, 0);
        state.meepleRegistry.place(state.meepleRegistry.getMeepleRaw(0, 0), repId);

        ScoringManager.collectPlayerPoints(state, false);

        assertEquals(9, state.getPlayerPoints(0));
        assertEquals(-1, MeepleLayoutBit.getGlobalAreaId(state.meepleRegistry.getMeepleRaw(0, 0)));
    }

    @Test
    void collectPlayerPointsAtGameEndAwardsIncompleteMonastery() {
        State state = new State(createMonasteryStartConfig());
        int monasteryTileId = state.gameConfig.startTile().getTileId();
        long monasteryArea = state.areaRegistry.getMonasteryArea(monasteryTileId);
        int repId = Area.getGlobalAreaId(monasteryArea);
        state.areaRegistry.areas[repId] = AreaLayoutBit.setOpenEdgesCounter(monasteryArea, 5);
        state.meepleRegistry.place(state.meepleRegistry.getMeepleRaw(0, 0), repId);

        ScoringManager.collectPlayerPoints(state, true);

        assertEquals(4, state.getPlayerPoints(0));
        assertEquals(-1, MeepleLayoutBit.getGlobalAreaId(state.meepleRegistry.getMeepleRaw(0, 0)));
    }

    @Test
    void collectPlayerPointsAtGameEndAwardsFieldPointsFromCompletedCastle() {
        State state = new State(createCastleStartConfig());
        long castleArea = state.areaRegistry.findRepresentative(state.areaRegistry.getArea(0, 0));
        int castleRepId = Area.getGlobalAreaId(castleArea);
        state.areaRegistry.areas[castleRepId] = AreaLayoutBit.setOpenEdgesCounter(castleArea, 0);
        int fieldAreaId = Area.getGlobalAreaId(state.areaRegistry.getArea(0, 3));
        state.meepleRegistry.place(state.meepleRegistry.getMeepleRaw(0, 0), fieldAreaId);

        ScoringManager.collectPlayerPoints(state, true);

        assertEquals(3, state.getPlayerPoints(0));
        assertEquals(-1, MeepleLayoutBit.getGlobalAreaId(state.meepleRegistry.getMeepleRaw(0, 0)));
    }

    private static GameConfiguration createStartFieldOnlyConfig() {
        GameConfigBuilder builder = new GameConfigBuilder().setPlayerCount(2).setRandomSeed(1).setMeeplePerPlayer(2);
        builder.setStartField().endTile();
        return builder.build(false);
    }

    private static GameConfiguration createMonasteryStartConfig() {
        GameConfigBuilder builder = new GameConfigBuilder().setPlayerCount(2).setRandomSeed(1).setMeeplePerPlayer(2);
        builder.setStartField().markAsIsMonastery().endTile();
        return builder.build(false);
    }

    private static GameConfiguration createCastleStartConfig() {
        GameConfigBuilder builder = new GameConfigBuilder().setPlayerCount(2).setRandomSeed(1).setMeeplePerPlayer(2);
        builder.setStartField().left().markAsCastle().endTile();
        return builder.build(false);
    }
}
