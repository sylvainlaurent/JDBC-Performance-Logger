package ch.sla.jdbcperflogger;

public enum TxCompletionType {
    COMMIT, ROLLBACK, SET_SAVE_POINT, ROLLBACK_TO_SAVEPOINT
}