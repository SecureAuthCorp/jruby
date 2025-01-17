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

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.dsl.NodeChild;
import com.oracle.truffle.api.dsl.NodeChildren;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.source.SourceSection;
import org.jruby.truffle.nodes.RubyNode;
import org.jruby.truffle.runtime.RubyContext;

@NodeChildren({
        @NodeChild(value="array", type=RubyNode.class),
        @NodeChild(value="index", type=RubyNode.class),
        @NodeChild(value="length", type=RubyNode.class)
})
public abstract class ArrayReadSliceDenormalizedNode extends RubyNode {

    @Child private ArrayReadSliceNormalizedNode readNode;

    public ArrayReadSliceDenormalizedNode(RubyContext context, SourceSection sourceSection) {
        super(context, sourceSection);
    }

    public abstract Object executeReadSlice(VirtualFrame frame, DynamicObject array, int index, int length);

    @Specialization(guards = "isRubyArray(array)")
    public Object read(VirtualFrame frame, DynamicObject array, int index, int length) {
        if (readNode == null) {
            CompilerDirectives.transferToInterpreter();
            readNode = insert(ArrayReadSliceNormalizedNodeGen.create(getContext(), getSourceSection(), null, null, null));
        }

        final int normalizedIndex = ArrayNodes.normalizeIndex(array, index);

        return readNode.executeReadSlice(frame, array, normalizedIndex, length);
    }

}
