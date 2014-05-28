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
        assertEquals(8889, DriverConfig.getServerPort().intValue());
        assertEquals(1, DriverConfig.getClientAddresses().size());
        final InetSocketAddress defaultClientAddress = DriverConfig.getClientAddresses().get(0);
        assertEquals("localhost", defaultClientAddress.getHostName());
        assertEquals(4561, defaultClientAddress.getPort());
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
