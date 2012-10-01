package slaurent.jdbcperflogger.driver;

import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.DriverPropertyInfo;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

//TODO DataSource, XADataSource
public class WrappingDriver implements Driver {
	public final static String URL_PREFIX = "jdbcperflogger:";
	public final static String CONFIG_FILE = "jdbcperflogger.xml";

	private final static Logger LOGGER = LoggerFactory
			.getLogger(WrappingDriver.class);

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
	public Connection connect(final String url, final Properties info)
			throws SQLException {
		if (!acceptsURL(url)) {
			return null;
		}
		LOGGER.debug("connect url=[{}]", url);
		Connection connection = DriverManager.getConnection(
				extractUrlForWrappedDriver(url), info);

		connection = (Connection) Proxy.newProxyInstance(WrappingDriver.class
				.getClassLoader(), Utils.extractAllInterfaces(connection
				.getClass()), new LoggingConnectionInvocationHandler(
				connectionCounter.incrementAndGet(), connection));
		return connection;
	}

	@Override
	public boolean acceptsURL(final String url) throws SQLException {
		return url.startsWith(URL_PREFIX);
	}

	@Override
	public DriverPropertyInfo[] getPropertyInfo(final String url,
			final Properties info) throws SQLException {
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

	public java.util.logging.Logger getParentLogger()
			throws SQLFeatureNotSupportedException {
		throw new SQLFeatureNotSupportedException();
	}

	private String extractUrlForWrappedDriver(final String url) {
		return url.substring(URL_PREFIX.length());
	}

}
