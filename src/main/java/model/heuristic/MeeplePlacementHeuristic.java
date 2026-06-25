package model.heuristic;

/// MeeplePlacementHeuristic
///
/// @param pointChangeConfig  config how the areas should influence the enrichment
/// @param storedPointsWeight how much of the are heuristic are the stored points of the area
/// @param openEdgesWeight    how much open edges influence the score
/// @param meepleSpendPenaltySlope slope k of the linear meeple spend penalty f(x)=kx+d
/// @param meepleSpendPenaltyIntercept intercept d of the linear meeple spend penalty f(x)=kx+d
public record MeeplePlacementHeuristic(
        AreaPointChangeConfig pointChangeConfig,
        @MutateRange(min = -1, max = 5, init = 2)
        float storedPointsWeight,

        @MutateRange(min=-1, max= 5, init = 0)
        float openEdgesWeight,

        @MutateRange(min = -10, max = 10, init = -0.05f)
        float meepleSpendPenaltySlope,

        @MutateRange(min = -30, max = 30, init = 3)
        float meepleSpendPenaltyIntercept
) {}
