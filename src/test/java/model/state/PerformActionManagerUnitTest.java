package model.state;

import model.Configuration.GameConfigBuilder;
import model.Configuration.GameConfiguration;
import model.exceptions.ActionMismatchException;
import org.junit.jupiter.api.Test;
import sge.CarcassonneAction;

import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PerformActionManagerUnitTest {

    @Test
    void performActionRejectsDrawActionObjects() {
        State state = new State(createPlayableConfig());

        assertThrows(ActionMismatchException.class, () -> PerformActionManager.performAction(state, drawAction(0)));
    }

    @Test
    void performDrawActionOnPlayableTileSetsCurrentTileAndSwitchesPhase() {
        State state = new State(createPlayableConfig());

        PerformActionManager.performDrawAction(state, 0);

        assertFalse(state.drawTile);
        assertEquals(0, state.tileDeck.getCurrentTile().getTileId());
        assertEquals(0, state.currentPlayer);
    }

    @Test
    void performDrawActionOnUnplayableTileKeepsDrawPhase() {
        State state = new State(createUnplayableFirstTileConfig());

        PerformActionManager.performDrawAction(state, 0);

        assertTrue(state.drawTile);
        assertNull(state.tileDeck.getCurrentTile());
        assertEquals(1, state.tileDeck.getDeckPos());
    }

    @Test
    void performActionPlacesCurrentTile() {
        State state = new State(createPlayableConfig());
        PerformActionManager.performDrawAction(state, 0);

        PerformActionManager.performAction(state, -1, 0, 0, -1);

        assertTrue(state.drawTile);
        assertEquals(1, state.currentPlayer);
        assertNull(state.tileDeck.getCurrentTile());
        assertEquals(2, state.tiles.size());
    }

    @Test
    void performActionRawCanForceTileIdByActionObjectAndCoordinates() {
        State state = new State(createPlayableConfig());
        PerformActionManager.performDrawAction(state, 0);

        PerformActionManager.performActionRaw(state, 1, placeAction(-1, 0, 0, -1));
        assertEquals(1, model.bits.TileLayoutBit.getTileId(state.tiles.get(model.bits.PositionLayoutBit.rawPack(-1, 0))));

        PerformActionManager.performDrawAction(state, 0);
        PerformActionManager.performActionRaw(state, 0, 0, 1, 0, -1);
        assertEquals(0, model.bits.TileLayoutBit.getTileId(state.tiles.get(model.bits.PositionLayoutBit.rawPack(0, 1))));
    }

    @Test
    void determineNextDrawActionMatchesTileDeckSoftDraw() {
        State state = new State(createPlayableConfig());
        Random random = zeroRandom();

        assertEquals(state.tileDeck.drawTileIdSoft(zeroRandom()), PerformActionManager.determineNextDrawAction(state, random));
    }

    @Test
    void performRandomDrawActionReturnsMinusOneWhenDeckIsExhausted() {
        State state = new State(createSingleTileConfig());
        PerformActionManager.performRandomDrawAction(state, zeroRandom());
        PerformActionManager.performAction(state, -1, 0, 0, -1);

        assertEquals(-1, PerformActionManager.performRandomDrawAction(state, zeroRandom()));
        assertTrue(state.isGameOver());
    }

    private static GameConfiguration createPlayableConfig() {
        GameConfigBuilder builder = new GameConfigBuilder().setPlayerCount(2).setRandomSeed(1).setMeeplePerPlayer(2);
        builder.addTile().endTile();
        builder.addTile().markAsIsMonastery().endTile();
        builder.setStartField().endTile();
        return builder.build(false);
    }

    private static GameConfiguration createUnplayableFirstTileConfig() {
        GameConfigBuilder builder = new GameConfigBuilder().setPlayerCount(2).setRandomSeed(1).setMeeplePerPlayer(2);
        builder.addTile().left().markAsCastle().top().markAsCastle().right().markAsCastle().bottom().markAsCastle().endTile();
        builder.addTile().endTile();
        builder.setStartField().endTile();
        return builder.build(false);
    }

    private static GameConfiguration createSingleTileConfig() {
        GameConfigBuilder builder = new GameConfigBuilder().setPlayerCount(2).setRandomSeed(1).setMeeplePerPlayer(2);
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

    private static Random zeroRandom() {
        return new Random() {
            @Override
            public int nextInt(int bound) {
                return 0;
            }
        };
    }
}
