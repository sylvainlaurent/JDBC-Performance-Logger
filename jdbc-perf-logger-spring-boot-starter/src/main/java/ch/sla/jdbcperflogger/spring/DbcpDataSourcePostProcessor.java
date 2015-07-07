package ch.sla.jdbcperflogger.spring;

import org.apache.commons.dbcp.BasicDataSource;
import org.springframework.beans.BeansException;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.context.annotation.Configuration;

import ch.sla.jdbcperflogger.driver.WrappingDriver;

@ConditionalOnClass(BasicDataSource.class)
@ConditionalOnExpression("${jdbcperflogger.enable:true}")
@Configuration
public class DbcpDataSourcePostProcessor extends AbstractDataSourcePostProcessor {

    @Override
    public Object postProcessBeforeInitialization(final Object bean, final String beanName) throws BeansException {
        if (bean instanceof BasicDataSource) {
            final BasicDataSource ds = (BasicDataSource) bean;
            // avoid to wrap an already wrapped datasource
            if (!ds.getUrl().startsWith(AbstractDataSourcePostProcessor.JDBC_URL_PREFIX)) {

                checkVisibleFromDataSource(BasicDataSource.class);
                checkUnderlyingDriverIsVisible(ds.getDriverClassName());

                ds.setUrl("jdbcperflogger:" + ds.getUrl());
                ds.setDriverClassName(WrappingDriver.class.getName());
            }
        }
        return bean;
    }

}
