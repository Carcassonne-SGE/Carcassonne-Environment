package model.enums;

/// Direction
///
/// enum for directions everywhere where directions are used it is always left,top, right, bottom
public enum Direction {
    LEFT(0),
    TOP(1),
    RIGHT(2),
    BOTTOM(3);


    private final int value;

    Direction(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }

    public static Direction fromLabelId(int id){
        return fromValue((id / 3)%4);
    }

    public static Direction fromValue(int value){
        return switch (value){
            case 0 -> LEFT;
            case 1 -> TOP;
            case 2 -> RIGHT;
            case 3 -> BOTTOM;
            default-> throw new IllegalArgumentException();
        };
    }

    public static int getDirectionsCount(){
        return 4;
    }

    public Direction getOpposite(){
        return Direction.fromValue((getValue()+2)%4);
    }
}

