package slaurent.jdbcperflogger;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.Statement;

public class LoggingConnectionInvocationHandler implements InvocationHandler {
    private final Connection wrappedConnection;
    final DatabaseType databaseType;

    LoggingConnectionInvocationHandler(final Connection wrappedConnection) {
        this.wrappedConnection = wrappedConnection;
        databaseType = Utils.getDatabaseType(wrappedConnection);
    }

    public Object invoke(final Object proxy, final Method method, final Object[] args) throws Throwable {
        final Object result = Utils.invokeUnwrapException(wrappedConnection, method, args);
        final String methodName = method.getName();
        if ("createStatement".equals(methodName)) {
            return Proxy.newProxyInstance(LoggingConnectionInvocationHandler.class.getClassLoader(),
                    new Class[] { Statement.class }, new LoggingStatementInvocationHandler((Statement) result,
                            databaseType));
        } else if ("prepareStatement".equals(methodName)) {
            return Proxy.newProxyInstance(LoggingConnectionInvocationHandler.class.getClassLoader(),
                    new Class[] { PreparedStatement.class }, new LoggingPreparedStatementInvocationHandler(
                            (PreparedStatement) result, (String) args[0], databaseType));
        }
        // TODO prepareCall

        return result;
    }

}
