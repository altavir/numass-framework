/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package hep.dataforge.maths.functions;

import hep.dataforge.exceptions.NotDefinedException;
import hep.dataforge.meta.Meta;
import hep.dataforge.utils.MetaFactory;

import java.util.HashMap;
import java.util.Map;

/**
 * A factory combining other factories by type
 *
 * @author Alexander Nozik
 */
public class MultiFactory<T>  {

    private final Map<String, MetaFactory<T>> factoryMap = new HashMap<>();

    public T build(String key, Meta meta) {
        if (factoryMap.containsKey(key)) {
            return factoryMap.get(key).build(meta);
        } else {
            throw new NotDefinedException("Function with type '" + key + "' not defined");
        }
    }

    public synchronized MultiFactory addFactory(String type, MetaFactory<T> factory) {
        this.factoryMap.put(type, factory);
        return this;
    }

}
