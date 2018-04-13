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

import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.Driver;
import java.sql.DriverAction;
import java.sql.DriverManager;
import java.sql.DriverPropertyInfo;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.text.MessageFormat;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import org.eclipse.jdt.annotation.Nullable;

import ch.sla.jdbcperflogger.DriverConfig;
import ch.sla.jdbcperflogger.Logger;
import ch.sla.jdbcperflogger.logger.PerfLoggerRemoting;

/**
 * This is the JDBC Driver implementation of the performance logger.
 *
 * @author slaurent
 *
 */
public class WrappingDriver implements Driver {
    public final static String URL_PREFIX = "jdbcperflogger:";
    private final static Logger LOGGER = Logger.getLogger(WrappingDriver.class);

    public final static WrappingDriver INSTANCE = new WrappingDriver();
    private final static Map<String, Driver> underlyingDrivers = new ConcurrentHashMap<String, Driver>();
    private static boolean registered;

    static {
        load();
    }

    public static synchronized Driver load() {
        if (!registered) {
            try {
                LOGGER.debug("classloader is " + INSTANCE.getClass().getClassLoader());
                DriverManager.registerDriver(INSTANCE, WrappingDriverAction.ACTION_INSTANCE);
            } catch (final SQLException e) {
                throw new RuntimeException(e);
            }
            PerfLoggerRemoting.start();
            registered = true;
        }
        return INSTANCE;
    }

    public static synchronized void unload() {
        if (registered) {
            try {
                // flag as deregistered even before actually deregistering to avoid infinite recursion
                registered = false;
                DriverManager.deregisterDriver(INSTANCE);
            } catch (final SQLException e) {
                throw new RuntimeException(e);
            } finally {
                PerfLoggerRemoting.stop();
            }
        }
    }

    private static final AtomicInteger connectionCounter = new AtomicInteger();

    public WrappingDriver() {
    }

    @Override
    @Nullable
    public Connection connect(final String url, @Nullable final Properties info) throws SQLException {
        if (!acceptsURL(url)) {
            return null;
        }
        LOGGER.debug("connect url=[" + url + "]");
        final String unWrappedUrl = extractUrlForWrappedDriver(url);

        Driver underlyingDriver = null;

        final String underlyingDriverClassName = DriverConfig.INSTANCE.getClassNameForJdbcUrl(unWrappedUrl);
        if (underlyingDriverClassName != null) {
            underlyingDriver = underlyingDrivers.get(underlyingDriverClassName);
        }

        if (underlyingDriver == null && underlyingDriverClassName != null) {
            try {
                final Class<?> underlyingDriverClass = Class.forName(underlyingDriverClassName);
                underlyingDriver = (Driver) underlyingDriverClass.newInstance();
                underlyingDrivers.put(underlyingDriverClassName, underlyingDriver);
            } catch (final ClassNotFoundException e) {
                final String msg = MessageFormat.format("Cannot find driver class {0} for JDBC url {1}",
                        underlyingDriverClassName, unWrappedUrl);
                LOGGER.warn(msg, e);
            } catch (final InstantiationException e) {
                throw new SQLException(e);
            } catch (final IllegalAccessException e) {
                throw new SQLException(e);
            }
        }
        if (underlyingDriver == null) {
            // unknown driver, just use the DriverManager to attempt to locate it
            try {
                underlyingDriver = DriverManager.getDriver(unWrappedUrl);
            } catch (final SQLException e) {
                throw new SQLException("Cannot get underlying JDBC driver for [" + unWrappedUrl
                        + "]. The underlying driver must be either registered with the DriverManager or listed "
                        + "in a jdbcperflogger.xml file, see documentation.",
                        e);
            }
        }
        final Driver finalUnderlyingDriver = underlyingDriver;
        final Connection connection = wrapConnection(unWrappedUrl, info, new Callable<@Nullable Connection>() {
            @Override
            public @Nullable Connection call() throws Exception {
                return finalUnderlyingDriver.connect(unWrappedUrl, info);
            }
        });

        return connection;
    }

    public @Nullable Connection wrapConnection(final String url, final @Nullable Properties info,
            final Callable<@Nullable Connection> underlyingConnectionCreator) throws SQLException {
        final long startNanos = System.nanoTime();

        Connection connection;
        try {
            connection = underlyingConnectionCreator.call();
        } catch (final Exception e) {
            if (e.getCause() instanceof SQLException) {
                throw (SQLException) e.getCause();
            } else {
                throw new SQLException(e);
            }
        }
        if (connection == null) {
            // short-circuit, the underlying driver was not the right one
            return null;
        }
        if (Proxy.isProxyClass(connection.getClass())
                && Proxy.getInvocationHandler(connection).getClass() == LoggingConnectionInvocationHandler.class) {
            // the connection may have already been wrapped if the caller asks for a jdbcperflogger: prefixed url while
            // using the java agent at the same time. In that case, just return the connection without wrapping it again
            return connection;
        }

        final long connectionCreationDuration = System.nanoTime() - startNanos;

        final Properties cleanedConnectionProperties = new Properties();
        if (info != null) {
            for (final String str : info.stringPropertyNames()) {
                if (!str.toLowerCase().contains("password")) {
                    cleanedConnectionProperties.setProperty(str, info.getProperty(str));
                }
            }
        }

        final LoggingConnectionInvocationHandler connectionInvocationHandler = new LoggingConnectionInvocationHandler(
                connectionCounter.incrementAndGet(), connection, url, cleanedConnectionProperties);
        connection = (Connection) Proxy.newProxyInstance(connection.getClass().getClassLoader(),
                Utils.extractAllInterfaces(connection.getClass()), connectionInvocationHandler);

        PerfLoggerRemoting.connectionCreated(connectionInvocationHandler, connectionCreationDuration);
        return connection;
    }

    @Override
    public boolean acceptsURL(@Nullable final String url) throws SQLException {
        return url != null && url.startsWith(URL_PREFIX);
    }

    @Override
    public DriverPropertyInfo[] getPropertyInfo(@Nullable final String url, @Nullable final Properties info)
            throws SQLException {
        return new DriverPropertyInfo[0];
    }

    @Override
    public int getMajorVersion() {
        return 1;
    }

    @Override
    public int getMinorVersion() {
        return 0;
    }

    @Override
    public boolean jdbcCompliant() {
        return false;
    }

    @Override
    public java.util.logging.Logger getParentLogger() throws SQLFeatureNotSupportedException {
        throw new SQLFeatureNotSupportedException();
    }

    private String extractUrlForWrappedDriver(final String url) {
        return url.substring(URL_PREFIX.length());
    }

    private static class WrappingDriverAction implements DriverAction {
        static WrappingDriverAction ACTION_INSTANCE = new WrappingDriverAction();

        @Override
        public void deregister() {
            WrappingDriver.unload();
        }

    }

}
