/*
 * Copyright (c) 2015 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.nodes.constants;

import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.source.SourceSection;
import org.jruby.truffle.nodes.RubyNode;
import org.jruby.truffle.nodes.core.BasicObjectNodes;
import org.jruby.truffle.runtime.LexicalScope;
import org.jruby.truffle.runtime.RubyConstant;
import org.jruby.truffle.runtime.RubyContext;
import org.jruby.truffle.runtime.control.RaiseException;

public class ReadConstantWithLexicalScopeNode extends RubyNode implements RestartableReadConstantNode {

    private final LexicalScope lexicalScope;
    private final String name;
    @Child protected LookupConstantWithLexicalScopeNode lookupConstantNode;
    @Child private GetConstantNode getConstantNode;

    public ReadConstantWithLexicalScopeNode(RubyContext context, SourceSection sourceSection, LexicalScope lexicalScope, String name) {
        super(context, sourceSection);
        this.lexicalScope = lexicalScope;
        this.name = name;
        this.lookupConstantNode = LookupConstantWithLexicalScopeNodeGen.create(context, sourceSection, lexicalScope, name);
        this.getConstantNode = GetConstantNodeGen.create(context, sourceSection, this, null, null, null);
    }

    @Override
    public Object execute(VirtualFrame frame) {
        final RubyConstant constant = lookupConstantNode.executeLookupConstant(frame);
        final DynamicObject module = lexicalScope.getLiveModule();

        return getConstantNode.executeGetConstant(frame, module, name, constant);
    }

    @Override
    public Object readConstant(VirtualFrame frame, Object module, String name) {
        return execute(frame);
    }

    @Override
    public Object isDefined(VirtualFrame frame) {
        final RubyConstant constant;
        try {
            constant = lookupConstantNode.executeLookupConstant(frame);
        } catch (RaiseException e) {
            if (BasicObjectNodes.getLogicalClass(((DynamicObject) e.getRubyException())) == getContext().getCoreLibrary().getNameErrorClass()) {
                // private constant
                return nil();
            }
            throw e;
        }

        if (constant == null) {
            return nil();
        } else {
            return createString("constant");
        }
    }

}
