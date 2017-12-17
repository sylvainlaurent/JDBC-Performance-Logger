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

import java.io.File;
import java.sql.Date;
import java.sql.ResultSetMetaData;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javax.swing.ImageIcon;
import javax.swing.JFileChooser;
import javax.swing.SwingUtilities;
import javax.swing.filechooser.FileNameExtensionFilter;

import ch.sla.jdbcperflogger.console.db.*;
import org.eclipse.jdt.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.sla.jdbcperflogger.StatementType;
import ch.sla.jdbcperflogger.console.net.AbstractLogReceiver;
import ch.sla.jdbcperflogger.model.ConnectionInfo;

public class PerfLoggerController {
    private final static Logger LOGGER = LoggerFactory.getLogger(PerfLoggerController.class);

    private final AbstractLogReceiver logReceiver;
    private final LogRepositoryUpdate logRepositoryUpdate;
    private final LogRepositoryRead logRepositoryRead;
    private final IClientConnectionDelegate clientConnectionDelegate;
    private final LogExporter logExporter;
    private final PerfLoggerPanel perfLoggerPanel;

    private abstract class SelectLogRunner {
        abstract void doSelect(ResultSetAnalyzer resultSetAnalyzer);

    }

    private final SelectLogRunner selectAllLogStatements = new SelectLogRunner() {
        @Override
        public void doSelect(final ResultSetAnalyzer resultSetAnalyzer) {
            logRepositoryRead.getStatements(createSearchCriteria(), resultSetAnalyzer, false);
        }
    };
    private final SelectLogRunner selectAllLogStatementsWithFilledSql = new SelectLogRunner() {
        @Override
        public void doSelect(final ResultSetAnalyzer resultSetAnalyzer) {
            logRepositoryRead.getStatements(createSearchCriteria(), resultSetAnalyzer, true);
        }
    };
    private final SelectLogRunner selectLogStatementsGroupByRawSql = new SelectLogRunner() {
        @Override
        public void doSelect(final ResultSetAnalyzer resultSetAnalyzer) {
            logRepositoryRead.getStatementsGroupByRawSQL(createSearchCriteria(), resultSetAnalyzer);
        }
    };
    private final SelectLogRunner selectLogStatementsGroupByFilledSql = new SelectLogRunner() {
        @Override
        public void doSelect(final ResultSetAnalyzer resultSetAnalyzer) {
            logRepositoryRead.getStatementsGroupByFilledSQL(createSearchCriteria(), resultSetAnalyzer);
        }
    };
    @Nullable
    private volatile String txtFilter;
    @Nullable
    private volatile String sqlPassthroughFilter;
    @Nullable
    private volatile Long minDurationNanos;
    private boolean excludeCommits;

    private SelectLogRunner currentSelectLogRunner = selectAllLogStatements;
    private boolean tableStructureChanged = true;
    private GroupBy groupBy = GroupBy.NONE;
    private FilterType filterType = FilterType.HIGHLIGHT;
    private final RefreshDataTask refreshDataTask;
    private final ScheduledExecutorService refreshDataScheduledExecutorService;
    private boolean lastSelectFromRepositoryIsInError = false;

    PerfLoggerController(final IClientConnectionDelegate clientConnectionDelegate,
            final AbstractLogReceiver logReceiver, final LogRepositoryUpdate logRepositoryUpdate,
            final LogRepositoryRead logRepositoryRead) {
        this.clientConnectionDelegate = clientConnectionDelegate;
        this.logReceiver = logReceiver;
        this.logRepositoryUpdate = logRepositoryUpdate;
        this.logRepositoryRead = logRepositoryRead;

        logExporter = new LogExporter(logRepositoryRead);

        perfLoggerPanel = new PerfLoggerPanel(this);
        perfLoggerPanel.setCloseEnable(!logReceiver.isServerMode());

        refreshDataTask = new RefreshDataTask();
        refreshDataScheduledExecutorService = Executors.newSingleThreadScheduledExecutor();
        refreshDataScheduledExecutorService.scheduleWithFixedDelay(refreshDataTask, 1, 2, TimeUnit.SECONDS);
    }

