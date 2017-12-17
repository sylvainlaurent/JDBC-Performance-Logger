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
import java.sql.Types;
import java.util.Collection;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import org.eclipse.jdt.annotation.Nullable;
import org.h2.Driver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.sla.jdbcperflogger.StatementType;
import ch.sla.jdbcperflogger.model.BatchedNonPreparedStatementsLog;
import ch.sla.jdbcperflogger.model.BatchedPreparedStatementsLog;
import ch.sla.jdbcperflogger.model.ConnectionInfo;
import ch.sla.jdbcperflogger.model.ResultSetLog;
import ch.sla.jdbcperflogger.model.StatementExecutedLog;
import ch.sla.jdbcperflogger.model.StatementLog;
import ch.sla.jdbcperflogger.model.TxCompleteLog;

public class LogRepositoryUpdateJdbc implements LogRepositoryUpdate {
    // TODO ajouter colonne clientId (processId)
    public static final int SCHEMA_VERSION = 7;

    static final int NB_ROWS_MAX = Integer.parseInt(System.getProperty("maxLoggedStatements", "20000"));
    private static final long CLEAN_UP_PERIOD_MS = TimeUnit.SECONDS.toMillis(30);

    private static final Logger LOGGER = LoggerFactory.getLogger(LogRepositoryUpdateJdbc.class);
    // visible for testing
    final Connection connectionUpdate;
    private final PreparedStatement addStatementLog;
    private final PreparedStatement updateStatementLogWithResultSet;
    private final PreparedStatement updateStatementLogAfterExecution;
    private final PreparedStatement addStatementLogWithAfterExecutionInfo;
    private final PreparedStatement addBatchedStatementLog;
    private final PreparedStatement addTxCompletionLog;
    private long lastModificationTime = System.currentTimeMillis();
    private final Timer cleanupTimer;
    private final String dbName;
    @Nullable
    private Long lastLostMessageTime;

    public LogRepositoryUpdateJdbc(final String name) {
        try {
            dbName = getDbPath(name);
            connectionUpdate = createDbConnection(dbName);
            try (Statement stmt = connectionUpdate.createStatement()) {
                stmt.execute("runscript from 'classpath:initdb.sql' charset 'UTF-8'");
            }

            cleanOldConnectionInfo(connectionUpdate);

            addStatementLog = connectionUpdate
                    .prepareStatement("insert into statement_log (logId, tstamp, statementType, rawSql, filledSql, " //
                            + "threadName, connectionId, timeout, autoCommit, transaction_Isolation)"//
                            + " values(?, ?, ?, ?, ?, ?, ?, ?, ?, ?)");
            addStatementLogWithAfterExecutionInfo = connectionUpdate
                    .prepareStatement("insert into statement_log (logId, tstamp, statementType, rawSql, filledSql, " //
                            + "threadName, connectionId, timeout, autoCommit, transaction_Isolation, executionDurationNanos, nbRows, " //
                            + "fetchDurationNanos, rsetUsageDurationNanos, exception)"//
                            + " values(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)");
            updateStatementLogWithResultSet = connectionUpdate.prepareStatement(
                    "update statement_log set fetchDurationNanos=?, rsetUsageDurationNanos=?, nbRows=? where logId=?");
            updateStatementLogAfterExecution = connectionUpdate.prepareStatement(
                    "update statement_log set executionDurationNanos=?, nbRows=?, exception=? where logId=?");

            addBatchedStatementLog = connectionUpdate.prepareStatement(
                    "insert into batched_statement_log (logId, batched_stmt_order, filledSql)" + " values(?, ?, ?)");

            addTxCompletionLog = connectionUpdate.prepareStatement(
                    "insert into statement_log (logId, tstamp, statementType, rawSql, filledSql, executionDurationNanos, "//
                            + "threadName, connectionId) "//
                            + "values (?,?,?,?,?,?,?,?)");

            cleanupTimer = new Timer(true);
            cleanupTimer.schedule(new CleanupTask(), CLEAN_UP_PERIOD_MS, CLEAN_UP_PERIOD_MS);

        } catch (final SQLException e) {
            throw new RuntimeException(e);
        }
    }

    static String getDbPath(final String name) {
        return "logdb/logrepository_" + name;
    }

    static Connection createDbConnection(final String dbName) throws SQLException {
        return createDbConnection(dbName, false);
    }

    private static Connection createDbConnection(final String path, final boolean inRecursion) throws SQLException {
        Driver.class.getClass();
        LOGGER.debug("Opening H2 connection for log repository " + path);
        Connection conn = null;
        try {
            conn = DriverManager.getConnection("jdbc:h2:mem:" + path);
            LOGGER.debug("connection commit mode auto={}", conn.getAutoCommit());
            conn.setAutoCommit(true);
            checkSchemaVersion(conn);

            return conn;
        } catch (final SQLException exc) {
            if (conn != null) {
                conn.close();
            }
            if (inRecursion) {
                throw exc;
            }
            LOGGER.warn("Unexpected error while opening DB connection, will delete DB files and try agaoin", exc);

            deleteDbFiles(path);
            return createDbConnection(path, true);
        }
    }

