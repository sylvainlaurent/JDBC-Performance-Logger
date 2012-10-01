package slaurent.jdbcperflogger.gui;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.EventQueue;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.SwingConstants;
import javax.swing.ToolTipManager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

//TODO features
//Barre de menu : open/save DB, setup connections
//GUI pour choisir adresse remote

//inverser sockets client et serveur, selon config
//plusieurs panels, 1 seul receiver

public class PerfLoggerGui implements IClientConnectionDelegate {
    private final static Logger LOGGER = LoggerFactory.getLogger(PerfLoggerGui.class);

    private JFrame frmJdbcPerformanceLogger;
    private JTabbedPane tabbedPane;

    /**
     * Launch the application.
     */
    public static void main(String[] args) {
        LOGGER.debug("PerfLoggerGui starting...");
        EventQueue.invokeLater(new Runnable() {
            @Override
            public void run() {
                try {
                    final PerfLoggerGui window = new PerfLoggerGui();
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
    public PerfLoggerGui() {
        initialize();
    }

    /**
     * Initialize the contents of the frame.
     */
    private void initialize() {
        ToolTipManager.sharedInstance().setInitialDelay(500);

        frmJdbcPerformanceLogger = new JFrame();
        frmJdbcPerformanceLogger.setMinimumSize(new Dimension(600, 300));
        frmJdbcPerformanceLogger.setTitle("JDBC Performance Logger");
        frmJdbcPerformanceLogger.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        tabbedPane = new JTabbedPane(SwingConstants.TOP);
        frmJdbcPerformanceLogger.getContentPane().add(tabbedPane, BorderLayout.CENTER);

        final JPanel welcomePanel = new WelcomePanel(this);
        tabbedPane.addTab("Welcome", null, welcomePanel, null);

        // TODO make server port configurable
        final PerfLoggerPanel serverPerfLoggerPanel = createServer(4561);
        tabbedPane.addTab("*:4561", serverPerfLoggerPanel);

        frmJdbcPerformanceLogger.pack();
        frmJdbcPerformanceLogger.setMinimumSize(frmJdbcPerformanceLogger.getSize());
        frmJdbcPerformanceLogger.setBounds(100, 100, 900, 600);

    }

    @Override
    public void createClientConnection(String host, int port) {
        final PerfLoggerPanel clientPerfLoggerPanel = connectToClient(host, port);
        tabbedPane.addTab(host + ":" + port, clientPerfLoggerPanel);
    }

    @Override
    public void close(PerfLoggerPanel perfLoggerPanel) {
        tabbedPane.remove(perfLoggerPanel);
        perfLoggerPanel.dispose();
        perfLoggerPanel.getLogReceiver().dispose();
        perfLoggerPanel.getLogRepository().close();
    }

    private PerfLoggerPanel connectToClient(String targetHost, int targetPort) {
        final LogRepository logRepository = new LogRepository(targetHost + "_" + targetPort);
        final AbstractLogReceiver logReceiver = new ClientLogReceiver(targetHost, targetPort, logRepository);
        logReceiver.start();

        return new PerfLoggerPanel(logReceiver, logRepository, this);
    }

    private PerfLoggerPanel createServer(int listeningPort) {
        final LogRepository logRepository = new LogRepository("server_" + listeningPort);

        final AbstractLogReceiver logReceiver = new ServerLogReceiver(listeningPort, logRepository);
        logReceiver.start();

        return new PerfLoggerPanel(logReceiver, logRepository, this);
    }

}
