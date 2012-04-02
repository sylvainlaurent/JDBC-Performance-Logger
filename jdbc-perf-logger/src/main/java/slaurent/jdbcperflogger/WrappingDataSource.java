package slaurent.jdbcperflogger;

import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.SQLException;

import javax.sql.DataSource;

public class WrappingDataSource implements DataSource {

    public Connection getConnection() throws SQLException {
        // TODO Auto-generated method stub
        return null;
    }

    public Connection getConnection(final String username, final String password) throws SQLException {
        // TODO Auto-generated method stub
        return null;
    }

    public PrintWriter getLogWriter() throws SQLException {
        // TODO Auto-generated method stub
        return null;
    }

    public void setLogWriter(final PrintWriter out) throws SQLException {
        // TODO Auto-generated method stub

    }

    public void setLoginTimeout(final int seconds) throws SQLException {
        // TODO Auto-generated method stub

    }

    public int getLoginTimeout() throws SQLException {
        // TODO Auto-generated method stub
        return 0;
    }

}
