package ch.sla.jdbcperflogger.gui;

public interface IClientConnectionDelegate {
    void createClientConnection(String host, int port);

    void close(PerfLoggerPanel perLoggerPanel);
}
