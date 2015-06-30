package ch.sla.jdbcperflogger.console.db;

import java.util.Collection;

import org.eclipse.jdt.annotation.Nullable;

import ch.sla.jdbcperflogger.model.BatchedNonPreparedStatementsLog;
import ch.sla.jdbcperflogger.model.BatchedPreparedStatementsLog;
import ch.sla.jdbcperflogger.model.ConnectionInfo;
import ch.sla.jdbcperflogger.model.ResultSetLog;
import ch.sla.jdbcperflogger.model.StatementExecutedLog;
import ch.sla.jdbcperflogger.model.StatementLog;
import ch.sla.jdbcperflogger.model.TxCompleteLog;

public interface LogRepositoryUpdate {

    void addConnection(ConnectionInfo connectionInfo);

    void addStatementLog(StatementLog log);

    void addStatementFullyExecutedLog(final Collection<StatementFullyExecutedLog> logs);

    void updateLogAfterExecution(StatementExecutedLog log);

    void updateLogWithResultSetLog(ResultSetLog log);

    void addBatchedPreparedStatementsLog(BatchedPreparedStatementsLog log);

    void addBatchedNonPreparedStatementsLog(BatchedNonPreparedStatementsLog log);

    void addTxCompletionLog(final TxCompleteLog log);

    void clear();

    void deleteStatementLog(final long... logIds);

    void dispose();

    long getLastModificationTime();

    void setLastLostMessageTime(@Nullable Long timestamp);

    @Nullable
    Long getLastLostMessageTime();
}