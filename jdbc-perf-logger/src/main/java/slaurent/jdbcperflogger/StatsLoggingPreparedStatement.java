package slaurent.jdbcperflogger;

import java.math.BigDecimal;
import java.net.URL;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Time;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.UUID;

public class StatsLoggingPreparedStatement extends StatsLoggingStatement implements PreparedStatement {
    private final String preparedSql;
    private final PreparedStatementValuesHolder paramValues = new PreparedStatementValuesHolder();
    private final List<Object> batchedPreparedOrNonPreparedStmtExecutions = new ArrayList<Object>();

    public StatsLoggingPreparedStatement(final PreparedStatement wrappedStatement, final String sql,
            final StatsLoggingConnection wrappingConnection) {
        super(wrappedStatement, wrappingConnection);
        preparedSql = sql;
    }

    private <V> V executeAndLogPstmtPerf(final SqlCallable<V> callable) throws SQLException {
        final long start = System.nanoTime();
        SQLException exc = null;
        try {
            return callable.call();
        } catch (final SQLException e) {
            exc = e;
            throw e;
        } finally {
            final long end = System.nanoTime();
            PerfLogger.logPreparedStatement(UUID.randomUUID(), preparedSql, paramValues, end - start,
                    StatementType.BASE_PREPARED_STMT, wrappingConnection.databaseType, exc);
        }
    }

    @Override
    public ResultSet executeQuery() throws SQLException {
        final UUID logId = UUID.randomUUID();
        final long start = System.nanoTime();
        SQLException exc = null;
        try {
            final ResultSet resultSet = super.executeQuery();
            return new StatsLoggingResultSet(resultSet, logId, StatementType.PREPARED_QUERY_STMT);
        } catch (final SQLException e) {
            exc = e;
            throw e;
        } finally {
            final long end = System.nanoTime();
            PerfLogger.logPreparedStatement(logId, preparedSql, paramValues, end - start,
                    StatementType.PREPARED_QUERY_STMT, wrappingConnection.databaseType, exc);
        }

    }

    @Override
    public int executeUpdate() throws SQLException {
        return executeAndLogPstmtPerf(new SqlCallable<Integer>() {

            public Integer call() throws SQLException {
                return StatsLoggingPreparedStatement.super.executeUpdate();
            }
        });
    }

    @Override
    public boolean execute() throws SQLException {
        return executeAndLogPstmtPerf(new SqlCallable<Boolean>() {

            public Boolean call() throws SQLException {
                return StatsLoggingPreparedStatement.super.execute();
            }
        });
    }

    @Override
    public void addBatch() throws SQLException {
        super.addBatch();
        batchedPreparedOrNonPreparedStmtExecutions.add(paramValues.copy());
    }

    @Override
    public void clearBatch() throws SQLException {
        super.clearBatch();
        batchedPreparedOrNonPreparedStmtExecutions.clear();
    }

    @Override
    public int[] executeBatch() throws SQLException {
        final long start = System.nanoTime();
        SQLException exc = null;
        try {
            return super.executeBatch_internal();
        } catch (final SQLException e) {
            exc = e;
            throw e;
        } finally {
            final long end = System.nanoTime();
            PerfLogger.logPreparedBatchedStatements(preparedSql, batchedPreparedOrNonPreparedStmtExecutions, end
                    - start, wrappingConnection.databaseType, exc);
            batchedPreparedOrNonPreparedStmtExecutions.clear();
        }

    }

    @Override
    public void clearParameters() throws SQLException {
        super.clearParameters();
        paramValues.clear();
    }

    @Override
    public void setNull(final int parameterIndex, final int sqlType) throws SQLException {
        super.setNull(parameterIndex, sqlType);
        paramValues.put(parameterIndex, new SqlTypedValue(null, sqlType));
    }

    @Override
    public void setByte(final int parameterIndex, final byte x) throws SQLException {
        super.setByte(parameterIndex, x);
        paramValues.put(parameterIndex, new SqlTypedValue(x, Types.TINYINT));
    }

    @Override
    public void setShort(final int parameterIndex, final short x) throws SQLException {
        super.setShort(parameterIndex, x);
        paramValues.put(parameterIndex, new SqlTypedValue(x, Types.SMALLINT));
    }

