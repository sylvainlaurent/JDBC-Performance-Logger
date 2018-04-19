/*
 *  Copyright 2013 Sylvain LAURENT
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package ch.sla.jdbcperflogger.logger;

import java.io.Closeable;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.concurrent.TimeUnit;

import ch.sla.jdbcperflogger.Logger;

class PerfLoggerClientThread extends Thread implements Closeable {
    private final static Logger LOGGER = Logger.getLogger(PerfLoggerClientThread.class);

    private static final int CONNECT_TIMEOUT_MS = 30000;

    volatile boolean done;

    private final InetSocketAddress socketAddress;

    private Socket socket;

    static PerfLoggerClientThread spawn(final InetSocketAddress socketAddress) {
        // avoid Classloader leaks

        return AccessController.doPrivileged(new PrivilegedAction<PerfLoggerClientThread>() {
            @Override
            public PerfLoggerClientThread run() {
                final ClassLoader savedClassLoader = Thread.currentThread().getContextClassLoader();
                try {
                    Thread.currentThread().setContextClassLoader(null);

                    final PerfLoggerClientThread thread = new PerfLoggerClientThread(socketAddress);
                    thread.start();
                    return thread;
                } finally {
                    Thread.currentThread().setContextClassLoader(savedClassLoader);
                }

            }
        });
    }

    private PerfLoggerClientThread(final InetSocketAddress socketAddress) {
        this.setDaemon(true);
        this.setName("PerfLoggerClient " + socketAddress);
        this.socketAddress = socketAddress;
    }

    @Override
    public void run() {
        while (!done) {
            try {
                final InetSocketAddress resolvedAddress = new InetSocketAddress(socketAddress.getHostName(),
                        socketAddress.getPort());
                socket = new Socket();
                socket.connect(resolvedAddress, CONNECT_TIMEOUT_MS);
            } catch (final IOException e) {
                LOGGER.debug("Unable to connect to " + socketAddress + ", will try again later", e);
                quietSleep(30);
                continue;
            }
            LOGGER.debug("Connected to " + socketAddress);
            try {
                final SocketLogSender sender = new SocketLogSender(socket);
                PerfLoggerRemoting.senders.add(sender);
                sender.run();
            } catch (final IOException e) {
                LOGGER.info("Error in connection with " + socketAddress + ", will try again later", e);
            }
        }
        LOGGER.debug("Client for " + socketAddress + "closed");
    }

    @Override
    public void close() {
        done = true;
        try {
            socket.close();
        } catch (final IOException e) {
            LOGGER.info("Error closing socket at port" + socket.getLocalPort());
        }
    }

    private void quietSleep(final int seconds) {
        try {
            Thread.sleep(TimeUnit.SECONDS.toMillis(seconds));
        } catch (final InterruptedException e) {
        }
    }
}