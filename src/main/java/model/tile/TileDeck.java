package model.tile;

import model.Configuration.GameConfiguration;
import model.collections.FrontierMap;
import model.exceptions.TileDeckException;
import model.state.State;

import java.util.Random;

/// TileDeck
///
/// Models the mutable deck of a Carcassonne game.
/// The remaining tiles are stored in {@code deckOrder} and draws are realized by swapping one
/// random remaining tile to the current {@code deckPos}. The tileDeck itself does not store an rng state
/// relies on passing a Random object to it
///
/// The TileDeck also keeps track of the current Open Tile(so the last drawn one)
///
/// provides functionality to determinize the deck. Which eagerly shuffles the deck and prevents lazy
/// shuffling on draw events. This is not usefully for normal playthroughs but very usefull for mcts Simulations
public class TileDeck {
    private static final ThreadLocal<int[]> SOFT_FIND_SCRATCH = ThreadLocal.withInitial(() -> new int[0]);
    private boolean determinized;                 // to store if deck is determinized or not

    private final GameConfiguration config;       // config contains all tile descriptions
    private final int[] deckOrder;                // stores for all tiles the ids and used for draw

    private int deckPos;                          // current head in the deckPos
    private TileSpec currentTile;                 // currentTile Cache



    /// TileDeck
    ///
    /// Creates a fresh deck from the configured game tiles.
    /// The order is not shuffled eagerly, instead draws use the seeded RNG to select one of the
    /// remaining entries on demand.
    public TileDeck(GameConfiguration config) {
        assert  config != null;
        this.config = config;
        TileSpec[] tiles = config.tiles();

        // need to do slow manual copy to keep the config interface to the outside
        deckOrder = new int[tiles.length];
        for (int i = 0; i < tiles.length; i++) {
            deckOrder[i] = tiles[i].getTileId();
        }

        if (config.determinized()) {
            // check if config wants a determinized deck
            determinize(new Random(config.seed()));
        }
    }

    public TileSpec getCurrentTile() {
        return currentTile;
    }

    public void setCurrentTile(TileSpec currentTile) {
        this.currentTile = currentTile;
    }

    /// findNextTile
    ///
    /// Uses the {@link #popGetTile(Random)} to draw tiles untile one has been found that has atleas
    /// one valid placement. If during this process tiles are found that can't be placed they are discarded
    /// from the game
    ///
    /// @param random
    /// @param frontier
    ///
    /// @return {@code null}
    ///
    /// Draws tiles until a tile is found that can be placed somewhere on the current frontier.
    /// Tiles that cannot be placed are discarded and stay consumed. Returns {@code null} if the
    /// deck is exhausted without finding a playable tile.
    ///
    /// Note: if a tile is drawn that is not playable it is permanently removed from the deck for that game
    public TileSpec findNextTile(Random random, FrontierMap frontier) {
        assert frontier != null && random != null;
        TileSpec tile;
        while ((tile = popGetTile(random)) != null) {
            if (hasAnyPlacement(frontier, tile.getTileId())) {
                return tile;
            }
        }
        return null;
    }

    /// findNextTileSoft
    ///
    /// Returns the same tile that {@link #findNextTile(Random, FrontierMap)} would return, but does not
    /// consume this deck.
    ///
    /// Note: works the same as the other method if the rng state would be the same
    public TileSpec findNextTileSoft(Random random, FrontierMap frontier) {
        if (determinized) {
            for (int i = deckPos; i < deckOrder.length; i++) {
                int tileId = deckOrder[i];
                if (hasAnyPlacement(frontier, tileId)) {
                    return config.tiles()[tileId];
                }
            }
            return null;
        }

        int[] scratch = getSoftFindScratch();
        System.arraycopy(deckOrder, deckPos, scratch, deckPos, deckOrder.length - deckPos);

        for (int virtualPos = deckPos; virtualPos < deckOrder.length; virtualPos++) {
            int remaining = deckOrder.length - virtualPos;
            int drawIndex = virtualPos + random.nextInt(remaining);
            int drawnTileId = scratch[drawIndex];
            scratch[drawIndex] = scratch[virtualPos];
            scratch[virtualPos] = drawnTileId;
            if (hasAnyPlacement(frontier, drawnTileId)) {
                return config.tiles()[drawnTileId];
            }
        }
        return null;
    }

    /// popGetTile
    ///
    /// Draws the next random remaining tile and returns its {@link TileSpec}.
    /// Returns {@code null} if the deck is already exhausted.
    public TileSpec popGetTile(Random random) {
        if (deckPos >= deckOrder.length) {
            return null;
        }
        return config.tiles()[popTile(random)];
    }

