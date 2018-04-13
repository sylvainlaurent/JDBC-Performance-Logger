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
package ch.sla.jdbcperflogger.driver;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.sql.ResultSet;
import java.util.UUID;

import org.eclipse.jdt.annotation.Nullable;

import ch.sla.jdbcperflogger.logger.PerfLogger;

public class LoggingResultSetInvocationHandler implements InvocationHandler {
    private final ResultSet wrappedResultSet;
    private final UUID logId;
    private final long fetchStartTime;
    private boolean closed;
    private int nbRowsIterated;
    private long fetchDurationNanos;

    LoggingResultSetInvocationHandler(final ResultSet rset, final UUID logId) {
        wrappedResultSet = rset;
        this.logId = logId;
        fetchStartTime = System.nanoTime();
    }

    @Override
    @Nullable
    public Object invoke(@Nullable final Object proxy, final Method method, final Object @Nullable [] args)
            throws Throwable {
        final long methodStartTime = System.nanoTime();
        final Object result = Utils.invokeUnwrapException(wrappedResultSet, method, args);
        final long oneRowFetchDurationNanos = System.nanoTime() - methodStartTime;

        final String methodName = method.getName();
        if (args == null || args.length == 0) {
            if ("close".equals(methodName)) {
                if (!closed) {
                    closed = true;
                    PerfLogger.logClosedResultSet(logId, System.nanoTime() - fetchStartTime, fetchDurationNanos,
                            nbRowsIterated);
                }
            } else if ("next".equals(methodName)) {
                fetchDurationNanos += oneRowFetchDurationNanos;
                if (Boolean.TRUE.equals(result)) {
                    nbRowsIterated++;
                }
            }
        }
        return result;
    }

}
