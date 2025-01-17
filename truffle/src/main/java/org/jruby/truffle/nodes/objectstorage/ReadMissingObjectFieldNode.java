/*
 * Copyright (c) 2013, 2015 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.nodes.objectstorage;

import com.oracle.truffle.api.nodes.InvalidAssumptionException;
import com.oracle.truffle.api.nodes.NodeCost;
import com.oracle.truffle.api.nodes.NodeInfo;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.object.Shape;
import org.jruby.truffle.nodes.core.BasicObjectNodes;

@NodeInfo(cost = NodeCost.POLYMORPHIC)
public class ReadMissingObjectFieldNode extends ReadObjectFieldChainNode {

    public ReadMissingObjectFieldNode(Shape objectLayout, ReadObjectFieldNode next) {
        super(objectLayout, next);
    }

    @Override
    public Object execute(DynamicObject object) {
        try {
            objectLayout.getValidAssumption().check();
        } catch (InvalidAssumptionException e) {
            replace(next);
            return next.execute(object);
        }

        if (object.getShape() == objectLayout) {
            return BasicObjectNodes.getContext(object).getCoreLibrary().getNilObject();
        } else {
            return next.execute(object);
        }
    }

    @Override
    public boolean isSet(DynamicObject object) {
        try {
            objectLayout.getValidAssumption().check();
        } catch (InvalidAssumptionException e) {
            replace(next);
            return next.isSet(object);
        }

        final boolean condition = object.getShape() == objectLayout;

        if (condition) {
            return false;
        } else {
            return next.isSet(object);
        }
    }

}
