package slaurent.jdbcperflogger.gui;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.net.Socket;
import java.net.SocketTimeoutException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import slaurent.jdbcperflogger.model.BatchedNonPreparedStatementsLog;
import slaurent.jdbcperflogger.model.BatchedPreparedStatementsLog;
import slaurent.jdbcperflogger.model.ResultSetLog;
import slaurent.jdbcperflogger.model.StatementLog;

public abstract class AbstractLogReceiver extends Thread {
    final static Logger LOGGER = LoggerFactory.getLogger(AbstractLogReceiver.class);

    protected final LogRepository logRepository;
    protected volatile boolean connected;
    protected volatile boolean paused = false;
    protected volatile boolean disposed = false;

    public AbstractLogReceiver(LogRepository logRepository) {
        this.logRepository = logRepository;
        this.setDaemon(true);
    }

    /**
     * @return the current number of connections with this log receiver
     */
    public int getConnectionsCount() {
        return connected ? 1 : 0;
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

    public void dispose() {
        disposed = true;
    }

    protected void handleConnection(final Socket socket) throws IOException {
        socket.setKeepAlive(true);
        socket.setSoTimeout(60 * 1000);

        final InputStream is = socket.getInputStream();
        final ObjectInputStream ois = new ObjectInputStream(is);

        try {
            connected = true;
            while (!disposed) {
                Object o;
                try {
                    o = ois.readObject();
                } catch (final ClassNotFoundException e) {
                    LOGGER.error(
                            "unknown class, maybe the client is not compatible with the GUI? the msg will be skipped",
                            e);
                    continue;
                } catch (final SocketTimeoutException e) {
                    LOGGER.trace("timeout while reading socket");
                    continue;
                }
                if (o == null || paused || disposed) {
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
            LOGGER.debug("Closing socket " + socket);
            socket.close();
        }

    }

    public abstract boolean isServerMode();
}