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

    void addTab(final String tabName, final Component tab) {
        tabbedPane.add(tabName, tab);
        tabbedPane.setSelectedComponent(tab);
    }

    void removeTab(final Component tab) {
        tabbedPane.remove(tab);
    }

}
