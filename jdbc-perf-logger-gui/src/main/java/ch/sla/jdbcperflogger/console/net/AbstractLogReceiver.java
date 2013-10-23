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
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.net.Socket;
import java.net.SocketTimeoutException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.sla.jdbcperflogger.console.db.LogRepository;
import ch.sla.jdbcperflogger.model.BatchedNonPreparedStatementsLog;
import ch.sla.jdbcperflogger.model.BatchedPreparedStatementsLog;
import ch.sla.jdbcperflogger.model.ResultSetLog;
import ch.sla.jdbcperflogger.model.StatementLog;

public abstract class AbstractLogReceiver extends Thread {
    private final static Logger LOGGER = LoggerFactory.getLogger(AbstractLogReceiver.class);

    protected final LogRepository logRepository;
    protected volatile boolean connected;
    protected volatile boolean paused = false;
    protected volatile boolean disposed = false;

    public AbstractLogReceiver(final LogRepository logRepository) {
        this.logRepository = logRepository;
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

    protected void handleConnection(final Socket socket) throws IOException {
        socket.setKeepAlive(true);
        socket.setSoTimeout(60 * 1000);

        final InputStream is = socket.getInputStream();
        final ObjectInputStream ois = new ObjectInputStream(is);

        try {
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
                } catch (final SocketTimeoutException e) {
                    LOGGER.trace("timeout while reading socket");
                    continue;
                }
                if (o == null || paused || disposed) {
                    continue;
                }
                if (o instanceof StatementLog) {
                    logRepository.addStatementLog((StatementLog) o);
                } else if (o instanceof ResultSetLog) {
                    logRepository.updateLogWithResultSetLog((ResultSetLog) o);
                } else if (o instanceof BatchedNonPreparedStatementsLog) {
                    logRepository.addBatchedNonPreparedStatementsLog((BatchedNonPreparedStatementsLog) o);
                } else if (o instanceof BatchedPreparedStatementsLog) {
                    logRepository.addBatchedPreparedStatementsLog((BatchedPreparedStatementsLog) o);
                } else {
                    throw new IllegalArgumentException("unexpected log, class=" + o.getClass());
                }
            }
        } finally {
            connected = false;
            ois.close();
            LOGGER.debug("Closing socket " + socket);
            socket.close();
        }

    }

    public abstract boolean isServerMode();
}