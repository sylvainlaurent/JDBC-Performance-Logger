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

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.eclipse.jdt.annotation.Nullable;

import ch.sla.jdbcperflogger.DatabaseType;
import ch.sla.jdbcperflogger.Logger;

public final class Utils {
    private final static Logger LOGGER = Logger.getLogger(Utils.class);

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

    @Nullable
    static Object invokeUnwrapException(final Object target, final Method method, final Object @Nullable [] args)
            throws Throwable {
        try {
            return method.invoke(target, args);
        } catch (final InvocationTargetException e) {
            throw e.getCause();
        }
    }

    static Object invokeUnwrapExceptionReturnNonNull(final Object target, final Method method,
            final Object @Nullable [] args) throws Throwable {
        try {
            final Object result = method.invoke(target, args);
            assert result != null;
            return result;
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
