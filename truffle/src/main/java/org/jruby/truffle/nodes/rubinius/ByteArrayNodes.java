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

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.source.SourceSection;
import org.jruby.truffle.nodes.core.*;
import org.jruby.truffle.runtime.RubyContext;
import org.jruby.truffle.runtime.control.RaiseException;
import org.jruby.truffle.runtime.layouts.Layouts;
import org.jruby.truffle.runtime.layouts.rubinius.ByteArrayLayoutImpl;
import org.jruby.util.ByteList;

@CoreClass(name = "Rubinius::ByteArray")
public abstract class ByteArrayNodes {

    public static DynamicObject createByteArray(DynamicObject rubyClass, ByteList bytes) {
        return Layouts.BYTE_ARRAY.createByteArray(Layouts.CLASS.getInstanceFactory(rubyClass), bytes);
    }

    @CoreMethod(names = "get_byte", required = 1, lowerFixnumParameters = 0)
    public abstract static class GetByteNode extends CoreMethodArrayArgumentsNode {

        public GetByteNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization
        public int getByte(DynamicObject bytes, int index) {
            return Layouts.BYTE_ARRAY.getBytes(bytes).get(index) & 0xff;
        }

    }

    @CoreMethod(names = "prepend", required = 1)
    public abstract static class PrependNode extends CoreMethodArrayArgumentsNode {

        public PrependNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization(guards = "isRubyString(string)")
        public DynamicObject prepend(DynamicObject bytes, DynamicObject string) {
            final int prependLength = Layouts.STRING.getByteList(string).getUnsafeBytes().length;
            final int originalLength = Layouts.BYTE_ARRAY.getBytes(bytes).getUnsafeBytes().length;
            final int newLength = prependLength + originalLength;
            final byte[] prependedBytes = new byte[newLength];
            System.arraycopy(Layouts.STRING.getByteList(string).getUnsafeBytes(), 0, prependedBytes, 0, prependLength);
            System.arraycopy(Layouts.BYTE_ARRAY.getBytes(bytes).getUnsafeBytes(), 0, prependedBytes, prependLength, originalLength);
            return ByteArrayNodes.createByteArray(getContext().getCoreLibrary().getByteArrayClass(), new ByteList(prependedBytes));
        }

    }

    @CoreMethod(names = "set_byte", required = 2, lowerFixnumParameters = {0, 1})
    public abstract static class SetByteNode extends CoreMethodArrayArgumentsNode {

        public SetByteNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization
        public Object setByte(DynamicObject bytes, int index, int value) {
            if (index < 0 || index >= Layouts.BYTE_ARRAY.getBytes(bytes).getRealSize()) {
                CompilerDirectives.transferToInterpreter();
                throw new RaiseException(getContext().getCoreLibrary().indexError("index out of bounds", this));
            }

            Layouts.BYTE_ARRAY.getBytes(bytes).set(index, value);
            return Layouts.BYTE_ARRAY.getBytes(bytes).get(index);
        }

    }

    @CoreMethod(names = "size")
    public abstract static class SizeNode extends CoreMethodArrayArgumentsNode {

        public SizeNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization
        public int size(DynamicObject bytes) {
            return Layouts.BYTE_ARRAY.getBytes(bytes).getRealSize();
        }

    }

    @CoreMethod(names = "locate", required = 3, lowerFixnumParameters = {1, 2})
    public abstract static class LocateNode extends CoreMethodArrayArgumentsNode {

        public LocateNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization(guards = "isRubyString(pattern)")
        public Object getByte(DynamicObject bytes, DynamicObject pattern, int start, int length) {
            final int index = new ByteList(Layouts.BYTE_ARRAY.getBytes(bytes), start, length).indexOf(Layouts.STRING.getByteList(pattern));

            if (index == -1) {
                return nil();
            } else {
                return start + index + StringNodes.length(pattern);
            }
        }

    }

}
