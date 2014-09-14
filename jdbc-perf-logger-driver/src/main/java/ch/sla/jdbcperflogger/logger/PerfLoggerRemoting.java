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

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketException;
import java.util.Date;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import ch.sla.jdbcperflogger.DriverConfig;
import ch.sla.jdbcperflogger.Logger;
import ch.sla.jdbcperflogger.driver.LoggingConnectionInvocationHandler;
import ch.sla.jdbcperflogger.model.BufferFullLogMessage;
import ch.sla.jdbcperflogger.model.ConnectionInfo;
import ch.sla.jdbcperflogger.model.LogMessage;

public class PerfLoggerRemoting {

    final static Set<LogSender> senders = new CopyOnWriteArraySet<PerfLoggerRemoting.LogSender>();
    final static Map<LoggingConnectionInvocationHandler, ConnectionInfo> connectionToInfo = new WeakHashMap<LoggingConnectionInvocationHandler, ConnectionInfo>();

    static {
        final Integer serverPort = DriverConfig.INSTANCE.getServerPort();
        if (serverPort != null) {
            PerfLoggerServerThread.spawn(serverPort);
        }

        for (final InetSocketAddress clientAddress : DriverConfig.INSTANCE.getClientAddresses()) {
            PerfLoggerClientThread.spawn(clientAddress);
        }
    }

    private PerfLoggerRemoting() {
    }

    public static void connectionCreated(final LoggingConnectionInvocationHandler connectionHandler,
            final long connectionCreationDuration) {
        final ConnectionInfo info = new ConnectionInfo(connectionHandler.getConnectionUuid(),
                connectionHandler.getConnectionId(), connectionHandler.getUrl(), new Date(),
                connectionCreationDuration, connectionHandler.getConnectionProperties());
        synchronized (connectionToInfo) {
            connectionToInfo.put(connectionHandler, info);
            postLog(info);
        }
    }

    static void postLog(final LogMessage log) {
        for (final LogSender sender : senders) {
            sender.postLog(log);
        }
    }

    // public for tests
    public static class LogSender implements Runnable {
        private final static Logger LOGGER2 = Logger.getLogger(LogSender.class);

        private final BlockingQueue<LogMessage> logsToSend = new LinkedBlockingQueue<LogMessage>(10000);
        private final Socket socket;
        private final AtomicBoolean queueFull = new AtomicBoolean();

        LogSender(final Socket socket) throws SocketException {
            this.socket = socket;
            socket.setKeepAlive(true);
            socket.setSoTimeout((int) TimeUnit.SECONDS.toMillis(10));
        }

        void postLog(final LogMessage log) {
            final boolean posted = logsToSend.offer(log);
            if (!posted) {
                queueFull.set(true);
                LOGGER2.warn("queue full, dropping remote log of statement");
            }
        }

        @Override
        public void run() {
            // first send all current connections information to the socket
            synchronized (connectionToInfo) {
                for (final ConnectionInfo connectionInfo : connectionToInfo.values()) {
                    logsToSend.offer(connectionInfo);
                }
            }

            ObjectOutputStream oos = null;
            try {
                oos = new ObjectOutputStream(socket.getOutputStream());
                int cnt = 0;
                while (true) {
                    try {
                        if (queueFull.compareAndSet(true, false)) {
                            oos.writeUnshared(new BufferFullLogMessage(System.currentTimeMillis()));
                        }

                        final LogMessage log = logsToSend.poll(10, TimeUnit.SECONDS);
                        if (log != null) {
                            oos.writeUnshared(log);
                        } else {
                            // check the socket state
                            if (socket.isClosed() || !socket.isConnected()) {
                                // client disconnected
                                break;
                            }
                            oos.writeUnshared(null);
                        }
                        cnt = (cnt + 1) % 10;
                        if (cnt == 0) {
                            // avoid mem leak when the stream keeps back
                            // references to serialized objects
                            oos.reset();
                        }
                    } catch (final InterruptedException e) {
                        break;
                    }
                }
            } catch (final IOException e) {
                LOGGER2.warn("socket error", e);
            } finally {
                LOGGER2.info("closing connection with " + socket);
                senders.remove(this);
                if (oos != null) {
                    try {
                        oos.close();
                    } catch (final IOException ignored) {
                    }
                }
                try {
                    socket.close();
                } catch (final IOException e) {
                    LOGGER2.error("error while closing socket", e);
                }
            }
        }
    }

}
