/*
 * Copyright (c) 2015 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.nodes.supercall;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.source.SourceSection;
import org.jruby.truffle.nodes.RubyNode;
import org.jruby.truffle.runtime.RubyArguments;
import org.jruby.truffle.runtime.RubyContext;
import org.jruby.truffle.runtime.control.RaiseException;
import org.jruby.truffle.runtime.methods.InternalMethod;

/**
 * Represents a super call with implicit arguments without a surrounding method
 */
public class ZSuperOutsideMethodNode extends RubyNode {

    final boolean insideDefineMethod;
    @Child LookupSuperMethodNode lookupSuperMethodNode;

    public ZSuperOutsideMethodNode(RubyContext context, SourceSection sourceSection, boolean insideDefineMethod) {
        super(context, sourceSection);
        this.insideDefineMethod = insideDefineMethod;
        lookupSuperMethodNode = LookupSuperMethodNodeGen.create(context, sourceSection, null);
    }

    @Override
    public final Object execute(VirtualFrame frame) {
        // This is MRI behavior
        CompilerDirectives.transferToInterpreter();
        if (insideDefineMethod) { // TODO (eregon, 22 July 2015): This check should be more dynamic.
            throw new RaiseException(getContext().getCoreLibrary().runtimeError(
                    "implicit argument passing of super from method defined by define_method() is not supported." +
                            " Specify all arguments explicitly.", this));
        } else {
            throw new RaiseException(getContext().getCoreLibrary().noSuperMethodError(this));
        }
    }

    @Override
    public Object isDefined(VirtualFrame frame) {
        final Object self = RubyArguments.getSelf(frame.getArguments());
        final InternalMethod superMethod = lookupSuperMethodNode.executeLookupSuperMethod(frame, self);

        if (superMethod == null) {
            return nil();
        } else {
            return createString("super");
        }
    }

}
