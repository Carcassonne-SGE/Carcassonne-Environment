package model.state;

import model.Configuration.GameConfigBuilder;
import model.Configuration.GameConfiguration;
import model.heuristic.HeuristicConfiguration;
import model.heuristic.HeuristicMixConfig;
import org.junit.jupiter.api.Test;
import sge.CarcassonneAction;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HeuristicManagerUnitTest {

    @Test
    void mixScoresUsesMeepleScoreOnlyWhenMeepleIsPlaced() {
        HeuristicMixConfig config = new HeuristicMixConfig(0.5f, 0.2f);

        assertEquals(3.0f, HeuristicManager.mixScores(3.0f, 7.0f, config, placeAction(-1, 0, 0, -1).getValue()));
        assertEquals(6.5f, HeuristicManager.mixScores(3.0f, 7.0f, config, placeAction(-1, 0, 0, 0).getValue()));
    }

    @Test
    void computePriorObjectAndIntVariantsMatch() {
        State state = new State(createStandardConfig());
        PerformActionManager.performDrawAction(state, 0);
        CarcassonneAction action = placeAction(-1, 0, 0, -1);
        HeuristicConfiguration config = HeuristicManager.createDefaultHeuristic();

        assertEquals(HeuristicManager.computePrior(state, action.getValue(), config), HeuristicManager.computePrior(state, action, config));
    }

    @Test
    void computePriorWithPrecomputedTileScoreMatchesMeepleAndMixCalculation() {
        State state = new State(createStandardConfig());
        PerformActionManager.performDrawAction(state, 0);
        CarcassonneAction action = placeAction(-1, 0, 0, 0);
        HeuristicConfiguration config = HeuristicManager.createDefaultHeuristic();
        float tileScore = HeuristicManager.tilePlacementScore(state, -1, 0, 0, config.positionHeuristik());
        float meepleScore = HeuristicManager.meepleScore(state, 0, 0, -1, 0, config.meeplePlacementHeuristic());
        float expected = HeuristicManager.mixScores(tileScore, meepleScore, config.heuristicMixConfig(), action.getValue());

        assertEquals(expected, HeuristicManager.computePrior(state, action.getValue(), tileScore, config));
    }

    @Test
    void meepleScoreHandlesNoMeepleAndMonasteryCases() {
        HeuristicConfiguration config = HeuristicManager.createDefaultHeuristic();

        State fieldState = new State(createStandardConfig());
        PerformActionManager.performDrawAction(fieldState, 0);
        assertEquals(0.0f, HeuristicManager.meepleScore(fieldState, -1, 0, -1, 0, config.meeplePlacementHeuristic()));

        State monasteryState = new State(createStandardConfig());
        PerformActionManager.performDrawAction(monasteryState, 1);
        assertTrue(HeuristicManager.meepleScore(monasteryState, 12, 0, -1, 0, config.meeplePlacementHeuristic()) > 0);
    }

    @Test
    void tilePlacementScoreIsPositiveForAdjacentPlacement() {
        State state = new State(createStandardConfig());
        PerformActionManager.performDrawAction(state, 0);
        HeuristicConfiguration config = HeuristicManager.createDefaultHeuristic();

        assertTrue(HeuristicManager.tilePlacementScore(state, -1, 0, 0, config.positionHeuristik()) > 0);
    }

    @Test
    void createDefaultHeuristicReturnsUsableConfiguration() {
        HeuristicConfiguration config = HeuristicManager.createDefaultHeuristic();

        assertTrue(config.positionHeuristik() != null);
        assertTrue(config.meeplePlacementHeuristic() != null);
        assertTrue(config.heuristicMixConfig() != null);
    }

    private static GameConfiguration createStandardConfig() {
        GameConfigBuilder builder = new GameConfigBuilder().setPlayerCount(2).setRandomSeed(1).setMeeplePerPlayer(2);
        builder.addTile().endTile();
        builder.addTile().markAsIsMonastery().endTile();
        builder.setStartField().endTile();
        return builder.build(false);
    }

    private static CarcassonneAction placeAction(int x, int y, int rot, int areaId) {
        return new CarcassonneAction(true, x, y, rot, areaId, 0);
    }
}
