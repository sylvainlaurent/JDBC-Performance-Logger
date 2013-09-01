package slaurent.jdbcperflogger.model;

import java.util.UUID;

import slaurent.jdbcperflogger.StatementType;

public class StatementLog extends AbstractLogMessage {

	private static final long serialVersionUID = 1L;

	private final String rawSql;
	private final String filledSql;
	private final boolean preparedStatement;

	public StatementLog(final int connectionId, final UUID logId,
			final long timestamp, final long executionTimeNanos,
			final StatementType statementType, final String sql,
			final String threadName, final Throwable sqlException) {
		super(connectionId, logId, timestamp, executionTimeNanos,
				statementType, threadName, sqlException);
		rawSql = sql;
		filledSql = sql;
		preparedStatement = false;
	}

	public StatementLog(final int connectionId, final UUID logId,
			final long timestamp, final long executionTimeNanos,
			final StatementType statementType, final String rawSql,
			final String filledSql, final String threadName,
			final Throwable sqlException) {
		super(connectionId, logId, timestamp, executionTimeNanos,
				statementType, threadName, sqlException);
		this.rawSql = rawSql;
		this.filledSql = filledSql;
		preparedStatement = true;
	}

	public String getRawSql() {
		return rawSql;
	}

	public String getFilledSql() {
		return filledSql;
	}

	public boolean isPreparedStatement() {
		return preparedStatement;
	}

}
