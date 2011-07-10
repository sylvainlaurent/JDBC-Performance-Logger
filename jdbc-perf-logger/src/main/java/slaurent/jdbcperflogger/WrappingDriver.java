package slaurent.jdbcperflogger;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.sql.Connection;
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.DriverPropertyInfo;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

public class WrappingDriver implements Driver {
    public final static String URL_PREFIX = "jdbcperflogger:";
    public final static String CONFIG_FILE = "jdbcperflogger.xml";

    private final static Logger LOGGER = LoggerFactory.getLogger(WrappingDriver.class);

    private final static WrappingDriver INSTANCE = new WrappingDriver();

    private static List<Constructor<WrappingConnection>> WRAPPING_CONN_CTORS;

    static {
        try {
            DriverManager.registerDriver(INSTANCE);
        } catch (final SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public WrappingDriver() {
    }

    public Connection connect(final String url, final Properties info) throws SQLException {
        if (!acceptsURL(url)) {
            return null;
        }
        LOGGER.debug("connect url=[{}]", url);
        Connection connection = DriverManager.getConnection(extractUrlForWrappedDriver(url), info);

        try {
            final List<Constructor<WrappingConnection>> wrappingConnectionConstructors = getWrappingConnectionConstructors();
            for (final Constructor<WrappingConnection> constructor : wrappingConnectionConstructors) {
                connection = constructor.newInstance(connection);
            }
        } catch (final Exception e) {
            throw new RuntimeException(e);
        }
        return connection;
    }

    public boolean acceptsURL(final String url) throws SQLException {
        return url.startsWith(URL_PREFIX);
    }

    public DriverPropertyInfo[] getPropertyInfo(final String url, final Properties info) throws SQLException {
        return new DriverPropertyInfo[0];
    }

    public int getMajorVersion() {
        return 1;
    }

    public int getMinorVersion() {
        return 0;
    }

    public boolean jdbcCompliant() {
        return false;
    }

    private String extractUrlForWrappedDriver(final String url) {
        return url.substring(URL_PREFIX.length());
    }

    static List<Constructor<WrappingConnection>> getWrappingConnectionConstructors() {
        if (WRAPPING_CONN_CTORS == null) {
            // we don't care if passing through here more than once because of thread issues, the result is idem potent
            // no need for double-check locking pattern.
            final List<Class<WrappingConnection>> classes = getWrappingConnectionClasses();
            final List<Constructor<WrappingConnection>> result = new ArrayList<Constructor<WrappingConnection>>(
                    classes.size());
            for (final Class<WrappingConnection> cl : classes) {
                try {
                    result.add(cl.getConstructor(Connection.class));
                } catch (final SecurityException e) {
                    throw new RuntimeException(e);
                } catch (final NoSuchMethodException e) {
                    throw new RuntimeException(e);
                }
            }
            WRAPPING_CONN_CTORS = result;
        }
        return WRAPPING_CONN_CTORS;
    }

    @SuppressWarnings("unchecked")
    static List<Class<WrappingConnection>> getWrappingConnectionClasses() {
        final List<Class<WrappingConnection>> defaultResult = Collections.singletonList(WrappingConnection.class);
        try {

            final DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            final DocumentBuilder docBuilder = dbf.newDocumentBuilder();
            final InputStream configFileStream = WrappingDriver.class.getResourceAsStream("/" + CONFIG_FILE);
            if (configFileStream == null) {
                LOGGER.warn("Cannot find " + CONFIG_FILE + " in the classpath, using default configuration");
                return defaultResult;
            }
            final Document doc = docBuilder.parse(configFileStream);
            final Element root = (Element) doc.getElementsByTagName("jdbc-perf-logger").item(0);
            final Element classListParent = (Element) root.getElementsByTagName("wrapping-connections").item(0);
            if (classListParent == null) {
                return defaultResult;
            }
            final NodeList classList = classListParent.getElementsByTagName("wrapping-connection");
            final List<Class<WrappingConnection>> result = new ArrayList<Class<WrappingConnection>>(
                    classList.getLength());
            for (int i = 0; i < classList.getLength(); i++) {
                final String className = classList.item(i).getTextContent();
                if (className == null) {
                    continue;
                }
                try {
                    result.add((Class<WrappingConnection>) Class.forName(className.trim()));
                } catch (final ClassNotFoundException e) {
                    LOGGER.warn("Cannot find class {}, falling back to default configuration", e);
                    return defaultResult;
                }
            }
            if (result.isEmpty()) {
                LOGGER.warn("No WrappingConnection configured, falling bac to default");
                return defaultResult;
            }
            return result;

        } catch (final ParserConfigurationException e) {
            throw new RuntimeException(e);
        } catch (final IOException e) {
            throw new RuntimeException(e);
        } catch (final SAXException e) {
            LOGGER.warn("Error parsing " + CONFIG_FILE + ", falling back to default config", e);
            return defaultResult;
        }
    }
}
