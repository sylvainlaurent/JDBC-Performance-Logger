package slaurent.jdbcperflogger.gui;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import slaurent.jdbcperflogger.model.BatchedNonPreparedStatementsLog;
import slaurent.jdbcperflogger.model.BatchedPreparedStatementsLog;
import slaurent.jdbcperflogger.model.ResultSetLog;
import slaurent.jdbcperflogger.model.StatementLog;

public class LogReceiver extends Thread {
    private final static Logger LOGGER = LoggerFactory.getLogger(LogReceiver.class);

    private final InetSocketAddress remoteAddress;
    private final LogRepository logRepository;

    private volatile boolean connected;
    private volatile boolean paused = false;

    public LogReceiver(InetSocketAddress remoteAddress, LogRepository logRepository) {
        this.remoteAddress = remoteAddress;
        this.logRepository = logRepository;
        this.setDaemon(true);
        this.setName("LogReceiver");

    }

    @Override
    public void run() {
        while (true) {
            try {
                final InetSocketAddress addr = new InetSocketAddress(remoteAddress.getHostName(),
                        remoteAddress.getPort());
                final Socket socket = new Socket(addr.getAddress(), addr.getPort());
                socket.setKeepAlive(true);
                final InputStream is = socket.getInputStream();
                final ObjectInputStream ois = new ObjectInputStream(is);
                LOGGER.info("Connected to remote {}", remoteAddress);
                try {
                    connected = true;
                    while (true) {
                        final Object o = ois.readObject();
                        if (o == null || paused) {
                            continue;
                        }
                        if (o instanceof StatementLog) {
                            logRepository.addStatementLog((StatementLog) o);
                        } else if (o instanceof ResultSetLog) {
                            logRepository.updateLogWithResultSetLog((ResultSetLog) o);
                        } else if (o instanceof BatchedNonPreparedStatementsLog) {
                            logRepository.addBatchedNonPreparedStatementsLog((BatchedNonPreparedStatementsLog) o);
                        } else if (o instanceof BatchedPreparedStatementsLog) {
                            logRepository.addBatchedPreparedStatementsLog((BatchedPreparedStatementsLog) o);
                        } else {
                            throw new IllegalArgumentException("unexpected log, class=" + o.getClass());
                        }
                    }
                } finally {
                    connected = false;
                    ois.close();
                    socket.close();
                }
            } catch (final IOException e) {
                LOGGER.debug("expected error", e);
            } catch (final Exception e) {
                LOGGER.error("unexpected error", e);
            }
            LOGGER.debug("Sleeping before trying to connect again to remote {}", remoteAddress);
            try {
                Thread.sleep(TimeUnit.SECONDS.toMillis(10));
            } catch (final InterruptedException e) {
            }

        }
    }

    public boolean isConnected() {
        return connected;
    }

    public void pauseReceivingLogs() {
        paused = true;
    }

    public void resumeReceivingLogs() {
        paused = false;
    }

    public boolean isPaused() {
        return paused;
    }
}
