package slaurent.jdbcperflogger.logger;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import org.junit.Test;

import slaurent.jdbcperflogger.driver.WrappingDriver;

public class PerfLoggerRemotingTest {

    private static final String TEST_TXT = "src/test/resourcesNonClasspath/test.txt";

    @Test
    public void testOpenFallbackConfigFile() throws Exception {
        final InputStream is = PerfLoggerRemoting.openConfigFile(WrappingDriver.CONFIG_FILE_FALLBACK_LOCATION);
        assertNotNull(is);
        is.close();
    }

    @Test
    public void testOpenFileNotInClasspath() throws Exception {
        InputStream is = PerfLoggerRemoting.openConfigFile(TEST_TXT);
        assertNotNull(is);
        is.close();

        is = PerfLoggerRemoting.openConfigFile("src/test/resourcesNonClasspath/notfound.txt");
        assertNull(is);
    }

    @Test
    public void testOpenDefaultConfigFile() throws Exception {
        final InputStream is = PerfLoggerRemoting.openConfigFile();
        assertNotNull(is);
        is.close();
    }

    @Test
    public void testOpenSystemSpecifiedInexistentConfigFile() throws Exception {
        System.setProperty(WrappingDriver.CONFIG_FILE_LOCATION_PROP_KEY, "dummy");
        final InputStream is = PerfLoggerRemoting.openConfigFile();
        assertNotNull(is);

        final String line = readFirstLine(is);
        assertTrue(line.contains("fallback"));
        is.close();
    }

    @Test
    public void testOpenSystemSpecifiedConfigFileMalformed() throws Exception {
        System.setProperty(WrappingDriver.CONFIG_FILE_LOCATION_PROP_KEY, TEST_TXT);
        final InputStream is = PerfLoggerRemoting.openConfigFile();
        assertNotNull(is);

        final String line = readFirstLine(is);
        assertTrue(line.contains("HELLO"));
        is.close();
    }

    private String readFirstLine(final InputStream is) throws IOException {
        return new BufferedReader(new InputStreamReader(is)).readLine();
    }
}
