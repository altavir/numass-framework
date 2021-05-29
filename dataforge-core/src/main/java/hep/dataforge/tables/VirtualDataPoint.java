/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package hep.dataforge.tables;

import hep.dataforge.names.NameList;
import hep.dataforge.values.Value;
import hep.dataforge.values.Values;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;
import java.util.function.BiFunction;

/**
 * A DataPoint that uses another data point or another object as a source but does not copy data
 * itself
 *
 * @author Alexander Nozik
 */
public class VirtualDataPoint<S> implements Values {

    private final S source;
    private final BiFunction<String, S, Value> transformation;
    private final NameList names;

    public VirtualDataPoint(S source, BiFunction<String, S, Value> transformation, String... names) {
        this.source = source;
        this.transformation = transformation;
        this.names = new NameList(names);
    }

    @NotNull
    @Override
    public Optional<Value> optValue(@NotNull String name) {
        if (hasValue(name)) {
            return Optional.ofNullable(transformation.apply(name, source));
        } else {
            return Optional.empty();
        }
    }

    @Override
    public NameList getNames() {
        return names;
    }


}
