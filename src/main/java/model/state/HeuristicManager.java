package model.state;

import model.area.Area;
import model.bits.AreaLayoutBit;
import model.bits.CarcassonneActionLayoutBit;
import model.bits.PositionLayoutBit;
import model.bits.TileLayoutBit;
import model.collections.ActionManagerMap;
import model.enums.AreaType;
import model.heuristic.AreaPointChangeConfig;
import model.heuristic.HeuristicConfiguration;
import model.heuristic.HeuristicMixConfig;
import model.heuristic.MeeplePlacementHeuristic;
import model.heuristic.PositionHeuristik;
import model.tile.Tile;
import sge.CarcassonneAction;

///  HeuristicManager
///
/// provides functionality that can be used by a agent for guiding the search
/// the standard functionality is to calculate a prior for a given action in
/// a state.
public final class HeuristicManager {

    private static final ThreadLocal<ActionManagerMap> data = ThreadLocal.withInitial(ActionManagerMap::new);
    // prevent instances of that class
    private HeuristicManager(){}

    /// computePrior Heuristic
    ///
    /// computes from a given state, action pair some information on how good that action will
    /// be.
    ///
    /// @param state the state in wich the actin would be executed
    /// @param action the action bit encoded information. GetValue on a action object or pack the value directly using
    /// CarcassonneActionLayoutBit. (done to reduce heap allocations)
    ///
    /// Note: does not check if the action is valid or can be performed. If many calls are done for the same
    /// (x,y) rot pair but different meeple position. May calculate tilePlacementScore first and mix with the meepleScore
    /// manual and cache the tilePlacementScore
    public static float computePrior(State state, int action, HeuristicConfiguration config){
        int x = CarcassonneActionLayoutBit.getX(action);
        int y = CarcassonneActionLayoutBit.getY(action);
        int rot = CarcassonneActionLayoutBit.getRotation(action);
        float tileScore = tilePlacementScore(state, x, y, rot, config.positionHeuristik());
        return computePrior(state, action, tileScore, config);
    }

    public static float computePrior(State state, int action, float tileScore, HeuristicConfiguration config){
        int x = CarcassonneActionLayoutBit.getX(action);
        int y = CarcassonneActionLayoutBit.getY(action);
        int rot = CarcassonneActionLayoutBit.getRotation(action);
        int meepleId = CarcassonneActionLayoutBit.getAreaId(action);

        float meepleScore = meepleScore(state, meepleId, rot, x, y, config.meeplePlacementHeuristic());
        return mixScores(tileScore, meepleScore, config.heuristicMixConfig(), action);
    }

    public static float mixScores(float tilePlacementScore, float meeplePlacementScore, HeuristicMixConfig config, int action) {
        boolean placesMeeple = CarcassonneActionLayoutBit.getAreaId(action) >= 0;

        if (placesMeeple) {
            return tilePlacementScore + (config.mixMeeplePlaced() * meeplePlacementScore) ;
        } else {
            return tilePlacementScore;
        }
    }

