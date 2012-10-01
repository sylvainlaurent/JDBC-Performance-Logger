package slaurent.jdbcperflogger.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import slaurent.jdbcperflogger.StatementType;

public class BatchedPreparedStatementsLog extends AbstractLogMessage {

    private static final long serialVersionUID = 1L;

    private final String rawSql;
    private final List<String> sqlList;

    public BatchedPreparedStatementsLog(final int connectionId, final UUID logId, final long timestamp,
            final long executionTimeNanos, final String rawSql, final List<String> sqlList, final String threadName,
            final Throwable exc) {
        super(connectionId, logId, timestamp, executionTimeNanos, StatementType.PREPARED_BATCH_EXECUTION, threadName,
                exc);
        this.rawSql = rawSql;
        this.sqlList = Collections.unmodifiableList(new ArrayList<String>(sqlList));
    }

    public String getRawSql() {
        return rawSql;
    }

    public List<String> getSqlList() {
        return sqlList;
    }

}
