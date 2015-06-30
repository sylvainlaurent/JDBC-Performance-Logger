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
package ch.sla.jdbcperflogger.console.net;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.sla.jdbcperflogger.console.db.LogRepositoryUpdate;

public class ServerLogReceiver extends AbstractLogReceiver {
    final static Logger LOGGER = LoggerFactory.getLogger(ServerLogReceiver.class);

    private final Set<AbstractLogReceiver> childReceivers = new CopyOnWriteArraySet<>();
    private final LogRepositoryUpdate logRepository;
    private int listenPort;
    @Nullable
    private volatile ServerSocket serverSocket;
    private final CountDownLatch serverStartedLatch = new CountDownLatch(1);

    public ServerLogReceiver(final int listenPort, final LogRepositoryUpdate logRepository) {
        this.listenPort = listenPort;
        this.logRepository = logRepository;
    }

    @Override
    public void dispose() {
        super.dispose();
        try {
            final ServerSocket serverSocketLocalVar = serverSocket;
            if (serverSocketLocalVar != null) {
                serverSocketLocalVar.close();
            }
        } catch (final IOException e) {
            LOGGER.error("error while closing socket", e);
        }
        try {
            this.join();
        } catch (final InterruptedException e) {
            // ignore
        }
    }

    // visible for testing
    protected int getListenPort() {
        return listenPort;
    }

    @Override
    public int getConnectionsCount() {
        int cnt = 0;
        // it's thread-safe to iterate over childReceivers because it's a CopyOnWriteArraySet
        for (final AbstractLogReceiver receiver : childReceivers) {
            cnt += receiver.getConnectionsCount();
        }
        return cnt;
    }

    @Override
    public void run() {
        try (ServerSocket serverSocketLocalVar = new ServerSocket(listenPort)) {
            // if listenPort is 0, a port has been chosen by the OS
            listenPort = serverSocketLocalVar.getLocalPort();
            serverSocketLocalVar.setSoTimeout((int) TimeUnit.MINUTES.toMillis(5));
            serverSocket = serverSocketLocalVar;
            this.setName("ServerLogReceiver " + listenPort);

            try (LogPersister logPersister = new LogPersister(logRepository)) {
                logPersister.start();

                // signal threads that might be waiting for the server to be ready
                serverStartedLatch.countDown();

                while (!disposed) {
                    try {
                        LOGGER.debug("Waiting for client connections on " + serverSocketLocalVar);
                        @NonNull
                        final Socket socket = serverSocketLocalVar.accept();
                        LOGGER.debug("Got client connection from " + socket);

                        final AbstractLogReceiver logReceiver = new AbstractLogReceiver() {
                            @Override
                            public void run() {
                                try {
                                    handleConnection(socket, logPersister);
                                } catch (final IOException e) {
                                    LOGGER.error("error while receiving logs from " + socket.getRemoteSocketAddress(),
                                            e);
                                } finally {
                                    childReceivers.remove(this);
                                }
                            }

                            @Override
                            public boolean isServerMode() {
                                return true;
                            }

                        };
                        logReceiver.setName("LogReceiver " + socket.getRemoteSocketAddress());
                        if (isPaused()) {
                            logReceiver.pauseReceivingLogs();
                        }
                        childReceivers.add(logReceiver);
                        logReceiver.start();
                    } catch (final SocketTimeoutException e) {
                        LOGGER.debug("timeout while accepting socket", e);
                    } catch (final IOException e) {
                        if (!disposed) {
                            LOGGER.error("error while accepting socket", e);
                        } else {
                            LOGGER.debug("error while accepting socket, the server has been closed", e);
                        }
                    }
                }
            } finally {
                LOGGER.debug("Closing server socket " + serverSocketLocalVar);
                if (!serverSocketLocalVar.isClosed()) {
                    try {
                        serverSocketLocalVar.close();
                    } catch (final IOException e) {
                        LOGGER.error("error while closing socket", e);
                    }
                }
            }
        } catch (final IOException e) {
            lastConnectionError = e;
            throw new RuntimeException(e);
        }

    }

    @Override
    public boolean isServerMode() {
        return true;
    }

    @Override
    public void pauseReceivingLogs() {
        super.pauseReceivingLogs();
        for (final AbstractLogReceiver child : childReceivers) {
            child.pauseReceivingLogs();
        }
    }

    @Override
    public void resumeReceivingLogs() {
        super.resumeReceivingLogs();
        for (final AbstractLogReceiver child : childReceivers) {
            child.resumeReceivingLogs();
        }
    }

    // visible for testing
    void waitUntilServerIsReady() throws InterruptedException {
        serverStartedLatch.await();
    }

}
