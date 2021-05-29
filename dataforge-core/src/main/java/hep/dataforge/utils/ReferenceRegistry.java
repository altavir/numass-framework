/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package hep.dataforge.utils;

import org.jetbrains.annotations.NotNull;

import java.lang.ref.Reference;
import java.lang.ref.WeakReference;
import java.util.*;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * A registry of listener references. References could be weak to allow GC to
 * finalize referenced objects.
 *
 * @author Alexander Nozik
 */
public class ReferenceRegistry<T> extends AbstractCollection<T> {

    private final Set<Reference<T>> weakRegistry = new CopyOnWriteArraySet<>();
    /**
     * Used only to store strongreferences
     */
    private final Set<T> strongRegistry = new CopyOnWriteArraySet<>();

    /**
     * Listeners could be added either as strong references or weak references. Thread safe
     *
     * @param obj
     */
    public boolean add(T obj, boolean isStrong) {
        if (isStrong) {
            strongRegistry.add(obj);
        }
        return weakRegistry.add(new WeakReference<>(obj));
    }

    /**
     * Add a strong reference to registry
     *
     * @param obj
     */
    @Override
    public boolean add(T obj) {
        return add(obj, true);
    }

    @Override
    public boolean remove(Object obj) {
        strongRegistry.remove(obj);
        Reference<T> reference = weakRegistry.stream().filter(it -> obj.equals(it.get())).findFirst().orElse(null);
        return reference != null && weakRegistry.remove(reference);
    }

    @Override
    public boolean removeIf(Predicate<? super T> filter) {
        return strongRegistry.removeIf(filter) && weakRegistry.removeIf(it -> filter.test(it.get()));
    }

    @Override
    public void clear() {
        strongRegistry.clear();
        weakRegistry.clear();
    }

    /**
     * Clean up all null entries from weak registry
     */
    private void cleanUp() {
        weakRegistry.removeIf(ref -> ref.get() == null);
    }

    @NotNull
    @Override
    public Iterator<T> iterator() {
//        cleanUp();
        return weakRegistry.stream().map(Reference::get).filter(Objects::nonNull).iterator();
    }


    @Override
    public int size() {
        return weakRegistry.size();
    }

    public Optional<T> findFirst(Predicate<T> predicate) {
        return this.weakRegistry.stream()
                .map(Reference::get)
                .filter((t) -> t != null && predicate.test(t))
                .findFirst();
    }

    public List<T> findAll(Predicate<T> predicate) {
        return this.weakRegistry.stream()
                .map(Reference::get)
                .filter((t) -> t != null && predicate.test(t))
                .collect(Collectors.toList());
    }
}
