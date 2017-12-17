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
import static ch.sla.jdbcperflogger.console.db.LogRepositoryConstants.TRANSACTION_ISOLATION_COLUMN;
import static java.util.UUID.randomUUID;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Properties;

import org.junit.Test;

import ch.sla.jdbcperflogger.StatementType;
import ch.sla.jdbcperflogger.TxCompletionType;
import ch.sla.jdbcperflogger.model.BatchedNonPreparedStatementsLog;
import ch.sla.jdbcperflogger.model.BatchedPreparedStatementsLog;
import ch.sla.jdbcperflogger.model.ConnectionInfo;
import ch.sla.jdbcperflogger.model.ResultSetLog;
import ch.sla.jdbcperflogger.model.StatementExecutedLog;
import ch.sla.jdbcperflogger.model.StatementLog;
import ch.sla.jdbcperflogger.model.TxCompleteLog;

@SuppressWarnings("null")
public class LogRepositoryUpdateJdbcTest extends AbstractLogRepositoryTest {
    @Test
    public void testSetup() {
        // just test setup and teardown
    }

    @Test
    public void testInsertAndRead() {
        final ConnectionInfo connectionInfo = insert1Connection();
        final StatementLog log = insert1Log(connectionInfo);

        final DetailedViewStatementLog readLog = repositoryRead.getStatementLog(1);
        assertEquals(log.getLogId(), readLog.getLogId());
        assertEquals(log.getRawSql(), readLog.getRawSql());
        assertEquals(log.getStatementType(), readLog.getStatementType());
        assertEquals(log.getThreadName(), readLog.getThreadName());
        assertEquals(log.getTimestamp(), readLog.getTimestamp());
        assertEquals(connectionInfo.getUrl(), readLog.getConnectionInfo().getUrl());
        assertEquals(connectionInfo.getConnectionNumber(), readLog.getConnectionInfo().getConnectionNumber());
        assertEquals(connectionInfo.getCreationDate(), readLog.getConnectionInfo().getCreationDate());
        assertEquals(connectionInfo.getConnectionCreationDuration(),
                readLog.getConnectionInfo().getConnectionCreationDuration());
        assertEquals(connectionInfo.getConnectionProperties(), readLog.getConnectionInfo().getConnectionProperties());
        assertEquals(connectionInfo.getUuid(), readLog.getConnectionInfo().getUuid());
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
        final ResultSetLog resultSetLog = new ResultSetLog(stmtLog.getLogId(), 321L, 300L, 765);
        repositoryUpdate.updateLogWithResultSetLog(resultSetLog);

        repositoryRead.getStatements(new LogSearchCriteria(), resultSet -> {
            resultSet.next();
            assertEquals(1, resultSet.getLong(ID_COLUMN));
            assertEquals(stmtLog.getRawSql(), resultSet.getString(LogRepositoryConstants.RAW_SQL_COLUMN));
            assertEquals(stmtLog.isAutoCommit(), resultSet.getBoolean(LogRepositoryConstants.AUTOCOMMIT_COLUMN));
            assertEquals(stmtLog.getTransactionIsolation(), resultSet.getInt(TRANSACTION_ISOLATION_COLUMN));
            assertEquals(12, resultSet.getInt(LogRepositoryConstants.CONNECTION_NUMBER_COLUMN));
            assertEquals(resultSetLog.getResultSetUsageDurationNanos(),
                    resultSet.getInt(LogRepositoryConstants.RSET_USAGE_TIME));
            assertEquals(resultSetLog.getFetchDurationNanos(),
                    resultSet.getInt(LogRepositoryConstants.FETCH_TIME_COLUMN));
            assertEquals(statementExecutedLog.getExecutionTimeNanos(),
                    resultSet.getInt(LogRepositoryConstants.EXEC_TIME_COLUMN));
            assertEquals(
                    statementExecutedLog.getExecutionTimeNanos() + resultSetLog.getResultSetUsageDurationNanos(),
                    resultSet.getInt(LogRepositoryConstants.EXEC_PLUS_RSET_USAGE_TIME));
            assertEquals(resultSetLog.getNbRowsIterated(), resultSet.getInt(LogRepositoryConstants.NB_ROWS_COLUMN));
            assertEquals(stmtLog.getThreadName(), resultSet.getString(LogRepositoryConstants.THREAD_NAME_COLUMN));
            assertEquals(stmtLog.getTimeout(), resultSet.getInt(LogRepositoryConstants.TIMEOUT_COLUMN));
            assertEquals(stmtLog.getTimestamp(),
                    resultSet.getTimestamp(LogRepositoryConstants.TSTAMP_COLUMN).getTime());
            assertEquals(stmtLog.getStatementType().getId(),
                    resultSet.getInt(LogRepositoryConstants.STMT_TYPE_COLUMN));
            assertTrue(resultSet.getBoolean(LogRepositoryConstants.ERROR_COLUMN));
        }, false);

        final DetailedViewStatementLog readLog = repositoryRead.getStatementLog(1);
        assertEquals(resultSetLog.getLogId(), readLog.getLogId());
    }

