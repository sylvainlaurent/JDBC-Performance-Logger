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

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.net.Socket;
import java.net.SocketTimeoutException;

import org.eclipse.jdt.annotation.Nullable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.sla.jdbcperflogger.model.LogMessage;

public abstract class AbstractLogReceiver extends Thread {
    private final static Logger LOGGER = LoggerFactory.getLogger(AbstractLogReceiver.class);

    // Visible for testing
    protected int SOCKET_TIMEOUT = 60 * 1000;

    protected volatile boolean connected;
    protected volatile boolean paused = false;
    protected volatile boolean disposed = false;
    @Nullable
    protected Throwable lastConnectionError;

    public AbstractLogReceiver() {
        this.setDaemon(true);
    }

    /**
     * @return the current number of connections with this log receiver
     */
    public int getConnectionsCount() {
        return connected ? 1 : 0;
    }

    public void pauseReceivingLogs() {
        paused = true;
    }

    public void resumeReceivingLogs() {
        paused = false;
    }

    public boolean isPaused() {
        return paused;
    }

    public void dispose() {
        disposed = true;
    }

    protected void handleConnection(final Socket socket, final LogPersister logPersister) throws IOException {
        socket.setKeepAlive(true);
        socket.setSoTimeout(SOCKET_TIMEOUT);

        final InputStream is = socket.getInputStream();

        try (ObjectInputStream ois = new ObjectInputStream(is)) {
            connected = true;
            while (!disposed) {
                Object o;
                try {
                    o = ois.readObject();
                } catch (final ClassNotFoundException e) {
                    LOGGER.error(
                            "unknown class, maybe the client is not compatible with the GUI? the msg will be skipped",
                            e);
                    continue;
                } catch (final EOFException e) {
                    LOGGER.debug("The remote closed its connection");
                    lastConnectionError = e;
                    break;
                } catch (final SocketTimeoutException e) {
                    LOGGER.debug("timeout while reading socket");
                    lastConnectionError = e;
                    continue;
                }
                if (o == null || paused || disposed) {
                    continue;
                }

                logPersister.putMessage((LogMessage) o);

            }
        } finally {
            connected = false;
            LOGGER.debug("Closing socket " + socket);
            socket.close();
        }

    }

    public abstract boolean isServerMode();

    public @Nullable Throwable getLastConnectionError() {
        return lastConnectionError;
    }

}