package ch.sla.jdbcperflogger.spring;

import org.springframework.beans.BeansException;
import org.springframework.beans.FatalBeanException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.core.Ordered;

import ch.sla.jdbcperflogger.driver.WrappingDriver;

public abstract class AbstractDataSourcePostProcessor implements BeanPostProcessor, Ordered {
    protected static final String JDBC_URL_PREFIX = "jdbcperflogger:";

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }

    protected void checkUnderlyingDriverIsVisible(final String driverClassName) {
        try {
            Class.forName(driverClassName, true, WrappingDriver.class.getClassLoader());
        } catch (final ClassNotFoundException e) {
            throw new FatalBeanException("The JDBC driver class " + driverClassName
                    + " must be visible by the JDBC-perf-logger driver. Check your classpath");
        }
    }

    protected void checkVisibleFromDataSource(final Class<?> dataSourceClass) {
        try {
            Class.forName(WrappingDriver.class.getName(), true, dataSourceClass.getClassLoader());
        } catch (final ClassNotFoundException e) {
            throw new FatalBeanException("The JDBC-perf-logger driver class must be available to your dataSource class "
                    + dataSourceClass + ". Check your classpath");
        }
    }

    @Override
    public Object postProcessAfterInitialization(final Object bean, final String beanName) throws BeansException {
        return bean;
    }

}