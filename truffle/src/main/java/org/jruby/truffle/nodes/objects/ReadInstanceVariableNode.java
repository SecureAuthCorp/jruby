/*
 * Copyright (c) 2013, 2015 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.nodes.objects;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.UnexpectedResultException;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.object.Property;
import com.oracle.truffle.api.object.Shape;
import com.oracle.truffle.api.source.SourceSection;
import com.oracle.truffle.api.utilities.BranchProfile;
import org.jruby.truffle.nodes.RubyNode;
import org.jruby.truffle.nodes.objectstorage.ReadHeadObjectFieldNode;
import org.jruby.truffle.runtime.RubyContext;
import org.jruby.truffle.translator.ReadNode;

public class ReadInstanceVariableNode extends RubyNode implements ReadNode {

    @Child private RubyNode receiver;
    @Child private ReadHeadObjectFieldNode readNode;
    private final boolean isGlobal;

    private final BranchProfile primitiveProfile = BranchProfile.create();

    public ReadInstanceVariableNode(RubyContext context, SourceSection sourceSection, String name, RubyNode receiver, boolean isGlobal) {
        super(context, sourceSection);
        this.receiver = receiver;
        readNode = new ReadHeadObjectFieldNode(name);
        this.isGlobal = isGlobal;
    }

    @Override
    public int executeInteger(VirtualFrame frame) throws UnexpectedResultException {
        final Object receiverObject = receiver.execute(frame);

        if (receiverObject instanceof DynamicObject) {
            return readNode.executeInteger((DynamicObject) receiverObject);
        } else {
            // TODO(CS): need to put this onto the fast path?

            CompilerDirectives.transferToInterpreter();
            throw new UnexpectedResultException(nil());
        }
    }

    @Override
    public long executeLong(VirtualFrame frame) throws UnexpectedResultException {
        final Object receiverObject = receiver.execute(frame);

        if (receiverObject instanceof DynamicObject) {
            return readNode.executeLong((DynamicObject) receiverObject);
        } else {
            // TODO(CS): need to put this onto the fast path?

            CompilerDirectives.transferToInterpreter();
            throw new UnexpectedResultException(nil());
        }
    }

    @Override
    public double executeDouble(VirtualFrame frame) throws UnexpectedResultException {
        final Object receiverObject = receiver.execute(frame);

        if (receiverObject instanceof DynamicObject) {
            return readNode.executeDouble((DynamicObject) receiverObject);
        } else {
            // TODO(CS): need to put this onto the fast path?

            CompilerDirectives.transferToInterpreter();
            throw new UnexpectedResultException(nil());
        }
    }

    @Override
    public Object execute(VirtualFrame frame) {
        final Object receiverObject = receiver.execute(frame);

        if (receiverObject instanceof DynamicObject) {
            return readNode.execute((DynamicObject) receiverObject);
        } else {
            primitiveProfile.enter();
            return nil();
        }
    }

    @Override
    public Object isDefined(VirtualFrame frame) {
        CompilerDirectives.transferToInterpreter();

        if (isGlobal) {
            final DynamicObject receiverValue = (DynamicObject) receiver.execute(frame);

            if (readNode.getName().equals("$~") || readNode.getName().equals("$!")) {
                return createString("global-variable");
            } else if (readNode.isSet(receiverValue)) {
                if (readNode.execute(receiverValue) == nil()) {
                    return nil();
                } else {
                    return createString("global-variable");
                }
            } else {
                return nil();
            }
        }

        final Object receiverObject = receiver.execute(frame);

        if (receiverObject instanceof DynamicObject) {
            final DynamicObject receiverRubyObject = (DynamicObject) receiverObject;

            final Shape layout = receiverRubyObject.getShape();
            final Property storageLocation = layout.getProperty(readNode.getName());

            if (storageLocation != null) {
                return createString("instance-variable");
            } else {
                return nil();
            }
        } else {
            return false;
        }
    }

    @Override
    public RubyNode makeWriteNode(RubyNode rhs) {
        return new WriteInstanceVariableNode(getContext(), getSourceSection(), (String) readNode.getName(), receiver, rhs, isGlobal);
    }
}
