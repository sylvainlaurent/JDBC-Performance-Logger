package ch.sla.jdbcperflogger.gui;

import static ch.sla.jdbcperflogger.gui.LogRepository.EXEC_COUNT_COLUMN;
import static ch.sla.jdbcperflogger.gui.LogRepository.EXEC_PLUS_FETCH_TIME_COLUMN;
import static ch.sla.jdbcperflogger.gui.LogRepository.EXEC_TIME_COLUMN;
import static ch.sla.jdbcperflogger.gui.LogRepository.FETCH_TIME_COLUMN;
import static ch.sla.jdbcperflogger.gui.LogRepository.FILLED_SQL_COLUMN;
import static ch.sla.jdbcperflogger.gui.LogRepository.RAW_SQL_COLUMN;
import static ch.sla.jdbcperflogger.gui.LogRepository.STMT_TYPE_COLUMN;
import static ch.sla.jdbcperflogger.gui.LogRepository.THREAD_NAME_COLUMN;
import static ch.sla.jdbcperflogger.gui.LogRepository.TOTAL_EXEC_TIME_COLUMN;
import static ch.sla.jdbcperflogger.gui.LogRepository.TSTAMP_COLUMN;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.KeyEventDispatcher;
import java.awt.KeyboardFocusManager;
import java.awt.Toolkit;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.io.CharArrayWriter;
import java.io.File;
import java.io.PrintWriter;
import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;

import javax.swing.BoxLayout;
import javax.swing.DefaultComboBoxModel;
import javax.swing.GroupLayout;
import javax.swing.GroupLayout.Alignment;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.LayoutStyle.ComponentPlacement;
import javax.swing.ListSelectionModel;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingUtilities;
import javax.swing.border.TitledBorder;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.UndoableEditEvent;
import javax.swing.event.UndoableEditListener;
import javax.swing.filechooser.FileNameExtensionFilter;

import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rsyntaxtextarea.SyntaxConstants;

import ch.sla.jdbcperflogger.model.StatementLog;

public class PerfLoggerPanel extends JPanel {

    private static final long serialVersionUID = 1L;
    private static final String GROUP_BY_FILLED_SQL = "Group by filled SQL";
    private static final String GROUP_BY_RAW_SQL = "Group by raw SQL";
    private static final String NO_GROUPING = "No grouping";

    private static final Map<String, Integer> COLUMNS_WIDTH;

    static {
        COLUMNS_WIDTH = new HashMap<String, Integer>();
        COLUMNS_WIDTH.put(TSTAMP_COLUMN, 180);
        COLUMNS_WIDTH.put(FETCH_TIME_COLUMN, 50);
        COLUMNS_WIDTH.put(EXEC_TIME_COLUMN, 50);
        COLUMNS_WIDTH.put(EXEC_PLUS_FETCH_TIME_COLUMN, 50);
        COLUMNS_WIDTH.put(STMT_TYPE_COLUMN, 60);
        COLUMNS_WIDTH.put(RAW_SQL_COLUMN, 300);
        COLUMNS_WIDTH.put(FILLED_SQL_COLUMN, 200);
        COLUMNS_WIDTH.put(THREAD_NAME_COLUMN, 200);
        COLUMNS_WIDTH.put(EXEC_COUNT_COLUMN, 100);
        COLUMNS_WIDTH.put(TOTAL_EXEC_TIME_COLUMN, 100);
    }

    private final AbstractLogReceiver logReceiver;
    private final LogRepository logRepository;
    private final LogExporter logExporter;
    private final IClientConnectionDelegate clientConnectionDelegate;
    private volatile String txtFilter;
    private volatile Long minDurationNanos;
    private JTextField txtFldSqlFilter;
    private JTextField txtFldMinDuration;
    private JTable table;
    private ResultSetDataModel dataModel;
    private RefreshDataTask refreshDataTask;
    private JComboBox comboBoxGroupBy;
    private RSyntaxTextArea txtFieldSqlDetail1;
    private RSyntaxTextArea txtFieldSqlDetail2;
    private JLabel lblStatus;
    private JScrollPane scrollPaneSqlDetail2;
    private JScrollPane scrollPaneSqlDetail1;
    private boolean tableStructureChanged = true;

