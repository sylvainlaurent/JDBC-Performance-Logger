/*
 *  Copyright 2013 Sylvain LAURENT
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package ch.sla.jdbcperflogger.logger;

import static java.util.concurrent.TimeUnit.NANOSECONDS;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.Field;
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

import org.eclipse.jdt.annotation.Nullable;

import ch.sla.jdbcperflogger.DatabaseType;
import ch.sla.jdbcperflogger.Logger;
import ch.sla.jdbcperflogger.StatementType;
import ch.sla.jdbcperflogger.TxCompletionType;
import ch.sla.jdbcperflogger.model.BatchedNonPreparedStatementsLog;
import ch.sla.jdbcperflogger.model.BatchedPreparedStatementsLog;
import ch.sla.jdbcperflogger.model.PreparedStatementValuesHolder;
import ch.sla.jdbcperflogger.model.ResultSetLog;
import ch.sla.jdbcperflogger.model.SqlTypedValue;
import ch.sla.jdbcperflogger.model.StatementExecutedLog;
import ch.sla.jdbcperflogger.model.StatementLog;
import ch.sla.jdbcperflogger.model.TxCompleteLog;

public class PerfLogger {
    private final static Logger LOGGER_ORIGINAL_SQL = Logger.getLogger(PerfLogger.class.getName() + ".originalSql");
    private final static Logger LOGGER_FILLED_SQL = Logger.getLogger(PerfLogger.class.getName() + ".filledSql");
    private final static Logger LOGGER_EXECUTED = Logger.getLogger(PerfLogger.class.getName() + ".executed");
    private final static Logger LOGGER_CLOSED_RESULTSET = Logger
            .getLogger(PerfLogger.class.getName() + ".closedResultSet");
    private final static Logger LOGGER_BATCHED_STATEMENTS_DETAIL = Logger
            .getLogger(PerfLogger.class.getName() + ".batchedStatementDetail");

    private final static Pattern PSTMT_PARAMETERS_PATTERN = Pattern.compile("\\?");

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

    public static void logBeforeStatement(final UUID connectionId, final UUID logId, final String sql,
            final StatementType statementType, final int timeout, final boolean autoCommit, final int transactionIsolation) {
        if (LOGGER_ORIGINAL_SQL.isDebugEnabled()) {
            LOGGER_ORIGINAL_SQL.debug("Before execution of non-prepared stmt " + logId + ": " + sql);
        }
        final long now = System.currentTimeMillis();
        PerfLoggerRemoting.postLog(new StatementLog(connectionId, logId, now, statementType, sql,
                Thread.currentThread().getName(), timeout, autoCommit, transactionIsolation));
    }

    public static void logBeforePreparedStatement(final UUID connectionId, final UUID logId, final String rawSql,
            final PreparedStatementValuesHolder pstmtValues, final StatementType statementType,
            final DatabaseType databaseType, final int timeout, final boolean autoCommit, final int transactionIsolation) {
        if (LOGGER_ORIGINAL_SQL.isDebugEnabled()) {
            LOGGER_ORIGINAL_SQL.debug("Before execution of prepared stmt " + logId + ": " + rawSql);
        }
        final String filledSql = fillParameters(rawSql, pstmtValues, databaseType);
        if (LOGGER_FILLED_SQL.isDebugEnabled()) {
            LOGGER_FILLED_SQL.debug("Before execution of prepared stmt " + logId + ": " + filledSql);
        }
        final long now = System.currentTimeMillis();
        PerfLoggerRemoting.postLog(new StatementLog(connectionId, logId, now, statementType, rawSql, filledSql,
                Thread.currentThread().getName(), timeout, autoCommit, transactionIsolation));
    }

    public static void logNonPreparedBatchedStatements(final UUID connectionId, final UUID logId,
            final List<String> batchedExecutions, final DatabaseType databaseType, final int timeout,
            final boolean autoCommit, final int transactionIsolation) {

        final long now = System.currentTimeMillis();
        if (LOGGER_ORIGINAL_SQL.isDebugEnabled()) {
            LOGGER_ORIGINAL_SQL
                    .debug("Before execution of " + batchedExecutions.size() + " batched non-prepared statements");
        }
        for (int i = 0; i < batchedExecutions.size(); i++) {
            final String sql = batchedExecutions.get(i);
            if (LOGGER_BATCHED_STATEMENTS_DETAIL.isDebugEnabled()) {
                LOGGER_BATCHED_STATEMENTS_DETAIL.debug("#" + i + ": " + sql);
            }
        }
        PerfLoggerRemoting.postLog(new BatchedNonPreparedStatementsLog(connectionId, logId, now, batchedExecutions,
                Thread.currentThread().getName(), timeout, autoCommit, transactionIsolation));
    }

    public static void logPreparedBatchedStatements(final UUID connectionId, final UUID logId, final String rawSql,
            final List<Object> batchedExecutions, final DatabaseType databaseType, final int timeout,
            final boolean autoCommit, final int transactionIsolation) {
        final long now = System.currentTimeMillis();
        if (LOGGER_ORIGINAL_SQL.isDebugEnabled()) {
            LOGGER_ORIGINAL_SQL.debug("Before execution of " + batchedExecutions.size()
                    + " batched prepared statements with raw sql " + rawSql);
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
                LOGGER_BATCHED_STATEMENTS_DETAIL.debug("#" + i + ": " + filledSql);
            }
        }
        PerfLoggerRemoting.postLog(new BatchedPreparedStatementsLog(connectionId, logId, now, rawSql, filledSqlList,
                Thread.currentThread().getName(), timeout, autoCommit, transactionIsolation));
    }

    public static void logStatementExecuted(final UUID logId, final long durationNanos,
            @Nullable final Long updateCount, @Nullable final Throwable sqlException) {
        if (LOGGER_EXECUTED.isDebugEnabled()) {
            LOGGER_EXECUTED.debug(TimeUnit.NANOSECONDS.toMillis(durationNanos) + "ms to execute  stmt #" + logId,
                    sqlException);
        }
        String excString = null;
        if (sqlException != null) {
            excString = dumpException(sqlException);
        }
        PerfLoggerRemoting.postLog(new StatementExecutedLog(logId, durationNanos, updateCount, excString));
    }

    private static String dumpException(final Throwable th) {
        final StringWriter stringWriter = new StringWriter(500);
        th.printStackTrace(new PrintWriter(stringWriter));
        return stringWriter.toString();
    }

    public static void logClosedResultSet(final UUID logId, final long resultSetIterationTimeNanos,
            final long fetchDurationNanos, final int nbRowsIterated) {
        if (LOGGER_CLOSED_RESULTSET.isDebugEnabled()) {
            LOGGER_CLOSED_RESULTSET.debug(NANOSECONDS.toMillis(resultSetIterationTimeNanos)
                    + "ms to use and close ResultSet, " + NANOSECONDS.toMillis(fetchDurationNanos)
                    + "ms in calls to rset.next(), iterating " + nbRowsIterated + " rows for statement #" + logId);
        }
        PerfLoggerRemoting
                .postLog(new ResultSetLog(logId, resultSetIterationTimeNanos, fetchDurationNanos, nbRowsIterated));
    }

    public static void logTransactionComplete(final UUID connectionUuid, final long startTimeStamp,
            final TxCompletionType txCompletionType, final long durationNanos,
            @Nullable final String savePointDescription) {
        final TxCompleteLog log = new TxCompleteLog(connectionUuid, startTimeStamp, txCompletionType, durationNanos,
                Thread.currentThread().getName(), savePointDescription);
        PerfLoggerRemoting.postLog(log);
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
                matcher = matcher.appendReplacement(strBuf, Matcher.quoteReplacement(valueAsString));
            }
        }
        matcher.appendTail(strBuf);
        return strBuf.toString();
    }

    static String getValueAsString(final SqlTypedValue sqlTypedValue, final DatabaseType databaseType) {
        final Object value = sqlTypedValue.value; // using a local variable for null analysis
        final String setter = sqlTypedValue.setter;
        String sqlTypeStr = setter;
        final int sqlType = sqlTypedValue.sqlType != null ? sqlTypedValue.sqlType : 0;
        if (sqlTypeStr == null) {
            sqlTypeStr = typesMap.get(sqlType);
            if (sqlTypeStr == null) {
                sqlTypeStr = "TYPE=" + sqlType;
            }
        }
        String additionalComment = null;

        final StringBuilder strBuilder = new StringBuilder(20);
        if (value == null) {
            strBuilder.append("NULL");
        } else if (sqlType == Types.CHAR //
                || sqlType == Types.VARCHAR//
                || sqlType == -15 // NCHAR, from java 6
                || sqlType == -9 // NVARCHAR, from java 6
                || "setString".equals(setter)//
                || "setNString".equals(setter)) {
            strBuilder.append("'" + value.toString().replace("'", "''") + "'");
        } else if (sqlType == Types.DATE || "setDate".equals(setter) || value instanceof java.sql.Date) {
            java.sql.Date sqlDate;
            if (value instanceof java.sql.Date) {
                sqlDate = (java.sql.Date) value;
            } else {
                sqlDate = new java.sql.Date(((java.util.Date) value).getTime());
            }

            // test if it's a real pure date
            final java.sql.Date pureSqlDate = java.sql.Date.valueOf(sqlDate.toString());
            if (sqlDate.getTime() == pureSqlDate.getTime()) {
                // yes, pure date
                strBuilder.append("date'" + sqlDate.toString() + "'");
            } else {
                strBuilder.append("cast(timestamp'" + new Timestamp(sqlDate.getTime()) + "' as DATE)");
                additionalComment = " (non pure)";
            }
        } else if (sqlType == Types.TIMESTAMP || "setTimestamp".equals(setter) || value instanceof java.sql.Timestamp) {
            Timestamp tstamp;
            if (value instanceof Timestamp) {
                tstamp = (Timestamp) value;
            } else {
                tstamp = new Timestamp(((java.util.Date) value).getTime());
            }
            strBuilder.append("timestamp'" + tstamp.toString() + "'");
        } else if (sqlType == Types.TIME || "setTime".equals(setter) || value instanceof java.sql.Time) {
            java.sql.Time sqlTime;
            if (value instanceof java.sql.Time) {
                sqlTime = (java.sql.Time) value;
            } else {
                sqlTime = new java.sql.Time(((java.util.Date) value).getTime());
            }
            strBuilder.append("time'" + sqlTime.toString() + "'");
        } else if (value instanceof Number) {
            strBuilder.append(value);
        } else if (value instanceof Boolean) {
            if (databaseType == DatabaseType.ORACLE) {
                strBuilder.append(((Boolean) value).booleanValue() ? 1 : 0);
            } else {
                strBuilder.append(value);
            }
        } else {
            strBuilder.append("?");
        }

        strBuilder.append(" /*");
        strBuilder.append(sqlTypeStr);
        if (additionalComment != null) {
            strBuilder.append(additionalComment);
        }
        strBuilder.append("*/");
        return strBuilder.toString();
    }
}