    /// popForceGetTile
    ///
    /// Forces the tile with the given tile id to be the next drawn tile and returns its
    /// {@link TileSpec}. This is usefully for an MCTS Agent that does not rely on deep copies but just
    /// reapplies the actions
    ///
    /// @throws TileDeckException if TileId was already placed or not found
    public TileSpec popForceGetTile(int tileId) {
        return config.tiles()[popTileForce(tileId)];
    }


    /// popTile
    ///
    /// Draws one random remaining tile id.
    /// The method performs one step of a  Fisher-Yates shuffle by picking a random remaining
    /// index, swapping it to the current deck head and advancing {@code deckPos}.
    public int popTile(Random random) {
        int remaining = deckOrder.length - deckPos;
        if (remaining <= 0) {
            throw new IllegalStateException("deck exhausted");
        }
        int drawIndex = determinized ? deckPos : deckPos + random.nextInt(remaining);
        int tileIndex = deckOrder[drawIndex];
        swap(drawIndex, deckPos);
        deckPos++;
        return tileIndex;
    }

    /// popTileForce
    ///
    /// Forces the tile with the given id to the current deck head and advances the deck.
    /// Returns the provided tile id so caller can directly index into the configured tile array.
    ///
    /// Note: tiled needs to be in the remaining deck
    /// @throws TileDeckException if TileId was already placed or not found
    public int popTileForce(int tileId) {
        for (int i = deckPos; i < deckOrder.length; i++) {
            if (deckOrder[i] == tileId) {
                swap(i, deckPos);
                deckPos += 1;
                return tileId;
            }
        }
        throw new TileDeckException("the tile id was already placed or simply does not exist");
    }

    /// drawTileIdSoft
    ///
    /// Selects a random remaining tile id without consuming it.
    /// Does not check whether the tile is placeable and does not modify the deck state.
    /// Returns {@code -1} if the deck is already exhausted.
    public int drawTileIdSoft(Random random){
        int remaining = deckOrder.length - deckPos;
        if (remaining <= 0) {
            return -1;
        }
        int drawIndex = determinized ? deckPos : deckPos + random.nextInt(remaining);
        return deckOrder[drawIndex];
    }

    /// determinize
    ///
    /// Shuffles the remaining part of the deck once and then keeps this order fixed for future draws.
    public void determinize(Random random) {
        for (int i = deckOrder.length - 1; i > deckPos; i--) {
            int j = deckPos + random.nextInt(i - deckPos + 1);
            swap(i, j);
        }
        determinized = true;
    }

    private void swap(int i, int j) {
        int temp = deckOrder[i];
        deckOrder[i] = deckOrder[j];
        deckOrder[j] = temp;
    }

    private int[] getSoftFindScratch() {
        int[] scratch = SOFT_FIND_SCRATCH.get();
        if (scratch.length < deckOrder.length) {
            scratch = new int[deckOrder.length];
            SOFT_FIND_SCRATCH.set(scratch);
        }
        return scratch;
    }

    /// deepCopy
    public TileDeck deepCopy() {
        return new TileDeck(deckOrder.clone(), deckPos, determinized, config, currentTile);
    }

    /// private deep copy constructor
    private TileDeck(int[] deckOrder, int deckPos, boolean determinized, GameConfiguration config,TileSpec currentTile) {
        this.deckOrder = deckOrder;
        this.deckPos = deckPos;
        this.determinized = determinized;
        this.config = config;
        this.currentTile = currentTile;
    }

    public int getDeckPos() {
        return deckPos;
    }


    /// unPop
    ///
    /// undu the last draw
    public void unPop(){
        assert  deckPos > 0;
        deckPos -= 1;
    }

    /// hasAnyPlacement
    ///
    /// Checks if the provided tile has at least one legal placement on the current frontier in
    /// any different rotation.
    ///
    /// Note: iterate over all possible action and stop when one has been found
    public boolean hasAnyPlacement(FrontierMap frontier, int tiled) {
        assert tiled >= 0 && frontier != null;

        var tileSpec = config.tiles()[tiled];
        int rotationCount = tileSpec.getDifferentRotationsCount();
        int tileEdges = tileSpec.getTileTemplate();

        // iterate over frontier with on hell of a for loop
        for (int frontierIndex = frontier.getNextIndex(0);
             frontierIndex != FrontierMap.EMPTY;
             frontierIndex = frontier.getNextIndex(frontierIndex + 1)) {


            int constraints = frontier.getValueAt(frontierIndex);
            for (int rot = 0; rot < rotationCount; rot++) {
                // iterate over all non-identical rotations of the tile
                if (Tile.isPlacementValid(constraints, tileEdges, rot)) {
                    return true;
                }
                // meeples are not important for checking if there exists a placement so this can be skiped
            }
        }
        return false;
    }

    public int getTileCount(){
        return deckOrder.length;
    }

    public int getAt(int index){
        return deckOrder[index];
    }
}
