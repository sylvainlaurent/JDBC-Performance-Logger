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
import java.net.ServerSocket;
import java.net.Socket;
import java.security.AccessController;
import java.security.PrivilegedAction;

import ch.sla.jdbcperflogger.Logger;

class PerfLoggerServerThread extends Thread implements Closeable {
    private final static Logger LOGGER = Logger.getLogger(PerfLoggerServerThread.class);

    ServerSocket serverSocket;
    volatile boolean done;

    static PerfLoggerServerThread spawn(final int serverPort) {
        // avoid Classloader leaks

        return AccessController.doPrivileged(new PrivilegedAction<PerfLoggerServerThread>() {
            @Override
            public PerfLoggerServerThread run() {
                final ClassLoader savedClassLoader = Thread.currentThread().getContextClassLoader();
                try {
                    Thread.currentThread().setContextClassLoader(null);

                    final PerfLoggerServerThread thread = new PerfLoggerServerThread(serverPort);
                    thread.start();
                    return thread;
                } finally {
                    Thread.currentThread().setContextClassLoader(savedClassLoader);
                }

            }
        });
    }

    private PerfLoggerServerThread(final int serverPort) {
        this.setDaemon(true);
        this.setName("PerfLoggerServer acceptor port " + serverPort);
        try {
            serverSocket = new ServerSocket(serverPort);
        } catch (final IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void run() {
        try {
            while (!done) {
                try {
                    final Socket socket = serverSocket.accept();
                    LOGGER.debug("Got client connection from " + socket);
                    final SocketLogSender sender = new SocketLogSender(socket);
                    final Thread logSenderThread = new Thread(sender, "PerfLoggerServer " + socket.getInetAddress()
                            + ":" + socket.getPort());
                    logSenderThread.setDaemon(true);
                    logSenderThread.start();
                    PerfLoggerRemoting.senders.add(sender);
                } catch (final IOException e) {
                    LOGGER.error("error while accepting socket", e);
                }
            }
        } finally {
            try {
                serverSocket.close();
            } catch (final IOException e) {
                LOGGER.error("error while closing socket", e);
            }
        }
    }

    @Override
    public void close() {
        done = true;
        try {
            serverSocket.close();
        } catch (final IOException e) {
            LOGGER.error("error closing socket at " + serverSocket.getLocalPort(), e);
        }
    }
}