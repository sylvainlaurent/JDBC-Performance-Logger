package slaurent.jdbcperflogger;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class LoggingStatementInvocationHandler implements InvocationHandler {
    protected static final String CLEAR_BATCH = "clearBatch";
    protected static final String ADD_BATCH = "addBatch";
    protected static final String EXECUTE_BATCH = "executeBatch";
    protected static final String EXECUTE_UPDATE = "executeUpdate";
    protected static final String EXECUTE = "execute";
    protected static final String EXECUTE_QUERY = "executeQuery";

    protected final DatabaseType databaseType;
    protected final Statement wrappedStatement;
    private final List<String> batchedNonPreparedStmtExecutions = new ArrayList<String>();

    LoggingStatementInvocationHandler(final Statement statement, final DatabaseType databaseType) {
        wrappedStatement = statement;
        this.databaseType = databaseType;
    }

    public Object invoke(final Object proxy, final Method method, final Object[] args) throws Throwable {
        final Object result;
        final String methodName = method.getName();
        if (EXECUTE_QUERY.equals(methodName)) {
            return internalExecuteQuery(method, args);
        } else if (EXECUTE.equals(methodName) || EXECUTE_UPDATE.equals(methodName)) {
            return internalExecute(method, args);
        } else if (EXECUTE_BATCH.equals(methodName)) {
            return internalExecuteBatch(method, args);
        } else {
            result = Utils.invokeUnwrapException(wrappedStatement, method, args);
            if (ADD_BATCH.equals(methodName)) {
                batchedNonPreparedStmtExecutions.add((String) args[0]);
            } else if (CLEAR_BATCH.equals(methodName)) {
                batchedNonPreparedStmtExecutions.clear();
            }
        }
        return result;
    }

    protected ResultSet internalExecuteQuery(final Method method, final Object[] args) throws Throwable {
        final UUID logId = UUID.randomUUID();
        final long start = System.nanoTime();
        Throwable exc = null;
        try {
            final ResultSet resultSet = (ResultSet) Utils.invokeUnwrapException(wrappedStatement, method, args);
            return (ResultSet) Proxy.newProxyInstance(LoggingStatementInvocationHandler.class.getClassLoader(),
                    resultSet.getClass().getInterfaces(), new LoggingResultSetInvocationHandler(resultSet, logId,
                            StatementType.NON_PREPARED_QUERY_STMT));
        } catch (final Throwable e) {
            exc = e;
            throw exc;
        } finally {
            final long end = System.nanoTime();
            PerfLogger.logStatement(logId, (String) args[0], end - start, StatementType.NON_PREPARED_QUERY_STMT, exc);
        }

    }

    protected Object internalExecute(final Method method, final Object[] args) throws Throwable {
        final long start = System.nanoTime();
        Throwable exc = null;
        try {
            return Utils.invokeUnwrapException(wrappedStatement, method, args);
        } catch (final Throwable e) {
            exc = e;
            throw exc;
        } finally {
            final long end = System.nanoTime();
            PerfLogger.logStatement(UUID.randomUUID(), (String) args[0], end - start,
                    StatementType.BASE_NON_PREPARED_STMT, exc);
        }
    }

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
            PerfLogger
                    .logNonPreparedBatchedStatements(batchedNonPreparedStmtExecutions, end - start, databaseType, exc);
            batchedNonPreparedStmtExecutions.clear();
        }

    }

}
