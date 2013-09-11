package ch.sla.jdbcperflogger.logger;

import java.sql.Types;
import java.text.SimpleDateFormat;

import org.junit.Assert;
import org.junit.Test;

import ch.sla.jdbcperflogger.driver.DatabaseType;
import ch.sla.jdbcperflogger.logger.PerfLogger;
import ch.sla.jdbcperflogger.model.PreparedStatementValuesHolder;
import ch.sla.jdbcperflogger.model.SqlTypedValue;

public class PerfLoggerTest {
	@Test
	public void testfillParameters() throws Exception {
		final PreparedStatementValuesHolder valHolder = new PreparedStatementValuesHolder();
		String filledSql;

		valHolder.put(1, new SqlTypedValue("toto$", Types.VARCHAR));
		filledSql = PerfLogger.fillParameters(
				"select * from toto where name = ?", valHolder,
				DatabaseType.ORACLE);
		Assert.assertEquals(
				"select * from toto where name = 'toto$' /*VARCHAR*/",
				filledSql);

		valHolder.put(2, new SqlTypedValue(36, Types.INTEGER));
		filledSql = PerfLogger.fillParameters(
				"select * from toto where name = ? and age < ?", valHolder,
				DatabaseType.ORACLE);
		Assert.assertEquals(
				"select * from toto where name = 'toto$' /*VARCHAR*/ and age < 36 /*INTEGER*/",
				filledSql);

		valHolder.put(1, new SqlTypedValue(null, Types.VARCHAR));
		filledSql = PerfLogger.fillParameters(
				"select * from toto where name = ?", valHolder,
				DatabaseType.ORACLE);
		Assert.assertEquals("select * from toto where name = NULL /*VARCHAR*/",
				filledSql);

		valHolder.put(1, new SqlTypedValue(null, Types.DATE));
		filledSql = PerfLogger.fillParameters(
				"select * from toto where param = ?", valHolder,
				DatabaseType.ORACLE);
		Assert.assertEquals("select * from toto where param = NULL /*DATE*/",
				filledSql);

		valHolder.put(
				1,
				new SqlTypedValue(new SimpleDateFormat("yyyy-MM-dd")
						.parse("2011-07-15"), Types.DATE));
		filledSql = PerfLogger.fillParameters(
				"select * from toto where param = ?", valHolder,
				DatabaseType.ORACLE);
		Assert.assertEquals(
				"select * from toto where param = date'2011-07-15' /*DATE*/",
				filledSql);

		valHolder.put(1, new SqlTypedValue(new Object(), Types.CLOB));
		filledSql = PerfLogger.fillParameters(
				"select * from toto where param = ?", valHolder,
				DatabaseType.ORACLE);
		Assert.assertEquals("select * from toto where param = ? /*CLOB*/",
				filledSql);
	}

	@Test
	public void testgetValueAsString_string() {
		final String val = PerfLogger.getValueAsString(new SqlTypedValue(
				"toto", Types.VARCHAR), DatabaseType.ORACLE);
		Assert.assertEquals("'toto' /*VARCHAR*/", val);
	}

}
