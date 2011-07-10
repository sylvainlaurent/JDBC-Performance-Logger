package slaurent.jdbcperflogger;

import java.lang.reflect.Field;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PerfLogger {
    private final static Logger LOGGER_ORIGINAL_SQL = LoggerFactory.getLogger(PerfLogger.class.getName()
            + ".originalSql");
    private final static Logger LOGGER_PREPARED_SQL = LoggerFactory.getLogger(PerfLogger.class.getName()
            + ".preparedSql");
    private final static Logger LOGGER_CLOSED_RESULTSET = LoggerFactory.getLogger(PerfLogger.class.getName()
            + ".closedResultSet");
    private final static Logger LOGGER_BATCHED_STATEMENTS_DETAIL = LoggerFactory.getLogger(PerfLogger.class.getName()
            + ".batchedStatementDetail");

    private final static Pattern PSTMT_PARAMETERS_PATTERN = Pattern.compile("\\?");

    private static PerfLoggerServer SERVER;

    private static Map<Integer, String> typesMap;

    static {
        typesMap = new HashMap<Integer, String>();
        try {
            for (final Field field : Types.class.getFields()) {
                if (field.getType() == Integer.TYPE) {
                    typesMap.put(Integer.valueOf(field.getInt(null)), field.getName());
                }
            }
        } catch (final IllegalAccessException e) {
            throw new RuntimeException(e);
        }

    }

    public static void logStatement(final UUID logId, final String sql, final long durationNanos,
            final StatementType statementType, final SQLException sqlException) {
        if (LOGGER_ORIGINAL_SQL.isDebugEnabled()) {
            LOGGER_ORIGINAL_SQL.debug(TimeUnit.NANOSECONDS.toMillis(durationNanos)
                    + "ms to execute non-prepared stmt #" + logId + ": " + sql, sqlException);
        }
        if (LOGGER_PREPARED_SQL.isDebugEnabled()) {
            LOGGER_PREPARED_SQL.debug(TimeUnit.NANOSECONDS.toMillis(durationNanos)
                    + "ms to execute non-prepared stmt #" + logId + ": " + sql, sqlException);
        }
        final long now = System.currentTimeMillis();
        postLog(new StatementLog(logId, now, durationNanos, statementType, sql, Thread.currentThread().getName(),
                sqlException));
    }

    public static void logPreparedStatement(final UUID logId, final String rawSql,
            final PreparedStatementValuesHolder pstmtValues, final long durationNanos,
            final StatementType statementType, final DatabaseType databaseType, final SQLException sqlException) {
        if (LOGGER_ORIGINAL_SQL.isDebugEnabled()) {
            LOGGER_ORIGINAL_SQL.debug(TimeUnit.NANOSECONDS.toMillis(durationNanos) + "ms to execute prepared stmt #"
                    + logId + ": " + rawSql, sqlException);
        }
        final String filledSql = fillParameters(rawSql, pstmtValues, databaseType);
        if (LOGGER_PREPARED_SQL.isDebugEnabled()) {
            LOGGER_PREPARED_SQL.debug(TimeUnit.NANOSECONDS.toMillis(durationNanos)
                    + "ms to execute non-prepared stmt #" + logId + ": " + filledSql, sqlException);
        }
        final long now = System.currentTimeMillis();
        postLog(new StatementLog(logId, now, durationNanos, statementType, rawSql, filledSql, Thread.currentThread()
                .getName(), sqlException));
    }

    public static void logClosedResultSet(final UUID logId, final long durationNanos,
            final StatementType statementType, final int nbRowsIterated) {
        if (LOGGER_CLOSED_RESULTSET.isDebugEnabled()) {
            LOGGER_CLOSED_RESULTSET.debug("{}ms to use and close ResultSet, iterating {} rows for statement #{}",
                    new Object[] { TimeUnit.NANOSECONDS.toMillis(durationNanos), nbRowsIterated, logId });
        }
        final long now = System.currentTimeMillis();
        postLog(new ResultSetLog(logId, now, durationNanos, statementType, Thread.currentThread().getName(),
                nbRowsIterated));
    }

    public static void logNonPreparedBatchedStatements(final List<String> batchedExecutions, final long durationNanos,
            final DatabaseType databaseType, final SQLException sqlException) {

        final long now = System.currentTimeMillis();
        if (LOGGER_ORIGINAL_SQL.isDebugEnabled()) {
            LOGGER_ORIGINAL_SQL.debug(TimeUnit.NANOSECONDS.toMillis(durationNanos) + "ms to execute batch of "
                    + batchedExecutions.size() + " non-prepared statements", sqlException);
        }
        for (int i = 0; i < batchedExecutions.size(); i++) {
            final String sql = batchedExecutions.get(i);
            if (LOGGER_BATCHED_STATEMENTS_DETAIL.isDebugEnabled()) {
                LOGGER_BATCHED_STATEMENTS_DETAIL.debug("#{}: {}", i, sql);
            }
        }
        postLog(new BatchedNonPreparedStatementsLog(UUID.randomUUID(), now, durationNanos, batchedExecutions, Thread
                .currentThread().getName(), sqlException));
    }

    public static void logPreparedBatchedStatements(final String rawSql, final List<Object> batchedExecutions,
            final long durationNanos, final DatabaseType databaseType, final SQLException sqlException) {
        final long now = System.currentTimeMillis();
        if (LOGGER_ORIGINAL_SQL.isDebugEnabled()) {
            LOGGER_ORIGINAL_SQL.debug(TimeUnit.NANOSECONDS.toMillis(durationNanos) + "ms to execute batch of "
                    + batchedExecutions.size() + " prepared statements with rawSql:\n" + rawSql, sqlException);
        }
        final List<String> filledSqlList = new ArrayList<String>(batchedExecutions.size());

        for (int i = 0; i < batchedExecutions.size(); i++) {
            final Object exec = batchedExecutions.get(i);
            final String filledSql;
            if (exec instanceof PreparedStatementValuesHolder) {
                filledSql = fillParameters(rawSql, (PreparedStatementValuesHolder) exec, databaseType);
            } else {
                filledSql = exec.toString();
            }
            filledSqlList.add(filledSql);
            if (LOGGER_BATCHED_STATEMENTS_DETAIL.isDebugEnabled()) {
                LOGGER_BATCHED_STATEMENTS_DETAIL.debug("#{}: {}", i, filledSql);
            }
        }
        postLog(new BatchedPreparedStatementsLog(UUID.randomUUID(), now, durationNanos, rawSql, filledSqlList, Thread
                .currentThread().getName(), sqlException));
    }

    static void postLog(final LogMessage log) {
        getPerfLoggerServer().postLog(log);
    }

    static PerfLoggerServer getPerfLoggerServer() {
        if (SERVER == null) {
            // double-locking
            synchronized (PerfLogger.class) {
                if (SERVER == null) {
                    // TODO make port configurable
                    SERVER = new PerfLoggerServer(4561);
                    SERVER.start();
                }
            }
        }
        return SERVER;
    }

    static String fillParameters(final String sql, final PreparedStatementValuesHolder pstmtValues,
            final DatabaseType databaseType) {
        Matcher matcher = PSTMT_PARAMETERS_PATTERN.matcher(sql);
        int i = 0;
        final StringBuffer strBuf = new StringBuffer((int) (sql.length() * 1.5));
        while (matcher.find()) {
            i++;
            final SqlTypedValue sqlTypedValue = pstmtValues.get(i);
            if (sqlTypedValue != null) {
                final String valueAsString = getValueAsString(sqlTypedValue, databaseType);
                matcher = matcher.appendReplacement(strBuf, valueAsString);
            }
        }
        matcher.appendTail(strBuf);
        return strBuf.toString();
    }

    static String getValueAsString(final SqlTypedValue sqlTypedValue, final DatabaseType databaseType) {
        switch (sqlTypedValue.sqlType) {
        case Types.VARCHAR:
            return "'" + sqlTypedValue.value + "'";
        case Types.DATE: {
            java.sql.Date sqlDate;
            if (sqlTypedValue.value instanceof java.sql.Date) {
                sqlDate = (java.sql.Date) sqlTypedValue.value;
            } else {
                sqlDate = new java.sql.Date(((java.util.Date) sqlTypedValue.value).getTime());
            }
            return "date'" + sqlDate.toString() + "'";
        }
        case Types.TIMESTAMP: {
            Timestamp tstamp;
            if (sqlTypedValue.value instanceof Timestamp) {
                tstamp = (Timestamp) sqlTypedValue.value;
            } else {
                tstamp = new Timestamp(((java.util.Date) sqlTypedValue.value).getTime());
            }
            return "timestamp'" + tstamp.toString() + "'";
        }
        case Types.TIME: {
            java.sql.Time sqlTime;
            if (sqlTypedValue.value instanceof java.sql.Time) {
                sqlTime = (java.sql.Time) sqlTypedValue.value;
            } else {
                sqlTime = new java.sql.Time(((java.util.Date) sqlTypedValue.value).getTime());
            }
            return "time'" + sqlTime.toString() + "'";
        }
        case Types.ARRAY:
        case Types.BLOB:
        case Types.CLOB:
        case Types.BINARY:
        case Types.JAVA_OBJECT:
        case Types.LONGVARBINARY:
        case Types.NCLOB:
        case Types.VARBINARY: {
            final StringBuilder strBuilder = new StringBuilder();
            strBuilder.append("? /*");
            strBuilder.append(typesMap.get(sqlTypedValue.sqlType));
            strBuilder.append("*/");
            return strBuilder.toString();
        }
        default: {
            final StringBuilder strBuilder = new StringBuilder();
            strBuilder.append(String.valueOf(sqlTypedValue.value));
            strBuilder.append("/*");
            strBuilder.append(typesMap.get(sqlTypedValue.sqlType));
            strBuilder.append("*/");
            return strBuilder.toString();
        }
        }
    }

}
