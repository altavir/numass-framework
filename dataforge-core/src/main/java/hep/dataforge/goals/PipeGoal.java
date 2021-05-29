/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package hep.dataforge.goals;

import java.util.concurrent.Executor;
import java.util.function.Function;
import java.util.stream.Stream;

/**
 * A one-to one pipeline goal
 *
 * @author Alexander Nozik
 * @param <S>
 * @param <T>
 */
public class PipeGoal<S, T> extends AbstractGoal<T> {

    private final Goal<? extends S> source;
    private final Function<S, T> transformation;

    public PipeGoal(Executor executor, Goal<? extends S> source, Function<S, T> transformation) {
        super(executor);
        this.source = source;
        this.transformation = transformation;
    }

    public PipeGoal(Goal<S> source, Function<S, T> transformation) {
        this.source = source;
        this.transformation = transformation;
    }

    @Override
    protected T compute() {
        return transformation.apply(source.get());
    }

    @Override
    public Stream<Goal<?>> dependencies() {
        return Stream.of(source);
    }

    /**
     * Attach new pipeline goal to this one using same executor
     *
     * @param <R>
     * @param trans
     * @return
     */
    public <R> PipeGoal<T, R> andThen(Function<T, R> trans) {
        return new PipeGoal<>(getExecutor(), this, trans);
    }

}
