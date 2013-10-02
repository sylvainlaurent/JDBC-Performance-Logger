package ch.sla.jdbcperflogger.console.ui;

public interface IClientConnectionDelegate {
    void createClientConnection(String host, int port);

    void close(PerfLoggerPanel perLoggerPanel);
}
