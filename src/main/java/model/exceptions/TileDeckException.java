package model.exceptions;

/// TileDeckException
///
/// thrown if a tile deck action is impossible in the current deck state
public class TileDeckException extends CaracssonneException{
    /// TileDeckException
    ///
    /// @param message concrete tile deck error message
    public TileDeckException(String message) {
        super("error while performing a tile deck action "+ message);
    }
}
