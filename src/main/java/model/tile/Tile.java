package model.tile;

import model.area.Area;
import model.bits.*;
import model.area.AreaRegistry;
import model.collections.FrontierMap;
import model.collections.PositionTileMap;
import model.collections.EdgeConstraintHelper;
import model.exceptions.InvalidTilePlacement;

/// Tile
///
/// A tile is a bitpacked long that contains all information for that tile. This class provides functionality
/// to work with that bitpacked long. Mainly it provides a placeTile method that integrates a tile into the overall
/// placed tilemap according to the carcassonne rules. Also provides fast checking to check if a position, rotation pair
///  is valid to. This class does not concern itself with meeples just the tiles itsel
public class Tile {

    private Tile() {}

    /// place Tile
    ///
    /// Takes some state context and the tile to be placed and integrates it with the tilemap.
    /// Merges the local Areas into the global map. Updates frontier
    ///
    /// @param tiles the Tile Map to store the tiles that are placed
    /// @param areas the global area manages collection
    /// @param frontier collection for storing next possible actions
    /// @param tileSpec the specification of the tile that is being placed contains information of the bitpacked tile
    /// @param positionBits the encoded position where tile is being placed encoded using model.bits.PositionLayout
    /// @param rot counterclockwise rotation [0,3] how it the tile rotated
    ///
    /// @return the tile encoding
    ///
    /// Note: does not check if the tile is in the frontier. Only checks if existing constraints are satisfied
    public static long placeTile(PositionTileMap tiles, AreaRegistry areas, FrontierMap frontier, TileSpec tileSpec, int positionBits, int rot) {
        if(!areConstraintsSatisfied(frontier.get(positionBits),tileSpec.getTileTemplate(),rot)){
            throw new InvalidTilePlacement(PositionLayoutBit.getX(positionBits),PositionLayoutBit.getY(positionBits),rot,tileSpec.getTileId());
        }

        // the template from the tile spec contains all edges bitpacked unrotated. Need to rotate that
        // by shifting and then bitpack the results to geht the encoding. Use raw pack all values are validated and trusted
        int edges = rotateEdgesCounterClockwise(tileSpec.getTileTemplate(), rot);
        int x =  PositionLayoutBit.getX(positionBits);
        int y =  PositionLayoutBit.getY(positionBits);
        long tile = TileLayoutBit.rawPack(edges, tileSpec.getTileId(), (byte)y, (byte)x, rot, tileSpec.isMonastery());

        // add the tile encoding to the collection that manages the tiles
        tiles.put(positionBits, tile);
        // need to merge the areas of the new tile to the adjacent areas of the neighbor tiles
        mergeAreasToMap(tile, tiles, areas, frontier);

        // remove current positon from the frontier. Position is no used
        frontier.remove(positionBits);
        return tile;
    }

    /// mergeAreasToMap
    ///
    /// Responsible for updating the areas to include the newly created Tile. Updates the completeness status
    /// of the edgeAreas and the Monastery  area. The EdgeAreas are handled in directly through the merge and the
    /// monastery directly
    ///
    /// Note: Side effects are that the completeness status is also updated and that the frontier constraints are
    /// added in that method for performance reasons
    private static void mergeAreasToMap(long tile, PositionTileMap tiles, AreaRegistry areas, FrontierMap frontier) {

        final int x = TileLayoutBit.getX(tile);
        final int y = TileLayoutBit.getY(tile);
        int tileId = TileLayoutBit.getTileId(tile);

        // iterate over all areas and connect them to the area of the neighbor tile that is in front of it
        for (int dir = 0; dir < 4; dir++) {
            // quick branchless neighbors calculation
            int neighborPos = PositionLayoutBit.rawPack(x + ((dir - 1) & ((dir & 1) - 1)), y +  ((2 - dir) * (dir & 1)));
            long neighborTile = tiles.get(neighborPos);
            if (neighborTile != 0L) {
                // has a neighbor at that position
                for (int i = 0; i < 3; i++) {
                    // iterate left, center, right and find area on the neighbor and merge it
                    int localId = dir * 3 + i;                  // area that is being currently placed
                    int otherLocalId = (dir * 3 + 8 - i) % 12;  // area on the neighbor
                    int globalId = Area.getGlobalAreaId(getEdgeArea(areas, tile, localId));
                    int otherGlobalId = Area.getGlobalAreaId(getEdgeArea(areas, neighborTile, otherLocalId));
                    areas.merge(globalId, otherGlobalId);
                }
            } else {
                // has no neighbor on that front. This means that the spot is free and can be used for next placement
                // to allow for fast placement and fast valid position checking the current edge is marked in the frontier
                // at the neighbor position as a constraint that the next tile has to fulfill
                int edgeType = getEdgeByDir(tile, dir);
                frontier.addConstraint(neighborPos, edgeType, (dir + 2) & 3);
            }
        }

        // update Monastery data. If tile is a monastery need to calculate how many are already in the 8-neighborhood
        // and even if the current tile is no monastery need to inform all tiles in the 8-neighborhood that a new tile
        // was placed. So that the completeness mechanisms works for the monasteries.
        for (int dx = -1; dx <= 1; dx++) {
            for (int dy = -1; dy <= 1; dy++) {
                if (dx != 0 || dy != 0) {
                    int otherPos = PositionLayoutBit.rawPack(x + dx, y + dy);
                    long other = tiles.get(otherPos);
                    if (other != 0L) {
                        // got position in the 8-neighborhood where a tile was placed
                        if (TileLayoutBit.getMonastery(other)) {
                            // other tile is monastery inform of own placement
                            long otherMonasteryArea = areas.getMonasteryArea(TileLayoutBit.getTileId(other));
                            Area.updateCompleteness(areas.areas, otherMonasteryArea);
                        }
                        if (TileLayoutBit.getMonastery(tile)) {
                            // current placed tile is a monastery update own completeness status
                            long ownMonasteryArea = areas.getMonasteryArea(tileId);
                            Area.updateCompleteness(areas.areas, ownMonasteryArea);
                        }
                    }
                }
            }
        }
    }

