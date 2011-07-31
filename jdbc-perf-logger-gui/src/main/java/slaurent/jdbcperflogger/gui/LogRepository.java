package slaurent.jdbcperflogger.gui;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.UUID;

import org.h2.Driver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import slaurent.jdbcperflogger.BatchedNonPreparedStatementsLog;
import slaurent.jdbcperflogger.BatchedPreparedStatementsLog;
import slaurent.jdbcperflogger.ResultSetLog;
import slaurent.jdbcperflogger.StatementLog;
import slaurent.jdbcperflogger.StatementType;

public class LogRepository {
    public static final String TSTAMP_COLUMN = "TSTAMP";
    public static final String STMT_TYPE_COLUMN = "STATEMENTTYPE";

    private static String DB_URL = "jdbc:h2:file:logrepository;DB_CLOSE_DELAY=-1";
    private static boolean dbInitialized;
    private static final Logger LOGGER = LoggerFactory.getLogger(LogRepository.class);

    private Connection connection;
    private PreparedStatement addStatementLog;
    private PreparedStatement updateStatementLog;
    private PreparedStatement addBatchedStatementLog;
    private static volatile long lastModificationTime = System.currentTimeMillis();

    public LogRepository() {
        try {
            Driver.class.getClass();
            connection = DriverManager.getConnection(DB_URL);
            // TODO : supprimer Db si erreur ˆ l'initialisation
            initDb();
        } catch (final SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public void close() {
        try {
            addStatementLog.close();
            updateStatementLog.close();
            addBatchedStatementLog.close();
            connection.close();
        } catch (final SQLException e) {
            LOGGER.error("error while closing the connection", e);
            // swallow, nothing we can do
        }
        connection = null;
    }

    private void initDb() throws SQLException {
        synchronized (LogRepository.class) {
            if (!dbInitialized) {
                final Statement stmt = connection.createStatement();
                try {
                    stmt.execute("runscript from 'classpath:initdb.sql' charset 'UTF-8'");
                } finally {
                    stmt.close();
                }

                dbInitialized = true;
            }
        }

        addStatementLog = connection
                .prepareStatement("insert into statement_log (logId, tstamp, statementType, rawSql, filledSql, " //
                        + "executionDurationNanos, threadName, exception)"//
                        + " values(?, ?, ?, ?, ?, ?, ?, ?)");
        updateStatementLog = connection
                .prepareStatement("update statement_log set fetchDurationNanos=?, nbRowsIterated=? where logId=?");

        addBatchedStatementLog = connection
                .prepareStatement("insert into batched_statement_log (logId, batched_stmt_order, filledSql)"
                        + " values(?, ?, ?)");
    }

    public void addStatementLog(StatementLog log) {
        try {
            addStatementLog.setObject(1, log.getLogId());
            addStatementLog.setTimestamp(2, new Timestamp(log.getTimestamp()));
            addStatementLog.setInt(3, log.getStatementType().getId());
            addStatementLog.setString(4, log.getRawSql());
            addStatementLog.setString(5, log.getFilledSql());
            addStatementLog.setLong(6, log.getExecutionTimeNanos());
            addStatementLog.setString(7, log.getThreadName());
            addStatementLog.setObject(8, log.getSqlException());
            addStatementLog.execute();
        } catch (final SQLException e) {
            throw new RuntimeException(e);
        }
        lastModificationTime = System.currentTimeMillis();
    }

    public void updateLogWithResultSetLog(ResultSetLog log) {
        try {
            updateStatementLog.setLong(1, log.getExecutionTimeNanos());
            updateStatementLog.setInt(2, log.getNbRowsIterated());
            updateStatementLog.setObject(3, log.getLogId());
            updateStatementLog.execute();
        } catch (final SQLException e) {
            throw new RuntimeException(e);
        }
        lastModificationTime = System.currentTimeMillis();
    }

    public void addBatchedPreparedStatementsLog(BatchedPreparedStatementsLog log) {
        try {
            addStatementLog.setObject(1, log.getLogId());
            addStatementLog.setTimestamp(2, new Timestamp(log.getTimestamp()));
            addStatementLog.setInt(3, log.getStatementType().getId());
            addStatementLog.setString(4, log.getRawSql());
            addStatementLog.setString(5, "(" + log.getSqlList().size() + " batched statements, click for details)");
            addStatementLog.setLong(6, log.getExecutionTimeNanos());
            addStatementLog.setString(7, log.getThreadName());
            addStatementLog.setObject(8, log.getSqlException());
            addStatementLog.execute();

            addBatchedStatementLog.setObject(1, log.getLogId());
            for (int i = 0; i < log.getSqlList().size(); i++) {
                addBatchedStatementLog.setInt(2, i);
                addBatchedStatementLog.setString(3, log.getSqlList().get(i));
                addBatchedStatementLog.addBatch();
            }
            addBatchedStatementLog.executeBatch();
        } catch (final SQLException e) {
            throw new RuntimeException(e);
        }
        lastModificationTime = System.currentTimeMillis();
    }

    public void addBatchedNonPreparedStatementsLog(BatchedNonPreparedStatementsLog log) {
        try {
            addStatementLog.setObject(1, log.getLogId());
            addStatementLog.setTimestamp(2, new Timestamp(log.getTimestamp()));
            addStatementLog.setInt(3, log.getStatementType().getId());
            addStatementLog.setString(4, "(" + log.getSqlList().size() + " batched statements, click for details)");
            addStatementLog.setString(5, "(click for details)");
            addStatementLog.setLong(6, log.getExecutionTimeNanos());
            addStatementLog.setString(7, log.getThreadName());
            addStatementLog.setObject(8, log.getSqlException());
            addStatementLog.execute();

            addBatchedStatementLog.setObject(1, log.getLogId());
            for (int i = 0; i < log.getSqlList().size(); i++) {
                addBatchedStatementLog.setInt(2, i);
                addBatchedStatementLog.setString(3, log.getSqlList().get(i));

                addBatchedStatementLog.addBatch();
            }
            addBatchedStatementLog.executeBatch();
        } catch (final SQLException e) {
            throw new RuntimeException(e);
        }
        lastModificationTime = System.currentTimeMillis();
    }

    public void clear() {
        try {
            connection.prepareStatement("truncate table statement_log").execute();
            connection.prepareStatement("truncate table batched_statement_log").execute();
        } catch (final SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public void getStatements(String filter, Long minDurationNanos, ResultSetAnalyzer analyzer) {
        String sql = "select id, tstamp, statementType, rawSql, filledSql, " //
                + "exec_plus_fetch_time, execution_time, fetch_time, nbRowsIterated, threadName, error " //
                + "from v_statement_log ";
        sql += getWhereClause(filter, minDurationNanos);
        sql += "order by tstamp";

        try {
            final PreparedStatement statement = connection.prepareStatement(sql);
            applyParametersForWhereClause(filter, minDurationNanos, statement);
            final ResultSet resultSet = statement.executeQuery();
            try {
                analyzer.analyze(resultSet);
            } finally {
                resultSet.close();
                statement.close();
            }
        } catch (final SQLException e) {
            throw new RuntimeException(e);
        }

    }

    public void getStatementsGroupByRawSQL(String filter, Long minDurationNanos, ResultSetAnalyzer analyzer) {
        String sql = "select min(id) as ID, rawSql, count(1) as exec_count, " //
                + "sum(executionDurationNanos) as total_exec_time, "//
                + "max(executionDurationNanos) as max_exec_time, " //
                + "min(executionDurationNanos) as min_exec_time, " //
                + "avg(executionDurationNanos) as avg_exec_time " //
                + "from statement_log ";
        if (filter != null && filter.length() > 0) {
            sql += "where (UPPER(rawSql) like ? or UPPER(filledSql) like ?)";
        }
        sql += "group by rawSql ";
        if (minDurationNanos != null) {
            sql += "having sum(executionDurationNanos)>=? ";
        }
        sql += "order by total_exec_time desc";

        try {
            final PreparedStatement statement = connection.prepareStatement(sql);
            applyParametersForWhereClause(filter, minDurationNanos, statement);
            final ResultSet resultSet = statement.executeQuery();
            try {
                analyzer.analyze(resultSet);
            } finally {
                resultSet.close();
                statement.close();
            }
        } catch (final SQLException e) {
            throw new RuntimeException(e);
        }

    }

    public void getStatementsGroupByFilledSQL(String filter, Long minDurationNanos, ResultSetAnalyzer analyzer) {
        String sql = "select min(id) as ID, rawSql, filledSql, count(1) as exec_count, " //
                + "sum(executionDurationNanos) as total_exec_time, "//
                + "max(executionDurationNanos) as max_exec_time, " //
                + "min(executionDurationNanos) as min_exec_time, " //
                + "avg(executionDurationNanos) as avg_exec_time " //
                + "from statement_log ";
        if (filter != null && filter.length() > 0) {
            sql += "where (UPPER(rawSql) like ? or UPPER(filledSql) like ?)";
        }
        sql += "group by rawSql, filledSql ";
        if (minDurationNanos != null) {
            sql += "having sum(executionDurationNanos)>=?";
        }
        sql += "order by total_exec_time desc";

        try {
            final PreparedStatement statement = connection.prepareStatement(sql);
            applyParametersForWhereClause(filter, minDurationNanos, statement);
            final ResultSet resultSet = statement.executeQuery();
            try {
                analyzer.analyze(resultSet);
            } finally {
                resultSet.close();
                statement.close();
            }
        } catch (final SQLException e) {
            throw new RuntimeException(e);
        }

    }

    private String getWhereClause(String filter, Long minDurationNanos) {
        String sql = "";
        boolean whereAdded = false;
        if (filter != null) {
            sql += "where UPPER(rawSql) like ? or UPPER(filledSql) like ? ";
            whereAdded = true;
        }
        if (minDurationNanos != null) {
            if (!whereAdded) {
                sql += "where ";
                whereAdded = true;
            } else {
                sql += "and ";
            }
            sql += "executionDurationNanos>? ";
        }
        return sql;
    }

    private void applyParametersForWhereClause(String filter, Long minDurationNanos, PreparedStatement statement)
            throws SQLException {
        if (filter != null) {
            statement.setString(1, "%" + filter.toUpperCase() + "%");
            statement.setString(2, "%" + filter.toUpperCase() + "%");
        }
        if (minDurationNanos != null) {
            statement.setLong(filter != null ? 3 : 1, minDurationNanos.longValue());
        }

    }

    public long getLastModificationTime() {
        return lastModificationTime;
    }

    public StatementLog getStatementLog(long id) {
        try {
            final PreparedStatement statement = connection
                    .prepareStatement("select logId, tstamp, statementType, rawSql, filledSql, " //
                            + "executionDurationNanos, threadName, exception from statement_log where id=?");
            statement.setLong(1, id);
            try {
                final ResultSet resultSet = statement.executeQuery();
                StatementLog result = null;
                if (resultSet.next()) {
                    final UUID logId = (UUID) resultSet.getObject(1);
                    final Timestamp tstamp = resultSet.getTimestamp(2);
                    final StatementType statementType = StatementType.fromId(resultSet.getInt(3));
                    final String rawSql = resultSet.getString(4);
                    final String filledSql = resultSet.getString(5);
                    final long durationNanos = resultSet.getLong(6);
                    final String threadName = resultSet.getString(7);
                    final SQLException exception = (SQLException) resultSet.getObject(8);
                    result = new StatementLog(logId, tstamp.getTime(), durationNanos, statementType, rawSql, filledSql,
                            threadName, exception);
                }
                resultSet.close();
                return result;
            } finally {
                statement.close();
            }
        } catch (final SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public int count() {
        try {
            final PreparedStatement statement = connection.prepareStatement("select count(1) from statement_log");
            try {
                final ResultSet resultSet = statement.executeQuery();
                resultSet.next();
                final int result = resultSet.getInt(1);
                resultSet.close();
                return result;
            } finally {
                statement.close();
            }
        } catch (final SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public long getTotalExecAndFetchTimeNanos() {
        try {
            final PreparedStatement statement = connection
                    .prepareStatement("select sum(exec_plus_fetch_time) from v_statement_log");
            try {
                final ResultSet resultSet = statement.executeQuery();
                resultSet.next();
                final long result = resultSet.getLong(1);
                resultSet.close();
                return result;
            } finally {
                statement.close();
            }
        } catch (final SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public long getTotalExecAndFetchTimeNanos(String filter, Long minDurationNanos) {
        String sql = "select sum(exec_plus_fetch_time) from v_statement_log ";
        sql += getWhereClause(filter, minDurationNanos);

        try {
            final PreparedStatement statement = connection.prepareStatement(sql);
            applyParametersForWhereClause(filter, minDurationNanos, statement);
            try {
                final ResultSet resultSet = statement.executeQuery();
                resultSet.next();
                final long result = resultSet.getLong(1);
                resultSet.close();
                return result;
            } finally {
                statement.close();
            }
        } catch (final SQLException e) {
            throw new RuntimeException(e);
        }

    }

    public void getBatchStatementExecutions(UUID logId, ResultSetAnalyzer analyzer) {
        String sql = "select batched_stmt_order, filledSql from batched_statement_log where logId=? ";
        sql += "order by batched_stmt_order";

        try {
            final PreparedStatement statement = connection.prepareStatement(sql);
            statement.setObject(1, logId);
            final ResultSet resultSet = statement.executeQuery();
            try {
                analyzer.analyze(resultSet);
            } finally {
                resultSet.close();
                statement.close();
            }
        } catch (final SQLException e) {
            throw new RuntimeException(e);
        }

    }

}
