package model.tile;

import model.Configuration.GameConfigBuilder;
import model.Configuration.GameConfiguration;
import model.collections.FrontierMap;
import model.bits.PositionLayoutBit;
import model.exceptions.TileDeckException;
import org.junit.jupiter.api.Test;

import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TileDeckUnitTest {

    @Test
    void currentTileCanBeReadAndWritten() {
        TileDeck deck = new TileDeck(createConfig(false));
        TileSpec tile = deck.popGetTile(zeroRandom());

        deck.setCurrentTile(tile);

        assertEquals(tile, deck.getCurrentTile());
    }

    @Test
    void popTileAndPopGetTileAdvanceDeckAndExhaustionReturnsNullOrThrows() {
        TileDeck deck = new TileDeck(createConfig(false));

        int tileId = deck.popTile(zeroRandom());
        assertEquals(0, tileId);
        assertEquals(1, deck.getDeckPos());
        assertEquals(1, deck.popGetTile(zeroRandom()).getTileId());
        assertEquals(2, deck.popGetTile(zeroRandom()).getTileId());
        assertNull(deck.popGetTile(zeroRandom()));
        assertThrows(IllegalStateException.class, () -> deck.popTile(zeroRandom()));
    }

    @Test
    void popForceGetTileAndPopTileForceReturnRequestedTileOrThrow() {
        TileDeck deck = new TileDeck(createConfig(false));

        assertEquals(2, deck.popTileForce(2));
        assertEquals(1, deck.getDeckPos());
        assertEquals(1, deck.popForceGetTile(1).getTileId());
        assertThrows(TileDeckException.class, () -> deck.popTileForce(2));
    }

    @Test
    void drawTileIdSoftDoesNotConsumeAndReturnsMinusOneWhenExhausted() {
        TileDeck deck = new TileDeck(createConfig(false));

        assertEquals(0, deck.drawTileIdSoft(zeroRandom()));
        assertEquals(0, deck.getDeckPos());
        deck.popTile(zeroRandom());
        deck.popTile(zeroRandom());
        deck.popTile(zeroRandom());
        assertEquals(-1, deck.drawTileIdSoft(zeroRandom()));
    }

    @Test
    void determinizeFixesFutureDrawsIndependentOfRandomInput() {
        TileDeck deck = new TileDeck(createConfig(false));

        deck.determinize(new Random(7));

        assertEquals(deck.drawTileIdSoft(new Random(1)), deck.drawTileIdSoft(new Random(999)));
    }

    @Test
    void findNextTileConsumesUnplayableTilesUntilPlayableOne() {
        TileDeck deck = new TileDeck(createConfig(false));
        FrontierMap frontier = new FrontierMap(2);
        frontier.addConstraint(PositionLayoutBit.pack(0, 0), 1, 0);

        TileSpec tile = deck.findNextTile(zeroRandom(), frontier);

        assertEquals(1, tile.getTileId());
        assertEquals(2, deck.getDeckPos());
    }

    @Test
    void findNextTileSoftFindsPlayableTileWithoutConsumingDeck() {
        TileDeck deck = new TileDeck(createConfig(false));
        FrontierMap frontier = new FrontierMap(2);
        frontier.addConstraint(PositionLayoutBit.pack(0, 0), 1, 0);

        TileSpec tile = deck.findNextTileSoft(zeroRandom(), frontier);

        assertEquals(1, tile.getTileId());
        assertEquals(0, deck.getDeckPos());
    }

    @Test
    void findNextTileAndSoftReturnNullWhenNoTileFits() {
        TileDeck deck = new TileDeck(createConfig(false));
        FrontierMap frontier = new FrontierMap(2);
        frontier.addConstraint(PositionLayoutBit.pack(0, 0), 1, 0);
        frontier.addConstraint(PositionLayoutBit.pack(0, 0), 2, 1);

        assertNull(deck.findNextTileSoft(zeroRandom(), frontier));
        assertNull(deck.findNextTile(zeroRandom(), frontier));
        assertEquals(3, deck.getDeckPos());
    }

    @Test
    void deepCopyCreatesIndependentDeckPosition() {
        TileDeck deck = new TileDeck(createConfig(false));
        deck.setCurrentTile(deck.popGetTile(zeroRandom()));

        TileDeck copy = deck.deepCopy();
        copy.popGetTile(zeroRandom());
        copy.unPop();

        assertNotSame(deck, copy);
        assertEquals(1, deck.getDeckPos());
        assertEquals(1, copy.getDeckPos());
        assertEquals(deck.getCurrentTile(), copy.getCurrentTile());
    }

    @Test
    void unPopMovesDeckPositionBackOne() {
        TileDeck deck = new TileDeck(createConfig(false));
        deck.popTile(zeroRandom());

        deck.unPop();

        assertEquals(0, deck.getDeckPos());
    }

    @Test
    void hasAnyPlacementChecksFrontierAndTileRotations() {
        TileDeck deck = new TileDeck(createConfig(false));
        FrontierMap frontier = new FrontierMap(2);
        frontier.addConstraint(PositionLayoutBit.pack(0, 0), 1, 0);

        assertTrue(deck.hasAnyPlacement(frontier, 1));
        assertEquals(false, deck.hasAnyPlacement(frontier, 0));
    }

    @Test
    void getTileCountAndGetAtExposeDeckOrder() {
        TileDeck deck = new TileDeck(createConfig(false));

        assertEquals(3, deck.getTileCount());
        assertEquals(0, deck.getAt(0));
        assertEquals(1, deck.getAt(1));
        assertEquals(2, deck.getAt(2));
        assertDoesNotThrow(() -> deck.getAt(1));
    }

    private static GameConfiguration createConfig(boolean determinized) {
        GameConfigBuilder builder = new GameConfigBuilder()
                .setPlayerCount(2)
                .setRandomSeed(3)
                .setDeterminized(determinized);

        builder.addTile().endTile();
        builder.addTile().left().markAsCastle().endTile();
        builder.addTile().top().markAsRoadAndAsEnd().endTile();
        builder.setStartField().endTile();
        return builder.build(false);
    }

    private static Random zeroRandom() {
        return new Random() {
            @Override
            public int nextInt(int bound) {
                return 0;
            }
        };
    }
}
