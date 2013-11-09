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
package ch.sla.jdbcperflogger.console.db;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.h2.Driver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.sla.jdbcperflogger.StatementType;
import ch.sla.jdbcperflogger.logger.ConnectionInfo;
import ch.sla.jdbcperflogger.model.BatchedNonPreparedStatementsLog;
import ch.sla.jdbcperflogger.model.BatchedPreparedStatementsLog;
import ch.sla.jdbcperflogger.model.ResultSetLog;
import ch.sla.jdbcperflogger.model.StatementExecutedLog;
import ch.sla.jdbcperflogger.model.StatementLog;

public class LogRepositoryJdbc implements LogRepositoryRead, LogRepositoryUpdate {
    // TODO ajouter colonne clientId (processId)
    public static final int SCHEMA_VERSION = 4;

    public static final String TSTAMP_COLUMN = "TSTAMP";
    public static final String STMT_TYPE_COLUMN = "STATEMENTTYPE";
    public static final String FILLED_SQL_COLUMN = "FILLEDSQL";
    public static final String RAW_SQL_COLUMN = "RAWSQL";
    public static final String EXEC_TIME_COLUMN = "execution_time";
    public static final String FETCH_TIME_COLUMN = "fetch_time";
    public static final String NB_ROWS_COLUMN = "nbRowsIterated";
    public static final String EXEC_PLUS_FETCH_TIME_COLUMN = "EXEC_PLUS_FETCH_TIME";
    public static final String THREAD_NAME_COLUMN = "threadName";
    public static final String CONNECTION_ID_COLUMN = "connectionId";
    public static final String ERROR_COLUMN = "ERROR";
    public static final String EXEC_COUNT_COLUMN = "EXEC_COUNT";
    public static final String TOTAL_EXEC_TIME_COLUMN = "TOTAL_EXEC_TIME";

    private static final int NB_ROWS_MAX = Integer.parseInt(System.getProperty("maxLoggedStatements", "20000"));
    private static final long CLEAN_UP_PERIOD_MS = TimeUnit.SECONDS.toMillis(30);

    private static final Logger LOGGER = LoggerFactory.getLogger(LogRepositoryJdbc.class);

    private final Connection connection;
    private final String repoName;
    private final PreparedStatement addStatementLog;
    private final PreparedStatement updateStatementLogWithResultSet;
    private final PreparedStatement updateStatementLogAfterExecution;
    private final PreparedStatement addBatchedStatementLog;
    private long lastModificationTime = System.currentTimeMillis();
    private final Timer cleanupTimer;

