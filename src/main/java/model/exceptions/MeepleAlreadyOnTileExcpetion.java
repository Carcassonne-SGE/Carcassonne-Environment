package model.exceptions;

/// MeepleAlreadyOnTileExcpetion
///
/// thrown if a meeple is placed on a area that already contains another meeple
public class MeepleAlreadyOnTileExcpetion extends CaracssonneException{
    /// MeepleAlreadyOnTileExcpetion
    ///
    /// @param transitive true if the conflict came only after merging with neighboring areas
    public MeepleAlreadyOnTileExcpetion(boolean transitive) {
        super("Can't place Meepele on Area where another Meeple is already placed, Transitively: "+ transitive);
    }
}
