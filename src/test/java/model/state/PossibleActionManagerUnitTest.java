package model.state;

import model.Configuration.GameConfigBuilder;
import model.Configuration.GameConfiguration;
import model.area.Area;
import model.collections.ActionSet;
import org.junit.jupiter.api.Test;
import sge.CarcassonneAction;

import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PossibleActionManagerUnitTest {

    @Test
    void calculatePossibleActionsUniqueReturnsExpectedFieldTileActions() {
        State state = new State(createSingleFieldTileConfig());
        PerformActionManager.performDrawAction(state, 0);

        ActionSet actions = PossibleActionManager.calculatePossibleActions(state, true);

        assertEquals(8, actions.size());
        assertTrue(actions.contains(placeAction(-1, 0, 0, -1)));
        assertTrue(actions.contains(placeAction(-1, 0, 0, 0)));
    }

    @Test
    void calculatePossibleActionsNonUniqueReturnsMoreActionsThanUnique() {
        State state = new State(createSingleFieldTileConfig());
        PerformActionManager.performDrawAction(state, 0);

        ActionSet unique = PossibleActionManager.calculatePossibleActions(state, true);
        ActionSet all = PossibleActionManager.calculatePossibleActions(state, false);

        assertTrue(all.size() > unique.size());
    }

    @Test
    void calculatePossibleActionsReturnsEmptyWhenGameOver() {
        State state = new State(createSingleFieldTileConfig());
        PerformActionManager.performDrawAction(state, 0);
        PerformActionManager.performAction(state, -1, 0, 0, -1);

        assertEquals(0, PossibleActionManager.calculatePossibleActions(state, true).size());
    }

    @Test
    void getRandomActionReturnsContainedValidAction() {
        State state = new State(createSingleFieldTileConfig());
        PerformActionManager.performDrawAction(state, 0);
        ActionSet actions = PossibleActionManager.calculatePossibleActions(state, true);

        int actionValue = PossibleActionManager.getRandomAction(state, zeroRandom(), 0.0f);
        CarcassonneAction action = new CarcassonneAction(true,
                model.bits.CarcassonneActionLayoutBit.getX(actionValue),
                model.bits.CarcassonneActionLayoutBit.getY(actionValue),
                model.bits.CarcassonneActionLayoutBit.getRotation(actionValue),
                model.bits.CarcassonneActionLayoutBit.getAreaId(actionValue),
                0);

        assertTrue(actions.contains(action));
    }

    @Test
    void getPossibleDrawActionsReturnsAllRemainingTileIds() {
        State state = new State(createThreeTileConfig());

        ActionSet drawActions = PossibleActionManager.getPossibleDrawActions(state);

        assertEquals(3, drawActions.size());
        assertTrue(drawActions.contains(drawAction(0)));
        assertTrue(drawActions.contains(drawAction(1)));
        assertTrue(drawActions.contains(drawAction(2)));
    }

    @Test
    void isMeeplePlacementValidDetectsConnectedMeeple() {
        State state = new State(createSingleFieldTileConfig());
        int startTileId = state.gameConfig.startTile().getTileId();
        model.points.Player.placeMeeple(state.areaRegistry, state.meepleRegistry, 0, state.areaRegistry.getArea(startTileId, 0));
        PerformActionManager.performDrawAction(state, 0);

        assertTrue(!PossibleActionManager.isMeeplePlacementValid(state, -1, 0, 0, 0));
    }

    private static GameConfiguration createSingleFieldTileConfig() {
        GameConfigBuilder builder = new GameConfigBuilder().setPlayerCount(2).setRandomSeed(1).setMeeplePerPlayer(2);
        builder.addTile().endTile();
        builder.setStartField().endTile();
        return builder.build(false);
    }

    private static GameConfiguration createThreeTileConfig() {
        GameConfigBuilder builder = new GameConfigBuilder().setPlayerCount(2).setRandomSeed(1).setMeeplePerPlayer(2);
        builder.addTile().endTile();
        builder.addTile().markAsIsMonastery().endTile();
        builder.addTile().left().markAsCastle().endTile();
        builder.setStartField().endTile();
        return builder.build(false);
    }

    private static CarcassonneAction placeAction(int x, int y, int rot, int areaId) {
        return new CarcassonneAction(true, x, y, rot, areaId, 0);
    }

    private static CarcassonneAction drawAction(int tileId) {
        return new CarcassonneAction(false, 0, 0, 0, 0, tileId);
    }

    private static Random zeroRandom() {
        return new Random() {
            @Override
            public int nextInt(int bound) {
                return 0;
            }

            @Override
            public float nextFloat() {
                return 0.0f;
            }
        };
    }
}