    private final SelectLogRunner selectAllLogStatements = new SelectLogRunner() {
        @Override
        public void doSelect(ResultSetAnalyzer resultSetAnalyzer) {
            logRepository.getStatements(txtFilter, minDurationNanos, resultSetAnalyzer);
        }
    };
    private final SelectLogRunner selectLogStatementsGroupByRawSql = new SelectLogRunner() {
        @Override
        public void doSelect(ResultSetAnalyzer resultSetAnalyzer) {
            logRepository.getStatementsGroupByRawSQL(txtFilter, minDurationNanos, resultSetAnalyzer);
        }
    };
    private final SelectLogRunner selectLogStatementsGroupByFilledSql = new SelectLogRunner() {
        @Override
        public void doSelect(ResultSetAnalyzer resultSetAnalyzer) {
            logRepository.getStatementsGroupByFilledSQL(txtFilter, minDurationNanos, resultSetAnalyzer);
        }
    };

    private SelectLogRunner currentSelectLogRunner = selectAllLogStatements;

    public PerfLoggerPanel(AbstractLogReceiver logReceiver, LogRepository logRepository,
            IClientConnectionDelegate clientConnectionDelegate) {
        this.logReceiver = logReceiver;
        this.logRepository = logRepository;
        logExporter = new LogExporter(logRepository);
        this.clientConnectionDelegate = clientConnectionDelegate;
        initialize();
    }

