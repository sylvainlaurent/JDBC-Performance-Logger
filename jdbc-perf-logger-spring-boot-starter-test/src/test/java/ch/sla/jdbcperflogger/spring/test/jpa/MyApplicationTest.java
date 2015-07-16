package ch.sla.jdbcperflogger.spring.test.jpa;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ch.sla.jdbcperflogger.logger.PerfLoggerRemoting;
import ch.sla.jdbcperflogger.logger.RecordingLogSender;
import ch.sla.jdbcperflogger.model.StatementLog;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(classes = MyApplication.class)
public class MyApplicationTest {
    private static final String SELECT_COUNT_FROM_PERSON = "SELECT COUNT(*) from PERSON";

    @Autowired
    private JdbcTemplate template;

    private final RecordingLogSender logRecorder = new RecordingLogSender();

    @Before
    public void setup() throws Exception {
        PerfLoggerRemoting.addSender(logRecorder);
    }

    @After
    public void tearDown() throws Exception {
        PerfLoggerRemoting.removeSender(logRecorder);
    }

    @SuppressWarnings("null")
    @Test
    public void testDefaultSettings() throws Exception {
        assertEquals(new Integer(1), template.queryForObject(SELECT_COUNT_FROM_PERSON, Integer.class));
        final StatementLog log = (StatementLog) logRecorder.lastLogMessage(2);
        assertTrue(log.getRawSql().contains(SELECT_COUNT_FROM_PERSON));
    }
}
