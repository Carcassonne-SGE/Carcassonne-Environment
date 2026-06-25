package model.exceptions;

/// InvalidTilePlacement
///
/// thrown if a tile is placed at a position or rotation that violates the edge constraints
public class InvalidTilePlacement extends CaracssonneException{
    /// InvalidTilePlacement
    ///
    /// @param x x position of the invalid placement
    /// @param y y position of the invalid placement
    /// @param rot rotation of the invalid placement
    /// @param tileId tile that was tried to place
    public InvalidTilePlacement(int x, int y, int rot, int tileId) {
        super(String.format("The tile placement of tileId %d at position (%d,%d) and rotation %d is invalid",tileId,x,y,rot));
    }
}
