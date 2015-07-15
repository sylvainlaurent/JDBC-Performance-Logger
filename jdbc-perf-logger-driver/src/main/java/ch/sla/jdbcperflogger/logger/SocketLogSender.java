package ch.sla.jdbcperflogger.logger;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.net.SocketException;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import ch.sla.jdbcperflogger.Logger;
import ch.sla.jdbcperflogger.model.BufferFullLogMessage;
import ch.sla.jdbcperflogger.model.ConnectionInfo;
import ch.sla.jdbcperflogger.model.LogMessage;

// public for tests
public class SocketLogSender implements Runnable, LogSender {
    private final static Logger LOGGER2 = Logger.getLogger(SocketLogSender.class);

    private final BlockingQueue<LogMessage> logsToSend = new LinkedBlockingQueue<LogMessage>(10000);
    private final Socket socket;
    private final AtomicBoolean queueFull = new AtomicBoolean();

    SocketLogSender(final Socket socket) throws SocketException {
        this.socket = socket;
        socket.setKeepAlive(true);
        socket.setSoTimeout((int) TimeUnit.SECONDS.toMillis(10));
    }

    /* (non-Javadoc)
     * @see ch.sla.jdbcperflogger.logger.LogSender#postLog(ch.sla.jdbcperflogger.model.LogMessage)
     */
    @Override
    public void postLog(final LogMessage log) {
        final boolean posted = logsToSend.offer(log);
        if (!posted) {
            queueFull.set(true);
            LOGGER2.warn("queue full, dropping remote log of statement");
        }
    }

    @Override
    public void run() {
        // first send all current connections information to the socket
        synchronized (PerfLoggerRemoting.connectionToInfo) {
            for (final ConnectionInfo connectionInfo : PerfLoggerRemoting.connectionToInfo.values()) {
                logsToSend.offer(connectionInfo);
            }
        }

        ObjectOutputStream oos = null;
        try {
            oos = new ObjectOutputStream(socket.getOutputStream());
            int cnt = 0;
            while (true) {
                try {
                    if (queueFull.compareAndSet(true, false)) {
                        oos.writeUnshared(new BufferFullLogMessage(System.currentTimeMillis()));
                    }

                    final LogMessage log = logsToSend.poll(10, TimeUnit.SECONDS);
                    if (log != null) {
                        oos.writeUnshared(log);
                    } else {
                        // check the socket state
                        if (socket.isClosed() || !socket.isConnected()) {
                            // client disconnected
                            break;
                        }
                        oos.writeUnshared(null);
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
            LOGGER2.warn("socket error", e);
        } finally {
            LOGGER2.info("closing connection with " + socket);
            PerfLoggerRemoting.senders.remove(this);
            if (oos != null) {
                try {
                    oos.close();
                } catch (final IOException ignored) {
                }
            }
            try {
                socket.close();
            } catch (final IOException e) {
                LOGGER2.error("error while closing socket", e);
            }
        }
    }
}