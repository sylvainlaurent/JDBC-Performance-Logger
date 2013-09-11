package ch.sla.jdbcperflogger.driver;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.Statement;

public class LoggingConnectionInvocationHandler implements InvocationHandler {
	private final int connectionId;
	private final Connection wrappedConnection;
	private final DatabaseType databaseType;

	LoggingConnectionInvocationHandler(final int connectionId,
			final Connection wrappedConnection) {
		this.connectionId = connectionId;
		this.wrappedConnection = wrappedConnection;
		databaseType = Utils.getDatabaseType(wrappedConnection);
	}

	@Override
	public Object invoke(final Object proxy, final Method method,
			final Object[] args) throws Throwable {
		final Object result = Utils.invokeUnwrapException(wrappedConnection,
				method, args);
		final String methodName = method.getName();
		if ("createStatement".equals(methodName)) {
			return Proxy.newProxyInstance(
					LoggingConnectionInvocationHandler.class.getClassLoader(),
					Utils.extractAllInterfaces(result.getClass()),
					new LoggingStatementInvocationHandler(connectionId,
							(Statement) result, databaseType));
		} else if ("prepareStatement".equals(methodName)
				|| "prepareCall".equals(methodName)) {
			return Proxy.newProxyInstance(
					LoggingConnectionInvocationHandler.class.getClassLoader(),
					Utils.extractAllInterfaces(result.getClass()),
					new LoggingPreparedStatementInvocationHandler(connectionId,
							(PreparedStatement) result, (String) args[0],
							databaseType));
		}

		return result;
	}

}
