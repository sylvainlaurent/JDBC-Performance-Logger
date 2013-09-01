package slaurent.jdbcperflogger.model;

import java.io.Serializable;
import java.util.HashMap;

/**
 * Map from parameter index or name to value.
 * 
 * @author slaurent
 * 
 */
public class PreparedStatementValuesHolder extends
		HashMap<Serializable, SqlTypedValue> {
	private static final long serialVersionUID = 1L;

	public PreparedStatementValuesHolder() {
		super(10);
	}

	public PreparedStatementValuesHolder(
			final PreparedStatementValuesHolder original) {
		super(original);
	}

	public PreparedStatementValuesHolder copy() {
		return new PreparedStatementValuesHolder(this);
	}
}
