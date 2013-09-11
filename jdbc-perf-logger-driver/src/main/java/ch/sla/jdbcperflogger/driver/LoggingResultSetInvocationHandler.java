package ch.sla.jdbcperflogger.driver;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.sql.ResultSet;
import java.util.UUID;

import ch.sla.jdbcperflogger.StatementType;
import ch.sla.jdbcperflogger.logger.PerfLogger;

public class LoggingResultSetInvocationHandler implements InvocationHandler {
	private final ResultSet wrappedResultSet;
	private final UUID logId;
	private final StatementType statementType;
	private final long fetchStartTime;
	private boolean closed;
	private int nbRowsIterated;

	LoggingResultSetInvocationHandler(final ResultSet rset, final UUID logId,
			final StatementType statementType) {
		wrappedResultSet = rset;
		this.logId = logId;
		this.statementType = statementType;
		fetchStartTime = System.nanoTime();
	}

	@Override
	public Object invoke(final Object proxy, final Method method,
			final Object[] args) throws Throwable {
		final Object result = Utils.invokeUnwrapException(wrappedResultSet,
				method, args);
		;
		final String methodName = method.getName();
		if (args == null || args.length == 0) {
			if ("close".equals(methodName)) {
				if (!closed) {
					closed = true;
					PerfLogger.logClosedResultSet(logId, System.nanoTime()
							- fetchStartTime, statementType, nbRowsIterated);
				}
			} else if ("next".equals(methodName)) {
				if (Boolean.TRUE.equals(result)) {
					nbRowsIterated++;
				}
			}
		}
		return result;
	}

}
