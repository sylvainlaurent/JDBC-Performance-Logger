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
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.sla.jdbcperflogger.console.db.LogRepositoryUpdate;

public class ClientLogReceiver extends AbstractLogReceiver {
    private final static Logger LOGGER = LoggerFactory.getLogger(ClientLogReceiver.class);

    final InetSocketAddress targetRemoteAddress;
    private final LogRepositoryUpdate logRepository;

    public ClientLogReceiver(final String targetHost, final int targetPort, final LogRepositoryUpdate logRepository) {
        targetRemoteAddress = InetSocketAddress.createUnresolved(targetHost, targetPort);
        this.logRepository = logRepository;
    }

    @Override
    public void run() {
        try (LogPersister logPersister = new LogPersister(logRepository)) {
            logPersister.start();
            while (!disposed) {
                try {
                    LOGGER.debug("Trying to connect to {}:{}", targetRemoteAddress.getHostName(),
                            targetRemoteAddress, targetRemoteAddress.getPort());
                    final Socket socket = new Socket(targetRemoteAddress.getHostName(), targetRemoteAddress.getPort());
                    LOGGER.info("Connected to remote {}", targetRemoteAddress);
                    handleConnection(socket, logPersister);
                } catch (final IOException e) {
                    lastConnectionError = e;
                    LOGGER.debug("expected error", e);
                }
                LOGGER.debug("Sleeping before trying to connect again to remote {}", targetRemoteAddress);
                try {
                    Thread.sleep(TimeUnit.SECONDS.toMillis(10));
                } catch (final InterruptedException ignored) {
                }

            }
        }

    }

    @Override
    public boolean isServerMode() {
        return false;
    }
}