    public LogRepositoryJdbc(final String name) {
        repoName = name;
        try {
            connection = createDbConnection("logdb/logrepository_" + name);
            final Statement stmt = connection.createStatement();
            try {
                stmt.execute("runscript from 'classpath:initdb.sql' charset 'UTF-8'");
            } finally {
                stmt.close();
            }

            cleanOldConnectionInfo(connection);

            addStatementLog = connection
                    .prepareStatement("insert into statement_log (logId, tstamp, statementType, rawSql, filledSql, " //
                            + "threadName, connectionId)"//
                            + " values(?, ?, ?, ?, ?, ?, ?)");
            updateStatementLogWithResultSet = connection
                    .prepareStatement("update statement_log set fetchDurationNanos=?, nbRowsIterated=? where logId=?");
            updateStatementLogAfterExecution = connection
                    .prepareStatement("update statement_log set executionDurationNanos=?, exception=? where logId=?");

            addBatchedStatementLog = connection
                    .prepareStatement("insert into batched_statement_log (logId, batched_stmt_order, filledSql)"
                            + " values(?, ?, ?)");

            cleanupTimer = new Timer(true);
            cleanupTimer.schedule(new CleanupTask(), CLEAN_UP_PERIOD_MS, CLEAN_UP_PERIOD_MS);
        } catch (final SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private Connection createDbConnection(final String path) throws SQLException {
        return createDbConnection(path, false);
    }

    private Connection createDbConnection(final String path, final boolean inRecursion) throws SQLException {
        Driver.class.getClass();
        LOGGER.debug("Opening H2 connection for log repository " + path);
        try {
            @Nonnull
            final Connection conn = DriverManager.getConnection("jdbc:h2:file:" + path + ";DB_CLOSE_DELAY=1");
            LOGGER.debug("connection commit mode auto={}", conn.getAutoCommit());
            conn.setAutoCommit(true);
            checkSchemaVersion(conn);

            return conn;
        } catch (final SQLException exc) {
            if (inRecursion) {
                throw exc;
            }
            LOGGER.warn("Unexpected error while opening DB connection, will delete DB files and try agaoin", exc);

            deleteDbFiles(path);
            return createDbConnection(path, true);
        }
    }

    private void deleteDbFiles(final String path) {
        final File dbFile = new File(path + ".h2.db");
        dbFile.delete();
        final File dbTraceFile = new File(path + ".trace.db");
        dbTraceFile.delete();
    }

    public void dispose() {
        LOGGER.debug("closing H2 connection for log repository " + repoName);
        cleanupTimer.cancel();
        try {
            addStatementLog.close();
            updateStatementLogWithResultSet.close();
            addBatchedStatementLog.close();
            connection.close();
        } catch (final SQLException e) {
            LOGGER.error("error while closing the connection", e);
            // swallow, nothing we can do
        }
    }

    private void cleanOldConnectionInfo(final Connection conn) throws SQLException {
        final Statement statement = conn.createStatement();
        final int nbRowsDeleted = statement.executeUpdate("delete from connection_info where not exists "
                + "(select 1 from statement_log where statement_log.connectionId=connection_info.connectionId)");
        LOGGER.debug("Deleted {} rows in connection_info", nbRowsDeleted);
        statement.close();
    }

    @Override
    public synchronized void addStatementLog(final StatementLog log) {
        try {
            int i = 1;
            addStatementLog.setObject(i++, log.getLogId());
            addStatementLog.setTimestamp(i++, new Timestamp(log.getTimestamp()));
            addStatementLog.setInt(i++, log.getStatementType().getId());
            addStatementLog.setString(i++, log.getRawSql());
            addStatementLog.setString(i++, log.getFilledSql());
            addStatementLog.setString(i++, log.getThreadName());
            addStatementLog.setObject(i++, log.getConnectionUuid());
            addStatementLog.execute();
        } catch (final SQLException e) {
            throw new RuntimeException(e);
        }
        lastModificationTime = System.currentTimeMillis();
    }

    @Override
    public synchronized void updateLogAfterExecution(final StatementExecutedLog log) {
        try {
            updateStatementLogAfterExecution.setLong(1, log.getExecutionTimeNanos());
            updateStatementLogAfterExecution.setObject(2, log.getSqlException());
            updateStatementLogAfterExecution.setObject(3, log.getLogId());
            updateStatementLogAfterExecution.execute();
        } catch (final SQLException e) {
            throw new RuntimeException(e);
        }
        lastModificationTime = System.currentTimeMillis();
    }

    @Override
    public synchronized void updateLogWithResultSetLog(final ResultSetLog log) {
        try {
            updateStatementLogWithResultSet.setLong(1, log.getResultSetIterationTimeNanos());
            updateStatementLogWithResultSet.setInt(2, log.getNbRowsIterated());
            updateStatementLogWithResultSet.setObject(3, log.getLogId());
            updateStatementLogWithResultSet.execute();
        } catch (final SQLException e) {
            throw new RuntimeException(e);
        }
        lastModificationTime = System.currentTimeMillis();
    }

    @Override
    public synchronized void addBatchedPreparedStatementsLog(final BatchedPreparedStatementsLog log) {
        try {
            int i = 1;
            addStatementLog.setObject(i++, log.getLogId());
            addStatementLog.setTimestamp(i++, new Timestamp(log.getTimestamp()));
            addStatementLog.setInt(i++, log.getStatementType().getId());
            addStatementLog.setString(i++, log.getRawSql());
            addStatementLog.setString(i++, "(" + log.getSqlList().size() + " batched statements, click for details)");
            addStatementLog.setString(i++, log.getThreadName());
            addStatementLog.execute();

            addBatchedStatementLog.setObject(1, log.getLogId());
            for (int j = 0; j < log.getSqlList().size(); j++) {
                addBatchedStatementLog.setInt(2, j);
                addBatchedStatementLog.setString(3, log.getSqlList().get(j));
                addBatchedStatementLog.addBatch();
            }
            addBatchedStatementLog.executeBatch();
        } catch (final SQLException e) {
            throw new RuntimeException(e);
        }
        lastModificationTime = System.currentTimeMillis();
    }

    @Override
    public synchronized void addBatchedNonPreparedStatementsLog(final BatchedNonPreparedStatementsLog log) {
        try {
            int i = 1;
            addStatementLog.setObject(i++, log.getLogId());
            addStatementLog.setTimestamp(i++, new Timestamp(log.getTimestamp()));
            addStatementLog.setInt(i++, log.getStatementType().getId());
            addStatementLog.setString(i++, "(" + log.getSqlList().size() + " batched statements, click for details)");
            addStatementLog.setString(i++, "(click for details)");
            addStatementLog.setString(i++, log.getThreadName());
            addStatementLog.execute();

            addBatchedStatementLog.setObject(1, log.getLogId());
            for (int j = 0; j < log.getSqlList().size(); j++) {
                addBatchedStatementLog.setInt(2, j);
                addBatchedStatementLog.setString(3, log.getSqlList().get(j));

                addBatchedStatementLog.addBatch();
            }
            addBatchedStatementLog.executeBatch();
        } catch (final SQLException e) {
            throw new RuntimeException(e);
        }
        lastModificationTime = System.currentTimeMillis();
    }

    @Override
    public synchronized void addConnection(final ConnectionInfo connectionInfo) {
        try {
            final PreparedStatement stmt = connection
                    .prepareStatement("merge into connection_info (connectionId, connectionNumber, url, creationDate) key(connectionId) values (?,?,?,?)");
            stmt.setObject(1, connectionInfo.getUuid());
            stmt.setInt(2, connectionInfo.getConnectionNumber());
            stmt.setString(3, connectionInfo.getUrl());
            stmt.setTimestamp(4, new Timestamp(connectionInfo.getCreationDate().getTime()));

            stmt.execute();
            stmt.close();
        } catch (final SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void clear() {
        try {
            connection.prepareStatement("truncate table batched_statement_log").execute();
            connection.prepareStatement("truncate table statement_log").execute();
        } catch (final SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void getStatements(final @Nullable String filter, final @Nullable Long minDurationNanos,
            final ResultSetAnalyzer analyzer, final boolean withFilledSql) {
        String sql = "select id, tstamp, statementType, rawSql, " //
                + "exec_plus_fetch_time, execution_time, fetch_time, "//
                + "nbRowsIterated, threadName, connectionNumber, error ";
        if (withFilledSql) {
            sql += ", " + FILLED_SQL_COLUMN;
        }
        sql += " from v_statement_log ";
        sql += getWhereClause(filter, minDurationNanos);
        sql += "order by tstamp";

        try {
            @Nonnull
            final PreparedStatement statement = connection.prepareStatement(sql);
            applyParametersForWhereClause(filter, minDurationNanos, statement);
            @Nonnull
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

    @Override
    public void getStatementsGroupByRawSQL(final @Nullable String filter, final @Nullable Long minDurationNanos,
            final ResultSetAnalyzer analyzer) {
        String sql = "select min(id) as ID, statementType, rawSql, count(1) as exec_count, " //
                + "sum(executionDurationNanos) as total_exec_time, "//
                + "max(executionDurationNanos) as max_exec_time, " //
                + "min(executionDurationNanos) as min_exec_time, " //
                + "avg(executionDurationNanos) as avg_exec_time " //
                + "from statement_log ";
        if (filter != null && filter.length() > 0) {
            sql += "where (UPPER(rawSql) like ? or UPPER(filledSql) like ?)";
        }
        sql += "group by statementType, rawSql ";
        if (minDurationNanos != null) {
            sql += "having sum(executionDurationNanos)>=? ";
        }
        sql += "order by total_exec_time desc";

        try {
            @Nonnull
            final PreparedStatement statement = connection.prepareStatement(sql);
            applyParametersForWhereClause(filter, minDurationNanos, statement);
            @Nonnull
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

    @Override
    public void getStatementsGroupByFilledSQL(final @Nullable String filter, final @Nullable Long minDurationNanos,
            final ResultSetAnalyzer analyzer) {
        String sql = "select min(id) as ID, statementType, rawSql, filledSql, count(1) as exec_count, " //
                + "sum(executionDurationNanos) as total_exec_time, "//
                + "max(executionDurationNanos) as max_exec_time, " //
                + "min(executionDurationNanos) as min_exec_time, " //
                + "avg(executionDurationNanos) as avg_exec_time " //
                + "from statement_log ";
        if (filter != null && filter.length() > 0) {
            sql += "where (UPPER(rawSql) like ? or UPPER(filledSql) like ?)";
        }
        sql += "group by statementType, rawSql, filledSql ";
        if (minDurationNanos != null) {
            sql += "having sum(executionDurationNanos)>=?";
        }
        sql += "order by total_exec_time desc";

        try {
            @Nonnull
            final PreparedStatement statement = connection.prepareStatement(sql);
            applyParametersForWhereClause(filter, minDurationNanos, statement);
            @Nonnull
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

    private String getWhereClause(@Nullable final String filter, @Nullable final Long minDurationNanos) {
        String sql = "";
        boolean whereAdded = false;
        if (filter != null) {
            sql += "where (UPPER(rawSql) like ? or UPPER(filledSql) like ?) ";
            whereAdded = true;
        }
        if (minDurationNanos != null) {
            if (!whereAdded) {
                sql += "where ";
                whereAdded = true;
            } else {
                sql += "and ";
            }
            sql += "exec_plus_fetch_time>? ";
        }
        return sql;
    }

    private void applyParametersForWhereClause(@Nullable final String filter, @Nullable final Long minDurationNanos,
            final PreparedStatement statement) throws SQLException {
        if (filter != null) {
            statement.setString(1, "%" + filter.toUpperCase() + "%");
            statement.setString(2, "%" + filter.toUpperCase() + "%");
        }
        if (minDurationNanos != null) {
            statement.setLong(filter != null ? 3 : 1, minDurationNanos.longValue());
        }

    }

    @Override
    public synchronized long getLastModificationTime() {
        return lastModificationTime;
    }

    @Nullable
    @Override
    public DetailedViewStatementLog getStatementLog(final long id) {
        try {
            final PreparedStatement statement = connection
                    .prepareStatement("select statement_log.id, statement_log.tstamp, statement_log.statementType, "//
                            + "statement_log.rawSql, statement_log.filledSql, " //
                            + "statement_log.executionDurationNanos, statement_log.threadName, statement_log.exception, "//
                            + "statement_log.connectionId,"//
                            + "connection_info.connectionNumber, connection_info.url, connection_info.creationDate "//
                            + "from statement_log join connection_info on (statement_log.connectionId=connection_info.connectionId) "//
                            + "where statement_log.id=?");
            statement.setLong(1, id);
            try {
                final ResultSet resultSet = statement.executeQuery();
                DetailedViewStatementLog result = null;
                if (resultSet.next()) {
                    int i = 1;
                    final long keyId = resultSet.getLong(i++);
                    final Timestamp tstamp = resultSet.getTimestamp(i++);
                    final StatementType statementType = StatementType.fromId(resultSet.getInt(i++));
                    @Nonnull
                    final String rawSql = resultSet.getString(i++);
                    @Nonnull
                    final String filledSql = resultSet.getString(i++);
                    final long durationNanos = resultSet.getLong(i++);
                    @Nonnull
                    final String threadName = resultSet.getString(i++);
                    final SQLException exception = (SQLException) resultSet.getObject(i++);
                    final UUID connectionId = (UUID) resultSet.getObject(i++);
                    final int connectionNumber = resultSet.getInt(i++);
                    final String connectionUrl = resultSet.getString(i++);
                    final Timestamp creationDate = resultSet.getTimestamp(i++);

                    final ConnectionInfo connectionInfo = new ConnectionInfo(connectionId, connectionNumber,
                            connectionUrl, creationDate);

                    result = new DetailedViewStatementLog(keyId, connectionInfo, tstamp.getTime(), statementType,
                            rawSql, filledSql, threadName, durationNanos, exception);
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

    @Override
    public int countStatements() {
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

    @Override
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

    @Override
    public long getTotalExecAndFetchTimeNanos(final @Nullable String filter, final @Nullable Long minDurationNanos) {
        String sql = "select sum(exec_plus_fetch_time) from v_statement_log ";
        sql += getWhereClause(filter, minDurationNanos);

        try {
            @Nonnull
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

    @Override
    public void getBatchStatementExecutions(final long keyId, final ResultSetAnalyzer analyzer) {
        String sql = "select batched_stmt_order, filledSql from batched_statement_log where id=? ";
        sql += "order by batched_stmt_order";

        try {
            final PreparedStatement statement = connection.prepareStatement(sql);
            statement.setLong(1, keyId);
            @Nonnull
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

    public void deleteOldRowsIfTooMany() {
        try {
            // first select the oldest timestamp to keep, then use it in the delete clause
            // using 2 queries is actually much faster with H2 than a single delete with a subquery
            LOGGER.debug("searching for most recent timestamp of log statements to delete");
            final PreparedStatement selectTstampStmt = connection
                    .prepareStatement("select tstamp from statement_log order by tstamp desc limit 1 offset "
                            + NB_ROWS_MAX);
            final ResultSet tstampResultSet = selectTstampStmt.executeQuery();
            if (tstampResultSet.next()) {
                final Timestamp timestamp = tstampResultSet.getTimestamp(1);
                if (timestamp != null) {
                    int nbRowsDeleted;
                    {
                        LOGGER.debug("Will delete all log statements earlier than {}", timestamp);
                        final PreparedStatement deleteStmt = connection
                                .prepareStatement("delete from statement_log where tstamp <= ?");
                        deleteStmt.setTimestamp(1, timestamp);
                        nbRowsDeleted = deleteStmt.executeUpdate();
                        LOGGER.debug("Deleted {} old statements", nbRowsDeleted);
                        deleteStmt.close();
                    }
                    if (nbRowsDeleted > 0) {
                        final Statement deleteStmt = connection.createStatement();
                        nbRowsDeleted = deleteStmt
                                .executeUpdate("delete from batched_statement_log where logId not in "//
                                        + "(select logId from statement_log)");
                        // nbRowsDeleted = deleteStmt2.executeUpdate();
                        LOGGER.debug("Deleted {} old batched_statements", nbRowsDeleted);
                        deleteStmt.close();

                        lastModificationTime = System.currentTimeMillis();
                    }
                }
            }
            tstampResultSet.close();
            selectTstampStmt.close();
        } catch (final SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private void checkSchemaVersion(final Connection conn) throws SQLException {
        final Statement statement = conn.createStatement();
        statement.execute("create table if not exists schema_version (version int not null)");

        int version = -1;
        final ResultSet rset = statement.executeQuery("select version from schema_version");
        if (rset.next()) {
            version = rset.getInt(1);
        }
        rset.close();

        if (version != SCHEMA_VERSION) {
            LOGGER.warn("Schema version changed, dropping all objects and recreating tables");
            statement.execute("drop all objects");
            statement.execute("create table if not exists schema_version (version int not null)");
            statement.execute("merge into schema_version key(version) values (" + SCHEMA_VERSION + ")");
        }
        statement.close();
    }

    private class CleanupTask extends TimerTask {

        @Override
        public void run() {
            deleteOldRowsIfTooMany();
        }

    }
}
