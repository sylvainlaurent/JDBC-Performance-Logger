package ch.sla.jdbcperflogger.spring.test.mybatis;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import javax.sql.DataSource;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mybatis.spring.SqlSessionFactoryBean;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ch.sla.jdbcperflogger.logger.PerfLoggerRemoting;
import ch.sla.jdbcperflogger.logger.RecordingLogSender;
import ch.sla.jdbcperflogger.model.StatementLog;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(classes = MyBatisTest.Application.class)
@ActiveProfiles("mybatis")
public class MyBatisTest {
    @Autowired
    private PersonMapper personMapper;
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
    public void test() throws Exception {
        assertEquals(1, personMapper.countPersons());
        final StatementLog log = (StatementLog) logRecorder.lastLogMessage(2);
        assertTrue(log.getRawSql().contains("select count(*) from person"));
    }

    @Configuration
    @ComponentScan
    @EnableAutoConfiguration
    @MapperScan(basePackageClasses = PersonMapper.class)
    static class Application {

        @Bean
        public SqlSessionFactoryBean sqlSessionFactory(final DataSource dataSource) throws Exception {
            final SqlSessionFactoryBean sqlSessionFactoryBean = new SqlSessionFactoryBean();
            sqlSessionFactoryBean.setDataSource(dataSource);
            return sqlSessionFactoryBean;
        }
    }
}
