package model.enums;

/// EdgeType
///
/// Represents the Edge of a tile
///
/// There are Multiple Road Types for different Connections. The Interpretation is that two roads are
/// connected iff they use the Same RoadType
public enum EdgeType {
    FIELD(0),
    CASTLE(1),
    ROAD0(2),
    ROAD1(3),
    ROAD2(4),
    ROAD3(5);

    private final int value;

    EdgeType(int value){
        this.value=value;
    }

    public int getValue(){
        return value;
    }

    public static EdgeType fromRoadId(int roadId) {
        return switch (roadId){
            case 0 -> ROAD0;
            case 1 -> ROAD1;
            case 2 -> ROAD2;
            case 3 -> ROAD3;
            default -> ROAD0;
        };
    }

    public static EdgeType fromValue(int value) {
        return switch (value) {
            case 1 -> CASTLE;
            case 0 -> FIELD;
            case 2 -> ROAD0;
            case 3 -> ROAD1;
            case 4 -> ROAD2;
            case 5 -> ROAD3;
            default -> throw new IllegalArgumentException("Unknown EdgeType value: " + value);
        };
    }

    /// toSimpleInt
    ///
    /// Maps a EdgeType to a int ignoring the Different types of Raods
    public int toSimpleInt(){
        return switch (value){
            case 0 -> 0;
            case 1 -> 1;
            default -> 2;
        };
    }
}