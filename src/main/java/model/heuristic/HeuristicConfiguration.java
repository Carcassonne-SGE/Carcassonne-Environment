package model.heuristic;

/// HeuristicConfiguration
///
/// the heuristic calculates a value for how good the position is(x,y,rot) of the now placed tile
/// and how good a meeplePlacement is in that configuration. Those values are then mixed to get
/// hopefully good results
///
/// @param positionHeuristik config for tile placement scoring
/// @param meeplePlacementHeuristic config for meeple placement scoring
/// @param heuristicMixConfig config for combining both scores
public record HeuristicConfiguration(
        PositionHeuristik positionHeuristik,
        MeeplePlacementHeuristic meeplePlacementHeuristic,
        HeuristicMixConfig heuristicMixConfig
) {}
