package model.tile;

import model.area.Area;
import model.bits.AreaLayoutBit;
import model.area.AreaRegistry;
import model.enums.AreaType;
import model.enums.Direction;
import model.enums.EdgeType;
import model.enums.LabelEdgePos;

import java.util.Objects;

/// Tile Spec
///
/// Models the concept of a Tile that has not been placed. It specifies the shape of a Tile
/// and the Areas that are on it. To read more about the areas see {@link Area}
public class TileSpec {

    /// the EdgeTypes full information array also includes information about road connection
    private final EdgeType[] edgeTypes;
    /// used for awarding points. Castles with blazons give more points
    private final boolean hasBlazon;
    /// indicates if a tile has a monastery
    private final boolean isMonastery;
    /// all tiles in carcassonne castles are either complete connected(one big castle) or
    /// each edge is its own castle. this flag allows to distinguish between both allowing
    /// to describe all possible tile type in the game
    private final boolean connectCastles;
    /// each tile has it's own tilespec and each tileSpec has it unique tileId so that id
    /// identifies the tileSpec and also the tile bit packed value
    private final int tileId;
    /// can be used to extend the env with rendering power. Can identify a sprite
    /// Note: not used
    private final int graphicsId;
    /// how many rotations of that tile really look different
    private final int differentRotationsCount;
    /// a int in which in the first 8 bit the edge types are encoded with four two bit
    /// numbers(0=grass, 1=Road, 2=Castle,3 unused). Is used for many optimizations
    /// Note: does not encode exact road type(ROAD0,ROAD1...)
    private final int tileTemplate;

    public TileSpec(EdgeType[] edgeTypes, boolean hasBlazon, boolean isMonastery, boolean connectCastles, int tileId,
                    int graphicsId) {
        this.edgeTypes = copyEdgeTypes(edgeTypes);
        this.hasBlazon = hasBlazon;
        this.isMonastery = isMonastery;
        this.connectCastles = connectCastles;
        this.tileId = tileId;
        this.graphicsId = graphicsId;
        this.differentRotationsCount = calculateDifferentRotationsCount();
        this.tileTemplate = calculateTileTemplate();
    }

    private static EdgeType[] copyEdgeTypes(EdgeType[] edgeTypes) {
        if (edgeTypes == null || edgeTypes.length != Direction.getDirectionsCount()) {
            throw new IllegalArgumentException("tile specs need exactly four edge types");
        }
        EdgeType[] edgeTypesCopy = edgeTypes.clone();
        for (int i = 0; i < edgeTypesCopy.length; i++) {
            if (edgeTypesCopy[i] == null) {
                throw new IllegalArgumentException("edge type at direction " + i + " must not be null");
            }
        }
        return edgeTypesCopy;
    }

    /// calculateTileTemplate
    ///
    /// a Tile Templates are the constraints of a tile. So the edges used for quick is position valid
    private int calculateTileTemplate(){
        int template = 0;
        for (int dir = 0; dir < edgeTypes.length; dir++) {
            // each edge type is max 2 bit
            template |= edgeTypes[dir].toSimpleInt() << (dir * 2);
        }
        return template;
    }


    /// calculateDifferentRotationsCount
    ///
    /// calculates how many different rotations the tile has
    private int calculateDifferentRotationsCount() {
        int left = edgeTypes[0].toSimpleInt(), top = edgeTypes[1].toSimpleInt(), right = edgeTypes[2].toSimpleInt(), bottom = edgeTypes[3].toSimpleInt();
        if (left == top && top == right && right == bottom) return 1;
        if (left == right && top == bottom) return 2;
        return 4;
    }

    /// calculateEdgeAreas
    ///
    /// Creates based on the Tile Specification the Areas that it contains it creates the Objects
    /// and returns an Array where the first 12 Elements are the Areas associated with the Edges
    /// and the 13th Element as the Area for the Monastery
    ///
    /// Note: Note that this Array is only used to fill the Area Registry
    public long[] calculateAreas() {
        long[] areas = createInitialAreas();
        mergeRoadAreas(areas);
        mergeCastleAreas(areas);
        mergeGrassAreas(areas);
        return areas;
    }

    /// createInitialAreas
    ///
    /// creates the initial Edge Labels by using only the Edge Description. Each Edge Area is it's
    /// independent Area. So they are not connected yet. If the EdgeType is Castle so are all Areas
    /// in that Edge Direction.
    ///
    /// **Exceptions**: Road Edges only the Middle is of type road and the others are Fields
    ///
    /// The Monastery area (index 12) is only created if the Tile type is of type Monastery otherwise
    /// that index remains {@code 0L}
    private long[] createInitialAreas() {
        long[] areas = new long[13];
        for (int i = 0; i < 12; i++) {
            Direction direction = Direction.fromLabelId(i);
            EdgeType edgeType = edgeTypes[direction.getValue()];
            LabelEdgePos edgePos = LabelEdgePos.fromLocalId(i);
            // logic for mapping the (direction + position on the Edge) is in fromEdgeType
            AreaType areaType = AreaType.fromEdgeType(edgeType, edgePos);
            areas[i] = Area.CreateArea(areaType, i, tileId, hasBlazon);
        }
        if (isMonastery) {
            areas[12] = Area.CreateArea(AreaType.MONASTERY, 12, tileId, hasBlazon);
        }
        return areas;
    }

