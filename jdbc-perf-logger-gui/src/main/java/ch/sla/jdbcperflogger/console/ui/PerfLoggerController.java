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

import java.io.CharArrayWriter;
import java.io.File;
import java.io.PrintWriter;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import javax.swing.JFileChooser;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.filechooser.FileNameExtensionFilter;

import ch.sla.jdbcperflogger.console.db.LogRepository;
import ch.sla.jdbcperflogger.console.db.ResultSetAnalyzer;
import ch.sla.jdbcperflogger.console.net.AbstractLogReceiver;
import ch.sla.jdbcperflogger.model.StatementLog;

@ParametersAreNonnullByDefault
public class PerfLoggerController {
    private final AbstractLogReceiver logReceiver;
    private final LogRepository logRepository;
    private final IClientConnectionDelegate clientConnectionDelegate;
    private final LogExporter logExporter;
    private final PerfLoggerPanel perfLoggerPanel;

    private abstract class SelectLogRunner {
        abstract void doSelect(ResultSetAnalyzer resultSetAnalyzer);

        @Nullable
        protected String getTxtFilter() {
            return filterType == FilterType.FILTER ? txtFilter : null;
        }

        @Nullable
        protected Long getMinDurationNanoFilter() {
            return filterType == FilterType.FILTER ? minDurationNanos : null;
        }
    }

    private final SelectLogRunner selectAllLogStatements = new SelectLogRunner() {
        @Override
        public void doSelect(final ResultSetAnalyzer resultSetAnalyzer) {
            logRepository.getStatements(getTxtFilter(), getMinDurationNanoFilter(), resultSetAnalyzer);
        }
    };
    private final SelectLogRunner selectLogStatementsGroupByRawSql = new SelectLogRunner() {
        @Override
        public void doSelect(final ResultSetAnalyzer resultSetAnalyzer) {
            logRepository.getStatementsGroupByRawSQL(getTxtFilter(), getMinDurationNanoFilter(), resultSetAnalyzer);
        }
    };
    private final SelectLogRunner selectLogStatementsGroupByFilledSql = new SelectLogRunner() {
        @Override
        public void doSelect(final ResultSetAnalyzer resultSetAnalyzer) {
            logRepository.getStatementsGroupByFilledSQL(getTxtFilter(), getMinDurationNanoFilter(), resultSetAnalyzer);
        }
    };
    @Nullable
    private volatile String txtFilter;
    @Nullable
    private volatile Long minDurationNanos;
    private SelectLogRunner currentSelectLogRunner = selectAllLogStatements;
    private final RefreshDataTask refreshDataTask;
    private boolean tableStructureChanged = true;
    private GroupBy groupBy = GroupBy.NONE;
    private FilterType filterType = FilterType.FILTER;

    PerfLoggerController(final IClientConnectionDelegate clientConnectionDelegate,
            final AbstractLogReceiver logReceiver, final LogRepository logRepository) {
        this.clientConnectionDelegate = clientConnectionDelegate;
        this.logReceiver = logReceiver;
        this.logRepository = logRepository;

        logExporter = new LogExporter(logRepository);

        perfLoggerPanel = new PerfLoggerPanel(this);
        perfLoggerPanel.setCloseEnable(!logReceiver.isServerMode());

        final Timer timer = new Timer(true);
        refreshDataTask = new RefreshDataTask();
        timer.schedule(refreshDataTask, 1000, 1000);
    }

    JPanel getPanel() {
        return perfLoggerPanel;
    }

    void setTextFilter(@Nullable final String filter) {
        if (filter == null || filter.isEmpty()) {
            txtFilter = null;
        } else {
            txtFilter = filter;
        }
        refresh();
    }

    void setMinDurationFilter(@Nullable final Long durationMs) {
        if (durationMs == null) {
            minDurationNanos = null;
        } else {
            minDurationNanos = TimeUnit.MILLISECONDS.toNanos(durationMs);
        }
        refresh();
    }

    void setGroupBy(final GroupBy groupBy) {
        this.groupBy = groupBy;
        switch (groupBy) {
        case NONE:
            currentSelectLogRunner = selectAllLogStatements;
            break;
        case RAW_SQL:
            currentSelectLogRunner = selectLogStatementsGroupByRawSql;
            break;
        case FILLED_SQL:
            currentSelectLogRunner = selectLogStatementsGroupByFilledSql;
            break;
        }
        tableStructureChanged = true;
        refresh();
    }

    void setFilterType(final FilterType filterType) {
        this.filterType = filterType;
        refresh();
    }

    void onSelectStatement(final Long logId) {
        statementSelected(logId);
    }

    void onClear() {
        logRepository.clear();
        refresh();
        statementSelected(null);
    }

    void onPause() {
        if (logReceiver.isPaused()) {
            logReceiver.resumeReceivingLogs();
            perfLoggerPanel.setPaused(false);
        } else {
            logReceiver.pauseReceivingLogs();
            perfLoggerPanel.setPaused(true);
        }

    }

    void onClose() {
        refreshDataTask.cancel();
        logReceiver.dispose();
        logRepository.dispose();
        clientConnectionDelegate.close(perfLoggerPanel);
    }

    void onExportCsv() {
        exportCsv();
    }

    void onExportSql() {
        exportSql();
    }

