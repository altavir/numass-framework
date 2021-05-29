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
package hep.dataforge.tables;

import hep.dataforge.exceptions.NameNotFoundException;
import hep.dataforge.meta.MetaMorph;
import hep.dataforge.meta.MetaUtils;
import hep.dataforge.meta.Metoid;
import hep.dataforge.values.Value;
import hep.dataforge.values.Values;

import java.util.Optional;
import java.util.stream.Stream;

/**
 * An adapter to read specific components from Values
 *
 * @author Alexander Nozik
 */
public interface ValuesAdapter extends Metoid, MetaMorph {

    String ADAPTER_KEY = "@adapter";

    /**
     * Get a value with specific designation from given DataPoint
     *
     * @param point
     * @param component
     * @return
     */
    default Value getComponent(Values point, String component) {
        return optComponent(point, component).orElseThrow(() -> new NameNotFoundException("Component with name " + component + " not found", component));
    }

    default public String getComponentName(String component) {
        return getMeta().getString(component);
    }

    /**
     * Opt a specific component
     *
     * @param values
     * @param component
     * @return
     */
    default Optional<Value> optComponent(Values values, String component) {
        return values.optValue(getComponentName(component));
    }

    default Optional<Double> optDouble(Values values, String component) {
        return optComponent(values, component).map(Value::getDouble);
    }

    /**
     * List all components declared in this adapter.
     *
     * @return
     */
    default Stream<String> listComponents() {
        return MetaUtils.valueStream(getMeta()).map(it -> it.getFirst().toString());
    }


}
