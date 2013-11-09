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
import java.sql.Statement;
import java.util.UUID;

import javax.annotation.Nullable;

public class LoggingConnectionInvocationHandler implements InvocationHandler {
    private final UUID connectionUuid;
    private final int connectionId;
    private final Connection wrappedConnection;
    private final DatabaseType databaseType;
    private final String url;

    LoggingConnectionInvocationHandler(final int connectionId, final Connection wrappedConnection, final String url) {
        connectionUuid = UUID.randomUUID();
        this.connectionId = connectionId;
        this.wrappedConnection = wrappedConnection;
        databaseType = Utils.getDatabaseType(wrappedConnection);
        this.url = url;
    }

    @Override
    @Nullable
    public Object invoke(@Nullable final Object proxy, @Nullable final Method method, @Nullable final Object[] args)
            throws Throwable {
        assert method != null;

        final Object result = Utils.invokeUnwrapException(wrappedConnection, method, args);
        final String methodName = method.getName();
        if (result != null) {
            if ("createStatement".equals(methodName)) {
                return Proxy.newProxyInstance(LoggingConnectionInvocationHandler.class.getClassLoader(), Utils
                        .extractAllInterfaces(result.getClass()), new LoggingStatementInvocationHandler(connectionUuid,
                        (Statement) result, databaseType));
            } else if (("prepareStatement".equals(methodName) || "prepareCall".equals(methodName)) && args != null) {
                return Proxy.newProxyInstance(LoggingConnectionInvocationHandler.class.getClassLoader(), Utils
                        .extractAllInterfaces(result.getClass()), new LoggingPreparedStatementInvocationHandler(
                        connectionUuid, (PreparedStatement) result, (String) args[0], databaseType));
            }
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

}
