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
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.concurrent.CopyOnWriteArraySet;

import ch.sla.jdbcperflogger.DriverConfig;
import ch.sla.jdbcperflogger.driver.LoggingConnectionInvocationHandler;
import ch.sla.jdbcperflogger.model.ConnectionInfo;
import ch.sla.jdbcperflogger.model.LogMessage;

public class PerfLoggerRemoting {

    final static Set<LogSender> senders = new CopyOnWriteArraySet<LogSender>();
    final static Map<LoggingConnectionInvocationHandler, ConnectionInfo> connectionToInfo = new WeakHashMap<LoggingConnectionInvocationHandler, ConnectionInfo>();
    final static List<Closeable> remotingThreads = new ArrayList<Closeable>();

    public static synchronized void start() {
        final Integer serverPort = DriverConfig.INSTANCE.getServerPort();
        if (serverPort != null) {
            remotingThreads.add(PerfLoggerServerThread.spawn(serverPort));
        }
        for (final InetSocketAddress clientAddress : DriverConfig.INSTANCE.getClientAddresses()) {
            remotingThreads.add(PerfLoggerClientThread.spawn(clientAddress));
        }
    }

    public static synchronized void stop() {
        for (final Closeable thread : remotingThreads) {
            try {
                thread.close();
            } catch (final IOException e) {
                // ignore
            }
        }
        remotingThreads.clear();
    }

    private PerfLoggerRemoting() {
    }

    public static void connectionCreated(final LoggingConnectionInvocationHandler connectionHandler,
            final long connectionCreationDuration) {
        final ConnectionInfo info = new ConnectionInfo(connectionHandler.getConnectionUuid(),
                connectionHandler.getConnectionId(), connectionHandler.getUrl(), new Date(), connectionCreationDuration,
                connectionHandler.getConnectionProperties());
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

    public static void addSender(final LogSender sender) {
        senders.add(sender);
    }

    public static void removeSender(final LogSender sender) {
        senders.remove(sender);
    }

}
