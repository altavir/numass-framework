/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package hep.dataforge.goals;

import java.util.stream.Stream;


/**
 * Goal with a-priori known result
 *
 * @param <T>
 * @author Alexander Nozik
 */
public class StaticGoal<T> extends AbstractGoal<T> {
    private final T result;

    public StaticGoal(T result) {
        this.result = result;
    }

    @Override
    public Stream<Goal<?>> dependencies() {
        return Stream.empty();
    }

    @Override
    protected T compute() throws Exception {
        return result;
    }
}
