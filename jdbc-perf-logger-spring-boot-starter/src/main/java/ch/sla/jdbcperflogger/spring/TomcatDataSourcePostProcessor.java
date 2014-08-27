package ch.sla.jdbcperflogger.spring;

import org.apache.tomcat.jdbc.pool.DataSource;
import org.springframework.beans.BeansException;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.context.annotation.Configuration;

import ch.sla.jdbcperflogger.driver.WrappingDriver;

@ConditionalOnClass(DataSource.class)
@ConditionalOnExpression("${jdbcperflogger.enable:true}")
@Configuration
public class TomcatDataSourcePostProcessor extends AbstractDataSourcePostProcessor {

    @Override
    public Object postProcessBeforeInitialization(final Object bean, final String beanName) throws BeansException {
        if (bean instanceof DataSource) {
            final DataSource ds = (DataSource) bean;

            checkVisibleFromDataSource(DataSource.class);
            checkUnderlyingDriverIsVisible(ds.getDriverClassName());

            ds.setUrl("jdbcperflogger:" + ds.getUrl());
            ds.setDriverClassName(WrappingDriver.class.getName());
        }
        return bean;
    }

}
