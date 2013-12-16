/* 
 *  Copyright 2013 Sylvain LAURENT
 *     
 *  Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package ch.sla.jdbcperflogger.logger;

import java.io.IOException;
import java.net.Socket;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.sla.jdbcperflogger.logger.PerfLoggerRemoting.LogSender;

class PerfLoggerClientThread extends Thread {
    private final static Logger LOGGER = LoggerFactory.getLogger(PerfLoggerClientThread.class);

    boolean done;

    private final String host;
    private final int port;

    static PerfLoggerClientThread spawn(final String host, final int port) {
        // avoid Classloader leaks

        return AccessController.doPrivileged(new PrivilegedAction<PerfLoggerClientThread>() {
            @Override
            public PerfLoggerClientThread run() {
                final ClassLoader savedClassLoader = Thread.currentThread().getContextClassLoader();
                try {
                    Thread.currentThread().setContextClassLoader(null);

                    final PerfLoggerClientThread thread = new PerfLoggerClientThread(host, port);
                    thread.start();
                    return thread;
                } finally {
                    Thread.currentThread().setContextClassLoader(savedClassLoader);
                }

            }
        });
    }

    private PerfLoggerClientThread(final String host, final int port) {
        this.setDaemon(true);
        this.setName("PerfLoggerClient " + host + ":" + port);
        this.host = host;
        this.port = port;
    }

    @Override
    public void run() {
        while (!done) {
            final Socket socket;
            try {
                socket = new Socket(host, port);
            } catch (final IOException e) {
                LOGGER.debug("Unable to connect to " + host + ":" + port + ", will try again later", e);
                quietSleep(30);
                continue;
            }
            LOGGER.debug("Connected to " + host + ":" + socket);
            try {
                final LogSender sender = new LogSender(socket);
                PerfLoggerRemoting.senders.add(sender);
                sender.run();
            } catch (final IOException e) {
                LOGGER.info("Error in connection with " + host + ":" + port + ", will try again later", e);
            }
        }
    }

    private void quietSleep(final int seconds) {
        try {
            Thread.sleep(TimeUnit.SECONDS.toMillis(seconds));
        } catch (final InterruptedException e) {
        }
    }
}