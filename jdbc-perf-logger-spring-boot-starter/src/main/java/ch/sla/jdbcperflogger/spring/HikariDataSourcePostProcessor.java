package ch.sla.jdbcperflogger.spring;

import ch.sla.jdbcperflogger.driver.WrappingDriver;
import com.zaxxer.hikari.HikariDataSource;
import org.springframework.beans.BeansException;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.context.annotation.Configuration;

@ConditionalOnClass(HikariDataSource.class)
@ConditionalOnExpression("${jdbcperflogger.enable:true}")
@Configuration
@AutoConfigureBefore(DataSourceAutoConfiguration.class)
public class HikariDataSourcePostProcessor extends AbstractDataSourcePostProcessor {

    @Override
    public Object postProcessBeforeInitialization(final Object bean, final String beanName) throws BeansException {
        if (bean instanceof HikariDataSource) {
            final HikariDataSource ds = (HikariDataSource) bean;
            // avoid to wrap an already wrapped datasource
            if (!ds.getJdbcUrl().startsWith(AbstractDataSourcePostProcessor.JDBC_URL_PREFIX)) {
                checkVisibleFromDataSource(HikariDataSource.class);
                checkUnderlyingDriverIsVisible(ds.getDriverClassName());

                ds.setJdbcUrl(JDBC_URL_PREFIX + ds.getJdbcUrl());
                ds.setDriverClassName(WrappingDriver.class.getName());
            }
        }
        return bean;
    }

}