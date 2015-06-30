package ch.sla.jdbcperflogger;

import java.util.logging.Level;

import org.eclipse.jdt.annotation.Nullable;

/**
 * Abstraction for logging.
 *
 * @author slaurent
 */
public class Logger {
    private final java.util.logging.Logger julLogger;

    private Logger(final java.util.logging.Logger julLogger) {
        this.julLogger = julLogger;
    }

    public static Logger getLogger(final Class<?> clazz) {
        return new Logger(java.util.logging.Logger.getLogger(clazz.getName()));
    }

    public static Logger getLogger(final String loggerName) {
        return new Logger(java.util.logging.Logger.getLogger(loggerName));
    }

    public void debug(final String msg) {
        julLogger.log(Level.FINE, msg);
    }

    public void debug(final String msg, final @Nullable Throwable exc) {
        julLogger.log(Level.FINE, msg, exc);

    }

    public void info(final String msg) {
        julLogger.log(Level.INFO, msg);

    }

    public void info(final String msg, final Throwable e) {
        julLogger.log(Level.INFO, msg, e);
    }

    public void warn(final String msg) {
        julLogger.log(Level.WARNING, msg);
    }

    public void warn(final String msg, final @Nullable Throwable e) {
        julLogger.log(Level.WARNING, msg, e);
    }

    public void error(final String msg) {
        julLogger.log(Level.SEVERE, msg);
    }

    public void error(final String msg, final Throwable e) {
        julLogger.log(Level.SEVERE, msg, e);
    }

    public boolean isDebugEnabled() {
        return julLogger.isLoggable(Level.FINE);
    }

}
