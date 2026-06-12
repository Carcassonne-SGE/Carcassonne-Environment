package model.state;

import model.area.Area;
import model.bits.AreaLayoutBit;
import model.area.AreaRegistry;
import model.collections.ActionManagerMap;
import model.collections.ActionSet;
import model.collections.FrontierMap;
import model.bits.PositionLayoutBit;
import model.tile.Tile;
import model.bits.CarcassonneActionLayoutBit;
import sge.CarcassonneAction;

import java.util.Random;
import java.util.Set;


/// PossibleActionManager
///
/// this class is responsible for providing function to fastly calculate a set that
/// contains all actions that can be performed in a given state according to the
/// Carcassonne rules. Also provides functions to get a random function very fast
public class PossibleActionManager {
    private static final ThreadLocal<PossibleActionData> possibleActionsData =
            ThreadLocal.withInitial(PossibleActionData::new);

    // prevent instances of that class
    private PossibleActionManager(){}

    /// calculatePossibleActionsUnique
    ///
    /// assumes that the state is in a place action state
    ///
    /// @param unique shall only generate and find distinct actions or all possible. distinct in a semantic sense
    /// @param state the current state(in a place action state)
    /// @return set of all possible CarcassonneActions
    public static ActionSet calculatePossibleActions(State state, boolean unique) {
        if (state.isGameOver()) {
            // if game over return empty set
            return new ActionSet(0);
        }

        final var frontier = state.frontier;
        final var currentTile = state.tileDeck.getCurrentTile();

        int rotationCount = unique?currentTile.getDifferentRotationsCount():4;
        // has player at least one free meeple if not skip meeple actions
        boolean hasMeeple = state.meepleRegistry.findFreeMeeple(state.currentPlayer) != 0;

        // approximate actions count to reduce resize effort
        int expectedPerPlacement = hasMeeple ? 5 : 1;   //estimate 5 meeple positions per tile
        ActionSet actions = new ActionSet(frontier.size() * rotationCount * expectedPerPlacement);

        // the template int encodes the edge constrains bit packed. So what are the edge types
        int tileEdges = currentTile.getTileTemplate();

        // frontier is a hashmap to iterate fast over it iterate over all elements of the
        // underlying array and end if no possilbe elements remain to be found
        int remaining = frontier.size();
        for (int i = 0; remaining != 0; i++) {
            int posBits = frontier.getPositionAt(i);
            if (posBits != FrontierMap.EMPTY) {
                remaining--;

                // found a frontier spot(so where a tile could be placed) check if
                // now check if edge constants are valid then generate actions
                int constraints = frontier.getValueAt(i);
                int x = PositionLayoutBit.getX(posBits);
                int y = PositionLayoutBit.getY(posBits);
                for (int rot = 0; rot < rotationCount; rot++) {
                    if (Tile.isPlacementValid(constraints, tileEdges, rot)) {
                        // add action for no meeple action
                        actions.addInt(CarcassonneActionLayoutBit.pack(x, y, rot, -1, 0, true));
                        if (hasMeeple) {
                            calculateLocalAreas(state, x, y, rot);
                            // add the meeple actions
                            addMeepleActions(state, x, y, rot, actions,unique);
                        }
                    }
                }
            }
        }
        return actions;
    }







