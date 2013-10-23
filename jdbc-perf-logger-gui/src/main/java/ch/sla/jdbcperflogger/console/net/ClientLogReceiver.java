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

import ch.sla.jdbcperflogger.console.db.LogRepository;

public class ClientLogReceiver extends AbstractLogReceiver {
    private final static Logger LOGGER = LoggerFactory.getLogger(ClientLogReceiver.class);

    final InetSocketAddress targetRemoteAddress;

    public ClientLogReceiver(final String targetHost, final int targetPort, final LogRepository logRepository) {
        super(logRepository);
        targetRemoteAddress = InetSocketAddress.createUnresolved(targetHost, targetPort);
    }

    @Override
    public void run() {
        while (!disposed) {
            try {
                final InetSocketAddress addr = new InetSocketAddress(targetRemoteAddress.getHostName(),
                        targetRemoteAddress.getPort());
                LOGGER.debug("Trying to connect to {}", targetRemoteAddress);
                final Socket socket = new Socket(addr.getAddress(), addr.getPort());
                LOGGER.info("Connected to remote {}", targetRemoteAddress);
                handleConnection(socket);
            } catch (final IOException e) {
                LOGGER.debug("expected error", e);
            }
            LOGGER.debug("Sleeping before trying to connect again to remote {}", targetRemoteAddress);
            try {
                Thread.sleep(TimeUnit.SECONDS.toMillis(10));
            } catch (final InterruptedException e) {
            }

        }
    }

    @Override
    public boolean isServerMode() {
        return false;
    }
}
