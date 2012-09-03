package slaurent.jdbcperflogger.model;

import java.util.HashMap;

import slaurent.jdbcperflogger.SqlTypedValue;

/**
 * Map from parameter index to value.
 * 
 * @author slaurent
 * 
 */
public class PreparedStatementValuesHolder extends HashMap<Integer, SqlTypedValue> {
    private static final long serialVersionUID = 1L;

    public PreparedStatementValuesHolder() {
        super(10);
    }

    public PreparedStatementValuesHolder(final PreparedStatementValuesHolder original) {
        super(original);
    }

    public PreparedStatementValuesHolder copy() {
        return new PreparedStatementValuesHolder(this);
    }
}
