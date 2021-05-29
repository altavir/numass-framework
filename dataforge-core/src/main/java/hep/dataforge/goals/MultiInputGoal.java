/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package hep.dataforge.goals;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 *
 * @author Alexander Nozik
 * @param <T>
 */
public abstract class MultiInputGoal<T> extends AbstractGoal<T> {

    //TODO replace RuntimeExceptions with specific exceptions
    public static String DEFAULT_SLOT = "";

    private final Map<String, Binding> bindings = new HashMap<>();

    public MultiInputGoal(ExecutorService executor) {
        super(executor);
    }

    /**
     * Bind the output slot of given goal to input slot of this goal
     *
     * @param dependency
     * @param inputSlot
     */
    protected void bindInput(Goal dependency, String inputSlot) {
        if (!this.bindings.containsKey(inputSlot)) {
            createBinding(inputSlot, Object.class);
        }
        bindings.get(inputSlot).bind(dependency);
    }

    //PENDING add default bining results?
    protected final void createBinding(String slot, Binding binding) {
        this.bindings.put(slot, binding);
    }

    protected final void createBinding(String slot, Class type) {
        this.bindings.put(slot, new SimpleBinding(type));
    }

    protected final void createListBinding(String slot, Class type) {
        this.bindings.put(slot, new ListBinding(type));
    }

    @Override
    protected T compute() throws Exception {
        return compute(gatherData());
    }

    protected Map<String, ?> gatherData() {
        Map<String, Object> data = new ConcurrentHashMap<>();
        bindings.forEach((slot, binding) -> {
            if (!binding.isBound()) {
                throw new RuntimeException("Required slot " + slot + " not boud");
            }
            data.put(slot, binding.getResult());
        });
        return data;
    }

    @Override
    public Stream<Goal<?>> dependencies() {
        Stream<Goal<?>> res = Stream.empty();
        for (Binding bnd : this.bindings.values()) {
            res = Stream.concat(res, bnd.dependencies());
        }
        return res;
    }

    protected abstract T compute(Map<String, ?> data);

    protected interface Binding<T> {

        /**
         * Start bound goal and return its result
         *
         * @return
         */
        T getResult();

        boolean isBound();

        void bind(Goal goal);

        Stream<Goal> dependencies();
    }

    protected class SimpleBinding<T> implements Binding<T> {

        private final Class<T> type;
        private Goal goal;

        public SimpleBinding(Class<T> type) {
            this.type = type;
        }

        @Override
        public T getResult() {
            goal.run();
            Object res = goal.asCompletableFuture().join();
            if (type.isInstance(res)) {
                return (T) res;
            } else {
                throw new RuntimeException("Type mismatch in goal result");
            }
        }

        @Override
        public boolean isBound() {
            return goal != null;
        }

        @Override
        public synchronized void bind(Goal goal) {
            if (isBound()) {
                throw new RuntimeException("Goal already bound");
            }
            this.goal = goal;
        }

        @Override
        public Stream<Goal> dependencies() {
            return Stream.concat(Stream.of(goal), goal.dependencies());
        }

    }

    protected class ListBinding<T> implements Binding<Set<T>> {

        private final Class<T> type;
        private final Set<Goal> goals = new HashSet<>();

        public ListBinding(Class<T> type) {
            this.type = type;
        }

        @Override
        public Set<T> getResult() {
            return goals.stream().parallel().map(goal -> {
                goal.run();
                Object res = goal.asCompletableFuture().join();
                if (type.isInstance(res)) {
                    return (T) res;
                } else {
                    throw new RuntimeException("Type mismatch in goal result");
                }
            }).collect(Collectors.toSet());
        }

        @Override
        public boolean isBound() {
            return !goals.isEmpty();
        }

        @Override
        public synchronized void bind(Goal goal) {
            this.goals.add(goal);
        }

        @Override
        public Stream<Goal> dependencies() {
            return goals.stream().flatMap(g -> Stream.concat(Stream.of(g), g.dependencies()));
        }

    }

}
