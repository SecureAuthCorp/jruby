/*
 * Copyright (c) 2015 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.nodes.rubinius;

import com.kenai.jffi.MemoryIO;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.ImportStatic;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.source.SourceSection;
import jnr.ffi.Pointer;
import org.jruby.truffle.nodes.core.BasicObjectNodes;
import org.jruby.truffle.nodes.core.PointerGuards;
import org.jruby.truffle.nodes.core.StringNodes;
import org.jruby.truffle.runtime.RubyContext;
import org.jruby.truffle.runtime.layouts.Layouts;
import org.jruby.truffle.runtime.rubinius.RubiniusTypes;
import org.jruby.util.ByteList;
import org.jruby.util.unsafe.UnsafeHolder;

public abstract class PointerPrimitiveNodes {

    /*
     * :pointer_allocate is not a real Rubinius primitive, but Rubinius provides no implementaiton
     * of Pointer#allocate, so we define this primitive and use it in our own code in the core
     * library to define that method.
     */

    @RubiniusPrimitive(name = "pointer_allocate")
    public static abstract class PointerAllocatePrimitiveNode extends RubiniusPrimitiveNode {

        public PointerAllocatePrimitiveNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization
        public DynamicObject allocate(DynamicObject pointerClass) {
            return PointerNodes.createPointer(pointerClass, PointerNodes.NULL_POINTER);
        }

    }

    @RubiniusPrimitive(name = "pointer_malloc")
    public static abstract class PointerMallocPrimitiveNode extends RubiniusPrimitiveNode {

        public PointerMallocPrimitiveNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization
        public DynamicObject malloc(DynamicObject pointerClass, int size) {
            return malloc(pointerClass, (long) size);
        }

        @Specialization
        public DynamicObject malloc(DynamicObject pointerClass, long size) {
            return PointerNodes.createPointer(pointerClass, getMemoryManager().newPointer(UnsafeHolder.U.allocateMemory(size)));
        }

    }

    @RubiniusPrimitive(name = "pointer_free")
    public static abstract class PointerFreePrimitiveNode extends RubiniusPrimitiveNode {

        public PointerFreePrimitiveNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization
        public DynamicObject free(DynamicObject pointer) {
            UnsafeHolder.U.freeMemory(Layouts.POINTER.getPointer(pointer).address());
            return pointer;
        }

    }

    @RubiniusPrimitive(name = "pointer_set_address")
    public static abstract class PointerSetAddressPrimitiveNode extends RubiniusPrimitiveNode {

        public PointerSetAddressPrimitiveNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization
        public long setAddress(DynamicObject pointer, int address) {
            return setAddress(pointer, (long) address);
        }

        @Specialization
        public long setAddress(DynamicObject pointer, long address) {
            Layouts.POINTER.setPointer(pointer, getMemoryManager().newPointer(address));
            return address;
        }

    }

    @RubiniusPrimitive(name = "pointer_add")
    public static abstract class PointerAddPrimitiveNode extends RubiniusPrimitiveNode {

        public PointerAddPrimitiveNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization
        public DynamicObject add(DynamicObject a, int b) {
            return add(a, (long) b);
        }

        @Specialization
        public DynamicObject add(DynamicObject a, long b) {
            return PointerNodes.createPointer(BasicObjectNodes.getLogicalClass(a), getMemoryManager().newPointer(Layouts.POINTER.getPointer(a).address() + b));
        }

    }

    @RubiniusPrimitive(name = "pointer_read_int")
    public static abstract class PointerReadIntPrimitiveNode extends RubiniusPrimitiveNode {

        public PointerReadIntPrimitiveNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization(guards = "isSigned(signed)")
        public int readInt(DynamicObject pointer, boolean signed) {
            return Layouts.POINTER.getPointer(pointer).getInt(0);
        }

        protected boolean isSigned(boolean signed) {
            return signed;
        }

    }

    @RubiniusPrimitive(name = "pointer_read_string", lowerFixnumParameters = 0)
    public static abstract class PointerReadStringPrimitiveNode extends RubiniusPrimitiveNode {

        public PointerReadStringPrimitiveNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization
        public DynamicObject readString(DynamicObject pointer, int length) {
            final byte[] bytes = new byte[length];
            Layouts.POINTER.getPointer(pointer).get(0, bytes, 0, length);
            return createString(bytes);
        }

    }

    @RubiniusPrimitive(name = "pointer_set_autorelease")
    public static abstract class PointerSetAutoreleasePrimitiveNode extends RubiniusPrimitiveNode {

        public PointerSetAutoreleasePrimitiveNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization
        public boolean setAutorelease(DynamicObject pointer, boolean autorelease) {
            // TODO CS 24-April-2015 let memory leak
            return autorelease;
        }

    }

    @RubiniusPrimitive(name = "pointer_set_at_offset", lowerFixnumParameters = {0, 1})
    @ImportStatic(RubiniusTypes.class)
    public static abstract class PointerSetAtOffsetPrimitiveNode extends RubiniusPrimitiveNode {

        public PointerSetAtOffsetPrimitiveNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization(guards = "type == TYPE_INT")
        public int setAtOffsetInt(DynamicObject pointer, int offset, int type, int value) {
            Layouts.POINTER.getPointer(pointer).putInt(offset, value);
            return value;
        }

        @Specialization(guards = "type == TYPE_LONG")
        public long setAtOffsetLong(DynamicObject pointer, int offset, int type, long value) {
            Layouts.POINTER.getPointer(pointer).putLong(offset, value);
            return value;
        }

        @Specialization(guards = "type == TYPE_ULONG")
        public long setAtOffsetULong(DynamicObject pointer, int offset, int type, long value) {
            Layouts.POINTER.getPointer(pointer).putLong(offset, value);
            return value;
        }

        @Specialization(guards = "type == TYPE_ULL")
        public long setAtOffsetULL(DynamicObject pointer, int offset, int type, long value) {
            Layouts.POINTER.getPointer(pointer).putLongLong(offset, value);
            return value;
        }

    }

    @RubiniusPrimitive(name = "pointer_read_pointer")
    public static abstract class PointerReadPointerPrimitiveNode extends RubiniusPrimitiveNode {

        public PointerReadPointerPrimitiveNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization
        public DynamicObject readPointer(DynamicObject pointer) {
            return PointerNodes.createPointer(BasicObjectNodes.getLogicalClass(pointer), Layouts.POINTER.getPointer(pointer).getPointer(0));
        }

    }

    @RubiniusPrimitive(name = "pointer_address")
    public static abstract class PointerAddressPrimitiveNode extends RubiniusPrimitiveNode {

        public PointerAddressPrimitiveNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization
        public long address(DynamicObject pointer) {
            return Layouts.POINTER.getPointer(pointer).address();
        }

    }

    @RubiniusPrimitive(name = "pointer_get_at_offset")
    @ImportStatic(RubiniusTypes.class)
    public static abstract class PointerGetAtOffsetPrimitiveNode extends RubiniusPrimitiveNode {

        public PointerGetAtOffsetPrimitiveNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization(guards = "type == TYPE_CHAR")
        public int getAtOffsetChar(DynamicObject pointer, int offset, int type) {
            return Layouts.POINTER.getPointer(pointer).getByte(offset);
        }

        @Specialization(guards = "type == TYPE_UCHAR")
        public int getAtOffsetUChar(DynamicObject pointer, int offset, int type) {
            return Layouts.POINTER.getPointer(pointer).getByte(offset);
        }

        @Specialization(guards = "type == TYPE_INT")
        public int getAtOffsetInt(DynamicObject pointer, int offset, int type) {
            return Layouts.POINTER.getPointer(pointer).getInt(offset);
        }

        @Specialization(guards = "type == TYPE_SHORT")
        public int getAtOffsetShort(DynamicObject pointer, int offset, int type) {
            return Layouts.POINTER.getPointer(pointer).getShort(offset);
        }

        @Specialization(guards = "type == TYPE_USHORT")
        public int getAtOffsetUShort(DynamicObject pointer, int offset, int type) {
            return Layouts.POINTER.getPointer(pointer).getShort(offset);
        }

        @Specialization(guards = "type == TYPE_LONG")
        public long getAtOffsetLong(DynamicObject pointer, int offset, int type) {
            return Layouts.POINTER.getPointer(pointer).getLong(offset);
        }

        @Specialization(guards = "type == TYPE_ULONG")
        public long getAtOffsetULong(DynamicObject pointer, int offset, int type) {
            return Layouts.POINTER.getPointer(pointer).getLong(offset);
        }

        @Specialization(guards = "type == TYPE_ULL")
        public long getAtOffsetULL(DynamicObject pointer, int offset, int type) {
            return Layouts.POINTER.getPointer(pointer).getLongLong(offset);
        }

        @TruffleBoundary
        @Specialization(guards = "type == TYPE_STRING")
        public DynamicObject getAtOffsetString(DynamicObject pointer, int offset, int type) {
            return createString(Layouts.POINTER.getPointer(pointer).getString(offset));
        }

        @Specialization(guards = "type == TYPE_PTR")
        public DynamicObject getAtOffsetPointer(DynamicObject pointer, int offset, int type) {
            final Pointer readPointer = Layouts.POINTER.getPointer(pointer).getPointer(offset);

            if (readPointer == null) {
                return nil();
            } else {
                return PointerNodes.createPointer(BasicObjectNodes.getLogicalClass(pointer), readPointer);
            }
        }

    }

    @RubiniusPrimitive(name = "pointer_write_string")
    public static abstract class PointerWriteStringPrimitiveNode extends RubiniusPrimitiveNode {

        public PointerWriteStringPrimitiveNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization(guards = "isRubyString(string)")
        public DynamicObject address(DynamicObject pointer, DynamicObject string, int maxLength) {
            final ByteList bytes = Layouts.STRING.getByteList(string);
            final int length = Math.min(bytes.length(), maxLength);
            Layouts.POINTER.getPointer(pointer).put(0, bytes.unsafeBytes(), bytes.begin(), length);
            return pointer;
        }

    }

    @RubiniusPrimitive(name = "pointer_read_string_to_null")
    @ImportStatic(PointerGuards.class)
    public static abstract class PointerReadStringToNullPrimitiveNode extends RubiniusPrimitiveNode {

        public PointerReadStringToNullPrimitiveNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization(guards = "isNullPointer(pointer)")
        public DynamicObject readNullPointer(DynamicObject pointer) {
            return StringNodes.createEmptyString(getContext().getCoreLibrary().getStringClass());
        }

        @TruffleBoundary
        @Specialization(guards = "!isNullPointer(pointer)")
        public DynamicObject readStringToNull(DynamicObject pointer) {
            return createString(MemoryIO.getInstance().getZeroTerminatedByteArray(Layouts.POINTER.getPointer(pointer).address()));
        }

    }

    @RubiniusPrimitive(name = "pointer_write_int")
    public static abstract class PointerWriteIntPrimitiveNode extends RubiniusPrimitiveNode {

        public PointerWriteIntPrimitiveNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization
        public DynamicObject address(DynamicObject pointer, int value) {
            Layouts.POINTER.getPointer(pointer).putInt(0, value);
            return pointer;
        }

    }

}
