package slaurent.jdbcperflogger.gui;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ClientLogReceiver extends AbstractLogReceiver {
    final static Logger LOGGER = LoggerFactory.getLogger(ClientLogReceiver.class);

    final InetSocketAddress targetRemoteAddress;

    public ClientLogReceiver(String targetHost, int targetPort, LogRepository logRepository) {
        super(logRepository);
        targetRemoteAddress = InetSocketAddress.createUnresolved(targetHost, targetPort);
    }

    @Override
    public void run() {
        while (!disposed) {
            try {
                final InetSocketAddress addr = new InetSocketAddress(targetRemoteAddress.getHostName(),
                        targetRemoteAddress.getPort());
                LOGGER.debug("Trying to connect to {}", targetRemoteAddress);
                final Socket socket = new Socket(addr.getAddress(), addr.getPort());
                LOGGER.info("Connected to remote {}", targetRemoteAddress);
                handleConnection(socket);
            } catch (final IOException e) {
                LOGGER.debug("expected error", e);
            }
            LOGGER.debug("Sleeping before trying to connect again to remote {}", targetRemoteAddress);
            try {
                Thread.sleep(TimeUnit.SECONDS.toMillis(10));
            } catch (final InterruptedException e) {
            }

        }
    }

    @Override
    public boolean isServerMode() {
        return false;
    }
}
