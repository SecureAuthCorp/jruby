/*
 * Copyright (c) 2014, 2015 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.nodes.exceptions;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.source.SourceSection;
import org.jruby.truffle.nodes.RubyNode;
import org.jruby.truffle.runtime.RubyContext;
import org.jruby.truffle.runtime.backtrace.Backtrace;
import org.jruby.truffle.runtime.control.RaiseException;
import org.jruby.truffle.runtime.control.ThreadExitException;
import org.jruby.truffle.runtime.layouts.Layouts;

public class TopLevelRaiseHandler extends RubyNode {

    @Child private RubyNode body;

    public TopLevelRaiseHandler(RubyContext context, SourceSection sourceSection, RubyNode body) {
        super(context, sourceSection);
        this.body = body;
    }

    @Override
    public Object execute(VirtualFrame frame) {
        try {
            return body.execute(frame);
        } catch (RaiseException e) {
            handleException(e);
        } catch (ThreadExitException e) {
            // Ignore
        }

        return nil();
    }

    @TruffleBoundary
    private void handleException(RaiseException e) {
        final Object rubyException = e.getRubyException();

        for (String line : Backtrace.DISPLAY_FORMATTER.format(getContext(), (DynamicObject) rubyException, Layouts.EXCEPTION.getBacktrace((DynamicObject) rubyException))) {
            System.err.println(line);
        }

        System.exit(1);
    }

}
