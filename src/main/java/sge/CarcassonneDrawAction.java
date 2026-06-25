package sge;

/// CarcassonneDrawAction
///
/// represents a draw action. If the game is in a indeterminate state(tile placed and no new tile drawn)
/// this action can be used to draw a tile from the deck. The tile needs to be still in the game so not placed
/// and not discarded
public class CarcassonneDrawAction extends CarcassonneAction{
    public CarcassonneDrawAction(int tileId){
        super(false,0,0,0,0,tileId);
    }

    
}
