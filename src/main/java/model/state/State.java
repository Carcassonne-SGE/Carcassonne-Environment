package model.state;

import model.Configuration.GameConfiguration;
import model.area.AreaRegistry;
import model.collections.ActionSet;
import model.collections.FrontierMap;
import model.bits.PositionLayoutBit;
import model.collections.PositionTileMap;
import model.exceptions.InvalidMeeplePositionException;
import model.exceptions.InvalidTilePlacement;
import model.exceptions.MeepleAlreadyOnTileExcpetion;
import model.exceptions.NoMeepleLeftException;
import model.exceptions.ActionMismatchException;
import model.heuristic.HeuristicConfiguration;
import model.points.MeepleRegistry;
import model.points.Player;
import model.tile.Tile;
import model.tile.TileDeck;
import model.tile.TileSpec;
import sge.CarcassonneAction;

import java.util.Set;

/// Models the Mutable State of a Carcassonne Game
///
/// Holds all data for playing a game. Game is completely characterized by that object
/// provides getter methods. If noncanonical it just returns the actial objects otherwise deep copies
///
/// {@link PerformActionManager} allows to perform actions on a state
/// {@link PossibleActionManager} allows to calculates all available actions in a state and fast random actions
public class State {

    final PositionTileMap tiles;            // stores currently placed tile placement
    final FrontierMap frontier;             // stores where tiles can be placed in the future and under which constrains

    final AreaRegistry areaRegistry;        // area Registry collection for managing possible meeple placements
    final MeepleRegistry meepleRegistry;    // meepleRegistry collection for storing where meeples are placed


    final GameConfiguration gameConfig;
    final TileDeck tileDeck;                // provides functionality for drawing tiles


    long playerPoints;                      // bit packed long stores all player points

    int currentPlayer;
    boolean drawTile;
    boolean canonical;                      // is original state or a noncanonical copy


    public State(GameConfiguration config) {
        int playerCount = config.playersCount();
        tiles = new PositionTileMap(128);
        areaRegistry = new AreaRegistry(config.tiles(), config.startTile());
        meepleRegistry = new MeepleRegistry(playerCount, config.getMeeplePerPlayer());
        tileDeck = new model.tile.TileDeck(config);
        frontier = new FrontierMap(32);
        gameConfig = config;
        drawTile = true;
        canonical = true;

        // place start tile
        Tile.placeTile(tiles, areaRegistry, frontier, config.startTile(), PositionLayoutBit.rawPack(0, 0), 0);
    }

    /// collectPlayerPoints
    ///
    /// convenience method to {@link ScoringManager#collectPlayerPoints(State, boolean)}
    public void collectPlayerPoints(boolean done) {
        ScoringManager.collectPlayerPoints(this, done);
    }

    /// calculatePossibleActionsUnique
    ///
    ///  convenience method to {@link PossibleActionManager#calculatePossibleActions(State, boolean)} with unique = true
    public ActionSet calculatePossibleActionsUnique() {
        return PossibleActionManager.calculatePossibleActions(this, true);
    }

    /// playerWin
    ///
    /// calculates if player with that playerId has won. Won means there is no player
    /// with more points. So there may be multiple winners if they have the same points
    ///
    /// @param playerId zero based
    public boolean playerWin(int playerId) {
        int playerPointsValue = Player.getPoints(this.playerPoints, playerId);
        // iterate over all player that are not the current
        for (int i = 0; i < gameConfig.playersCount(); i++) {
            if (i != playerId) {
                int otherPoints = Player.getPoints(this.playerPoints, i);
                if (otherPoints > playerPointsValue) {
                    // if other player has more points this player has not won
                    return false;
                }
            }
        }
        // if no player has more point current player has won
        return true;
    }

