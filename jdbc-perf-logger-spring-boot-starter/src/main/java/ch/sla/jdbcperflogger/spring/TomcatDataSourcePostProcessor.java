package ch.sla.jdbcperflogger.spring;

import org.apache.tomcat.jdbc.pool.DataSource;
import org.springframework.beans.BeansException;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.context.annotation.Configuration;

import ch.sla.jdbcperflogger.driver.WrappingDriver;

@ConditionalOnClass(DataSource.class)
@ConditionalOnExpression("${jdbcperflogger.enable:true}")
@Configuration
@AutoConfigureBefore(DataSourceAutoConfiguration.class)
public class TomcatDataSourcePostProcessor extends AbstractDataSourcePostProcessor {

    @Override
    public Object postProcessBeforeInitialization(final Object bean, final String beanName) throws BeansException {
        if (bean instanceof DataSource) {
            final DataSource ds = (DataSource) bean;
            // avoid to wrap an already wrapped datasource
            if (!ds.getUrl().startsWith(AbstractDataSourcePostProcessor.JDBC_URL_PREFIX)) {
                checkVisibleFromDataSource(DataSource.class);
                checkUnderlyingDriverIsVisible(ds.getDriverClassName());

                ds.setUrl(JDBC_URL_PREFIX + ds.getUrl());
                ds.setDriverClassName(WrappingDriver.class.getName());
            }
        }
        return bean;
    }

}
