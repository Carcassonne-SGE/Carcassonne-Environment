package model.heuristic;

/// HeuristicMixConfig
///
/// @param mixMeeplePlaced mix factor if a meeple is placed
/// @param mixMeepleNotPlaced mix factor if no meeple is placed
public record HeuristicMixConfig(
        @MutateRange(min = 0, max = 1, init =  1.0f)
        float mixMeeplePlaced,
        @MutateRange(min = 0, max = 1, init =  0.4f)
        float mixMeepleNotPlaced
) {}
