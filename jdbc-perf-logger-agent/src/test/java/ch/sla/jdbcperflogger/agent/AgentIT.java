package ch.sla.jdbcperflogger.agent;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

import org.junit.Before;
import org.junit.Test;

import ch.sla.jdbcperflogger.driver.LoggingConnectionInvocationHandler;
import ch.sla.jdbcperflogger.driver.WrappingDriver;

public class AgentIT {

    @Before
    public void setup() {
        assertTrue(Agent.isLoaded());
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
}
