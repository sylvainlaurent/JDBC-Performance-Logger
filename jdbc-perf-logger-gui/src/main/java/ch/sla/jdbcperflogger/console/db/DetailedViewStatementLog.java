package ch.sla.jdbcperflogger.console.db;

import java.util.UUID;

import javax.annotation.Nullable;

import ch.sla.jdbcperflogger.StatementType;
import ch.sla.jdbcperflogger.model.ConnectionInfo;

public class DetailedViewStatementLog {
    private final UUID logId;
    private final long timestamp;
    @Nullable
    private final StatementType statementType;
    private final String rawSql;
    private final String filledSql;
    private final String threadName;
    private final long durationNanos;
    @Nullable
    private final String sqlException;
    private final ConnectionInfo connectionInfo;

    public DetailedViewStatementLog(final UUID logId, final ConnectionInfo connectionInfo, final long timestamp,
            @Nullable final StatementType statementType, final String rawSql, final String filledSql,
            final String threadName, final long durationNanos, @Nullable final String exception) {
        this.logId = logId;
        this.connectionInfo = connectionInfo;
        this.timestamp = timestamp;
        this.statementType = statementType;
        this.rawSql = rawSql;
        this.filledSql = filledSql;
        this.threadName = threadName;
        this.durationNanos = durationNanos;
        sqlException = exception;
    }

    public UUID getLogId() {
        return logId;
    }

    public ConnectionInfo getConnectionInfo() {
        return connectionInfo;
    }

    public long getTimestamp() {
        return timestamp;
    }

    @Nullable
    public StatementType getStatementType() {
        return statementType;
    }

    public String getRawSql() {
        return rawSql;
    }

    public String getFilledSql() {
        return filledSql;
    }

    public String getThreadName() {
        return threadName;
    }

    public long getDurationNanos() {
        return durationNanos;
    }

    @Nullable
    public String getSqlException() {
        return sqlException;
    }

}