    @Test
    public void testDeleteOldRowsIfTooMany() {
        final StatementLog log = insert1Log();

        assertEquals(1, countRowsInTable("statement_log"));
        // nothing to delete if not too many rows
        repositoryUpdate.deleteOldRowsIfTooMany();
        assertEquals(1, countRowsInTable("statement_log"));
        assertNull(repositoryUpdate.getLastLostMessageTime());

        repositoryUpdate.setLastLostMessageTime(System.currentTimeMillis() - 1);
        for (int i = 1; i < 2 * LogRepositoryUpdateJdbc.NB_ROWS_MAX; i++) {
            final StatementLog newLog = new StatementLog(log.getConnectionUuid(), randomUUID(),
                    System.currentTimeMillis(), StatementType.BASE_NON_PREPARED_STMT, "myrawsql" + i,
                    Thread.currentThread().getName(), i, i % 2 == 0, i % 4);
            repositoryUpdate.addStatementLog(newLog);
        }
        assertNotNull(repositoryUpdate.getLastLostMessageTime());
        assertEquals(2 * LogRepositoryUpdateJdbc.NB_ROWS_MAX, countRowsInTable("statement_log"));
        repositoryUpdate.deleteOldRowsIfTooMany();
        assertTrue(countRowsInTable("statement_log") <= LogRepositoryUpdateJdbc.NB_ROWS_MAX);
        assertNull(repositoryUpdate.getLastLostMessageTime());
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
                StatementType.BASE_NON_PREPARED_STMT, "myrawsql2", Thread.currentThread().getName(), 2, false, 1);
        repositoryUpdate.addStatementLog(log2);

        final StatementLog log3 = new StatementLog(log1.getConnectionUuid(), randomUUID(), System.currentTimeMillis(),
                StatementType.BASE_NON_PREPARED_STMT, "myrawsql3", Thread.currentThread().getName(), 3, false, 1);
        repositoryUpdate.addStatementLog(log3);

