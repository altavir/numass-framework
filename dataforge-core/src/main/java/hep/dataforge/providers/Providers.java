package hep.dataforge.providers;

import hep.dataforge.exceptions.ChainPathNotSupportedException;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Utility methods for providers
 * Created by darksnake on 25-Apr-17.
 */
public class Providers {
    /**
     * Provide using custom resolver.
     *
     * @param path
     * @param resolver
     * @return
     */
    public static Optional<?> provide(Path path, Function<String, Optional<?>> resolver) {
        Optional<?> opt = resolver.apply(path.getName().toString());
        Optional<Path> tailOpt = path.optTail();
        if (tailOpt.isPresent()) {
            return opt.flatMap(res -> {
                if (res instanceof Provider) {
                    Provider p = (Provider) res;
                    //using default chain target if needed
                    Path tail = tailOpt.get();
                    if (tail.getTarget().isEmpty()) {
                        tail = tail.withTarget(p.getDefaultChainTarget());
                    }
                    return p.provide(tail);
                } else {
                    throw new ChainPathNotSupportedException();
                }
            });
        } else {
            return opt;
        }
    }

    public static Optional<?> provide(Object provider, Path path) {
        return provide(path, str -> provideDirect(provider, path.getTarget(), str));
    }


    public static Collection<String> listTargets(Object provider) {
        return findProviders(provider.getClass()).keySet();
    }

    @SuppressWarnings("unchecked")
    public static Stream<String> listContent(Object provider, String target) {
        return Stream.of(provider.getClass().getMethods())
                .filter(method -> method.isAnnotationPresent(ProvidesNames.class))
                .filter(method -> Objects.equals(method.getAnnotation(ProvidesNames.class).value(), target))
                .findFirst()
                .map(method -> {
                    try {
                        Object list = method.invoke(provider);
                        if (list instanceof Stream) {
                            return (Stream<String>) list;
                        } else if (list instanceof Collection) {
                            return ((Collection<String>) list).stream();
                        } else {
                            throw new Error("Wrong method annotated with ProvidesNames");
                        }
                    } catch (Exception e) {
                        throw new RuntimeException("Failed to provide names by reflections", e);
                    }
                }).orElse(Stream.empty());
    }

    /**
     * Provide direct descendant without using chain path
     *
     * @param provider
     * @param target
     * @param name
     * @return
     */
    private static Optional<?> provideDirect(Object provider, String target, String name) {
        Map<String, Method> providers = findProviders(provider.getClass());

        // using default target if needed
        if (target.isEmpty() && provider instanceof Provider) {
            target = ((Provider) provider).getDefaultTarget();
        }

        if (!providers.containsKey(target)) {
            return Optional.empty();
        } else {
            Method method = providers.get(target);
            try {
                Object result = method.invoke(provider, name);
                if (result instanceof Optional) {
                    return (Optional<?>) result;
                } else {
                    return Optional.ofNullable(result);
                }
            } catch (IllegalAccessException | InvocationTargetException | ClassCastException e) {
                throw new RuntimeException("Failed to provide by reflections. The method " + method.getName() + " is not a provider method", e);
            }
        }
    }

    private static Map<String, Method> findProviders(Class cl) {
        return Stream.of(cl.getMethods())
                .filter(method -> method.isAnnotationPresent(Provides.class))
                .collect(Collectors.toMap(method -> method.getAnnotation(Provides.class).value(), method -> method));
    }

}
