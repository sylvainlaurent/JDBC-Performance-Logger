package slaurent.jdbcperflogger;

import java.util.HashMap;

/**
 * Map from parameter index to value.
 * 
 * @author slaurent
 * 
 */
public class PreparedStatementValuesHolder extends HashMap<Integer, SqlTypedValue> {
    private static final long serialVersionUID = 1L;

    PreparedStatementValuesHolder() {
        super(10);
    }

    PreparedStatementValuesHolder(final PreparedStatementValuesHolder original) {
        super(original);
    }

    PreparedStatementValuesHolder copy() {
        return new PreparedStatementValuesHolder(this);
    }
}
