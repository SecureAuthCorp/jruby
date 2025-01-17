/*
 * Copyright (c) 2015 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.nodes.core.array;

import com.oracle.truffle.api.dsl.ImportStatic;
import com.oracle.truffle.api.dsl.NodeChild;
import com.oracle.truffle.api.dsl.NodeChildren;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.source.SourceSection;
import org.jruby.truffle.nodes.RubyNode;
import org.jruby.truffle.runtime.RubyContext;
import org.jruby.truffle.runtime.array.ArrayUtils;
import org.jruby.truffle.runtime.layouts.Layouts;

@NodeChildren({
        @NodeChild(value="array", type=RubyNode.class),
        @NodeChild(value="index", type=RubyNode.class),
        @NodeChild(value="value", type=RubyNode.class)
})
@ImportStatic(ArrayGuards.class)
public abstract class ArrayWriteNormalizedNode extends RubyNode {

    @Child private EnsureCapacityArrayNode ensureCapacityNode;
    @Child private GeneralizeArrayNode generalizeNode;

    public ArrayWriteNormalizedNode(RubyContext context, SourceSection sourceSection) {
        super(context, sourceSection);

        // TODO CS 9-Feb-15 make this lazy later on
        ensureCapacityNode = EnsureCapacityArrayNodeGen.create(context, sourceSection, null, null);
        generalizeNode = GeneralizeArrayNodeGen.create(context, sourceSection, null, null);
    }

    public abstract Object executeWrite(VirtualFrame frame, DynamicObject array, int index, Object value);

    // Writing at index 0 into a null array creates a new array of the most specific type

    @Specialization(
            guards={"isRubyArray(array)", "isNullArray(array)", "index == 0"}
    )
    public boolean writeNull0(DynamicObject array, int index, boolean value) {
        ArrayNodes.setStore(array, new Object[]{value}, 1);
        return value;
    }

    @Specialization(
            guards={"isRubyArray(array)", "isNullArray(array)", "index == 0"}
    )
    public int writeNull0(DynamicObject array, int index, int value) {
        ArrayNodes.setStore(array, new int[]{value}, 1);
        return value;
    }

    @Specialization(
            guards={"isRubyArray(array)", "isNullArray(array)", "index == 0"}
    )
    public long writeNull0(DynamicObject array, int index, long value) {
        ArrayNodes.setStore(array, new long[]{value}, 1);
        return value;
    }

    @Specialization(
            guards={"isRubyArray(array)", "isNullArray(array)", "index == 0"}
    )
    public double writeNull0(DynamicObject array, int index, double value) {
        ArrayNodes.setStore(array, new double[]{value}, 1);
        return value;
    }

    @Specialization(
            guards={"isRubyArray(array)", "isNullArray(array)", "index == 0"}
    )
    public DynamicObject writeNull0(DynamicObject array, int index, DynamicObject value) {
        ArrayNodes.setStore(array, new Object[]{value}, 1);
        return value;
    }

    // Writing beyond index 0 in a null array creates an Object[] as we need to fill the rest with nil

    @Specialization(
            guards={"isRubyArray(array)", "isNullArray(array)", "index != 0"}
    )
    public Object writeNullBeyond(DynamicObject array, int index, Object value) {
        final Object[] store = new Object[index + 1];

        for (int n = 0; n < index; n++) {
            store[n] = nil();
        }

        store[index] = value;
        ArrayNodes.setStore(array, store, store.length);
        return value;
    }

    // Writing within an existing array with a compatible type

    @Specialization(
            guards={"isRubyArray(array)", "isObjectArray(array)", "isInBounds(array, index)"}
    )
    public boolean writeWithin(DynamicObject array, int index, boolean value) {
        final Object[] store = (Object[]) Layouts.ARRAY.getStore(array);
        store[index] = value;
        return value;
    }

    @Specialization(
            guards={"isRubyArray(array)", "isIntArray(array)", "isInBounds(array, index)"}
    )
    public int writeWithin(DynamicObject array, int index, int value) {
        final int[] store = (int[]) Layouts.ARRAY.getStore(array);
        store[index] = value;
        return value;
    }

    @Specialization(
            guards={"isRubyArray(array)", "isLongArray(array)", "isInBounds(array, index)"}
    )
    public int writeWithinIntIntoLong(DynamicObject array, int index, int value) {
        writeWithin(array, index, (long) value);
        return value;
    }

    @Specialization(
            guards={"isRubyArray(array)", "isLongArray(array)", "isInBounds(array, index)"}
    )
    public long writeWithin(DynamicObject array, int index, long value) {
        final long[] store = (long[]) Layouts.ARRAY.getStore(array);
        store[index] = value;
        return value;
    }

    @Specialization(
            guards={"isRubyArray(array)", "isDoubleArray(array)", "isInBounds(array, index)"}
    )
    public double writeWithin(DynamicObject array, int index, double value) {
        final double[] store = (double[]) Layouts.ARRAY.getStore(array);
        store[index] = value;
        return value;
    }

    @Specialization(
            guards={"isRubyArray(array)", "isObjectArray(array)", "isInBounds(array, index)"}
    )
    public Object writeWithin(DynamicObject array, int index, Object value) {
        final Object[] store = (Object[]) Layouts.ARRAY.getStore(array);
        store[index] = value;
        return value;
    }

    // Writing within an existing array with an incompatible type - need to generalise

    @Specialization(
            guards={"isRubyArray(array)", "isIntArray(array)", "isInBounds(array, index)"}
    )
    public long writeWithinInt(DynamicObject array, int index, long value) {
        final int[] intStore = (int[]) Layouts.ARRAY.getStore(array);
        final long[] longStore = new long[Layouts.ARRAY.getSize(array)];

        for (int n = 0; n < Layouts.ARRAY.getSize(array); n++) {
            longStore[n] = intStore[n];
        }

        longStore[index] = value;
        ArrayNodes.setStore(array, longStore, Layouts.ARRAY.getSize(array));
        return value;
    }

    @Specialization(
            guards={"isRubyArray(array)", "isIntArray(array)", "isInBounds(array, index)", "!isInteger(value)", "!isLong(value)"}
    )
    public Object writeWithinInt(DynamicObject array, int index, Object value) {
        final Object[] objectStore = ArrayUtils.box((int[]) Layouts.ARRAY.getStore(array));
        objectStore[index] = value;
        ArrayNodes.setStore(array, objectStore, Layouts.ARRAY.getSize(array));
        return value;
    }

    @Specialization(
            guards={"isRubyArray(array)", "isLongArray(array)", "isInBounds(array, index)", "!isInteger(value)", "!isLong(value)"}
    )
    public Object writeWithinLong(DynamicObject array, int index, Object value) {
        final Object[] objectStore = ArrayUtils.box((long[]) Layouts.ARRAY.getStore(array));
        objectStore[index] = value;
        ArrayNodes.setStore(array, objectStore, Layouts.ARRAY.getSize(array));
        return value;
    }

    @Specialization(
            guards={"isRubyArray(array)", "isDoubleArray(array)", "isInBounds(array, index)", "!isDouble(value)"}
    )
    public Object writeWithinDouble(DynamicObject array, int index, Object value) {
        final Object[] objectStore = ArrayUtils.box((double[]) Layouts.ARRAY.getStore(array));
        objectStore[index] = value;
        ArrayNodes.setStore(array, objectStore, Layouts.ARRAY.getSize(array));
        return value;
    }

    // Extending an array of compatible type by just one

    @Specialization(
            guards={"isRubyArray(array)", "isObjectArray(array)", "isExtendingByOne(array, index)"}
    )
    public boolean writeExtendByOne(VirtualFrame frame, DynamicObject array, int index, boolean value) {
        ensureCapacityNode.executeEnsureCapacity(frame, array, index + 1);
        ((Object[]) Layouts.ARRAY.getStore(array))[index] = value;
        ArrayNodes.setStore(array, Layouts.ARRAY.getStore(array), index + 1);
        return value;
    }

    @Specialization(
            guards={"isRubyArray(array)", "isIntArray(array)", "isExtendingByOne(array, index)"}
    )
    public int writeExtendByOne(VirtualFrame frame, DynamicObject array, int index, int value) {
        ensureCapacityNode.executeEnsureCapacity(frame, array, index + 1);
        ((int[]) Layouts.ARRAY.getStore(array))[index] = value;
        ArrayNodes.setStore(array, Layouts.ARRAY.getStore(array), index + 1);
        return value;
    }

    @Specialization(
            guards={"isRubyArray(array)", "isLongArray(array)", "isExtendingByOne(array, index)"}
    )
    public int writeExtendByOneIntIntoLong(VirtualFrame frame, DynamicObject array, int index, int value) {
        ensureCapacityNode.executeEnsureCapacity(frame, array, index + 1);
        ((long[]) Layouts.ARRAY.getStore(array))[index] = value;
        ArrayNodes.setStore(array, Layouts.ARRAY.getStore(array), index + 1);
        return value;
    }

    @Specialization(
            guards={"isRubyArray(array)", "isLongArray(array)", "isExtendingByOne(array, index)"}
    )
    public long writeExtendByOne(VirtualFrame frame, DynamicObject array, int index, long value) {
        ensureCapacityNode.executeEnsureCapacity(frame, array, index + 1);
        ((long[]) Layouts.ARRAY.getStore(array))[index] = value;
        ArrayNodes.setStore(array, Layouts.ARRAY.getStore(array), index + 1);
        return value;
    }

    @Specialization(
            guards={"isRubyArray(array)", "isDoubleArray(array)", "isExtendingByOne(array, index)"}
    )
    public double writeExtendByOne(VirtualFrame frame, DynamicObject array, int index, double value) {
        ensureCapacityNode.executeEnsureCapacity(frame, array, index + 1);
        ((double[]) Layouts.ARRAY.getStore(array))[index] = value;
        ArrayNodes.setStore(array, Layouts.ARRAY.getStore(array), index + 1);
        return value;
    }

    @Specialization(
            guards={"isRubyArray(array)", "isObjectArray(array)", "isExtendingByOne(array, index)"}
    )
    public DynamicObject writeExtendByOne(VirtualFrame frame, DynamicObject array, int index, DynamicObject value) {
        ensureCapacityNode.executeEnsureCapacity(frame, array, index + 1);
        ((Object[]) Layouts.ARRAY.getStore(array))[index] = value;
        ArrayNodes.setStore(array, Layouts.ARRAY.getStore(array), index + 1);
        return value;
    }

    @Specialization(
        guards={"isRubyArray(array)", "isObjectArray(array)", "isExtendingByOne(array, index)"}
    )
    public int writeObjectExtendByOne(VirtualFrame frame, DynamicObject array, int index, int value) {
        ensureCapacityNode.executeEnsureCapacity(frame, array, index + 1);
        ((Object[]) Layouts.ARRAY.getStore(array))[index] = value;
        ArrayNodes.setStore(array, Layouts.ARRAY.getStore(array), index + 1);
        return value;
    }

    // Writing beyond the end of an array - may need to generalise to Object[] or otherwise extend

    @Specialization(
            guards={"isRubyArray(array)", "!isObjectArray(array)", "!isInBounds(array, index)", "!isExtendingByOne(array, index)"}
    )
    public Object writeBeyondPrimitive(VirtualFrame frame, DynamicObject array, int index, Object value) {
        generalizeNode.executeGeneralize(frame, array, index + 1);
        final Object[] objectStore = ((Object[]) Layouts.ARRAY.getStore(array));

        for (int n = Layouts.ARRAY.getSize(array); n < index; n++) {
            objectStore[n] = nil();
        }

        objectStore[index] = value;
        ArrayNodes.setStore(array, Layouts.ARRAY.getStore(array), index + 1);
        return value;
    }

    @Specialization(
            guards={"isRubyArray(array)", "isObjectArray(array)", "!isInBounds(array, index)", "!isExtendingByOne(array, index)"}
    )
    public Object writeBeyondObject(VirtualFrame frame, DynamicObject array, int index, Object value) {
        ensureCapacityNode.executeEnsureCapacity(frame, array, index + 1);
        final Object[] objectStore = ((Object[]) Layouts.ARRAY.getStore(array));

        for (int n = Layouts.ARRAY.getSize(array); n < index; n++) {
            objectStore[n] = nil();
        }

        objectStore[index] = value;
        ArrayNodes.setStore(array, Layouts.ARRAY.getStore(array), index + 1);
        return value;
    }

    // Guards

    protected static boolean isInBounds(DynamicObject array, int index) {
        return index >= 0 && index < Layouts.ARRAY.getSize(array);
    }

    protected static boolean isExtendingByOne(DynamicObject array, int index) {
        return index == Layouts.ARRAY.getSize(array);
    }

}
