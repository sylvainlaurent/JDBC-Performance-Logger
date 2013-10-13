/* 
 *  Copyright 2013 Sylvain LAURENT
 *     
 *  Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package ch.sla.jdbcperflogger.logger;

import static ch.sla.jdbcperflogger.driver.WrappingDriver.CONFIG_FILE_DEFAULT_LOCATION;
import static ch.sla.jdbcperflogger.driver.WrappingDriver.CONFIG_FILE_FALLBACK_LOCATION;
import static ch.sla.jdbcperflogger.driver.WrappingDriver.CONFIG_FILE_LOCATION_PROP_KEY;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.net.SocketException;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

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

import ch.sla.jdbcperflogger.driver.WrappingDriver;
import ch.sla.jdbcperflogger.model.LogMessage;

public class PerfLoggerRemoting {
    private final static Logger LOGGER = LoggerFactory.getLogger(PerfLoggerRemoting.class);

    final static Set<LogSender> senders = new CopyOnWriteArraySet<PerfLoggerRemoting.LogSender>();

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
                new PerfLoggerServerThread(Integer.parseInt(port)).start();
            }
            final NodeList targetClientList = root.getElementsByTagName("target-console");
            for (int i = 0; i < targetClientList.getLength(); i++) {
                final NamedNodeMap attributes = targetClientList.item(i).getAttributes();
                final String host = attributes.getNamedItem("host").getTextContent();
                final String port = attributes.getNamedItem("port").getTextContent();
                new PerfLoggerClientThread(host, Integer.parseInt(port)).start();
            }
        } catch (final ParserConfigurationException e) {
            throw new RuntimeException(e);
        } catch (final IOException e) {
            throw new RuntimeException(e);
        } catch (final SAXException e) {
            LOGGER.warn("Error parsing " + WrappingDriver.CONFIG_FILE_DEFAULT_LOCATION, e);
        }

    }

    static InputStream openConfigFile() {
        String location = System.getProperty(CONFIG_FILE_LOCATION_PROP_KEY);
        if (location == null) {
            LOGGER.debug("No System property " + CONFIG_FILE_LOCATION_PROP_KEY + " defined, looking for config at "
                    + CONFIG_FILE_DEFAULT_LOCATION);
            location = CONFIG_FILE_DEFAULT_LOCATION;
        }

        InputStream configFileStream = openConfigFile(location);
        if (configFileStream == null) {
            location = CONFIG_FILE_FALLBACK_LOCATION;
            configFileStream = openConfigFile(location);
            if (configFileStream == null) {
                throw new RuntimeException("Unexpected: cannot find " + CONFIG_FILE_FALLBACK_LOCATION);
            }
        }
        LOGGER.info("Using config file " + location);

        return configFileStream;
    }

    static InputStream openConfigFile(final String location) {
        InputStream configFileStream = WrappingDriver.class.getResourceAsStream("/" + location);
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

    private PerfLoggerRemoting() {
    }

    static void postLog(final LogMessage log) {
        for (final LogSender sender : senders) {
            sender.postLog(log);
        }
    }

    static class LogSender implements Runnable {
        private final Logger LOGGER = LoggerFactory.getLogger(LogSender.class);

        private final BlockingQueue<LogMessage> logsToSend = new LinkedBlockingQueue<LogMessage>(10000);
        private final Socket socket;

        LogSender(final Socket socket) throws SocketException {
            this.socket = socket;
            socket.setKeepAlive(true);
            socket.setSoTimeout((int) TimeUnit.SECONDS.toMillis(10));
        }

        void postLog(final LogMessage log) {
            final boolean posted = logsToSend.offer(log);
            if (!posted) {
                LOGGER.warn("queue full, dropping remote log of statement");
            }
        }

        @Override
        public void run() {
            ObjectOutputStream oos = null;
            try {
                oos = new ObjectOutputStream(socket.getOutputStream());
                int cnt = 0;
                while (true) {
                    try {
                        final LogMessage log = logsToSend.poll(10, TimeUnit.SECONDS);
                        if (log != null) {
                            oos.writeObject(log);
                        } else {
                            // check the socket state
                            if (socket.isClosed() || !socket.isConnected()) {
                                // client disconnected
                                break;
                            }
                            oos.writeObject(null);
                        }
                        cnt = (cnt + 1) % 10;
                        if (cnt == 0) {
                            // avoid mem leak when the stream keeps back
                            // references to serialized objects
                            oos.reset();
                        }
                    } catch (final InterruptedException e) {
                        break;
                    }
                }
            } catch (final IOException e) {
                LOGGER.warn("socket error", e);
            } finally {
                LOGGER.info("closing connection with {}:{}", socket.getInetAddress(), socket.getPort());
                senders.remove(this);
                if (oos != null) {
                    try {
                        oos.close();
                    } catch (final IOException e) {
                    }
                }
                try {
                    socket.close();
                } catch (final IOException e) {
                    LOGGER.error("error while closing socket", e);
                }
            }
        }
    }

}
