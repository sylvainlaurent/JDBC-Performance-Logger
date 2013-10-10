package ch.sla.jdbcperflogger.console.ui;

import static ch.sla.jdbcperflogger.console.db.LogRepository.ERROR_COLUMN;
import static ch.sla.jdbcperflogger.console.db.LogRepository.EXEC_COUNT_COLUMN;
import static ch.sla.jdbcperflogger.console.db.LogRepository.EXEC_PLUS_FETCH_TIME_COLUMN;
import static ch.sla.jdbcperflogger.console.db.LogRepository.EXEC_TIME_COLUMN;
import static ch.sla.jdbcperflogger.console.db.LogRepository.FETCH_TIME_COLUMN;
import static ch.sla.jdbcperflogger.console.db.LogRepository.FILLED_SQL_COLUMN;
import static ch.sla.jdbcperflogger.console.db.LogRepository.RAW_SQL_COLUMN;
import static ch.sla.jdbcperflogger.console.db.LogRepository.STMT_TYPE_COLUMN;
import static ch.sla.jdbcperflogger.console.db.LogRepository.THREAD_NAME_COLUMN;
import static ch.sla.jdbcperflogger.console.db.LogRepository.TOTAL_EXEC_TIME_COLUMN;
import static ch.sla.jdbcperflogger.console.db.LogRepository.TSTAMP_COLUMN;

import java.awt.Color;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.KeyEventDispatcher;
import java.awt.KeyboardFocusManager;
import java.awt.Toolkit;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.math.BigDecimal;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.DefaultComboBoxModel;
import javax.swing.ImageIcon;
import javax.swing.JButton;
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

import ch.sla.jdbcperflogger.console.ui.PerfLoggerController.FilterType;
import ch.sla.jdbcperflogger.console.ui.PerfLoggerController.GroupBy;

@SuppressWarnings("serial")
public class PerfLoggerPanel extends JPanel {

    private static final Map<String, Integer> COLUMNS_WIDTH;

    static {
        COLUMNS_WIDTH = new HashMap<String, Integer>();
        COLUMNS_WIDTH.put(TSTAMP_COLUMN, 100);
        COLUMNS_WIDTH.put(FETCH_TIME_COLUMN, 50);
        COLUMNS_WIDTH.put(EXEC_TIME_COLUMN, 50);
        COLUMNS_WIDTH.put(EXEC_PLUS_FETCH_TIME_COLUMN, 50);
        COLUMNS_WIDTH.put(STMT_TYPE_COLUMN, 40);
        COLUMNS_WIDTH.put(RAW_SQL_COLUMN, 350);
        COLUMNS_WIDTH.put(FILLED_SQL_COLUMN, 200);
        COLUMNS_WIDTH.put(THREAD_NAME_COLUMN, 200);
        COLUMNS_WIDTH.put(EXEC_COUNT_COLUMN, 100);
        COLUMNS_WIDTH.put(TOTAL_EXEC_TIME_COLUMN, 100);
        COLUMNS_WIDTH.put(ERROR_COLUMN, 0);
    }

    private final PerfLoggerController perfLoggerController;
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

    public PerfLoggerPanel(PerfLoggerController perfLoggerController) {
        this.perfLoggerController = perfLoggerController;
        initialize();
    }

