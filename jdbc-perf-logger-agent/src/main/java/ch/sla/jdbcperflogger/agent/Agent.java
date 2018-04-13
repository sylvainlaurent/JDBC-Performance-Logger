package ch.sla.jdbcperflogger.agent;

import static net.bytebuddy.matcher.ElementMatchers.named;

import java.io.IOException;
import java.lang.instrument.Instrumentation;
import java.sql.Driver;

import org.eclipse.jdt.annotation.Nullable;

import ch.sla.jdbcperflogger.driver.WrappingDriver;
import net.bytebuddy.agent.builder.AgentBuilder;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.dynamic.DynamicType.Builder;
import net.bytebuddy.implementation.MethodDelegation;
import net.bytebuddy.matcher.ElementMatchers;
import net.bytebuddy.utility.JavaModule;

public class Agent {
    protected static final String PREFIX = "[jdbc-perf-logger-agent]";
    private static boolean loaded = false;

    public static void premain(final String agentArgs, final Instrumentation inst) throws IOException {
        installAgent(agentArgs, inst);
    }

    public static void agentmain(final String agentArgs, final Instrumentation inst) {
        installAgent(agentArgs, inst);
    }

    private static void installAgent(final String agentArgs, final Instrumentation inst) {
        System.out.print(PREFIX + " Loading...");
        // final ByteBuddy byteBuddy = new ByteBuddy().with(Implementation.Context.Disabled.Factory.INSTANCE);

        new AgentBuilder.Default()//
                .with(new AgentBuilder.Listener.Adapter() {
                    @Override
                    public void onError(final String typeName, final @Nullable ClassLoader classLoader,
                            final @Nullable JavaModule module,
                            final boolean loaded1, final Throwable throwable) {
                        System.err.println(PREFIX + " ERROR " + typeName);
                        throwable.printStackTrace(System.err);
                    }

                })//
                  // .with(byteBuddy)//
                  // .with(AgentBuilder.InitializationStrategy.NoOp.INSTANCE)//
                  // .with(AgentBuilder.RedefinitionStrategy.REDEFINITION)//
                  // .with(AgentBuilder.TypeStrategy.Default.REBASE)//
                .type(ElementMatchers.isSubTypeOf(Driver.class).and(ElementMatchers.noneOf(WrappingDriver.class)))//
                .transform(new AgentBuilder.Transformer() {

                    @Override
                    public Builder<?> transform(final Builder<?> builder, final TypeDescription typeDescription,
                            final @Nullable ClassLoader classLoader, final @Nullable JavaModule module) {
                        System.out.println(PREFIX + " Transforming " + typeDescription + " for interception");
                        return builder//
                                .method(named("connect"))//
                                .intercept(MethodDelegation.withDefaultConfiguration()
                                        .filter(ElementMatchers.isMethod().and(named("connect")))
                                        .to(new DriverInterceptor()))//
                        ;
                    }
                })//
                .installOn(inst);

        // TODO: intercept javax.sql.DataSource, javax.sql.PooledConnection.getConnection()...

        loaded = true;
        System.out.println("OK");
    }

    public static boolean isLoaded() {
        return loaded;
    }

}
