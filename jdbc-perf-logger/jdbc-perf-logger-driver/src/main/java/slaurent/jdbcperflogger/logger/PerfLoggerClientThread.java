package slaurent.jdbcperflogger.logger;

import java.io.IOException;
import java.net.Socket;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import slaurent.jdbcperflogger.logger.PerfLoggerRemoting.LogSender;

class PerfLoggerClientThread extends Thread {
    private final static Logger LOGGER = LoggerFactory.getLogger(PerfLoggerClientThread.class);

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
                PerfLoggerRemoting.senders.add(sender);
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