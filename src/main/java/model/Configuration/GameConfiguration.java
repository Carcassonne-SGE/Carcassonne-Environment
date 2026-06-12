package model.Configuration;

import model.heuristic.HeuristicConfiguration;
import model.state.HeuristicManager;
import model.tile.TileSpec;

public final class GameConfiguration {
    private final int seed;
    private final int playersCount;
    private final TileSpec[] tiles;
    private final TileSpec startTile;
    private final boolean determinized;

    private final HeuristicConfiguration defaultHeuristic;
    private final int meeplePerPlayer;


    GameConfiguration(int seed, int playersCount, TileSpec[] tiles, TileSpec startTile, boolean determinized, int meeplePerPlayer) {
        this.seed = seed;
        this.playersCount = playersCount;
        this.tiles = tiles;
        this.startTile = startTile;
        this.determinized = determinized;
        this.meeplePerPlayer = meeplePerPlayer;
        defaultHeuristic = HeuristicManager.createDefaultHeuristic();
    }

    public int seed() {
        return seed;
    }

    public int playersCount() {
        return playersCount;
    }

    public TileSpec[] tiles() {
        return tiles;
    }

    public TileSpec startTile() {
        return startTile;
    }

    /// deepCopy
    ///
    /// creates a deep copy of the game configuration and all configured tile specs
    public GameConfiguration deepCopy() {
        TileSpec[] tilesCopy = new TileSpec[tiles.length];
        for (int i = 0; i < tiles.length; i++) {
            tilesCopy[i] = tiles[i].deepCopy();
        }
        return new GameConfiguration(seed, playersCount, tilesCopy, startTile.deepCopy(), determinized, meeplePerPlayer);
    }

    public boolean determinized() {
        return determinized;
    }

    public int getMeeplePerPlayer() {
        return meeplePerPlayer;
    }

    public HeuristicConfiguration getDefaultHeuristic() {
        return defaultHeuristic;
    }
}