    PerfLoggerPanel getPanel() {
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

    void setSqlPassThroughFilter(@Nullable final String filter) {
        if (filter == null || filter.isEmpty()) {
            sqlPassthroughFilter = null;
        } else {
            sqlPassthroughFilter = filter;
        }
        refresh();
    }

    void appendFilter(final String columnName, final @Nullable Object value) {
        String filter = sqlPassthroughFilter;
        if (filter != null) {
            filter += " AND ";
        } else {
            filter = "";
        }
        filter += columnName;
        if (value != null) {
            filter += "='" + value + "'";
        } else {
            filter += " is null";
        }
        perfLoggerPanel.setAdvancedFilter(filter);

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

    void setExcludeCommits(final boolean excludeCommits) {
        this.excludeCommits = excludeCommits;
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
        perfLoggerPanel.setAdvancedFilter(null);
        refresh();
    }

    void setFilterType(final FilterType filterType) {
        this.filterType = filterType;
        refresh();
    }

    void onSelectStatement(final @Nullable Long logId) {
        statementSelected(logId);
    }

    public void onDeleteSelectedStatements(final long... logIds) {
        logRepositoryUpdate.deleteStatementLog(logIds);
        refresh();
    }

    void onClear() {
        logRepositoryUpdate.clear();
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
        refreshDataScheduledExecutorService.shutdownNow();
        try {
            refreshDataScheduledExecutorService.awaitTermination(5, TimeUnit.SECONDS);
        } catch (final InterruptedException e) {
            e.printStackTrace();
        }
        logReceiver.dispose();
        logRepositoryUpdate.dispose();
        logRepositoryRead.dispose();
        clientConnectionDelegate.close(this);
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
        perfLoggerPanel.setTxtToHighlight(txtFilter);

        refreshDataTask.forceRefresh();
        refreshDataScheduledExecutorService.submit(refreshDataTask);
    }

    private void statementSelected(@Nullable final Long logId) {
        String txt1 = "";
        String txt2 = "";
        String connectionUrl = null;
        String connectionCreationDate = null;
        Long connectionCreationDurationMillis = null;
        String connectionPropertiesString = null;
        DetailedViewStatementLog statementLog = null;
        if (logId != null) {
            statementLog = logRepositoryRead.getStatementLog(logId);
        }
        long deltaTimestampBaseMillis = 0;

        if (statementLog != null) {

            final StatementType statementType = statementLog.getStatementType();
            switch (groupBy) {
            case NONE:
                txt1 = statementLog.getRawSql();
                if (statementType != null) {
                    switch (statementType) {
                    case NON_PREPARED_BATCH_EXECUTION:
                        txt1 = logExporter.getBatchedExecutions(statementLog);
                        txt2 = txt1;
                        break;
                    case PREPARED_BATCH_EXECUTION:
                        txt2 = logExporter.getBatchedExecutions(statementLog);
                        break;
                    case BASE_PREPARED_STMT:
                    case PREPARED_QUERY_STMT:
                    default:
                        txt2 = statementLog.getFilledSql();
                        break;
                    }
                }
                deltaTimestampBaseMillis = statementLog.getTimestamp();

                final ConnectionInfo connectionInfo = statementLog.getConnectionInfo();
                connectionUrl = connectionInfo.getUrl();
                connectionPropertiesString = connectionInfo.getConnectionProperties().toString();
                final SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                connectionCreationDate = format.format(connectionInfo.getCreationDate());
                connectionCreationDurationMillis = TimeUnit.NANOSECONDS
                        .toMillis(connectionInfo.getConnectionCreationDuration());
                break;
            case RAW_SQL:
                if (statementType != null) {
                    switch (statementType) {
                    case BASE_NON_PREPARED_STMT:
                    case BASE_PREPARED_STMT:
                    case PREPARED_BATCH_EXECUTION:
                    case PREPARED_QUERY_STMT:
                    case NON_PREPARED_QUERY_STMT:
                    case TRANSACTION:
                        txt1 = statementLog.getRawSql();
                        break;
                    case NON_PREPARED_BATCH_EXECUTION:
                        txt1 = "Cannot display details in \"Group by\" modes";
                        break;
                    }
                }
                break;
            case FILLED_SQL:
                if (statementType != null) {
                    switch (statementType) {
                    case BASE_NON_PREPARED_STMT:
                    case PREPARED_BATCH_EXECUTION:
                    case NON_PREPARED_QUERY_STMT:
                        txt1 = statementLog.getRawSql();
                        break;
                    case BASE_PREPARED_STMT:
                    case PREPARED_QUERY_STMT:
                    case TRANSACTION:
                        txt1 = statementLog.getRawSql();
                        txt2 = statementLog.getFilledSql();
                        break;
                    case NON_PREPARED_BATCH_EXECUTION:
                        txt1 = "Cannot display details in \"Group by\" modes";
                        break;
                    }
                }
                break;
            }

            final String sqlException = statementLog.getSqlException();
            if (sqlException != null) {
                txt1 += "\n\n" + sqlException;
                txt2 += "\n\n" + sqlException;
            }
        }
        if (!txt1.equals(perfLoggerPanel.txtFieldRawSql.getText())) {
            perfLoggerPanel.txtFieldRawSql.setText(txt1);
            perfLoggerPanel.txtFieldRawSql.select(0, 0);
        }
        if (!txt2.equals(perfLoggerPanel.txtFieldFilledSql.getText())) {
            perfLoggerPanel.txtFieldFilledSql.setText(txt2);
            perfLoggerPanel.txtFieldFilledSql.select(0, 0);
        }
        perfLoggerPanel.setTxtToHighlight(txtFilter);
        perfLoggerPanel.connectionUrlField.setText(connectionUrl);
        perfLoggerPanel.connectionCreationDateField.setText(connectionCreationDate);
        perfLoggerPanel.connectionCreationDurationField
                .setText(connectionCreationDurationMillis != null ? connectionCreationDurationMillis.toString() : "");
        perfLoggerPanel.connectionPropertiesField.setText(connectionPropertiesString);

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
            selectAllLogStatementsWithFilledSql.doSelect(logExporter.getSqlLogExporter(targetFile));
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
            selectAllLogStatementsWithFilledSql.doSelect(logExporter.getCsvLogExporter(targetFile));
        }
    }

    @Nullable
    protected String getTxtFilter() {
        return filterType == FilterType.FILTER ? txtFilter : null;
    }

    @Nullable
    protected Long getMinDurationNanoFilter() {
        return filterType == FilterType.FILTER ? minDurationNanos : null;
    }

    protected LogSearchCriteria createSearchCriteria() {
        final LogSearchCriteria searchCriteria = new LogSearchCriteria();
        searchCriteria.setFilter(getTxtFilter());
        searchCriteria.setMinDurationNanos(getMinDurationNanoFilter());
        searchCriteria.setRemoveTransactionCompletions(excludeCommits);
        searchCriteria.setSqlPassThroughFilter(sqlPassthroughFilter);
        return searchCriteria;
    }

    /**
     * A task that regularly polls the associated {@link LogRepositoryUpdate} to check for new statements to display. If
     * the UI must be refreshed it is later done in the EDT.
     *
     * @author slaurent
     */
    private class RefreshDataTask implements Runnable {
        private volatile long lastRefreshTime;
        private int connectionsCount;

        @Override
        public void run() {
            try {
                doRun();
            } catch (final Exception exc) {
                // we must catch any exception otherwise the scheduling stops
                LOGGER.error("Error in background refresh task", exc);
            }
        }

        private void doRun() {

            if (logRepositoryUpdate.getLastModificationTime() <= lastRefreshTime
                    && connectionsCount == logReceiver.getConnectionsCount()) {
                return;
            }
            connectionsCount = logReceiver.getConnectionsCount();

            lastRefreshTime = logRepositoryUpdate.getLastModificationTime();
            doRefreshData(currentSelectLogRunner);

            final StringBuilder txt = new StringBuilder();
            if (logReceiver.getConnectionsCount() == 0) {
                perfLoggerPanel.lblConnectionStatus
                        .setIcon(new ImageIcon(PerfLoggerController.class.getResource("/icons/network-offline.png")));
                final Throwable lastConnectionError = logReceiver.getLastConnectionError();
                if (lastConnectionError != null) {
                    perfLoggerPanel.lblConnectionStatus.setToolTipText(lastConnectionError.toString());
                } else {
                    perfLoggerPanel.lblConnectionStatus.setToolTipText("");
                }
            } else {
                perfLoggerPanel.lblConnectionStatus.setIcon(
                        new ImageIcon(PerfLoggerController.class.getResource("/icons/network-transmit-receive.png")));
                if (logReceiver.isServerMode()) {
                    perfLoggerPanel.lblConnectionStatus.setToolTipText(connectionsCount + " connection(s)");
                } else {
                    perfLoggerPanel.lblConnectionStatus.setToolTipText("Connected");
                }
            }
            txt.append(logRepositoryRead.countStatements());
            txt.append(" statements logged - ");
            txt.append(TimeUnit.NANOSECONDS.toMillis(logRepositoryRead.getTotalExecAndFetchTimeNanos()));
            txt.append("ms total execution time (with fetch)");

            final LogSearchCriteria searchCriteria = createSearchCriteria();
            if (searchCriteria.atLeastOneFilterApplied()) {
                txt.append(" - ");
                txt.append(
                        TimeUnit.NANOSECONDS.toMillis(logRepositoryRead.getTotalExecAndFetchTimeNanos(searchCriteria)));
                txt.append("ms total filtered");
            }
            final Long lastLostMessageTime = logRepositoryUpdate.getLastLostMessageTime();
            if (lastLostMessageTime != null) {
                txt.append(" - WARNING: missed statements on ");
                txt.append(DateFormat.getTimeInstance().format(new Date(lastLostMessageTime)));
            }

            SwingUtilities.invokeLater(() -> perfLoggerPanel.lblStatus.setText(txt.toString()));
        }

        void forceRefresh() {
            lastRefreshTime = -1L;
        }

        void doRefreshData(final SelectLogRunner selectLogRunner) {
            try {
                selectLogRunner.doSelect(resultSet -> {
                    final ResultSetMetaData resultSetMetaData = resultSet.getMetaData();
                    final int columnCount = resultSetMetaData.getColumnCount();

                    final List<String> tempColumnNames = new ArrayList<>();
                    final List<Class<?>> tempColumnTypes = new ArrayList<>();
                    final List<Object[]> tempRows = new ArrayList<>();
                    try {
                        for (int i = 1; i <= columnCount; i++) {
                            String columnLabel = resultSetMetaData.getColumnLabel(i);
                            tempColumnNames.add(columnLabel.toUpperCase());
                            if (LogRepositoryConstants.TRANSACTION_ISOLATION_COLUMN.equals(columnLabel)) {
                                tempColumnTypes.add(String.class);
                            } else {
                                tempColumnTypes.add(Class.forName(resultSetMetaData.getColumnClassName(i)));
                            }
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

                    SwingUtilities.invokeLater(() -> {
                        if (lastSelectFromRepositoryIsInError) {
                            perfLoggerPanel.txtFieldRawSql.setText("");
                        }
                        lastSelectFromRepositoryIsInError = false;
                        perfLoggerPanel.setData(tempRows, tempColumnNames, tempColumnTypes,
                                tableStructureChanged);
                        tableStructureChanged = false;
                    });
                });
            } catch (final Exception ex) {
                LOGGER.debug("error retrieving log statements", ex);
                SwingUtilities.invokeLater(() -> {
                    lastSelectFromRepositoryIsInError = true;
                    perfLoggerPanel.txtFieldRawSql.setText(ex.getMessage());
                    perfLoggerPanel.setData(new ArrayList<>(), new ArrayList<>(),
                            new ArrayList<>(), true);
                    tableStructureChanged = true;
                });
            }
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