    /**
     * To be executed in EDT
     */
    private void refresh() {
        if (filterType == FilterType.FILTER) {
            perfLoggerPanel.table.setTxtToHighlight(null);
            perfLoggerPanel.table.setMinDurationNanoToHighlight(null);
        } else {
            perfLoggerPanel.table.setTxtToHighlight(txtFilter);
            perfLoggerPanel.table.setMinDurationNanoToHighlight(minDurationNanos);
        }

        refreshDataTask.forceRefresh();
    }

    private void statementSelected(@Nullable final Long logId) {
        String txt1 = "";
        String txt2 = "";
        StatementLog statementLog = null;
        if (logId != null) {
            statementLog = logRepository.getStatementLog(logId);
        }
        long deltaTimestampBaseMillis = 0;

        if (statementLog != null) {
            switch (groupBy) {
            case NONE:
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
                deltaTimestampBaseMillis = statementLog.getTimestamp();
                break;
            case RAW_SQL:
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
                break;
            case FILLED_SQL:
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
                break;
            }

            final Throwable sqlException = statementLog.getSqlException();
            if (sqlException != null) {
                final CharArrayWriter writer = new CharArrayWriter();
                sqlException.printStackTrace(new PrintWriter(writer));
                txt2 += writer.toString();
            }
        }
        perfLoggerPanel.txtFieldRawSql.setText(txt1);
        perfLoggerPanel.txtFieldRawSql.select(0, 0);
        perfLoggerPanel.txtFieldFilledSql.setText(txt2);
        perfLoggerPanel.txtFieldFilledSql.select(0, 0);
        // scrollPaneSqlDetail1.setEnabled(txt1 != null);
        // scrollPaneSqlDetail2.setEnabled(txt2 != null);

        perfLoggerPanel.setDeltaTimestampBaseMillis(deltaTimestampBaseMillis);
    }

    private void exportSql() {
        final JFileChooser fileChooser = new JFileChooser();
        fileChooser.setFileFilter(new FileNameExtensionFilter("SQL file", "sql"));
        if (fileChooser.showSaveDialog(perfLoggerPanel) == JFileChooser.APPROVE_OPTION) {
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
        if (fileChooser.showSaveDialog(perfLoggerPanel) == JFileChooser.APPROVE_OPTION) {
            File targetFile = fileChooser.getSelectedFile();
            if (!targetFile.getName().toLowerCase().endsWith(".csv")) {
                targetFile = new File(targetFile.getAbsolutePath() + ".csv");
            }
            selectAllLogStatements.doSelect(logExporter.getCsvLogExporter(targetFile));
        }
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
            final String txtFilterFinal = txtFilter;
            final Long minDurationNanosFinal = minDurationNanos;
            if ((txtFilterFinal != null && txtFilterFinal.length() > 0)
                    || (minDurationNanosFinal != null && minDurationNanosFinal.longValue() > 0)) {
                txt.append(" - ");
                txt.append(TimeUnit.NANOSECONDS.toMillis(logRepository.getTotalExecAndFetchTimeNanos(txtFilter,
                        minDurationNanos)));
                txt.append("ms total filtered");
            }
            SwingUtilities.invokeLater(new Runnable() {
                @Override
                public void run() {
                    perfLoggerPanel.lblStatus.setText(txt.toString());
                }
            });
        }

        void forceRefresh() {
            lastRefreshTime = -1L;
        }

        void doRefreshData(final SelectLogRunner selectLogRunner) {
            selectLogRunner.doSelect(new ResultSetAnalyzer() {
                @Override
                public void analyze(final ResultSet resultSet) throws SQLException {
                    final ResultSetMetaData resultSetMetaData = resultSet.getMetaData();
                    final int columnCount = resultSetMetaData.getColumnCount();

                    final List<String> tempColumnNames = new ArrayList<String>();
                    final List<Class<?>> tempColumnTypes = new ArrayList<Class<?>>();
                    final List<Object[]> tempRows = new ArrayList<Object[]>();
                    try {
                        for (int i = 1; i <= columnCount; i++) {
                            tempColumnNames.add(resultSetMetaData.getColumnLabel(i).toUpperCase());
                            tempColumnTypes.add(Class.forName(resultSetMetaData.getColumnClassName(i)));
                        }

                        while (resultSet.next()) {
                            final Object[] row = new Object[columnCount];
                            for (int i = 1; i <= columnCount; i++) {
                                row[i - 1] = resultSet.getObject(i);
                            }
                            tempRows.add(row);
                        }
                    } catch (final ClassNotFoundException e) {
                        throw new RuntimeException(e);
                    }

                    SwingUtilities.invokeLater(new Runnable() {
                        @Override
                        public void run() {
                            perfLoggerPanel.setData(tempRows, tempColumnNames, tempColumnTypes, tableStructureChanged);
                            tableStructureChanged = false;
                        }
                    });
                }
            });
        }

    }

    enum GroupBy {
        NONE("-"), RAW_SQL("Raw SQL"), FILLED_SQL("Filled SQL");
        final private String title;

        GroupBy(final String title) {
            this.title = title;
        }

        @Override
        public String toString() {
            return title;
        }
    }

    enum FilterType {
        HIGHLIGHT("Highlight"), FILTER("Filter");
        final private String title;

        FilterType(final String title) {
            this.title = title;
        }

        @Override
        public String toString() {
            return title;
        }
    }
}
