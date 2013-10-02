package ch.sla.jdbcperflogger.console.ui;

import java.awt.BorderLayout;
import java.awt.Component;

import javax.swing.JFrame;
import javax.swing.JTabbedPane;
import javax.swing.SwingConstants;

@SuppressWarnings("serial")
public class PerfLoggerGuiMainFrame extends JFrame {

    private final JTabbedPane tabbedPane;

    /**
     * Create the frame.
     */
    public PerfLoggerGuiMainFrame() {
        this.setTitle("JDBC Performance Logger");
        // TODO handle clean exit
        this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        tabbedPane = new JTabbedPane(SwingConstants.TOP);
        this.getContentPane().add(tabbedPane, BorderLayout.CENTER);

        this.pack();
        // this.setMinimumSize(this.getSize());
        // this.setBounds(100, 100, 900, 600);
    }

    void addTab(String tabName, Component tab) {
        tabbedPane.add(tabName, tab);
    }

    void removeTab(Component tab) {
        tabbedPane.remove(tab);
    }

}
