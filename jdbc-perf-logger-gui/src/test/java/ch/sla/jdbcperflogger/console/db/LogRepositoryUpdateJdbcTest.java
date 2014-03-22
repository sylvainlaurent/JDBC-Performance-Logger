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
package ch.sla.jdbcperflogger.console.db;

import static ch.sla.jdbcperflogger.console.db.LogRepositoryConstants.ID_COLUMN;
import static java.util.UUID.randomUUID;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Properties;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import ch.sla.jdbcperflogger.StatementType;
import ch.sla.jdbcperflogger.TxCompletionType;
import ch.sla.jdbcperflogger.model.BatchedNonPreparedStatementsLog;
import ch.sla.jdbcperflogger.model.ConnectionInfo;
import ch.sla.jdbcperflogger.model.ResultSetLog;
import ch.sla.jdbcperflogger.model.StatementExecutedLog;
import ch.sla.jdbcperflogger.model.StatementLog;
import ch.sla.jdbcperflogger.model.TxCompleteLog;

@SuppressWarnings("null")
public class LogRepositoryUpdateJdbcTest {
    private LogRepositoryUpdateJdbc repositoryUpdate;
    private LogRepositoryRead repositoryRead;

    @Before
    public void setup() {
        repositoryUpdate = new LogRepositoryUpdateJdbc("test");
        repositoryRead = new LogRepositoryReadJdbc("test");
    }

    @After
    public void tearDown() {
        repositoryUpdate.dispose();
        repositoryRead.dispose();
    }

    @Test
    public void testSetup() {
        // just test setup and teardown
    }

    @Test
    public void testInsertAndRead() {
        final StatementLog log = insert1Log();

        final DetailedViewStatementLog readLog = repositoryRead.getStatementLog(1);
        assertEquals(log.getLogId(), readLog.getLogId());
        assertEquals(log.getRawSql(), readLog.getRawSql());
        assertEquals(log.getStatementType(), readLog.getStatementType());
        assertEquals(log.getThreadName(), readLog.getThreadName());
        assertEquals(log.getTimestamp(), readLog.getTimestamp());
    }

    @Test
    public void testUpdateExecutionAfterInsert() {
        final StatementLog stmtLog = insert1Log();
        final StatementExecutedLog statementExecutedLog = new StatementExecutedLog(stmtLog.getLogId(), 123, 567L,
                "myexception");
        repositoryUpdate.updateLogAfterExecution(statementExecutedLog);
        final DetailedViewStatementLog readLog = repositoryRead.getStatementLog(1);
        assertEquals(statementExecutedLog.getSqlException(), readLog.getSqlException());
    }

    @Test
    public void testUpdateResultSetAfterInsert() {
        final StatementLog stmtLog = insert1Log();
        final StatementExecutedLog statementExecutedLog = new StatementExecutedLog(stmtLog.getLogId(), 123, 567L,
                "myexception");
        repositoryUpdate.updateLogAfterExecution(statementExecutedLog);
        final ResultSetLog resultSetLog = new ResultSetLog(stmtLog.getLogId(), 321, 765);
        repositoryUpdate.updateLogWithResultSetLog(resultSetLog);

        repositoryRead.getStatements(new LogSearchCriteria(), new ResultSetAnalyzer() {

            @Override
            public void analyze(final ResultSet resultSet) throws SQLException {
                resultSet.next();
                assertEquals(1, resultSet.getLong(ID_COLUMN));
                assertEquals(stmtLog.getRawSql(), resultSet.getString(LogRepositoryConstants.RAW_SQL_COLUMN));
                assertEquals(stmtLog.isAutoCommit(), resultSet.getBoolean(LogRepositoryConstants.AUTOCOMMIT_COLUMN));
                assertEquals(12, resultSet.getInt(LogRepositoryConstants.CONNECTION_NUMBER_COLUMN));
                assertEquals(resultSetLog.getResultSetIterationTimeNanos(),
                        resultSet.getInt(LogRepositoryConstants.FETCH_TIME_COLUMN));
                assertEquals(statementExecutedLog.getExecutionTimeNanos(),
                        resultSet.getInt(LogRepositoryConstants.EXEC_TIME_COLUMN));
                assertEquals(
                        statementExecutedLog.getExecutionTimeNanos() + resultSetLog.getResultSetIterationTimeNanos(),
                        resultSet.getInt(LogRepositoryConstants.EXEC_PLUS_FETCH_TIME_COLUMN));
                assertEquals(resultSetLog.getNbRowsIterated(), resultSet.getInt(LogRepositoryConstants.NB_ROWS_COLUMN));
                assertEquals(stmtLog.getThreadName(), resultSet.getString(LogRepositoryConstants.THREAD_NAME_COLUMN));
                assertEquals(stmtLog.getTimeout(), resultSet.getInt(LogRepositoryConstants.TIMEOUT_COLUMN));
                assertEquals(stmtLog.getTimestamp(), resultSet.getTimestamp(LogRepositoryConstants.TSTAMP_COLUMN)
                        .getTime());
                assertEquals(stmtLog.getStatementType().getId(),
                        resultSet.getInt(LogRepositoryConstants.STMT_TYPE_COLUMN));
                assertTrue(resultSet.getBoolean(LogRepositoryConstants.ERROR_COLUMN));
            }
        }, false);

        final DetailedViewStatementLog readLog = repositoryRead.getStatementLog(1);
        assertEquals(resultSetLog.getLogId(), readLog.getLogId());
    }

