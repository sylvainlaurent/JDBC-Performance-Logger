package ch.sla.jdbcperflogger.logger;

import java.io.IOException;
import java.net.InetSocketAddress;

import org.junit.Assert;
import org.junit.Test;

public class PerfLoggerClientThreadTest {

    @Test
    public void testCCL() throws InterruptedException, IOException {
        final ClassLoader oldCcl = currentCcl();
        try {
            final MyClassLoader myClassLoader = new MyClassLoader();
            Thread.currentThread().setContextClassLoader(myClassLoader);

            final PerfLoggerClientThread thread = PerfLoggerClientThread.spawn(new InetSocketAddress("localhost", 0));
            Thread.sleep(1000);
            final ClassLoader classLoaderInsideThread = thread.getContextClassLoader();
            thread.done = true;
            thread.interrupt();
            thread.join();

            Assert.assertNotSame(myClassLoader, classLoaderInsideThread);
        } finally {
            Thread.currentThread().setContextClassLoader(oldCcl);
        }
    }

    private static ClassLoader currentCcl() {
        return Thread.currentThread().getContextClassLoader();
    }

}
