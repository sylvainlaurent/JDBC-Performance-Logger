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
package ch.sla.jdbcperflogger.driver;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Savepoint;
import java.sql.Statement;
import java.sql.Time;
import java.sql.Timestamp;
import java.sql.Types;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import ch.sla.jdbcperflogger.StatementType;
import ch.sla.jdbcperflogger.logger.PerfLoggerRemoting;
import ch.sla.jdbcperflogger.logger.RecordingLogSender;
import ch.sla.jdbcperflogger.model.BatchedNonPreparedStatementsLog;
import ch.sla.jdbcperflogger.model.BatchedPreparedStatementsLog;
import ch.sla.jdbcperflogger.model.ResultSetLog;
import ch.sla.jdbcperflogger.model.StatementExecutedLog;
import ch.sla.jdbcperflogger.model.StatementLog;

@SuppressWarnings("null")
public class WrappingDriverTest {
    private final static SimpleDateFormat YMD_FORMAT = new SimpleDateFormat("yyyy-MM-dd");
    private final static SimpleDateFormat DATE_PLUS_TIME_FORMAT = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS");

    private Connection connection;
    private RecordingLogSender logRecorder;

    @Before
    public void setup() throws Exception {
        // connection = DriverManager.getConnection("jdbcperflogger:jdbc:h2:mem:", "sa", "");
        connection = DriverManager.getConnection("jdbcperflogger:jdbc:hsqldb:mem:mydb;shutdown=true", "sa", "");
        logRecorder = new RecordingLogSender();
        PerfLoggerRemoting.addSender(logRecorder);
    }

    @After
    public void tearDown() throws Exception {
        PerfLoggerRemoting.removeSender(logRecorder);
        connection.close();
    }

    @Test
    public void testSetupDriver() throws Exception {
        Assert.assertNotNull(connection);
    }

    @Test(expected = SQLException.class)
    public void testUnknownDriver() throws Exception {
        DriverManager.getConnection("jdbcperflogger:jdbc:nonexistingdriver");
    }

    @Test
    public void testSelectNonPrepared() throws Exception {
        final Statement statement = connection.createStatement();
        executeStatementAndCheckLogged(statement, "create table test (key_id int);");
        // Thread.sleep(5000);
        executeQueryAndCheckLogged(statement, "select * from test;");
        executeQueryAndCheckLogged(statement, "select * from test;");
        executeQueryAndCheckLogged(statement, "select * from test;");
        statement.close();
    }

    @Test
    public void testTimeoutSelectNonPrepared() throws Exception {
        final Statement statement = connection.createStatement();
        statement.setQueryTimeout(123);
        statement.execute("create table test (key_id int);");
        assertEquals(123, ((StatementLog) logRecorder.lastLogMessage(1)).getTimeout());
        statement.close();
    }

    @Test
    public void testAutocommit() throws Exception {
        final Statement statement = connection.createStatement();
        statement.execute("create table test (key_id int);");
        StatementLog statementLog = ((StatementLog) logRecorder.lastLogMessage(1));
        assertTrue(statementLog.isAutoCommit());

        connection.setAutoCommit(false);
        statement.execute("create table test2 (key_id int);");
        statementLog = ((StatementLog) logRecorder.lastLogMessage(1));
        assertFalse(statementLog.isAutoCommit());

        statement.close();
    }

