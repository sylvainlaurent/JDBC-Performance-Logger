package slaurent.jdbcperflogger;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;

public class StatsLoggingResultSet extends WrappingResultSet {
    private final UUID logId;
    private final long fetchStartTime;
    private final StatementType statementType;
    private boolean closed;
    private int nbRowsIterated;

    public StatsLoggingResultSet(final ResultSet wrappedResultSet, final UUID logId, final StatementType statementType) {
        super(wrappedResultSet);
        this.logId = logId;
        this.statementType = statementType;
        fetchStartTime = System.nanoTime();
    }

    @Override
    public void close() throws SQLException {
        super.close();
        if (!closed) {
            closed = true;
            PerfLogger.logClosedResultSet(logId, System.nanoTime() - fetchStartTime, statementType, nbRowsIterated);
        }
    }

    @Override
    public boolean next() throws SQLException {
        final boolean hasNext = super.next();
        if (hasNext) {
            nbRowsIterated++;
        }
        return hasNext;
    }

}
