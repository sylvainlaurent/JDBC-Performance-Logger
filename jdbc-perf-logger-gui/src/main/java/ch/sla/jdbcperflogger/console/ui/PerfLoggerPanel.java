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

import java.awt.Color;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.KeyEventDispatcher;
import java.awt.KeyboardFocusManager;
import java.awt.SystemColor;
import java.awt.Toolkit;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Nullable;
import javax.swing.DefaultComboBoxModel;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.ScrollPaneConstants;
import javax.swing.border.EtchedBorder;
import javax.swing.border.TitledBorder;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.UndoableEditEvent;
import javax.swing.event.UndoableEditListener;

import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rsyntaxtextarea.SyntaxConstants;
import org.fife.ui.rsyntaxtextarea.SyntaxScheme;
import org.fife.ui.rsyntaxtextarea.TokenTypes;

import ch.sla.jdbcperflogger.console.db.LogRepositoryConstants;
import ch.sla.jdbcperflogger.console.ui.PerfLoggerController.FilterType;
import ch.sla.jdbcperflogger.console.ui.PerfLoggerController.GroupBy;

/**
 * @author slaurent
 * 
 */
@SuppressWarnings("serial")
public class PerfLoggerPanel extends JPanel {

    private static final Map<String, Integer> COLUMNS_WIDTH;

    static {
        COLUMNS_WIDTH = new HashMap<>();
        COLUMNS_WIDTH.put(LogRepositoryConstants.TSTAMP_COLUMN, 150);
        COLUMNS_WIDTH.put(LogRepositoryConstants.FETCH_TIME_COLUMN, 50);
        COLUMNS_WIDTH.put(LogRepositoryConstants.EXEC_TIME_COLUMN, 50);
        COLUMNS_WIDTH.put(LogRepositoryConstants.EXEC_PLUS_FETCH_TIME_COLUMN, 50);
        COLUMNS_WIDTH.put(LogRepositoryConstants.STMT_TYPE_COLUMN, 40);
        COLUMNS_WIDTH.put(LogRepositoryConstants.RAW_SQL_COLUMN, 350);
        COLUMNS_WIDTH.put(LogRepositoryConstants.FILLED_SQL_COLUMN, 200);
        COLUMNS_WIDTH.put(LogRepositoryConstants.THREAD_NAME_COLUMN, 200);
        COLUMNS_WIDTH.put(LogRepositoryConstants.EXEC_COUNT_COLUMN, 100);
        COLUMNS_WIDTH.put(LogRepositoryConstants.TOTAL_EXEC_TIME_COLUMN, 100);
        COLUMNS_WIDTH.put(LogRepositoryConstants.TIMEOUT_COLUMN, 70);
        COLUMNS_WIDTH.put(LogRepositoryConstants.ERROR_COLUMN, 0);
    }

    private JTextField txtFldSqlFilter;
    private JTextField txtFldMinDuration;
    CustomTable table;
    private ResultSetDataModel dataModel;
    private JComboBox<GroupBy> comboBoxGroupBy;
    private JComboBox<FilterType> comboBoxFilterType;
    private JButton btnClose;
    private JButton btnPause;

    RSyntaxTextArea txtFieldRawSql;
    RSyntaxTextArea txtFieldFilledSql;
    JLabel lblStatus;
    private StatementTimestampTableCellRenderer stmtTimestampCellRenderer;
    JTextField connectionUrlField;
    JTextField connectionCreationDateField;
    private JTextField sqlClauseField;
    JTextField connectionPropertiesField;

