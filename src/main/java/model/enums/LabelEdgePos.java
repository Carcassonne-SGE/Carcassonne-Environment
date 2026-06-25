package model.enums;

/// LabelEdgePos
///
/// enum for handling information about the position on an edge.
public enum LabelEdgePos {
    Right(0),
    Center(1),
    Left(2);

    private final int value;

    LabelEdgePos(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }

    public static LabelEdgePos fromValue(int value) {
        return switch (value) {
            case 2-> Left;
            case 1-> Center;
            case 0-> Right;
            default -> throw new IllegalArgumentException();
        };
    }

    public static LabelEdgePos fromLocalId(int localId) {
        return fromValue(localId%3);
    }

    public int toLocalId( Direction direction){
        return direction.getValue()*3 + getValue();
    }

    public LabelEdgePos flipped(){
        return fromValue(2-getValue());
    }

    public static int getLabelEdgePosCount(){
        return 3;
    }
}