    @Test
    public void testExecuteNonPrepared() throws Exception {
        {
            final String sql = "create table test (key_id int);";
            final Statement statement = connection.createStatement();
            assertEquals(0, logRecorder.getRecordedLogMessages().length);
            statement.execute(sql);
            assertEquals(((StatementExecutedLog) logRecorder.lastLogMessage(0)).getLogId(),
                    ((StatementLog) logRecorder.lastLogMessage(1)).getLogId());
            assertEquals(sql, ((StatementLog) logRecorder.lastLogMessage(1)).getRawSql());
            statement.close();
        }
        {
            final String sql = "insert into test (key_id) values (123)";
            final Statement statement = connection.createStatement();
            final int nb = statement.executeUpdate(sql);
            Assert.assertEquals(1, nb);
            assertEquals(((StatementExecutedLog) logRecorder.lastLogMessage(0)).getLogId(),
                    ((StatementLog) logRecorder.lastLogMessage(1)).getLogId());
            assertEquals(sql, ((StatementLog) logRecorder.lastLogMessage(1)).getRawSql());
            assertEquals(StatementType.BASE_NON_PREPARED_STMT,
                    ((StatementLog) logRecorder.lastLogMessage(1)).getStatementType());
            assertEquals(1L, ((StatementExecutedLog) logRecorder.lastLogMessage(0)).getUpdateCount().longValue());
            statement.close();
        }
        {
            final String sql = "update test set key_id=453 where key_id=876";
            final Statement statement = connection.createStatement();
            final int nb = statement.executeUpdate(sql);
            Assert.assertEquals(0, nb);
            assertEquals(0L, ((StatementExecutedLog) logRecorder.lastLogMessage(0)).getUpdateCount().longValue());
            statement.close();
        }
    }

    @Test
    public void testExecutePrepared() throws Exception {
        {
            final String sql = "create table test (key_id int);";
            final PreparedStatement statement = connection.prepareStatement(sql);
            assertEquals(null, logRecorder.lastLogMessage(0));
            statement.execute();
            assertEquals(((StatementExecutedLog) logRecorder.lastLogMessage(0)).getLogId(),
                    ((StatementLog) logRecorder.lastLogMessage(1)).getLogId());
            assertEquals(sql, ((StatementLog) logRecorder.lastLogMessage(1)).getRawSql());
            statement.close();
        }
        {
            final String sql = "insert into test (key_id) values (?)";
            final PreparedStatement statement = connection.prepareStatement(sql);
            statement.setInt(1, 123);
            final int nb = statement.executeUpdate();
            Assert.assertEquals(1, nb);
            assertEquals(((StatementExecutedLog) logRecorder.lastLogMessage(0)).getLogId(),
                    ((StatementLog) logRecorder.lastLogMessage(1)).getLogId());
            assertEquals(sql, ((StatementLog) logRecorder.lastLogMessage(1)).getRawSql());
            assertEquals(StatementType.BASE_PREPARED_STMT,
                    ((StatementLog) logRecorder.lastLogMessage(1)).getStatementType());
            assertEquals("insert into test (key_id) values (123 /*setInt*/)",
                    ((StatementLog) logRecorder.lastLogMessage(1)).getFilledSql());
            assertEquals(1L, ((StatementExecutedLog) logRecorder.lastLogMessage(0)).getUpdateCount().longValue());
            statement.close();
        }
        {
            final String sql = "update test set key_id=453 where key_id=?";
            final PreparedStatement statement = connection.prepareStatement(sql);
            statement.setInt(1, 87687);
            final int nb = statement.executeUpdate();
            Assert.assertEquals(0, nb);
            assertEquals(0L, ((StatementExecutedLog) logRecorder.lastLogMessage(0)).getUpdateCount().longValue());
            statement.close();
        }
    }