    public PerfLoggerPanel(final PerfLoggerController perfLoggerController) {

        dataModel = new ResultSetDataModel();
        final GridBagLayout gridBagLayout = new GridBagLayout();
        gridBagLayout.columnWidths = new int[] { 36, 0 };
        gridBagLayout.rowHeights = new int[] { 30, 316, 29, 0 };
        gridBagLayout.columnWeights = new double[] { 1.0, Double.MIN_VALUE };
        gridBagLayout.rowWeights = new double[] { 0.0, 1.0, 0.0, Double.MIN_VALUE };
        setLayout(gridBagLayout);

        final JPanel topPanel = new JPanel();
        final GridBagConstraints gbc_topPanel = new GridBagConstraints();
        gbc_topPanel.fill = GridBagConstraints.BOTH;
        gbc_topPanel.insets = new Insets(0, 0, 5, 0);
        gbc_topPanel.gridx = 0;
        gbc_topPanel.gridy = 0;
        add(topPanel, gbc_topPanel);
        final GridBagLayout gbl_topPanel = new GridBagLayout();
        gbl_topPanel.columnWidths = new int[] { 51, 0, 0, 0, 0 };
        gbl_topPanel.rowHeights = new int[] { 0, 0 };
        gbl_topPanel.columnWeights = new double[] { 1.0, 0.0, 0.0, 0.0, Double.MIN_VALUE };
        gbl_topPanel.rowWeights = new double[] { 0.0, Double.MIN_VALUE };
        topPanel.setLayout(gbl_topPanel);

        final JPanel filterPanel = new JPanel();
        filterPanel.setBorder(new TitledBorder(new EtchedBorder(EtchedBorder.LOWERED, null, null), "Filter",
                TitledBorder.LEADING, TitledBorder.TOP, null, null));
        final GridBagConstraints gbc_filterPanel = new GridBagConstraints();
        gbc_filterPanel.fill = GridBagConstraints.HORIZONTAL;
        gbc_filterPanel.insets = new Insets(0, 0, 0, 5);
        gbc_filterPanel.gridx = 0;
        gbc_filterPanel.gridy = 0;
        topPanel.add(filterPanel, gbc_filterPanel);
        final GridBagLayout gbl_filterPanel = new GridBagLayout();
        gbl_filterPanel.columnWidths = new int[] { 0, 51, 246, 0 };
        gbl_filterPanel.rowHeights = new int[] { 30, 0, 0 };
        gbl_filterPanel.columnWeights = new double[] { 0.0, 0.0, 1.0, Double.MIN_VALUE };
        gbl_filterPanel.rowWeights = new double[] { 0.0, 1.0, Double.MIN_VALUE };
        filterPanel.setLayout(gbl_filterPanel);

        comboBoxFilterType = new JComboBox<>();
        comboBoxFilterType.setModel(new DefaultComboBoxModel<>(EnumSet.allOf(FilterType.class).toArray(
                new FilterType[0])));
        comboBoxFilterType.setSelectedItem(FilterType.HIGHLIGHT);
        comboBoxFilterType.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(@Nullable final ActionEvent e) {
                perfLoggerController.setFilterType(comboBoxFilterType.getItemAt(comboBoxFilterType.getSelectedIndex()));
            }
        });
        final GridBagConstraints gbc_filterTypeComboBox = new GridBagConstraints();
        gbc_filterTypeComboBox.insets = new Insets(0, 0, 5, 5);
        gbc_filterTypeComboBox.fill = GridBagConstraints.HORIZONTAL;
        gbc_filterTypeComboBox.gridx = 0;
        gbc_filterTypeComboBox.gridy = 0;
        filterPanel.add(comboBoxFilterType, gbc_filterTypeComboBox);

        final JLabel lblText = new JLabel("Text:");
        final GridBagConstraints gbc_lblText = new GridBagConstraints();
        gbc_lblText.anchor = GridBagConstraints.BASELINE_TRAILING;
        gbc_lblText.insets = new Insets(0, 0, 5, 5);
        gbc_lblText.gridx = 1;
        gbc_lblText.gridy = 0;
        filterPanel.add(lblText, gbc_lblText);

        {
            txtFldSqlFilter = new JTextField();
            final GridBagConstraints gbc_txtFldSqlFilter = new GridBagConstraints();
            gbc_txtFldSqlFilter.anchor = GridBagConstraints.BASELINE;
            gbc_txtFldSqlFilter.fill = GridBagConstraints.HORIZONTAL;
            gbc_txtFldSqlFilter.insets = new Insets(0, 0, 5, 0);
            gbc_txtFldSqlFilter.gridx = 2;
            gbc_txtFldSqlFilter.gridy = 0;
            filterPanel.add(txtFldSqlFilter, gbc_txtFldSqlFilter);
            txtFldSqlFilter.setColumns(10);

            txtFldSqlFilter.getDocument().addUndoableEditListener(new UndoableEditListener() {
                @Override
                public void undoableEditHappened(@Nullable final UndoableEditEvent e) {
                    perfLoggerController.setTextFilter(txtFldSqlFilter.getText());
                }
            });
        }
        {

            final JPanel panel = new JPanel();
            final GridBagConstraints gbc_panel = new GridBagConstraints();
            gbc_panel.gridwidth = 3;
            gbc_panel.fill = GridBagConstraints.BOTH;
            gbc_panel.gridx = 0;
            gbc_panel.gridy = 1;
            filterPanel.add(panel, gbc_panel);
            final GridBagLayout gbl_panel = new GridBagLayout();
            gbl_panel.columnWidths = new int[] { 0, 0, 0, 0, 0, 0 };
            gbl_panel.rowHeights = new int[] { 0, 0 };
            gbl_panel.columnWeights = new double[] { 0.0, 1.0, 0.0, 0.0, 0.0, Double.MIN_VALUE };
            gbl_panel.rowWeights = new double[] { 0.0, Double.MIN_VALUE };
            panel.setLayout(gbl_panel);

            final JLabel lblSqlClause = new JLabel("Advanced filter");
            final GridBagConstraints gbc_lblSqlClause = new GridBagConstraints();
            gbc_lblSqlClause.anchor = GridBagConstraints.EAST;
            gbc_lblSqlClause.insets = new Insets(0, 0, 0, 5);
            gbc_lblSqlClause.gridx = 0;
            gbc_lblSqlClause.gridy = 0;
            panel.add(lblSqlClause, gbc_lblSqlClause);
            {
                sqlClauseField = new JTextField();
                sqlClauseField
                        .setToolTipText("<html>\n<p>Use this field to further filter statements by directly injecting a<br>\nWHERE clause to the SELECT statement used by the console<br>\nagainst its internal H2 database.</p>\n<p>You may use the column names that appear in the list below.<br>\nCaution: times are in nanoseconds in the internal DB<br>\nExamples:</p>\n<ul>\n<li>THREADNAME like 'Execute%'</li>\n<li>CONNECTIONNUMBER=2</li>\n<li>NBROWSITERATED>10</li>\n<li>ERROR=1</li>\n</ul>\n</html>");
                final GridBagConstraints gbc_sqlClauseField = new GridBagConstraints();
                gbc_sqlClauseField.insets = new Insets(0, 0, 0, 5);
                gbc_sqlClauseField.fill = GridBagConstraints.HORIZONTAL;
                gbc_sqlClauseField.gridx = 1;
                gbc_sqlClauseField.gridy = 0;
                panel.add(sqlClauseField, gbc_sqlClauseField);
                sqlClauseField.setColumns(10);
                sqlClauseField.getDocument().addUndoableEditListener(new UndoableEditListener() {
                    @Override
                    public void undoableEditHappened(@Nullable final UndoableEditEvent e) {
                        perfLoggerController.setSqlPassThroughFilter(sqlClauseField.getText());
                    }
                });
            }
            {
                final JLabel lblDurationms = new JLabel("Exec duration (ms) >=");
                final GridBagConstraints gbc_lblDurationms = new GridBagConstraints();
                gbc_lblDurationms.insets = new Insets(0, 0, 0, 5);
                gbc_lblDurationms.gridx = 2;
                gbc_lblDurationms.gridy = 0;
                panel.add(lblDurationms, gbc_lblDurationms);
            }
            txtFldMinDuration = new JTextField();
            final GridBagConstraints gbc_txtFldMinDuration = new GridBagConstraints();
            gbc_txtFldMinDuration.insets = new Insets(0, 0, 0, 5);
            gbc_txtFldMinDuration.gridx = 3;
            gbc_txtFldMinDuration.gridy = 0;
            panel.add(txtFldMinDuration, gbc_txtFldMinDuration);
            txtFldMinDuration.setColumns(5);

            final JCheckBox chckbxExcludeCommits = new JCheckBox("Exclude commits");
            final GridBagConstraints gbc_chckbxExcludeCommits = new GridBagConstraints();
            gbc_chckbxExcludeCommits.gridx = 4;
            gbc_chckbxExcludeCommits.gridy = 0;
            panel.add(chckbxExcludeCommits, gbc_chckbxExcludeCommits);
            chckbxExcludeCommits.addItemListener(new ItemListener() {
                @Override
                public void itemStateChanged(@SuppressWarnings("null") final ItemEvent e) {
                    perfLoggerController.setExcludeCommits(chckbxExcludeCommits.isSelected());
                }
            });
            txtFldMinDuration.getDocument().addUndoableEditListener(new UndoableEditListener() {
                @Override
                public void undoableEditHappened(@Nullable final UndoableEditEvent e) {
                    assert e != null;
                    Long minDurationMs = null;
                    if (txtFldMinDuration.getText().length() > 0) {
                        try {
                            minDurationMs = new BigDecimal(txtFldMinDuration.getText()).longValue();
                        } catch (final NumberFormatException exc) {
                            e.getEdit().undo();
                            return;
                        }
                    }
                    perfLoggerController.setMinDurationFilter(minDurationMs);
                }
            });
        }

        final JPanel groupByPanel = new JPanel();
        groupByPanel.setBorder(new TitledBorder(new EtchedBorder(EtchedBorder.LOWERED, null, null), "Group by",
                TitledBorder.LEADING, TitledBorder.TOP, null, null));
        final GridBagConstraints gbc_groupByPanel = new GridBagConstraints();
        gbc_groupByPanel.fill = GridBagConstraints.BOTH;
        gbc_groupByPanel.insets = new Insets(0, 0, 0, 5);
        gbc_groupByPanel.gridx = 1;
        gbc_groupByPanel.gridy = 0;
        topPanel.add(groupByPanel, gbc_groupByPanel);
        final GridBagLayout gbl_groupByPanel = new GridBagLayout();
        gbl_groupByPanel.columnWidths = new int[] { 0, 0 };
        gbl_groupByPanel.rowHeights = new int[] { 0, 0 };
        gbl_groupByPanel.columnWeights = new double[] { 0.0, Double.MIN_VALUE };
        gbl_groupByPanel.rowWeights = new double[] { 0.0, Double.MIN_VALUE };
        groupByPanel.setLayout(gbl_groupByPanel);

        comboBoxGroupBy = new JComboBox<>();
        final GridBagConstraints gbc_comboBoxGroupBy = new GridBagConstraints();
        gbc_comboBoxGroupBy.fill = GridBagConstraints.HORIZONTAL;
        gbc_comboBoxGroupBy.gridx = 0;
        gbc_comboBoxGroupBy.gridy = 0;
        groupByPanel.add(comboBoxGroupBy, gbc_comboBoxGroupBy);
        comboBoxGroupBy.setModel(new DefaultComboBoxModel<>(EnumSet.allOf(GroupBy.class).toArray(new GroupBy[0])));
        comboBoxGroupBy.setSelectedIndex(0);
        comboBoxGroupBy.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(@Nullable final ActionEvent e) {
                perfLoggerController.setGroupBy(comboBoxGroupBy.getItemAt(comboBoxGroupBy.getSelectedIndex()));
            }
        });

        btnPause = new JButton();
        btnPause.setBorder(null);
        btnPause.setContentAreaFilled(false);
        final GridBagConstraints gbc_btnPause = new GridBagConstraints();
        gbc_btnPause.insets = new Insets(0, 0, 0, 5);
        gbc_btnPause.gridx = 2;
        gbc_btnPause.gridy = 0;
        topPanel.add(btnPause, gbc_btnPause);
        btnPause.setBorderPainted(false);
        btnPause.setIcon(new ImageIcon(PerfLoggerPanel.class.getResource("/icons/32px-Media-playback-pause.png")));
        btnPause.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(@Nullable final ActionEvent e) {
                perfLoggerController.onPause();
            }
        });

        final JButton btnClear = new JButton();
        btnClear.setBorder(null);
        btnClear.setBorderPainted(false);
        btnClear.setContentAreaFilled(false);
        final GridBagConstraints gbc_btnClear = new GridBagConstraints();
        gbc_btnClear.gridx = 3;
        gbc_btnClear.gridy = 0;
        topPanel.add(btnClear, gbc_btnClear);
        btnClear.setIcon(new ImageIcon(PerfLoggerPanel.class.getResource("/icons/32px-Edit-clear.png")));
        btnClear.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(@Nullable final ActionEvent e) {
                perfLoggerController.onClear();
            }
        });

        final JSplitPane splitPane = new JSplitPane();
        splitPane.setResizeWeight(0.8);
        splitPane.setOneTouchExpandable(true);
        splitPane.setBorder(null);
        splitPane.setContinuousLayout(true);
        splitPane.setOrientation(JSplitPane.VERTICAL_SPLIT);

        final JScrollPane logListPanel = new JScrollPane();
        logListPanel.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
        table = new CustomTable(dataModel);
        table.setSelectionForeground(SystemColor.textHighlightText);
        table.setSelectionBackground(SystemColor.textHighlight);
        table.setDefaultRenderer(Byte.class, new CustomTableCellRenderer());
        table.setDefaultRenderer(String.class, new CustomTableCellRenderer());
        stmtTimestampCellRenderer = new StatementTimestampTableCellRenderer();
        table.setDefaultRenderer(Timestamp.class, stmtTimestampCellRenderer);
        table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        table.setFillsViewportHeight(true);
        table.setSelectionMode(ListSelectionModel.SINGLE_INTERVAL_SELECTION);
        table.setAutoCreateRowSorter(true);
        logListPanel.setViewportView(table);

        table.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
            @Override
            public void valueChanged(@Nullable final ListSelectionEvent e) {
                assert e != null;
                if (!e.getValueIsAdjusting()) {
                    final ListSelectionModel lsm = (ListSelectionModel) e.getSource();
                    if (lsm.getMinSelectionIndex() >= 0) {
                        final long logId = dataModel.getIdAtRow(table.convertRowIndexToModel(lsm.getMinSelectionIndex()));
                        perfLoggerController.onSelectStatement(logId);
                    } else {
                        perfLoggerController.onSelectStatement(null);
                    }
                }
            }
        });
        table.addKeyListener(new KeyAdapter() {

            @Override
            public void keyReleased(@Nullable final KeyEvent e) {
                assert e != null;
                if (e.getKeyCode() == java.awt.event.KeyEvent.VK_BACK_SPACE) {
                    final int[] selectedRowsTableIndexes = table.getSelectedRows();
                    final long[] logIds = new long[selectedRowsTableIndexes.length];
                    for (int i = 0; i < selectedRowsTableIndexes.length; i++) {
                        logIds[i] = dataModel.getIdAtRow(table.convertRowIndexToModel(selectedRowsTableIndexes[i]));
                    }
                    perfLoggerController.onDeleteSelectedStatements(logIds);
                }

            }

        });
        splitPane.setTopComponent(logListPanel);

        final JPanel sqlDetailPanel = new JPanel();
        sqlDetailPanel.setBorder(new TitledBorder(null, "SQL detail", TitledBorder.LEADING, TitledBorder.TOP, null,
                null));
        splitPane.setBottomComponent(sqlDetailPanel);
        final GridBagLayout gbl_sqlDetailPanel = new GridBagLayout();
        gbl_sqlDetailPanel.columnWidths = new int[] { 842, 0 };
        gbl_sqlDetailPanel.rowHeights = new int[] { 112, 0 };
        gbl_sqlDetailPanel.columnWeights = new double[] { 1.0, Double.MIN_VALUE };
        gbl_sqlDetailPanel.rowWeights = new double[] { 1.0, Double.MIN_VALUE };
        sqlDetailPanel.setLayout(gbl_sqlDetailPanel);

        final JSplitPane splitPaneSqlDetails = new JSplitPane();
        splitPaneSqlDetails.setResizeWeight(0.5);
        splitPaneSqlDetails.setContinuousLayout(true);
        splitPaneSqlDetails.setOrientation(JSplitPane.VERTICAL_SPLIT);
        final GridBagConstraints gbc_splitPaneSqlDetails = new GridBagConstraints();
        gbc_splitPaneSqlDetails.fill = GridBagConstraints.BOTH;
        gbc_splitPaneSqlDetails.gridx = 0;
        gbc_splitPaneSqlDetails.gridy = 0;
        sqlDetailPanel.add(splitPaneSqlDetails, gbc_splitPaneSqlDetails);

        final JPanel panelRawSql = new JPanel();
        splitPaneSqlDetails.setLeftComponent(panelRawSql);
        final GridBagLayout gbl_panelRawSql = new GridBagLayout();
        gbl_panelRawSql.columnWidths = new int[] { 0, 0, 0 };
        gbl_panelRawSql.rowHeights = new int[] { 0, 0, 0 };
        gbl_panelRawSql.columnWeights = new double[] { 0.0, 1.0, Double.MIN_VALUE };
        gbl_panelRawSql.rowWeights = new double[] { 0.0, 1.0, Double.MIN_VALUE };
        panelRawSql.setLayout(gbl_panelRawSql);

        final JPanel connectinInfoPanel = new JPanel();
        final GridBagConstraints gbc_connectinInfoPanel = new GridBagConstraints();
        gbc_connectinInfoPanel.gridwidth = 2;
        gbc_connectinInfoPanel.insets = new Insets(0, 0, 5, 0);
        gbc_connectinInfoPanel.fill = GridBagConstraints.BOTH;
        gbc_connectinInfoPanel.gridx = 0;
        gbc_connectinInfoPanel.gridy = 0;
        panelRawSql.add(connectinInfoPanel, gbc_connectinInfoPanel);
        final GridBagLayout gbl_connectinInfoPanel = new GridBagLayout();
        gbl_connectinInfoPanel.columnWidths = new int[] { 0, 0, 0, 0, 0 };
        gbl_connectinInfoPanel.rowHeights = new int[] { 0, 0, 0 };
        gbl_connectinInfoPanel.columnWeights = new double[] { 0.0, 1.0, 0.0, 0.0, Double.MIN_VALUE };
        gbl_connectinInfoPanel.rowWeights = new double[] { 0.0, 0.0, Double.MIN_VALUE };
        connectinInfoPanel.setLayout(gbl_connectinInfoPanel);

        final JLabel lblConnectionUrl = new JLabel("Connection URL:");
        final GridBagConstraints gbc_lblConnectionUrl = new GridBagConstraints();
        gbc_lblConnectionUrl.insets = new Insets(5, 0, 5, 5);
        gbc_lblConnectionUrl.anchor = GridBagConstraints.EAST;
        gbc_lblConnectionUrl.gridx = 0;
        gbc_lblConnectionUrl.gridy = 0;
        connectinInfoPanel.add(lblConnectionUrl, gbc_lblConnectionUrl);

        connectionUrlField = new JTextField();
        final GridBagConstraints gbc_connectionUrlField = new GridBagConstraints();
        gbc_connectionUrlField.insets = new Insets(5, 0, 5, 5);
        gbc_connectionUrlField.fill = GridBagConstraints.HORIZONTAL;
        gbc_connectionUrlField.gridx = 1;
        gbc_connectionUrlField.gridy = 0;
        connectinInfoPanel.add(connectionUrlField, gbc_connectionUrlField);
        connectionUrlField.setColumns(20);

        final JLabel lblCreated = new JLabel("Created:");
        final GridBagConstraints gbc_lblCreated = new GridBagConstraints();
        gbc_lblCreated.anchor = GridBagConstraints.EAST;
        gbc_lblCreated.insets = new Insets(5, 0, 5, 5);
        gbc_lblCreated.gridx = 2;
        gbc_lblCreated.gridy = 0;
        connectinInfoPanel.add(lblCreated, gbc_lblCreated);

        connectionCreationDateField = new JTextField();
        final GridBagConstraints gbc_connectionCreationDateField = new GridBagConstraints();
        gbc_connectionCreationDateField.insets = new Insets(5, 0, 5, 0);
        gbc_connectionCreationDateField.fill = GridBagConstraints.HORIZONTAL;
        gbc_connectionCreationDateField.gridx = 3;
        gbc_connectionCreationDateField.gridy = 0;
        connectinInfoPanel.add(connectionCreationDateField, gbc_connectionCreationDateField);
        connectionCreationDateField.setColumns(15);

        final JLabel lblConectionProperties = new JLabel("Connection Properties:");
        lblConectionProperties.setToolTipText("(Password property removed)");
        final GridBagConstraints gbc_lblConectionProperties = new GridBagConstraints();
        gbc_lblConectionProperties.anchor = GridBagConstraints.EAST;
        gbc_lblConectionProperties.insets = new Insets(0, 5, 5, 5);
        gbc_lblConectionProperties.gridx = 0;
        gbc_lblConectionProperties.gridy = 1;
        connectinInfoPanel.add(lblConectionProperties, gbc_lblConectionProperties);

        connectionPropertiesField = new JTextField();
        final GridBagConstraints gbc_connectionPropertiesField = new GridBagConstraints();
        gbc_connectionPropertiesField.insets = new Insets(0, 0, 5, 0);
        gbc_connectionPropertiesField.gridwidth = 3;
        gbc_connectionPropertiesField.fill = GridBagConstraints.HORIZONTAL;
        gbc_connectionPropertiesField.gridx = 1;
        gbc_connectionPropertiesField.gridy = 1;
        connectinInfoPanel.add(connectionPropertiesField, gbc_connectionPropertiesField);
        connectionPropertiesField.setColumns(10);

        final JButton btnCopy1 = new JButton();
        btnCopy1.setBorderPainted(false);
        btnCopy1.setBorder(null);
        btnCopy1.setContentAreaFilled(false);
        final GridBagConstraints gbc_btnCopy1 = new GridBagConstraints();
        gbc_btnCopy1.insets = new Insets(0, 0, 0, 5);
        gbc_btnCopy1.gridx = 0;
        gbc_btnCopy1.gridy = 1;
        panelRawSql.add(btnCopy1, gbc_btnCopy1);
        btnCopy1.setIcon(new ImageIcon(PerfLoggerPanel.class.getResource("/icons/32px-Edit-copy_purple.png")));
        btnCopy1.setToolTipText("Copy the SQL statement unmodified (potentiall with '?' for bind variables");
        btnCopy1.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(@Nullable final ActionEvent e) {
                final StringSelection stringSelection = new StringSelection(txtFieldRawSql.getText());
                Toolkit.getDefaultToolkit().getSystemClipboard().setContents(stringSelection, stringSelection);
            }
        });

        final JScrollPane scrollPaneRawSql = new JScrollPane();
        final GridBagConstraints gbc_scrollPaneRawSql = new GridBagConstraints();
        gbc_scrollPaneRawSql.fill = GridBagConstraints.BOTH;
        gbc_scrollPaneRawSql.gridx = 1;
        gbc_scrollPaneRawSql.gridy = 1;
        panelRawSql.add(scrollPaneRawSql, gbc_scrollPaneRawSql);

        txtFieldRawSql = new RSyntaxTextArea();
        scrollPaneRawSql.setViewportView(txtFieldRawSql);
        applySqlSyntaxColoring(txtFieldRawSql);
        txtFieldRawSql.setOpaque(false);
        txtFieldRawSql.setEditable(false);
        txtFieldRawSql.setLineWrap(true);

        final JPanel panelFilledSql = new JPanel();
        splitPaneSqlDetails.setRightComponent(panelFilledSql);
        final GridBagLayout gbl_panelFilledSql = new GridBagLayout();
        gbl_panelFilledSql.columnWidths = new int[] { 0, 0, 0 };
        gbl_panelFilledSql.rowHeights = new int[] { 0, 0 };
        gbl_panelFilledSql.columnWeights = new double[] { 0.0, 1.0, Double.MIN_VALUE };
        gbl_panelFilledSql.rowWeights = new double[] { 1.0, Double.MIN_VALUE };
        panelFilledSql.setLayout(gbl_panelFilledSql);
        final JButton btnCopy2 = new JButton();
        btnCopy2.setBorder(null);
        btnCopy2.setBorderPainted(false);
        btnCopy2.setContentAreaFilled(false);
        final GridBagConstraints gbc_btnCopy2 = new GridBagConstraints();
        gbc_btnCopy2.insets = new Insets(0, 0, 0, 5);
        gbc_btnCopy2.gridx = 0;
        gbc_btnCopy2.gridy = 0;
        panelFilledSql.add(btnCopy2, gbc_btnCopy2);
        btnCopy2.setIcon(new ImageIcon(PerfLoggerPanel.class.getResource("/icons/32px-Edit-copy_purple.png")));
        btnCopy2.setToolTipText("Copy the SQL statement to the clipboard, with the bind variables replaced by their actual value");

        final JScrollPane scrollPaneFilledSql = new JScrollPane();
        final GridBagConstraints gbc_scrollPaneFilledSql = new GridBagConstraints();
        gbc_scrollPaneFilledSql.fill = GridBagConstraints.BOTH;
        gbc_scrollPaneFilledSql.gridx = 1;
        gbc_scrollPaneFilledSql.gridy = 0;
        panelFilledSql.add(scrollPaneFilledSql, gbc_scrollPaneFilledSql);

        txtFieldFilledSql = new RSyntaxTextArea();
        scrollPaneFilledSql.setViewportView(txtFieldFilledSql);
        applySqlSyntaxColoring(txtFieldFilledSql);
        txtFieldFilledSql.setOpaque(false);
        txtFieldFilledSql.setEditable(false);
        txtFieldFilledSql.setLineWrap(true);
        btnCopy2.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(@Nullable final ActionEvent e) {
                final StringSelection stringSelection = new StringSelection(txtFieldFilledSql.getText());
                Toolkit.getDefaultToolkit().getSystemClipboard().setContents(stringSelection, stringSelection);
            }
        });

        final GridBagConstraints gbc_splitPane = new GridBagConstraints();
        gbc_splitPane.fill = GridBagConstraints.BOTH;
        gbc_splitPane.insets = new Insets(0, 0, 5, 0);
        gbc_splitPane.gridx = 0;
        gbc_splitPane.gridy = 1;
        add(splitPane, gbc_splitPane);

        final JPanel bottomPanel = new JPanel();
        final GridBagConstraints gbc_bottomPanel = new GridBagConstraints();
        gbc_bottomPanel.anchor = GridBagConstraints.NORTH;
        gbc_bottomPanel.fill = GridBagConstraints.HORIZONTAL;
        gbc_bottomPanel.gridx = 0;
        gbc_bottomPanel.gridy = 2;
        add(bottomPanel, gbc_bottomPanel);
        final GridBagLayout gbl_bottomPanel = new GridBagLayout();
        gbl_bottomPanel.columnWidths = new int[] { 507, 125, 125, 79, 0 };
        gbl_bottomPanel.rowHeights = new int[] { 29, 0 };
        gbl_bottomPanel.columnWeights = new double[] { 1.0, 0.0, 0.0, 0.0, Double.MIN_VALUE };
        gbl_bottomPanel.rowWeights = new double[] { 0.0, Double.MIN_VALUE };
        bottomPanel.setLayout(gbl_bottomPanel);

        btnClose = new JButton("Close");
        btnClose.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(@Nullable final ActionEvent e) {
                perfLoggerController.onClose();
            }
        });

        final JButton btnExportCsv = new JButton("Export CSV...");
        btnExportCsv.setToolTipText("Export all statements to a CSV file");
        btnExportCsv.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(@Nullable final ActionEvent e) {
                perfLoggerController.onExportCsv();
            }
        });

        final JButton btnExportSql = new JButton("Export SQL...");
        btnExportSql.setToolTipText("Export all statements as a sql script");
        btnExportSql.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(@Nullable final ActionEvent e) {
                perfLoggerController.onExportSql();
            }
        });

        lblStatus = new JLabel(" ");
        final GridBagConstraints gbc_lblStatus = new GridBagConstraints();
        gbc_lblStatus.anchor = GridBagConstraints.BASELINE;
        gbc_lblStatus.fill = GridBagConstraints.HORIZONTAL;
        gbc_lblStatus.insets = new Insets(0, 0, 0, 5);
        gbc_lblStatus.gridx = 0;
        gbc_lblStatus.gridy = 0;
        bottomPanel.add(lblStatus, gbc_lblStatus);
        final GridBagConstraints gbc_btnExportSql = new GridBagConstraints();
        gbc_btnExportSql.anchor = GridBagConstraints.BASELINE_LEADING;
        gbc_btnExportSql.insets = new Insets(0, 0, 0, 5);
        gbc_btnExportSql.gridx = 1;
        gbc_btnExportSql.gridy = 0;
        bottomPanel.add(btnExportSql, gbc_btnExportSql);
        final GridBagConstraints gbc_btnExportCsv = new GridBagConstraints();
        gbc_btnExportCsv.anchor = GridBagConstraints.BASELINE_LEADING;
        gbc_btnExportCsv.insets = new Insets(0, 0, 0, 5);
        gbc_btnExportCsv.gridx = 2;
        gbc_btnExportCsv.gridy = 0;
        bottomPanel.add(btnExportCsv, gbc_btnExportCsv);
        final GridBagConstraints gbc_btnClose = new GridBagConstraints();
        gbc_btnClose.anchor = GridBagConstraints.BASELINE_LEADING;
        gbc_btnClose.gridx = 3;
        gbc_btnClose.gridy = 0;
        bottomPanel.add(btnClose, gbc_btnClose);

        KeyboardFocusManager.getCurrentKeyboardFocusManager().addKeyEventDispatcher(new KeyEventDispatcher() {
            @Override
            public boolean dispatchKeyEvent(@Nullable final KeyEvent e) {
                assert e != null;
                if (e.getKeyCode() == java.awt.event.KeyEvent.VK_BACK_SPACE
                        && e.getModifiers() == java.awt.event.InputEvent.CTRL_MASK && e.getID() == KeyEvent.KEY_PRESSED) {
                    perfLoggerController.onClear();
                    return true;
                }
                return false;
            }
        });

    }

    private void applySqlSyntaxColoring(final RSyntaxTextArea txtArea) {
        txtArea.setCurrentLineHighlightColor(Color.WHITE);
        txtArea.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_SQL);
        final SyntaxScheme scheme = txtArea.getSyntaxScheme();
        scheme.getStyle(TokenTypes.LITERAL_CHAR).background = Color.CYAN;
        scheme.getStyle(TokenTypes.LITERAL_NUMBER_DECIMAL_INT).background = Color.YELLOW;
        // scheme.getStyle(TokenTypes.LITERAL_NUMBER_FLOAT).background = Color.YELLOW;
    }

    void setCloseEnable(final boolean enabled) {
        btnClose.setEnabled(enabled);
        if (enabled) {
            btnClose.setToolTipText("");
        } else {
            btnClose.setToolTipText("Server connection cannot be closed, only GUI-initiated connections can be closed");
        }
    }

    void setData(final List<Object[]> rows, final List<String> columnNames, final List<Class<?>> columnTypes,
            final boolean tableStructureChanged) {
        final int selectedRow = table.getSelectedRow();
        int modelRowIndex = -1;
        if (selectedRow >= 0) {
            modelRowIndex = table.convertRowIndexToModel(selectedRow);
        }

        dataModel.setNewData(rows, columnNames, columnTypes);
        if (tableStructureChanged) {
            for (int i = 0; i < dataModel.getColumnCount(); i++) {
                final Integer width = COLUMNS_WIDTH.get(dataModel.getColumnName(i));
                if (width != null) {
                    table.getColumnModel().getColumn(i).setPreferredWidth(width.intValue());
                }
            }
        } else if (selectedRow >= 0 && selectedRow < rows.size() && modelRowIndex < rows.size()) {
            final int newSelectedRowIndex = table.convertRowIndexToView(modelRowIndex);
            table.setRowSelectionInterval(newSelectedRowIndex, newSelectedRowIndex);
        }
    }

    void setPaused(final boolean paused) {
        if (paused) {
            btnPause.setIcon(new ImageIcon(PerfLoggerPanel.class.getResource("/icons/32px-Media-record.png")));
        } else {
            btnPause.setIcon(new ImageIcon(PerfLoggerPanel.class.getResource("/icons/32px-Media-playback-pause.png")));
        }
    }

    void setDeltaTimestampBaseMillis(final long deltaTimestampBaseMillis) {
        stmtTimestampCellRenderer.setDeltaTimestampBaseMillis(deltaTimestampBaseMillis);
        if (dataModel.getRowCount() > 0) {
            dataModel.fireTableRowsUpdated(0, dataModel.getRowCount() - 1);
        }
    }
}
