package ch.sla.jdbcperflogger.logger;

import java.net.URL;
import java.net.URLClassLoader;

class MyClassLoader extends URLClassLoader {

    public MyClassLoader() {
        super(new URL[0]);
    }

}