    @Test
    public void testSelectPrepared() throws Exception {
        {
            final Statement statement = connection.createStatement();
            statement.execute(
                    "create table test (key_id int, myDate date, myTimestamp timestamp(0), myTime time, myBoolean boolean, myString varchar(128));");
            statement.close();
        }
        {
            final PreparedStatement statement = connection.prepareStatement("select * from test where key_id=?");
            statement.setInt(1, 1);
            statement.executeQuery().close();

            assertEquals(((StatementExecutedLog) logRecorder.lastLogMessage(1)).getLogId(),
                    ((StatementLog) logRecorder.lastLogMessage(2)).getLogId());
            assertEquals(((ResultSetLog) logRecorder.lastLogMessage(0)).getLogId(),
                    ((StatementLog) logRecorder.lastLogMessage(2)).getLogId());

            assertEquals("select * from test where key_id=1 /*setInt*/",
                    ((StatementLog) logRecorder.lastLogMessage(2)).getFilledSql());

            statement.setInt(1, 2);
            statement.executeQuery().close();
            assertEquals("select * from test where key_id=2 /*setInt*/",
                    ((StatementLog) logRecorder.lastLogMessage(2)).getFilledSql());

            statement.setByte(1, (byte) 112);
            statement.executeQuery().close();
            assertEquals("select * from test where key_id=112 /*setByte*/",
                    ((StatementLog) logRecorder.lastLogMessage(2)).getFilledSql());

            statement.setLong(1, 123);
            statement.executeQuery().close();
            assertEquals("select * from test where key_id=123 /*setLong*/",
                    ((StatementLog) logRecorder.lastLogMessage(2)).getFilledSql());

            statement.setLong(1, 123);
            statement.executeQuery().close();
            assertEquals("select * from test where key_id=123 /*setLong*/",
                    ((StatementLog) logRecorder.lastLogMessage(2)).getFilledSql());

            {
                final ResultSet query = statement.executeQuery();
                Thread.sleep(55);
                query.close();
                final ResultSetLog resultSetLog = (ResultSetLog) logRecorder.lastLogMessage(0);
                assertEquals(0, resultSetLog.getFetchDurationNanos());
                assertTrue(resultSetLog.getResultSetUsageDurationNanos() >= 55);
            }

            statement.close();
            // check that calling close() twice is ok
            statement.close();
        }
        {
            final PreparedStatement statement = connection.prepareStatement("select * from test where myDate=?");
            statement.setDate(1, sqlDate("2013-02-28"));
            statement.executeQuery().close();
            assertEquals("select * from test where myDate=date'2013-02-28' /*setDate*/",
                    ((StatementLog) logRecorder.lastLogMessage(2)).getFilledSql());

            final Date utilDateWithTime = utilDateWithTime("2013-02-28T13:45:56.123");
            statement.setDate(1, new java.sql.Date(utilDateWithTime.getTime()));
            statement.executeQuery().close();
            assertEquals(
                    "select * from test where myDate=cast(timestamp'2013-02-28 13:45:56.123' as DATE) /*setDate (non pure)*/",
                    ((StatementLog) logRecorder.lastLogMessage(2)).getFilledSql());

            statement.setObject(1, sqlDate("2013-02-28"));
            statement.executeQuery().close();
            assertEquals("select * from test where myDate=date'2013-02-28' /*setObject*/",
                    ((StatementLog) logRecorder.lastLogMessage(2)).getFilledSql());
            statement.setObject(1, utilDate("2013-02-28"));
            statement.executeQuery().close();
            assertEquals("select * from test where myDate=? /*setObject*/",
                    ((StatementLog) logRecorder.lastLogMessage(2)).getFilledSql());
            statement.setObject(1, utilDate("2013-02-28"), Types.DATE);
            statement.executeQuery().close();
            assertEquals("select * from test where myDate=date'2013-02-28' /*DATE*/",
                    ((StatementLog) logRecorder.lastLogMessage(2)).getFilledSql());
            statement.close();
        }
        {
            final PreparedStatement statement = connection.prepareStatement("select * from test where myTimestamp=?");
            statement.setTimestamp(1, sqlTimestamp("2013-02-28 15:23:43.123"));
            statement.executeQuery().close();
            assertEquals("select * from test where myTimestamp=timestamp'2013-02-28 15:23:43.123' /*setTimestamp*/",
                    ((StatementLog) logRecorder.lastLogMessage(2)).getFilledSql());
        }
        {
            final PreparedStatement statement = connection.prepareStatement("select * from test where myTime=?");

            statement.setTime(1, sqlTime("15:23:43"));
            statement.executeQuery().close();
            assertEquals("select * from test where myTime=time'15:23:43' /*setTime*/",
                    ((StatementLog) logRecorder.lastLogMessage(2)).getFilledSql());
        }
        {
            final PreparedStatement statement = connection.prepareStatement("select * from test where myDate=?");
            statement.setDate(1, java.sql.Date.valueOf("2011-01-02"));
            statement.executeQuery().close();
            assertEquals("select * from test where myDate=date'2011-01-02' /*setDate*/",
                    ((StatementLog) logRecorder.lastLogMessage(2)).getFilledSql());

            statement.close();
        }
        {
            final PreparedStatement statement = connection.prepareStatement("select * from test where myBoolean=?");
            statement.setBoolean(1, true);
            statement.executeQuery().close();
            assertEquals("select * from test where myBoolean=true /*setBoolean*/",
                    ((StatementLog) logRecorder.lastLogMessage(2)).getFilledSql());
            statement.close();
        }
        {
            final PreparedStatement statement = connection.prepareStatement("select * from test where myString=?");
            statement.setString(1, "hel'lo");
            statement.executeQuery().close();
            assertEquals("select * from test where myString='hel''lo' /*setString*/",
                    ((StatementLog) logRecorder.lastLogMessage(2)).getFilledSql());
            statement.close();
        }
    }

