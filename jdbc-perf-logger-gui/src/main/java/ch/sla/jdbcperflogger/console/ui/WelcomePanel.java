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

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.TreeSet;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

import javax.annotation.Nullable;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.border.TitledBorder;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

@SuppressWarnings("serial")
public class WelcomePanel extends JPanel {
    private final JTextField txtTargetHost;
    private final JTextField txtTargetPort;
    private final JList<HostPort> recentConnectionsList;
    private final TreeSet<HostPort> recentConnections = new TreeSet<>();
    private final Preferences prefs = Preferences.userNodeForPackage(WelcomePanel.class);

    public WelcomePanel(final IClientConnectionDelegate clientConnectionCreator) {
        final GridBagLayout gridBagLayout = new GridBagLayout();
        gridBagLayout.columnWidths = new int[] { 438, 0 };
        gridBagLayout.rowHeights = new int[] { 46, 88, 0 };
        gridBagLayout.columnWeights = new double[] { 1.0, Double.MIN_VALUE };
        gridBagLayout.rowWeights = new double[] { 0.0, 1.0, Double.MIN_VALUE };
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
        gbl_clientModePanel.rowHeights = new int[] { 28, 30, 51, 0 };
        gbl_clientModePanel.columnWeights = new double[] { 0.0, 1.0, 1.0, Double.MIN_VALUE };
        gbl_clientModePanel.rowWeights = new double[] { 0.0, 0.0, 1.0, Double.MIN_VALUE };
        clientModePanel.setLayout(gbl_clientModePanel);

        final JButton btnConnect = new JButton("Connect");
        btnConnect.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(@Nullable final ActionEvent e) {
                final String targetHost = txtTargetHost.getText();
                if (targetHost != null) {
                    final int port = Integer.parseInt(txtTargetPort.getText());
                    addConnection(targetHost, port);
                    clientConnectionCreator.createClientConnection(targetHost, port);
                }
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
        gbc_lblPort.insets = new Insets(0, 0, 5, 5);
        gbc_lblPort.gridx = 0;
        gbc_lblPort.gridy = 1;
        clientModePanel.add(lblPort, gbc_lblPort);

        txtTargetPort = new JTextField();
        txtTargetPort.setText("8889");
        txtTargetPort.setColumns(5);
        final GridBagConstraints gbc_txtTargetPort = new GridBagConstraints();
        gbc_txtTargetPort.fill = GridBagConstraints.HORIZONTAL;
        gbc_txtTargetPort.anchor = GridBagConstraints.NORTH;
        gbc_txtTargetPort.insets = new Insets(0, 0, 5, 5);
        gbc_txtTargetPort.gridx = 1;
        gbc_txtTargetPort.gridy = 1;
        clientModePanel.add(txtTargetPort, gbc_txtTargetPort);
        btnConnect.setActionCommand("connect");
        final GridBagConstraints gbc_btnConnect = new GridBagConstraints();
        gbc_btnConnect.insets = new Insets(0, 0, 5, 0);
        gbc_btnConnect.anchor = GridBagConstraints.SOUTHWEST;
        gbc_btnConnect.gridx = 2;
        gbc_btnConnect.gridy = 1;
        clientModePanel.add(btnConnect, gbc_btnConnect);

        final JLabel lblRecentConnections = new JLabel("Recent connections");
        final GridBagConstraints gbc_lblRecentConnections = new GridBagConstraints();
        gbc_lblRecentConnections.anchor = GridBagConstraints.NORTH;
        gbc_lblRecentConnections.insets = new Insets(0, 0, 0, 5);
        gbc_lblRecentConnections.gridx = 0;
        gbc_lblRecentConnections.gridy = 2;
        clientModePanel.add(lblRecentConnections, gbc_lblRecentConnections);

        final JScrollPane recentConnectionsScrollPane = new JScrollPane();
        final GridBagConstraints gbc_recentConnectionsScrollPane = new GridBagConstraints();
        gbc_recentConnectionsScrollPane.fill = GridBagConstraints.BOTH;
        gbc_recentConnectionsScrollPane.gridwidth = 2;
        gbc_recentConnectionsScrollPane.gridx = 1;
        gbc_recentConnectionsScrollPane.gridy = 2;
        clientModePanel.add(recentConnectionsScrollPane, gbc_recentConnectionsScrollPane);

        recentConnectionsList = new JList<HostPort>();
        recentConnectionsList.addKeyListener(new KeyAdapter() {
            @Override
            public void keyTyped(@Nullable final KeyEvent e) {
                assert e != null;
                switch (e.getKeyChar()) {
                case KeyEvent.VK_DELETE:
                case KeyEvent.VK_BACK_SPACE:
                    final HostPort hostPort = getSelectedRecentConnection();
                    if (hostPort != null) {
                        removeRecentConnection(hostPort);
                    }
                }
            }
        });
        recentConnectionsList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        recentConnectionsList.addListSelectionListener(new ListSelectionListener() {
            @Override
            public void valueChanged(@Nullable final ListSelectionEvent e) {
                assert e != null;
                if (e.getValueIsAdjusting()) {
                    return;
                }
                final HostPort hostPort = getSelectedRecentConnection();
                if (hostPort != null) {
                    txtTargetHost.setText(hostPort.getHost());
                    txtTargetPort.setText(Integer.toString(hostPort.getPort()));
                }
            }

        });
        recentConnectionsScrollPane.setViewportView(recentConnectionsList);

        readPrefsForRecentConnections();
    }

    @Nullable
    private HostPort getSelectedRecentConnection() {
        final int idx = recentConnectionsList.getSelectedIndex();
        if (idx == -1) {
            return null;
        }
        final HostPort hostPort = recentConnectionsList.getModel().getElementAt(idx);
        return hostPort;
    }

    private void addConnection(final String host, final int port) {
        recentConnections.add(new HostPort(host, port));
        recentConnectionsList.setListData(recentConnections.toArray(new HostPort[recentConnections.size()]));

        prefs.putInt(host, port);
        try {
            prefs.sync();
        } catch (final BackingStoreException e) {
            throw new IllegalStateException(e);
        }
    }

    private void removeRecentConnection(final HostPort hostPort) {
        prefs.remove(hostPort.getHost());
        recentConnections.remove(hostPort);
        recentConnectionsList.setListData(recentConnections.toArray(new HostPort[recentConnections.size()]));
    }

    private void readPrefsForRecentConnections() {
        try {
            for (final String host : prefs.keys()) {
                final int port = prefs.getInt(host, -1);
                if (port != -1) {
                    addConnection(host, port);
                }
            }
        } catch (final BackingStoreException e) {
            throw new IllegalStateException(e);
        }
    }
}
