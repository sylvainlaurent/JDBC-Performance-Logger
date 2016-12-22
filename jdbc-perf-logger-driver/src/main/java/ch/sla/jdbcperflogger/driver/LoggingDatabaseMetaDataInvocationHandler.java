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
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.eclipse.jdt.annotation.Nullable;

import ch.sla.jdbcperflogger.DatabaseType;
import ch.sla.jdbcperflogger.StatementType;
import ch.sla.jdbcperflogger.logger.PerfLogger;

public class LoggingDatabaseMetaDataInvocationHandler implements InvocationHandler {
    protected static final String GET_TABLES = "getTables";
    protected static final String GET_SCHEMAS = "getSchemas";
    protected static final String GET_COLUMNS = "getColumns";

    protected UUID connectionId;
    protected final DatabaseType databaseType;
    protected final DatabaseMetaData wrappedMetadata;
    protected @Nullable UUID lastExecutionLogId;

    LoggingDatabaseMetaDataInvocationHandler(final UUID connectionId, final DatabaseMetaData metadata,
            final DatabaseType databaseType) {
        this.connectionId = connectionId;
        wrappedMetadata = metadata;
        this.databaseType = databaseType;
    }

    @Override
    @Nullable
    public Object invoke(final @Nullable Object _proxy, final Method method, final @Nullable Object[] args)
            throws Throwable {

        final Object result;
        final String methodName = method.getName();
        if (GET_SCHEMAS.equals(methodName)) {
            return internalExecuteGetSchemas(method, args);
        } else if (GET_TABLES.equals(methodName)) {
            return internalExecuteGetTables(method, args);
        } else if (GET_COLUMNS.equals(methodName)) {
            return internalExecuteGetColumns(method, args);
        } else {
			
			final UUID logId = UUID.randomUUID();
			final long start = System.nanoTime();
			PerfLogger.logBeforeStatement(connectionId, logId, (String) method.getName(), StatementType.METADATA,
					0, wrappedMetadata.getConnection().getAutoCommit());
			Throwable exc = null;
			try {
				result = Utils.invokeUnwrapException(wrappedMetadata, method, args);
			} catch (final Throwable e) {
				exc = e;
				throw exc;
			} finally {
				final long end = System.nanoTime();
				PerfLogger.logStatementExecuted(logId, end - start, null, exc);
				lastExecutionLogId = logId;
			}
 
        }
        return result;
    }

    @Nullable
    protected final ResultSet internalExecuteGetSchemas(final Method method, final Object[] args) throws Throwable {
        final UUID logId = UUID.randomUUID();
        final long start = System.nanoTime();
        PerfLogger.logBeforeStatement(connectionId, logId, (String) method.getName(), StatementType.METADATA,
                0, wrappedMetadata.getConnection().getAutoCommit());
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

    @Nullable
    protected final ResultSet internalExecuteGetTables(final Method method, final Object[] args) throws Throwable {
        final UUID logId = UUID.randomUUID();
        final long start = System.nanoTime();
        PerfLogger.logBeforeStatement(connectionId, logId, (String) method.getName(), StatementType.METADATA,
                0, wrappedMetadata.getConnection().getAutoCommit());
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

    @Nullable
    protected final ResultSet internalExecuteGetColumns(final Method method, final Object[] args) throws Throwable {
        final UUID logId = UUID.randomUUID();
        final long start = System.nanoTime();
        PerfLogger.logBeforeStatement(connectionId, logId, (String) method.getName(), StatementType.METADATA,
                0, wrappedMetadata.getConnection().getAutoCommit());
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

    private ResultSet getAndWrapResultSet(final Method method, final @Nullable Object[] args, final UUID logId)
            throws Throwable {
        final ResultSet resultSet = (ResultSet) Utils.invokeUnwrapExceptionReturnNonNull(wrappedMetadata, method,
                args);
        return (ResultSet) Proxy.newProxyInstance(LoggingDatabaseMetaDataInvocationHandler.class.getClassLoader(),
                Utils.extractAllInterfaces(resultSet.getClass()),
                new LoggingResultSetInvocationHandler(resultSet, logId));
    }


}