    @Test(expected = SQLException.class)
    public void testClearParameters() throws Exception {
        {
            final Statement statement = connection.createStatement();
            statement.execute("create table test (key_id int)");
            statement.close();
        }
        {
            final PreparedStatement statement = connection.prepareStatement("select * from test where key_id=?");
            statement.setInt(1, 1);
            statement.executeQuery().close();
            statement.executeQuery().close();
            statement.clearParameters();
            statement.executeQuery().close();
            // should fail if no exception is raised
            Assert.fail("last executeQuery() should have failed because not all param values bound");
        }
    }

    @Test
    public void testBatchedNonPrepared() throws Exception {
        {
            final Statement statement = connection.createStatement();
            statement.execute("create table test (key_id int);");
            statement.close();
        }
        // TimeUnit.SECONDS.sleep(10);
        final Statement statement = connection.createStatement();
        {
            for (int i = 0; i < 100; i++) {
                statement.addBatch("insert into test (key_id) values (" + i + ")");
            }
            final int[] nbRowsBatch = statement.executeBatch();
            assertEquals(100, nbRowsBatch.length);
            for (int i = 0; i < 100; i++) {
                assertEquals(1, nbRowsBatch[i]);
            }
            assertEquals(((StatementExecutedLog) logRecorder.lastLogMessage(0)).getLogId(),
                    ((BatchedNonPreparedStatementsLog) logRecorder.lastLogMessage(1)).getLogId());
            final List<String> sqlList = ((BatchedNonPreparedStatementsLog) logRecorder.lastLogMessage(1)).getSqlList();
            assertEquals(100, sqlList.size());
            assertEquals("insert into test (key_id) values (0)", sqlList.get(0));
            assertEquals("insert into test (key_id) values (99)", sqlList.get(99));
            assertEquals(100L, ((StatementExecutedLog) logRecorder.lastLogMessage(0)).getUpdateCount().longValue());
        }
        {
            logRecorder.clearLogs();
            // calling executeBatch() again without adding a batch
            statement.executeBatch();
            final List<String> sqlList = ((BatchedNonPreparedStatementsLog) logRecorder.lastLogMessage(1)).getSqlList();
            assertEquals(0, sqlList.size());
        }
        {
            // check that clearBatch() does clear the logged batched statement
            logRecorder.clearLogs();
            for (int i = 0; i < 10; i++) {
                statement.addBatch("insert into test (key_id) values (" + i + ")");
            }
            statement.clearBatch();
            statement.executeBatch();
            final List<String> sqlList = ((BatchedNonPreparedStatementsLog) logRecorder.lastLogMessage(1)).getSqlList();
            assertEquals(0, sqlList.size());
        }
        statement.close();
        // TimeUnit.SECONDS.sleep(10);
    }

