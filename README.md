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

## How to setup the JDBC Driver
- add the jdbc-logger-driver and slf4j-api jars to the classpath of the JDBC-client application
- Prefix your current JDBC URL with `jdbcperflogger:`, example: `jdbcperflogger:jdbc:h2:mem:` or `jdbcperflogger:jdbc:oracle:thin:@myhost:1521:orcl`
- (optional) add a `jdbcperflogger.xml` file to the classpath (see the example file for indications). If both the driver and console are used on the same machine, there's nothing to do: the driver will try to connect to the console on localhost:4561. 
- (optional) the location of the config file can be overriden with the System property `jdbcperflogger.config.location`. Example : `java -Djdbcperflogger.config.location=/Users/me/myjdbcperflogger.xml`

## How to use the graphical console
- launch `bin/jdbc-performance-logger-gui` (unix/MacOS) or `bin\jdbc-performance-logger-gui.bat`
- by default the console waits for connections from jdbc-logger-drivers on port 4561. All statements will be logged to the same tab
- The console can also connect to a jdbc-logger-driver instance on a specific host and port. A tab is created for each host/port combination.
- Once a tab is opened, the status of the connection is indicated at the bottom of the panel. If the connection is broken and was initiated by the console, the console will try to reconnect regularly. If the connection was initiated by the driver, the latter will try to reconnect regularly.

## Tested databases
- H2 (lightly)
- Oracle 10.2

## Current limitations
- No DataSource nor XADataSource class provided

## Source code
The source code is available on GitHub : https://github.com/sylvainlaurent/JDBC-Performance-Logger
