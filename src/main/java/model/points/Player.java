package model.points;

import model.area.Area;
import model.bits.AreaLayoutBit;
import model.area.AreaRegistry;
import model.exceptions.InvalidMeeplePositionException;
import model.exceptions.MeepleAlreadyOnTileExcpetion;
import model.exceptions.NoMeepleLeftException;
import model.state.State;

/// Player
///
/// helper functions for packed player state
/// used to manage player points and player meeple placement
public final class Player {
    private Player(){

    }

    /// checkMeeplePlacement
    ///
    /// checks if  a meeple placement is valid. A placement is valid, if the area position
    /// has currently no meeple on it and if the player placing the meeple has still free meeples
    /// to place
    ///
    /// @param areas the AreaRegistry you get that from the state
    /// @param meeples the MeepleRegistry you get that from the state
    /// @param playerId id of the player placing the meeple, zero based
    /// @param area the area where the meeple is being placed. this means
    /// the actual long encoding of the area. Does not need to be the Representative
    ///
    /// Note: assumes area is a valid bit packed area
    ///
    /// @throws MeepleAlreadyOnTileExcpetion
    /// @throws NoMeepleLeftException
    public static void checkMeeplePlacement(AreaRegistry areas, MeepleRegistry meeples, int playerId,  long area){
        long areaRep = areas.findRepresentative(area);
        if (Area.hasMeeple(areaRep)) {
            throw new MeepleAlreadyOnTileExcpetion(false);
        }
        int meeple = meeples.findFreeMeeple(playerId);
        if (meeple == 0) {
            throw new NoMeepleLeftException(playerId);
        }
    }

    /// placeMeeple
    ///
    /// places one free meeple of the player on the given area. Checks also
    /// if the meeple placement is valid.
    ///
    /// @param areas the AreaRegistry you get that from the state
    /// @param meeples the MeepleRegistry you get that from the state
    /// @param playerId id of the player placing the meeple, zero based
    /// @param area the area where the meeple is being placed. this means
    /// the actual long encoding of the area. Does not need to be the Representative
    ///
    ///  @throws MeepleAlreadyOnTileExcpetion
    ///  @throws NoMeepleLeftException
    public static void placeMeeple(AreaRegistry areas, MeepleRegistry meeples, int playerId,  long area) {
        long areaRep = areas.findRepresentative(area);
        checkMeeplePlacement(areas,meeples,playerId,area);
        int meeple = meeples.findFreeMeeple(playerId);

        // place meeple and update representative state for later quick placement
        meeples.place(meeple, Area.getGlobalAreaId(area));
        int meeplesCount = meeples.findMeeplesOnAreaCount(areas, areaRep, playerId);
        if (meeplesCount > AreaLayoutBit.getMaxMeeples(areaRep)) {
            int repId = Area.getGlobalAreaId(areaRep);
            areas.areas[repId] = AreaLayoutBit.setMaxMeeples(areaRep, meeplesCount);
        }
    }

    /// getPoints
    ///
    /// @param playerPoints the bit packed player points information
    /// @param playerId the id(zero based) of the player you want to get the points of
    /// returns the points of a player given the playerPoints bitpacked int
    /// uses bit operations to get the points of one specific player
    public static int getPoints(long playerPoints, int playerId) {
        return (int) ((playerPoints >>> (playerId * 12)) & 0xFFFL);
    }

    /// addPoints
    ///
    /// @param playerPoints the bit packed player points information
    /// @param playerId the id(zero based) of the player you want to add the points of
    /// @param value the points you want to add
    /// returns the new bitpacked points value
    ///
    /// Note: does not change old bit packed points long but creates a new one
    public static long addPoints(long playerPoints, int playerId, int value) {
        int shift = playerId * 12;
        long mask = 0xFFFL << shift;
        long v = (playerPoints >>> shift) & 0xFFFL;
        v = (v + value) & 0xFFFL;
        return (playerPoints & ~mask) | (v << shift);
    }
}
