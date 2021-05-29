/* 
 * Copyright 2015 Alexander Nozik.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package hep.dataforge.control.collectors;

import hep.dataforge.tables.ValuesListener;
import hep.dataforge.utils.DateTimeUtils;
import hep.dataforge.values.Value;
import hep.dataforge.values.ValueFactory;
import hep.dataforge.values.ValueMap;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A class to dynamically collect measurements from multi-channel devices and
 * bundle them into DataPoints. The collect method is called when all values are
 * present.
 *
 * @author Alexander Nozik
 */
public class PointCollector implements ValueCollector {

    private final List<String> names;
    private final ValuesListener consumer;
    private final Map<String, Value> valueMap = new ConcurrentHashMap<>();
    //TODO make time averaging?

    public PointCollector(ValuesListener consumer, Collection<String> names) {
        this.names = new ArrayList<>(names);
        this.consumer = consumer;
    }

    public PointCollector(ValuesListener consumer, String... names) {
        this.names = Arrays.asList(names);
        this.consumer = consumer;
    }

    @Override
    public void put(String name, Value value) {
        valueMap.put(name, value);
        if (valueMap.keySet().containsAll(names)) {
            collect();
        }
    }

    @Override
    public void put(String name, Object value) {
        valueMap.put(name, ValueFactory.of(value));
        if (valueMap.keySet().containsAll(names)) {
            collect();
        }
    }

    /**
     * Could be used to force collect even if not all values are present
     */
    @Override
    public void collect() {
        collect(DateTimeUtils.now());
    }

    public synchronized void collect(Instant time) {
        ValueMap.Builder point = new ValueMap.Builder();

        point.putValue("timestamp", time);
        valueMap.entrySet().forEach((entry) -> {
            point.putValue(entry.getKey(), entry.getValue());
        });

        // filling all missing values with nulls
        names.stream().filter((name) -> (!point.build().hasValue(name))).forEach((name) -> {
            point.putValue(name, ValueFactory.NULL);
        });

        consumer.accept(point.build());
        valueMap.clear();
    }

    @Override
    public void clear() {
        valueMap.clear();
    }
    
    

}
