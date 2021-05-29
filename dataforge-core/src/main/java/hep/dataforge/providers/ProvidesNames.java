package hep.dataforge.providers;

import java.lang.annotation.*;

/**
 * Annotates the method that returns either collection of String or stream of String
 * Created by darksnake on 26-Apr-17.
 */

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Inherited
@Documented
public @interface ProvidesNames {
    /**
     * The name of the target this method provides
     *
     * @return
     */
    String value();


}