    @Test
    public void testDeleteOldRowsIfTooMany() {
        final StatementLog log = insert1Log();

        assertEquals(1, countRowsInTable("statement_log"));
        // nothing to delete of not too many rows
        repositoryUpdate.deleteOldRowsIfTooMany();
        assertEquals(1, countRowsInTable("statement_log"));

        for (int i = 1; i < 2 * LogRepositoryUpdateJdbc.NB_ROWS_MAX; i++) {
            final StatementLog newLog = new StatementLog(log.getConnectionUuid(), randomUUID(),
                    System.currentTimeMillis(), StatementType.BASE_NON_PREPARED_STMT, "myrawsql" + i, Thread
                            .currentThread().getName(), i, i % 2 == 0);
            repositoryUpdate.addStatementLog(newLog);
        }
        assertEquals(2 * LogRepositoryUpdateJdbc.NB_ROWS_MAX, countRowsInTable("statement_log"));
        repositoryUpdate.deleteOldRowsIfTooMany();
        assertTrue(countRowsInTable("statement_log") <= LogRepositoryUpdateJdbc.NB_ROWS_MAX);
    }

    @Test
    public void testClear() {
        insert1Log();

        assertEquals(1, countRowsInTable("statement_log"));
        repositoryUpdate.clear();
        assertEquals(0, countRowsInTable("statement_log"));
    }

    @Test
    public void testDelete() {
        final StatementLog log1 = insert1Log();
        final StatementLog log2 = new StatementLog(log1.getConnectionUuid(), randomUUID(), System.currentTimeMillis(),
                StatementType.BASE_NON_PREPARED_STMT, "myrawsql2", Thread.currentThread().getName(), 2, false);
        repositoryUpdate.addStatementLog(log2);

        final StatementLog log3 = new StatementLog(log1.getConnectionUuid(), randomUUID(), System.currentTimeMillis(),
                StatementType.BASE_NON_PREPARED_STMT, "myrawsql3", Thread.currentThread().getName(), 3, false);
        repositoryUpdate.addStatementLog(log3);

        assertEquals(3, countRowsInTable("statement_log"));
        repositoryUpdate.deleteStatementLog(1, 3);
        assertEquals(1, countRowsInTable("statement_log"));
    }

    @Test
    public void testaddBatchedNonPreparedStatementsLog() {
        final StatementLog log = insert1Log();

        final List<String> sqlList = Arrays.asList("st1", "st2", "st3");
        final BatchedNonPreparedStatementsLog batchedLogs = new BatchedNonPreparedStatementsLog(
                log.getConnectionUuid(), randomUUID(), System.currentTimeMillis(), sqlList, "myThread", 13, true);
        repositoryUpdate.addBatchedNonPreparedStatementsLog(batchedLogs);
        assertEquals(2, countRowsInTable("statement_log"));
        assertEquals(3, countRowsInTable("batched_statement_log"));
    }

    @Test
    public void testaddTxComplete() {
        final StatementLog log = insert1Log();

        final TxCompleteLog txCompleteLog = new TxCompleteLog(log.getConnectionUuid(), System.currentTimeMillis(),
                TxCompletionType.SET_SAVE_POINT, 12, "mythread", "mySavePoint");
        repositoryUpdate.addTxCompletionLog(txCompleteLog);
        assertEquals(2, countRowsInTable("statement_log"));

        final DetailedViewStatementLog readTxLog = repositoryRead.getStatementLog(2);
        assertEquals(StatementType.TRANSACTION, readTxLog.getStatementType());
        assertEquals("SET_SAVE_POINT mySavePoint", readTxLog.getRawSql());
        assertEquals(txCompleteLog.getThreadName(), readTxLog.getThreadName());
        assertEquals(txCompleteLog.getTimestamp(), readTxLog.getTimestamp());
    }

    private StatementLog insert1Log() {
        final Properties connProps = new Properties();
        connProps.setProperty("myprop", "myval");
        final ConnectionInfo connectionInfo = new ConnectionInfo(randomUUID(), 12, "jdbc:toto", new Date(), connProps);
        repositoryUpdate.addConnection(connectionInfo);

        final StatementLog log = new StatementLog(connectionInfo.getUuid(), randomUUID(), System.currentTimeMillis(),
                StatementType.BASE_NON_PREPARED_STMT, "myrawsql", Thread.currentThread().getName(), 123, true);
        repositoryUpdate.addStatementLog(log);
        return log;
    }

    private int countRowsInTable(final String table) {
        try (Statement stmt = repositoryUpdate.connectionUpdate.createStatement()) {
            try (ResultSet rset = stmt.executeQuery("select count(1) from " + table)) {
                rset.next();
                return rset.getInt(1);
            }
        } catch (final SQLException e) {
            throw new RuntimeException(e);
        }
    }
}
