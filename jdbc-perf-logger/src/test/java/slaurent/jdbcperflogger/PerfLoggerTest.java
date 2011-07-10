package slaurent.jdbcperflogger;

import java.sql.Types;

import org.junit.Assert;
import org.junit.Test;

public class PerfLoggerTest {
    @Test
    public void testfillParameters() {
        final PreparedStatementValuesHolder valHolder = new PreparedStatementValuesHolder();
        valHolder.put(1, new SqlTypedValue("toto", Types.VARCHAR));

        String filledSql;
        filledSql = PerfLogger.fillParameters("select * from toto where name = ?", valHolder, DatabaseType.ORACLE);
        Assert.assertEquals("select * from toto where name = 'toto'", filledSql);

        valHolder.put(2, new SqlTypedValue(36, Types.INTEGER));
        filledSql = PerfLogger.fillParameters("select * from toto where name = ? and age < ?", valHolder,
                DatabaseType.ORACLE);
        Assert.assertEquals("select * from toto where name = 'toto' and age < 36", filledSql);
    }

    @Test
    public void testgetValueAsString_string() {
        final String val = PerfLogger.getValueAsString(new SqlTypedValue("toto", Types.VARCHAR), DatabaseType.ORACLE);
        Assert.assertEquals("'toto'", val);
    }

}
