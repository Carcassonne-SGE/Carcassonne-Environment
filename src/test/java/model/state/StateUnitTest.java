package model.state;

import model.Configuration.GameConfigBuilder;
import model.Configuration.GameConfiguration;
import model.bits.PositionLayoutBit;
import model.collections.ActionSet;
import model.exceptions.ActionMismatchException;
import model.exceptions.InvalidMeeplePositionException;
import model.points.Player;
import org.junit.jupiter.api.Test;
import sge.CarcassonneAction;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StateUnitTest {

    @Test
    void constructorInitializesBoardFrontierAndDrawPhase() {
        State state = new State(createStandardConfig());

        assertTrue(state.drawTile);
        assertTrue(state.canonical);
        assertEquals(0, state.currentPlayer);
        assertNull(state.getCurrentTile());
        assertEquals(1, state.tiles.size());
        assertTrue(state.tiles.get(PositionLayoutBit.rawPack(0, 0)) != 0L);
        assertEquals(4, state.frontier.size());
        assertEquals(-1, state.getCurrentPlayerSge());
        assertFalse(state.isGameOver());
    }

    @Test
    void doActionDrawThenPlaceAdvancesTurnAndClearsCurrentTile() {
        State state = new State(createSingleFieldTileConfig());

        state.doAction(drawAction(0));
        assertFalse(state.drawTile);
        assertEquals(0, state.getCurrentTile().getTileId());

        state.doAction(placeAction(-1, 0, 0, -1));

        assertTrue(state.drawTile);
        assertEquals(1, state.currentPlayer);
        assertNull(state.getCurrentTile());
        assertEquals(2, state.tiles.size());
        assertTrue(state.isGameOver());
    }

    @Test
    void doActionRejectsMismatchedActionType() {
        State state = new State(createStandardConfig());

        assertThrows(ActionMismatchException.class, () -> state.doAction(placeAction(-1, 0, 0, -1)));

        state.doAction(drawAction(0));

        assertThrows(ActionMismatchException.class, () -> state.doAction(drawAction(1)));
    }

    @Test
    void doPlaceActionRejectsMonasteryMeepleOnNonMonasteryTile() {
        State state = new State(createStandardConfig());
        state.doAction(drawAction(0));

        assertThrows(InvalidMeeplePositionException.class, () -> state.doPlaceAction(-1, 0, 0, 12));
    }

    @Test
    void nextPlayerTogglesPhaseAndAdvancesAfterPlacePhase() {
        State state = new State(createStandardConfig());

        state.nextPlayer();
        assertFalse(state.drawTile);
        assertEquals(0, state.currentPlayer);

        state.nextPlayer();
        assertTrue(state.drawTile);
        assertEquals(1, state.currentPlayer);
    }

    @Test
    void deepCopyAndCanonicalGettersRespectIsolationRules() {
        State state = new State(createStandardConfig());

        State copy = state.deepCopy();
        State downgraded = state.deepCopy(true);

        assertNotSame(state, copy);
        assertNotSame(state.tiles, copy.tiles);
        assertTrue(copy.canonical);
        assertFalse(downgraded.canonical);
        assertSame(state.gameConfig, copy.gameConfig);
        assertNotSame(state.gameConfig, downgraded.gameConfig);

        assertNotSame(state.tiles, state.getTiles());
        assertNotSame(state.areaRegistry, state.getAreaRegistry());
        assertNotSame(state.meepleRegistry, state.getMeepleRegistry());
        assertNotSame(state.frontier, state.getFrontier());
        assertNotSame(state.tileDeck, state.getTileDeck());

        assertSame(downgraded.tiles, downgraded.getTiles());
        assertSame(downgraded.areaRegistry, downgraded.getAreaRegistry());
        assertSame(downgraded.meepleRegistry, downgraded.getMeepleRegistry());
        assertSame(downgraded.frontier, downgraded.getFrontier());
        assertSame(downgraded.tileDeck, downgraded.getTileDeck());
        assertSame(downgraded.gameConfig, downgraded.getGameConfig());
    }

    @Test
    void pointsUtilitiesAndWinChecksUsePackedPlayerPoints() {
        State state = new State(createStandardConfig());
        state.playerPoints = Player.addPoints(state.playerPoints, 0, 10);
        state.playerPoints = Player.addPoints(state.playerPoints, 1, 7);

        assertEquals(10, state.getPlayerPoints(0));
        assertEquals(7, state.getPlayerPoints(1));
        assertTrue(state.playerWin(0));
        assertFalse(state.playerWin(1));
        assertEquals(3.0, state.getUtilityValue(0));
        assertEquals(-3.0, state.getCompetitiveUtility(1));
        assertEquals(17.0, state.getCollaborativeUtility());
    }

    @Test
    void convenienceMethodsDelegateToManagers() {
        State state = new State(createSingleFieldTileConfig());
        state.doAction(drawAction(0));

        ActionSet actions = state.calculatePossibleActionsUnique();
        CarcassonneAction action = actions.getActionObject(0);

        assertEquals(PossibleActionManager.calculatePossibleActions(state, true).size(), actions.size());
        assertEquals(HeuristicManager.computePrior(state, action, state.gameConfig.getDefaultHeuristic()), state.heuristicPrior(action));
    }

    @Test
    void toStringIncludesDeckAndPlayerSummary() {
        State state = new State(createStandardConfig());
        String value = state.toString();

        assertTrue(value.contains("card:"));
        assertTrue(value.contains("Player0"));
        assertTrue(value.contains("meeplesFree"));
    }

    private static GameConfiguration createStandardConfig() {
        GameConfigBuilder builder = new GameConfigBuilder()
                .setPlayerCount(2)
                .setRandomSeed(1)
                .setMeeplePerPlayer(2);
        builder.addTile().endTile();
        builder.addTile().markAsIsMonastery().endTile();
        builder.setStartField().endTile();
        return builder.build(false);
    }

    private static GameConfiguration createSingleFieldTileConfig() {
        GameConfigBuilder builder = new GameConfigBuilder()
                .setPlayerCount(2)
                .setRandomSeed(1)
                .setMeeplePerPlayer(2);
        builder.addTile().endTile();
        builder.setStartField().endTile();
        return builder.build(false);
    }

    private static CarcassonneAction drawAction(int tileId) {
        return new CarcassonneAction(false, 0, 0, 0, 0, tileId);
    }

    private static CarcassonneAction placeAction(int x, int y, int rot, int areaId) {
        return new CarcassonneAction(true, x, y, rot, areaId, 0);
    }
}
