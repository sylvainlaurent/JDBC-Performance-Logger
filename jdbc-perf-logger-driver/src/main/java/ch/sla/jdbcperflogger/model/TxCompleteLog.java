package ch.sla.jdbcperflogger.model;

import java.util.UUID;

import org.eclipse.jdt.annotation.Nullable;

import ch.sla.jdbcperflogger.TxCompletionType;

public class TxCompleteLog implements LogMessage {
    private static final long serialVersionUID = 1L;

    private final UUID connectionUuid;
    private final long timestamp;
    private final TxCompletionType completionType;
    private final long executionTimeNanos;
    private final String threadName;
    @Nullable
    private final String savePointDescription;

    public TxCompleteLog(final UUID connectionUuid, final long timestamp, final TxCompletionType completionType,
            final long executionTimeNanos, final String threadName, final @Nullable String savePointDescription) {
        this.connectionUuid = connectionUuid;
        this.timestamp = timestamp;
        this.completionType = completionType;
        this.executionTimeNanos = executionTimeNanos;
        this.threadName = threadName;
        this.savePointDescription = savePointDescription;
    }

    public UUID getConnectionUuid() {
        return connectionUuid;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public TxCompletionType getCompletionType() {
        return completionType;
    }

    public long getExecutionTimeNanos() {
        return executionTimeNanos;
    }

    public String getThreadName() {
        return threadName;
    }

    @Nullable
    public String getSavePointDescription() {
        return savePointDescription;
    }

    @Override
    public String toString() {
        return "TxCompleteLog["//
                + "connectionUuid=" + connectionUuid//
                + ", timestamp=" + timestamp//
                + ", completionType=" + completionType//
                + ", executionTimeNanos=" + executionTimeNanos//
                + ", threadName=" + threadName//
                + ", savePointDescription=" + savePointDescription//
                + "]";
    }
}