    /// calculateLocalAreas
    ///
    /// when placing a tile the area composition of the complete stage is changed and though transitivity
    /// tiles that were previously not connected may be connected after the placement. On global
    /// and local scale. This can be a problem because depending on that a meeple may be placeable
    /// there or not.  This method simulates that locally.
    ///
    /// @param state the state which is used as context in which the placement shall be simulated
    /// @param x x-axis pos of the placed tile
    /// @param y y-axis pos of the placed tile
    /// @param rotation [0,3] counterclockwise rotation of the tile
    ///
    /// @return does not return anything but stores the resulting localAreas in possibleActionsData.get().localAreas
    /// the information contains the area information after placement including meeple information
    /// can be used for further meeple placement simulation
    ///
    /// Note: works threadsafe and overwrite  possibleActionsData.get().localAreas
    private static void calculateLocalAreas(State state, int x, int y, int rotation) {
        ActionManagerMap map = possibleActionsData.get().map;
        long[] localAreas =  possibleActionsData.get().localAreas;
        var areas = state.areaRegistry;
        var tiles = state.tiles;
        var current = state.tileDeck.getCurrentTile();

        // works by creating a copy of the local areas possibleActionsData.localAreas and
        // iterates over all neighbor areas(so of the tile oppsoite to the current area)
        // checks if the areaRep of the neighbor has been seen before if not get the maxMeeple
        // information from there. If Already seen merge to the localArea that has already seen
        // it because they now belong to the same area

        // copy the local area in the working array and make map empty
        System.arraycopy(areas.areas, current.getTileId() * 13, localAreas, 0, 12);
        map.clear();

        for (int dir = 0; dir < 4; dir++) {
            // branchless way for calculating the 4 neighborhood of current pos(order left,top,right bottom)
            int xOther = x + ((dir - 1) & ((dir & 1) - 1));
            int yOther = y + ((2 - dir) * (dir & 1));
            long otherTile = tiles.get(PositionLayoutBit.rawPack(xOther, yOther));
            // valid bit is used in bitPacked to != 0 can be used to quickly check there is a tile
            if (otherTile != 0L) {
                for (int offset = 0; offset < 3; offset++) {
                    // offset: left, center, right on a edge
                    int localId = dir * 3 + offset;
                    int rotatedLocalId = (localId + rotation * 3) % 12;
                    // rep of the area at the rotated edge at the offset position
                    long ownRep = AreaRegistry.findLocalRepresentative(localAreas, localAreas[rotatedLocalId]);

                    int otherLocalId = (dir * 3 + 8 - offset) % 12;
                    long otherRep = areas.findRepresentative(Tile.getEdgeArea(areas, otherTile, otherLocalId));
                    // otherRepId rep of the area opposite to ownRep on the neighbor tile
                    int otherRepId = Area.getGlobalAreaId(otherRep);

                    // so iterate over all edge areas and got the opposite area
                    // check if the otherRepId has been seen yet or not
                    int firstLocalRepId = map.get(otherRepId);
                    if (firstLocalRepId == -1) {
                        // if not seen it is a ne one. Need to add it so that future calls
                        // know we've already seen it and extract information if meeple can be placed
                        // there and store that information in the local area working array
                        int ownRepLocalId = AreaLayoutBit.getLocalAreaId(ownRep);
                        map.add(otherRepId, ownRepLocalId);
                        if (Area.hasMeeple(otherRep)) {
                            localAreas[ownRepLocalId] = AreaLayoutBit.setMaxMeeples(ownRep, 1);
                        }
                    } else {
                        // we've seen that are rep already. There exists a path over outside tiles
                        // between two local areas. need to merge them locally
                        long firstLocalRep = AreaRegistry.findLocalRepresentative(localAreas, localAreas[firstLocalRepId]);
                        AreaRegistry.localMerge(localAreas, ownRep, firstLocalRep);
                    }
                }
            }
        }
    }

