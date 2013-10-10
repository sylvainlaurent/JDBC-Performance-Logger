package ch.sla.jdbcperflogger.console.db;

import java.sql.SQLException;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import javax.annotation.Nullable;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import ch.sla.jdbcperflogger.StatementType;
import ch.sla.jdbcperflogger.model.StatementLog;

public class LogRepositoryTest {
    @Nullable
    private LogRepository repository;

    @Before
    public void setup() {
        repository = new LogRepository("test");
    }

    @After
    public void tearDown() {
        final LogRepository repository2 = repository;
        if (repository2 != null) {
            repository2.dispose();
        }
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
        final LogRepository repository2 = repository;
        if (repository2 != null) {
            repository2.addStatementLog(log);
            repository2.addStatementLog(log);
            repository2.getStatementLog(1);
        }
    }
}
