package sge;

import model.Configuration.ConfigManager;
import model.Configuration.GameConfiguration;
import at.ac.tuwien.ifs.sge.game.ActionRecord;
import at.ac.tuwien.ifs.sge.game.Game;
import model.state.PerformActionManager;
import model.state.PossibleActionManager;
import model.state.State;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.Set;

/// CarcassonneGame
///
/// This class uses the model package to provide the game functionality
/// in the form the sge env requires it.
/// The CarcassonneGame is immutable each action creates a new object.
///
/// The link to the Sge Env for documentation:
/// [SGE env repo](https://gitlab.com/StrategyGameEngine)
public class CarcassonneGame implements Game<CarcassonneAction, State> {
    private final GameConfiguration config;
    private final ArrayList<ActionRecord<CarcassonneAction>> actionRecords = new ArrayList<>(75);
    private final State state;
    private final Random rand;
    private final boolean canonical;

    public CarcassonneGame() {
        this("default", 2);
    }

    public CarcassonneGame(String configName, int numberOfPlayers) {
        this(configName,numberOfPlayers,new Random().nextInt());
    }

    public CarcassonneGame(String configName, int numberOfPlayers, int seed) {
        config =  new ConfigManager().getConfig(configName).setPlayerCount(numberOfPlayers).setRandomSeed(seed).build(true);
        state = new State(config);
        canonical = true;
        rand = new Random(seed +1);
    }

    @Override
    public Game<CarcassonneAction, State> doAction(CarcassonneAction action) {
        State stateCopy = state.deepCopy(false);
        int actingPlayer = getCurrentPlayer();
        stateCopy.doAction(action);
        var game = new CarcassonneGame(config, stateCopy, rand, canonical);
        game.actionRecords.addAll(this.actionRecords);
        game.actionRecords.add(new ActionRecord<>(actingPlayer, action));
        return game;
    }

    @Override
    public Set<CarcassonneAction> getPossibleActions() {
        if (isGameOver()) {
            return Collections.emptySet();
        }else if(getCurrentPlayer() < 0){
            // we are in a indeterminate state which means we are in a draw action. Need to give all possible draw Actions
            return PossibleActionManager.getPossibleDrawActions(state);
        }
        // game is not over and we are in a action action state need to calculate all place actions for the current tile
        return PossibleActionManager.calculatePossibleActions(state,false);
    }


    @Override
    public List<ActionRecord<CarcassonneAction>> getActionRecords() {
        return new ArrayList<>(actionRecords);
    }


    @Override
    public boolean isGameOver() {
        return state.isGameOver();
    }

    @Override
    public Game<CarcassonneAction, State> getGame(int i) {
        var game = new CarcassonneGame(config, state.deepCopy(false), rand, canonical);
        game.actionRecords.addAll(this.actionRecords);
        return game;
    }
    @Override
    public CarcassonneAction determineNextAction() {
        if(getCurrentPlayer() < 0){
            int tileId = PerformActionManager.determineNextDrawAction(state, rand);
            return new CarcassonneDrawAction(tileId);
        }else {
            return null;
        }
    }


    @Override
    public double getUtilityValue(int i) {
        return state.getCollaborativeUtility();
    }

    @Override
    public State getBoard() {
        return state.deepCopy(true);
    }

    @Override
    public int getCurrentPlayer() {
        return state.getCurrentPlayerSge();
    }

    @Override
    public int getNumberOfPlayers() {
        return config.playersCount();
    }

    @Override
    public int getMinimumNumberOfPlayers() {
        return 2;
    }

    @Override
    public int getMaximumNumberOfPlayers() {
        return 4;
    }

    @Override
    public boolean isCanonical() {
        return canonical;
    }

    @Override
    public String toString() {
        return state != null?state.toString():"state not init";
    }

    // private copy constructor
    private CarcassonneGame(GameConfiguration config, State state, Random rand, boolean canonical) {
        this.config = config;
        this.state = state;
        this.rand = rand;
        this.canonical = canonical;
    }
}
