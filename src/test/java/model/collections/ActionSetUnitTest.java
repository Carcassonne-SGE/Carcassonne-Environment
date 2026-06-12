package model.collections;

import org.junit.jupiter.api.Test;
import sge.CarcassonneAction;

import java.util.Iterator;
import java.util.NoSuchElementException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ActionSetUnitTest {

    @Test
    void addAndAddIntIncreaseSizeAndPreserveOrder() {
        ActionSet actionSet = new ActionSet(1);
        CarcassonneAction first = placeAction(1, 2, 3, 4);
        CarcassonneAction second = placeAction(-1, -2, 1, 5);

        actionSet.add(first);
        actionSet.addInt(second.getValue());

        assertEquals(2, actionSet.size());
        assertEquals(first.getValue(), actionSet.get(0));
        assertEquals(second.getValue(), actionSet.get(1));
    }

    @Test
    void clearResetsVisibleContents() {
        ActionSet actionSet = new ActionSet(2);
        actionSet.add(placeAction(0, 0, 0, CarcassonneAction.NO_MEEPLE_AREA_ID));
        actionSet.add(drawAction(7));

        actionSet.clear();

        assertEquals(0, actionSet.size());
        assertFalse(actionSet.iterator().hasNext());
    }

    @Test
    void iteratorReturnsAllElementsAndThrowsWhenExhausted() {
        ActionSet actionSet = new ActionSet(1);
        CarcassonneAction first = placeAction(1, 1, 0, 2);
        CarcassonneAction second = drawAction(9);
        actionSet.add(first);
        actionSet.add(second);

        Iterator<CarcassonneAction> iterator = actionSet.iterator();

        assertTrue(iterator.hasNext());
        assertEquals(first, iterator.next());
        assertTrue(iterator.hasNext());
        assertEquals(second, iterator.next());
        assertFalse(iterator.hasNext());
        assertThrows(NoSuchElementException.class, iterator::next);
    }

    @Test
    void containsUsesActionEqualityAndRejectsOtherObjects() {
        ActionSet actionSet = new ActionSet(1);
        CarcassonneAction stored = placeAction(3, -2, 2, 6);
        actionSet.add(stored);

        assertTrue(actionSet.contains(placeAction(3, -2, 2, 6)));
        assertFalse(actionSet.contains(placeAction(3, -2, 2, 5)));
        assertFalse(actionSet.contains("not an action"));
    }

    @Test
    void getAndGetActionObjectValidateBounds() {
        ActionSet actionSet = new ActionSet(1);
        CarcassonneAction action = drawAction(12);
        actionSet.add(action);

        assertEquals(action.getValue(), actionSet.get(0));
        assertEquals(action, actionSet.getActionObject(0));
        assertThrows(IndexOutOfBoundsException.class, () -> actionSet.get(-1));
        assertThrows(IndexOutOfBoundsException.class, () -> actionSet.get(1));
        assertThrows(IndexOutOfBoundsException.class, () -> actionSet.getActionObject(-1));
        assertThrows(IndexOutOfBoundsException.class, () -> actionSet.getActionObject(1));
    }

    @Test
    void growsBeyondInitialCapacity() {
        ActionSet actionSet = new ActionSet(1);

        actionSet.add(placeAction(0, 0, 0, 0));
        actionSet.add(placeAction(1, 0, 1, 1));
        actionSet.add(placeAction(2, 0, 2, 2));

        assertEquals(3, actionSet.size());
        assertEquals(2, actionSet.getActionObject(2).getX());
    }

    private static CarcassonneAction placeAction(int x, int y, int rotation, int areaId) {
        return new CarcassonneAction(true, x, y, rotation, areaId, 0);
    }

    private static CarcassonneAction drawAction(int tileId) {
        return new CarcassonneAction(false, 0, 0, 0, 0, tileId);
    }
}
