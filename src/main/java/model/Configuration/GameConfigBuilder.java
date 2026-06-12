package model.Configuration;

import model.exceptions.CaracssonneException;
import model.tile.TileSpec;

import java.util.ArrayList;
import java.util.List;

/// GameConfigBuilder
///
/// is a Class that helps to create Configurations Objects which specifies a Ruleset that is
/// used in the Env
///
/// It Allows for general Configuration and for specification what tiles are used and how frequent they are
///
/// The Builder uses always the last set value
public class GameConfigBuilder {
    private int seed;
    private int playerCount;
    private boolean determinized;
    private int meeplePerPlayer = 7;
    private TileConfigBuilder startTileBuilder;
    private final List<TileConfigBuilder> gameTilesBuilderList = new ArrayList<>(75);


    public GameConfigBuilder setRandomSeed(int seed) {
        this.seed = seed;
        return this;
    }

    public GameConfigBuilder setPlayerCount(int playerCount) {
        if(playerCount < 2 || playerCount >= 5){
            throw  new CaracssonneException("invalid player count "+playerCount);
        }
        this.playerCount = playerCount;
        return this;
    }

    public GameConfigBuilder setDeterminized(boolean determinized) {
        this.determinized = determinized;
        return this;
    }

    public GameConfigBuilder setMeeplePerPlayer(int meeplePerPlayer) {
        this.meeplePerPlayer = meeplePerPlayer;
        return this;
    }


    public TileConfigBuilder addTile() {
        var field = new TileConfigBuilder(this);
        gameTilesBuilderList.add(field);
        return field;
    }

    public TileConfigBuilder setStartField() {
        startTileBuilder = new TileConfigBuilder(this);
        return startTileBuilder;
    }

    /// build and create the configration shall only be called once
    ///
    /// assumes setStartFiled was called
    public GameConfiguration build(boolean useOccurrences) {
        int tileCount = gameTilesBuilderList.stream()
                .mapToInt(tile -> useOccurrences ? tile.getOccurrences() : 1)
                .sum();


        if(tileCount >126){
            throw  new CaracssonneException("No many Tiles in Configuration maximum ist 125 but got "+gameTilesBuilderList);
        }

        TileSpec[] tiles = new TileSpec[tileCount];
        int head = 0;
        for (TileConfigBuilder tileBuilder : gameTilesBuilderList) {
            int realOccurrences = useOccurrences ? tileBuilder.getOccurrences() : 1;
            for (int i = 0; i < realOccurrences; i++) {
                tiles[head] = tileBuilder.build(head);
                head++;
            }
        }
        TileSpec startTile = startTileBuilder.build(head);
        return new GameConfiguration(seed, playerCount, tiles, startTile, determinized, meeplePerPlayer);
    }
}
