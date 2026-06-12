package model.tile;

import model.area.Area;
import model.area.AreaRegistry;
import model.bits.EdgeConstraintLayoutBit;
import model.bits.PositionLayoutBit;
import model.bits.TileLayoutBit;
import model.collections.FrontierMap;
import model.collections.PositionTileMap;
import model.enums.EdgeType;
import model.exceptions.InvalidTilePlacement;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TileUnitTest {

    @Test
    void placeTileStoresTileRemovesCurrentFrontierAndAddsNeighbors() {
        TileSpec tileSpec = fieldTile(0);
        AreaRegistry areas = new AreaRegistry(new TileSpec[]{tileSpec}, fieldTile(1));
        PositionTileMap tiles = new PositionTileMap(8);
        FrontierMap frontier = new FrontierMap(8);
        int position = PositionLayoutBit.pack(0, 0);
        frontier.addConstraint(position, 0, 0);
        frontier.addConstraint(position, 0, 1);
        frontier.addConstraint(position, 0, 2);
        frontier.addConstraint(position, 0, 3);

        long placed = Tile.placeTile(tiles, areas, frontier, tileSpec, position, 0);

        assertEquals(placed, tiles.get(position));
        assertEquals(0, frontier.get(position));
        assertEquals(4, frontier.size());
        assertTrue(frontier.get(PositionLayoutBit.pack(-1, 0)) != 0);
        assertTrue(frontier.get(PositionLayoutBit.pack(0, 1)) != 0);
        assertTrue(frontier.get(PositionLayoutBit.pack(1, 0)) != 0);
        assertTrue(frontier.get(PositionLayoutBit.pack(0, -1)) != 0);
        assertEquals(0, TileLayoutBit.getEdges(placed));
        assertEquals(0, TileLayoutBit.getTileId(placed));
        assertEquals(0, TileLayoutBit.getRotation(placed));
        assertEquals(0, TileLayoutBit.getX(placed));
        assertEquals(0, TileLayoutBit.getY(placed));
    }

    @Test
    void placeTileRejectsUnsatisfiedConstraints() {
        TileSpec tileSpec = fieldTile(0);
        AreaRegistry areas = new AreaRegistry(new TileSpec[]{tileSpec}, fieldTile(1));
        PositionTileMap tiles = new PositionTileMap(4);
        FrontierMap frontier = new FrontierMap(4);
        int position = PositionLayoutBit.pack(0, 0);
        frontier.addConstraint(position, 2, 0);

        assertThrows(InvalidTilePlacement.class, () -> Tile.placeTile(tiles, areas, frontier, tileSpec, position, 0));
    }

    @Test
    void isPlacementValidWithFrontierRequiresPositionToExistAndMatch() {
        FrontierMap frontier = new FrontierMap(2);
        int position = PositionLayoutBit.pack(2, 3);
        int tileEdges = mixedTile(0).getTileTemplate();
        frontier.addConstraint(position, 1, 0);
        frontier.addConstraint(position, 2, 1);
        frontier.addConstraint(position, 0, 2);
        frontier.addConstraint(position, 0, 3);

        assertTrue(Tile.isPlacementValid(frontier, position, tileEdges, 1));
        assertEquals(false, Tile.isPlacementValid(frontier, PositionLayoutBit.pack(9, 9), tileEdges, 1));
    }

    @Test
    void isPlacementValidWithRawConstraintsReturnsFalseForZero() {
        assertEquals(false, Tile.isPlacementValid(0, fieldTile(0).getTileTemplate(), 0));
    }

    @Test
    void areConstraintsSatisfiedChecksRotatedTileEdges() {
        int tileEdges = mixedTile(0).getTileTemplate();
        int matchingConstraints = EdgeConstraintLayoutBit.rawPack(2, 3, 1, 1);
        int mismatchingConstraints = EdgeConstraintLayoutBit.rawPack(3, 3, 1, 1);

        assertTrue(Tile.areConstraintsSatisfied(matchingConstraints, tileEdges, 1));
        assertEquals(false, Tile.areConstraintsSatisfied(mismatchingConstraints, tileEdges, 1));
    }

    @Test
    void getEdgeAreaRespectsTileRotation() {
        TileSpec tileSpec = mixedTile(0);
        AreaRegistry areas = new AreaRegistry(new TileSpec[]{tileSpec}, fieldTile(1));
        long rotatedTile = TileLayoutBit.rawPack(tileSpec.getTileTemplate(), 0, (byte) 0, (byte) 0, 1, false);

        long edgeArea = Tile.getEdgeArea(areas, rotatedTile, 0);

        assertEquals(Area.getGlobalAreaId(areas.getEdgeArea(0, 3)), Area.getGlobalAreaId(edgeArea));
    }

    @Test
    void getEdgeByDirReturnsRotatedEdgeEncodingFromTileBits() {
        long tile = TileLayoutBit.rawPack((0) | (1 << 2) | (2 << 4), 0, (byte) 0, (byte) 0, 0, false);

        assertEquals(0, Tile.getEdgeByDir(tile, 0));
        assertEquals(1, Tile.getEdgeByDir(tile, 1));
        assertEquals(2, Tile.getEdgeByDir(tile, 2));
        assertEquals(0, Tile.getEdgeByDir(tile, 3));
    }

    private static TileSpec fieldTile(int tileId) {
        return new TileSpec(new EdgeType[]{EdgeType.FIELD, EdgeType.FIELD, EdgeType.FIELD, EdgeType.FIELD}, false, false, false, tileId, tileId);
    }

    private static TileSpec mixedTile(int tileId) {
        return new TileSpec(new EdgeType[]{EdgeType.FIELD, EdgeType.CASTLE, EdgeType.ROAD0, EdgeType.FIELD}, false, false, false, tileId, tileId);
    }
}
