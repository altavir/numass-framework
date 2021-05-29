/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package hep.dataforge.goals;

import java.util.concurrent.Executor;
import java.util.function.Supplier;
import java.util.stream.Stream;

/**
 * A goal which has no dependencies but generates result in a lazy way
 *
 * @param <T>
 * @author Alexander Nozik
 */
public class GeneratorGoal<T> extends AbstractGoal<T> {

    private final Supplier<T> sup;

    public GeneratorGoal(Executor executor, Supplier<T> sup) {
        super(executor);
        this.sup = sup;
    }

    public GeneratorGoal(Supplier<T> sup) {
        this.sup = sup;
    }

    @Override
    protected T compute() throws Exception {
        return sup.get();
    }

    @Override
    public Stream<Goal<?>> dependencies() {
        return Stream.empty();
    }

}
