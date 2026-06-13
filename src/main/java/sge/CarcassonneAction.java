package sge;

import model.bits.CarcassonneActionLayoutBit;

/// CarcassonneAction
///
/// this Action encodes an Action for the {@link  CarcassonneGame} to use
/// the constructor takes all required parameter to generate the Action.
///
/// In my understanding the SGE-Env handles Indeterminism by moving the env into an
/// undetermined state and let the SGE choose a random action. So that there is no
/// nondeterminism when performing action. For that to work there are two types of
/// actions a placement Action and a Draw Action. Internally they are both just an
/// int and this method wraps that int to a class and provides some convenience methods
/// All actions have:
///     - isAction in {true,false}  if true normal place Action if false draw action
///     - x in (-128,128)           x pos of tile only used for place actions
///     - y in (-128,128)           y pos of tile only used for place actions
///     - rot in [0,3]              rotation of tile counterclockwise only used for place actions
///     - area id [-1,12]           meeple position if -1 no meeple placement, [0,11] edge area, 12 Monastery  only used for place actions
///                                 note that the localId passed here is not rotated. So the indexing starts from left bottom
///                                 corner and goes clockwise until 11. This indexing is not influenced by the rotation so look
///                                 at it from the default rotation 0
///     - tileId in [0, 255]        id of the drawn tile only used for draw Actions
/// If values are set that are not needed for the type they are ignored
///
/// Internally an action is just an encoded int using BitPacking based on the {@link model.bits.CarcassonneActionLayout}
/// this class is responsible for hiding that fact from the game itself and provide a simple interface.
public  class CarcassonneAction {

    // special area ids with special meaning
    public static final int NO_MEEPLE_AREA_ID = -1;
    public static final int MONASTERY_AREA_ID = 12;

    // internal int representation of the Action
    private final int actionEncoding;


    public CarcassonneAction(boolean isAction, int x, int y,int rotation, int areaId,int tileId) {
        this.actionEncoding = CarcassonneActionLayoutBit.pack(x, y, rotation, areaId, tileId, isAction);
        if (x > 127 || x < -128 || y > 127 || y < -128) {
            throw new IllegalArgumentException("Coordinates out of range");
        }
        if (isAction) {
            if (rotation > 3 || rotation < 0) {
                throw new IllegalArgumentException("Rotation out of range");
            }
            if (areaId < NO_MEEPLE_AREA_ID || areaId > MONASTERY_AREA_ID) {
                throw new IllegalArgumentException("Area ID out of range");
            }
            if (tileId != 0) {
                throw new IllegalArgumentException("Tile ID must be 0 for player actions");
            }
        } else {
            if (tileId < 0 || tileId > 255) {
                throw new IllegalArgumentException("Tile ID out of range");
            }
        }
    }

    /// get Value
    ///
    /// returns the initial representation of the action
    public int getValue(){
        return actionEncoding;
    }

    public boolean isAction() {
        return CarcassonneActionLayoutBit.getIsAction(this.actionEncoding);
    }

    public int getX() {
        return CarcassonneActionLayoutBit.getX(this.actionEncoding);
    }

    public int getY() {
        return CarcassonneActionLayoutBit.getY(this.actionEncoding);
    }

    public int getRotation() {
        return CarcassonneActionLayoutBit.getRotation(this.actionEncoding);
    }

    public int getAreaId() {
        return CarcassonneActionLayoutBit.getAreaId(this.actionEncoding);
    }

    public int getTileId() {
        return CarcassonneActionLayoutBit.getTileId(this.actionEncoding);
    }
    public static boolean areActionsEqual(int action1, int action2) {
        boolean action1IsAction = CarcassonneActionLayoutBit.getIsAction(action1);
        boolean action2IsAction = CarcassonneActionLayoutBit.getIsAction(action2);
        // absolute inefficient could compare the bit pattern but this is clearer to understand
        if (action1IsAction || action2IsAction) {
            return action1IsAction == action2IsAction
                    && CarcassonneActionLayoutBit.getX(action1) == CarcassonneActionLayoutBit.getX(action2)
                    && CarcassonneActionLayoutBit.getY(action1) == CarcassonneActionLayoutBit.getY(action2)
                    && CarcassonneActionLayoutBit.getRotation(action1) == CarcassonneActionLayoutBit.getRotation(action2)
                    && CarcassonneActionLayoutBit.getAreaId(action1) == CarcassonneActionLayoutBit.getAreaId(action2)
                    && CarcassonneActionLayoutBit.getTileId(action1) == CarcassonneActionLayoutBit.getTileId(action2);
        }

        return CarcassonneActionLayoutBit.getTileId(action1) == CarcassonneActionLayoutBit.getTileId(action2);
    }
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof CarcassonneAction that)) return false;
        // got a action. need to differentiate between a draw and a place action. because
        // if we got a draw action we do not care about the stored x,y ...


        return areActionsEqual(actionEncoding, that.actionEncoding);
    }

    @Override
    public int hashCode() {
        return actionEncoding;
    }

    @Override
    public String toString() {
        return "CarcassonneAction{" +
                "actionEncoding=" + actionEncoding +
                '}';
    }
}
