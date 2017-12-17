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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import ch.sla.jdbcperflogger.StatementType;

public class BatchedPreparedStatementsLog extends AbstractBeforeStatementExecutionLog {

    private static final long serialVersionUID = 1L;

    private final String rawSql;
    private final List<String> sqlList;

    public BatchedPreparedStatementsLog(final UUID connectionId, final UUID logId, final long timestamp,
            final String rawSql, final List<String> sqlList, final String threadName, final int timeout,
            final boolean autoCommit, final int transactionIsolation) {
        super(connectionId, logId, timestamp, StatementType.PREPARED_BATCH_EXECUTION, threadName, timeout, autoCommit, transactionIsolation);
        this.rawSql = rawSql;
        this.sqlList = Collections.unmodifiableList(new ArrayList<String>(sqlList));
    }

    public String getRawSql() {
        return rawSql;
    }

    public List<String> getSqlList() {
        return sqlList;
    }

    @Override
    public String toString() {
        return "BatchedPreparedStatementsLog["//
                + "logId=" + getLogId()//
                + ", rawSql=" + rawSql//
                + ", timestamp=" + getTimestamp()//
                + ", statementType=" + getStatementType()//
                + ", threadName=" + getThreadName()//
                + ", timeout=" + getTimeout()//
                + ", autocommit=" + isAutoCommit()//
                + ", transactionIsolation=" + getTransactionIsolation()//
                + "]";
    }

}