    /**
     * Initialize the contents of the frame.
     */
    private void initialize() {
        // logListPanel.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);

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
        gbl_filterPanel.columnWidths = new int[] { 0, 51, 246, 0, 54, 0 };
        gbl_filterPanel.rowHeights = new int[] { 30, 0 };
        gbl_filterPanel.columnWeights = new double[] { 0.0, 0.0, 1.0, 0.0, 0.0, Double.MIN_VALUE };
        gbl_filterPanel.rowWeights = new double[] { 0.0, Double.MIN_VALUE };
        filterPanel.setLayout(gbl_filterPanel);

        comboBoxFilterType = new JComboBox<FilterType>();
        comboBoxFilterType.setModel(new DefaultComboBoxModel<FilterType>(EnumSet.allOf(FilterType.class).toArray(
                new FilterType[0])));
        comboBoxFilterType.setSelectedItem(FilterType.FILTER);
        comboBoxFilterType.addActionListener(new ActionListener() {
            @SuppressWarnings("null")
            @Override
            public void actionPerformed(ActionEvent e) {
                perfLoggerController.setFilterType(comboBoxFilterType.getItemAt(comboBoxFilterType.getSelectedIndex()));
            }
        });
        final GridBagConstraints gbc_filterTypeComboBox = new GridBagConstraints();
        gbc_filterTypeComboBox.insets = new Insets(0, 0, 0, 5);
        gbc_filterTypeComboBox.fill = GridBagConstraints.HORIZONTAL;
        gbc_filterTypeComboBox.gridx = 0;
        gbc_filterTypeComboBox.gridy = 0;
        filterPanel.add(comboBoxFilterType, gbc_filterTypeComboBox);

        final JLabel lblText = new JLabel("Text:");
        final GridBagConstraints gbc_lblText = new GridBagConstraints();
        gbc_lblText.anchor = GridBagConstraints.BASELINE_TRAILING;
        gbc_lblText.insets = new Insets(0, 0, 0, 5);
        gbc_lblText.gridx = 1;
        gbc_lblText.gridy = 0;
        filterPanel.add(lblText, gbc_lblText);

        {
            txtFldSqlFilter = new JTextField();
            final GridBagConstraints gbc_txtFldSqlFilter = new GridBagConstraints();
            gbc_txtFldSqlFilter.anchor = GridBagConstraints.BASELINE;
            gbc_txtFldSqlFilter.fill = GridBagConstraints.HORIZONTAL;
            gbc_txtFldSqlFilter.insets = new Insets(0, 0, 0, 5);
            gbc_txtFldSqlFilter.gridx = 2;
            gbc_txtFldSqlFilter.gridy = 0;
            filterPanel.add(txtFldSqlFilter, gbc_txtFldSqlFilter);
            txtFldSqlFilter.setColumns(10);

            txtFldSqlFilter.getDocument().addUndoableEditListener(new UndoableEditListener() {
                @Override
                public void undoableEditHappened(UndoableEditEvent e) {
                    perfLoggerController.setTextFilter(txtFldSqlFilter.getText());
                }
            });
        }
        {
            final JLabel lblDurationms = new JLabel("Exec duration (ms) >=");
            final GridBagConstraints gbc_lblDurationms = new GridBagConstraints();
            gbc_lblDurationms.anchor = GridBagConstraints.BASELINE_TRAILING;
            gbc_lblDurationms.insets = new Insets(0, 0, 0, 5);
            gbc_lblDurationms.gridx = 3;
            gbc_lblDurationms.gridy = 0;
            filterPanel.add(lblDurationms, gbc_lblDurationms);
        }
        {
            txtFldMinDuration = new JTextField();
            final GridBagConstraints gbc_txtFldMinDuration = new GridBagConstraints();
            gbc_txtFldMinDuration.anchor = GridBagConstraints.BASELINE;
            gbc_txtFldMinDuration.fill = GridBagConstraints.HORIZONTAL;
            gbc_txtFldMinDuration.gridx = 4;
            gbc_txtFldMinDuration.gridy = 0;
            txtFldMinDuration.setColumns(5);
            filterPanel.add(txtFldMinDuration, gbc_txtFldMinDuration);
            txtFldMinDuration.getDocument().addUndoableEditListener(new UndoableEditListener() {
                @Override
                public void undoableEditHappened(UndoableEditEvent e) {
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

        comboBoxGroupBy = new JComboBox<GroupBy>();
        final GridBagConstraints gbc_comboBoxGroupBy = new GridBagConstraints();
        gbc_comboBoxGroupBy.fill = GridBagConstraints.HORIZONTAL;
        gbc_comboBoxGroupBy.gridx = 0;
        gbc_comboBoxGroupBy.gridy = 0;
        groupByPanel.add(comboBoxGroupBy, gbc_comboBoxGroupBy);
        comboBoxGroupBy
                .setModel(new DefaultComboBoxModel<GroupBy>(EnumSet.allOf(GroupBy.class).toArray(new GroupBy[0])));
        comboBoxGroupBy.setSelectedIndex(0);
        comboBoxGroupBy.addActionListener(new ActionListener() {
            @SuppressWarnings("null")
            @Override
            public void actionPerformed(ActionEvent e) {
                perfLoggerController.setGroupBy(comboBoxGroupBy.getItemAt(comboBoxGroupBy.getSelectedIndex()));
            }
        });

        btnPause = new JButton();
        final GridBagConstraints gbc_btnPause = new GridBagConstraints();
        gbc_btnPause.insets = new Insets(0, 0, 0, 5);
        gbc_btnPause.gridx = 2;
        gbc_btnPause.gridy = 0;
        topPanel.add(btnPause, gbc_btnPause);
        btnPause.setBorderPainted(false);
        btnPause.setIcon(new ImageIcon(PerfLoggerPanel.class.getResource("/icons/32px-Media-playback-pause.png")));
        btnPause.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                perfLoggerController.onPause();
            }
        });

        final JButton btnClear = new JButton();
        final GridBagConstraints gbc_btnClear = new GridBagConstraints();
        gbc_btnClear.gridx = 3;
        gbc_btnClear.gridy = 0;
        topPanel.add(btnClear, gbc_btnClear);
        btnClear.setBorderPainted(false);
        btnClear.setIcon(new ImageIcon(PerfLoggerPanel.class.getResource("/icons/32px-Edit-clear.png")));
        btnClear.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
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
        table.setSelectionForeground(Color.blue);
        table.setSelectionBackground(Color.yellow);
        table.setDefaultRenderer(Byte.class, new CustomTableCellRenderer());
        table.setDefaultRenderer(String.class, new CustomTableCellRenderer());
        table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        table.setFillsViewportHeight(true);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setAutoCreateRowSorter(true);
        logListPanel.setViewportView(table);

        table.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
            @Override
            public void valueChanged(ListSelectionEvent e) {
                if (!e.getValueIsAdjusting()) {
                    final ListSelectionModel lsm = (ListSelectionModel) e.getSource();
                    Long logId = null;
                    if (lsm.getMinSelectionIndex() >= 0) {
                        logId = dataModel.getIdAtRow(table.convertRowIndexToModel(lsm.getMinSelectionIndex()));
                    }
                    if (logId != null) {
                        perfLoggerController.onSelectStatement(logId);
                    }
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
        gbl_panelRawSql.rowHeights = new int[] { 0, 0 };
        gbl_panelRawSql.columnWeights = new double[] { 0.0, 1.0, Double.MIN_VALUE };
        gbl_panelRawSql.rowWeights = new double[] { 1.0, Double.MIN_VALUE };
        panelRawSql.setLayout(gbl_panelRawSql);

        final JButton btnCopy1 = new JButton();
        final GridBagConstraints gbc_btnCopy1 = new GridBagConstraints();
        gbc_btnCopy1.insets = new Insets(0, 0, 0, 5);
        gbc_btnCopy1.gridx = 0;
        gbc_btnCopy1.gridy = 0;
        panelRawSql.add(btnCopy1, gbc_btnCopy1);
        btnCopy1.setIcon(new ImageIcon(PerfLoggerPanel.class.getResource("/icons/32px-Edit-copy_purple.png")));
        btnCopy1.setToolTipText("Copy the SQL statement unmodified (potentiall with '?' for bind variables");
        btnCopy1.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                final StringSelection stringSelection = new StringSelection(txtFieldRawSql.getText());
                Toolkit.getDefaultToolkit().getSystemClipboard().setContents(stringSelection, stringSelection);
            }
        });

        final JScrollPane scrollPaneRawSql = new JScrollPane();
        final GridBagConstraints gbc_scrollPaneRawSql = new GridBagConstraints();
        gbc_scrollPaneRawSql.fill = GridBagConstraints.BOTH;
        gbc_scrollPaneRawSql.gridx = 1;
        gbc_scrollPaneRawSql.gridy = 0;
        panelRawSql.add(scrollPaneRawSql, gbc_scrollPaneRawSql);

        txtFieldRawSql = new RSyntaxTextArea();
        scrollPaneRawSql.setViewportView(txtFieldRawSql);
        txtFieldRawSql.setCurrentLineHighlightColor(Color.WHITE);
        txtFieldRawSql.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_SQL);
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
        txtFieldFilledSql.setCurrentLineHighlightColor(Color.WHITE);
        txtFieldFilledSql.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_SQL);
        txtFieldFilledSql.setOpaque(false);
        txtFieldFilledSql.setEditable(false);
        txtFieldFilledSql.setLineWrap(true);
        btnCopy2.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
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
            public void actionPerformed(ActionEvent e) {
                perfLoggerController.onClose();
            }
        });

        final JButton btnExportCsv = new JButton("Export CSV...");
        btnExportCsv.setToolTipText("Export all statements to a CSV file");
        btnExportCsv.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                perfLoggerController.onExportCsv();
            }
        });

        final JButton btnExportSql = new JButton("Export SQL...");
        btnExportSql.setToolTipText("Export all statements as a sql script");
        btnExportSql.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
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
            public boolean dispatchKeyEvent(KeyEvent e) {
                if (e.getKeyCode() == java.awt.event.KeyEvent.VK_BACK_SPACE
                        && e.getModifiers() == java.awt.event.InputEvent.CTRL_MASK && e.getID() == KeyEvent.KEY_PRESSED) {
                    perfLoggerController.onClear();
                    return true;
                }
                return false;
            }
        });

    }

    void setCloseEnable(boolean enabled) {
        btnClose.setEnabled(false);
        btnClose.setToolTipText("Server connection cannot be closed, only GUI-initiated connections can be closed");
    }

    void setData(List<Object[]> rows, List<String> columnNames, List<Class<?>> columnTypes,
            boolean tableStructureChanged) {
        dataModel.setNewData(rows, columnNames, columnTypes);
        if (tableStructureChanged) {
            for (int i = 0; i < dataModel.getColumnCount(); i++) {
                final Integer width = COLUMNS_WIDTH.get(dataModel.getColumnName(i));
                if (width != null) {
                    table.getColumnModel().getColumn(i).setPreferredWidth(width.intValue());
                }
            }
        }
    }

    void setPaused(boolean paused) {
        if (paused) {
            btnPause.setIcon(new ImageIcon(PerfLoggerPanel.class.getResource("/icons/32px-Media-record.png")));
        } else {
            btnPause.setIcon(new ImageIcon(PerfLoggerPanel.class.getResource("/icons/32px-Media-playback-pause.png")));
        }
    }
}
