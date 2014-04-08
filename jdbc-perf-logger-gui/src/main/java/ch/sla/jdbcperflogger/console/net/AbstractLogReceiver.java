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
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.sla.jdbcperflogger.console.db.LogRepositoryUpdate;
import ch.sla.jdbcperflogger.console.db.StatementFullyExecutedLog;
import ch.sla.jdbcperflogger.model.BatchedNonPreparedStatementsLog;
import ch.sla.jdbcperflogger.model.BatchedPreparedStatementsLog;
import ch.sla.jdbcperflogger.model.BufferFullLogMessage;
import ch.sla.jdbcperflogger.model.ConnectionInfo;
import ch.sla.jdbcperflogger.model.LogMessage;
import ch.sla.jdbcperflogger.model.ResultSetLog;
import ch.sla.jdbcperflogger.model.StatementExecutedLog;
import ch.sla.jdbcperflogger.model.StatementLog;
import ch.sla.jdbcperflogger.model.TxCompleteLog;

public abstract class AbstractLogReceiver extends Thread {
    private final static Logger LOGGER = LoggerFactory.getLogger(AbstractLogReceiver.class);

    protected final LogRepositoryUpdate logRepository;
    protected volatile boolean connected;
    protected volatile boolean paused = false;
    protected volatile boolean disposed = false;

    protected BlockingQueue<LogMessage> logs = new ArrayBlockingQueue<>(10000);

    public AbstractLogReceiver(final LogRepositoryUpdate logRepository) {
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
        final LogPersister logPersister = new LogPersister();
        logPersister.start();
        try {
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
                        break;
                    } catch (final SocketTimeoutException e) {
                        LOGGER.trace("timeout while reading socket");
                        continue;
                    }
                    if (o == null || paused || disposed) {
                        continue;
                    }
                    try {
                        logs.put((LogMessage) o);
                    } catch (final InterruptedException e) {
                        LOGGER.warn("interrupted", e);
                        continue;
                    }

                }
            } finally {
                connected = false;

                LOGGER.debug("Closing socket " + socket);
                socket.close();
            }
        } finally {
            try {
                logPersister.join();
            } catch (final InterruptedException e) {
                LOGGER.error("error while waiting for LogPersister thread to finish", e);
            }
        }

    }

    public abstract boolean isServerMode();

    private class LogPersister extends Thread {

        @Override
        public void run() {
            final List<LogMessage> drainedLogs = new ArrayList<>(1000);
            final List<StatementFullyExecutedLog> statementFullyExecutedLogs = new ArrayList<StatementFullyExecutedLog>(
                    100);

            while (!disposed) {
                LogMessage logMessage;
                try {
                    logMessage = logs.poll(1, TimeUnit.SECONDS);
                } catch (final InterruptedException e) {
                    LOGGER.warn("interrupted", e);
                    continue;
                }

                if (logMessage == null) {
                    continue;
                }
                drainedLogs.clear();
                drainedLogs.add(logMessage);
                logs.drainTo(drainedLogs);

                for (int i = 0; i < drainedLogs.size(); i++) {
                    logMessage = drainedLogs.get(i);
                    if (i < drainedLogs.size() - 2 && logMessage instanceof StatementLog
                            && drainedLogs.get(i + 1) instanceof StatementExecutedLog) {
                        final StatementExecutedLog statementExecutedLog = (StatementExecutedLog) drainedLogs.get(i + 1);
                        ResultSetLog resultSetLog = null;
                        if (drainedLogs.get(i + 2) instanceof ResultSetLog) {
                            resultSetLog = (ResultSetLog) drainedLogs.get(i + 2);
                        }
                        final StatementFullyExecutedLog statementFullyExecutedLog = new StatementFullyExecutedLog(
                                (StatementLog) logMessage, statementExecutedLog, resultSetLog);
                        statementFullyExecutedLogs.add(statementFullyExecutedLog);

                        i += 1 + (resultSetLog != null ? 1 : 0);
                        continue;
                    }

                    if (!statementFullyExecutedLogs.isEmpty()) {
                        logRepository.addStatementFullyExecutedLog(statementFullyExecutedLogs);
                        statementFullyExecutedLogs.clear();
                    }

                    if (logMessage instanceof ConnectionInfo) {
                        logRepository.addConnection((ConnectionInfo) logMessage);
                    } else if (logMessage instanceof StatementLog) {
                        logRepository.addStatementLog((StatementLog) logMessage);
                    } else if (logMessage instanceof StatementExecutedLog) {
                        logRepository.updateLogAfterExecution((StatementExecutedLog) logMessage);
                    } else if (logMessage instanceof ResultSetLog) {
                        logRepository.updateLogWithResultSetLog((ResultSetLog) logMessage);
                    } else if (logMessage instanceof BatchedNonPreparedStatementsLog) {
                        logRepository.addBatchedNonPreparedStatementsLog((BatchedNonPreparedStatementsLog) logMessage);
                    } else if (logMessage instanceof BatchedPreparedStatementsLog) {
                        logRepository.addBatchedPreparedStatementsLog((BatchedPreparedStatementsLog) logMessage);
                    } else if (logMessage instanceof TxCompleteLog) {
                        logRepository.addTxCompletionLog((TxCompleteLog) logMessage);
                    } else if (logMessage instanceof BufferFullLogMessage) {
                        logRepository.setLastLostMessageTime(((BufferFullLogMessage) logMessage).getTimestamp());
                    } else {
                        throw new IllegalArgumentException("unexpected log, class=" + logMessage.getClass());
                    }
                }

                if (!statementFullyExecutedLogs.isEmpty()) {
                    logRepository.addStatementFullyExecutedLog(statementFullyExecutedLogs);
                    statementFullyExecutedLogs.clear();
                }

            }
        }
    }
}