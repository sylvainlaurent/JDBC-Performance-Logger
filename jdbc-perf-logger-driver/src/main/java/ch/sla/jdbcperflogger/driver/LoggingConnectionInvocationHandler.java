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
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.Savepoint;
import java.sql.Statement;
import java.util.Properties;
import java.util.UUID;

import org.eclipse.jdt.annotation.Nullable;

import ch.sla.jdbcperflogger.DatabaseType;
import ch.sla.jdbcperflogger.TxCompletionType;
import ch.sla.jdbcperflogger.logger.PerfLogger;

public class LoggingConnectionInvocationHandler implements InvocationHandler {
    private final UUID connectionUuid;
    private final int connectionId;
    private final Connection wrappedConnection;
    private final DatabaseType databaseType;
    private final String url;
    private final Properties connectionProperties;

    LoggingConnectionInvocationHandler(final int connectionId, final Connection wrappedConnection, final String url,
            final Properties connectionProperties) {
        connectionUuid = UUID.randomUUID();
        this.connectionId = connectionId;
        this.wrappedConnection = wrappedConnection;
        databaseType = Utils.getDatabaseType(wrappedConnection);
        this.url = url;
        this.connectionProperties = connectionProperties;
    }

    @Override
    @Nullable
    public Object invoke(@Nullable final Object proxy, final Method method, final Object @Nullable [] args)
            throws Throwable {

        final String methodName = method.getName();

        TxCompletionType txCompletionType = null;
        String savePointDescription = null;
        if ("commit".equals(methodName)) {
            txCompletionType = TxCompletionType.COMMIT;
        } else if ("rollback".equals(methodName)) {
            if (args == null) {
                txCompletionType = TxCompletionType.ROLLBACK;
            } else {
                txCompletionType = TxCompletionType.ROLLBACK_TO_SAVEPOINT;
                final Savepoint savepoint = (Savepoint) args[0];
                savePointDescription = savepoint.toString();
            }
        } else if ("setSavepoint".equals(methodName)) {
            txCompletionType = TxCompletionType.SET_SAVE_POINT;
        }
        final long startTimeStamp = System.currentTimeMillis();
        long startNanos = -1;
        if (txCompletionType != null) {
            startNanos = System.nanoTime();
        }

        final Object result = Utils.invokeUnwrapException(wrappedConnection, method, args);
        if (result != null) {
            if ("createStatement".equals(methodName)) {
                return Proxy.newProxyInstance(result.getClass().getClassLoader(),
                        Utils.extractAllInterfaces(result.getClass()),
                        new LoggingStatementInvocationHandler(connectionUuid, (Statement) result, databaseType));
            } else if (("prepareStatement".equals(methodName) || "prepareCall".equals(methodName)) && args != null) {
                return Proxy.newProxyInstance(result.getClass().getClassLoader(),
                        Utils.extractAllInterfaces(result.getClass()), new LoggingPreparedStatementInvocationHandler(
                                connectionUuid, (PreparedStatement) result, (String) args[0], databaseType));
            }

        }

        if (txCompletionType != null) {
            if (txCompletionType == TxCompletionType.SET_SAVE_POINT && result != null) {
                final Savepoint savepoint = (Savepoint) result;
                savePointDescription = savepoint.toString();
            }
            PerfLogger.logTransactionComplete(connectionUuid, startTimeStamp, txCompletionType,
                    System.nanoTime() - startNanos, savePointDescription);
        }

        return result;
    }

    public UUID getConnectionUuid() {
        return connectionUuid;
    }

    public int getConnectionId() {
        return connectionId;
    }

    public String getUrl() {
        return url;
    }

    public Properties getConnectionProperties() {
        return connectionProperties;
    }

}
