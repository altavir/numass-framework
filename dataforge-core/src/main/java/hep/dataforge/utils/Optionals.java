package hep.dataforge.utils;

import java.util.*;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Temporary Either implementation. Waiting for Java 9 to replace
 * Created by darksnake on 19-Apr-17.
 */
public class Optionals<V> {

    public static <T> Optionals<T> either(Supplier<Optional<T>>... sups) {
        return new Optionals<T>(Arrays.asList(sups));
    }

    public static <T> Optionals<T> either(Stream<Supplier<Optional<T>>> stream) {
        return new Optionals<T>(stream.collect(Collectors.toList()));
    }

    public static <T> Optionals<T> either(Optional<T> opt) {
        return new Optionals<T>(() -> opt);
    }

    public static <T> Optionals<T> either(T opt) {
        return new Optionals<T>(() -> Optional.ofNullable(opt));
    }

    private Optionals(Collection<Supplier<Optional<V>>> set) {
        this.sups.addAll(set);
    }

    private Optionals(Supplier<Optional<V>> sup) {
        this.sups.add(sup);
    }

    private final List<Supplier<Optional<V>>> sups = new ArrayList<>();

    public Optionals<V> or(Supplier<Optional<V>> opt) {
        List<Supplier<Optional<V>>> newSups = new ArrayList<>(sups);
        newSups.add(opt);
        return new Optionals<>(newSups);
    }

    public Optionals<V> or(Optional<V> opt) {
        return or(() -> opt);
    }

    public Optionals<V> or(V val) {
        return or(() -> Optional.ofNullable(val));
    }

    public Optional<V> opt() {
        for (Supplier<Optional<V>> sup : sups) {
            Optional<V> opt = sup.get();
            if (opt.isPresent()) {
                return opt;
            }
        }
        return Optional.empty();
    }
}
