package model.points;

import model.area.Area;
import model.area.AreaRegistry;
import model.bits.AreaLayoutBit;
import model.bits.MeepleLayoutBit;

/// MeepleRegistry
///
/// manages all meeples of all players in one flat array and provides fast helper functions
public class MeepleRegistry {

    private final int[] meeples;
    private final int meeplesCount;

    /// MeepleRegistry
    ///
    /// creates all meeples for the given player count and meeple count per player
    public MeepleRegistry(int playerNumber, int meeplesCount) {
        this.meeplesCount = meeplesCount;
        meeples = new int[meeplesCount * playerNumber];
        for (int playerId = 0; playerId < playerNumber; playerId++) {
            for (int meepleId = 0; meepleId < meeplesCount; meepleId++) {
                meeples[indexOf(playerId, meepleId)] = Meeple.createMeeple(playerId, meepleId, meeplesCount);
            }
        }
    }

    /// findFreeMeeple
    ///
    /// returns one free meeple of the player or 0 if no free meeple exists
    public int findFreeMeeple(int playerId) {
        for (int i = 0; i < meeplesCount; i++) {
            int meeple = getMeepleRaw(playerId, i);
            if (Meeple.isFree(meeple)) {
                return meeple;
            }
        }
        return 0;
    }

    public boolean hasPlayerMeepleOn(int playerId, long areaRep){
        int count = 0;
        for( int i = 0; i < meeplesCount; i++){
            int meeple = getMeepleRaw(playerId,i);
            if(MeepleLayoutBit.getGlobalMeepleId(meeple)== Area.getGlobalAreaId(areaRep)){
                count++;
            }
        }
        return count >= 1;
    }

    /// countFreeMeeples
    ///
    /// counts how many free meeples the player still has
    public int countFreeMeeples(int playerId) {
        int count = 0;
        for (int i = 0; i < meeplesCount; i++) {
            if (Meeple.isFree(getMeepleRaw(playerId, i))) {
                count++;
            }
        }
        return count;
    }

    /// getMeepleRaw
    ///
    /// returns the raw encoded meeple of the player at the local meeple index
    public int getMeepleRaw(int playerId, int meepleId) {
        return meeples[indexOf(playerId, meepleId)];
    }

    /// getMeepleRawByGlobalId
    ///
    /// returns the raw encoded meeple at the global meeple index
    public int getMeepleRawByGlobalId(int globalMeepleId) {
        return meeples[globalMeepleId];
    }

    /// getMeeplesPerPlayerCount
    ///
    /// returns how many meeples each player has in total
    public int getMeeplesPerPlayerCount() {
        return meeplesCount;
    }

    /// findMeeplesOnAreaCount
    ///
    /// counts how many meeples the player has on the given representative area
    public int findMeeplesOnAreaCount(AreaRegistry areas, long areaRep, int playerId) {
        int count = 0;
        for (int i = 0; i < meeplesCount; i++) {
            int meeple = getMeepleRaw(playerId, i);
            if (!Meeple.isFree(meeple)) {
                long meepleAreaRep = areas.findRepresentative(MeepleLayoutBit.getGlobalAreaId(meeple));
                if (Area.getGlobalAreaId(meepleAreaRep) == Area.getGlobalAreaId(areaRep)) {
                    count += 1;
                }
            }
        }
        return count;
    }

    /// findMeeplePlayerIdOnExactArea returns the player id if a meeple is placed exactly on that area id
    ///
    /// @param globalAreaId exact area id where the meeple has to be on
    /// @param playersCount number of players to scan
    /// @return player id or -1 if no meeple is on this exact area
    public int findMeeplePlayerIdOnExactArea(int globalAreaId, int playersCount) {
        for (int playerId = 0; playerId < playersCount; playerId++) {
            for (int meepleId = 0; meepleId < meeplesCount; meepleId++) {
                int meeple = getMeepleRaw(playerId, meepleId);
                if (!Meeple.isFree(meeple) && MeepleLayoutBit.getGlobalAreaId(meeple) == globalAreaId) {
                    return playerId;
                }
            }
        }
        return -1;
    }

    private MeepleRegistry(int[] meeples, int meeplesCount) {
        this.meeples = meeples;
        this.meeplesCount = meeplesCount;
    }

    /// deepCopy
    ///
    /// creates a deep copy of the internal meeple array
    public MeepleRegistry deepCopy() {
        return new MeepleRegistry(meeples.clone(), meeplesCount);
    }

    private int indexOf(int playerId, int meepleId) {
        return playerId * meeplesCount + meepleId;
    }

    /// place
    ///
    /// places the provided raw meeple on the given global area id
    public void place(int meeple, int areaId){
        int meepleGlobalId = MeepleLayoutBit.getGlobalMeepleId(meeple);
        meeples[meepleGlobalId] = MeepleLayoutBit.rawSetGlobalAreaId(meeple, areaId);
    }

    /// clearMeepleByGlobalId
    ///
    /// clears the meeple and makes it free again
    public void clearMeepleByGlobalId(int globalMeepleId) {
        meeples[globalMeepleId] = Meeple.clear(meeples[globalMeepleId]);
    }

    /// getMeeples
    ///
    /// returns the internal raw meeple array
    public int[] getMeeples() {
        return meeples;
    }
}
