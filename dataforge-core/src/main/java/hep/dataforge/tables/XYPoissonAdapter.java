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

import hep.dataforge.meta.Meta;
import hep.dataforge.utils.Optionals;
import hep.dataforge.values.Value;
import hep.dataforge.values.ValueFactory;
import hep.dataforge.values.Values;

import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * Specialized adapter for poissonian distributed values
 *
 * @author darksnake
 */
public class XYPoissonAdapter extends BasicAdapter {

    public XYPoissonAdapter(Meta meta) {
        super(meta);
    }

    @Override
    public Optional<Value> optComponent(Values values, String component) {
        if (Objects.equals(component, Adapters.Y_ERROR_KEY)) {
            return Optionals.either(super.optComponent(values, Adapters.Y_ERROR_KEY)).or(() -> {
                double y = Adapters.getYValue(this, values).getDouble();
                if (y > 0) {
                    return Optional.of(ValueFactory.of(Math.sqrt(y)));
                } else {
                    return Optional.empty();
                }
            }).opt();
        } else {
            return super.optComponent(values, component);
        }
    }

    @Override
    public Stream<String> listComponents() {
        return Stream.concat(super.listComponents(), Stream.of(Adapters.Y_ERROR_KEY)).distinct();
    }

}
