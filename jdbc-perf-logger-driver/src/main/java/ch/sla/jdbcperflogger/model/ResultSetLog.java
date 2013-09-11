package ch.sla.jdbcperflogger.model;

import java.util.UUID;

import ch.sla.jdbcperflogger.StatementType;

public class ResultSetLog extends AbstractLogMessage {

	private static final long serialVersionUID = 1L;

	private final int nbRowsIterated;

	public ResultSetLog(final UUID logId, final long timestamp,
			final long executionTimeNanos, final StatementType statementType,
			final String threadName, final int nbRowsIterated) {
		super(-1, logId, timestamp, executionTimeNanos, statementType,
				threadName, null);
		this.nbRowsIterated = nbRowsIterated;
	}

	public int getNbRowsIterated() {
		return nbRowsIterated;
	}

}
