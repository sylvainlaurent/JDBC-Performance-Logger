package ch.sla.jdbcperflogger.logger;

import ch.sla.jdbcperflogger.model.LogMessage;

public interface LogSender {

    void postLog(LogMessage log);

}