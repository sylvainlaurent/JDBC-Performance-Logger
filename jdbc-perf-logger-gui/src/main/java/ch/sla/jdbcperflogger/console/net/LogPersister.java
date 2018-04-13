package ch.sla.jdbcperflogger.console.net;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

import org.eclipse.jdt.annotation.Nullable;
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

class LogPersister extends Thread implements AutoCloseable {
    final static Logger LOGGER = LoggerFactory.getLogger(LogPersister.class);

    private volatile boolean disposed = false;
    protected final LogRepositoryUpdate logRepository;
    private final BlockingQueue<LogMessage> logs = new ArrayBlockingQueue<>(10000);

    LogPersister(final LogRepositoryUpdate logRepository) {
        this.logRepository = logRepository;
        this.setName("LogPersister");
    }

    void putMessage(final LogMessage msg) {
        try {
            logs.put(msg);
        } catch (final InterruptedException e) {
            LOGGER.warn("interrupted", e);
        }
    }

    @Override
    public void close() {
        disposed = true;
        try {
            this.join();
        } catch (final InterruptedException e) {
            LOGGER.error("error while waiting for LogPersister thread to finish", e);
        }
    }

    @Override
    public void run() {
        final List<LogMessage> drainedLogs = new ArrayList<>(1000);
        final List<StatementFullyExecutedLog> statementFullyExecutedLogs = new ArrayList<>(
                100);

        while (!disposed) {
            @Nullable
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