    /// doPlaceAction
    ///
    /// performs a player place action on the state. Takes the decoded information
    /// and processes it and throws exception if action is wrong
    ///
    /// @param x  x position of the new tile
    /// @param y y position of the new tile
    /// @param rot the rotation of the tile in {0,1,2,3}  counterclockwise rotation
    /// @param localAreaId area where a meeple is placed [0,12] and -1 if no meeple
    /// @throws ActionMismatchException happens if a place action is performed if in a draw state
    /// @throws MeepleAlreadyOnTileExcpetion happens if the area already contains a Meeple
    /// @throws NoMeepleLeftException happen if the player has no meeples to place but tries to place a meeple
    /// @throws InvalidMeeplePositionException happens if a meeple is placed on a monastery but the tile has non
    /// @throws InvalidTilePlacement happens if the (x,y) rot are not valid due to violated edge constrains or
    ///  non-frontier position
    void doPlaceAction(int x, int y, int rot, int localAreaId) {
        int posBits = PositionLayoutBit.rawPack(x, y);
        var currentTile = tileDeck.getCurrentTile();

        if(drawTile || currentPlayer < 0){
            throw new ActionMismatchException();
        }
        if(!currentTile.isMonastery() && localAreaId == 12){
            throw new InvalidMeeplePositionException(currentTile.getTileId(),localAreaId);
        }
        if(localAreaId >= 0){
            long area = areaRegistry.getAreaRot(currentTile.getTileId(), localAreaId, rot);
            Player.checkMeeplePlacement(areaRegistry, meepleRegistry, currentPlayer, area);
        }
        if(localAreaId >= 0 && localAreaId < 12 && !PossibleActionManager.isMeeplePlacementValid(this,x,y,rot,localAreaId)){
            throw new MeepleAlreadyOnTileExcpetion(true);
        }
        if(!Tile.isPlacementValid(frontier,posBits,currentTile.getTileTemplate(),rot)){
            throw new InvalidTilePlacement(x,y,rot,currentTile.getTileId());
        }

        Tile.placeTile(tiles, areaRegistry, frontier, currentTile, posBits, rot);
        // if meeple is placed to the placement
        if (localAreaId >= 0) {
            long area = areaRegistry.getAreaRot(currentTile.getTileId(), localAreaId, rot);

            Player.placeMeeple(areaRegistry, meepleRegistry, currentPlayer, area);
        }
        collectPlayerPoints(false);
        nextPlayer();
        tileDeck.setCurrentTile(null);

        // When the last drawable tile has just been placed, the match ends immediately.
        // Trigger final scoring here so SGE sees the scored terminal state.
        if (isGameOver()) {
            collectPlayerPoints(true);
        }
    }

    /// doAction
    ///
    /// takes a action either a draw action or a place action depending on the current state. Mutates
    /// the state object it is called on
    ///
    /// @param action the Action object
    public void doAction(CarcassonneAction action){
        if(action.isAction() && drawTile || !action.isAction() && !drawTile){
            throw new ActionMismatchException();
        }

        if (drawTile) {
            PerformActionManager.performDrawAction(this, action.getTileId());
        } else {
            PerformActionManager.performAction(this, action);
        }
    }

    /// nextPlayer
    ///
    /// if currently in a draw action switch to place action and if in place action
    /// switch to draw action.
    ///
    /// Also if a place action was performed is swtiches to the next player and loops
    ///
    /// Note: Does not check if the action was actually performed
    void nextPlayer() {
        if (!drawTile) {
            currentPlayer = (currentPlayer + 1) % gameConfig.playersCount();
        }
        drawTile = !drawTile;
    }

    /// deepCopy
    ///
    /// creates a deep copy of the current state. Works very fast
    /// the only part that is not deepCopied is the tileSpec and config because it is meant to be immutable
    ///
    ///  if downGrade is true the tileSpec and the configuration is also deepCopied. Downgrade means that a canonical
    /// state become noncanonical. In that process those structures are also deep copied to forestall java reflection
    /// attacks
    public State deepCopy(boolean downGrade) {
        var tilesCopy = tiles.deepCopy();
        var areasCopy = areaRegistry.deepCopy();
        var meeplesCopy = meepleRegistry.deepCopy();
        var frontierCopy = frontier.deepCopy();
        var deckCopy = tileDeck.deepCopy();

        var gc = (downGrade && canonical) ? gameConfig.deepCopy():gameConfig;

        return new State(tilesCopy, frontierCopy, areasCopy, meeplesCopy, gc, deckCopy, playerPoints, currentPlayer, drawTile, canonical && !downGrade);
    }

    /// deepCopy
    ///
    /// creates a deep copy of the current state. Works very fast
    /// the only part that is not deepCopied is the tileSpec and config because it is meant to be immutable
    public State deepCopy(){
        return deepCopy(false);
    }

    /// getCurrentPlayer
    ///
    /// returns the player id(zero based) of the player that will next do it's place action
    /// Note: does not care if in draw action. So does not follow sge draw action player convention
    public int getCurrentPlayer() {
        return currentPlayer;
    }

    /// getCurrentTile
    ///
    /// returns the tile that was last drawn. So the tile that is currenty seen
    /// if in a draw action {@code null} is returned
    public TileSpec getCurrentTile() {
        return tileDeck.getCurrentTile();
    }

    /// isGameOver
    ///
    /// return is the game is over or not.
    public boolean isGameOver() {
        return tileDeck.getCurrentTile() == null && tileDeck.getDeckPos() >= tileDeck.getTileCount();
    }

    // private deepcopy constructor
    private State(PositionTileMap tiles, FrontierMap frontier, AreaRegistry areaRegistry, MeepleRegistry meepleRegistry, GameConfiguration gameConfig, model.tile.TileDeck tileDeck, long playerPoints, int currentPlayer, boolean drawTile, boolean canonical) {
        this.tiles = tiles;
        this.frontier = frontier;
        this.areaRegistry = areaRegistry;
        this.meepleRegistry = meepleRegistry;
        this.gameConfig = gameConfig;
        this.tileDeck = tileDeck;
        this.playerPoints = playerPoints;
        this.currentPlayer = currentPlayer;
        this.drawTile = drawTile;
        this.canonical = canonical;
    }

