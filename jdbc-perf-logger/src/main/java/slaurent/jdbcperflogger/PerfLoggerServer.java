package slaurent.jdbcperflogger;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PerfLoggerServer extends Thread {
    private final static Logger LOGGER = LoggerFactory.getLogger(PerfLoggerServer.class);

    private ServerSocket serverSocket;
    private final Set<LogSenderThread> senders = new CopyOnWriteArraySet<PerfLoggerServer.LogSenderThread>();

    PerfLoggerServer(final int serverPort) {
        this.setDaemon(true);
        this.setName("PerfLoggerServer");
        try {
            serverSocket = new ServerSocket(serverPort);
        } catch (final IOException e) {
            throw new RuntimeException(e);
        }
    }

    void postLog(final LogMessage log) {
        for (final LogSenderThread sender : senders) {
            sender.postLog(log);
        }
    }

    @Override
    public void run() {
        try {
            while (true) {
                try {
                    final Socket socket = serverSocket.accept();
                    LOGGER.debug("Got client connection from " + socket);
                    final LogSenderThread sender = new LogSenderThread(socket);
                    senders.add(sender);
                    sender.start();
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

    class LogSenderThread extends Thread {
        private final Logger LOGGER = LoggerFactory.getLogger(LogSenderThread.class);

        private final BlockingQueue<LogMessage> logsToSend = new LinkedBlockingQueue<LogMessage>(10000);
        private final Socket socket;

        LogSenderThread(final Socket socket) throws SocketException {
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
