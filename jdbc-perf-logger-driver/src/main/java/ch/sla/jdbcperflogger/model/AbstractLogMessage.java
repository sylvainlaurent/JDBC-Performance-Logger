package ch.sla.jdbcperflogger.model;

import java.util.UUID;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

import ch.sla.jdbcperflogger.StatementType;

@ParametersAreNonnullByDefault
public class AbstractLogMessage implements LogMessage {

    private static final long serialVersionUID = 3766419115920080440L;
    private final int connectionId;
    private final UUID logId;
    private final long timestamp;
    private final long executionTimeNanos;
    private final StatementType statementType;
    private final String threadName;
    @Nullable
    private final Throwable sqlException;

    public AbstractLogMessage(final int connectionId, final UUID logId, final long timestamp,
            final long executionTimeNanos, final StatementType statementType, final String threadName,
            @Nullable final Throwable sqlException) {
        this.connectionId = connectionId;
        this.logId = logId;
        this.timestamp = timestamp;
        this.executionTimeNanos = executionTimeNanos;
        this.statementType = statementType;
        this.threadName = threadName;
        this.sqlException = sqlException;
    }

    public int getConnectionId() {
        return connectionId;
    }

    public UUID getLogId() {
        return logId;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public long getExecutionTimeNanos() {
        return executionTimeNanos;
    }

    public StatementType getStatementType() {
        return statementType;
    }

    public String getThreadName() {
        return threadName;
    }

    @Nullable
    public Throwable getSqlException() {
        return sqlException;
    }

}