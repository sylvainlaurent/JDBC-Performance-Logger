package slaurent.jdbcperflogger.driver;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

public class WrappingDriverTest {
	private Connection connection;

	@BeforeClass
	public static void setupClass() throws Exception {
		Class.forName(org.h2.Driver.class.getName());
		Class.forName(WrappingDriver.class.getName());
	}

	@Before
	public void setup() throws Exception {
		connection = DriverManager.getConnection("jdbcperflogger:jdbc:h2:mem:",
				"sa", "");
	}

	@After
	public void tearDown() throws Exception {
		if (connection != null) {
			connection.close();
			connection = null;
		}
	}

	@Test
	public void testSetupDriver() throws Exception {
		Assert.assertNotNull(connection);
	}

	@Test
	public void testSelectNonPrepared() throws Exception {
		final Statement statement = connection.createStatement();
		statement.execute("create table test (key_id int);");
		Thread.sleep(5000);
		statement.executeQuery("select * from test;");
		statement.executeQuery("select * from test;");
		statement.executeQuery("select * from test;");
		statement.close();
	}

	@Test
	public void testExecutePrepared() throws Exception {
		{
			final PreparedStatement statement = connection
					.prepareStatement("create table test (key_id int);");
			statement.execute();
			statement.close();
		}
		{
			final PreparedStatement statement = connection
					.prepareStatement("insert into test (key_id) values (?)");
			statement.setInt(1, 123);
			final int nb = statement.executeUpdate();
			Assert.assertEquals(1, nb);
			statement.close();
		}
	}

	@Test
	public void testSelectPrepared() throws Exception {
		{
			final Statement statement = connection.createStatement();
			statement
					.execute("create table test (key_id int, myDate date, myTimestamp timestamp(0), myTime time, myBoolean boolean);");
			statement.close();
		}
		{
			final PreparedStatement statement = connection
					.prepareStatement("select * from test where key_id=?");
			statement.setInt(1, 1);
			statement.executeQuery().close();
			statement.setInt(1, 2);
			statement.executeQuery().close();
			statement.setByte(1, (byte) 112);
			statement.executeQuery().close();
			statement.setLong(1, 123);
			statement.executeQuery().close();
			statement.close();
			statement.close();
		}
		{
			final PreparedStatement statement = connection
					.prepareStatement("select * from test where myDate=?");
			statement.setDate(1, new java.sql.Date(System.currentTimeMillis()));
			statement.executeQuery().close();
			statement.setObject(1,
					new java.sql.Date(System.currentTimeMillis()));
			statement.executeQuery().close();
			statement.setObject(1, new java.util.Date());
			statement.executeQuery().close();
			statement.setObject(1, new java.util.Date(), Types.DATE);
			statement.executeQuery().close();
			statement.close();
		}
		{
			final PreparedStatement statement = connection
					.prepareStatement("select * from test where myTimestamp=?");
			statement.setTimestamp(1,
					new java.sql.Timestamp(System.currentTimeMillis()));
			statement.executeQuery().close();
			statement.setTime(1, new java.sql.Time(System.currentTimeMillis()));
			statement.executeQuery().close();
			statement.setDate(1, java.sql.Date.valueOf("2011-01-02"));
			statement.executeQuery().close();
			statement.close();
		}
		{
			final PreparedStatement statement = connection
					.prepareStatement("select * from test where myBoolean=?");
			statement.setBoolean(1, true);
			statement.executeQuery().close();
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
			final PreparedStatement statement = connection
					.prepareStatement("select * from test where key_id=?");
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
		for (int i = 0; i < 100; i++) {
			statement.addBatch("insert into test (key_id) values (" + i + ")");
		}
		statement.executeBatch();
		statement.executeBatch();
		for (int i = 0; i < 10; i++) {
			statement.addBatch("insert into test (key_id) values (" + i + ")");
		}
		statement.clearBatch();
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
		final PreparedStatement statement = connection
				.prepareStatement("insert into test (key_id) values (?)");
		for (int i = 0; i < 100; i++) {
			statement.setInt(1, i);
			statement.addBatch();
		}
		statement.executeBatch();
		statement.executeBatch();
		for (int i = 0; i < 10; i++) {
			statement.setInt(1, i);
			statement.addBatch();
		}
		statement.clearBatch();
		statement.close();
		// TimeUnit.SECONDS.sleep(10);
	}

	@Test
	// @Ignore
	public void testManySelect() throws Exception {
		{
			final Statement statement = connection.createStatement();
			statement.execute("create table test (key_id int)");
			statement.execute("insert into test (key_id) values(300)");
			statement.close();
		}
		final PreparedStatement statement = connection
				.prepareStatement("select * from test where key_id=?");
		for (int i = 0; i < 10000; i++) {
			statement.setInt(1, i);
			final ResultSet resultSet = statement.executeQuery();
			resultSet.next();
			// Thread.sleep(5);
			resultSet.close();
			// Thread.sleep(10);
		}
	}

	@Test(expected = SQLException.class)
	public void testException() throws Exception {
		final Statement statement = connection.createStatement();
		try {
			statement.execute("create table test (key_id int);");
			// TimeUnit.SECONDS.sleep(10);
			statement.execute("create table test (key_id int);");
		} finally {
			// TimeUnit.SECONDS.sleep(5);
			statement.close();
		}
	}

	@Test
	public void testCallable() throws Exception {
		{
			final PreparedStatement statement = connection
					.prepareCall("call 2*3");
			final ResultSet resultSet = statement.executeQuery();
			Assert.assertTrue(resultSet.next());
			Assert.assertEquals(6, resultSet.getInt(1));
			resultSet.close();
			statement.close();
		}
		{
			final CallableStatement statement = connection
					.prepareCall("{call 2*?}");
			// we should use set(name,value) instead, but it's not supported by
			// H2..
			statement.setInt(1, 7);
			final ResultSet resultSet = statement.executeQuery();
			Assert.assertTrue(resultSet.next());
			Assert.assertEquals(14, resultSet.getInt(1));
			resultSet.close();
			statement.close();
		}
	}
}
