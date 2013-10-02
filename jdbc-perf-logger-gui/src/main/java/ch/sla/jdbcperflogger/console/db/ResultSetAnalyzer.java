package ch.sla.jdbcperflogger.console.db;

import java.sql.ResultSet;
import java.sql.SQLException;

public interface ResultSetAnalyzer {
    void analyze(ResultSet resultSet) throws SQLException;
}
