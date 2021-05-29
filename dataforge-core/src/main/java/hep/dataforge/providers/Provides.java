package hep.dataforge.providers;

import java.lang.annotation.*;

/**
 * An annotation to mark provider methods. Provider method must take single string as argument and return {@link java.util.Optional}
 * Created by darksnake on 25-Apr-17.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Inherited
@Documented
public @interface Provides {
    /**
     * The name of the target this method provides
     *
     * @return
     */
    String value();
}