    private static void deleteDbFiles(final String path) {
        final File dbFile = new File(path + ".h2.db");
        dbFile.delete();
        final File dbTraceFile = new File(path + ".trace.db");
        dbTraceFile.delete();
    }

    @Override
    public void dispose() {
        LOGGER.debug("closing H2 connection for log repository " + dbName);
        cleanupTimer.cancel();
        try {
            addStatementLog.close();
            updateStatementLogWithResultSet.close();
            addBatchedStatementLog.close();
            connectionUpdate.close();
        } catch (final SQLException e) {
            LOGGER.error("error while closing the connection", e);
            // swallow, nothing we can do
        }
    }

    private void cleanOldConnectionInfo(final Connection conn) throws SQLException {
        try (Statement statement = conn.createStatement()) {
            final int nbRowsDeleted = statement.executeUpdate("delete from connection_info where not exists "
                    + "(select 1 from statement_log where statement_log.connectionId=connection_info.connectionId)");
            LOGGER.debug("Deleted {} rows in connection_info", nbRowsDeleted);
        }
    }

    @Override
    public synchronized void addStatementLog(final StatementLog log) {
        LOGGER.debug("addStatementLog:{}", log);
        try {
            int i = 1;
            addStatementLog.setObject(i++, log.getLogId());
            addStatementLog.setTimestamp(i++, new Timestamp(log.getTimestamp()));
            addStatementLog.setInt(i++, log.getStatementType().getId());
            addStatementLog.setString(i++, log.getRawSql());
            addStatementLog.setString(i++, log.getFilledSql());
            addStatementLog.setString(i++, log.getThreadName());
            addStatementLog.setObject(i++, log.getConnectionUuid());
            addStatementLog.setInt(i++, log.getTimeout());
            addStatementLog.setBoolean(i++, log.isAutoCommit());
            addStatementLog.setInt(i++, log.getTransactionIsolation());
            final int insertCount = addStatementLog.executeUpdate();
            assert insertCount == 1;
        } catch (final SQLException e) {
            throw new RuntimeException(e);
        }
        lastModificationTime = System.currentTimeMillis();
    }

    @Override
    public synchronized void addStatementFullyExecutedLog(final Collection<StatementFullyExecutedLog> logs) {
        LOGGER.debug("addStatementLogWithAfterExecutionInfo");
        try {
            for (final StatementFullyExecutedLog log : logs) {
                int i = 1;
                addStatementLogWithAfterExecutionInfo.setObject(i++, log.getLogId());
                addStatementLogWithAfterExecutionInfo.setTimestamp(i++, new Timestamp(log.getTimestamp()));
                addStatementLogWithAfterExecutionInfo.setInt(i++, log.getStatementType().getId());
                addStatementLogWithAfterExecutionInfo.setString(i++, log.getRawSql());
                addStatementLogWithAfterExecutionInfo.setString(i++, log.getFilledSql());
                addStatementLogWithAfterExecutionInfo.setString(i++, log.getThreadName());
                addStatementLogWithAfterExecutionInfo.setObject(i++, log.getConnectionUuid());
                addStatementLogWithAfterExecutionInfo.setInt(i++, log.getTimeout());
                addStatementLogWithAfterExecutionInfo.setBoolean(i++, log.isAutoCommit());
                addStatementLogWithAfterExecutionInfo.setInt(i++, log.getTransactionIsolation());
                addStatementLogWithAfterExecutionInfo.setLong(i++, log.getExecutionTimeNanos());
                addStatementLogWithAfterExecutionInfo.setObject(i++, log.getNbRowsIterated(), Types.INTEGER);
                addStatementLogWithAfterExecutionInfo.setObject(i++, log.getFetchDurationNanos(), Types.BIGINT);
                addStatementLogWithAfterExecutionInfo.setObject(i++, log.getResultSetUsageDurationNanos(),
                        Types.BIGINT);
                addStatementLogWithAfterExecutionInfo.setString(i++, log.getSqlException());
                addStatementLogWithAfterExecutionInfo.addBatch();
            }
            addStatementLogWithAfterExecutionInfo.executeBatch();
        } catch (final SQLException e) {
            throw new RuntimeException(e);
        }
        lastModificationTime = System.currentTimeMillis();
    }

