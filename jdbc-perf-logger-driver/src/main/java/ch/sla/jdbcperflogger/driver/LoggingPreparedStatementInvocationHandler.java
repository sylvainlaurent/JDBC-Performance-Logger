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
package ch.sla.jdbcperflogger.driver;

import java.io.Serializable;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLType;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.eclipse.jdt.annotation.Nullable;

import ch.sla.jdbcperflogger.DatabaseType;
import ch.sla.jdbcperflogger.StatementType;
import ch.sla.jdbcperflogger.logger.PerfLogger;
import ch.sla.jdbcperflogger.model.PreparedStatementValuesHolder;
import ch.sla.jdbcperflogger.model.SqlTypedValue;

public class LoggingPreparedStatementInvocationHandler extends LoggingStatementInvocationHandler {
    private static final String CLEAR_PARAMETERS = "clearParameters";

    private final String rawSql;
    private final PreparedStatementValuesHolder paramValues = new PreparedStatementValuesHolder();
    private final List<Object> batchedPreparedOrNonPreparedStmtExecutions = new ArrayList<Object>();

    LoggingPreparedStatementInvocationHandler(final UUID connectionId, final PreparedStatement statement,
            final String rawSql, final DatabaseType databaseType) {
        super(connectionId, statement, databaseType);
        this.rawSql = rawSql;
    }

    @Override
    @Nullable
    public Object invoke(final @Nullable Object proxy, final Method method, final Object @Nullable [] args)
            throws Throwable {

        final Object result;
        final String methodName = method.getName();
        if (args == null || args.length == 0) {
            if (EXECUTE_QUERY.equals(methodName) && args == null) {
                return internalExecutePreparedQuery(method);
            } else if ((EXECUTE.equals(methodName) || EXECUTE_UPDATE.equals(methodName)
                    || EXECUTE_LARGE_UPDATE.equals(methodName))) {
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
            // TODO : handle getResultSet to return a proxy to the resultset like in internalExecutePreparedQuery
        } else {
            if (methodName.startsWith("set")) {
                result = Utils.invokeUnwrapException(wrappedStatement, method, args);
                if ("setNull".equals(methodName) && args[1] instanceof Integer) {
                    paramValues.put((Serializable) args[0], new SqlTypedValue(null, ((Integer) args[1])));
                } else if (args.length == 2 || "setDate".equals(methodName) || "setTime".equals(methodName)
                        || "setTimestamp".equals(methodName)) {
                    paramValues.put((Serializable) args[0], new SqlTypedValue(args[1], methodName));
                } else if ("setObject".equals(methodName)) {
                    final Class<?>[] argType = method.getParameterTypes();
                    if (argType.length > 2) {
                        Integer sqlType = null;
                        if (argType[2] == Integer.TYPE) {
                            sqlType = (Integer) args[2];
                        } else if (argType[2] == SQLType.class) {
                            sqlType = ((SQLType) args[2]).getVendorTypeNumber();
                        }
                        paramValues.put((Serializable) args[0], new SqlTypedValue(args[1], sqlType));
                    }
                }
                return result;
            }
        }
        return super.invoke(proxy, method, args);
    }

    protected ResultSet internalExecutePreparedQuery(final Method method) throws Throwable {
        final UUID logId = UUID.randomUUID();
        final Connection connection = wrappedStatement.getConnection();
        PerfLogger.logBeforePreparedStatement(connectionId, logId, rawSql, paramValues,
                StatementType.PREPARED_QUERY_STMT, databaseType, wrappedStatement.getQueryTimeout(),
                connection.getAutoCommit(), connection.getTransactionIsolation());
        final long start = System.nanoTime();
        Throwable exc = null;
        try {
            final ResultSet resultSet = (ResultSet) Utils.invokeUnwrapExceptionReturnNonNull(wrappedStatement, method,
                    null);
            return (ResultSet) Proxy.newProxyInstance(resultSet.getClass().getClassLoader(),
                    Utils.extractAllInterfaces(resultSet.getClass()),
                    new LoggingResultSetInvocationHandler(resultSet, logId));
        } catch (final Throwable e) {
            exc = e;
            throw exc;
        } finally {
            final long end = System.nanoTime();
            PerfLogger.logStatementExecuted(logId, end - start, null, exc);
            lastExecutionLogId = logId;
        }

    }

    @Nullable
    protected Object internalExecutePrepared(final Method method, final Object @Nullable [] args) throws Throwable {
        final UUID logId = UUID.randomUUID();
        final long start = System.nanoTime();
        final Connection connection = wrappedStatement.getConnection();
        PerfLogger.logBeforePreparedStatement(connectionId, logId, rawSql, paramValues,
                StatementType.BASE_PREPARED_STMT, databaseType, wrappedStatement.getQueryTimeout(),
                connection.getAutoCommit(), connection.getTransactionIsolation());
        Throwable exc = null;
        Long updateCount = null;
        try {
            final Object result = Utils.invokeUnwrapException(wrappedStatement, method, args);
            if (result instanceof Number) {
                updateCount = ((Number) result).longValue();
            }
            return result;
        } catch (final Throwable e) {
            exc = e;
            throw exc;
        } finally {
            final long end = System.nanoTime();
            PerfLogger.logStatementExecuted(logId, end - start, updateCount, exc);
            lastExecutionLogId = logId;
        }
    }

    @Override
    @Nullable
    protected Object internalExecuteBatch(final Method method, final Object @Nullable [] args) throws Throwable {
        final UUID logId = UUID.randomUUID();
        final Connection connection = wrappedStatement.getConnection();
        PerfLogger.logPreparedBatchedStatements(connectionId, logId, rawSql, batchedPreparedOrNonPreparedStmtExecutions,
                databaseType, wrappedStatement.getQueryTimeout(), connection.getAutoCommit(), connection.getTransactionIsolation());
        try {
            return internalExecuteBatchInternal(method, args, logId);
        } finally {
            batchedPreparedOrNonPreparedStmtExecutions.clear();
            lastExecutionLogId = logId;
        }

    }

}
