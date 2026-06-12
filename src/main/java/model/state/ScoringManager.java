package model.state;

import model.Configuration.GameConfiguration;
import model.area.Area;
import model.area.AreaRegistry;
import model.bits.AreaLayoutBit;
import model.bits.MeepleLayoutBit;
import model.collections.SeenFlagMap;
import model.points.Meeple;
import model.points.MeepleRegistry;
import model.points.Player;

import java.util.Arrays;

/// ScoringManager
///
/// provides functionality to collect the playerpoints. Is Threadsafe
public class ScoringManager {
    // the function of the ScoringManager need work arrays. To reduce heap allocations
    // they are statically allocated. To keep the env Threadsafe ThreadLocal is used to get
    // diffrent arrays for diffrent threads so that parallel execution does not mess things up
    private static final ThreadLocal<ScoringManagerThreadLocal> data =
            ThreadLocal.withInitial(ScoringManagerThreadLocal::new);



    // does only have static methods prevent instance of that class
    private ScoringManager(){}

    /// collectPlayerPoints
    /// @param state the state on which it collects the points
    /// @param done if the end-Scoring is performed(as described in the Carcassonne rules)
    ///
    /// collects the points of the player and performs the end point collection if done is true
    /// mutates the state. So Iterates over all placed meeples and changes the points if the filed
    /// is completed. The result is that all meeple are agian free that should be free and that the
    /// player points are increased according to the rules
    ///
    /// - gives points
    /// - free meeples
    /// - collects final points
    public static void collectPlayerPoints(State state, boolean done){
        var areaRegistry = state.areaRegistry;
        var scoreData = data.get();


        if(done){
            // add the points for the farmers into the meeples. Need to calculate that first
            // so that those can be handled like all other meeples.
            collectFieldPoints(areaRegistry);
        }

        var config = state.gameConfig;
        var meeplesRegistry = state.meepleRegistry;

        // the players with the most meeple on a area get the points. So check that we need
        // to calculate for all areas with meeples on it what the max number of meeples is a player
        // has placed on the area.
        // then we can iterate over all meeples anf if not valued yet give the points if the numer
        // of meeples is the max that was previously calculated

        // use seen to find the max meeples for all areas exatly once
        scoreData.seen.ensureSeen(areaRegistry.areas.length);

        int[] meeples = meeplesRegistry.getMeeples();
        // need to calculate for the areas what the max count of meeples is there. To safe time look only
        // at areas where meeples are placed. To further safe calculate the max meeples on a area exatly once
        for(int i = 0; i < meeples.length; i++){
            int meeple = meeples[i];
            if (!Meeple.isFree(meeple)) {
                long area = areaRegistry.findRepresentative(MeepleLayoutBit.getGlobalAreaId(meeple));
                int globalAreaId = Area.getGlobalAreaId(area);
                // complete means if the road is closed on both side or castle closed etc.
                // either the area is completed or the game is over in both cases can collect them
                boolean complete = Area.isCompleted(area) || done;
                if (complete && scoreData.seen.notSeenAndSet( globalAreaId)) {
                    // found a area where we are interested how many meeples are placed on there
                    // clalcualte that value and save it in the area encoding
                    int maxMeeples = findMaxMeeplesAndFree(config, meeplesRegistry, areaRegistry, area);
                    areaRegistry.areas[globalAreaId] = AreaLayoutBit.setMaxMeeples(area, maxMeeples);
                }
            }
        }

        // iterate over all meeples that were marked to collect so the remaining reps
        for(int i = 0; i < meeples.length; i++){
            int meeple = meeples[i];
            if(MeepleLayoutBit.getReadyToCollect(meeple)){
                long area = areaRegistry.findRepresentative(MeepleLayoutBit.getGlobalAreaId(meeple));
                // if the player has the max meeples give the stored points from the area
                if(AreaLayoutBit.getMaxMeeples(area) == MeepleLayoutBit.getCount(meeple)){
                    int points = Area.getStoredPoints(area, done);
                    int playerId = Meeple.getPlayerIdFromGlobal(i, config.getMeeplePerPlayer());
                    state.playerPoints = Player.addPoints(state.playerPoints, playerId, points);
                }
                meeplesRegistry.clearMeepleByGlobalId(MeepleLayoutBit.getGlobalMeepleId(meeple));
            }
        }
    }

