package model.area;

import model.bits.AreaLayoutBit;
import model.tile.TileSpec;

/// Area Registry
///
/// This is a glorified collection. It manages an long[] where all areas are stored
/// it provides functionality for merging the areas on a game global scale in a balanced way,
/// to find the representatves and other minor things.
///
/// Provides extreme fast deepCopy
public class AreaRegistry {
    // each edge has 3 areas (left, center, right) + one monastery area  3*4+1 = 13
    private static final int areaByTileCount = 13;

    // the tiles in the game for checked accesses
    private final int tilesCount;
    // the actual area array will be of size  areaByTileCount*tilesCount
    public final long[] areas;


    /// AreaRegistry
    ///
    /// public Constructor called from state. Takes Tile specs and the start-Tile
    /// and build the areas for all of these. Each area is in the array even before
    /// it is placed
    ///
    /// Note: Assumes that the largest tileSpecs id < tileSpecs.length+1 and startTile.id < tileSpecs.length+1
    public AreaRegistry(TileSpec[] tileSpecs, TileSpec startTile) {
        // allocate the areas array for the tiles that can be placed and the initial Tile
        tilesCount = tileSpecs.length + 1;
        areas = new long[tilesCount * areaByTileCount];

        // iterate over all tiles, calculate the areas in a local manner and copy into the large global area array
        for (TileSpec tileSpec : tileSpecs) {
            int tileId = tileSpec.getTileId();
            checkTileIdIsValid(tileId);
            long[] tileAreas = tileSpec.calculateAreas();
            System.arraycopy(tileAreas, 0, this.areas, tileId * areaByTileCount, areaByTileCount);
        }

        // calculate local areas for the initial tile and copy into the areas array
        int startTileId = startTile.getTileId();
        checkTileIdIsValid(startTileId);
        long[] startAreas = startTile.calculateAreas();
        System.arraycopy(startAreas, 0, this.areas, startTileId * areaByTileCount, areaByTileCount);
    }


    /// findRepresentative
    ///
    /// given an area id returns the area that is its representative
    public long findRepresentative(long areaBits) {
        int id = Area.getGlobalAreaId(areaBits);
        long current = areas[id];
        int parentId = AreaLayoutBit.getParentGlobalAreaId(current);
        // if it's own parent can stop with just one access to mem
        if (parentId == id) return current;
        return findRepresentative(parentId);
    }

    /// findRepresentative
    ///
    /// given a global id returns the area representative of that area
    /// on the way it performs path halving so that many accesses make if very fast
    /// with short chains
    ///
    /// Is unchecked and works on the global array
    public long findRepresentative(int globalId) {
        int id = globalId;
        // do not recursively, should be a bit faster
        while (true) {
            long v = areas[id];
            // check if the current element it's own parent and end if so found parent
            int p = AreaLayoutBit.getParentGlobalAreaId(v);
            if (p == id) return v;
            //check if parent is already representative by getting grandparent if not set curent parent to gradnparent
            int gp = AreaLayoutBit.getParentGlobalAreaId(areas[p]);
            if (gp != p) areas[id] = AreaLayoutBit.setParentGlobalAreaId(v, gp);
            id = p;
        }
    }

    /// merge
    ///
    /// merges two global areas
    /// can be called multiple times and works as expected
    ///
    /// Note:  assumes that the areas are not of type Monastery
    public void merge(int areaGlobalIdA, int areaGlobalIdB) {
        long a = findRepresentative(areaGlobalIdA);
        long b = findRepresentative(areaGlobalIdB);

        // does the balancing so that no branch gets to long
        if (AreaLayoutBit.getSize(a) < AreaLayoutBit.getSize(b)) {
            long tmp = a;
            a = b;
            b = tmp;
        }

        // if the merge is actually new the points are added if both areas and stored in the parent
        if (!Area.equals(a, b)) {
            Area.merge(areas, Area.getGlobalAreaId(a), Area.getGlobalAreaId(b), true);
        }
        Area.updateCompleteness(areas, a);
    }

    /// findLocalRepresentative
    ///
    /// @param localAreas local Area Registry indexed by loca area id should be length 13
    /// @param area you want to loop to find representative
    ///
    /// is a generalized version of findRepresentative that uses only the localIds
    /// a Array is given and the indexes of the areas are found using hte localids of the areas
    public static long findLocalRepresentative(long[] localAreas, long area) {
        // get informations
        int localId = AreaLayoutBit.getLocalAreaId(area);
        long current = localAreas[localId];
        int areaParentId = AreaLayoutBit.getParentGlobalAreaId(current);
        int areaId = Area.getGlobalAreaId(current);
        int tileId = AreaLayoutBit.getTileId(current);
        // check if parent can stop
        if (areaId == areaParentId) {
            return current;
        }
        int localParentId = getLocalAreaIdUnchecked(areaParentId, tileId);
        long parent = localAreas[localParentId];
        // to recursive call to find rep of parent
        long rep = findLocalRepresentative(localAreas, parent);
        // set parent to the representative
        localAreas[localId] = AreaLayoutBit.setParentGlobalAreaId(current, Area.getGlobalAreaId(rep));
        return rep;
    }

