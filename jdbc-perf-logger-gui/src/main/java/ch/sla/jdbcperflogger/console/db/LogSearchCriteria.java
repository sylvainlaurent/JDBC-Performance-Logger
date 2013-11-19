package ch.sla.jdbcperflogger.console.db;

import ch.sla.jdbcperflogger.annotations.NonNullByDefault;

@NonNullByDefault(false)
public class LogSearchCriteria {
    private String filter;
    private Long minDurationNanos;
    private boolean removeTransactionCompletions;

    public String getFilter() {
        return filter;
    }

    public void setFilter(final String filter) {
        this.filter = filter;
    }

    public Long getMinDurationNanos() {
        return minDurationNanos;
    }

    public void setMinDurationNanos(final Long minDurationNanos) {
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
}