    @Test
    public void testBatchedPrepared() throws Exception {
        {
            final Statement statement = connection.createStatement();
            statement.execute("create table test (key_id int);");
            statement.close();
        }
        // TimeUnit.SECONDS.sleep(10);
        final PreparedStatement statement = connection.prepareStatement("insert into test (key_id) values (?)");

        {
            for (int i = 0; i < 100; i++) {
                statement.setInt(1, i);
                statement.addBatch();
            }
            statement.executeBatch();

            assertEquals(((StatementExecutedLog) logRecorder.lastLogMessage(0)).getLogId(),
                    ((BatchedPreparedStatementsLog) logRecorder.lastLogMessage(1)).getLogId());
            assertTrue(((StatementExecutedLog) logRecorder.lastLogMessage(0)).getExecutionTimeNanos() > 0);
            final List<String> sqlList = ((BatchedPreparedStatementsLog) logRecorder.lastLogMessage(1)).getSqlList();
            assertEquals(100, sqlList.size());
            assertEquals("insert into test (key_id) values (0 /*setInt*/)", sqlList.get(0));
            assertEquals("insert into test (key_id) values (99 /*setInt*/)", sqlList.get(99));
            assertEquals(100L, ((StatementExecutedLog) logRecorder.lastLogMessage(0)).getUpdateCount().longValue());
        }
        // {
        // statement.executeBatch();
        // final List<String> sqlList = ((BatchedPreparedStatementsLog) logRecorder.lastLogMessage(1)).getSqlList();
        // assertEquals(0, sqlList.size());
        // }
        {
            for (int i = 0; i < 10; i++) {
                statement.setInt(1, i);
                statement.addBatch();
            }
            statement.clearBatch();
            statement.executeBatch();
            final List<String> sqlList = ((BatchedPreparedStatementsLog) logRecorder.lastLogMessage(1)).getSqlList();
            assertEquals(0, sqlList.size());
        }
        statement.close();
        // TimeUnit.SECONDS.sleep(10);
    }

    @Test(expected = SQLException.class)
    public void testException() throws Exception {
        final Statement statement = connection.createStatement();
        try {
            statement.execute("create table test (key_id int);");

            statement.execute("create table test (key_id int);");
        } catch (final SQLException exc) {
            final StringWriter stringWriter = new StringWriter(500);
            exc.printStackTrace(new PrintWriter(stringWriter));

            assertEquals(stringWriter.toString(),
                    ((StatementExecutedLog) logRecorder.lastLogMessage(0)).getSqlException());
            assertEquals("create table test (key_id int);", ((StatementLog) logRecorder.lastLogMessage(1)).getRawSql());
            assertEquals("create table test (key_id int);",
                    ((StatementLog) logRecorder.lastLogMessage(1)).getFilledSql());
            throw exc;
        } finally {
            statement.close();
        }
    }

