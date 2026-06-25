package model.exceptions;

/// NoMeepleLeftException
///
/// thrown if a player wants to place a meeple but already used all own meeples
public class NoMeepleLeftException extends CaracssonneException{
    /// NoMeepleLeftException
    ///
    /// @param playerId player that has no meeple left
    public NoMeepleLeftException(int playerId) {
        super(String.format("Can't place a meeple the player %d has no meeple left", playerId));
    }
}
