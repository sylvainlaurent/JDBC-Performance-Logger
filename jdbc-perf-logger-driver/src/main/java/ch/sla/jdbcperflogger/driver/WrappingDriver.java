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
import java.util.Properties;
import java.util.concurrent.atomic.AtomicInteger;

import javax.annotation.Nullable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.sla.jdbcperflogger.logger.PerfLoggerRemoting;

/**
 * This is the JDBC Driver implementation of the performance logger.
 * 
 * @author slaurent
 * 
 */
public class WrappingDriver implements Driver {
    public final static String URL_PREFIX = "jdbcperflogger:";
    private final static Logger LOGGER = LoggerFactory.getLogger(WrappingDriver.class);

    private final static WrappingDriver INSTANCE = new WrappingDriver();

    static {
        try {
            DriverManager.registerDriver(INSTANCE);
        } catch (final SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private final AtomicInteger connectionCounter = new AtomicInteger();

    public WrappingDriver() {
    }

    @Override
    @Nullable
    public Connection connect(@Nullable final String url, @Nullable final Properties info) throws SQLException {
        if (!acceptsURL(url)) {
            return null;
        }
        assert url != null;
        LOGGER.debug("connect url=[{}]", url);
        final String unWrappedUrl = extractUrlForWrappedDriver(url);
        Connection connection = DriverManager.getConnection(unWrappedUrl, info);

        final LoggingConnectionInvocationHandler connectionInvocationHandler = new LoggingConnectionInvocationHandler(
                connectionCounter.incrementAndGet(), connection, unWrappedUrl);
        connection = (Connection) Proxy.newProxyInstance(WrappingDriver.class.getClassLoader(),
                Utils.extractAllInterfaces(connection.getClass()), connectionInvocationHandler);

        PerfLoggerRemoting.connectionCreated(connectionInvocationHandler);

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