    @Test
    public void testCallable() throws Exception {
        {
            final PreparedStatement statement = connection.prepareCall("call 2*3");
            final ResultSet resultSet = statement.executeQuery();
            assertEquals("call 2*3", ((StatementLog) logRecorder.lastLogMessage(1)).getRawSql());
            assertEquals("call 2*3", ((StatementLog) logRecorder.lastLogMessage(1)).getFilledSql());
            assertEquals(StatementType.PREPARED_QUERY_STMT,
                    ((StatementLog) logRecorder.lastLogMessage(1)).getStatementType());

            assertTrue(resultSet.next());
            assertEquals(6, resultSet.getInt(1));
            resultSet.close();
            assertEquals(1, ((ResultSetLog) logRecorder.lastLogMessage(0)).getNbRowsIterated());
            statement.close();
        }
        {
            final CallableStatement statement = connection.prepareCall("{call 2*?}");
            // we should use set(name,value) instead, but it's not supported by
            // H2..
            statement.setInt(1, 7);
            final ResultSet resultSet = statement.executeQuery();
            assertEquals("{call 2*?}", ((StatementLog) logRecorder.lastLogMessage(1)).getRawSql());
            assertEquals("{call 2*7 /*setInt*/}", ((StatementLog) logRecorder.lastLogMessage(1)).getFilledSql());
            assertEquals(StatementType.PREPARED_QUERY_STMT,
                    ((StatementLog) logRecorder.lastLogMessage(1)).getStatementType());
            Assert.assertTrue(resultSet.next());
            Assert.assertEquals(14, resultSet.getInt(1));
            resultSet.close();
            statement.close();
        }
    }

    @Test
    public void testCommit() throws Exception {
        connection.setAutoCommit(false);
        connection.commit();
    }

    @Test
    public void testRollback() throws Exception {
        connection.setAutoCommit(false);
        connection.rollback();
    }

    @Test
    public void testSetSavepoint() throws Exception {
        connection.setAutoCommit(false);
        connection.setSavepoint();
    }

    @Test
    public void testRollbackToSavepoint() throws Exception {
        connection.setAutoCommit(false);
        final Savepoint savepoint = connection.setSavepoint();
        connection.rollback(savepoint);
    }

    @Test
    public void testManySelect() throws Exception {
        {
            final Statement statement = connection.createStatement();
            statement.execute("create table test (key_id int)");
            statement.execute("insert into test (key_id) values(300)");
            statement.close();
        }
        connection.setAutoCommit(false);
        final PreparedStatement statement = connection.prepareStatement("select * from test where key_id=?");
        for (int i = 0; i < 10000; i++) {
            statement.setInt(1, i);
            final ResultSet resultSet = statement.executeQuery();
            resultSet.next();
            // Thread.sleep(5);
            resultSet.close();
            // Thread.sleep(10);
        }
        Thread.sleep(5000);
    }

    private void executeStatementAndCheckLogged(final Statement statement, final String sql) throws SQLException {
        statement.execute(sql);
        final StatementLog statementLog = (StatementLog) logRecorder.lastLogMessage(1);
        final StatementExecutedLog statementExecutedLog = (StatementExecutedLog) logRecorder.lastLogMessage(0);
        Assert.assertEquals(sql, statementLog.getRawSql());
        Assert.assertEquals(statementLog.getLogId(), statementExecutedLog.getLogId());
    }

    private void executeQueryAndCheckLogged(final Statement statement, final String sql) throws SQLException {
        statement.executeQuery(sql);
        final StatementLog statementLog = (StatementLog) logRecorder.lastLogMessage(1);
        final StatementExecutedLog statementExecutedLog = (StatementExecutedLog) logRecorder.lastLogMessage(0);
        Assert.assertEquals(sql, statementLog.getRawSql());
        Assert.assertEquals(statementLog.getLogId(), statementExecutedLog.getLogId());
    }

    private static java.util.Date utilDate(final String dateString) throws ParseException {
        return YMD_FORMAT.parse(dateString);
    }

    private static java.util.Date utilDateWithTime(final String dateString) throws ParseException {
        return DATE_PLUS_TIME_FORMAT.parse(dateString);
    }

    private static java.sql.Date sqlDate(final String dateString) throws ParseException {
        return new java.sql.Date(utilDate(dateString).getTime());
    }

    private static java.sql.Timestamp sqlTimestamp(final String tstampString) throws ParseException {
        return Timestamp.valueOf(tstampString);
    }

    private static java.sql.Time sqlTime(final String timeString) throws ParseException {
        return Time.valueOf(timeString);
    }

}