    /// getPlayerPoints
    ///
    /// returns how many points a player hat
    /// @param playerId zero based id of the player
    /// @return points
    public int getPlayerPoints(int playerId) {
        return Player.getPoints(this.playerPoints, playerId);
    }

    public double getUtilityValue(int i) {
        return getCompetitiveUtility(i);
    }

    /// getCompetitiveUtility
    ///
    /// competitive Utility from the view of provided player.
    /// Special Case 2 Players: difference of points: ownPoints - otherPlayerPoints
    /// Generalization:  ownPoints - avgPointsOtherPlayerPoints
    ///
    /// @param playerId playerId(zero based) of the current Player
    /// @return the utilityValue
    public double getCompetitiveUtility(int playerId) {
        int playerCount = gameConfig.playersCount();
        int currentPoints = Player.getPoints(this.playerPoints, playerId);

        int sumOtherPoints = 0;
        for (int i = 0; i < playerCount; i++) {
            if (i != playerId) {
                sumOtherPoints += Player.getPoints(this.playerPoints, i);
            }
        }

        float avgOtherPoints = sumOtherPoints / (float)(playerCount - 1);
        return currentPoints - avgOtherPoints;
    }

    /// getCollaborativeUtility
    ///
    /// @return the sum of the points of all players
    public double getCollaborativeUtility() {
        int playerCount = gameConfig.playersCount();
        int sumPoints = 0;
        for (int i = 0; i < playerCount; i++) {
            sumPoints += Player.getPoints(this.playerPoints, i);
        }
        return sumPoints;
    }

    /// getCurrentPlayerSge
    ///
    /// returns the current player for the sge env. Meaning if in a draw action
    /// returns negative value if in a draw action state and if in a place action the id of the player that is placing the tile
    public int getCurrentPlayerSge() {
        return drawTile ? -(this.currentPlayer + 1) : this.currentPlayer;
    }

    @Override
    public String toString() {
        // builds string for quick state summary. containing deck position, and player points and free meeples of players
        StringBuilder builder = new StringBuilder(String.format("card: \t %d \t", tileDeck.getDeckPos()));
        for (int i = 0; i < this.gameConfig.playersCount(); i++) {
            int points = Player.getPoints(this.playerPoints, i);
            int freeMeeples = this.meepleRegistry.countFreeMeeples(i);
            builder.append(String.format("\tPlayer%d: points=%d \t meeplesFree=%d ", i, points, freeMeeples));
        }
        return builder.toString();
    }

    /// heuristicPrior
    ///
    /// convenience wrapper for {@link HeuristicManager#computePrior(State, int, HeuristicConfiguration)}
    public float heuristicPrior(CarcassonneAction action){
        return HeuristicManager.computePrior(this,action,gameConfig.getDefaultHeuristic());
    }

    // the getter function for non canonical state can simply return the internal structure but canonical need to keep
    // it safe. Thats why for canonical deepCopies are done


    /// getTiles
    ///
    /// return the internal placed tile structure from the state. if canonical returns deepCopy
    ///
    /// Note: do not recommend use for canonical
    public PositionTileMap getTiles() {
        return canonical ? tiles.deepCopy() : tiles;
    }

    /// getAreaRegistry
    ///
    /// return the area registry for managing meeple placements. if canonical returns deepCopy
    ///
    /// Note: do not recommend use for canonical
    public AreaRegistry getAreaRegistry() {
        return canonical ? areaRegistry.deepCopy() : areaRegistry;
    }

    /// getMeepleRegistry
    ///
    /// return the meeple registry storing placed meeples. if canonical returns deepCopy
    ///
    /// Note: do not recommend use for canonical
    public MeepleRegistry getMeepleRegistry() {
        return canonical ? meepleRegistry.deepCopy() : meepleRegistry;
    }

    /// getFrontier
    ///
    /// return the frontier map where tiles can be placed. if canonical returns deepCopy
    ///
    /// Note: do not recommend use for canonical
    public FrontierMap getFrontier() {
        return canonical ? frontier.deepCopy() : frontier;
    }

    /// getTileDeck
    ///
    /// return the tile deck for drawing tiles. if canonical returns deepCopy
    ///
    /// Note: do not recommend use for canonical
    public TileDeck getTileDeck() {
        return canonical ? tileDeck.deepCopy() : tileDeck;
    }

    /// getGameConfig
    ///
    /// return the game configuration
    ///
    /// Note: do not recommend use for canonical.
    public GameConfiguration getGameConfig() {
        return gameConfig;
    }
}
