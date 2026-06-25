package model.collections;

import sge.CarcassonneAction;
import model.bits.CarcassonneActionLayoutBit;

import java.util.AbstractSet;
import java.util.Iterator;
import java.util.NoSuchElementException;

import java.util.AbstractSet;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.function.IntFunction;

/// ActionSet
///
/// ia a very simple and fast implementation of a AbstractSet of type CarcassonneAction
/// used to comply with the SGE Env contract(the interfaces)
///
/// To avoid boxing and other heap overhead as much as possible it uses internally a int[]
/// the action. When created you can give it a initial Capacity and it automatically increases
/// in size if needed.
///
/// Additionally, to the CarcassonneAction typed Set methods there are also simple int methods that avoid
/// the boxing
///
/// Note: may not be completely in line with the abstract types of AbstractSet. For example does not handle
/// duplicates. Acts more like a List. But did speed up the simulations a lot and for the user this should
/// make no difference because the game creates the set
///
///  Uses int a value but is only intended for CarcassonneActionLayout
public final class ActionSet extends AbstractSet<CarcassonneAction> {
    private int[] data;
    private int size;

    /// ActionSet public Constructor
    ///
    /// @param initialCapacity size of the internal array at the start
    public ActionSet(int initialCapacity) {
        this.data = new int[Math.max(1, initialCapacity)];
        this.size = 0;
    }

    /// add
    /// inserts a value into the set this is the boxed variant needed for the interface
    /// @return true
    /// Note: does not check for duplicates and uses just the addInt method
    @Override
    public boolean add(CarcassonneAction value) {
        addInt(value.getValue());
        return true;
    }

    /// size
    ///
    /// returns how many elements are in the sets. Initially 0 for each incremented by one
    @Override
    public int size() {
        return size;
    }

    /// clear
    ///
    /// set the size to 0
    /// Note: does not really delete the elements in the array just the size var. Should not matter because they cannot
    /// be accessed anymore. But do not expose the reference otherwise well, it could be accessed
    @Override
    public void clear() {
        size = 0;
    }

    /// addInt
    ///
    /// adds a value to the set. If the set is not large enough it is increased
    /// does not check if that value is already in the set
    /// Note: should be called when possible not the boxed variant
    public void addInt(int value) {
        int s = size;
        if (s == data.length) grow(s + 1);
        data[s] = value;
        size = s + 1;
    }

    /// iterator
    ///
    /// simple iterator that goes over all elements. If a value was inserted multiple time the iterator does also
    /// show it multiple times
    @Override
    public Iterator<CarcassonneAction> iterator() {
        return new Iterator<>() {
            private int i = 0;
            @Override
            public boolean hasNext() {
                return i < size;
            }
            @Override
            public CarcassonneAction next() {
                if (i >= size) throw new NoSuchElementException();
                int val = data[i++];
                return createActionObject(val);
            }
        };
    }

    // speed up sge match function because the iterator way of the standard implementation of contains is slow
    @Override
    public boolean contains(Object o) {
      if(!(o instanceof CarcassonneAction action)){
            return false;
        }
        int value = action.getValue();
        for (int i = 0; i < size; i++){
            if(CarcassonneAction.areActionsEqual(value,data[i])){
                return true;
            }
        }
        return false;
    }

    /// grow
    ///
    /// private helper function increases the size of the array
    private void grow(int minCapacity) {
        int newCap = data.length + (data.length >>> 1) + 1;
        if (newCap < minCapacity) newCap = minCapacity;
        int[] n = new int[newCap];
        System.arraycopy(data, 0, n, 0, size);
        data = n;
    }
    /// get
    ///
    /// returns the element at an index
    /// @throws IndexOutOfBoundsException if index >= size || index < 0
    public int get(int index){
        if(index < size && index >= 0){
            return data[index];
        }
        throw new IndexOutOfBoundsException();
    }

    /// getActionObject
    ///
    /// returns the element at an index in the class format of the action for the interface outwards
    /// @throws IndexOutOfBoundsException if index >= size || index < 0
    public CarcassonneAction getActionObject(int index){
        if(index < size && index >= 0){
            return createActionObject(data[index]);
        }
        throw new IndexOutOfBoundsException();
    }

    private CarcassonneAction createActionObject(int val ){
        return new CarcassonneAction(
                CarcassonneActionLayoutBit.getIsAction(val),
                CarcassonneActionLayoutBit.getX(val),
                CarcassonneActionLayoutBit.getY(val),
                CarcassonneActionLayoutBit.getRotation(val),
                CarcassonneActionLayoutBit.getAreaId(val),
                CarcassonneActionLayoutBit.getTileId(val)
        );
    }

}