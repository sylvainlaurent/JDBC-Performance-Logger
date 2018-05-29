/*
 *  Copyright 2018 Matthias Mueller
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
package ch.sla.jdbcperflogger.driver;

import static org.awaitility.Awaitility.await;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

import org.junit.Test;

import ch.sla.jdbcperflogger.DriverConfig;

public class WrappingDriverUnloadTest {

    private static final int WAIT_TIME = 10;

    @SuppressWarnings("null")
    @Test
    public void testUnload() throws Exception {
        final Integer serverPort = DriverConfig.INSTANCE.getServerPort();
        final int clientPort = DriverConfig.INSTANCE.getClientAddresses().get(0).getPort();
        final ServerSocket logReceiver = new ServerSocket(clientPort);

        await().atMost(WAIT_TIME, TimeUnit.SECONDS).until(portInUse(clientPort));

        WrappingDriver.load();

        try {
            await().atMost(WAIT_TIME, TimeUnit.SECONDS).until(portInUse(serverPort));

            final Socket client = logReceiver.accept();
            final int remotePort = client.getPort();

            // following line is commented, not working on macOS
            // await().atMost(WAIT_TIME, TimeUnit.SECONDS).until(portInUse(remotePort));

            client.close();
            logReceiver.close();

            WrappingDriver.unload();
            await().atMost(WAIT_TIME, TimeUnit.SECONDS).until(portNotInUse(clientPort));
            await().atMost(WAIT_TIME, TimeUnit.SECONDS).until(portNotInUse(serverPort));
            await().atMost(WAIT_TIME, TimeUnit.SECONDS).until(portNotInUse(remotePort));
        } finally {
            WrappingDriver.load();
            await().atMost(WAIT_TIME, TimeUnit.SECONDS).until(portInUse(serverPort));
        }
    }

    protected Callable<Boolean> portInUse(final int port) {
        return () -> !canOpenPort(port);
    }

    protected Callable<Boolean> portNotInUse(final int port) {
        return () -> canOpenPort(port);
    }

    protected boolean canOpenPort(final int port) {
        try (
                ServerSocket serverSocket = new ServerSocket(port)) {
            return true;
        } catch (final IOException e) {
            return false;
        }
    }

}