        assertEquals(3, countRowsInTable("statement_log"));
        repositoryUpdate.deleteStatementLog(1, 3);
        assertEquals(1, countRowsInTable("statement_log"));
    }

    @Test
    public void testaddBatchedNonPreparedStatementsLog() {
        final StatementLog log = insert1Log();

        final List<String> sqlList = Arrays.asList("st1", "st2", "st3");
        final BatchedNonPreparedStatementsLog batchedLogs = new BatchedNonPreparedStatementsLog(log.getConnectionUuid(),
                randomUUID(), System.currentTimeMillis(), sqlList, "myThread", 13, true, 1);
        repositoryUpdate.addBatchedNonPreparedStatementsLog(batchedLogs);
        assertEquals(2, countRowsInTable("statement_log"));
        assertEquals(3, countRowsInTable("batched_statement_log"));
    }

    @Test
    public void testaddBatchedPreparedStatementsLog() {
        final StatementLog log = insert1Log();

        final List<String> sqlList = Arrays.asList("st1", "st2", "st3");
        final BatchedPreparedStatementsLog batchedLogs = new BatchedPreparedStatementsLog(log.getConnectionUuid(),
                randomUUID(), System.currentTimeMillis(), "myRaw stmt", sqlList, "myThread", 13, true, 1);
        repositoryUpdate.addBatchedPreparedStatementsLog(batchedLogs);
        assertEquals(2, countRowsInTable("statement_log"));
        assertEquals(3, countRowsInTable("batched_statement_log"));

        final StatementExecutedLog statementExecutedLog = new StatementExecutedLog(batchedLogs.getLogId(), 123, null,
                "myexception");
        repositoryUpdate.updateLogAfterExecution(statementExecutedLog);

        repositoryRead.getStatements(new LogSearchCriteria(), resultSet -> {
            resultSet.next();
            resultSet.next();
            assertEquals(2, resultSet.getLong(ID_COLUMN));
            assertEquals(batchedLogs.getRawSql(), resultSet.getString(LogRepositoryConstants.RAW_SQL_COLUMN));
            assertEquals(batchedLogs.isAutoCommit(),
                    resultSet.getBoolean(LogRepositoryConstants.AUTOCOMMIT_COLUMN));
            assertEquals(batchedLogs.getTransactionIsolation(),
                    resultSet.getInt(TRANSACTION_ISOLATION_COLUMN));
            assertEquals(0, resultSet.getInt(LogRepositoryConstants.FETCH_TIME_COLUMN));
            assertEquals(0, resultSet.getInt(LogRepositoryConstants.RSET_USAGE_TIME));
            assertEquals(statementExecutedLog.getExecutionTimeNanos(),
                    resultSet.getInt(LogRepositoryConstants.EXEC_TIME_COLUMN));
            assertEquals(statementExecutedLog.getExecutionTimeNanos(),
                    resultSet.getInt(LogRepositoryConstants.EXEC_PLUS_RSET_USAGE_TIME));
            assertEquals(0, resultSet.getInt(LogRepositoryConstants.NB_ROWS_COLUMN));
            assertEquals(batchedLogs.getThreadName(),
                    resultSet.getString(LogRepositoryConstants.THREAD_NAME_COLUMN));
            assertEquals(batchedLogs.getTimeout(), resultSet.getInt(LogRepositoryConstants.TIMEOUT_COLUMN));
            assertEquals(batchedLogs.getTimestamp(),
                    resultSet.getTimestamp(LogRepositoryConstants.TSTAMP_COLUMN).getTime());
            assertEquals(batchedLogs.getStatementType().getId(),
                    resultSet.getInt(LogRepositoryConstants.STMT_TYPE_COLUMN));
            assertTrue(resultSet.getBoolean(LogRepositoryConstants.ERROR_COLUMN));
        }, false);
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

    @Test
    public void testlastLostMessageTime() {
        Long l = repositoryUpdate.getLastLostMessageTime();
        assertNull(l);
        repositoryUpdate.setLastLostMessageTime(123L);
        assertEquals(123L, repositoryUpdate.getLastLostMessageTime().longValue());
        repositoryUpdate.setLastLostMessageTime(null);
        l = repositoryUpdate.getLastLostMessageTime();
        assertNull(l);
    }

    @Test
    public void testaddStatementFullyExecutedLog() {
        final Properties connProps = new Properties();
        connProps.setProperty("myprop", "myval");
        final ConnectionInfo connectionInfo = new ConnectionInfo(randomUUID(), 12, "jdbc:toto", new Date(), 12,
                connProps);
        repositoryUpdate.addConnection(connectionInfo);

        final List<StatementFullyExecutedLog> fullLogs = new ArrayList<>();
        {
            final StatementLog log = new StatementLog(connectionInfo.getUuid(), randomUUID(),
                    System.currentTimeMillis(), StatementType.BASE_NON_PREPARED_STMT, "myrawsql",
                    Thread.currentThread().getName(), 123, true, 1);
            final StatementExecutedLog statementExecutedLog = new StatementExecutedLog(log.getLogId(), 234L, 456L,
                    "myexception");
            fullLogs.add(new StatementFullyExecutedLog(log, statementExecutedLog, null));
        }
        {
            final StatementLog log = new StatementLog(connectionInfo.getUuid(), randomUUID(),
                    System.currentTimeMillis(), StatementType.BASE_NON_PREPARED_STMT, "myrawsql",
                    Thread.currentThread().getName(), 123, true, 1);
            final StatementExecutedLog statementExecutedLog = new StatementExecutedLog(log.getLogId(), 234L, 456L,
                    "myexception");
            final ResultSetLog resultSetLog = new ResultSetLog(log.getLogId(), 789L, 700L, 21);
            fullLogs.add(new StatementFullyExecutedLog(log, statementExecutedLog, resultSetLog));
        }

        repositoryUpdate.addStatementFullyExecutedLog(fullLogs);
        assertEquals(2, countRowsInTable("statement_log"));

        repositoryRead.getStatements(new LogSearchCriteria(), resultSet -> {
            {
                resultSet.next();
                final StatementFullyExecutedLog stmtLog = fullLogs.get(0);

                assertEquals(1, resultSet.getLong(ID_COLUMN));
                assertEquals(stmtLog.getRawSql(), resultSet.getString(LogRepositoryConstants.RAW_SQL_COLUMN));
                assertEquals(stmtLog.isAutoCommit(),
                        resultSet.getBoolean(LogRepositoryConstants.AUTOCOMMIT_COLUMN));
                assertEquals(stmtLog.getTransactionIsolation(),
                        resultSet.getInt(TRANSACTION_ISOLATION_COLUMN));
                assertEquals(12, resultSet.getInt(LogRepositoryConstants.CONNECTION_NUMBER_COLUMN));

                resultSet.getLong(LogRepositoryConstants.RSET_USAGE_TIME);
                assertTrue(resultSet.wasNull());

                resultSet.getLong(LogRepositoryConstants.FETCH_TIME_COLUMN);
                assertTrue(resultSet.wasNull());

                assertEquals(stmtLog.getExecutionTimeNanos(),
                        resultSet.getLong(LogRepositoryConstants.EXEC_TIME_COLUMN));
                assertEquals(stmtLog.getExecutionTimeNanos(),
                        resultSet.getLong(LogRepositoryConstants.EXEC_PLUS_RSET_USAGE_TIME));
                resultSet.getInt(LogRepositoryConstants.NB_ROWS_COLUMN);
                assertTrue(resultSet.wasNull());
                assertEquals(stmtLog.getThreadName(),
                        resultSet.getString(LogRepositoryConstants.THREAD_NAME_COLUMN));
                assertEquals(stmtLog.getTimeout(), resultSet.getInt(LogRepositoryConstants.TIMEOUT_COLUMN));
                assertEquals(stmtLog.getTimestamp(),
                        resultSet.getTimestamp(LogRepositoryConstants.TSTAMP_COLUMN).getTime());
                assertEquals(stmtLog.getStatementType().getId(),
                        resultSet.getInt(LogRepositoryConstants.STMT_TYPE_COLUMN));
                assertTrue(resultSet.getBoolean(LogRepositoryConstants.ERROR_COLUMN));
            }
            {
                resultSet.next();
                final StatementFullyExecutedLog stmtLog = fullLogs.get(1);

                assertEquals(2, resultSet.getLong(ID_COLUMN));
                assertEquals(stmtLog.getRawSql(), resultSet.getString(LogRepositoryConstants.RAW_SQL_COLUMN));
                assertEquals(stmtLog.isAutoCommit(),
                        resultSet.getBoolean(LogRepositoryConstants.AUTOCOMMIT_COLUMN));
                assertEquals(stmtLog.getTransactionIsolation(),
                        resultSet.getInt(TRANSACTION_ISOLATION_COLUMN));
                assertEquals(12, resultSet.getInt(LogRepositoryConstants.CONNECTION_NUMBER_COLUMN));
                assertEquals(stmtLog.getResultSetUsageDurationNanos().longValue(),
                        resultSet.getLong(LogRepositoryConstants.RSET_USAGE_TIME));
                assertEquals(stmtLog.getFetchDurationNanos().longValue(),
                        resultSet.getLong(LogRepositoryConstants.FETCH_TIME_COLUMN));
                assertEquals(stmtLog.getExecutionTimeNanos(),
                        resultSet.getLong(LogRepositoryConstants.EXEC_TIME_COLUMN));
                assertEquals(stmtLog.getExecutionTimeNanos() + stmtLog.getResultSetUsageDurationNanos(),
                        resultSet.getLong(LogRepositoryConstants.EXEC_PLUS_RSET_USAGE_TIME));
                assertEquals(stmtLog.getNbRowsIterated().intValue(),
                        resultSet.getInt(LogRepositoryConstants.NB_ROWS_COLUMN));
                assertEquals(stmtLog.getThreadName(),
                        resultSet.getString(LogRepositoryConstants.THREAD_NAME_COLUMN));
                assertEquals(stmtLog.getTimeout(), resultSet.getInt(LogRepositoryConstants.TIMEOUT_COLUMN));
                assertEquals(stmtLog.getTimestamp(),
                        resultSet.getTimestamp(LogRepositoryConstants.TSTAMP_COLUMN).getTime());
                assertEquals(stmtLog.getStatementType().getId(),
                        resultSet.getInt(LogRepositoryConstants.STMT_TYPE_COLUMN));
                assertTrue(resultSet.getBoolean(LogRepositoryConstants.ERROR_COLUMN));
            }
        }, false);
    }

    @Test
    public void testgetLastModificationTime() throws Exception {
        final long beforeInsert = System.currentTimeMillis();
        Thread.sleep(5L);
        insert1Log();
        final long t2 = repositoryUpdate.getLastModificationTime();
        assertTrue(t2 > beforeInsert);
    }
}
