package ch.sla.jdbcperflogger.gui;

import java.sql.ResultSet;
import java.sql.SQLException;

public interface ResultSetAnalyzer {
	void analyze(ResultSet resultSet) throws SQLException;
}
