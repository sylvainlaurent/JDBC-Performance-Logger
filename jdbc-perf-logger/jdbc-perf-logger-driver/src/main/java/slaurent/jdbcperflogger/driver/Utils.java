package slaurent.jdbcperflogger.driver;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class Utils {
    private final static Logger LOGGER = LoggerFactory.getLogger(Utils.class);

    private Utils() {

    }

    static DatabaseType getDatabaseType(final Connection connection) {
        String dbProduct;
        try {
            dbProduct = connection.getMetaData().getDatabaseProductName();
        } catch (final SQLException e) {
            LOGGER.error("cannot get db product name");
            return DatabaseType.GENERIC;
        }
        if ("Oracle".equals(dbProduct)) {
            return DatabaseType.ORACLE;
        }
        return DatabaseType.GENERIC;
    }

    static Object invokeUnwrapException(final Object target, final Method method, final Object[] args) throws Throwable {
        try {
            return method.invoke(target, args);
        } catch (final InvocationTargetException e) {
            throw e.getCause();
        }
    }

    static Class<?>[] extractAllInterfaces(final Class<?> clazz) {
        final Set<Class<?>> interfaces = new HashSet<Class<?>>();
        for (Class<?> currClazz = clazz; currClazz != null; currClazz = currClazz.getSuperclass()) {
            Collections.addAll(interfaces, currClazz.getInterfaces());
        }

        return interfaces.toArray(new Class[interfaces.size()]);
    }

}
