package model.heuristic;

///  PositionHeuristik
///
/// configures how the individual part are combined for the position heuristic. Main goal is to value
/// how this placement would enrich the already existing areas where it prioritizes areas that have
/// already meeples placed on it, so that those get enriched the most
///
/// @param nonMeepleAreaWeight  in \[-1,1] how much areas that do not have a meeple on them yet contribute to the
/// heuristic
/// @param grassSurrogatePoints in \[-1,2) it is to slow to calculate how many castles are completed and touched by
///                             the grass field. So instead this value is used to approximate it very roughly so that
///  grass action still
///                             have a chance to be selected even if we don't know the exact grass points value
/// @param pointChangeConfig    config how the areas should influence the enrichment
/// @param neighborWeight       can control hoch much better positions are with more neighbors so more tiles next to
/// it in \[0,10}
/// @param areaWeight           how much areas influence in [0,4]
/// Note: the ranges are only suggestions and are not checked.
public record PositionHeuristik(
        @MutateRange(min = -1, max = 1, init = 2)
        float nonMeepleAreaWeight,
        AreaPointChangeConfig pointChangeConfig,
        @MutateRange(min = -1, max = 2, init = 0.2f)
        float grassSurrogatePoints,
        @MutateRange(min = 0, max = 10, init = 5)
        float neighborWeight,
        @MutateRange(min = 0, max = 4, init = 2)
        float areaWeight
) {}
