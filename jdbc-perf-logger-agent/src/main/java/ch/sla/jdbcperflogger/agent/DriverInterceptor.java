package ch.sla.jdbcperflogger.agent;

import java.sql.Connection;
import java.util.concurrent.Callable;

import org.eclipse.jdt.annotation.Nullable;

import ch.sla.jdbcperflogger.driver.WrappingDriver;
import net.bytebuddy.implementation.bind.annotation.SuperCall;

public class DriverInterceptor {
    public @Nullable Connection connect(final String url, final java.util.Properties info,
            @SuperCall final Callable<@Nullable Connection> originalConnectionCreator) throws Exception {

        return WrappingDriver.INSTANCE.wrapConnection(url, info, originalConnectionCreator);
    }
}