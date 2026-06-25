package sge;

/// CarcassonnePlaceAction
///
/// normal place action. If tile was drawn this action represents a player placing a tile
/// at a position, rotation and if a meeple is placed and if so where
public class CarcassonnePlaceAction extends CarcassonneAction{
    public CarcassonnePlaceAction( int x, int y,int rotation, int areaId) {
        super(true,x,y,rotation,areaId,0);
    }

    public static CarcassonnePlaceAction placeTileNoMeeple(int x, int y,int rotation){
        return new CarcassonnePlaceAction(x,y,rotation,-1);
    }
    public static CarcassonnePlaceAction placeTileMonastery(int x, int y,int rotation){
        return new CarcassonnePlaceAction(x,y,rotation,12);
    }
    public static CarcassonnePlaceAction placeTileEdgeArea(int x, int y,int rotation, int edgeArea){
        if(edgeArea < 0 || edgeArea > 11){
            throw new IllegalArgumentException("Given Area is not a Edge area should be in [0,11] ");
        }
        return new CarcassonnePlaceAction(x,y,rotation,edgeArea);
    }
}