    /// findMaxMeeplesAndFree
    ///
    /// sideeffects: if a player has multiple meeples on that area all duplicates are removed and in the
    /// one remaining per player the count is stored for final collection and the rep is marked as collectable
    ///
    /// the side effects make execution significantly faster due to less heap allocation and less instruction in general
    /// @param config the GameConfig
    /// @param meeplesRegistry the registriy of the current State
    /// @param areas the current areas of the state
    /// @param area the area of interest where we want to calculate the number of meeples on it.
    ///
    /// @return how many meeples the player witht the most meeples has placed on the given area
    public static int findMaxMeeplesAndFree(GameConfiguration config, MeepleRegistry meeplesRegistry, AreaRegistry areas, long area){
        int globalMax = 0;
        // iterate over all player
        for(int i = 0; i < config.playersCount(); i++){
            int meepleCount = 0;        // how many meeples has current player
            int meepleRep = -1;         // first meeple found on that area. This one stays all other are removed
            for(int j = 0; j < config.getMeeplePerPlayer();j++){
                // iterate over all meeples of player and find a placed meeple that is placed on the area
                int meeple = meeplesRegistry.getMeepleRaw(i,j);
                if (!Meeple.isFree(meeple)) {
                    int globalAreaId = MeepleLayoutBit.getGlobalAreaId(meeple);
                    long rep = areas.findRepresentative(globalAreaId);
                    if (Area.equals(rep, area)) {
                        // found a meeple that we are interested in
                        meepleCount += 1;
                        if (meepleRep == -1) {
                            // found representative that can stay
                            meepleRep = meeple;
                        } else {
                            meeplesRegistry.clearMeepleByGlobalId(MeepleLayoutBit.getGlobalMeepleId(meeple));
                            // found duplicate can remove that meeple(just a partial step later the rep is also removed
                            // just makes processing easier)
                        }
                    }
                }
            }
            if (meepleCount > globalMax){
                globalMax = meepleCount;
            }
            if(meepleRep != -1) {
                // mark rep as collectable and store the number of meeple in the meeple encoding
                int globalId = MeepleLayoutBit.getGlobalMeepleId(meepleRep);
                var meeples = meeplesRegistry.getMeeples();
                meeples[globalId] = MeepleLayoutBit.rawSetReadyToCollect(
                        MeepleLayoutBit.rawSetCount(meepleRep, meepleCount)
                        , true);
            }
        }
        return globalMax;
    }