    /// meepleScore
    ///
    /// calculates a heuristic prior to a meeple place position in a given state.
    public static float meepleScore(State state, int meepleArea, int rot, int x, int y, MeeplePlacementHeuristic config ) {
        if (meepleArea < 0) {
            return 0;
        }

        float meepleSpendPenalty = meepleSpendPenalty(state, config);

        var areas = state.areaRegistry;
        var tiles = state.tiles;
        var currentTile = state.tileDeck.getCurrentTile();
        int tileId = currentTile.getTileId();

        if (meepleArea == 12) {
            float monasteryHeuristic = 0;
            for (int dx = -1; dx <= 1; dx++) {
                for (int dy = -1; dy <= 1; dy++) {
                    if (dx != 0 || dy != 0) {
                        int otherPos = PositionLayoutBit.rawPack(x + dx, y + dy);
                        long other = tiles.get(otherPos);
                        if (other != 0L) {
                            // got position in the 8-neighborhood where a tile was placed
                            monasteryHeuristic += config.pointChangeConfig().monasteryPointsChange();
                        }
                    }
                }
            }
            return monasteryHeuristic - meepleSpendPenalty;
        }

        // Here we want to calculate how many points would be gathered by placing the meeple now
        // collecting the points
        //
        // here is a big approximation. When placing a tile though transitivity can new areas
        // that had nothing to do with each other be now connected. Do solve that accurately
        // something like calculateLocalAreas from the PossibleActionManger could be used the
        // the problem is that i am not willing to slow down the heuristic so much to do that
        // correctly so instead i just use the local connectivity only the get the points that
        // are gatherer through that meeple placement.
        var map = data.get();
        map.clear();
        float value = 0.0f;
        long wantedAreaRep = areas.findRepresentative(areas.getEdgeArea(tileId, meepleArea, rot));

        for (int dir = 0; dir < 4; dir++) {
            // quick branchless neighbors calculation
            int neighborPos = PositionLayoutBit.rawPack(x + ((dir - 1) & ((dir & 1) - 1)), y + ((2 - dir) * (dir & 1)));
            long neighborTile = tiles.get(neighborPos);
            if (neighborTile != 0L) {
                // has a neighbor at that position
                for (int i = 0; i < 3; i++) {
                    // iterate left, center, right and find area on the neighbor area
                    int localId = dir * 3 + i;
                    long localArea = areas.getEdgeArea(tileId, localId, rot);
                    var localAreaRep = areas.findRepresentative(localArea);
                    int localAreaRepLocalId = AreaLayoutBit.getLocalAreaId(localAreaRep);


                    int otherLocalId = (dir * 3 + 8 - i) % 12;
                    long otherArea = Tile.getEdgeArea(areas, neighborTile, otherLocalId);
                    long otherRep = areas.findRepresentative(otherArea);

                    int otherRepGlobal = Area.getGlobalAreaId(otherRep);
                    if (map.get(otherRepGlobal) == -1 && Area.equals(localAreaRep, wantedAreaRep)) {

                        float storedPoints = AreaLayoutBit.getStoredPoints(otherRep);
                        boolean hasBlazon = state.tileDeck.getCurrentTile().hasBlazon();
                        float areaHeuristic = calculateEdgeAreaHeuristicScore(otherRep, hasBlazon, config.pointChangeConfig());

                        value +=  (storedPoints * config.storedPointsWeight() + areaHeuristic);
                        value-= AreaLayoutBit.getOpenEdgesCounter(otherRep) * config.openEdgesWeight();

                        // remember to only give the heuristic points for that area only once
                        map.add(otherRepGlobal, localAreaRepLocalId);
                    }
                }
            }
        }
        return value - meepleSpendPenalty;
    }

    private static float meepleSpendPenalty(State state, MeeplePlacementHeuristic config) {
        int step = Math.max(0, state.tileDeck.getDeckPos() - 1);
        return config.meepleSpendPenaltySlope() * step + config.meepleSpendPenaltyIntercept();
    }

