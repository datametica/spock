package org.spockframework.runtime.annotations;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * Annotation to mark a class being run concurrently.
 *
 * @author chetanc
 */
@Documented
@Target({TYPE})
@Retention(RUNTIME)
public @interface Parallel {

}
