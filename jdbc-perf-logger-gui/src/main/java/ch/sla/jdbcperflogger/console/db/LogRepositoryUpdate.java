package ch.sla.jdbcperflogger.console.db;

import ch.sla.jdbcperflogger.logger.ConnectionInfo;
import ch.sla.jdbcperflogger.model.BatchedNonPreparedStatementsLog;
import ch.sla.jdbcperflogger.model.BatchedPreparedStatementsLog;
import ch.sla.jdbcperflogger.model.ResultSetLog;
import ch.sla.jdbcperflogger.model.StatementExecutedLog;
import ch.sla.jdbcperflogger.model.StatementLog;
import ch.sla.jdbcperflogger.model.TxCompleteLog;

public interface LogRepositoryUpdate {

    void addConnection(ConnectionInfo connectionInfo);

    void addStatementLog(StatementLog log);

    void updateLogAfterExecution(StatementExecutedLog log);

    void updateLogWithResultSetLog(ResultSetLog log);

    void addBatchedPreparedStatementsLog(BatchedPreparedStatementsLog log);

    void addBatchedNonPreparedStatementsLog(BatchedNonPreparedStatementsLog log);

    void addTxCompletionLog(final TxCompleteLog log);

    void clear();

}