    /// collectFieldPoints
    ///
    /// stores the field point in the meeple. Need to count for all fields areas where a meeple is placed on
    /// how many completed castle are connected to it.
    ///
    /// @param areaRegistry the current areas of the state
    ///
    /// first builds a linked list over all completed castles. This actually means all little areas the big
    /// area is made up by.
    /// Note: sideeffect is that the points are stored in the areas
    private static void collectFieldPoints(AreaRegistry areaRegistry) {
        long[] areas = areaRegistry.areas;

        var scoreData = data.get();
        scoreData.ensureFinalCollectCapacity(areas.length);

        // head and next build linkedlists. where Each linkedlist contains all areas of that castle
        // head is -1 for all non reps and a valid globalAreaId for all reps. This id indicates what the
        // first element of the linkedlist for that rep is. After that the next element can always be found
        // in the next array. start says globalAreaId1 is the first then look in next[globalAreaId1] to get
        // the next element and so on.


        // iterate over all areas that are part of a completed castle
        for (int j = 0; j < areas.length; j++) {
            long currentArea = areas[j];

            // area type 1 is castle see enum
            if (AreaLayoutBit.getAreaType(currentArea) == 1 && AreaLayoutBit.getLocalAreaId(currentArea) < 12) {
                long rep = areaRegistry.findRepresentative(j);
                if (Area.isCompleted(rep)) {
                    int repId = Area.getGlobalAreaId(rep);
                    // append this area to the start of the linked list. So old start is now next
                    // and head points to current globalAreaId
                    scoreData.appendToLinkedList(j,repId);
                }
            }
        }

        for (int globalCastleId = 0; globalCastleId < areas.length; globalCastleId++) {
            // iterate over all areaId to search for reps
            int curr = scoreData.head[globalCastleId];
            while (curr != -1) {
                // iterate over all areas of that rep
                long currentArea = areas[curr];
                int currentAreaLocalId = AreaLayoutBit.getLocalAreaId(currentArea);
                int tileId = AreaLayoutBit.getTileId(currentArea);

                // get area left and right of the current castle area
                int right = (currentAreaLocalId == 11 ? 0 : currentAreaLocalId + 1);
                int left = (currentAreaLocalId == 0 ? 11 : currentAreaLocalId - 1);
                int leftNeighbourId = AreaRegistry.getGlobalAreaIdUnchecked(tileId, left);
                int rightNeighbourId = AreaRegistry.getGlobalAreaIdUnchecked(tileId, right);
                long leftRep = areaRegistry.findRepresentative(leftNeighbourId);
                long rightRep = areaRegistry.findRepresentative(rightNeighbourId);
                int leftRepId = Area.getGlobalAreaId(leftRep);
                int rightRepId = Area.getGlobalAreaId(rightRep);

                // check if left/ right area is grass and the points for that area were not yet granted
                // to give points only once, store for what castle id the last points where given. Because we are
                // processing all castles after each other(due to the linked list) we know that after a
                // new castle area is processed the old will never appear gain. So by storing the current
                // castle id after awarding points and checking if the current and the last given are
                // different we know it was granted only once

                //area type 0 is grass see area Type faster that enum access
                if (AreaLayoutBit.getAreaType(leftRep) == 0 && scoreData.lastSeenCastle[leftRepId] != globalCastleId) {
                    scoreData.lastSeenCastle[leftRepId] = globalCastleId;
                    areas[leftRepId] = AreaLayoutBit.addStoredPoints(areas[leftRepId], 3);
                }

                if (AreaLayoutBit.getAreaType(rightRep) == 0 && scoreData.lastSeenCastle[rightRepId] != globalCastleId) {
                    scoreData.lastSeenCastle[rightRepId] = globalCastleId;
                    areas[rightRepId] = AreaLayoutBit.addStoredPoints(areas[rightRepId], 3);
                }
                curr = scoreData.next[curr];
            }
        }

    }


    private static class ScoringManagerThreadLocal {
        // linked list where head stores the starts
        int[] head = new int[0];
        int[] next = new int[0];
        // used to store for what caslte the last time points to a grass field were awarded
        // used to award grass points only once
        int[] lastSeenCastle = new int[0];

        SeenFlagMap seen = new SeenFlagMap();



        /// ensureFinalCollectCapacity
        ///
        /// ensures that the linkedListArrays have the correct size
        ///
        /// fills head and lastSeenCastle with -1
        public void ensureFinalCollectCapacity(int size) {
            // linked list. Head
            head = ensureSize(size, head);   // stores where the lists start
            next = ensureSize(size, next);   // stores where the list continues

            lastSeenCastle = ensureSize(size, lastSeenCastle);
            Arrays.fill(head, -1);
            Arrays.fill(lastSeenCastle, -1);
        }

        private int[] ensureSize(int size, int[] array) {
            return (size != array.length) ? new int[size] : array;
        }

        /// appendToLinkedList
        ///
        /// appends a areaId to the linkedList of the giben representative
        public void appendToLinkedList(int areaId, int repId) {
            next[areaId] = head[repId];
            head[repId] = areaId;
        }

    }

}

