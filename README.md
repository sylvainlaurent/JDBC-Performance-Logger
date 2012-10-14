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
- add the jdbc-logger-driver, slf4j-api jars and jdbcperflogger.xml file to the classpath of the JDBC-client application
- configure jdbcperflogger.xml (see the file for indications). If both the driver and console are used on the same machine and the default TCP port 4561 is OK, there's nothing to do. 

## Graphical console
- launch `bin/jdbc-performance-logger-gui` (unix/MacOS) or `bin\jdbc-performance-logger-gui.bat`
- by default the console waits for connections from jdbc-logger-drivers on port 4561. All statements will be logged to the same tab
- The console can also connect to a jdbc-logger-driver instance on a specific host and port. A tab is created for each host/port combination.
- Once a tab is opened, the status of the connection is indicated at the bottom of the panel. If the connection is broken and was initiated by the console, the console will try to reconnect regularly. If the connection was initiated by the driver, the latter will try to reconnect regularly.

## Source code
The source code is available on GitHub : https://github.com/sylvainlaurent/JDBC-Performance-Logger
