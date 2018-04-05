package ch.sla.jdbcperflogger.agent;

import static java.util.Objects.requireNonNull;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.lang.reflect.Proxy;
import java.net.URL;
import java.net.URLClassLoader;
import java.sql.Connection;
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

import org.junit.Before;
import org.junit.Test;

import ch.sla.jdbcperflogger.driver.LoggingConnectionInvocationHandler;
import ch.sla.jdbcperflogger.driver.WrappingDriver;

public class AgentIT {

    private static final String WRAPPED_DRIVER = "org.apache.derby.jdbc.EmbeddedDriver";

    @Before
    public void setup() {
        // assertTrue(Agent.isLoaded());
    }

    @Test
    public void returns_a_wrapped_connection_when_using_normal_url() throws SQLException {
        final Connection connection = DriverManager.getConnection("jdbc:h2:mem:", "sa", "");
        assertTrue("is proxy", Proxy.isProxyClass(connection.getClass()));
        assertEquals(LoggingConnectionInvocationHandler.class, Proxy.getInvocationHandler(connection).getClass());
    }

    @Test
    public void returns_a_wrapped_connection_when_using_jdbcperf_url() throws SQLException {
        final Connection connection = DriverManager.getConnection(WrappingDriver.URL_PREFIX + "jdbc:h2:mem:", "sa", "");
        assertTrue("is proxy", Proxy.isProxyClass(connection.getClass()));
        assertEquals(LoggingConnectionInvocationHandler.class, Proxy.getInvocationHandler(connection).getClass());
    }

    @Test(expected = ClassNotFoundException.class)
    public void wrapped_driver_is_not_in_classpath() throws ClassNotFoundException {
        Class.forName(WRAPPED_DRIVER);
    }

    @Test()
    public void load_wrapped_from_sub_classloader() throws Exception {
        final File wrappedDriverJarFile = new File(System.getProperty("wrapped.driver.url"));
        final URLClassLoader childClassLoader = new URLClassLoader(new URL[] { wrappedDriverJarFile.toURI().toURL() });
        // register the driver in the DriverManager
        @SuppressWarnings("unchecked")
        final Class<Driver> driverClass = (Class<Driver>) Class.forName(WRAPPED_DRIVER, true,
                childClassLoader);
        final Driver driver = driverClass.newInstance();

        final Connection connection = requireNonNull(
                driver.connect("jdbc:derby:memory:myDB;create=true", new Properties()));
        assertTrue("is proxy", Proxy.isProxyClass(connection.getClass()));
        assertEquals(LoggingConnectionInvocationHandler.class, Proxy.getInvocationHandler(connection).getClass());

        boolean foundInterfaceLoadedBySubClassLoader = false;
        for (final Class<?> itf : connection.getClass().getInterfaces()) {
            if (itf.getClassLoader() == childClassLoader) {
                foundInterfaceLoadedBySubClassLoader = true;
            }
        }
        assertTrue("Should have found an interface loaded by the subclassloader", foundInterfaceLoadedBySubClassLoader);
    }

}
