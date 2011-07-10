package slaurent.jdbcperflogger;

public class SqlTypedValue {
	final int sqlType;
	final Object value;

	SqlTypedValue(final Object value, final int sqlType) {
		this.value = value;
		this.sqlType = sqlType;
	}

}
