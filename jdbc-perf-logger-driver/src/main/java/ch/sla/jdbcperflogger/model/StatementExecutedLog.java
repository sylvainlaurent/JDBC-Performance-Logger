/*
 *  Copyright 2013 Sylvain LAURENT
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package ch.sla.jdbcperflogger.model;

import java.util.UUID;

import org.eclipse.jdt.annotation.Nullable;

public class StatementExecutedLog implements LogMessage {

    private static final long serialVersionUID = 1L;

    private final UUID logId;
    private final long executionTimeNanos;
    @Nullable
    private final Long updateCount;
    @Nullable
    private final String sqlException;

    public StatementExecutedLog(final UUID logId, final long executionTimeNanos, @Nullable final Long updateCount,
            @Nullable final String sqlException) {
        this.logId = logId;
        this.executionTimeNanos = executionTimeNanos;
        this.updateCount = updateCount;
        this.sqlException = sqlException;
    }

    public UUID getLogId() {
        return logId;
    }

    public long getExecutionTimeNanos() {
        return executionTimeNanos;
    }

    @Nullable
    public Long getUpdateCount() {
        return updateCount;
    }

    @Nullable
    public String getSqlException() {
        return sqlException;
    }

    @Override
    public String toString() {
        return "StatementExecutedLog["//
                + "logId=" + logId//
                + ", executionTimeNanos=" + executionTimeNanos//
                + ", updateCount=" + updateCount//
                + "]";
    }

}
