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
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import javax.annotation.Nullable;

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
    public Object invoke(@Nullable final Object proxy, @Nullable final Method _method, @Nullable final Object[] args)
            throws Throwable {
        assert _method != null;
        final Method method = _method;

        final Object result;
        final String methodName = method.getName();
        if (args == null || args.length == 0) {
            if (EXECUTE_QUERY.equals(methodName) && args == null) {
                return internalExecutePreparedQuery(method);
            } else if ((EXECUTE.equals(methodName) || EXECUTE_UPDATE.equals(methodName))) {
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

    protected ResultSet internalExecutePreparedQuery(final Method method) throws Throwable {
        final UUID logId = UUID.randomUUID();
        PerfLogger.logBeforePreparedStatement(connectionId, logId, rawSql, paramValues,
                StatementType.PREPARED_QUERY_STMT, databaseType, wrappedStatement.getQueryTimeout());
        final long start = System.nanoTime();
        Throwable exc = null;
        try {
            final ResultSet resultSet = (ResultSet) Utils.invokeUnwrapExceptionReturnNonNull(wrappedStatement, method,
                    null);
            return (ResultSet) Proxy.newProxyInstance(LoggingPreparedStatementInvocationHandler.class.getClassLoader(),
                    Utils.extractAllInterfaces(resultSet.getClass()), new LoggingResultSetInvocationHandler(resultSet,
                            logId));
        } catch (final Throwable e) {
            exc = e;
            throw exc;
        } finally {
            final long end = System.nanoTime();
            PerfLogger.logStatementExecuted(logId, end - start, exc);
        }

    }

    @Nullable
    protected Object internalExecutePrepared(final Method method, @Nullable final Object[] args) throws Throwable {
        final UUID logId = UUID.randomUUID();
        final long start = System.nanoTime();
        PerfLogger.logBeforePreparedStatement(connectionId, logId, rawSql, paramValues,
                StatementType.BASE_PREPARED_STMT, databaseType, wrappedStatement.getQueryTimeout());
        Throwable exc = null;
        try {
            return Utils.invokeUnwrapException(wrappedStatement, method, args);
        } catch (final Throwable e) {
            exc = e;
            throw exc;
        } finally {
            final long end = System.nanoTime();
            PerfLogger.logStatementExecuted(logId, end - start, exc);
        }
    }

    @Override
    @Nullable
    protected Object internalExecuteBatch(final Method method, @Nullable final Object[] args) throws Throwable {
        final UUID logId = UUID.randomUUID();
        PerfLogger.logPreparedBatchedStatements(connectionId, rawSql, batchedPreparedOrNonPreparedStmtExecutions,
                databaseType, wrappedStatement.getQueryTimeout());
        final long start = System.nanoTime();
        Throwable exc = null;
        try {
            return Utils.invokeUnwrapException(wrappedStatement, method, args);
        } catch (final Throwable e) {
            exc = e;
            throw exc;
        } finally {
            final long end = System.nanoTime();
            PerfLogger.logStatementExecuted(logId, end - start, exc);
            batchedPreparedOrNonPreparedStmtExecutions.clear();
        }

    }

}
