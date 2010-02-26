package harvard.robobees.simbeeotic.configuration;


import com.google.inject.BindingAnnotation;

import java.lang.annotation.Target;
import java.lang.annotation.Retention;
import static java.lang.annotation.RetentionPolicy.RUNTIME;
import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.ElementType.METHOD;


/**
 * @author bkate
 */
public class ConfigurationAnnotations {

    private ConfigurationAnnotations() {
    }


    @BindingAnnotation
    @Target({ FIELD, PARAMETER, METHOD })
    @Retention(RUNTIME)
    public static @interface GlobalScope {
    }
}
