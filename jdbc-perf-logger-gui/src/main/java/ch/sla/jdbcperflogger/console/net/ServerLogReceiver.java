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
import java.util.concurrent.TimeUnit;

import javax.annotation.Nonnull;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.sla.jdbcperflogger.console.db.LogRepositoryUpdate;

public class ServerLogReceiver extends AbstractLogReceiver {
    final static Logger LOGGER = LoggerFactory.getLogger(ServerLogReceiver.class);

    private ServerSocket serverSocket;
    private final Set<AbstractLogReceiver> childReceivers = new CopyOnWriteArraySet<>();
    private final LogRepositoryUpdate logRepository;

    public ServerLogReceiver(final int listenPort, final LogRepositoryUpdate logRepository) {
        try {
            serverSocket = new ServerSocket(listenPort);
            serverSocket.setSoTimeout((int) TimeUnit.MINUTES.toMillis(5));
        } catch (final IOException e) {
            throw new RuntimeException(e);
        }
        this.setName("ServerLogReceiver " + listenPort);

        this.logRepository = logRepository;
    }

    @Override
    public void dispose() {
        super.dispose();
        try {
            serverSocket.close();
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
        return serverSocket.getLocalPort();
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
        try (LogPersister logPersister = new LogPersister(logRepository)) {
            logPersister.start();
            while (!disposed) {
                try {
                    LOGGER.debug("Waiting for client connections on " + serverSocket);
                    @Nonnull
                    final Socket socket = serverSocket.accept();
                    LOGGER.debug("Got client connection from " + socket);

                    final AbstractLogReceiver logReceiver = new AbstractLogReceiver() {
                        @Override
                        public void run() {
                            try {
                                handleConnection(socket, logPersister);
                            } catch (final IOException e) {
                                LOGGER.error("error while receiving logs from " + socket.getRemoteSocketAddress(), e);
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
            LOGGER.debug("Closing server socket " + serverSocket);
            if (!serverSocket.isClosed()) {
                try {
                    serverSocket.close();
                } catch (final IOException e) {
                    LOGGER.error("error while closing socket", e);
                }
            }
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

}
