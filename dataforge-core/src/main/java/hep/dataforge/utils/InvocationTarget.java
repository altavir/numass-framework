package hep.dataforge.utils;

import java.lang.reflect.InvocationTargetException;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;

/**
 * An object that could receive a custom command. By default uses reflections to invoke a public method
 * Created by darksnake on 06-May-17.
 */
public interface InvocationTarget {
    /**
     * @param command
     * @param arguments
     * @return
     */
    default Object invoke(String command, Object... arguments) {
        try {
            return getClass().getMethod(command, Stream.of(arguments).map(Object::getClass).toArray(i -> new Class[i]))
                    .invoke(this, arguments);
        } catch (IllegalAccessException | NoSuchMethodException e) {
            throw new RuntimeException("Can't resolve command", e);
        } catch (InvocationTargetException e) {
            throw new RuntimeException("Execution of command failed", e);
        }
    }

    /**
     * Execute a command asynchronously
     * @param command
     * @param arguments
     * @return
     */
    default CompletableFuture<? extends Object> invokeInFuture(String command, Object... arguments) {
        return CompletableFuture.supplyAsync(() -> invoke(command, arguments));
    }
}
