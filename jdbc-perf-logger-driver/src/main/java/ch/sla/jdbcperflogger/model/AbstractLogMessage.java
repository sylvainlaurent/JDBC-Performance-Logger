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

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

import ch.sla.jdbcperflogger.StatementType;

@ParametersAreNonnullByDefault
public class AbstractLogMessage implements LogMessage {

    private static final long serialVersionUID = 3766419115920080440L;
    private final int connectionId;
    private final UUID logId;
    private final long timestamp;
    private final long executionTimeNanos;
    private final StatementType statementType;
    private final String threadName;
    @Nullable
    private final Throwable sqlException;

    public AbstractLogMessage(final int connectionId, final UUID logId, final long timestamp,
            final long executionTimeNanos, final StatementType statementType, final String threadName,
            @Nullable final Throwable sqlException) {
        this.connectionId = connectionId;
        this.logId = logId;
        this.timestamp = timestamp;
        this.executionTimeNanos = executionTimeNanos;
        this.statementType = statementType;
        this.threadName = threadName;
        this.sqlException = sqlException;
    }

    public int getConnectionId() {
        return connectionId;
    }

    public UUID getLogId() {
        return logId;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public long getExecutionTimeNanos() {
        return executionTimeNanos;
    }

    public StatementType getStatementType() {
        return statementType;
    }

    public String getThreadName() {
        return threadName;
    }

    @Nullable
    public Throwable getSqlException() {
        return sqlException;
    }

}