    /// isPlacementValid
    ///
    /// checks if a tile can be placed at the provided position and rotation given the current frontier
    ///
    /// @param frontier current frontier with the frontier constraints
    /// @param positionBit the encoded x,y positions encoded with PositionLayoutBit
    /// @param rot the rotation of the tile in [0,3] counter-clockwise
    /// @param tileEdges the encoded edges of the tile that is about to being placed or that should be checked if it
    /// could be placed
    public static boolean isPlacementValid(FrontierMap frontier, int positionBit, int tileEdges, int rot) {
        int constraints = frontier.get(positionBit);
        return isPlacementValid(constraints, tileEdges, rot);
    }

    /// isPlacementValid
    ///
    /// checks if tile edges rotated do not violating any edge constraints and check if it is in the frontier.
    /// So it checks if the tile with that edges could be placed(not considering meeple aspect of placement) if a
    /// frontier positions would have those constraints
    ///
    /// @param constraints bit packed constraints for the placement of a tile
    /// @param tileEdges the bitpacked edges of an area
    /// @param rot the rotation in which the tile is being placed
    /// @return true if the position is in the frontier and the constraints are satisfied according to the carcassonne
    /// rules else false
    public static boolean isPlacementValid(int constraints, int tileEdges, int rot) {
        if (constraints == 0) {
            // not in frontier invalid position
            return false;
        }
        // valid if constraints are satisfied
        return areConstraintsSatisfied(constraints,tileEdges,rot);
    }

    public static boolean areConstraintsSatisfied(int constraints, int tileEdges, int rot) {
        int rotatedTileEdges = rotateEdgesCounterClockwise(tileEdges, rot);
        int encodedTileEdges=  EdgeConstraintHelper.encodeTileEdges(rotatedTileEdges);
        return EdgeConstraintHelper.matchesRequiredNonZero(constraints, encodedTileEdges);
    }


    /// getEdgeArea
    ///
    /// finds the area encoding of a area given the local area id and the tile it belongs to
    /// handels the rotation already correctly. So the localAreaId is the way it would be rendered
    ///
    /// @param areas the Area Registry containg all Area information
    /// @param tile the bitpacekd tile information
    /// @param localId the area of interest
    /// @return the area encoding of the requested localArea
    public static long getEdgeArea(AreaRegistry areas, long tile, int localId) {
        int realLocalId = (localId + 3 * TileLayoutBit.getRotation(tile)) % 12;
        return areas.getEdgeArea(TileLayoutBit.getTileId(tile), realLocalId);
    }


    /// rotateEdgesCounterClockwise
    ///
    /// takes the embedded edges and rotates them rotation times counterClockwise
    /// @param rotation how often rotated in [0,3]
    /// @param edges8 the bitpacked edges(template)
    private static int rotateEdgesCounterClockwise(int edges8, int rotation) {
        // each edge has in the encoding two bits so need to move all rotation * 2 to the left
        int shift = rotation << 1;
        // (edges8 >>> shift)  moves the  encoding to the correct spot
        // the bits that are shifted out of the 8 bit scope need to be brought at the correct spot
        // (edges8 << (8 - shift)) shifts the overflow to the spot back in scope in a loop
        // then need to or them to combine them and apply the mask 0xFF so that the lowest byte remains
        return ((edges8 >>> shift) | (edges8 << (8 - shift))) & 0xFF;
    }

    /// getEdgeByDir
    /// returns from the given bitpacked tile a edge in the direciton of dir
    ///
    /// @param tile bitpacked tile
    /// @param dir direction 0,1,2,3 left,top,right, bottom already rotated
    /// @return the edge encoding
    public static int getEdgeByDir(long tile, int dir) {
        int shift = (dir & 3) << 1;
        return (TileLayoutBit.getEdges(tile) >>> shift) & 3;
    }

}
