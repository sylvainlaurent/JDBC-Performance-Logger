package ch.sla.jdbcperflogger.console.ui;

import java.awt.Desktop;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Properties;

import org.eclipse.jdt.annotation.Nullable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GuiUtils {
    private final static Logger LOGGER = LoggerFactory.getLogger(WelcomePanel.class);

    static String getAppVersion() {
        final Properties mavenProps = new Properties();
        try (@Nullable
                InputStream pomPropsFile = WelcomePanel.class
                .getResourceAsStream("/META-INF/maven/com.github.sylvainlaurent.jdbcperflogger/jdbc-perf-logger-gui/pom.properties")) {
            if (pomPropsFile != null) {
                mavenProps.load(pomPropsFile);
            }
            return mavenProps.getProperty("version");
        } catch (final IOException e) {
            LOGGER.warn("", e);
            return "unknown";
        }
    }

    static void openWebSite(final String address) {
        if (Desktop.isDesktopSupported()) {
            try {
                final URI uri = new URI(address);
                Desktop.getDesktop().browse(uri);
            } catch (URISyntaxException | IOException e) {
                throw new RuntimeException("unexpected", e);
            }
        }
    }
}
