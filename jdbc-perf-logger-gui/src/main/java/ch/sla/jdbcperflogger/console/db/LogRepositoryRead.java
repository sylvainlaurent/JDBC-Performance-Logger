package ch.sla.jdbcperflogger.console.db;

import java.util.UUID;

import org.eclipse.jdt.annotation.Nullable;

public interface LogRepositoryRead {

    void getStatements(LogSearchCriteria searchCriteria, ResultSetAnalyzer analyzer, boolean withFilledSql);

    void getStatementsGroupByRawSQL(LogSearchCriteria searchCriteria, ResultSetAnalyzer analyzer);

    void getStatementsGroupByFilledSQL(LogSearchCriteria searchCriteria, ResultSetAnalyzer analyzer);

    void getBatchStatementExecutions(UUID logId, ResultSetAnalyzer analyzer);

    @Nullable
    DetailedViewStatementLog getStatementLog(long id);

    int countStatements();

    long getTotalExecAndFetchTimeNanos();

    long getTotalExecAndFetchTimeNanos(LogSearchCriteria searchCriteria);

    void dispose();
}