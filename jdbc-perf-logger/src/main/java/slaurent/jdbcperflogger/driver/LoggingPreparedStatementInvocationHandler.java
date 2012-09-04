package slaurent.jdbcperflogger.driver;

import java.io.Serializable;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import slaurent.jdbcperflogger.StatementType;
import slaurent.jdbcperflogger.logger.PerfLogger;
import slaurent.jdbcperflogger.model.PreparedStatementValuesHolder;

public class LoggingPreparedStatementInvocationHandler extends LoggingStatementInvocationHandler {
    private static final String CLEAR_PARAMETERS = "clearParameters";

    private final String rawSql;
    private final PreparedStatementValuesHolder paramValues = new PreparedStatementValuesHolder();
    private final List<Object> batchedPreparedOrNonPreparedStmtExecutions = new ArrayList<Object>();

    LoggingPreparedStatementInvocationHandler(final PreparedStatement statement, final String rawSql,
            final DatabaseType databaseType) {
        super(statement, databaseType);
        this.rawSql = rawSql;
    }

    @Override
    public Object invoke(final Object proxy, final Method method, final Object[] args) throws Throwable {
        final Object result;
        final String methodName = method.getName();
        if (args == null || args.length == 0) {
            if (EXECUTE.equals(methodName) || EXECUTE_UPDATE.equals(methodName)) {
                return internalExecutePrepared(method, args);
            } else if (ADD_BATCH.equals(methodName)) {
                result = Utils.invokeUnwrapException(wrappedStatement, method, args);
                batchedPreparedOrNonPreparedStmtExecutions.add(paramValues.copy());
                return result;
            } else if (CLEAR_BATCH.equals(methodName)) {
                result = Utils.invokeUnwrapException(wrappedStatement, method, args);
                batchedPreparedOrNonPreparedStmtExecutions.clear();
                return result;
            } else if (CLEAR_PARAMETERS.equals(methodName)) {
                result = Utils.invokeUnwrapException(wrappedStatement, method, args);
                paramValues.clear();
                return result;
            }
        } else {
            if (methodName.startsWith("set")) {
                result = Utils.invokeUnwrapException(wrappedStatement, method, args);
                if ("setNull".equals(methodName)) {
                    paramValues.put((Serializable) args[0], new SqlTypedValue(null, ((Integer) args[1]).intValue()));
                } else if (args.length == 2 || "setDate".equals(methodName) || "setTime".equals(methodName)
                        || "setTimestamp".equals(methodName)) {
                    paramValues.put((Serializable) args[0], new SqlTypedValue(args[1], methodName));
                } else if ("setObject".equals(methodName)) {
                    paramValues.put((Serializable) args[0], new SqlTypedValue(args[1], ((Integer) args[2]).intValue()));
                }
                return result;
            }
        }
        return super.invoke(proxy, method, args);
    }

    @Override
    protected ResultSet internalExecuteQuery(final Method method, final Object[] args) throws Throwable {
        final UUID logId = UUID.randomUUID();
        final long start = System.nanoTime();
        Throwable exc = null;
        try {
            final ResultSet resultSet = (ResultSet) Utils.invokeUnwrapException(wrappedStatement, method, args);
            return (ResultSet) Proxy.newProxyInstance(LoggingPreparedStatementInvocationHandler.class.getClassLoader(),
                    resultSet.getClass().getInterfaces(), new LoggingResultSetInvocationHandler(resultSet, logId,
                            StatementType.PREPARED_QUERY_STMT));
        } catch (final Throwable e) {
            exc = e;
            throw exc;
        } finally {
            final long end = System.nanoTime();
            PerfLogger.logPreparedStatement(logId, rawSql, paramValues, end - start, StatementType.PREPARED_QUERY_STMT,
                    databaseType, exc);
        }

    }

    protected Object internalExecutePrepared(final Method method, final Object[] args) throws Throwable {
        final long start = System.nanoTime();
        Throwable exc = null;
        try {
            return Utils.invokeUnwrapException(wrappedStatement, method, args);
        } catch (final Throwable e) {
            exc = e;
            throw exc;
        } finally {
            final long end = System.nanoTime();
            PerfLogger.logPreparedStatement(UUID.randomUUID(), rawSql, paramValues, end - start,
                    StatementType.BASE_PREPARED_STMT, databaseType, exc);
        }
    }

    @Override
    protected Object internalExecuteBatch(final Method method, final Object[] args) throws Throwable {
        final long start = System.nanoTime();
        Throwable exc = null;
        try {
            return Utils.invokeUnwrapException(wrappedStatement, method, args);
        } catch (final Throwable e) {
            exc = e;
            throw exc;
        } finally {
            final long end = System.nanoTime();
            PerfLogger.logPreparedBatchedStatements(rawSql, batchedPreparedOrNonPreparedStmtExecutions, end - start,
                    databaseType, exc);
            batchedPreparedOrNonPreparedStmtExecutions.clear();
        }

    }

}
