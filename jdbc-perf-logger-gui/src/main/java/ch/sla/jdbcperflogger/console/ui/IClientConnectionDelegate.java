package ch.sla.jdbcperflogger.console.ui;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
public interface IClientConnectionDelegate {
    void createClientConnection(String host, int port);

    void close(PerfLoggerPanel perLoggerPanel);
}