    /// addMeepleActions
    ///
    /// adds all possible meeple placement action in the providede action ActionSet
    /// makes sure that all meeple actions are inserted exactly once in unique is true
    ///
    /// @param state the state in which the actions need to be possilbe
    /// @param x x-Axis pos of the placed tile
    /// @param y y-Axis pos of the placed tile
    /// @param rot the rotation [0,3] counterclockwise rotaition
    /// @param actions the set in which the actions are placed into
    /// @param unique only rep meeple pos or all
    ///
    /// if unique == true Does not generate all possible meeple action but only all different so does not create duplicates.
    /// duplicate in a semantic sense so two position result in the same result they are duplicates
    ///
    /// Note: assumes calculateLocalAreas was called before and in possibleActionsData.get().localAreas are
    /// the simulated localAreas
    private static void addMeepleActions(State state, int x, int y, int rot, ActionSet actions, boolean unique) {
        long[] localAreas = possibleActionsData.get().localAreas;
        var currentTile = state.tileDeck.getCurrentTile();
        for (int localId = 0; localId < 12; localId++) {
            int rotatedLocalId = (localId + rot * 3) % 12;
            long rep = AreaRegistry.findLocalRepresentative(localAreas, localAreas[rotatedLocalId]);
            boolean isRep = AreaLayoutBit.getLocalAreaId(rep) == rotatedLocalId;
            if ((isRep|| !unique) && AreaLayoutBit.getMaxMeeples(rep) == 0) {
                // add the action only if there are no meeples on the area
                // add actions only for the rep, so that there are no duplicates
                actions.addInt(CarcassonneActionLayoutBit.pack(x, y, rot, localId, 0, true));
            }
        }

        if (currentTile.isMonastery() && (!unique || rot == 0)) {
            long monasteryArea = state.areaRegistry.getMonasteryArea(currentTile.getTileId());
            if (!Area.hasMeeple(state.areaRegistry.findRepresentative(monasteryArea))) {
                // add Monastery action if the current tile has one
                actions.addInt(CarcassonneActionLayoutBit.pack(x, y, rot, 12, 0, true));
            }
        }
    }

    /// getRandomAction
    ///
    /// returns a random action that can be performed in state. The focus is speed. So the
    /// function compared to the alternative(calculating all possible action and take one)
    /// reactively fast. The main draw back is that it is not uniform sampled so has a intenral
    /// bias
    ///
    /// ensures that the returned action is performable
    ///
    /// @param state the state in which the action needs to be performable
    /// @param rand the random object to get the random numbers
    /// @param meepleProb with which probability is a meeple action generated
    /// @return the action encoding of the random action
    public static int getRandomAction(State state, Random rand, float meepleProb) {
        var frontier = state.frontier;
        int tableLen = frontier.slotCount();

        // first randomly select a position in the frontier table and search from there for a valid
        // position. Then select a random rotation for that tile and depending on meeplePropa ranodm meeple position
        int startIdx = rand.nextInt(tableLen);
        boolean hasMeeple = state.meepleRegistry.findFreeMeeple(state.currentPlayer) != 0;

        for (int i = 0; i < tableLen; i++) {
            int currentIndex = (startIdx + i) % tableLen;
            int posBits = frontier.getPositionAt(currentIndex);

            if (posBits != FrontierMap.EMPTY) {
                // found candidate position
                int rot = getRandomActionRotation(state, rand, currentIndex);
                if (rot != -1) {
                    // found a valid pos and rotation
                    int x = PositionLayoutBit.getX(posBits);
                    int y = PositionLayoutBit.getY(posBits);

                    if (rand.nextFloat() <= meepleProb && hasMeeple) {
                        int localId = getRandomMeeple(state, rand, x, y, rot);
                        if (localId != -1) {
                            // return random meeple action
                            return CarcassonneActionLayoutBit.pack(x, y, rot, localId, 0, true);
                        }
                    }
                    // return random place action without meeple
                    return CarcassonneActionLayoutBit.pack(x, y, rot, -1, 0, true);
                }
            }
        }
        return 0;
    }

    /// getRandomActionRotation
    ///
    /// @param state from where the rotation shall be valid
    /// @param rand Random object to get pseduo random numbers
    /// @param frontierIndex index in the frontier table of the position where a rotation shall be selected
    /// @return -1 if there is no possible rotation [0,3] a random possible rotation
    private static int getRandomActionRotation(State state, Random rand, int frontierIndex){
        var currentTile = state.tileDeck.getCurrentTile();
        var frontier = state.frontier;
        int rotationCount = currentTile.getDifferentRotationsCount();
        int constraints = frontier.getValueAt(frontierIndex);
        int tileEdges = currentTile.getTileTemplate();
        int randomRot = rand.nextInt(rotationCount);
        for (int i = 0; i < rotationCount; i++) {
            int rot = ( randomRot+i) %rotationCount;
            // start at a random indext [0,3] and loop over
            if (Tile.isPlacementValid(constraints, tileEdges, rot)) {
                // if pos+rot is valid return that rotation
                return rot;
            }
        }
        return -1;
    }

