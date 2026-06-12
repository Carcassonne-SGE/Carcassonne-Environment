package model.state;

import model.exceptions.ActionMismatchException;
import sge.CarcassonneAction;

import java.util.Random;

///  PerformActionManager
///
/// provides functionality for performing actions on a state. Takes the state and mutates the state
public class PerformActionManager {

    // prevent instances of that class
    private PerformActionManager(){}

    /// performActionRaw
    ///
    /// performs a place Action on the given state and forces the given tileId to be used as the current Tile
    /// the provided tileId needs to be not palced yet
    ///
    /// @param state the state on which the action is performed
    /// @param tileId the tile which is being placed. This overwrites the currentTile logic of the state
    /// @param action the action that shall be executed
    ///
    /// Note: {@link State#doPlaceAction(int, int, int, int)} for more documentation
    public static void performActionRaw(State state, int tileId, CarcassonneAction action) {
        performActionRaw(state,tileId,action.getX(),action.getY(),action.getRotation(),action.getAreaId());
    }

    /// performAction
    ///
    /// performs a place action on the state given the action mutates the state after action it is in a drawState
    ///
    /// @param state the state on which the action is performed
    /// @param action the action that shall be executed
    ///
    /// Note: {@link State#doPlaceAction(int, int, int, int)} for more documentation
    public static void performAction(State state, CarcassonneAction action) {
        if(!action.isAction()){
            throw new ActionMismatchException();
        }
        performAction(state,action.getX(),action.getY(),action.getRotation(),action.getAreaId());
    }

    /// performActionRaw
    ///
    /// performs a place action on the given state that forces the given tile to be placed at the/with the
    /// given action data
    ///
    /// @param state the state on which the action is performed
    /// @param tileId the tile which is being placed. This overwrites the currentTile logic of the state
    /// @param x x-Axis position where the tile will be placed
    /// @param y y-Axis position where the tle will be placed
    /// @param rot the rotation of the now placed tile in [0,3] rotates counter-clockwise
    /// @param localAreaId -1 if no meeple otherwise the area id where to place the meeple
    /// Note: {@link State#doPlaceAction(int, int, int, int)} for more documentation
    public static void performActionRaw(State state, int tileId, int x, int y, int rot, int localAreaId) {
        // force the currentTile
        var tileDeck = state.tileDeck;
        tileDeck.unPop();
        var currentTile = tileDeck.popForceGetTile(tileId);
        state.tileDeck.setCurrentTile(currentTile);

        // do the place action
        state.doPlaceAction(x,y,rot,localAreaId);
    }



    /// performActionRaw
    ///
    /// does a place action on the provided state with the provided action information
    ///
    /// @param state the state on which the action is performed
    /// @param x x-Axis position where the tile will be placed
    /// @param y y-Axis position where the tle will be placed
    /// @param rot the rotation of the now placed tile in [0,3] rotates counter-clockwise
    /// @param localAreaId -1 if no meeple otherwise the area id where to place the meeple
    ///
    /// Note: {@link State#doPlaceAction(int, int, int, int)} for more documentation
    public static void performAction(State state, int x, int y, int rot, int localAreaId) {
        state.doPlaceAction(x,y,rot,localAreaId);
    }

    ///  performDrawAction
    ///
    /// does a draw Action. So after a place action the game is in an indeterminate state. Then a draw
    /// action needs to be performed that draws the given tileId from the tileDeck. This methods does
    /// exactly that. It removes the given tileId from the deck and makes is the currentTile
    ///
    /// If the tile with the provided tileId is not placeable the game stays in the indeterminate state
    /// and requires another draw action. The old tile is consumed and out of the game
    ///
    /// @param state the state on which the action is performed
    /// @param tileId the id of the new currentTile
    public static void performDrawAction(State state, int tileId) {
        var deck = state.tileDeck;
        state.tileDeck.setCurrentTile(null);
        var tile = deck.popForceGetTile(tileId);
        if (deck.hasAnyPlacement(state.frontier, tileId)) {
            state.tileDeck.setCurrentTile(tile);
            state.nextPlayer();
        }else {
            state.tileDeck.setCurrentTile(null);
        }

        // game may be now over because there is no tile left need to trigger final points collection
        if (state.isGameOver()) {
            state.collectPlayerPoints(true);
        }
    }

    /// determineNextDrawAction
    ///
    /// uses the random object to return a random draw action
    ///
    /// @param state the state in which the draw action needs to be possible in
    /// @param random Random object used to get pseudo random numbers
    public static int determineNextDrawAction(State state, Random random) {
        return state.tileDeck.drawTileIdSoft(random);
    }

    /// performRandomDrawAction
    ///
    /// performs a random draw action if the drawn tile can be placed switch to action state
    /// otherwise stay in draw phase
    ///
    /// @param state the state in which the draw action needs to be possible in
    /// @param random Random object used to get pseudo random numbers
    ///
    /// @return tileId of the drawn tile or -1 with no tile was found
    public static int performRandomDrawAction(State state, Random random) {
        var tile = state.tileDeck.popGetTile(random);
        if (tile != null) {
            // found a tile can return that
            state.tileDeck.setCurrentTile(tile);
            if (state.tileDeck.hasAnyPlacement(state.frontier, tile.getTileId())) {
                state.tileDeck.setCurrentTile(tile);
                state.nextPlayer();
            }
            return tile.getTileId();
        }

        // found no tile need to trigger final points collection and return -1
        if (state.isGameOver()) {
            state.collectPlayerPoints(true);
        }
        return -1;

    }
}
