/*
 * Copyright (c) 2015 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.runtime.subsystems;

import com.oracle.truffle.api.Truffle;
import com.oracle.truffle.api.frame.FrameInstance;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.object.DynamicObject;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import org.jruby.truffle.nodes.core.ThreadNodes;
import org.jruby.truffle.runtime.RubyCallStack;
import org.jruby.truffle.runtime.RubyContext;
import org.jruby.truffle.runtime.backtrace.Backtrace;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;

@SuppressWarnings("restriction")
public class InstrumentationServerManager {

    private final RubyContext context;
    private final int port;
    private final HttpServer server; // not final as we want a gentler failure

    private volatile boolean shuttingDown = false;

    public InstrumentationServerManager(RubyContext context, int port) {
        this.context = context;
        this.port = port;

        HttpServer server = null;
        try {
            server = HttpServer.create(new InetSocketAddress(port), 0);
        } catch (IOException e) {
            e.printStackTrace();
        }
        this.server = server;
    }

    public int getPort() {
        return port;
    }

    public void start() {
        server.createContext("/stacks", new HttpHandler() {

            @Override
            public void handle(HttpExchange httpExchange) {
                try {
                    final StringBuilder builder = new StringBuilder();

                    context.getSafepointManager().pauseAllThreadsAndExecuteFromNonRubyThread(false, new SafepointAction() {

                        @Override
                        public void run(DynamicObject thread, Node currentNode) {
                            try {
                                Backtrace backtrace = RubyCallStack.getBacktrace(null);

                                synchronized (this) {
                                    // Not thread-safe so keep the formatting synchronized for now.
                                    String[] lines = Backtrace.DISPLAY_FORMATTER.format(context, null, backtrace);

                                    builder.append(String.format("#%d %s", Thread.currentThread().getId(), Thread.currentThread().getName()));
                                    builder.append("\n");
                                    for (String line : lines) {
                                        builder.append(line);
                                        builder.append("\n");
                                    }
                                    builder.append("\n");
                                }
                            } catch (Throwable e) {
                                e.printStackTrace();
                            }
                        }

                    });

                    final byte[] bytes = builder.toString().getBytes("UTF-8");

                    httpExchange.getResponseHeaders().set("Content-Type", "text/plain");
                    httpExchange.sendResponseHeaders(200, bytes.length);

                    final OutputStream stream = httpExchange.getResponseBody();
                    stream.write(bytes);
                    stream.close();
                } catch (IOException e) {
                    if (shuttingDown) {
                        return;
                    }
                    e.printStackTrace();
                }
            }

        });

        server.createContext("/break", new HttpHandler() {

            @Override
            public void handle(HttpExchange httpExchange) {
                try {
                    Thread mainThread = ThreadNodes.getCurrentFiberJavaThread(context.getThreadManager().getRootThread());
                    context.getSafepointManager().pauseMainThreadAndExecuteLaterFromNonRubyThread(mainThread, new SafepointAction() {
                        @Override
                        public void run(DynamicObject thread, final Node currentNode) {
                            new SimpleShell(context).run(Truffle.getRuntime().getCurrentFrame()
                                    .getFrame(FrameInstance.FrameAccess.MATERIALIZE, true).materialize(), currentNode);
                        }
                    });

                    httpExchange.getResponseHeaders().set("Content-Type", "text/plain");
                    httpExchange.sendResponseHeaders(200, 0);
                    httpExchange.getResponseBody().close();
                } catch (IOException e) {
                    if (shuttingDown) {
                        return;
                    }
                    e.printStackTrace();
                }
            }

        });

        server.start();
    }

    public void shutdown() {
        if (server != null) {
            shuttingDown = true;
            // Leave it some time to send the remaining bytes.
            server.stop(1);
        }
    }

}
