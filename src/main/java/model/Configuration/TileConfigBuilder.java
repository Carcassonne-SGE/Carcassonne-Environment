package model.Configuration;


import model.tile.TileSpec;

import java.util.Arrays;

/// TileConfigBuilder
///
/// Is a Builder that is used by the {@link GameConfigBuilder} to create a Tile
/// Call the Methods to specify the fields if the configuration is done
/// the endTile Methode needs to be called for fluent building. Do not use
/// this Class Independently form the {@link GameConfigBuilder}
///
/// The Default Tile has at all Edges Grass and the Methods change those
/// Edges accordingly. Additionally a Tile can have/be a Monastery (default is false)
/// The Blazon is specified for the hole Tile so all Castle on this
/// tile are marked with that(default false)
///
/// the occurrences specify how often a Tile is in the Final Game Deck default is 1
///
/// The Castles are by default not connected and each Castle Edge is its own castle by setting
/// connectCastles to true they are connected to one big Castle(important to specify all types
/// tile shapes)
///
/// To Specify a Edge first select the direction then call the method that performs the
/// desired transformation of the Edge
///
/// The Last set values are used and Edge Operations can overwrite each other
public class TileConfigBuilder {
    // Parent Builder used for endTile Method
    private final GameConfigBuilder gameConfigBuilder;
    private final model.enums.EdgeType[] edgeTypes;
    private int occurrences = 1;
    private boolean hasBlazon;
    private boolean isMonastery;
    private boolean connectCastles;
    private int graphicsId;

    private int roadId = 0;

    public TileSpec build(int tileId){
        return new TileSpec(edgeTypes, hasBlazon, isMonastery, connectCastles, tileId, graphicsId);
    }

    public TileConfigBuilder(GameConfigBuilder gameConfigBuilder) {
        this.gameConfigBuilder = gameConfigBuilder;

        this.edgeTypes = new model.enums.EdgeType[4];
        Arrays.fill(edgeTypes, model.enums.EdgeType.FIELD);
    }

    TileConfigBuilder(){
        this(null);
    }

    private model.enums.EdgeType getNextRoad() {
        var edge = model.enums.EdgeType.fromRoadId(roadId);
        this.roadId = (this.roadId + 1) % 4;
        return edge;
    }


    /// markAsHasBlazon
    ///
    /// set hasBlazon to true for meaning read the docstring of {@link TileConfigBuilder} class
    public TileConfigBuilder markAsHasBlazon() {
        hasBlazon = true;
        return this;
    }

    /// markAsConnectCastles
    ///
    /// set ConnectedCastles to true for meaning read the docstring of {@link TileConfigBuilder} class
    public TileConfigBuilder markAsConnectCastles() {
        connectCastles = true;
        return this;
    }

    /// markAsIsMonastery
    ///
    /// set isMonastery to true for meaning read the docstring of {@link TileConfigBuilder} class
    public TileConfigBuilder markAsIsMonastery() {
        isMonastery = true;
        return this;
    }

    /// setOccurrences
    ///
    /// sets the absolut frequency of the current tile in the deck
    public TileConfigBuilder setOccurrences(int occurrences) {
        this.occurrences = occurrences;
        return this;
    }

    public TileConfigBuilder setGraphicsId(int graphicsId) {
        this.graphicsId = graphicsId;
        return this;
    }

    /// starts the configuration of the top Edge
    public EdgeConfigBuilder top() {
        return new EdgeConfigBuilder(model.enums.Direction.TOP);
    }

    /// starts the configuration of the bottom Edge
    public EdgeConfigBuilder bottom() {
        return new EdgeConfigBuilder(model.enums.Direction.BOTTOM);
    }

    /// starts the configuration of the left Edge
    public EdgeConfigBuilder left() {
        return new EdgeConfigBuilder(model.enums.Direction.LEFT);
    }

    /// starts the configuration of the right Edge
    public EdgeConfigBuilder right() {
        return new EdgeConfigBuilder(model.enums.Direction.RIGHT);
    }

    public GameConfigBuilder endTile() {
        return gameConfigBuilder;
    }


    /// EdgeConfigBuilder
    ///
    /// Subclass for building and configuring the Edge of a Tile needed to support a fluent Api
    /// style configuration
    public class EdgeConfigBuilder {
        private final model.enums.Direction direction;

        public EdgeConfigBuilder(model.enums.Direction direction) {
            this.direction = direction;
        }

        public TileConfigBuilder markAsCastle() {
            edgeTypes[direction.getValue()] = model.enums.EdgeType.CASTLE;
            return TileConfigBuilder.this;
        }

        public TileConfigBuilder markAsField() {
            edgeTypes[direction.getValue()] = model.enums.EdgeType.FIELD;
            return TileConfigBuilder.this;
        }


        public TileConfigBuilder markAsRoadAndAsEnd() {
            model.enums.EdgeType nextRoad = getNextRoad();
            edgeTypes[direction.getValue()] = nextRoad;
            return TileConfigBuilder.this;
        }

        public TileConfigBuilder markAsRoadAndConnectTop() {
            model.enums.EdgeType nextRoad = getNextRoad();
            edgeTypes[direction.getValue()] = nextRoad;
            edgeTypes[model.enums.Direction.TOP.getValue()] = nextRoad;
            return TileConfigBuilder.this;
        }

        public TileConfigBuilder markAsRoadAndConnectBottom() {
            model.enums.EdgeType nextRoad = getNextRoad();
            edgeTypes[direction.getValue()] = nextRoad;
            edgeTypes[model.enums.Direction.BOTTOM.getValue()] = nextRoad;
            return TileConfigBuilder.this;
        }

        public TileConfigBuilder markAsRoadAndConnectLeft() {
            model.enums.EdgeType nextRoad = getNextRoad();
            edgeTypes[direction.getValue()] = nextRoad;
            edgeTypes[model.enums.Direction.LEFT.getValue()] = nextRoad;
            return TileConfigBuilder.this;
        }

        public TileConfigBuilder markAsRoadAndConnectRight() {
            model.enums.EdgeType nextRoad = getNextRoad();
            edgeTypes[direction.getValue()] = nextRoad;
            edgeTypes[model.enums.Direction.RIGHT.getValue()] = nextRoad;
            return TileConfigBuilder.this;
        }
    }


    public boolean isHasBlazon() {
        return hasBlazon;
    }

    public int getOccurrences() {
        return occurrences;
    }

    public model.enums.EdgeType[] getEdgeTypes() {
        return edgeTypes;
    }

    public boolean isMonastery() {
        return isMonastery;
    }

    public boolean isConnectCastles() {
        return connectCastles;
    }
}
