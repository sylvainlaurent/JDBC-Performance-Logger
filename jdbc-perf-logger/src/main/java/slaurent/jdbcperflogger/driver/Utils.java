package slaurent.jdbcperflogger.driver;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.sql.Connection;
import java.sql.SQLException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class Utils {
    private final static Logger LOGGER = LoggerFactory.getLogger(Utils.class);

    static DatabaseType getDatabaseType(final Connection connection) {
        String dbProduct;
        try {
            dbProduct = connection.getMetaData().getDatabaseProductName();
        } catch (final SQLException e) {
            LOGGER.error("cannot get db product name");
            return DatabaseType.UNKNOWN;
        }
        if ("Oracle".equals(dbProduct)) {
            return DatabaseType.ORACLE;
        }
        return DatabaseType.UNKNOWN;
    }

    static Object invokeUnwrapException(final Object target, final Method method, final Object[] args) throws Throwable {
        try {
            return method.invoke(target, args);
        } catch (final InvocationTargetException e) {
            throw e.getCause();
        }
    }

}
