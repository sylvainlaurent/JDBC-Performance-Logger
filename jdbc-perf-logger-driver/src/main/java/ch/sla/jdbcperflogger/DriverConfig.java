package ch.sla.jdbcperflogger;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

public class DriverConfig {
    private final static Logger LOGGER = LoggerFactory.getLogger(DriverConfig.class);

    @Nullable
    private static Integer serverPort;
    private static final List<InetSocketAddress> clientAddresses = new ArrayList<InetSocketAddress>();

    static {

        final InputStream configFileStream = openConfigFile();

        try {
            final DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            final DocumentBuilder docBuilder = dbf.newDocumentBuilder();
            final Document doc = docBuilder.parse(configFileStream);
            final Element root = (Element) doc.getElementsByTagName("jdbc-perf-logger").item(0);
            final NodeList localServersList = root.getElementsByTagName("local-server");
            for (int i = 0; i < localServersList.getLength(); i++) {
                final String port = localServersList.item(i).getAttributes().getNamedItem("port").getTextContent();
                serverPort = Integer.parseInt(port);
            }

            final NodeList targetClientList = root.getElementsByTagName("target-console");
            for (int i = 0; i < targetClientList.getLength(); i++) {
                final NamedNodeMap attributes = targetClientList.item(i).getAttributes();
                @Nonnull
                final String host = attributes.getNamedItem("host").getTextContent();
                @Nonnull
                final String port = attributes.getNamedItem("port").getTextContent();
                clientAddresses.add(InetSocketAddress.createUnresolved(host, Integer.parseInt(port)));
            }
        } catch (final ParserConfigurationException e) {
            throw new RuntimeException(e);
        } catch (final IOException e) {
            throw new RuntimeException(e);
        } catch (final SAXException e) {
            LOGGER.warn("Error parsing " + PerfLoggerConstants.CONFIG_FILE_DEFAULT_LOCATION, e);
        }

    }

    static InputStream openConfigFile() {
        String location = System.getProperty(PerfLoggerConstants.CONFIG_FILE_LOCATION_PROP_KEY);
        if (location == null) {
            LOGGER.debug("No System property " + PerfLoggerConstants.CONFIG_FILE_LOCATION_PROP_KEY
                    + " defined, looking for config at " + PerfLoggerConstants.CONFIG_FILE_DEFAULT_LOCATION);
            location = PerfLoggerConstants.CONFIG_FILE_DEFAULT_LOCATION;
        }

        InputStream configFileStream = openConfigFile(location);
        if (configFileStream == null) {
            location = PerfLoggerConstants.CONFIG_FILE_FALLBACK_LOCATION;
            configFileStream = openConfigFile(location);
            if (configFileStream == null) {
                throw new RuntimeException("Unexpected: cannot find "
                        + PerfLoggerConstants.CONFIG_FILE_FALLBACK_LOCATION);
            }
        }
        LOGGER.info("Using config file " + location);

        return configFileStream;
    }

    @Nullable
    static InputStream openConfigFile(final String location) {
        InputStream configFileStream = PerfLoggerConstants.class.getResourceAsStream("/" + location);
        if (configFileStream == null) {
            LOGGER.debug("Cannot find config file " + location + " in the classpath, trying on filesystem");

            try {
                configFileStream = new FileInputStream(location);
            } catch (final FileNotFoundException e) {
                LOGGER.debug("Cannot find config file " + location + " on the filesystem");
                // not found, just return null
            }
        }
        return configFileStream;

    }

    @Nullable
    public static Integer getServerPort() {
        return serverPort;
    }

    public static List<InetSocketAddress> getClientAddresses() {
        return clientAddresses;
    }

}
