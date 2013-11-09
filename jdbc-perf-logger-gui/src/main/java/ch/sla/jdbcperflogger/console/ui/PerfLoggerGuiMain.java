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
package ch.sla.jdbcperflogger.console.ui;

import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.Frame;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import javax.swing.JPanel;
import javax.swing.ToolTipManager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.sla.jdbcperflogger.console.db.LogRepositoryJdbc;
import ch.sla.jdbcperflogger.console.net.AbstractLogReceiver;
import ch.sla.jdbcperflogger.console.net.ClientLogReceiver;
import ch.sla.jdbcperflogger.console.net.ServerLogReceiver;

public class PerfLoggerGuiMain implements IClientConnectionDelegate {
    private final static Logger LOGGER = LoggerFactory.getLogger(PerfLoggerGuiMain.class);

    private final PerfLoggerGuiMainFrame frmJdbcPerformanceLogger;

    private final Map<String, PerfLoggerController> connectionsToLogController = new HashMap<>();

    /**
     * Launch the application.
     */
    public static void main(final String[] args) {
        LOGGER.debug("PerfLoggerGuiMain starting...");
        EventQueue.invokeLater(new Runnable() {
            @Override
            public void run() {
                try {
                    final PerfLoggerGuiMain window = new PerfLoggerGuiMain();
                    window.frmJdbcPerformanceLogger.setVisible(true);
                } catch (final Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }

    /**
     * Create the application.
     */
    public PerfLoggerGuiMain() {
        ToolTipManager.sharedInstance().setInitialDelay(500);

        frmJdbcPerformanceLogger = new PerfLoggerGuiMainFrame();

        final JPanel welcomePanel = new WelcomePanel(this);
        frmJdbcPerformanceLogger.addTab("Welcome", welcomePanel);

        // TODO make server port configurable
        final PerfLoggerController serverPerfLoggerController = createServer(4561);
        frmJdbcPerformanceLogger.addTab("*:4561", serverPerfLoggerController.getPanel());

        frmJdbcPerformanceLogger.pack();
        frmJdbcPerformanceLogger.setExtendedState(Frame.MAXIMIZED_BOTH);
        frmJdbcPerformanceLogger.setMinimumSize(new Dimension(600, 500));

    }

    @Override
    public void createClientConnection(final String host, final int port) {
        final PerfLoggerController clientPerfLoggerController = connectToClient(host, port);
        frmJdbcPerformanceLogger.addTab(host + ":" + port, clientPerfLoggerController.getPanel());
    }

    @Override
    public void close(final PerfLoggerController perfLoggerController) {
        frmJdbcPerformanceLogger.removeTab(perfLoggerController.getPanel());
        final Iterator<Entry<String, PerfLoggerController>> iterator = connectionsToLogController.entrySet().iterator();
        while (iterator.hasNext()) {
            final Entry<String, PerfLoggerController> entry = iterator.next();
            if (entry.getValue() == perfLoggerController) {
                iterator.remove();
            }
        }
    }

    private PerfLoggerController connectToClient(final String targetHost, final int targetPort) {
        final String hostAndPort = targetHost + "_" + targetPort;
        PerfLoggerController perfLoggerController = connectionsToLogController.get(hostAndPort);
        if (perfLoggerController == null) {
            final LogRepositoryJdbc logRepository = new LogRepositoryJdbc(hostAndPort);
            final AbstractLogReceiver logReceiver = new ClientLogReceiver(targetHost, targetPort, logRepository);
            logReceiver.start();

            perfLoggerController = new PerfLoggerController(this, logReceiver, logRepository);
            connectionsToLogController.put(hostAndPort, perfLoggerController);
        }
        return perfLoggerController;
    }

    private PerfLoggerController createServer(final int listeningPort) {
        final LogRepositoryJdbc logRepository = new LogRepositoryJdbc("server_" + listeningPort);

        final AbstractLogReceiver logReceiver = new ServerLogReceiver(listeningPort, logRepository);
        logReceiver.start();

        return new PerfLoggerController(this, logReceiver, logRepository);
    }

}
