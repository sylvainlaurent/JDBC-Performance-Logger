package ch.sla.jdbcperflogger.logger;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.sla.jdbcperflogger.logger.PerfLoggerRemoting.LogSender;

class PerfLoggerServerThread extends Thread {
    private final static Logger LOGGER = LoggerFactory.getLogger(PerfLoggerServerThread.class);

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
                    PerfLoggerRemoting.senders.add(sender);
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