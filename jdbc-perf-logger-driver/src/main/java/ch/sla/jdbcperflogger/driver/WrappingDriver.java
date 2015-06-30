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
import java.sql.DriverManager;
import java.sql.DriverPropertyInfo;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.text.MessageFormat;
import java.util.Map;
import java.util.Properties;
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

    private final static WrappingDriver INSTANCE = new WrappingDriver();
    private final static Map<String, Driver> underlyingDrivers = new ConcurrentHashMap<String, Driver>();
    private static boolean registered;

    static {
        load();
    }

    public static synchronized Driver load() {
        if (!registered) {
            try {
                DriverManager.registerDriver(INSTANCE);
            } catch (final SQLException e) {
                throw new RuntimeException(e);
            }
            registered = true;
        }
        return INSTANCE;
    }

    public static synchronized void unload() {
        if (registered) {
            try {
                DriverManager.deregisterDriver(INSTANCE);
                // TODO : properly stop threads and sockets
                registered = false;
            } catch (final SQLException e) {
                throw new RuntimeException(e);
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
            underlyingDriver = DriverManager.getDriver(unWrappedUrl);
        }

        final long startNanos = System.nanoTime();

        Connection connection = underlyingDriver.connect(unWrappedUrl, info);

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
                connectionCounter.incrementAndGet(), connection, unWrappedUrl, cleanedConnectionProperties);
        connection = (Connection) Proxy.newProxyInstance(WrappingDriver.class.getClassLoader(),
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

    public java.util.logging.Logger getParentLogger() throws SQLFeatureNotSupportedException {
        throw new SQLFeatureNotSupportedException();
    }

    private String extractUrlForWrappedDriver(final String url) {
        return url.substring(URL_PREFIX.length());
    }

}
