/*
 * Copyright (c) 2015 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */

package org.jruby.truffle.nodes.objects;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.dsl.NodeChild;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.source.SourceSection;
import org.jruby.truffle.nodes.RubyNode;
import org.jruby.truffle.nodes.core.BasicObjectNodes;
import org.jruby.truffle.nodes.objectstorage.WriteHeadObjectFieldNode;
import org.jruby.truffle.runtime.RubyContext;

@NodeChild(value = "child")
public abstract class FreezeNode extends RubyNode {

    @Child private WriteHeadObjectFieldNode writeFrozenNode;

    public FreezeNode(RubyContext context, SourceSection sourceSection) {
        super(context, sourceSection);
    }

    public abstract Object executeFreeze(Object object);

    @Specialization
    public Object freeze(boolean object) {
        return object;
    }

    @Specialization
    public Object freeze(int object) {
        return object;
    }

    @Specialization
    public Object freeze(long object) {
        return object;
    }

    @Specialization
    public Object freeze(double object) {
        return object;
    }

    @Specialization(guards = "isNil(nil)")
    public Object freeze(Object nil) {
        return nil();
    }

    @Specialization(guards = "isRubyBignum(object)")
    public Object freezeBignum(DynamicObject object) {
        return object;
    }

    @Specialization(guards = "isRubySymbol(symbol)")
    public Object freezeSymbol(DynamicObject symbol) {
        return symbol;
    }

    @Specialization(guards = { "!isNil(object)", "!isRubyBignum(object)", "!isRubySymbol(object)" })
    public Object freeze(DynamicObject object) {
        if (writeFrozenNode == null) {
            CompilerDirectives.transferToInterpreter();
            writeFrozenNode = insert(new WriteHeadObjectFieldNode(BasicObjectNodes.FROZEN_IDENTIFIER));
        }

        writeFrozenNode.execute(object, true);

        return object;
    }
}