    @Override
    public synchronized void updateLogAfterExecution(final StatementExecutedLog log) {
        LOGGER.debug("updateLogAfterExecution:{}", log);
        try {
            int i = 1;
            updateStatementLogAfterExecution.setLong(i++, log.getExecutionTimeNanos());
            @Nullable
            final Long updateCount = log.getUpdateCount();
            if (updateCount != null) {
                updateStatementLogAfterExecution.setLong(i++, updateCount);
            } else {
                updateStatementLogAfterExecution.setNull(i++, Types.BIGINT);
            }
            updateStatementLogAfterExecution.setString(i++, log.getSqlException());
            updateStatementLogAfterExecution.setObject(i++, log.getLogId());
            updateStatementLogAfterExecution.executeUpdate();
        } catch (final SQLException e) {
            throw new RuntimeException(e);
        }
        lastModificationTime = System.currentTimeMillis();
    }

    @Override
    public synchronized void updateLogWithResultSetLog(final ResultSetLog log) {
        LOGGER.debug("updateLogWithResultSetLog:{}", log);
        try {
            int i = 1;
            updateStatementLogWithResultSet.setLong(i++, log.getFetchDurationNanos());
            updateStatementLogWithResultSet.setLong(i++, log.getResultSetUsageDurationNanos());
            updateStatementLogWithResultSet.setInt(i++, log.getNbRowsIterated());
            updateStatementLogWithResultSet.setObject(i++, log.getLogId());
            updateStatementLogWithResultSet.execute();
        } catch (final SQLException e) {
            throw new RuntimeException(e);
        }
        lastModificationTime = System.currentTimeMillis();
    }

