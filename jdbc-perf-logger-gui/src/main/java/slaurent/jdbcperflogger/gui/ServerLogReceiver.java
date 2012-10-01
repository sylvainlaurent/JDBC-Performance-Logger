package slaurent.jdbcperflogger.gui;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ServerLogReceiver extends AbstractLogReceiver {
    final static Logger LOGGER = LoggerFactory.getLogger(ServerLogReceiver.class);

    private ServerSocket serverSocket;

    public ServerLogReceiver(int listenPort, LogRepository logRepository) {
        super(logRepository);
        try {
            serverSocket = new ServerSocket(listenPort);
            serverSocket.setSoTimeout(60 * 1000);
        } catch (final IOException e) {
            throw new RuntimeException(e);
        }
        this.setName("ServerLogReceiver " + listenPort);
    }

    // TODO : override mgt methods pour propager aux LogReceivers spawned par l'acceptor

    @Override
    public void dispose() {
        super.dispose();
        try {
            serverSocket.close();
        } catch (final IOException e) {
            LOGGER.error("error while closing socket", e);
        }
    }

    @Override
    public void run() {
        try {
            while (!disposed) {
                try {
                    LOGGER.debug("Waiting for client connections on " + serverSocket);
                    final Socket socket = serverSocket.accept();
                    LOGGER.debug("Got client connection from " + socket);

                    final AbstractLogReceiver logReceiver = new AbstractLogReceiver(logRepository) {
                        @Override
                        public void run() {
                            try {
                                handleConnection(socket);
                            } catch (final IOException e) {
                                LOGGER.error("error while receiving logs from " + socket.getRemoteSocketAddress(), e);
                            }
                        }

                    };
                    logReceiver.setName("LogReceiver " + socket.getRemoteSocketAddress());
                    logReceiver.start();
                } catch (final SocketTimeoutException e) {
                    LOGGER.debug("timeout while accepting socket", e);
                } catch (final IOException e) {
                    if (!disposed) {
                        LOGGER.error("error while accepting socket", e);
                    } else {
                        LOGGER.debug("error while accepting socket, the server has been closed", e);
                    }
                }
            }
        } finally {
            LOGGER.debug("Closing server socket " + serverSocket);
            if (!serverSocket.isClosed()) {
                try {
                    serverSocket.close();
                } catch (final IOException e) {
                    LOGGER.error("error while closing socket", e);
                }
            }
        }

    }
}
