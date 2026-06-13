package model.heuristic;

/// AreaPointChangeConfig
///
/// @param roadPointsChange      hum much shall a road Area enrich the overallArea in \[-3,3]
/// @param castlePointsChange    hum much shall a  castle Area enrich the overallArea \[-3,3]
/// @param fieldPointsChange     hum much shall a field Area enrich the overallArea \[-3,3]
/// @param monasteryPointsChange hum much shall a monastery Area enrich the overallArea \[-3,3]
public record AreaPointChangeConfig(
        @MutateRange(min = -3, max = 3,init = 1)
        float roadPointsChange,
        @MutateRange(min = -3, max = 3, init =  2)
        float castlePointsChange,
        @MutateRange(min = -3, max = 3, init = 0.1f)
        float fieldPointsChange,
        @MutateRange(min = -3, max = 3, init = 2)
        float monasteryPointsChange
) {}