    /// getRandomMeeple
    ///
    /// takes a x,y,rot configuration and returns one valid meeple placement so the localAreaId
    /// where the meeple shall be placed. Is biased but fast. Returns -1 if non is valid
    ///
    ///  @param state from where the rotation shall be valid
    ///  @param rand Random object to get pseduo random numbers
    /// @param x x-Axios pos of the tile where it will be placed
    /// @param y y-Acis pos of the tile
    /// @param rot rotation of the tile where a meeple pos shall be selected [0,3] counterclockwise
    /// @return localAreaId for meeple placement or -1 if no position is applicable
    private static int getRandomMeeple(State state, Random rand, int x, int y, int rot){
        long[] localAreas = possibleActionsData.get().localAreas;
        // calculate how the areas would look like after placing the current tile at x,y, rot
        calculateLocalAreas(state,x,y,rot);

        var currentTile = state.tileDeck.getCurrentTile();

        // handle monastery like edge areas for more uniform sampeling
        // iterate over all areas with random start point and select the first valid area
        int areaCount = currentTile.isMonastery() ? 13:12;
        int randArea = rand.nextInt(areaCount);
        for(int i = 0; i < areaCount; i++){
            int localId = (randArea +i) % areaCount;
            if(localId == 12){
                // monastery
                long monasteryArea = state.areaRegistry.getMonasteryArea(currentTile.getTileId());
                if (!Area.hasMeeple(state.areaRegistry.findRepresentative(monasteryArea))) {
                    return localId;
                }
            }else{
                // edge Area
                int rotatedLocalId = (localId + rot * 3) % 12;
                long rep = AreaRegistry.findLocalRepresentative(localAreas, localAreas[rotatedLocalId]);
                boolean isRep = AreaLayoutBit.getLocalAreaId(rep) == rotatedLocalId;
                if (isRep && AreaLayoutBit.getMaxMeeples(rep) == 0) {
                    return localId;
                }
            }
        }
        return -1;
    }

    /// getPossibleDrawActions
    ///
    /// returns from a given state all possible draw actions. Do all tiles that could be drawn
    public static ActionSet getPossibleDrawActions(State state){
        ActionSet actionsSet = new ActionSet(40);
        var deck = state.tileDeck;
        for(int i = deck.getDeckPos(); i < deck.getTileCount(); i++){
            int tileId = deck.getAt(i);
            actionsSet.addInt(CarcassonneActionLayoutBit.pack(0, 0, 0, -1, tileId, false));
        }
        return actionsSet;
    }

    /// isMeeplePlacementValid
    ///
    /// checks if a meeple can be placed at the given localAreadId if the current tile is placed at the
    /// given position and rotation. Checks it without changing the TileMap itself but rather simulates it.
    /// Checks only edgeArea
    ///
    /// @param state on which the currentTile is placed
    /// @param x x-Axis pos
    /// @param y y-Axis pos
    /// @param rot [0,3] counterclockwise rotation
    /// @param localAreaId area you want to check (assumes is a edge area in [0,11])
    public static boolean isMeeplePlacementValid(State state, int x, int y, int rot, int localAreaId){
        calculateLocalAreas(state,x,y,rot);
        int rotatedLocalId = (localAreaId + rot * 3) % 12;
        long[] localAreas = possibleActionsData.get().localAreas;
        long rep = AreaRegistry.findLocalRepresentative(localAreas, localAreas[rotatedLocalId]);
        return AreaLayoutBit.getMaxMeeples(rep) == 0;
    }

    static class PossibleActionData{
        ActionManagerMap map = new ActionManagerMap();
        long[] localAreas = new long[13];
    }
}
