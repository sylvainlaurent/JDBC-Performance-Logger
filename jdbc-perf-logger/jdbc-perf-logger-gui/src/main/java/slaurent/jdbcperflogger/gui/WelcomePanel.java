package slaurent.jdbcperflogger.gui;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.GroupLayout;
import javax.swing.GroupLayout.Alignment;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.LayoutStyle.ComponentPlacement;
import javax.swing.border.TitledBorder;

public class WelcomePanel extends JPanel {
    private static final long serialVersionUID = 1L;
    private final JTextField txtTargetHost;
    private final JTextField txtTargetPort;

    public WelcomePanel(final IClientConnectionDelegate clientConnectionCreator) {

        final JPanel serverModePanel = new JPanel();
        serverModePanel.setBorder(new TitledBorder("Server mode"));

        final JPanel clientModePanel = new JPanel();
        clientModePanel.setBorder(new TitledBorder("Client mode"));

        final GroupLayout groupLayout = new GroupLayout(this);
        groupLayout.setHorizontalGroup(groupLayout.createParallelGroup(Alignment.LEADING).addGroup(
                groupLayout
                        .createSequentialGroup()
                        .addContainerGap()
                        .addGroup(
                                groupLayout
                                        .createParallelGroup(Alignment.LEADING)
                                        .addGroup(
                                                groupLayout
                                                        .createSequentialGroup()
                                                        .addComponent(serverModePanel, GroupLayout.DEFAULT_SIZE, 438,
                                                                Short.MAX_VALUE).addContainerGap())
                                        .addGroup(
                                                groupLayout
                                                        .createSequentialGroup()
                                                        .addComponent(clientModePanel, GroupLayout.DEFAULT_SIZE,
                                                                GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                                        .addContainerGap()))));
        groupLayout.setVerticalGroup(groupLayout.createParallelGroup(Alignment.LEADING).addGroup(
                groupLayout
                        .createSequentialGroup()
                        .addContainerGap()
                        .addComponent(serverModePanel, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE,
                                GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(ComponentPlacement.RELATED)
                        .addComponent(clientModePanel, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE,
                                GroupLayout.PREFERRED_SIZE).addGap(216)));

        final JLabel lblHost = new JLabel("Host");

        txtTargetHost = new JTextField();
        txtTargetHost.setText("localhost");
        txtTargetHost.setColumns(10);

        final JLabel lblPort = new JLabel("Port");

        txtTargetPort = new JTextField();
        txtTargetPort.setText("8889");
        txtTargetPort.setColumns(5);

        final JButton btnConnect = new JButton("Connect");
        btnConnect.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                clientConnectionCreator.createClientConnection(txtTargetHost.getText(),
                        Integer.parseInt(txtTargetPort.getText()));
            }
        });
        btnConnect.setActionCommand("connect");

        final GroupLayout gl_panel_1 = new GroupLayout(clientModePanel);
        gl_panel_1.setHorizontalGroup(gl_panel_1.createParallelGroup(Alignment.LEADING).addGroup(
                gl_panel_1
                        .createSequentialGroup()
                        .addContainerGap()
                        .addGroup(
                                gl_panel_1.createParallelGroup(Alignment.LEADING).addComponent(lblHost)
                                        .addComponent(lblPort))
                        .addGap(18)
                        .addGroup(
                                gl_panel_1
                                        .createParallelGroup(Alignment.LEADING)
                                        .addComponent(txtTargetHost, GroupLayout.DEFAULT_SIZE, 372, Short.MAX_VALUE)
                                        .addGroup(
                                                gl_panel_1
                                                        .createSequentialGroup()
                                                        .addComponent(txtTargetPort, GroupLayout.PREFERRED_SIZE,
                                                                GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                                                        .addPreferredGap(ComponentPlacement.UNRELATED)
                                                        .addComponent(btnConnect))).addContainerGap()));
        gl_panel_1.setVerticalGroup(gl_panel_1.createParallelGroup(Alignment.LEADING).addGroup(
                gl_panel_1
                        .createSequentialGroup()
                        .addGroup(
                                gl_panel_1
                                        .createParallelGroup(Alignment.BASELINE)
                                        .addComponent(lblHost)
                                        .addComponent(txtTargetHost, GroupLayout.PREFERRED_SIZE,
                                                GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE))
                        .addPreferredGap(ComponentPlacement.RELATED, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addGroup(
                                gl_panel_1
                                        .createParallelGroup(Alignment.BASELINE)
                                        .addComponent(lblPort)
                                        .addComponent(txtTargetPort, GroupLayout.PREFERRED_SIZE,
                                                GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                                        .addComponent(btnConnect))));
        clientModePanel.setLayout(gl_panel_1);

        final JLabel lblWaitingForConnections = new JLabel("Waiting for connections on port:");

        final JLabel serverPortLabel = new JLabel("4561");
        final GroupLayout gl_panel = new GroupLayout(serverModePanel);
        gl_panel.setHorizontalGroup(gl_panel.createParallelGroup(Alignment.LEADING).addGroup(
                gl_panel.createSequentialGroup().addContainerGap().addComponent(lblWaitingForConnections).addGap(5)
                        .addComponent(serverPortLabel)));
        gl_panel.setVerticalGroup(gl_panel.createParallelGroup(Alignment.LEADING).addGroup(
                gl_panel.createSequentialGroup()
                        .addContainerGap()
                        .addGroup(
                                gl_panel.createParallelGroup(Alignment.LEADING).addComponent(lblWaitingForConnections)
                                        .addComponent(serverPortLabel))));
        serverModePanel.setLayout(gl_panel);
        setLayout(groupLayout);
    }

}