    @Override
    public synchronized void addBatchedPreparedStatementsLog(final BatchedPreparedStatementsLog log) {
        LOGGER.debug("addBatchedPreparedStatementsLog:{}", log);
        try {
            int i = 1;
            addStatementLog.setObject(i++, log.getLogId());
            addStatementLog.setTimestamp(i++, new Timestamp(log.getTimestamp()));
            addStatementLog.setInt(i++, log.getStatementType().getId());
            addStatementLog.setString(i++, log.getRawSql());
            addStatementLog.setString(i++, "(" + log.getSqlList().size() + " batched statements, click for details)");
            addStatementLog.setString(i++, log.getThreadName());
            addStatementLog.setObject(i++, log.getConnectionUuid());
            addStatementLog.setInt(i++, log.getTimeout());
            addStatementLog.setBoolean(i++, log.isAutoCommit());
            addStatementLog.setInt(i++, log.getTransactionIsolation());
            addStatementLog.executeUpdate();

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
        LOGGER.debug("addBatchedNonPreparedStatementsLog:{}", log);
        try {
            int i = 1;
            addStatementLog.setObject(i++, log.getLogId());
            addStatementLog.setTimestamp(i++, new Timestamp(log.getTimestamp()));
            addStatementLog.setInt(i++, log.getStatementType().getId());
            addStatementLog.setString(i++, "(" + log.getSqlList().size() + " batched statements, click for details)");
            addStatementLog.setString(i++, "(click for details)");
            addStatementLog.setString(i++, log.getThreadName());
            addStatementLog.setObject(i++, log.getConnectionUuid());
            addStatementLog.setInt(i++, log.getTimeout());
            addStatementLog.setBoolean(i++, log.isAutoCommit());
            addStatementLog.setInt(i++, log.getTransactionIsolation());
            addStatementLog.executeUpdate();

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
        LOGGER.debug("addConnection:{}", connectionInfo);
        try (PreparedStatement stmt = connectionUpdate
                .prepareStatement("merge into connection_info (connectionId, connectionNumber, url, creationDate, "//
                        + "connectionCreationDurationNanos, connectionProperties)"//
                        + " key(connectionId) values (?,?,?,?,?,?)")) {
            int i = 1;
            stmt.setObject(i++, connectionInfo.getUuid());
            stmt.setInt(i++, connectionInfo.getConnectionNumber());
            stmt.setString(i++, connectionInfo.getUrl());
            stmt.setTimestamp(i++, new Timestamp(connectionInfo.getCreationDate().getTime()));
            stmt.setLong(i++, connectionInfo.getConnectionCreationDuration());
            stmt.setObject(i++, connectionInfo.getConnectionProperties());

            stmt.execute();
        } catch (final SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public synchronized void addTxCompletionLog(final TxCompleteLog log) {
        LOGGER.debug("addTxCompletionLog:{}", log);
        try {
            int i = 1;
            addTxCompletionLog.setObject(i++, UUID.randomUUID());
            addTxCompletionLog.setTimestamp(i++, new Timestamp(log.getTimestamp()));
            addTxCompletionLog.setInt(i++, StatementType.TRANSACTION.getId());
            String rawSql = log.getCompletionType().name();
            if (log.getSavePointDescription() != null) {
                rawSql += " " + log.getSavePointDescription();
            }
            addTxCompletionLog.setString(i++, rawSql);
            addTxCompletionLog.setString(i++, "/*" + rawSql + "*/");
            addTxCompletionLog.setLong(i++, log.getExecutionTimeNanos());
            addTxCompletionLog.setString(i++, log.getThreadName());
            addTxCompletionLog.setObject(i++, log.getConnectionUuid());
            addTxCompletionLog.execute();
        } catch (final SQLException e) {
            throw new RuntimeException(e);
        }
        lastModificationTime = System.currentTimeMillis();
    }

    @Override
    public void clear() {
        try (Statement statement = connectionUpdate.createStatement()) {
            statement.execute("truncate table batched_statement_log");
            statement.execute("truncate table statement_log");
        } catch (final SQLException e) {
            throw new RuntimeException(e);
        }
        lastLostMessageTime = null;
        lastModificationTime = System.currentTimeMillis();
    }

    @Override
    public void deleteStatementLog(final long... logIds) {
        try (PreparedStatement statement = connectionUpdate
                .prepareStatement("delete from statement_log where statement_log.id=?")) {

            for (int i = 0; i < logIds.length; i++) {
                final long logId = logIds[i];
                statement.setLong(1, logId);
                statement.addBatch();
                if (i % 100 == 0) {
                    statement.executeBatch();
                }
            }
            statement.executeBatch();
        } catch (final SQLException e) {
            throw new RuntimeException(e);
        }
        lastModificationTime = System.currentTimeMillis();
    }

    public void deleteOldRowsIfTooMany() {
        // first select the oldest timestamp to keep, then use it in the delete clause
        // using 2 queries is actually much faster with H2 than a single delete with a subquery
        final long startTime = System.currentTimeMillis();
        LOGGER.debug("searching for most recent timestamp of log statements to delete");

        try (Connection connection = createDbConnection(dbName)) {

            try (PreparedStatement selectTstampStmt = connection.prepareStatement(
                    "select tstamp from statement_log order by tstamp desc limit 1 offset " + NB_ROWS_MAX);
                    ResultSet tstampResultSet = selectTstampStmt.executeQuery()) {
                if (tstampResultSet.next()) {
                    final Timestamp timestamp = tstampResultSet.getTimestamp(1);
                    int nbRowsDeleted;
                    {
                        LOGGER.debug("Will delete all log statements earlier than {}", timestamp);
                        try (PreparedStatement deleteStmt = connection
                                .prepareStatement("delete from statement_log where tstamp <= ?")) {
                            deleteStmt.setTimestamp(1, timestamp);
                            nbRowsDeleted = deleteStmt.executeUpdate();
                            LOGGER.debug("Deleted {} old statements", nbRowsDeleted);
                        }
                    }
                    if (nbRowsDeleted > 0) {
                        try (Statement deleteStmt = connection.createStatement()) {
                            nbRowsDeleted = deleteStmt
                                    .executeUpdate("delete from batched_statement_log where logId not in "//
                                            + "(select logId from statement_log)");
                            // nbRowsDeleted = deleteStmt2.executeUpdate();
                            LOGGER.debug("Deleted {} old batched_statements", nbRowsDeleted);
                            lastModificationTime = System.currentTimeMillis();
                        }
                    }

                    // clear flag about lost statements if they have been purged
                    final Long lastLostMessageTime2 = lastLostMessageTime;
                    if (lastLostMessageTime2 != null && lastLostMessageTime2 < timestamp.getTime()) {
                        setLastLostMessageTime(null);
                    }
                }
            }
            LOGGER.debug("Peformed deleteOldRowsIfTooMany in {}ms", System.currentTimeMillis() - startTime);
        } catch (final SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public synchronized long getLastModificationTime() {
        return lastModificationTime;
    }

    @Override
    public void setLastLostMessageTime(@Nullable final Long timestamp) {
        lastLostMessageTime = timestamp;
        lastModificationTime = System.currentTimeMillis();
    }

    @Override
    @Nullable
    public Long getLastLostMessageTime() {
        return lastLostMessageTime;
    }

    private static void checkSchemaVersion(final Connection conn) throws SQLException {
        try (Statement statement = conn.createStatement()) {
            statement.execute("create table if not exists schema_version (version int not null)");

            int version = -1;
            try (ResultSet rset = statement.executeQuery("select version from schema_version")) {
                if (rset.next()) {
                    version = rset.getInt(1);
                }
            }

            if (version != SCHEMA_VERSION) {
                LOGGER.warn("Schema version changed, dropping all objects and recreating tables");
                statement.execute("drop all objects");
                statement.execute("create table if not exists schema_version (version int not null)");
                statement.execute("merge into schema_version key(version) values (" + SCHEMA_VERSION + ")");
            }
        }
    }

    private class CleanupTask extends TimerTask {

        @Override
        public void run() {
            deleteOldRowsIfTooMany();
        }

    }

}
