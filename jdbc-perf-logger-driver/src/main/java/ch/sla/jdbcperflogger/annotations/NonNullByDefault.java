package ch.sla.jdbcperflogger.annotations;

import static java.lang.annotation.ElementType.CONSTRUCTOR;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.PACKAGE;
import static java.lang.annotation.ElementType.TYPE;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import javax.annotation.ParametersAreNonnullByDefault;

/**
 * Same behavior of Eclipse annotation. The semantic is different from {@link ParametersAreNonnullByDefault} since it
 * treats return values as non-nullable too.
 * 
 * @author slaurent
 * 
 */
@Documented
@Retention(RetentionPolicy.CLASS)
@Target({ PACKAGE, TYPE, METHOD, CONSTRUCTOR })
public @interface NonNullByDefault {
    /**
     * When parameterized with <code>false</code>, the annotation specifies that the current element should not apply
     * any default to un-annotated types.
     */
    boolean value() default true;
}
