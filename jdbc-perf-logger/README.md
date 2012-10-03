# JDBC Performance Logger

## Purpose
Measuring performance of SQL statements executed through JDBC.

## Why yet another project?
Although other tools already exist around JDBC performance monitoring ([log4jdbc](http://code.google.com/p/log4jdbc/), [P6Spy](http://sourceforge.net/projects/p6spy/), [JDbMonitor](http://www.jdbmonitor.com/)...), I did not find the features I was looking for : a GUI, measurement of statement execution and ResultSet iteration, cumulative measures...

## Features
 - Graphical console (Swing-based) with analysis feature
  - filter on statement text, minimum execution time
  - Group statements to count executions of identical statements and measure cumuated time
  - support for multiple connections
  - the connection between the monitored java application (JDBC proxy driver) and the console can be initiated from either side
 - Logging of bound values of prepared statements
 - separate measure of statement execution time and result set iteration time
 - Handling of batched statements
 - Logging of SQLExceptions

## JDBC Driver setup
 - add the jdbc-logger-driver and slf4j-api jars and jdbcperflogger.xml file to the classpath of the JDBC-client application
 - configure jdbcperflogger.xml (see the file for indications)

## Graphical console
- launch `bin/jdbc-performance-logger-gui[.bat]`