    /// tilePlacementScore
    ///
    /// provides a score that indicates how well the current placement will increase
    /// the surrounding areas
    public static float tilePlacementScore(State state, int x, int y, int rot, PositionHeuristik config){
        var tiles = state.tiles;
        var areas = state.areaRegistry;
        var currentTile = state.tileDeck.getCurrentTile();
        int tileId = currentTile.getTileId();

        var map = data.get();
        map.clear();

        float edgeHeuristic = 0;
        int neighbourCount = 0;

        // iterate over all areas and and find the area of the neighbor tile that is in front of it
        // and increase heuristic based on how much those areas would be enriched
        for (int dir = 0; dir < 4; dir++) {
            // quick branchless neighbors calculation
            int neighborPos = PositionLayoutBit.rawPack(x + ((dir - 1) & ((dir & 1) - 1)), y + ((2 - dir) * (dir & 1)));
            long neighborTile = tiles.get(neighborPos);
            if (neighborTile != 0L) {
                neighbourCount += 1;
                // has a neighbor at that position
                for (int i = 0; i < 3; i++) {
                    // iterate left, center, right and find area on the neighbor area
                    int localId = dir * 3 + i;
                    long localArea = areas.getEdgeArea(tileId, localId, rot);
                    var localAreaRep = areas.findRepresentative(localArea);
                    int localAreaRepLocalId = AreaLayoutBit.getLocalAreaId(localAreaRep);


                    int otherLocalId = (dir * 3 + 8 - i) % 12;
                    long otherArea = Tile.getEdgeArea(areas, neighborTile, otherLocalId);
                    long otherRep = areas.findRepresentative(otherArea);

                    int otherRepGlobal = Area.getGlobalAreaId(otherRep);
                    if(map.get(otherRepGlobal) == -1){
                        // always increase heuristic for each distinct area once. So check if rep has been seen
                        // and store the rep int he map

                        float storedPoints = AreaLayoutBit.getStoredPoints(otherRep);
                        if(Area.getAreaTypeTyped(otherRep) == AreaType.FIELD) {
                            storedPoints = config.grassSurrogatePoints();
                        }
                        float areaHeuristic = calculateEdgeAreaHeuristicScore(otherRep,currentTile.hasBlazon(),config.pointChangeConfig());
                        float weight = Area.hasMeeple(otherRep) ? 1 : config.nonMeepleAreaWeight();
                        edgeHeuristic += (storedPoints+areaHeuristic*config.areaWeight())*weight;

                        // remember to only give the heuristic points for that area only once
                        map.add(otherRepGlobal,localAreaRepLocalId);
                    }

                    // Note do not need to give areas that are not connected to anything else a heuristic value
                    // a tile does not enrich itself in the way the other semantic is meant because that enrichment
                    // is always present in all rotations(also makes this method faster :=) )
                }
            }
        }

        // need to handle monastery
        float monasteryHeuristic = 0;
        // handle monastery
        for (int dx = -1; dx <= 1; dx++) {
            for (int dy = -1; dy <= 1; dy++) {
                if (dx != 0 || dy != 0) {
                    int otherPos = PositionLayoutBit.rawPack(x + dx, y + dy);
                    long other = tiles.get(otherPos);
                    if (other != 0L) {
                        // got position in the 8-neighborhood where a tile was placed
                        if (TileLayoutBit.getMonastery(other)) {
                            long monasteryArea = areas.getMonasteryArea(TileLayoutBit.getTileId(other));
                            if (Area.hasMeeple(areas.findRepresentative(monasteryArea))) {
                                monasteryHeuristic += config.pointChangeConfig().monasteryPointsChange();
                            }
                        }
                    }
                }
            }
        }

        // encourage higher neighbor count
        float neighbourHeuristic = neighbourCount * config.neighborWeight();

        return edgeHeuristic + monasteryHeuristic + neighbourHeuristic;
    }
    private static float calculateEdgeAreaHeuristicScore(long area, boolean hasBlazon, AreaPointChangeConfig config){
        var areaType = Area.getAreaTypeTyped(area);
        if (areaType == AreaType.ROAD) {
            return config.roadPointsChange();
        } else if (areaType == AreaType.CASTLE) {
            return (hasBlazon ? 2 : 1)*config.castlePointsChange();
        } else if (areaType == AreaType.FIELD) {
            return config.fieldPointsChange();
        }
        return 0;
    }



    /// wrapper for the computePrior(State state, int action) to support the public action object interface
    /// may be slower due to heap interactions. Just calls action.getValue and calls the other method
    public static float computePrior(State state, CarcassonneAction action, HeuristicConfiguration config){
        return computePrior(state,action.getValue(),config);
    }


    public static HeuristicConfiguration createDefaultHeuristic() {
        return new HeuristicConfiguration(
                new PositionHeuristik(0.8905984f, new AreaPointChangeConfig(3.0f, 3.0f, 0.8194832f, 2.421291f), -0.86330575f, 2.7497416f, 0.5292585f),
                new MeeplePlacementHeuristic(new AreaPointChangeConfig(-2.9060357f, 2.9741135f, -2.9998648f, 3.0f), 4.890546f, -0.48826158f, 0.008647887f, 1.3024675f),
                new HeuristicMixConfig(0.9999822f, 0.13731274f)
        );
    }
}
