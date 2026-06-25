package model.exceptions;

/// InvalidMeeplePositionException
///
/// thrown if a meeple should be placed on a local area that is not valid for that tile
public class InvalidMeeplePositionException extends CaracssonneException{
    /// InvalidMeeplePositionException
    ///
    /// @param tileId tile where the invalid meeple position was used
    /// @param areaId local area id that is invalid on that tile
    public InvalidMeeplePositionException(int tileId, int areaId) {
        super(String.format("Can't be placed at this position on that tileType tileId %d and areaId %d",tileId,areaId));
    }
}
