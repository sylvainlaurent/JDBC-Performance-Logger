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

public class ResultSetLog implements LogMessage {

    private static final long serialVersionUID = 1L;

    private final UUID logId;

    private final long resultSetUsageDurationNanos;
    private final long fetchDurationNanos;
    private final int nbRowsIterated;

    public ResultSetLog(final UUID logId, final long resultSetIterationTimeNanos, final long fetchDurationNanos,
            final int nbRowsIterated) {
        this.logId = logId;
        this.resultSetUsageDurationNanos = resultSetIterationTimeNanos;
        this.fetchDurationNanos = fetchDurationNanos;
        this.nbRowsIterated = nbRowsIterated;
    }

    public UUID getLogId() {
        return logId;
    }

    public long getResultSetUsageDurationNanos() {
        return resultSetUsageDurationNanos;
    }

    public long getFetchDurationNanos() {
        return fetchDurationNanos;
    }

    public int getNbRowsIterated() {
        return nbRowsIterated;
    }

    @Override
    public String toString() {
        return "ResultSetLog["//
                + "logId=" + logId//
                + ", resultSetUsageDurationNanos=" + resultSetUsageDurationNanos//
                + ", fetchDurationNanos=" + fetchDurationNanos//
                + ", nbRowsIterated=" + nbRowsIterated//
                + "]";
    }

}
