package slaurent.jdbcperflogger.gui;

import java.sql.SQLException;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import slaurent.jdbcperflogger.StatementType;
import slaurent.jdbcperflogger.model.StatementLog;

public class LogRepositoryTest {
    private LogRepository repository;

    @Before
    public void setup() {
        repository = new LogRepository();
    }

    @After
    public void tearDown() {
        repository.close();
    }

    @Test
    public void testSetup() {
        // just test setup and teardown
    }

    @Test
    public void testInsertAndRead() {
        final StatementLog log = new StatementLog(123, UUID.randomUUID(), System.currentTimeMillis(),
                TimeUnit.MILLISECONDS.toNanos(256), StatementType.BASE_NON_PREPARED_STMT, "myrawsql", Thread
                        .currentThread().getName(), new SQLException());
        repository.addStatementLog(log);
        repository.addStatementLog(log);
        repository.getStatementLog(1);
    }
}
