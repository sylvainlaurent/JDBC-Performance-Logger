package ch.sla.jdbcperflogger.console.ui;

import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.Frame;

import javax.swing.JPanel;
import javax.swing.ToolTipManager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.sla.jdbcperflogger.console.db.LogRepository;
import ch.sla.jdbcperflogger.console.net.AbstractLogReceiver;
import ch.sla.jdbcperflogger.console.net.ClientLogReceiver;
import ch.sla.jdbcperflogger.console.net.ServerLogReceiver;

public class PerfLoggerGuiMain implements IClientConnectionDelegate {
    private final static Logger LOGGER = LoggerFactory.getLogger(PerfLoggerGuiMain.class);

    private PerfLoggerGuiMainFrame frmJdbcPerformanceLogger;

    /**
     * Launch the application.
     */
    public static void main(String[] args) {
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
        initialize();
    }

    /**
     * Initialize the contents of the frame.
     */
    private void initialize() {
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
    public void createClientConnection(String host, int port) {
        final PerfLoggerController clientPerfLoggerController = connectToClient(host, port);
        frmJdbcPerformanceLogger.addTab(host + ":" + port, clientPerfLoggerController.getPanel());
    }

    @Override
    public void close(PerfLoggerPanel perfLoggerPanel) {
        frmJdbcPerformanceLogger.removeTab(perfLoggerPanel);
    }

    private PerfLoggerController connectToClient(String targetHost, int targetPort) {
        final LogRepository logRepository = new LogRepository(targetHost + "_" + targetPort);
        final AbstractLogReceiver logReceiver = new ClientLogReceiver(targetHost, targetPort, logRepository);
        logReceiver.start();

        return new PerfLoggerController(this, logReceiver, logRepository);
    }

    private PerfLoggerController createServer(int listeningPort) {
        final LogRepository logRepository = new LogRepository("server_" + listeningPort);

        final AbstractLogReceiver logReceiver = new ServerLogReceiver(listeningPort, logRepository);
        logReceiver.start();

        return new PerfLoggerController(this, logReceiver, logRepository);
    }

}
