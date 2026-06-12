package model.enums;

/// AreaType Enum
///
/// For each type an area can have there is a different Type the difference to EdgeType is that
/// it not distinguished between the different roads. The information about that is stored in the area long
///
/// This enum is rarely used due to the slowness of Enums in java. But the types and the in values
/// are used consistently though out hte project
public enum AreaType {
    FIELD(0), CASTLE(1), ROAD(2), MONASTERY(3);

    private final int value;

    AreaType(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }

    public static AreaType fromValue(int value) {
        return switch (value) {
            case 1 -> CASTLE;
            case 0 -> FIELD;
            case 2 -> ROAD;
            case 3 -> MONASTERY;
            default -> throw new IllegalArgumentException("invalid AreaType");
        };
    }

    /// fromEdgeType
    ///
    /// maps the Edge Type and the positon on that edge(left, center,right) to the type
    /// so that if the type is road the areas left and right are grass and center is road
    public static AreaType fromEdgeType(EdgeType edgeType, LabelEdgePos labelPosition) {
        var areaType = AreaType.fromValue(edgeType.toSimpleInt());
        if(areaType == AreaType.ROAD && labelPosition != LabelEdgePos.Center){
            return AreaType.FIELD;
        }
        return areaType;
    }

}