    /// mergeRoadAreas
    ///
    /// takes the provided areas and merges the all Road Areas that should be connected two Road
    /// areas are considered to should be connected if both EdgeTypes are the same RoadType
    /// (ROAD0,ROAD1,ROAD2,ROAD3)
    private void mergeRoadAreas(long[] areas) {
        //iterate over all direction
        for (int dir = 0; dir < Direction.getDirectionsCount(); dir++) {
            // find center area of the current direction
            Direction direction = Direction.fromValue(dir);
            EdgeType edgeType = edgeTypes[dir];
            long currentArea = areas[LabelEdgePos.Center.toLocalId(direction)];


            if (Area.getAreaTypeTyped(currentArea) == AreaType.ROAD) {
                //iterate over all remaining Directions
                for (int otherDir = dir + 1; otherDir < Direction.getDirectionsCount(); otherDir++) {
                    Direction otherDirection = Direction.fromValue(otherDir);
                    EdgeType otherEdgeType = edgeTypes[otherDir];
                    long otherArea = areas[LabelEdgePos.Center.toLocalId(otherDirection)];

                    //if the Edge of the current Area and the other are have the same RoadId merge the areas
                    if (edgeType == otherEdgeType) {
                        AreaRegistry.localMerge(areas, currentArea, otherArea);
                    }
                }
            }
        }
    }

    /// mergeCastleAreas
    ///
    /// Merges the Castle Areas according to the rules.
    /// - Merge all Castle Areas of one Edge
    /// - Merge All EdgeCastles iff the tile was marked with {@code connectCastles=true} The Assumption
    ///         was that there are either multiple not connected Castles or one big one. This assumption
    ///         is satisfied by the default tile deck
    private void mergeCastleAreas(long[] areas) {
        // Main Castle area in case of connected Castles all merge with that area
        long mainCastleArea = 0;
        for (int i = 0; i < 12; i++) {
            var area = areas[i];
            if (Area.getAreaTypeTyped(area) == AreaType.CASTLE) {
                if (connectCastles) {
                    if (mainCastleArea == 0L) {
                        mainCastleArea = area;
                    }
                    AreaRegistry.localMerge(areas, mainCastleArea, area);
                } else {
                    // all Areas of the current Edge are castle find relative right one and all merge to that
                    long castleRoot = areas[(AreaLayoutBit.getLocalAreaId(area) / 3) * 3];
                    AreaRegistry.localMerge(areas, castleRoot, area);
                }

            }
        }
    }


    /// mergeGrassAreas
    ///
    /// Connects Grass the Areas along the Edges of a Tile so that in the End no Gras Tiles can be merged because
    /// they are blocked
    ///
    /// A Area connection can be blocked if:
    /// - A Road is in between and blocks
    /// - A Castle is in between that stretches completely from one side to its opposite side
    private void mergeGrassAreas(long[] areas) {
        // iterate over all local Areas Clockwise
        for (int i = 0; i < areas.length; i++) {
            long area = areas[i];
            if (area != 0L && Area.getAreaTypeTyped(area) == AreaType.FIELD) {
                //find potential merge partner by locking forward until one was found or blocked
                long mergePartner = 0;
                for (int j = 1; j < 12; j++) {
                    int potentialMergeId = (j + i) % 12;
                    long potentialMerge = areas[potentialMergeId];
                    AreaType potMergeType = Area.getAreaTypeTyped(potentialMerge);

                    if (potMergeType == AreaType.FIELD) {
                        // found merge partner
                        mergePartner = potentialMerge;
                    }

                    boolean blockByCastle =
                            connectCastles && Area.getAreaTypeTyped(areas[(potentialMergeId + 6) % 12]) == AreaType.CASTLE;
                    boolean byRoad = potMergeType == AreaType.ROAD;
                    if (blockByCastle || byRoad) {
                        //blocked stop search mergePartner stays null
                        break;
                    }

                    //continue search because not blocked
                }

                if (mergePartner != 0L) {
                    AreaRegistry.localMerge(areas, area, mergePartner);
                    i = AreaLayoutBit.getLocalAreaId(area);
                }
            }
        }
    }


    /// the EdgeTypes full information array also includes information about road connection
    public EdgeType getEdgeType(Direction direction, int rot) {
        return edgeTypes[(direction.getValue() + rot) & 3];
    }

    /// used for awarding points. Castles with blazons give more points
    public boolean hasBlazon() {
        return hasBlazon;
    }
    /// indicates if a tile has a monastery
    public boolean isMonastery() {
        return isMonastery;
    }
    /// all tiles in carcassonne castles are either complete connected(one big castle) or
    /// each edge is its own castle. this flag allows to distinguish between both allowing
    /// to describe all possible tile type in the game
    public boolean isConnectCastles() {
        return connectCastles;
    }

    /// each tile has it's own tilespec and each tileSpec has it unique tileId so that id
    /// identifies the tileSpec and also the tile bit packed value
    public int getTileId() {
        return tileId;
    }

    public int getGraphicsId() {
        return graphicsId;
    }

    /// deepCopy
    ///
    /// creates an independent copy of the tile spec and its edge type array
    public TileSpec deepCopy() {
        return new TileSpec(edgeTypes, hasBlazon, isMonastery, connectCastles, tileId, graphicsId);
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof TileSpec tileSpec)) return false;
        return tileId == tileSpec.tileId;
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(tileId);
    }

    /// returns how many different unique rotations are there for that tile
    public int getDifferentRotationsCount() {
        return differentRotationsCount;
    }

    /// getTileTemplate
    ///
    /// returns a bitpacked int left,top,tight, botoom of to bit that reprsent
    /// the edgetype
    ///
    /// 0: grass
    /// 1: Road
    /// 2:Castle
    /// 3: unused
    public int getTileTemplate() {
        return tileTemplate;
    }
}
