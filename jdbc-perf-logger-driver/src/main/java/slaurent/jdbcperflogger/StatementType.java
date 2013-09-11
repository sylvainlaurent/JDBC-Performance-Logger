package slaurent.jdbcperflogger;

public enum StatementType {
	BASE_NON_PREPARED_STMT(1), BASE_PREPARED_STMT(2), //
	NON_PREPARED_QUERY_STMT(3), PREPARED_QUERY_STMT(4), //
	PREPARED_BATCH_EXECUTION(5), NON_PREPARED_BATCH_EXECUTION(6);

	private static StatementType[] vals;

	private final int id;

	private StatementType(final int id) {
		this.id = id;
		addToVals(id);
	}

	private void addToVals(final int id) {
		if (vals == null) {
			vals = new StatementType[7];
		}
		vals[id] = this;
	}

	public int getId() {
		return id;
	}

	public static StatementType fromId(final int id) {
		return id > 0 && id < vals.length ? vals[id] : null;
	}
}
