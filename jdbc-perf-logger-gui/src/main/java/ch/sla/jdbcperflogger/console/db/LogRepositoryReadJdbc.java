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

import static ch.sla.jdbcperflogger.console.db.LogRepositoryConstants.AVG_EXEC_PLUS_RSET_USAGE_TIME_COLUMN;
import static ch.sla.jdbcperflogger.console.db.LogRepositoryConstants.MAX_EXEC_PLUS_RSET_USAGE_TIME_COLUMN;
import static ch.sla.jdbcperflogger.console.db.LogRepositoryConstants.MIN_EXEC_PLUS_RSET_USAGE_TIME_COLUMN;
import static ch.sla.jdbcperflogger.console.db.LogRepositoryConstants.TOTAL_EXEC_PLUS_RSET_USAGE_TIME_COLUMN;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Properties;
import java.util.UUID;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.sla.jdbcperflogger.StatementType;
import ch.sla.jdbcperflogger.model.ConnectionInfo;

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
        final StringBuilder sql = new StringBuilder("select id, tstamp, statementType, rawSql, " //
                + "exec_plus_rset_usage_time, execution_time, rset_usage_time, fetch_time, "//
                + "nbRows, threadName, connectionNumber, timeout, autoCommit, transaction_Isolation, error ");
        if (withFilledSql) {
            sql.append(", " + LogRepositoryConstants.FILLED_SQL_COLUMN);
        }
        sql.append(" from v_statement_log ");
        sql.append(getWhereClause(searchCriteria));
        sql.append(" order by tstamp, id");

        try (PreparedStatement statement = connectionRead.prepareStatement(sql.toString())) {
            applyParametersForWhereClause(searchCriteria, statement);
            try (ResultSet resultSet = statement.executeQuery()) {
                analyzer.analyze(resultSet);
            }

        } catch (final SQLException e) {
            throw new RuntimeException(e);
        }

    }

    @Override
    public void getStatementsGroupByRawSQL(final LogSearchCriteria searchCriteria, final ResultSetAnalyzer analyzer) {
        final StringBuilder sql = new StringBuilder(
                "select * from (select min(id) as ID, statementType, rawSql, count(1) as exec_count, " //
                        + "sum(executionDurationNanos+coalesce(rsetUsageDurationNanos,0)) as "
                        + TOTAL_EXEC_PLUS_RSET_USAGE_TIME_COLUMN + ", "//
                        + "max(executionDurationNanos+coalesce(rsetUsageDurationNanos,0)) as "
                        + MAX_EXEC_PLUS_RSET_USAGE_TIME_COLUMN + ", " //
                        + "min(executionDurationNanos+coalesce(rsetUsageDurationNanos,0)) as "
                        + MIN_EXEC_PLUS_RSET_USAGE_TIME_COLUMN + ", " //
                        + "avg(executionDurationNanos+coalesce(rsetUsageDurationNanos,0)) as "
                        + AVG_EXEC_PLUS_RSET_USAGE_TIME_COLUMN + " " //
                        + "from statement_log ");
        boolean whereAdded = false;

        if (searchCriteria.getFilter() != null) {
            whereAdded = addWhereClause(sql, whereAdded, "(UPPER(rawSql) like ? or UPPER(filledSql) like ?)");
        }
        sql.append("group by statementType, rawSql ");
        if (searchCriteria.getMinDurationNanos() != null) {
            sql.append("having sum(executionDurationNanos++coalesce(rsetUsageDurationNanos,0))>=? ");
        }
        sql.append(") ");
        if (searchCriteria.getSqlPassThroughFilter() != null) {
            addWhereClause(sql, false, searchCriteria.getSqlPassThroughFilter());
        }
        if (searchCriteria.isRemoveTransactionCompletions()) {
            addWhereClause(sql, false, "statementType<>" + StatementType.TRANSACTION.getId());
        }

        sql.append(" order by " + TOTAL_EXEC_PLUS_RSET_USAGE_TIME_COLUMN + " desc");

        try (PreparedStatement statement = connectionRead.prepareStatement(sql.toString())) {
            applyParametersForWhereClause(searchCriteria, statement);
            try (ResultSet resultSet = statement.executeQuery()) {
                analyzer.analyze(resultSet);
            }
        } catch (final SQLException e) {
            throw new RuntimeException(e);
        }

    }

    @Override
    public void getStatementsGroupByFilledSQL(final LogSearchCriteria searchCriteria,
            final ResultSetAnalyzer analyzer) {
        final StringBuilder sql = new StringBuilder(
                "select * from (select min(id) as ID, statementType, rawSql, filledSql, count(1) as exec_count, " //
                        + "sum(executionDurationNanos+coalesce(rsetUsageDurationNanos,0)) as "
                        + TOTAL_EXEC_PLUS_RSET_USAGE_TIME_COLUMN + ", "//
                        + "max(executionDurationNanos+coalesce(rsetUsageDurationNanos,0)) as "
                        + MAX_EXEC_PLUS_RSET_USAGE_TIME_COLUMN + ", " //
                        + "min(executionDurationNanos+coalesce(rsetUsageDurationNanos,0)) as "
                        + MIN_EXEC_PLUS_RSET_USAGE_TIME_COLUMN + ", " //
                        + "avg(executionDurationNanos+coalesce(rsetUsageDurationNanos,0)) as "
                        + AVG_EXEC_PLUS_RSET_USAGE_TIME_COLUMN + " " //
                        + "from statement_log ");
        if (searchCriteria.getFilter() != null) {
            sql.append("where (UPPER(rawSql) like ? or UPPER(filledSql) like ?)");
        }
        sql.append("group by statementType, rawSql, filledSql ");
        if (searchCriteria.getMinDurationNanos() != null) {
            sql.append("having sum(executionDurationNanos++coalesce(rsetUsageDurationNanos,0))>=?");
        }
        sql.append(") ");
        if (searchCriteria.getSqlPassThroughFilter() != null) {
            addWhereClause(sql, false, searchCriteria.getSqlPassThroughFilter());
        }
        if (searchCriteria.isRemoveTransactionCompletions()) {
            addWhereClause(sql, false, "statementType<>" + StatementType.TRANSACTION.getId());
        }
        sql.append(" order by " + TOTAL_EXEC_PLUS_RSET_USAGE_TIME_COLUMN + " desc");

        try (PreparedStatement statement = connectionRead.prepareStatement(sql.toString())) {
            applyParametersForWhereClause(searchCriteria, statement);
            try (ResultSet resultSet = statement.executeQuery()) {
                analyzer.analyze(resultSet);
            }
        } catch (final SQLException e) {
            throw new RuntimeException(e);
        }

    }

    private CharSequence getWhereClause(final LogSearchCriteria searchCriteria) {
        final StringBuilder sql = new StringBuilder(50);
        boolean whereAdded = false;
        if (searchCriteria.getFilter() != null) {
            whereAdded = addWhereClause(sql, whereAdded, "(UPPER(rawSql) like ? or UPPER(filledSql) like ?) ");
        }
        if (searchCriteria.getMinDurationNanos() != null) {
            whereAdded = addWhereClause(sql, whereAdded, "exec_plus_rset_usage_time>? ");
        }
        if (searchCriteria.isRemoveTransactionCompletions()) {
            whereAdded = addWhereClause(sql, whereAdded, "statementType<>" + StatementType.TRANSACTION.getId() + " ");
        }
        if (searchCriteria.getSqlPassThroughFilter() != null) {
            whereAdded = addWhereClause(sql, whereAdded, searchCriteria.getSqlPassThroughFilter());
        }
        return sql;
    }

    private boolean addWhereClause(final StringBuilder buffer, boolean whereAdded, final String clause) {
        if (!whereAdded) {
            buffer.append(" where ");
            whereAdded = true;
        } else {
            buffer.append(" and ");
        }
        buffer.append(clause);
        return whereAdded;
    }

    private void applyParametersForWhereClause(final LogSearchCriteria searchCriteria,
            final PreparedStatement statement) throws SQLException {
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
        final String sql = "select statement_log.logId, statement_log.tstamp, statement_log.statementType, "//
                + "statement_log.rawSql, statement_log.filledSql, " //
                + "statement_log.threadName, statement_log.exception, "//
                + "statement_log.connectionId,"//
                + "connection_info.connectionNumber, connection_info.url, connection_info.creationDate,"//
                + "connection_info.connectionCreationDurationNanos, connection_info.connectionProperties "//
                + "from statement_log join connection_info on (statement_log.connectionId=connection_info.connectionId) "//
                + "where statement_log.id=?";

        try (final PreparedStatement statement = connectionRead.prepareStatement(sql)) {
            statement.setLong(1, id);
            try (final ResultSet resultSet = statement.executeQuery()) {
                DetailedViewStatementLog result = null;
                if (resultSet.next()) {
                    int i = 1;
                    final UUID logId = (UUID) resultSet.getObject(i++);
                    final Timestamp tstamp = resultSet.getTimestamp(i++);
                    final StatementType statementType = StatementType.fromId(resultSet.getInt(i++));
                    @NonNull
                    final String rawSql = resultSet.getString(i++);
                    @NonNull
                    final String filledSql = resultSet.getString(i++);
                    @NonNull
                    final String threadName = resultSet.getString(i++);
                    final String exception = resultSet.getString(i++);
                    final UUID connectionId = (UUID) resultSet.getObject(i++);
                    final int connectionNumber = resultSet.getInt(i++);
                    final String connectionUrl = resultSet.getString(i++);
                    final Timestamp creationDate = resultSet.getTimestamp(i++);
                    final long connectionCreationDurationNanos = resultSet.getLong(i++);
                    final Properties connectionProperties = (Properties) resultSet.getObject(i++);

                    final ConnectionInfo connectionInfo = new ConnectionInfo(connectionId, connectionNumber,
                            connectionUrl, creationDate, connectionCreationDurationNanos, connectionProperties);

                    result = new DetailedViewStatementLog(logId, connectionInfo, tstamp.getTime(), statementType,
                            rawSql, filledSql, threadName, exception);
                }
                return result;
            }
        } catch (final SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public int countStatements() {
        try {
            try (PreparedStatement statement = connectionRead.prepareStatement("select count(1) from statement_log")) {
                try (ResultSet resultSet = statement.executeQuery()) {
                    resultSet.next();
                    return resultSet.getInt(1);
                }
            }
        } catch (final SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public long getTotalExecAndFetchTimeNanos() {
        try (PreparedStatement statement = connectionRead
                .prepareStatement("select sum(exec_plus_rset_usage_time) from v_statement_log")) {
            try (ResultSet resultSet = statement.executeQuery()) {
                resultSet.next();
                return resultSet.getLong(1);
            }
        } catch (final SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public long getTotalExecAndFetchTimeNanos(final LogSearchCriteria searchCriteria) {
        String sql = "select sum(exec_plus_rset_usage_time) from v_statement_log ";
        sql += getWhereClause(searchCriteria);

        try (PreparedStatement statement = connectionRead.prepareStatement(sql)) {
            applyParametersForWhereClause(searchCriteria, statement);
            try (ResultSet resultSet = statement.executeQuery()) {
                resultSet.next();
                return resultSet.getLong(1);
            }
        } catch (final SQLException e) {
            throw new RuntimeException(e);
        }

    }

    @Override
    public void getBatchStatementExecutions(final UUID logId, final ResultSetAnalyzer analyzer) {
        String sql = "select batched_stmt_order, filledSql from batched_statement_log where logId=? ";
        sql += "order by batched_stmt_order";

        try (PreparedStatement statement = connectionRead.prepareStatement(sql)) {
            statement.setObject(1, logId);
            try (ResultSet resultSet = statement.executeQuery()) {
                analyzer.analyze(resultSet);
            }
        } catch (final SQLException e) {
            throw new RuntimeException(e);
        }

    }

}
