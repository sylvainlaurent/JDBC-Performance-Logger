package ch.sla.jdbcperflogger.console.db;

import static java.util.UUID.randomUUID;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Date;
import java.util.Properties;

import org.junit.After;
import org.junit.Before;

import ch.sla.jdbcperflogger.StatementType;
import ch.sla.jdbcperflogger.model.ConnectionInfo;
import ch.sla.jdbcperflogger.model.StatementLog;

@SuppressWarnings("null")
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

    protected StatementLog insert1Log() {
        final Properties connProps = new Properties();
        connProps.setProperty("myprop", "myval");
        final ConnectionInfo connectionInfo = new ConnectionInfo(randomUUID(), 12, "jdbc:toto", new Date(), connProps);
        repositoryUpdate.addConnection(connectionInfo);

        final StatementLog log = new StatementLog(connectionInfo.getUuid(), randomUUID(), System.currentTimeMillis(),
                StatementType.BASE_NON_PREPARED_STMT, "myrawsql", Thread.currentThread().getName(), 123, true);
        repositoryUpdate.addStatementLog(log);
        return log;
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