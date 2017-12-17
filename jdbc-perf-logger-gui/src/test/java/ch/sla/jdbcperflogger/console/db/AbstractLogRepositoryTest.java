package ch.sla.jdbcperflogger.console.db;

import static java.sql.Connection.TRANSACTION_NONE;
import static java.util.UUID.randomUUID;
import static org.eclipse.jdt.annotation.DefaultLocation.PARAMETER;
import static org.eclipse.jdt.annotation.DefaultLocation.RETURN_TYPE;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Date;
import java.util.Properties;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.junit.After;
import org.junit.Before;

import ch.sla.jdbcperflogger.StatementType;
import ch.sla.jdbcperflogger.model.ConnectionInfo;
import ch.sla.jdbcperflogger.model.StatementLog;

@NonNullByDefault({ PARAMETER, RETURN_TYPE })
public class AbstractLogRepositoryTest {

    protected LogRepositoryUpdateJdbc repositoryUpdate;
    protected LogRepositoryRead repositoryRead;

    @Before
    public void setup() {
        repositoryUpdate = new LogRepositoryUpdateJdbc("test");
        repositoryRead = new LogRepositoryReadJdbc("test");
    }

    @After
    public void tearDown() {
        repositoryUpdate.dispose();
        repositoryRead.dispose();
    }

    protected ConnectionInfo insert1Connection() {
        final Properties connProps = new Properties();
        connProps.setProperty("myprop", "myval");
        final ConnectionInfo connectionInfo = new ConnectionInfo(randomUUID(), 12, "jdbc:toto", new Date(), 12,
                connProps);
        repositoryUpdate.addConnection(connectionInfo);
        return connectionInfo;
    }

    protected StatementLog insert1Log(final ConnectionInfo connectionInfo) {

        final StatementLog log = new StatementLog(connectionInfo.getUuid(), randomUUID(), System.currentTimeMillis(),
                StatementType.BASE_NON_PREPARED_STMT, "myrawsql", Thread.currentThread().getName(), 123, true, TRANSACTION_NONE);
        repositoryUpdate.addStatementLog(log);
        return log;
    }

    protected StatementLog insert1Log() {
        final ConnectionInfo connectionInfo = insert1Connection();
        return insert1Log(connectionInfo);
    }

    protected int countRowsInTable(final String table) {
        try (Statement stmt = repositoryUpdate.connectionUpdate.createStatement()) {
            try (ResultSet rset = stmt.executeQuery("select count(1) from " + table)) {
                rset.next();
                return rset.getInt(1);
            }
        } catch (final SQLException e) {
            throw new RuntimeException(e);
        }
    }

}