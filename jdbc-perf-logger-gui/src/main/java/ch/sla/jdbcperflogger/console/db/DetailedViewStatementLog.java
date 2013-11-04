package ch.sla.jdbcperflogger.console.db;

import javax.annotation.Nullable;

import ch.sla.jdbcperflogger.StatementType;

public class DetailedViewStatementLog {
    private final long keyId;
    private final long timestamp;
    private final StatementType statementType;
    private final String rawSql;
    private final String filledSql;
    private final int connectionId;
    private final String threadName;
    private final long durationNanos;
    @Nullable
    private final Throwable sqlException;

    public DetailedViewStatementLog(final long keyId, final int connectionId, final long timestamp,
            final StatementType statementType, final String rawSql, final String filledSql, final String threadName,
            final long durationNanos, final Throwable exception) {
        this.keyId = keyId;
        this.connectionId = connectionId;
        this.timestamp = timestamp;
        this.statementType = statementType;
        this.rawSql = rawSql;
        this.filledSql = filledSql;
        this.threadName = threadName;
        this.durationNanos = durationNanos;
        sqlException = exception;
    }

    public long getKeyId() {
        return keyId;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public StatementType getStatementType() {
        return statementType;
    }

    public String getRawSql() {
        return rawSql;
    }

    public String getFilledSql() {
        return filledSql;
    }

    public int getConnectionId() {
        return connectionId;
    }

    public String getThreadName() {
        return threadName;
    }

    public long getDurationNanos() {
        return durationNanos;
    }

    @Nullable
    public Throwable getSqlException() {
        return sqlException;
    }

}
