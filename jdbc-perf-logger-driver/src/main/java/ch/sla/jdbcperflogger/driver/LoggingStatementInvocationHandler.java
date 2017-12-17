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

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.eclipse.jdt.annotation.Nullable;

import ch.sla.jdbcperflogger.DatabaseType;
import ch.sla.jdbcperflogger.StatementType;
import ch.sla.jdbcperflogger.logger.PerfLogger;

public class LoggingStatementInvocationHandler implements InvocationHandler {
    protected static final String CLEAR_BATCH = "clearBatch";
    protected static final String ADD_BATCH = "addBatch";
    protected static final String EXECUTE_BATCH = "executeBatch";
    protected static final String EXECUTE_LARGE_BATCH = "executeLargeBatch";
    protected static final String EXECUTE_UPDATE = "executeUpdate";
    protected static final String EXECUTE_LARGE_UPDATE = "executeLargeUpdate";
    protected static final String EXECUTE = "execute";
    protected static final String EXECUTE_QUERY = "executeQuery";
    protected static final String GET_RESULT_SET = "getResultSet";

    protected UUID connectionId;
    protected final DatabaseType databaseType;
    protected final Statement wrappedStatement;
    private final List<String> batchedNonPreparedStmtExecutions = new ArrayList<String>();
    protected @Nullable UUID lastExecutionLogId;

    LoggingStatementInvocationHandler(final UUID connectionId, final Statement statement,
            final DatabaseType databaseType) {
        this.connectionId = connectionId;
        wrappedStatement = statement;
        this.databaseType = databaseType;
    }

    @Override
    @Nullable
    public Object invoke(final @Nullable Object _proxy, final Method method, final Object @Nullable [] args)
            throws Throwable {

        final Object result;
        final String methodName = method.getName();
        if (EXECUTE_QUERY.equals(methodName) && args != null) {
            return internalExecuteQuery(method, args);
        } else if ((EXECUTE.equals(methodName) || EXECUTE_UPDATE.equals(methodName)
                || EXECUTE_LARGE_UPDATE.equals(methodName)) && args != null) {
            return internalExecute(method, args);
        } else if (EXECUTE_BATCH.equals(methodName) || EXECUTE_LARGE_BATCH.equals(methodName)) {
            return internalExecuteBatch(method, args);
        } else if (GET_RESULT_SET.equals(methodName)) {
            // TODO : handle getResultSet to return a proxy to the resultset like in internalExecuteQuery
            assert lastExecutionLogId != null;
            return getAndWrapResultSet(method, args, lastExecutionLogId);
        } else {
            result = Utils.invokeUnwrapException(wrappedStatement, method, args);
            if (ADD_BATCH.equals(methodName) && args != null) {
                batchedNonPreparedStmtExecutions.add((String) args[0]);
            } else if (CLEAR_BATCH.equals(methodName)) {
                batchedNonPreparedStmtExecutions.clear();
            }
        }
        return result;
    }

    protected final ResultSet internalExecuteQuery(final Method method, final Object[] args) throws Throwable {
        final UUID logId = UUID.randomUUID();
        final long start = System.nanoTime();
        final Connection connection = wrappedStatement.getConnection();
        PerfLogger.logBeforeStatement(connectionId, logId, (String) args[0], StatementType.NON_PREPARED_QUERY_STMT,
                wrappedStatement.getQueryTimeout(), connection.getAutoCommit(), connection.getTransactionIsolation());
        Throwable exc = null;
        try {
            return getAndWrapResultSet(method, args, logId);
        } catch (final Throwable e) {
            exc = e;
            throw exc;
        } finally {
            final long end = System.nanoTime();
            PerfLogger.logStatementExecuted(logId, end - start, null, exc);
            lastExecutionLogId = logId;
        }

    }

    private ResultSet getAndWrapResultSet(final Method method, final Object @Nullable [] args, final UUID logId)
            throws Throwable {
        final ResultSet resultSet = (ResultSet) Utils.invokeUnwrapExceptionReturnNonNull(wrappedStatement, method,
                args);
        return (ResultSet) Proxy.newProxyInstance(resultSet.getClass().getClassLoader(),
                Utils.extractAllInterfaces(resultSet.getClass()),
                new LoggingResultSetInvocationHandler(resultSet, logId));
    }

    @Nullable
    protected final Object internalExecute(final Method method, final Object[] args) throws Throwable {
        final UUID logId = UUID.randomUUID();
        final Connection connection = wrappedStatement.getConnection();
        PerfLogger.logBeforeStatement(connectionId, logId, (String) args[0], StatementType.BASE_NON_PREPARED_STMT,
                wrappedStatement.getQueryTimeout(), connection.getAutoCommit(), connection.getTransactionIsolation());
        Throwable exc = null;
        Long updateCount = null;
        final long start = System.nanoTime();
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

    @Nullable
    protected Object internalExecuteBatch(final Method method, final Object @Nullable [] args) throws Throwable {
        final UUID logId = UUID.randomUUID();
        final Connection connection = wrappedStatement.getConnection();
        PerfLogger.logNonPreparedBatchedStatements(connectionId, logId, batchedNonPreparedStmtExecutions, databaseType,
                wrappedStatement.getQueryTimeout(), connection.getAutoCommit(), connection.getTransactionIsolation());
        try {
            return internalExecuteBatchInternal(method, args, logId);
        } finally {
            batchedNonPreparedStmtExecutions.clear();
            lastExecutionLogId = logId;
        }

    }

    @Nullable
    protected Object internalExecuteBatchInternal(final Method method, final Object @Nullable [] args, final UUID logId)
            throws Throwable {
        Throwable exc = null;
        long updateCount = -1;
        final long start = System.nanoTime();
        try {
            final Object result = Utils.invokeUnwrapException(wrappedStatement, method, args);
            if (result instanceof int[]) {
                final int[] nbRows = (int[]) result;
                long totalRows = 0;
                for (int i = 0; i < nbRows.length; i++) {
                    totalRows += nbRows[i];
                }
                updateCount = totalRows;
            } else if (result instanceof long[]) {// java8
                final long[] nbRows = (long[]) result;
                long totalRows = 0;
                for (int i = 0; i < nbRows.length; i++) {
                    totalRows += nbRows[i];
                }
                updateCount = totalRows;
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

}