    /// localMerge
    public static void localMerge(long[] localAreas, long localArea1, long localArea2) {
        long area1Rep = findLocalRepresentative(localAreas, localArea1);
        long area2Rep = findLocalRepresentative(localAreas, localArea2);

        if (Area.equals(area1Rep,area2Rep)) {
            return ;
        }
        // balances in a simple way
        int rep1ASize = AreaLayoutBit.getSize(area1Rep);
        int rep2ASize = AreaLayoutBit.getSize(area2Rep);
        if (rep1ASize < rep2ASize) {
            long temp = area1Rep;
            area1Rep = area2Rep;
            area2Rep = temp;
        }

        Area.merge(localAreas, AreaLayoutBit.getLocalAreaId(area1Rep), AreaLayoutBit.getLocalAreaId(area2Rep), false);
    }

    /// deepCopy
    ///
    /// performs a deep copy of the internal areas array is very fast
    public AreaRegistry deepCopy() {
        return new AreaRegistry(tilesCount, areas.clone());
    }

    /// Private Constructor only used for deepcopy
    private AreaRegistry(int tilesCount, long[] areas) {
        this.tilesCount = tilesCount;
        this.areas = areas;
    }

    /// getArea
    ///
    /// return an area of the internal array changes to that returned long changes nothing in the
    /// internal area structure needs to be done explicitly
    ///
    /// works also for monastery and generally all
    public long getArea(int tileId, int areaId) {
        return areas[getGlobalAreaId(tileId, areaId)];
    }

    /// getGlobalAreaId
    ///
    /// Returns the global area id based on the tileID the local areaId
    /// Note: does validity checks so not that fast
    public int getGlobalAreaId(int tileId, int areaId) {
        checkTileIdIsValid(tileId);
        checkAreaIdIsValid(areaId);
        return areaId + (areaByTileCount * tileId);
    }

    /// getEdgeArea
    ///
    /// helper function that allows the access on edgeAreas but not to the monastery
    /// just helper for getArea
    /// Note: assumes areaId < 12
    public long getEdgeArea(int tileId, int areaId) {
        if (areaId >= 12) {
            throw new IllegalArgumentException();
        }
        return getArea(tileId, areaId);
    }

    /// getMonasteryArea
    ///
    /// returns the monastery area of that tile. If it is not a monastery 0 is retuned
    public long getMonasteryArea(int tileId) {
        return getArea(tileId, 12);
    }

    /// getEdgeArea
    ///
    /// helper function that returns the edge areas
    public long getEdgeArea(int tileId, int areaLocalId, int rot) {
        int realLocalId = (areaLocalId + 3 * rot) % 12;
        return getEdgeArea(tileId, realLocalId);
    }

    /// getAreaRot
    ///
    /// returns an area but rotated. Rotated means the localid looks exactly like in
    /// the real placed game world
    public long getAreaRot(int tileId, int localId, int rot){
        if (localId == 12) {
            return  getMonasteryArea(tileId);
        } else {
            return getEdgeArea(tileId,localId,rot);
        }
    }

    /// Helper function to check if two areas are the same
    ///
    /// Note: does not assume that area1 and area2 are represenatives so works for all
    public boolean isSameArea(long area1, long area2) {
        return Area.equals(findRepresentative(area1), findRepresentative(area2));
    }

    /// helper function for validity of tileId
    private void checkTileIdIsValid(int tileId) {
        if (tileId < 0 || tileId >= tilesCount) {
            throw new IllegalArgumentException();
        }
    }

    /// helper function for validity of areaId
    private void checkAreaIdIsValid(int areaId) {
        if (areaId < 0 || areaId >= areaByTileCount) {
            throw new IllegalArgumentException();
        }
    }

    /// helper function for getGlobalAreaIdUnchecked
    /// Note: Unchecked and therefore fast
    public static int getGlobalAreaIdUnchecked(int tileId, int areaId) {
        return tileId * areaByTileCount + areaId;
    }

    /// helper function for getLocalAreaIdUnchecked
    /// Note: Unchecked and therefore fast
    public static int getLocalAreaIdUnchecked(int globalId, int tileId) {
        return globalId - (areaByTileCount * tileId);
    }
}
