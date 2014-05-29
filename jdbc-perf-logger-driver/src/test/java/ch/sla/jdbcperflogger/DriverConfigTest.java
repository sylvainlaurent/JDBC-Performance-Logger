package ch.sla.jdbcperflogger;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.InetSocketAddress;

import org.junit.Test;

public class DriverConfigTest {

    private static final String TEST_TXT = "src/test/resourcesNonClasspath/test.txt";

    @SuppressWarnings("null")
    @Test
    public void testDefaultConfig() throws Exception {
        assertEquals(8889, DriverConfig.INSTANCE.getServerPort().intValue());
        assertEquals(1, DriverConfig.INSTANCE.getClientAddresses().size());
        final InetSocketAddress defaultClientAddress = DriverConfig.INSTANCE.getClientAddresses().get(0);
        assertEquals("localhost", defaultClientAddress.getHostName());
        assertEquals(4561, defaultClientAddress.getPort());

        assertEquals("oracle.jdbc.OracleDriver", DriverConfig.INSTANCE.getClassNameForJdbcUrl("jdbc:oracle:"));
        assertEquals("oracle.jdbc.OracleDriver", DriverConfig.INSTANCE.getClassNameForJdbcUrl("jdbc:oracle:thin:toto"));
        assertEquals("com.MyDriver", DriverConfig.INSTANCE.getClassNameForJdbcUrl("jdbc:mydriver:"));
        assertNull(DriverConfig.INSTANCE.getClassNameForJdbcUrl("jdbc:mynonexisting:"));
    }

    @Test
    public void testOpenFallbackConfigFile() throws Exception {
        final InputStream is = DriverConfig.openConfigFile(PerfLoggerConstants.CONFIG_FILE_FALLBACK_LOCATION);
        assertNotNull(is);
        is.close();
    }

    @Test
    public void testOpenFileNotInClasspath() throws Exception {
        InputStream is = DriverConfig.openConfigFile(TEST_TXT);
        assertNotNull(is);
        is.close();

        is = DriverConfig.openConfigFile("src/test/resourcesNonClasspath/notfound.txt");
        assertNull(is);
    }

    @Test
    public void testOpenDefaultConfigFile() throws Exception {
        final InputStream is = DriverConfig.openConfigFile();
        assertNotNull(is);
        is.close();
    }

    @Test
    public void testOpenSystemSpecifiedInexistentConfigFile() throws Exception {
        System.setProperty(PerfLoggerConstants.CONFIG_FILE_LOCATION_PROP_KEY, "dummy");
        final InputStream is = DriverConfig.openConfigFile();
        assertNotNull(is);

        final String line = readFirstLine(is);
        assertTrue(line.contains("fallback"));
        is.close();
    }

    @Test
    public void testOpenSystemSpecifiedConfigFileMalformed() throws Exception {
        System.setProperty(PerfLoggerConstants.CONFIG_FILE_LOCATION_PROP_KEY, TEST_TXT);
        final InputStream is = DriverConfig.openConfigFile();
        assertNotNull(is);

        final String line = readFirstLine(is);
        assertTrue(line.contains("HELLO"));
        is.close();
    }

    private String readFirstLine(final InputStream is) throws IOException {
        return new BufferedReader(new InputStreamReader(is)).readLine();
    }

}
