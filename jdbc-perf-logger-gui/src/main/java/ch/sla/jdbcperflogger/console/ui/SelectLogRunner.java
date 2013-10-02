package ch.sla.jdbcperflogger.console.ui;

import ch.sla.jdbcperflogger.console.db.ResultSetAnalyzer;

public interface SelectLogRunner {
    void doSelect(ResultSetAnalyzer resultSetAnalyzer);
}
