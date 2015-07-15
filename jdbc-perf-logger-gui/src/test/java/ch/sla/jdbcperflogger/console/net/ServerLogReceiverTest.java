package ch.sla.jdbcperflogger.console.net;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.net.UnknownHostException;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import ch.sla.jdbcperflogger.console.db.LogRepositoryUpdateJdbc;

@SuppressWarnings("null")
public class ServerLogReceiverTest {
    private ServerLogReceiver receiver;
    @Mock
    private LogRepositoryUpdateJdbc repository;
    private ObjectOutputStream oos;
    private Socket clientSocket;

    @Before
    public void setUp() throws Exception {
        receiver = new ServerLogReceiver(0, repository);
        receiver.start();
        receiver.waitUntilServerIsReady();
    }

    @After
    public void teardown() throws Exception {
        if (oos != null) {
            oos.close();
        }
        if (clientSocket != null) {
            clientSocket.close();
        }
        receiver.dispose();
    }

    @Test
    public void testGetConnectionsCount() throws Exception {
        assertEquals(0, receiver.getConnectionsCount());
        connectToServer();
        Thread.sleep(1000);
        assertEquals(1, receiver.getConnectionsCount());
    }

    @Test
    public void testPauseReceivingLogs() throws Exception {
        assertFalse(receiver.isPaused());
        receiver.pauseReceivingLogs();
        assertTrue(receiver.isPaused());
        receiver.resumeReceivingLogs();
        assertFalse(receiver.isPaused());
    }

    private Socket connectToServer() throws UnknownHostException, IOException {
        clientSocket = new Socket("localhost", receiver.getListenPort());
        oos = new ObjectOutputStream(clientSocket.getOutputStream());
        oos.writeObject(null);
        return clientSocket;
    }

}
