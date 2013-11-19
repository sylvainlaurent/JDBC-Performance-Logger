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

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.UUID;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.sla.jdbcperflogger.StatementType;
import ch.sla.jdbcperflogger.logger.ConnectionInfo;

public class LogRepositoryReadJdbc implements LogRepositoryRead {

    private static final Logger LOGGER = LoggerFactory.getLogger(LogRepositoryReadJdbc.class);

    private final Connection connectionRead;

    public LogRepositoryReadJdbc(final String dbName) {
        try {
            connectionRead = LogRepositoryUpdateJdbc.createDbConnection(LogRepositoryUpdateJdbc.getDbPath(dbName));
        } catch (final SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void dispose() {
        try {
            connectionRead.close();
        } catch (final SQLException e) {
            LOGGER.error("error while closing the connection", e);
            // swallow, nothing we can do
        }
    }

    @Override
    public void getStatements(final LogSearchCriteria searchCriteria, final ResultSetAnalyzer analyzer,
            final boolean withFilledSql) {
        String sql = "select id, tstamp, statementType, rawSql, " //
                + "exec_plus_fetch_time, execution_time, fetch_time, "//
                + "nbRowsIterated, threadName, connectionNumber, error ";
        if (withFilledSql) {
            sql += ", " + LogRepositoryConstants.FILLED_SQL_COLUMN;
        }
        sql += " from v_statement_log ";
        sql += getWhereClause(searchCriteria);
        sql += "order by tstamp";

        try {
            @Nonnull
            final PreparedStatement statement = connectionRead.prepareStatement(sql);
            applyParametersForWhereClause(searchCriteria, statement);
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
    public void getStatementsGroupByRawSQL(final LogSearchCriteria searchCriteria, final ResultSetAnalyzer analyzer) {
        String sql = "select min(id) as ID, statementType, rawSql, count(1) as exec_count, " //
                + "sum(executionDurationNanos) as total_exec_time, "//
                + "max(executionDurationNanos) as max_exec_time, " //
                + "min(executionDurationNanos) as min_exec_time, " //
                + "avg(executionDurationNanos) as avg_exec_time " //
                + "from statement_log ";
        if (searchCriteria.getFilter() != null) {
            sql += "where (UPPER(rawSql) like ? or UPPER(filledSql) like ?)";
        }
        sql += "group by statementType, rawSql ";
        if (searchCriteria.getMinDurationNanos() != null) {
            sql += "having sum(executionDurationNanos)>=? ";
        }
        sql += "order by total_exec_time desc";

        try {
            @Nonnull
            final PreparedStatement statement = connectionRead.prepareStatement(sql);
            applyParametersForWhereClause(searchCriteria, statement);
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
    public void getStatementsGroupByFilledSQL(final LogSearchCriteria searchCriteria, final ResultSetAnalyzer analyzer) {
        String sql = "select min(id) as ID, statementType, rawSql, filledSql, count(1) as exec_count, " //
                + "sum(executionDurationNanos) as total_exec_time, "//
                + "max(executionDurationNanos) as max_exec_time, " //
                + "min(executionDurationNanos) as min_exec_time, " //
                + "avg(executionDurationNanos) as avg_exec_time " //
                + "from statement_log ";
        if (searchCriteria.getFilter() != null) {
            sql += "where (UPPER(rawSql) like ? or UPPER(filledSql) like ?)";
        }
        sql += "group by statementType, rawSql, filledSql ";
        if (searchCriteria.getMinDurationNanos() != null) {
            sql += "having sum(executionDurationNanos)>=?";
        }
        sql += "order by total_exec_time desc";

        try {
            @Nonnull
            final PreparedStatement statement = connectionRead.prepareStatement(sql);
            applyParametersForWhereClause(searchCriteria, statement);
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

    private String getWhereClause(final LogSearchCriteria searchCriteria) {
        String sql = "";
        boolean whereAdded = false;
        if (searchCriteria.getFilter() != null) {
            sql += "where (UPPER(rawSql) like ? or UPPER(filledSql) like ?) ";
            whereAdded = true;
        }
        if (searchCriteria.getMinDurationNanos() != null) {
            if (!whereAdded) {
                sql += "where ";
                whereAdded = true;
            } else {
                sql += "and ";
            }
            sql += "exec_plus_fetch_time>? ";
        }
        if (searchCriteria.isRemoveTransactionCompletions()) {
            if (!whereAdded) {
                sql += "where ";
                whereAdded = true;
            } else {
                sql += "and ";
            }
            sql += "statementType<>" + StatementType.TRANSACTION.getId() + " ";
        }
        return sql;
    }

    private void applyParametersForWhereClause(final LogSearchCriteria searchCriteria, final PreparedStatement statement)
            throws SQLException {
        final String filter = searchCriteria.getFilter();
        int i = 1;
        if (filter != null) {
            statement.setString(i++, "%" + filter.toUpperCase() + "%");
            statement.setString(i++, "%" + filter.toUpperCase() + "%");
        }
        final Long minDurationNanos = searchCriteria.getMinDurationNanos();
        if (minDurationNanos != null) {
            statement.setLong(i++, minDurationNanos.longValue());
        }
    }

    @Nullable
    @Override
    public DetailedViewStatementLog getStatementLog(final long id) {
        try {
            final PreparedStatement statement = connectionRead
                    .prepareStatement("select statement_log.logId, statement_log.tstamp, statement_log.statementType, "//
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
                    final UUID logId = (UUID) resultSet.getObject(i++);
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

                    result = new DetailedViewStatementLog(logId, connectionInfo, tstamp.getTime(), statementType,
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
            final PreparedStatement statement = connectionRead.prepareStatement("select count(1) from statement_log");
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
            final PreparedStatement statement = connectionRead
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
    public long getTotalExecAndFetchTimeNanos(final LogSearchCriteria searchCriteria) {
        String sql = "select sum(exec_plus_fetch_time) from v_statement_log ";
        sql += getWhereClause(searchCriteria);

        try {
            @Nonnull
            final PreparedStatement statement = connectionRead.prepareStatement(sql);
            applyParametersForWhereClause(searchCriteria, statement);
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
    public void getBatchStatementExecutions(final UUID logId, final ResultSetAnalyzer analyzer) {
        String sql = "select batched_stmt_order, filledSql from batched_statement_log where logId=? ";
        sql += "order by batched_stmt_order";

        try {
            final PreparedStatement statement = connectionRead.prepareStatement(sql);
            statement.setObject(1, logId);
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

}