    @Override
    public void setInt(final int parameterIndex, final int x) throws SQLException {
        super.setInt(parameterIndex, x);
        paramValues.put(parameterIndex, new SqlTypedValue(x, Types.INTEGER));
    }

    @Override
    public void setLong(final int parameterIndex, final long x) throws SQLException {
        super.setLong(parameterIndex, x);
        paramValues.put(parameterIndex, new SqlTypedValue(x, Types.BIGINT));
    }

    @Override
    public void setFloat(final int parameterIndex, final float x) throws SQLException {
        super.setFloat(parameterIndex, x);
        paramValues.put(parameterIndex, new SqlTypedValue(x, Types.REAL));
    }

    @Override
    public void setDouble(final int parameterIndex, final double x) throws SQLException {
        super.setDouble(parameterIndex, x);
        paramValues.put(parameterIndex, new SqlTypedValue(x, Types.DOUBLE));
    }

    @Override
    public void setBigDecimal(final int parameterIndex, final BigDecimal x) throws SQLException {
        super.setBigDecimal(parameterIndex, x);
        paramValues.put(parameterIndex, new SqlTypedValue(x, Types.NUMERIC));
    }

    @Override
    public void setString(final int parameterIndex, final String x) throws SQLException {
        super.setString(parameterIndex, x);
        paramValues.put(parameterIndex, new SqlTypedValue(x, Types.VARCHAR));
    }

    @Override
    public void setDate(final int parameterIndex, final Date x) throws SQLException {
        super.setDate(parameterIndex, x);
        paramValues.put(parameterIndex, new SqlTypedValue(x, Types.DATE));
    }

    @Override
    public void setTime(final int parameterIndex, final Time x) throws SQLException {
        super.setTime(parameterIndex, x);
        paramValues.put(parameterIndex, new SqlTypedValue(x, Types.TIME));
    }

    @Override
    public void setTimestamp(final int parameterIndex, final Timestamp x) throws SQLException {
        super.setTimestamp(parameterIndex, x);
        paramValues.put(parameterIndex, new SqlTypedValue(x, Types.TIMESTAMP));
    }

    @Override
    public void setObject(final int parameterIndex, final Object x, final int targetSqlType) throws SQLException {
        super.setObject(parameterIndex, x, targetSqlType);
        paramValues.put(parameterIndex, new SqlTypedValue(x, targetSqlType));
    }

    @Override
    public void setObject(final int parameterIndex, final Object x) throws SQLException {
        super.setObject(parameterIndex, x);
        paramValues.put(parameterIndex, new SqlTypedValue(x, getSqlTypeForObject(x)));
    }

    @Override
    public void setDate(final int parameterIndex, final Date x, final Calendar cal) throws SQLException {
        super.setDate(parameterIndex, x, cal);
        paramValues.put(parameterIndex, new SqlTypedValueWithCalendar(x, cal, Types.DATE));
    }

    @Override
    public void setTime(final int parameterIndex, final Time x, final Calendar cal) throws SQLException {
        super.setTime(parameterIndex, x, cal);
        paramValues.put(parameterIndex, new SqlTypedValueWithCalendar(x, cal, Types.TIME));
    }

    @Override
    public void setTimestamp(final int parameterIndex, final Timestamp x, final Calendar cal) throws SQLException {
        super.setTimestamp(parameterIndex, x, cal);
        paramValues.put(parameterIndex, new SqlTypedValueWithCalendar(x, cal, Types.TIMESTAMP));
    }

    @Override
    public void setURL(final int parameterIndex, final URL x) throws SQLException {
        super.setURL(parameterIndex, x);
        paramValues.put(parameterIndex, new SqlTypedValue(x, Types.DATALINK));
    }

    @Override
    public void setNString(final int parameterIndex, final String value) throws SQLException {
        super.setNString(parameterIndex, value);
        paramValues.put(parameterIndex, new SqlTypedValue(value, Types.NVARCHAR));
    }

    private int getSqlTypeForObject(final Object o) {
        if (o == null) {
            return Types.NULL;
        } else if (o instanceof Timestamp) {
            return Types.TIMESTAMP;
        } else if (o instanceof java.sql.Date) {
            return Types.DATE;
        } else if (o instanceof java.sql.Time) {
            return Types.TIME;
        } else if (o instanceof java.util.Date) {
            return Types.TIMESTAMP;
        } else {
            // TODO completer types SQL
            return Types.OTHER;
        }
    }
}
