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

import ch.sla.jdbcperflogger.StatementType;

public class AbstractBeforeStatementExecutionLog implements LogMessage {

    private static final long serialVersionUID = 1L;

    private final UUID connectionUuid;
    private final UUID logId;
    private final long timestamp;
    private final StatementType statementType;
    private final String threadName;
    private final int timeout;
    private final boolean autoCommit;
    private final int transactionIsolation;

    public AbstractBeforeStatementExecutionLog(final UUID connectionId, final UUID logId, final long timestamp,
                                               final StatementType statementType, final String threadName, final int timeout, final boolean autoCommit, int transactionIsolation) {
        connectionUuid = connectionId;
        this.logId = logId;
        this.timestamp = timestamp;
        this.statementType = statementType;
        this.threadName = threadName;
        this.timeout = timeout;
        this.autoCommit = autoCommit;
        this.transactionIsolation = transactionIsolation;
    }

    public UUID getConnectionUuid() {
        return connectionUuid;
    }

    public UUID getLogId() {
        return logId;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public StatementType getStatementType() {
        return statementType;
    }

    public String getThreadName() {
        return threadName;
    }

    public int getTimeout() {
        return timeout;
    }

    public boolean isAutoCommit() {
        return autoCommit;
    }

    public int getTransactionIsolation() {
        return transactionIsolation;
    }
}