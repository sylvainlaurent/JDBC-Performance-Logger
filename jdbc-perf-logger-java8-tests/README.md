# Java 8 support

The driver works with java 6 minimum but takes into account new methods introduced by java 8 / JDBC 4.2 like `executeLargeUpdate`.

Unit tests for java 8 new methods are implemented in a separate maven module that requires java 8.
This module is activated through a maven profile that is active only if the current JDK is at least 1.8.
