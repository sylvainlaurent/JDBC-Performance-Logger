package slaurent.jdbcperflogger.gui;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.concurrent.TimeUnit;

import slaurent.jdbcperflogger.StatementType;
import slaurent.jdbcperflogger.model.StatementLog;

public class LogExporter {
    private final LogRepository logRepository;

    LogExporter(LogRepository logRepository) {
        this.logRepository = logRepository;
    }

    SqlLogExporter getSqlLogExporter(File exportFile) {
        return new SqlLogExporter(exportFile);
    }

    CsvLogExporter getCsvLogExporter(File exportFile) {
        return new CsvLogExporter(exportFile);
    }

    String getBatchedExecutions(StatementLog statementLog) {
        final StringBuilder strBuilder = new StringBuilder();
        logRepository.getBatchStatementExecutions(statementLog.getLogId(), new ResultSetAnalyzer() {
            @Override
            public void analyze(ResultSet resultSet) throws SQLException {
                while (resultSet.next()) {
                    strBuilder.append("/* #");
                    strBuilder.append(resultSet.getInt(1));
                    strBuilder.append(" */ ");
                    strBuilder.append(resultSet.getString(2));
                    strBuilder.append(";\n");
                }
            }
        });
        return strBuilder.toString();
    }

    private class SqlLogExporter implements ResultSetAnalyzer {
        private final File exportFile;

        SqlLogExporter(File exportFile) {
            this.exportFile = exportFile;
        }

        @Override
        public void analyze(ResultSet resultSet) throws SQLException {
            final PrintWriter writer;
            try {
                writer = new PrintWriter(exportFile);
            } catch (final FileNotFoundException e) {
                throw new RuntimeException(e);
            }

            final SimpleDateFormat tstampFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
            try {

                while (resultSet.next()) {
                    final Timestamp timestamp = resultSet.getTimestamp(LogRepository.TSTAMP_COLUMN);
                    writer.print("/*");
                    writer.print(tstampFormat.format(timestamp));
                    writer.print(" exec=");
                    writer.print(TimeUnit.NANOSECONDS.toMillis(resultSet.getLong(LogRepository.EXEC_TIME_COLUMN)));
                    writer.print("ms ");

                    final int nbRows = resultSet.getInt(LogRepository.NB_ROWS_COLUMN);
                    if (!resultSet.wasNull()) {
                        writer.print(nbRows);
                        writer.print(", row(s) fetched in ");
                        writer.print(TimeUnit.NANOSECONDS.toMillis(resultSet.getLong(LogRepository.FETCH_TIME_COLUMN)));
                        writer.print("ms ");
                    }

                    final StatementType stmtType = StatementType.fromId(resultSet
                            .getByte(LogRepository.STMT_TYPE_COLUMN));
                    switch (stmtType) {
                    case NON_PREPARED_BATCH_EXECUTION:
                    case PREPARED_BATCH_EXECUTION:
                        writer.print(stmtType.name());
                        writer.println("*/");
                        final StatementLog statementLog = logRepository.getStatementLog(resultSet.getLong("ID"));
                        writer.println(getBatchedExecutions(statementLog));
                        break;
                    default:
                        writer.print("*/ ");
                        final String filledSql = resultSet.getString(LogRepository.FILLED_SQL_COLUMN);
                        writer.print(filledSql);
                        if (!filledSql.endsWith(";")) {
                            writer.print(";");
                        }
                    }
                    writer.println();
                }

            } finally {
                writer.close();
            }
        }

    }

    private class CsvLogExporter implements ResultSetAnalyzer {
        private final File exportFile;

        CsvLogExporter(File exportFile) {
            this.exportFile = exportFile;
        }

        @Override
        public void analyze(ResultSet resultSet) throws SQLException {
            final PrintWriter writer;
            try {
                writer = new PrintWriter(exportFile);
            } catch (final FileNotFoundException e) {
                throw new RuntimeException(e);
            }

            final SimpleDateFormat tstampFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
            try {
                writer.println("TIMESTAMP\tEXEC_TIME_S\tFETCH_TIME_S\tEXEC_PLUS_FETCH_TIME_S\tFETCHED_ROWS\tSTMT_TYPE\tRAW_STATEMENT\tFILLED_STATEMENT\tTHREAD_NAME\tCONNECTION_ID\tERROR");
                while (resultSet.next()) {
                    final Timestamp timestamp = resultSet.getTimestamp(LogRepository.TSTAMP_COLUMN);
                    writer.print(tstampFormat.format(timestamp));
                    writer.print('\t');
                    writer.print(resultSet.getLong(LogRepository.EXEC_TIME_COLUMN));
                    writer.print("e-9");

                    final int nbRows = resultSet.getInt(LogRepository.NB_ROWS_COLUMN);
                    if (!resultSet.wasNull()) {
                        writer.print('\t');
                        writer.print(resultSet.getLong(LogRepository.FETCH_TIME_COLUMN));
                        writer.print("e-9");
                        writer.print('\t');
                        writer.print(resultSet.getLong(LogRepository.EXEC_PLUS_FETCH_TIME_COLUMN));
                        writer.print("e-9");
                        writer.print('\t');
                        writer.print(nbRows);
                    } else {
                        writer.print("\t\t");
                        // fetch==0
                        writer.print(resultSet.getLong(LogRepository.EXEC_TIME_COLUMN));
                        writer.print("e-9");
                        writer.print('\t');
                    }

                    final StatementType stmtType = StatementType.fromId(resultSet
                            .getByte(LogRepository.STMT_TYPE_COLUMN));
                    writer.print('\t');
                    writer.print(stmtType.name());
                    writer.print('\t');
                    writer.print(escapeStrings(resultSet.getString(LogRepository.RAW_SQL_COLUMN)));
                    writer.print('\t');
                    switch (stmtType) {
                    case NON_PREPARED_BATCH_EXECUTION:
                    case PREPARED_BATCH_EXECUTION:
                        break;
                    default:
                        writer.print(escapeStrings(resultSet.getString(LogRepository.FILLED_SQL_COLUMN)));
                    }

                    writer.print('\t');
                    writer.print(resultSet.getString(LogRepository.THREAD_NAME_COLUMN));
                    writer.print('\t');
                    writer.print(resultSet.getString(LogRepository.CONNECTION_ID_COLUMN));
                    writer.print('\t');
                    writer.print(resultSet.getString(LogRepository.ERROR_COLUMN));

                    writer.println();
                }

            } finally {
                writer.close();
            }
        }

        private String escapeStrings(String orig) {
            return orig;
        }
    }

}
