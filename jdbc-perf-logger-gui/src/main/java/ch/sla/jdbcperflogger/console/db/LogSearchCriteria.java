package ch.sla.jdbcperflogger.console.db;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;

@NonNullByDefault({})
public class LogSearchCriteria {
    private String filter;
    private Long minDurationNanos;
    private boolean removeTransactionCompletions;
    private String sqlPassThroughFilter;

    public String getFilter() {
        return filter;
    }

    public void setFilter(final @Nullable String filter) {
        this.filter = filter;
    }

    public Long getMinDurationNanos() {
        return minDurationNanos;
    }

    public void setMinDurationNanos(final @Nullable Long minDurationNanos) {
        this.minDurationNanos = minDurationNanos;
    }

    public boolean isRemoveTransactionCompletions() {
        return removeTransactionCompletions;
    }

    public void setRemoveTransactionCompletions(final boolean removeTransactionCompletions) {
        this.removeTransactionCompletions = removeTransactionCompletions;
    }

    public boolean atLeastOneFilterApplied() {
        return (filter != null && !filter.isEmpty()) || minDurationNanos != null || removeTransactionCompletions;
    }

    public String getSqlPassThroughFilter() {
        return sqlPassThroughFilter;
    }

    public void setSqlPassThroughFilter(final @Nullable String sqlPassThroughFilter) {
        this.sqlPassThroughFilter = sqlPassThroughFilter;
    }

}
