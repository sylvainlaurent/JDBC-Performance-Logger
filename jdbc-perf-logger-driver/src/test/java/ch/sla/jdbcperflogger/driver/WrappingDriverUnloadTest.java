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

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

import org.junit.Assert;
import org.junit.Test;

import ch.sla.jdbcperflogger.DriverConfig;

public class WrappingDriverUnloadTest {

    @Test
    public void testUnload() throws Exception {
        WrappingDriver.load();
        try {
            final int clientPort = DriverConfig.INSTANCE.getClientAddresses().get(0).getPort();
            final ServerSocket logReceiver = new ServerSocket(clientPort);
            final Socket accept = logReceiver.accept();
            final int remotePort = accept.getPort();
            accept.close();
            logReceiver.close();

            WrappingDriver.unload();
            assertPortIsAvailable(clientPort);
            assertPortIsAvailable(remotePort);
            final Integer serverPort = DriverConfig.INSTANCE.getServerPort();
            if (null != serverPort) {
                assertPortIsAvailable(serverPort);
            }
        } finally {
            WrappingDriver.load();
        }
    }

    protected void assertPortIsAvailable(final Integer port) {
        ServerSocket s = null;
        boolean portAvailable = true;
        try {
            s = new ServerSocket(port);
        } catch (final IOException e) {
            portAvailable = false;
        } finally {
            if (s != null) {
                try {
                    s.close();
                } catch (final IOException e) {
                }
            }
        }
        Assert.assertTrue("Port " + port + " must be closed after WrappingDriver.unload()!", portAvailable);
    }

}
