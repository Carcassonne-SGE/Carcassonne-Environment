package model.area;

import model.bits.AreaLayoutBit;
import model.enums.AreaType;


/// Area
///
/// Meeples can be placed on different parts of a Tile to capture this, the concept
/// of an Area is introduced. Everything on which a Meeple can be placed is considered
/// a Area.
///
/// The Most important feature is that Areas can be merged to larger Area. Those merged Areas
/// create a "new" Area containing the tiles from both
///
/// To Implement this concept of a Area a Union-Find Data-Structure was used. An Area is here
/// a Set containing the individual sub areas. Basically each area has a parent. The top nodes have
/// itself as parent. If i want to check if two areas are part of the same area i go this chain up
/// and compare the "representatives". To make that fast, on each way up the path is halved so that
/// overtime the chain get really tiny
///
/// One thing is that the representatives need to be recalculated every time they are accessed
///
/// Originally the Area class held all information this was to slow. Now the area information
/// is stored in a long(which makes to copy an area extremely fast). This Area class provides helper
/// functions
///
/// the representative stores additional important information
public class Area {
    private static final int AREAS_PER_TILE = 13;

    public static AreaType getAreaTypeTyped(long area) {
        return AreaType.fromValue(AreaLayoutBit.getAreaType(area));
    }

    public static int getGlobalAreaId(long area) {
        return AreaLayoutBit.getTileId(area) * AREAS_PER_TILE + AreaLayoutBit.getLocalAreaId(area);
    }

    /// CreateArea
    ///
    /// Returns a new Area with the specified information
    public static long CreateArea(AreaType areaType, int localAreaId, int tileId, boolean hasBlazon) {
        int globalAreaId = AreaRegistry.getGlobalAreaIdUnchecked(tileId, localAreaId);
        if (localAreaId < 0 || localAreaId >= 13 || (areaType == AreaType.MONASTERY && localAreaId != 12)) {
            throw new IllegalArgumentException();
        }
        int points = getInitialPoints(areaType, hasBlazon);
        int openEdges = areaType == AreaType.MONASTERY ? 8 : 1;
        return AreaLayoutBit.rawPack(areaType.getValue(), localAreaId, tileId, points, openEdges, 0, 1, globalAreaId);
    }

    /// getInitialPoints
    ///
    /// returns the initial points for the given areaType
    private static int getInitialPoints(AreaType areaType, boolean hasBlazon) {
        if (areaType == AreaType.ROAD) {
            return 1;
        } else if (areaType == AreaType.CASTLE) {
            return (hasBlazon ? 2 : 1);
        } else if (areaType == AreaType.FIELD) {
            return 0;
        } else if (areaType == AreaType.MONASTERY) {
            return 1;
        }
        return 0;
    }

    /// merge
    ///
    /// Two areas are merged into one does everything there is to do. Takes an array where the areas
    /// are stored is and the Indices of both areas. The result changes the content of the array
    ///
    /// The merging is not balanced. Always merge b to a. So a is new parent of b
    /// if mergePoints is true the points of both areas are added
    ///
    /// Note: both area Indices need to be representatives
    public static void merge(long[] areas, int aIndex, int bIndex, boolean mergePoints) {
        // get values
        long a = areas[aIndex];
        long b = areas[bIndex];
        int bSize = AreaLayoutBit.getSize(b);
        int bOpenEdges = AreaLayoutBit.getOpenEdgesCounter(b);
        int bStoredPoints = AreaLayoutBit.getStoredPoints(b);
        int bMaxMeeples = AreaLayoutBit.getMaxMeeples(b);
        int aGlobalId = getGlobalAreaId(a);

        // do merging
        areas[bIndex] = AreaLayoutBit.setParentGlobalAreaId(b, aGlobalId);
        if (mergePoints) {
            areas[aIndex] = AreaLayoutBit.addStoredPoints(a, bStoredPoints);
        }
        // update parent information
        areas[aIndex] = AreaLayoutBit.addOpenEdgesCounter(areas[aIndex], bOpenEdges);
        areas[aIndex] = AreaLayoutBit.addSize(areas[aIndex], bSize);
        areas[aIndex] = AreaLayoutBit.setMaxMeeples(areas[aIndex], Math.max(AreaLayoutBit.getMaxMeeples(a), bMaxMeeples));
    }

    /// updateCompleteness
    ///
    /// each area stores a Completeness counter/ openEdgesCounter which is used do determine
    /// if an area is completed or not. If the counter is zero it is completed. This method
    /// shall be called every time an event happens that influences that counter. Those events where
    /// it needs to be called depend on the AreaType
    ///
    /// if AreaType is not Monastery the completeness counter is initialized with 1. In this
    /// context it is more like open-Edges/ open-Areas counter. So each area is open and needs to
    /// be closed and this counts that. In case two areas are merged this method shall be called to
    /// reduce the counter by -2
    ///
    /// If it is a monastery the initial completeness Counter is 8 and every time a neighbouring tile is placed
    /// this method shall be called. It reduces the counter by one. So that if there are 8 neighbours the
    /// counter is zero
    public static void updateCompleteness(long[] areas, long areaR) {
        int index = getGlobalAreaId(areaR);
        long area = areas[index];
        if (AreaLayoutBit.getAreaType(area) == 3) {
            areas[index] = AreaLayoutBit.addOpenEdgesCounter(area, -1);
        } else {
            areas[index] = AreaLayoutBit.addOpenEdgesCounter(area, -2);
        }
    }

    /// getStoredPoints
    ///
    /// returns the current stored points in an area. Takes if the game is over or not
    /// points according to the rule of Carcassonne 
    /// Note: area is assumed to be a representatives
    public static int getStoredPoints(long area, boolean done) {
        if (AreaLayoutBit.getAreaType(area) == AreaType.MONASTERY.getValue()) {
            return 9 - AreaLayoutBit.getOpenEdgesCounter(area);
        }
        int storedPoints = AreaLayoutBit.getStoredPoints(area);
        if (!done && AreaLayoutBit.getAreaType(area) == AreaType.CASTLE.getValue()) {
            return storedPoints * 2;
        }
        return storedPoints;
    }

    /// isCompleted
    ///
    /// helper function to check if a area is completed so if the edges-Counter is zero
    /// Note: assumes that the area is a representative
    public static boolean isCompleted(long area) {
        return AreaLayoutBit.getOpenEdgesCounter(area) == 0;
    }

    /// isRepresentative
    ///
    /// Helper function to check if the given area is already a representative
    public static boolean isRepresentative(long area) {
        return AreaLayoutBit.getParentGlobalAreaId(area) == getGlobalAreaId(area);
    }

    ///  hasMeeple
    ///
    /// helper function to check if a area has least 1 meeple pleaded on it
    /// Note: assumes area is a Representative
    public static boolean hasMeeple(long area) {
        return AreaLayoutBit.getMaxMeeples(area) > 0;
    }

    /// equals
    ///
    /// checks if the globalAreaId of two areas is the same
    /// is needed in the case where two areas are the same but one is maybe older and different
    public static boolean equals(long a, long b) {
        int aGlobalId = getGlobalAreaId(a);
        int bGlobalId = getGlobalAreaId(b);
        return aGlobalId == bGlobalId;
    }


}
