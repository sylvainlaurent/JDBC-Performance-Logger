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
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.concurrent.TimeUnit;

import org.eclipse.jdt.annotation.Nullable;

import ch.sla.jdbcperflogger.StatementType;
import ch.sla.jdbcperflogger.console.db.DetailedViewStatementLog;
import ch.sla.jdbcperflogger.console.db.LogRepositoryConstants;
import ch.sla.jdbcperflogger.console.db.LogRepositoryRead;
import ch.sla.jdbcperflogger.console.db.ResultSetAnalyzer;

public class LogExporter {
    private final LogRepositoryRead logRepository;

    LogExporter(final LogRepositoryRead logRepository) {
        this.logRepository = logRepository;
    }

    SqlLogExporter getSqlLogExporter(final File exportFile) {
        return new SqlLogExporter(exportFile);
    }

    CsvLogExporter getCsvLogExporter(final File exportFile) {
        return new CsvLogExporter(exportFile);
    }

    String getBatchedExecutions(final DetailedViewStatementLog statementLog) {
        final StringBuilder strBuilder = new StringBuilder();
        logRepository.getBatchStatementExecutions(statementLog.getLogId(), resultSet -> {
            while (resultSet.next()) {
                strBuilder.append("/* #");
                strBuilder.append(resultSet.getInt(1));
                strBuilder.append(" */ ");
                final String sql = resultSet.getString(2) + ";";
                strBuilder.append(sql);
                strBuilder.append("\n");
            }
        });
        return strBuilder.toString();
    }

    private class SqlLogExporter implements ResultSetAnalyzer {
        private final File exportFile;

        SqlLogExporter(final File exportFile) {
            this.exportFile = exportFile;
        }

        @Override
        public void analyze(final ResultSet resultSet) throws SQLException {
            try (PrintWriter writer = new PrintWriter(exportFile)) {

                final SimpleDateFormat tstampFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");

                while (resultSet.next()) {
                    final Timestamp timestamp = resultSet.getTimestamp(LogRepositoryConstants.TSTAMP_COLUMN);
                    writer.print("/*");
                    writer.print(tstampFormat.format(timestamp));
                    writer.print(" exec=");
                    writer.print(
                            TimeUnit.NANOSECONDS.toMillis(resultSet.getLong(LogRepositoryConstants.EXEC_TIME_COLUMN)));
                    writer.print("ms ");

                    final int nbRows = resultSet.getInt(LogRepositoryConstants.NB_ROWS_COLUMN);
                    if (!resultSet.wasNull()) {
                        writer.print(nbRows);
                        writer.print(", row(s) fetched in ");
                        writer.print(TimeUnit.NANOSECONDS
                                .toMillis(resultSet.getLong(LogRepositoryConstants.FETCH_TIME_COLUMN)));
                        writer.print("ms ");
                    }

                    final StatementType stmtType = StatementType
                            .fromId(resultSet.getByte(LogRepositoryConstants.STMT_TYPE_COLUMN));
                    switch (stmtType) {
                    case NON_PREPARED_BATCH_EXECUTION:
                    case PREPARED_BATCH_EXECUTION:
                        writer.print(stmtType.name());
                        writer.println("*/");
                        final DetailedViewStatementLog statementLog = logRepository
                                .getStatementLog(resultSet.getLong("ID"));
                        if (statementLog != null) {
                            writer.println(getBatchedExecutions(statementLog));
                        }
                        break;
                    default:
                        writer.print("*/ ");
                        final String filledSql = resultSet.getString(LogRepositoryConstants.FILLED_SQL_COLUMN);
                        writer.print(filledSql);
                        if (!filledSql.endsWith(";")) {
                            writer.print(";");
                        }
                    }
                    writer.println();
                }

            } catch (final FileNotFoundException e) {
                throw new RuntimeException(e);
            }

        }
    }

    private class CsvLogExporter implements ResultSetAnalyzer {
        private final File exportFile;

        CsvLogExporter(final File exportFile) {
            this.exportFile = exportFile;
        }

        @Override
        public void analyze(final ResultSet resultSet) throws SQLException {
            final PrintWriter writer;
            try {
                writer = new PrintWriter(exportFile);
            } catch (final FileNotFoundException e) {
                throw new RuntimeException(e);
            }

            final SimpleDateFormat tstampFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
            try {
                writer.println(
                        "TIMESTAMP\tEXEC_TIME_S\tFETCH_TIME_S\tEXEC_PLUS_RSET_USAGE_TIME_S\tFETCHED_ROWS\tSTMT_TYPE\tRAW_STATEMENT\tFILLED_STATEMENT\tTHREAD_NAME\tCONNECTION_ID\tTIMEOUT\tERROR");
                while (resultSet.next()) {
                    final Timestamp timestamp = resultSet.getTimestamp(LogRepositoryConstants.TSTAMP_COLUMN);
                    writer.print(tstampFormat.format(timestamp));
                    writer.print('\t');
                    writer.print(resultSet.getLong(LogRepositoryConstants.EXEC_TIME_COLUMN));
                    writer.print("e-9");

                    final int nbRows = resultSet.getInt(LogRepositoryConstants.NB_ROWS_COLUMN);
                    if (!resultSet.wasNull()) {
                        writer.print('\t');
                        writer.print(resultSet.getLong(LogRepositoryConstants.FETCH_TIME_COLUMN));
                        writer.print("e-9");
                        writer.print('\t');
                        writer.print(resultSet.getLong(LogRepositoryConstants.EXEC_PLUS_RSET_USAGE_TIME));
                        writer.print("e-9");
                        writer.print('\t');
                        writer.print(nbRows);
                    } else {
                        writer.print("\t\t");
                        // fetch==0
                        writer.print(resultSet.getLong(LogRepositoryConstants.EXEC_TIME_COLUMN));
                        writer.print("e-9");
                        writer.print('\t');
                    }

                    final StatementType stmtType = StatementType
                            .fromId(resultSet.getByte(LogRepositoryConstants.STMT_TYPE_COLUMN));
                    writer.print('\t');
                    writer.print(stmtType.name());
                    writer.print('\t');
                    writer.print(escapeStrings(resultSet.getString(LogRepositoryConstants.RAW_SQL_COLUMN)));
                    writer.print('\t');
                    switch (stmtType) {
                    case NON_PREPARED_BATCH_EXECUTION:
                    case PREPARED_BATCH_EXECUTION:
                        break;
                    default:
                        writer.print(escapeStrings(resultSet.getString(LogRepositoryConstants.FILLED_SQL_COLUMN)));
                    }

                    writer.print('\t');
                    writer.print(resultSet.getString(LogRepositoryConstants.THREAD_NAME_COLUMN));
                    writer.print('\t');
                    writer.print(resultSet.getString(LogRepositoryConstants.CONNECTION_NUMBER_COLUMN));
                    writer.print('\t');
                    writer.print(resultSet.getString(LogRepositoryConstants.TIMEOUT_COLUMN));
                    writer.print('\t');
                    writer.print(resultSet.getString(LogRepositoryConstants.ERROR_COLUMN));

                    writer.println();
                }

            } finally {
                writer.close();
            }
        }

        private String escapeStrings(@Nullable final String orig) {
            if (orig == null) {
                return "";
            }
            return orig;
        }
    }

}
