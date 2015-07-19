# Spring-Boot starter for JDBC Performance Logger

If you are using [Spring Boot](http://projects.spring.io/spring-boot/), this module allows to quickly setup the performance logger with almost no configuration.

## Maven dependency
Add the following dependency to your `pom.xml` :
```xml
<dependency>
    <groupId>com.github.sylvainlaurent.jdbcperflogger</groupId>
    <artifactId>jdbc-perf-logger-spring-boot-starter</artifactId>
    <version>...</version>
</dependency>
```

(don't forget to set the correct version!)

This will automatically wrap the JDBC driver with the JDBC-perf-logger driver, as long as a supported DataSource is used, which is currenly :
- [Tomcat DataSource](http://tomcat.apache.org/tomcat-7.0-doc/jdbc-pool.html) (`org.apache.tomcat.jdbc.pool.DataSource`)
- [Commons DBCP 1 BasicDataSource](http://commons.apache.org/proper/commons-dbcp/) (`org.apache.commons.dbcp.BasicDataSource`)

## Spring-Boot configuration properties
These are Spring (Boot) properties, so they can e defined at various levels (application.properties, command line... see Spring-Boot doc).
 
- `jdbcperflogger.enable` : can be set to `false` to totally disable the logger (the JDBC driver will not be wrapped). Default value is `true`

## JDBC-perf-logger configuration
There's nothing specific to Spring-Boot here, so see the [base documentation](../README.md).

## When autoconfiguration might not work
In some situations, the DataSource bean is instantiated earlier than our BeanPostProcessor so that it does not have the opportunity to change the driver class and URL. In such cases, this `jdbc-perf-logger-spring-boot-starter` is useless and you'll need to set the driver class and connection URL as explained in the [base documentation](../README.md).

## Potential ClassLoader issues
If using the jar packaging of Spring-Boot, everything should work out of the box.

If using the classical war packaging, check the [base documentation](../README.md) (section _Potential ClassLoader issues_).
