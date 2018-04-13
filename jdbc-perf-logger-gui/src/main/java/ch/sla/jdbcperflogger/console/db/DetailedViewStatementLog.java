package ch.sla.jdbcperflogger.console.db;

import java.util.UUID;

import lombok.Getter;
import lombok.ToString;
import org.eclipse.jdt.annotation.Nullable;

import ch.sla.jdbcperflogger.StatementType;
import ch.sla.jdbcperflogger.model.ConnectionInfo;

@Getter
@ToString
public class DetailedViewStatementLog {
    private final UUID logId;
    private final long timestamp;
    @Nullable
    private final StatementType statementType;
    private final String rawSql;
    private final String filledSql;
    private final String threadName;
    @Nullable
    private final String sqlException;
    private final ConnectionInfo connectionInfo;

    public DetailedViewStatementLog(final UUID logId, final ConnectionInfo connectionInfo, final long timestamp,
            @Nullable final StatementType statementType, final String rawSql, final String filledSql,
            final String threadName, @Nullable final String exception) {
        this.logId = logId;
        this.connectionInfo = connectionInfo;
        this.timestamp = timestamp;
        this.statementType = statementType;
        this.rawSql = rawSql;
        this.filledSql = filledSql;
        this.threadName = threadName;
        sqlException = exception;
    }

}
