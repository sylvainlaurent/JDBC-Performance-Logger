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
import java.awt.Frame;
import java.util.HashMap;
import java.util.Map;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

import org.eclipse.jdt.annotation.Nullable;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.ToolTipManager;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.sla.jdbcperflogger.console.db.LogRepositoryRead;
import ch.sla.jdbcperflogger.console.db.LogRepositoryReadJdbc;
import ch.sla.jdbcperflogger.console.db.LogRepositoryUpdate;
import ch.sla.jdbcperflogger.console.db.LogRepositoryUpdateJdbc;
import ch.sla.jdbcperflogger.console.net.AbstractLogReceiver;
import ch.sla.jdbcperflogger.console.net.ClientLogReceiver;
import ch.sla.jdbcperflogger.console.net.ServerLogReceiver;

public class PerfLoggerGuiMain implements IClientConnectionDelegate {
    private static final String LOOK_AND_FEEL_CLASS_NAME_PREF_KEY = "lookAndFeelClassName";

    private final static Logger LOGGER = LoggerFactory.getLogger(PerfLoggerGuiMain.class);

    private final PerfLoggerGuiMainFrame frmJdbcPerformanceLogger;

    private final Map<String, PerfLoggerController> connectionsToLogController = new HashMap<>();
    private static final Preferences prefs = Preferences.userNodeForPackage(PerfLoggerGuiMain.class);

    /**
     * Launch the application.
     */
    public static void main(final String[] args) {
        LOGGER.debug("PerfLoggerGuiMain starting...");
        SwingUtilities.invokeLater(() -> {
            installLookAndFeel();

            try {
                final PerfLoggerGuiMain window = new PerfLoggerGuiMain();
                window.frmJdbcPerformanceLogger.setVisible(true);
            } catch (final Exception e) {
                e.printStackTrace();
            }
        });
    }

    private static void installLookAndFeel() {
        final String lfClassName = getPreferredLookAndFeel();
        try {
            if (lfClassName != null) {
                UIManager.setLookAndFeel(lfClassName);
            } else {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            }
        } catch (ClassNotFoundException | InstantiationException | IllegalAccessException
                | UnsupportedLookAndFeelException e) {
            LOGGER.warn("Error setting LookAndFeel", e);
        }
    }

    @Nullable
    static String getPreferredLookAndFeel() {
        return prefs.get(LOOK_AND_FEEL_CLASS_NAME_PREF_KEY, null);
    }

    static void savePreferredLookAndFeel(final @Nullable String lfClassName) {
        if (lfClassName == null) {
            prefs.remove(LOOK_AND_FEEL_CLASS_NAME_PREF_KEY);
        } else {
            prefs.put(LOOK_AND_FEEL_CLASS_NAME_PREF_KEY, lfClassName);
        }
        try {
            prefs.sync();
        } catch (final BackingStoreException e) {
            throw new IllegalArgumentException(e);
        }
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
        connectionsToLogController.entrySet().removeIf(entry -> entry.getValue() == perfLoggerController);
    }

    private PerfLoggerController connectToClient(final String targetHost, final int targetPort) {
        final String hostAndPort = targetHost + "_" + targetPort;
        PerfLoggerController perfLoggerController = connectionsToLogController.get(hostAndPort);
        if (perfLoggerController == null) {
            final LogRepositoryUpdate logRepositoryUpdate = new LogRepositoryUpdateJdbc(hostAndPort);
            final LogRepositoryRead logRepositoryRead = new LogRepositoryReadJdbc(hostAndPort);
            final AbstractLogReceiver logReceiver = new ClientLogReceiver(targetHost, targetPort, logRepositoryUpdate);
            logReceiver.start();

            perfLoggerController = new PerfLoggerController(this, logReceiver, logRepositoryUpdate, logRepositoryRead);
            connectionsToLogController.put(hostAndPort, perfLoggerController);
        }
        return perfLoggerController;
    }

    private PerfLoggerController createServer(final int listeningPort) {
        final LogRepositoryUpdate logRepositoryUpdate = new LogRepositoryUpdateJdbc("server_" + listeningPort);
        final LogRepositoryRead logRepositoryRead = new LogRepositoryReadJdbc("server_" + listeningPort);

        final AbstractLogReceiver logReceiver = new ServerLogReceiver(listeningPort, logRepositoryUpdate);
        logReceiver.start();

        return new PerfLoggerController(this, logReceiver, logRepositoryUpdate, logRepositoryRead);
    }

}
