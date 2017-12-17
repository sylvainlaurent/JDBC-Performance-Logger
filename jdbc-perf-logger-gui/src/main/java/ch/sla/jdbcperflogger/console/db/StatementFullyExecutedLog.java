package ch.sla.jdbcperflogger.console.db;

import java.util.UUID;

import org.eclipse.jdt.annotation.Nullable;

import ch.sla.jdbcperflogger.StatementType;
import ch.sla.jdbcperflogger.model.ResultSetLog;
import ch.sla.jdbcperflogger.model.StatementExecutedLog;
import ch.sla.jdbcperflogger.model.StatementLog;

public class StatementFullyExecutedLog {
    private final StatementLog statementLog;
    private final StatementExecutedLog statementExecutedLog;
    @Nullable
    private final ResultSetLog resultSetLog;

    public StatementFullyExecutedLog(final StatementLog statementLog, final StatementExecutedLog statementExecutedLog,
            @Nullable final ResultSetLog resultSetLog) {
        this.statementLog = statementLog;
        this.statementExecutedLog = statementExecutedLog;
        this.resultSetLog = resultSetLog;
    }

    public UUID getConnectionUuid() {
        return statementLog.getConnectionUuid();
    }

    public UUID getLogId() {
        return statementLog.getLogId();
    }

    public long getTimestamp() {
        return statementLog.getTimestamp();
    }

    public StatementType getStatementType() {
        return statementLog.getStatementType();
    }

    public String getThreadName() {
        return statementLog.getThreadName();
    }

    public int getTimeout() {
        return statementLog.getTimeout();
    }

    public boolean isAutoCommit() {
        return statementLog.isAutoCommit();
    }

    public int getTransactionIsolation() {
        return statementLog.getTransactionIsolation();
    }

    public String getRawSql() {
        return statementLog.getRawSql();
    }

    public String getFilledSql() {
        return statementLog.getFilledSql();
    }

    public boolean isPreparedStatement() {
        return statementLog.isPreparedStatement();
    }

    public long getExecutionTimeNanos() {
        return statementExecutedLog.getExecutionTimeNanos();
    }

    public long getExecutionPlusResultSetUsageTimeNanos() {
        return statementExecutedLog.getExecutionTimeNanos() + getResultSetUsageDurationNanosDefault0();
    }

    @Nullable
    public Long getUpdateCount() {
        return statementExecutedLog.getUpdateCount();
    }

    @Nullable
    public String getSqlException() {
        return statementExecutedLog.getSqlException();
    }

    @Nullable
    public Long getResultSetUsageDurationNanos() {
        // extracted to local variable to make eclipse null-analysis happy...
        final ResultSetLog resultSetLog2 = resultSetLog;
        return resultSetLog2 != null ? resultSetLog2.getResultSetUsageDurationNanos() : null;
    }

    public long getResultSetUsageDurationNanosDefault0() {
        final Long nanos = getResultSetUsageDurationNanos();
        if (nanos != null) {
            return nanos.longValue();
        } else {
            return 0L;
        }
    }

    @Nullable
    public Long getFetchDurationNanos() {
        // extracted to local variable to make eclipse null-analysis happy...
        final ResultSetLog resultSetLog2 = resultSetLog;
        return resultSetLog2 != null ? resultSetLog2.getFetchDurationNanos() : null;
    }

    @Nullable
    public Integer getNbRowsIterated() {
        // extracted to local variable to make eclipse null-analysis happy...
        final ResultSetLog resultSetLog2 = resultSetLog;
        return resultSetLog2 != null ? resultSetLog2.getNbRowsIterated() : null;
    }
}