    /**
     * Initialize the contents of the frame.
     */
    private void initialize() {

        final JSplitPane splitPane = new JSplitPane();
        splitPane.setResizeWeight(0.8);
        splitPane.setOneTouchExpandable(true);
        splitPane.setBorder(null);
        splitPane.setContinuousLayout(true);
        splitPane.setOrientation(JSplitPane.VERTICAL_SPLIT);

        final JScrollPane logListPanel = new JScrollPane();
        logListPanel.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
        // logListPanel.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);

        dataModel = new ResultSetDataModel();
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
                    statementSelected(logId);
                }
            }
        });
        splitPane.setTopComponent(logListPanel);

        final JPanel sqlDetailPanel = new JPanel();
        sqlDetailPanel.setBorder(new TitledBorder(null, "SQL detail", TitledBorder.LEADING, TitledBorder.TOP, null,
                null));
        splitPane.setBottomComponent(sqlDetailPanel);

        final JSplitPane splitPane_1 = new JSplitPane();
        splitPane_1.setResizeWeight(0.5);
        splitPane_1.setContinuousLayout(true);
        splitPane_1.setOrientation(JSplitPane.VERTICAL_SPLIT);
        final GroupLayout gl_sqlDetailPanel = new GroupLayout(sqlDetailPanel);
        gl_sqlDetailPanel.setHorizontalGroup(gl_sqlDetailPanel.createParallelGroup(Alignment.LEADING).addGroup(
                gl_sqlDetailPanel.createSequentialGroup().addContainerGap()
                        .addComponent(splitPane_1, GroupLayout.PREFERRED_SIZE, 0, Short.MAX_VALUE).addContainerGap()));
        gl_sqlDetailPanel.setVerticalGroup(gl_sqlDetailPanel.createParallelGroup(Alignment.LEADING)
                .addGroup(
                        gl_sqlDetailPanel.createSequentialGroup().addContainerGap().addComponent(splitPane_1)
                                .addContainerGap()));

        scrollPaneSqlDetail1 = new JScrollPane();
        scrollPaneSqlDetail1.setMinimumSize(new Dimension(23, 30));
        splitPane_1.setLeftComponent(scrollPaneSqlDetail1);
        scrollPaneSqlDetail1.setOpaque(false);

        txtFieldSqlDetail1 = new RSyntaxTextArea();
        txtFieldSqlDetail1.setCurrentLineHighlightColor(Color.WHITE);
        txtFieldSqlDetail1.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_SQL);
        txtFieldSqlDetail1.setOpaque(false);
        txtFieldSqlDetail1.setEditable(false);
        txtFieldSqlDetail1.setLineWrap(true);
        scrollPaneSqlDetail1.setViewportView(txtFieldSqlDetail1);

        final JPanel panelCopy1 = new JPanel();
        panelCopy1.setBorder(null);
        panelCopy1.setOpaque(false);
        panelCopy1.setLayout(new BoxLayout(panelCopy1, BoxLayout.X_AXIS));
        final JButton btnCopy1 = new JButton("Copy raw SQL");
        btnCopy1.setToolTipText("Copy the SQL statement unmodified (potentiall with '?' for bind variables");
        panelCopy1.add(btnCopy1);
        scrollPaneSqlDetail1.setRowHeaderView(panelCopy1);

        scrollPaneSqlDetail2 = new JScrollPane();
        scrollPaneSqlDetail2.setMinimumSize(new Dimension(23, 30));
        splitPane_1.setRightComponent(scrollPaneSqlDetail2);
        scrollPaneSqlDetail2.setOpaque(false);

        txtFieldSqlDetail2 = new RSyntaxTextArea();
        txtFieldSqlDetail2.setCurrentLineHighlightColor(Color.WHITE);
        txtFieldSqlDetail2.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_SQL);
        txtFieldSqlDetail2.setOpaque(false);
        txtFieldSqlDetail2.setEditable(false);
        txtFieldSqlDetail2.setLineWrap(true);
        scrollPaneSqlDetail2.setViewportView(txtFieldSqlDetail2);

        final JPanel panelCopy2 = new JPanel();
        panelCopy2.setBorder(null);
        panelCopy2.setOpaque(false);
        panelCopy2.setLayout(new BoxLayout(panelCopy2, BoxLayout.X_AXIS));
        final JButton btnCopy2 = new JButton("Copy filled SQL");
        btnCopy2.setToolTipText("Copy the SQL statement to the clipboard, with the bind variables replaced by their actual value");
        panelCopy2.add(btnCopy2);
        scrollPaneSqlDetail2.setRowHeaderView(panelCopy2);
        btnCopy2.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                final StringSelection stringSelection = new StringSelection(txtFieldSqlDetail2.getText());
                Toolkit.getDefaultToolkit().getSystemClipboard().setContents(stringSelection, stringSelection);
            }
        });
        btnCopy1.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                final StringSelection stringSelection = new StringSelection(txtFieldSqlDetail1.getText());
                Toolkit.getDefaultToolkit().getSystemClipboard().setContents(stringSelection, stringSelection);
            }
        });
        sqlDetailPanel.setLayout(gl_sqlDetailPanel);

        final JLabel lblFilter = new JLabel("Filter:");

        txtFldSqlFilter = new JTextField();
        txtFldSqlFilter.setColumns(10);
        txtFldSqlFilter.getDocument().addUndoableEditListener(new UndoableEditListener() {
            @Override
            public void undoableEditHappened(UndoableEditEvent e) {
                refresh();
            }
        });

        final JLabel lblDurationms = new JLabel("Exec duration (ms) >=");

        txtFldMinDuration = new JTextField();
        txtFldMinDuration.getDocument().addUndoableEditListener(new UndoableEditListener() {
            @Override
            public void undoableEditHappened(UndoableEditEvent e) {
                if (txtFldMinDuration.getText().length() > 0) {
                    try {
                        new BigDecimal(txtFldMinDuration.getText());
                    } catch (final NumberFormatException exc) {
                        e.getEdit().undo();
                        return;
                    }
                }
                refresh();
            }
        });
        txtFldMinDuration.setColumns(4);

        comboBoxGroupBy = new JComboBox();
        comboBoxGroupBy.setModel(new DefaultComboBoxModel(new String[] { NO_GROUPING, GROUP_BY_RAW_SQL,
                GROUP_BY_FILLED_SQL }));
        comboBoxGroupBy.setSelectedIndex(0);
        comboBoxGroupBy.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                tableStructureChanged = true;
                refresh();
            }
        });

        final JButton btnClear = new JButton("Clear");
        btnClear.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                logRepository.clear();
                refresh();
            }
        });

        final JButton btnPause = new JButton("Pause");
        btnPause.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                if (logReceiver.isPaused()) {
                    logReceiver.resumeReceivingLogs();
                    btnPause.setText("Pause");
                } else {
                    logReceiver.pauseReceivingLogs();
                    btnPause.setText("Resume");
                }
            }
        });

        final JPanel panel = new JPanel();

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
                                                        .addComponent(lblFilter, GroupLayout.PREFERRED_SIZE,
                                                                GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE)
                                                        .addPreferredGap(ComponentPlacement.RELATED)
                                                        .addComponent(txtFldSqlFilter, GroupLayout.PREFERRED_SIZE, 246,
                                                                Short.MAX_VALUE)
                                                        .addPreferredGap(ComponentPlacement.UNRELATED)
                                                        .addComponent(lblDurationms)
                                                        .addPreferredGap(ComponentPlacement.RELATED)
                                                        .addComponent(txtFldMinDuration, GroupLayout.PREFERRED_SIZE,
                                                                GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                                                        .addPreferredGap(ComponentPlacement.RELATED)
                                                        .addComponent(comboBoxGroupBy, GroupLayout.PREFERRED_SIZE,
                                                                GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE)
                                                        .addPreferredGap(ComponentPlacement.UNRELATED)
                                                        .addComponent(btnClear)
                                                        .addPreferredGap(ComponentPlacement.RELATED)
                                                        .addComponent(btnPause))
                                        .addGroup(groupLayout.createSequentialGroup().addComponent(splitPane))
                                        .addComponent(panel, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE,
                                                Short.MAX_VALUE)).addContainerGap()));
        groupLayout.setVerticalGroup(groupLayout.createParallelGroup(Alignment.LEADING).addGroup(
                groupLayout
                        .createSequentialGroup()
                        .addContainerGap()
                        .addGroup(
                                groupLayout
                                        .createParallelGroup(Alignment.BASELINE)
                                        .addComponent(lblFilter)
                                        .addComponent(txtFldSqlFilter, GroupLayout.PREFERRED_SIZE,
                                                GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                                        .addComponent(lblDurationms)
                                        .addComponent(txtFldMinDuration, GroupLayout.PREFERRED_SIZE,
                                                GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                                        .addComponent(comboBoxGroupBy, GroupLayout.PREFERRED_SIZE,
                                                GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                                        .addComponent(btnClear).addComponent(btnPause))
                        .addPreferredGap(ComponentPlacement.UNRELATED)
                        .addComponent(splitPane, GroupLayout.DEFAULT_SIZE, 200, Short.MAX_VALUE)
                        .addPreferredGap(ComponentPlacement.RELATED)
                        .addComponent(panel, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE,
                                GroupLayout.PREFERRED_SIZE).addContainerGap()));

        lblStatus = new JLabel(" ");

        final JButton btnExportSql = new JButton("Export SQL...");
        btnExportSql.setToolTipText("Export all statements as a sql script");
        btnExportSql.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                exportSql();
            }
        });

        final JButton btnExportCsv = new JButton("Export CSV...");
        btnExportCsv.setToolTipText("Export all statements to a CSV file");
        btnExportCsv.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                exportCsv();
            }
        });

        final JButton btnClose = new JButton("Close");
        btnClose.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                clientConnectionDelegate.close(PerfLoggerPanel.this);
            }
        });
        if (logReceiver.isServerMode()) {
            btnClose.setEnabled(false);
            btnClose.setToolTipText("Server connection cannot be closed, only GUI-initiated connections can be closed");
        }
        final GroupLayout gl_panel = new GroupLayout(panel);
        gl_panel.setHorizontalGroup(gl_panel.createParallelGroup(Alignment.LEADING).addGroup(
                gl_panel.createSequentialGroup().addContainerGap()
                        .addComponent(lblStatus, GroupLayout.DEFAULT_SIZE, 751, Short.MAX_VALUE)
                        .addPreferredGap(ComponentPlacement.RELATED).addComponent(btnExportSql)//
                        .addPreferredGap(ComponentPlacement.RELATED).addComponent(btnExportCsv)//
                        .addPreferredGap(ComponentPlacement.RELATED).addComponent(btnClose)//
                        .addContainerGap()));
        gl_panel.setVerticalGroup(gl_panel.createParallelGroup(Alignment.LEADING).addGroup(
                gl_panel.createParallelGroup(Alignment.BASELINE).addComponent(lblStatus).addComponent(btnExportSql)
                        .addComponent(btnExportCsv).addComponent(btnClose)));
        panel.setLayout(gl_panel);
        this.setLayout(groupLayout);

        final Timer timer = new Timer(true);
        refreshDataTask = new RefreshDataTask();
        timer.schedule(refreshDataTask, 1000, 1000);

        KeyboardFocusManager.getCurrentKeyboardFocusManager().addKeyEventDispatcher(new KeyEventDispatcher() {
            @Override
            public boolean dispatchKeyEvent(KeyEvent e) {
                if (e.getKeyCode() == java.awt.event.KeyEvent.VK_BACK_SPACE
                        && e.getModifiers() == java.awt.event.InputEvent.CTRL_MASK && e.getID() == KeyEvent.KEY_PRESSED) {
                    logRepository.clear();
                    refresh();
                    return true;
                }
                return false;
            }
        });
    }

    /**
     * To be executed in EDT
     */
    private void refresh() {
        {
            final String txt = txtFldSqlFilter.getText();
            if (txt.length() == 0) {
                txtFilter = null;
            } else {
                txtFilter = txt;
            }
        }
        {
            final String txt = txtFldMinDuration.getText();
            if (txt.length() == 0) {
                minDurationNanos = null;
            } else {
                minDurationNanos = TimeUnit.MILLISECONDS.toNanos(new BigDecimal(txt).longValue());
            }
        }
        if (NO_GROUPING.equals(comboBoxGroupBy.getSelectedItem())) {
            currentSelectLogRunner = selectAllLogStatements;
        } else if (GROUP_BY_RAW_SQL.equals(comboBoxGroupBy.getSelectedItem())) {
            currentSelectLogRunner = selectLogStatementsGroupByRawSql;
        } else if (GROUP_BY_FILLED_SQL.equals(comboBoxGroupBy.getSelectedItem())) {
            currentSelectLogRunner = selectLogStatementsGroupByFilledSql;
        } else {
            throw new IllegalArgumentException("unexpected value " + comboBoxGroupBy.getSelectedItem());
        }

        refreshDataTask.forceRefresh();
    }

    private void statementSelected(Long logId) {
        String txt1 = "";
        String txt2 = "";
        if (logId != null) {
            final StatementLog statementLog = logRepository.getStatementLog(logId);
            final Object selectedItem = comboBoxGroupBy.getSelectedItem();
            if (NO_GROUPING.equals(selectedItem)) {
                txt1 = statementLog.getRawSql();
                switch (statementLog.getStatementType()) {
                case NON_PREPARED_BATCH_EXECUTION:
                    txt1 = logExporter.getBatchedExecutions(statementLog);
                    break;
                case PREPARED_BATCH_EXECUTION:
                    txt2 = logExporter.getBatchedExecutions(statementLog);
                    break;
                case BASE_PREPARED_STMT:
                case PREPARED_QUERY_STMT:
                    txt2 = statementLog.getFilledSql();
                    break;
                default:
                    break;
                }
            } else if (GROUP_BY_RAW_SQL.equals(selectedItem)) {
                switch (statementLog.getStatementType()) {
                case BASE_NON_PREPARED_STMT:
                case BASE_PREPARED_STMT:
                case PREPARED_BATCH_EXECUTION:
                case PREPARED_QUERY_STMT:
                case NON_PREPARED_QUERY_STMT:
                    txt1 = statementLog.getRawSql();
                    break;
                case NON_PREPARED_BATCH_EXECUTION:
                    txt1 = "Cannot display details in \"Group by\" modes";
                }
            } else if (GROUP_BY_FILLED_SQL.equals(selectedItem)) {
                switch (statementLog.getStatementType()) {
                case BASE_NON_PREPARED_STMT:
                case PREPARED_BATCH_EXECUTION:
                case NON_PREPARED_QUERY_STMT:
                    txt1 = statementLog.getRawSql();
                    break;
                case BASE_PREPARED_STMT:
                case PREPARED_QUERY_STMT:
                    txt1 = statementLog.getRawSql();
                    txt2 = statementLog.getFilledSql();
                    break;
                case NON_PREPARED_BATCH_EXECUTION:
                    txt1 = "Cannot display details in \"Group by\" modes";
                }
            } else {
                throw new IllegalArgumentException("unexpected selectedItem " + selectedItem);
            }

            if (statementLog.getSqlException() != null) {
                final CharArrayWriter writer = new CharArrayWriter();
                statementLog.getSqlException().printStackTrace(new PrintWriter(writer));
                txt2 += writer.toString();
            }
        }
        txtFieldSqlDetail1.setText(txt1);
        txtFieldSqlDetail1.select(0, 0);
        txtFieldSqlDetail2.setText(txt2);
        txtFieldSqlDetail2.select(0, 0);
        // scrollPaneSqlDetail1.setEnabled(txt1 != null);
        // scrollPaneSqlDetail2.setEnabled(txt2 != null);
    }

    private void exportSql() {
        final JFileChooser fileChooser = new JFileChooser();
        fileChooser.setFileFilter(new FileNameExtensionFilter("SQL file", "sql"));
        if (fileChooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            File targetFile = fileChooser.getSelectedFile();
            if (!targetFile.getName().toLowerCase().endsWith(".sql")) {
                targetFile = new File(targetFile.getAbsolutePath() + ".sql");
            }
            selectAllLogStatements.doSelect(logExporter.getSqlLogExporter(targetFile));
        }
    }

    private void exportCsv() {
        final JFileChooser fileChooser = new JFileChooser();
        fileChooser.setFileFilter(new FileNameExtensionFilter("CSV file", "csv"));
        if (fileChooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            File targetFile = fileChooser.getSelectedFile();
            if (!targetFile.getName().toLowerCase().endsWith(".csv")) {
                targetFile = new File(targetFile.getAbsolutePath() + ".csv");
            }
            selectAllLogStatements.doSelect(logExporter.getCsvLogExporter(targetFile));
        }
    }

    public AbstractLogReceiver getLogReceiver() {
        return logReceiver;
    }

    public LogRepository getLogRepository() {
        return logRepository;
    }

    public void dispose() {
        refreshDataTask.cancel();
    }

    /**
     * A {@link TimerTask} that regularly polls the associated {@link LogRepository} to check for new statements to
     * display. If the UI must be refreshed it is later done in the EDT.
     * 
     * @author slaurent
     * 
     */
    private class RefreshDataTask extends TimerTask {
        private volatile long lastRefreshTime;
        private int connectionsCount;

        @Override
        public void run() {
            if (logRepository.getLastModificationTime() <= lastRefreshTime
                    && connectionsCount == logReceiver.getConnectionsCount()) {
                return;
            }
            connectionsCount = logReceiver.getConnectionsCount();

            lastRefreshTime = logRepository.getLastModificationTime();
            doRefreshData(currentSelectLogRunner);

            final StringBuilder txt = new StringBuilder();
            if (logReceiver.isServerMode()) {
                txt.append(connectionsCount);
                txt.append(" connection(s) - ");
            }
            txt.append(logRepository.countStatements());
            txt.append(" statements logged - ");
            txt.append(TimeUnit.NANOSECONDS.toMillis(logRepository.getTotalExecAndFetchTimeNanos()));
            txt.append("ms total execution time (with fetch)");
            if ((txtFilter != null && txtFilter.length() > 0)
                    || (minDurationNanos != null && minDurationNanos.longValue() > 0)) {
                txt.append(" - ");
                txt.append(TimeUnit.NANOSECONDS.toMillis(logRepository.getTotalExecAndFetchTimeNanos(txtFilter,
                        minDurationNanos)));
                txt.append("ms total filtered");
            }
            SwingUtilities.invokeLater(new Runnable() {
                @Override
                public void run() {
                    lblStatus.setText(txt.toString());
                }
            });
        }

        void forceRefresh() {
            lastRefreshTime = -1L;
        }

        void doRefreshData(SelectLogRunner selectLogRunner) {
            selectLogRunner.doSelect(new ResultSetAnalyzer() {
                @Override
                public void analyze(ResultSet resultSet) throws SQLException {
                    final ResultSetMetaData resultSetMetaData = resultSet.getMetaData();
                    final int columnCount = resultSetMetaData.getColumnCount();

                    final List<String> tempColumnNames = new ArrayList<String>();
                    final List<Class<?>> tempColumnTypes = new ArrayList<Class<?>>();
                    final List<Object[]> tempRows = new ArrayList<Object[]>();
                    try {
                        for (int i = 1; i <= columnCount; i++) {
                            tempColumnNames.add(resultSetMetaData.getColumnLabel(i).toUpperCase());
                            if (resultSetMetaData.getColumnType(i) == Types.TIMESTAMP) {
                                tempColumnTypes.add(String.class);
                            } else {
                                tempColumnTypes.add(Class.forName(resultSetMetaData.getColumnClassName(i)));
                            }
                        }

                        final SimpleDateFormat tstampFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
                        while (resultSet.next()) {
                            final Object[] row = new Object[columnCount];
                            for (int i = 1; i <= columnCount; i++) {
                                row[i - 1] = resultSet.getObject(i);
                                if (row[i - 1] instanceof Timestamp) {
                                    row[i - 1] = tstampFormat.format(row[i - 1]);
                                }
                            }
                            tempRows.add(row);
                        }
                    } catch (final ClassNotFoundException e) {
                        throw new RuntimeException(e);
                    }

                    SwingUtilities.invokeLater(new Runnable() {
                        @Override
                        public void run() {
                            dataModel.setNewData(tempRows, tempColumnNames, tempColumnTypes);
                            if (tableStructureChanged) {
                                for (int i = 0; i < dataModel.getColumnCount(); i++) {
                                    final Integer width = COLUMNS_WIDTH.get(dataModel.getColumnName(i));
                                    if (width != null) {
                                        table.getColumnModel().getColumn(i).setPreferredWidth(width.intValue());
                                    }
                                }
                                tableStructureChanged = false;
                            }
                        }
                    });
                }
            });
        }

    }
}
