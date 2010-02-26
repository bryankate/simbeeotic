package harvard.robobees.simbeeotic.model;


import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.annotation.ElementType;


/**
 * Use this annotation on a method in a {@link AbstractPhysicalModel} to
 * indicate that the method should be invoked on receipt of a particular
 * type of event. The annotated method must take exactly two arguments,
 * the {@link SimTime} and a subclass of {@link Event}.
 *
 * @author bkate
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface EventHandler {

}
