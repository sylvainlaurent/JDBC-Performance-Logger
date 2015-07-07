package ch.sla.jdbcperflogger.spring.test.mybatis;

import static org.junit.Assert.assertEquals;

import javax.sql.DataSource;

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
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(classes = MyBatisTest.Application.class)
public class MyBatisTest {
    @Autowired
    private PersonMapper personMapper;

    @Test
    public void test() throws Exception {
        assertEquals(1, personMapper.countPersons());
    }

    @Configuration
    @ComponentScan
    @EnableAutoConfiguration
    @MapperScan(basePackageClasses = PersonMapper.class)
    static class Application {
        @Bean
        public SqlSessionFactoryBean sqlSessionFactoryBean(final DataSource dataSource) {
            final SqlSessionFactoryBean sqlSessionFactoryBean = new SqlSessionFactoryBean();
            sqlSessionFactoryBean.setDataSource(dataSource);
            return sqlSessionFactoryBean;
        }
    }
}
