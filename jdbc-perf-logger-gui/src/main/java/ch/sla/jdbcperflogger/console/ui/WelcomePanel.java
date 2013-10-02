package ch.sla.jdbcperflogger.console.ui;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.border.TitledBorder;

@SuppressWarnings("serial")
public class WelcomePanel extends JPanel {
    private final JTextField txtTargetHost;
    private final JTextField txtTargetPort;

    public WelcomePanel(final IClientConnectionDelegate clientConnectionCreator) {
        final GridBagLayout gridBagLayout = new GridBagLayout();
        gridBagLayout.columnWidths = new int[] { 438, 0 };
        gridBagLayout.rowHeights = new int[] { 46, 88, 0 };
        gridBagLayout.columnWeights = new double[] { 1.0, Double.MIN_VALUE };
        gridBagLayout.rowWeights = new double[] { 0.0, 0.0, Double.MIN_VALUE };
        setLayout(gridBagLayout);

        final JPanel serverModePanel = new JPanel();
        serverModePanel.setBorder(new TitledBorder("Server mode"));
        final GridBagConstraints gbc_serverModePanel = new GridBagConstraints();
        gbc_serverModePanel.anchor = GridBagConstraints.NORTH;
        gbc_serverModePanel.fill = GridBagConstraints.HORIZONTAL;
        gbc_serverModePanel.insets = new Insets(0, 0, 5, 0);
        gbc_serverModePanel.gridx = 0;
        gbc_serverModePanel.gridy = 0;
        add(serverModePanel, gbc_serverModePanel);
        final GridBagLayout gbl_serverModePanel = new GridBagLayout();
        gbl_serverModePanel.columnWidths = new int[] { 204, 32, 0 };
        gbl_serverModePanel.rowHeights = new int[] { 16, 0 };
        gbl_serverModePanel.columnWeights = new double[] { 0.0, 0.0, Double.MIN_VALUE };
        gbl_serverModePanel.rowWeights = new double[] { 0.0, Double.MIN_VALUE };
        serverModePanel.setLayout(gbl_serverModePanel);

        final JLabel lblWaitingForConnections = new JLabel("Waiting for connections on port:");
        final GridBagConstraints gbc_lblWaitingForConnections = new GridBagConstraints();
        gbc_lblWaitingForConnections.anchor = GridBagConstraints.NORTHWEST;
        gbc_lblWaitingForConnections.insets = new Insets(0, 0, 0, 5);
        gbc_lblWaitingForConnections.gridx = 0;
        gbc_lblWaitingForConnections.gridy = 0;
        serverModePanel.add(lblWaitingForConnections, gbc_lblWaitingForConnections);

        final JLabel serverPortLabel = new JLabel("4561");
        final GridBagConstraints gbc_serverPortLabel = new GridBagConstraints();
        gbc_serverPortLabel.anchor = GridBagConstraints.NORTHWEST;
        gbc_serverPortLabel.gridx = 1;
        gbc_serverPortLabel.gridy = 0;
        serverModePanel.add(serverPortLabel, gbc_serverPortLabel);

        final JPanel clientModePanel = new JPanel();
        clientModePanel.setBorder(new TitledBorder("Client mode"));
        final GridBagConstraints gbc_clientModePanel = new GridBagConstraints();
        gbc_clientModePanel.anchor = GridBagConstraints.NORTH;
        gbc_clientModePanel.fill = GridBagConstraints.HORIZONTAL;
        gbc_clientModePanel.gridx = 0;
        gbc_clientModePanel.gridy = 1;
        add(clientModePanel, gbc_clientModePanel);
        final GridBagLayout gbl_clientModePanel = new GridBagLayout();
        gbl_clientModePanel.columnWidths = new int[] { 30, 74, 292, 0 };
        gbl_clientModePanel.rowHeights = new int[] { 28, 30, 0 };
        gbl_clientModePanel.columnWeights = new double[] { 0.0, 0.0, 1.0, Double.MIN_VALUE };
        gbl_clientModePanel.rowWeights = new double[] { 0.0, 0.0, Double.MIN_VALUE };
        clientModePanel.setLayout(gbl_clientModePanel);

        final JButton btnConnect = new JButton("Connect");
        btnConnect.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                clientConnectionCreator.createClientConnection(txtTargetHost.getText(),
                        Integer.parseInt(txtTargetPort.getText()));
            }
        });

        final JLabel lblHost = new JLabel("Host");
        final GridBagConstraints gbc_lblHost = new GridBagConstraints();
        gbc_lblHost.anchor = GridBagConstraints.EAST;
        gbc_lblHost.insets = new Insets(0, 0, 5, 5);
        gbc_lblHost.gridx = 0;
        gbc_lblHost.gridy = 0;
        clientModePanel.add(lblHost, gbc_lblHost);

        txtTargetHost = new JTextField();
        txtTargetHost.setText("localhost");
        txtTargetHost.setColumns(10);
        final GridBagConstraints gbc_txtTargetHost = new GridBagConstraints();
        gbc_txtTargetHost.fill = GridBagConstraints.HORIZONTAL;
        gbc_txtTargetHost.anchor = GridBagConstraints.NORTH;
        gbc_txtTargetHost.insets = new Insets(0, 0, 5, 0);
        gbc_txtTargetHost.gridwidth = 2;
        gbc_txtTargetHost.gridx = 1;
        gbc_txtTargetHost.gridy = 0;
        clientModePanel.add(txtTargetHost, gbc_txtTargetHost);

        final JLabel lblPort = new JLabel("Port");
        final GridBagConstraints gbc_lblPort = new GridBagConstraints();
        gbc_lblPort.anchor = GridBagConstraints.EAST;
        gbc_lblPort.insets = new Insets(0, 0, 0, 5);
        gbc_lblPort.gridx = 0;
        gbc_lblPort.gridy = 1;
        clientModePanel.add(lblPort, gbc_lblPort);

        txtTargetPort = new JTextField();
        txtTargetPort.setText("8889");
        txtTargetPort.setColumns(5);
        final GridBagConstraints gbc_txtTargetPort = new GridBagConstraints();
        gbc_txtTargetPort.anchor = GridBagConstraints.NORTHWEST;
        gbc_txtTargetPort.insets = new Insets(0, 0, 0, 5);
        gbc_txtTargetPort.gridx = 1;
        gbc_txtTargetPort.gridy = 1;
        clientModePanel.add(txtTargetPort, gbc_txtTargetPort);
        btnConnect.setActionCommand("connect");
        final GridBagConstraints gbc_btnConnect = new GridBagConstraints();
        gbc_btnConnect.anchor = GridBagConstraints.SOUTHWEST;
        gbc_btnConnect.gridx = 2;
        gbc_btnConnect.gridy = 1;
        clientModePanel.add(btnConnect, gbc_btnConnect);
    }

}
