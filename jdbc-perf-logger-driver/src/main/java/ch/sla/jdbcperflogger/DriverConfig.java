package ch.sla.jdbcperflogger;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

public class DriverConfig {
    private final static Logger LOGGER = Logger.getLogger(DriverConfig.class);

    public final static DriverConfig INSTANCE;

    @Nullable
    private Integer serverPort;
    private final List<InetSocketAddress> clientAddresses = new ArrayList<InetSocketAddress>();
    private final Map<String, String> driverPrefixToClassName = new HashMap<String, String>();

    static {

        final InputStream configFileStream = openConfigFile();
        INSTANCE = parseConfig(configFileStream);
    }

    static DriverConfig parseConfig(final InputStream configFileStream) {
        try {
            final DriverConfig config = new DriverConfig();
            final DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            final DocumentBuilder docBuilder = dbf.newDocumentBuilder();
            final Document doc = docBuilder.parse(configFileStream);
            final Element root = (Element) doc.getElementsByTagName("jdbc-perf-logger").item(0);

            {
                final NodeList localServersList = root.getElementsByTagName("local-server");
                for (int i = 0; i < localServersList.getLength(); i++) {
                    final String port = localServersList.item(i).getAttributes().getNamedItem("port").getTextContent();
                    config.serverPort = Integer.parseInt(port);
                }
            }

            {
                final NodeList targetClientList = root.getElementsByTagName("target-console");
                for (int i = 0; i < targetClientList.getLength(); i++) {
                    final NamedNodeMap attributes = targetClientList.item(i).getAttributes();
                    @NonNull
                    final String host = attributes.getNamedItem("host").getTextContent();
                    @NonNull
                    final String port = attributes.getNamedItem("port").getTextContent();
                    config.clientAddresses.add(InetSocketAddress.createUnresolved(host, Integer.parseInt(port)));
                }
            }

            final NodeList jdbcDriversRootNodesList = doc.getElementsByTagName("jdbc-drivers");
            if (jdbcDriversRootNodesList.getLength() > 0) {
                final NodeList jdbcDriversNodeList = ((Element) jdbcDriversRootNodesList.item(0))
                        .getElementsByTagName("jdbc-driver");
                for (int i = 0; i < jdbcDriversNodeList.getLength(); i++) {
                    final Element jdbcDriverNode = ((Element) jdbcDriversNodeList.item(i));
                    final NodeList prefixNode = jdbcDriverNode.getElementsByTagName("prefix");
                    final NodeList classNameNode = jdbcDriverNode.getElementsByTagName("driver-class-name");
                    if (prefixNode.getLength() > 0 && classNameNode.getLength() > 0) {
                        final String prefix = prefixNode.item(0).getTextContent();
                        final String className = classNameNode.item(0).getTextContent();
                        if (prefix != null && className != null) {
                            config.driverPrefixToClassName.put(prefix.trim(), className.trim());
                        }
                    }
                }
            }

            return config;
        } catch (final ParserConfigurationException e) {
            throw new RuntimeException(e);
        } catch (final IOException e) {
            throw new RuntimeException(e);
        } catch (final SAXException e) {
            throw new RuntimeException(e);
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
                throw new RuntimeException(
                        "Unexpected: cannot find " + PerfLoggerConstants.CONFIG_FILE_FALLBACK_LOCATION);
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
    public Integer getServerPort() {
        return serverPort;
    }

    public List<InetSocketAddress> getClientAddresses() {
        return clientAddresses;
    }

    @Nullable
    public String getClassNameForJdbcUrl(final String jdbcUrl) {
        for (final Entry<String, String> driver : driverPrefixToClassName.entrySet()) {
            if (jdbcUrl.startsWith(driver.getKey())) {
                return driver.getValue();
            }
        }
        return null;
    }

}
