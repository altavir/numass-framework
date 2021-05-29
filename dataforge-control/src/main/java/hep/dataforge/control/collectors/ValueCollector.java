/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package hep.dataforge.control.collectors;

import hep.dataforge.values.Value;
import hep.dataforge.values.ValueFactory;

/**
 * A collector of values which listens to some input values until condition
 * satisfied then pushes the result to external listener.
 *
 * @author <a href="mailto:altavir@gmail.com">Alexander Nozik</a>
 */
public interface ValueCollector {

    void put(String name, Value value);

    default void put(String name, Object value) {
        put(name, ValueFactory.of(value));
    }

    /**
     * Send current cached result to listener. Could be used to force collect
     * even if not all values are present.
     */
    void collect();
    
    /**
     * Clear currently collected data
     */
    void clear();
    
}
