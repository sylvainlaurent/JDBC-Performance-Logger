package slaurent.jdbcperflogger;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

public class BatchedPreparedStatementsLog extends AbstractLogMessage {

    private static final long serialVersionUID = 1L;

    private final String rawSql;
    private final List<String> sqlList;

    public BatchedPreparedStatementsLog(final UUID logId, final long timestamp, final long executionTimeNanos,
            final String rawSql, final List<String> sqlList, final String threadName, final SQLException exc) {
        super(logId, timestamp, executionTimeNanos, StatementType.PREPARED_BATCH_EXECUTION, threadName, exc);
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
