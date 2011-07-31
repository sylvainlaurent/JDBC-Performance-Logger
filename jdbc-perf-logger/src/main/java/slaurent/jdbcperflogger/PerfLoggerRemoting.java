package slaurent.jdbcperflogger;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
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

public class PerfLoggerRemoting {
    private final static Logger LOGGER = LoggerFactory.getLogger(PerfLoggerRemoting.class);

    private final static Set<LogSender> senders = new CopyOnWriteArraySet<PerfLoggerRemoting.LogSender>();
    static {

        final InputStream configFileStream = WrappingDriver.class.getResourceAsStream("/" + WrappingDriver.CONFIG_FILE);
        if (configFileStream == null) {
            LOGGER.warn("Cannot find " + WrappingDriver.CONFIG_FILE
                    + " in the classpath, no logging will be sent to a console");
        } else {
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
                LOGGER.warn("Error parsing " + WrappingDriver.CONFIG_FILE, e);
            }
        }

    }

    private PerfLoggerRemoting() {
    }

    static void postLog(final LogMessage log) {
        for (final LogSender sender : senders) {
            sender.postLog(log);
        }
    }

    static class PerfLoggerServerThread extends Thread {
        private ServerSocket serverSocket;

        PerfLoggerServerThread(final int serverPort) {
            this.setDaemon(true);
            this.setName("PerfLoggerServer acceptor port " + serverPort);
            try {
                serverSocket = new ServerSocket(serverPort);
            } catch (final IOException e) {
                throw new RuntimeException(e);
            }
        }

        @Override
        public void run() {
            try {
                while (true) {
                    try {
                        final Socket socket = serverSocket.accept();
                        LOGGER.debug("Got client connection from " + socket);
                        final LogSender sender = new LogSender(socket);
                        final Thread logSenderThread = new Thread(sender, "PerfLoggerServer " + socket.getInetAddress()
                                + ":" + socket.getPort());
                        logSenderThread.setDaemon(true);
                        logSenderThread.start();
                        senders.add(sender);
                    } catch (final IOException e) {
                        LOGGER.error("error while accepting socket", e);
                    }
                }
            } finally {
                try {
                    serverSocket.close();
                } catch (final IOException e) {
                    LOGGER.error("error while closing socket", e);
                }
            }
        }
    }

    static class PerfLoggerClientThread extends Thread {
        private final String host;
        private final int port;

        PerfLoggerClientThread(final String host, final int port) {
            this.setDaemon(true);
            this.setName("PerfLoggerClient " + host + ":" + port);
            this.host = host;
            this.port = port;
        }

        @Override
        public void run() {
            while (true) {
                final Socket socket;
                try {
                    socket = new Socket(host, port);
                } catch (final IOException e) {
                    LOGGER.debug("Unable to connect to " + host + ":" + port + ", will try again later", e);
                    quietSleep(30);
                    continue;
                }
                LOGGER.debug("Connected to " + host + ":" + socket);
                try {
                    final LogSender sender = new LogSender(socket);
                    senders.add(sender);
                    sender.run();
                } catch (final IOException e) {
                    LOGGER.info("Error in connection with " + host + ":" + port + ", will try again later", e);
                }
            }
        }

        private void quietSleep(final int seconds) {
            try {
                Thread.sleep(TimeUnit.SECONDS.toMillis(seconds));
            } catch (final InterruptedException e) {
            }
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
                            // avoid mem leak when the stream keeps back references to serialized objects
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
