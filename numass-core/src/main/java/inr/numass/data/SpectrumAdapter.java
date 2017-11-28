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
package inr.numass.data;

import hep.dataforge.meta.Meta;
import hep.dataforge.meta.MetaBuilder;
import hep.dataforge.tables.BasicAdapter;
import hep.dataforge.tables.ValueMap;
import hep.dataforge.tables.ValuesAdapter;
import hep.dataforge.values.Value;
import hep.dataforge.values.Values;

import java.util.Optional;
import java.util.stream.Stream;

import static hep.dataforge.tables.Adapters.*;

/**
 * @author Darksnake
 */
public class SpectrumAdapter extends BasicAdapter {

    private static final String POINT_LENGTH_NAME = "time";

    public SpectrumAdapter(Meta meta) {
        super(meta);
    }

    public SpectrumAdapter(String xName, String yName, String yErrName, String measurementTime) {
        super(new MetaBuilder(ValuesAdapter.ADAPTER_KEY)
                .setValue(X_VALUE_KEY, xName)
                .setValue(Y_VALUE_KEY, yName)
                .setValue(Y_ERROR_KEY, yErrName)
                .setValue(POINT_LENGTH_NAME, measurementTime)
                .build()
        );
    }

    public SpectrumAdapter(String xName, String yName, String measurementTime) {
        super(new MetaBuilder(ValuesAdapter.ADAPTER_KEY)
                .setValue(X_VALUE_KEY, xName)
                .setValue(Y_VALUE_KEY, yName)
                .setValue(POINT_LENGTH_NAME, measurementTime)
                .build()
        );
    }

    public double getTime(Values point) {
        return this.optComponent(point, POINT_LENGTH_NAME).map(Value::doubleValue).orElse(1d);
    }

    public Values buildSpectrumDataPoint(double x, long count, double t) {
        return ValueMap.of(new String[]{getComponentName(X_VALUE_KEY), getComponentName(Y_VALUE_KEY),
                        getComponentName(POINT_LENGTH_NAME)},
                x, count, t);
    }

    public Values buildSpectrumDataPoint(double x, long count, double countErr, double t) {
        return ValueMap.of(new String[]{getComponentName(X_VALUE_KEY), getComponentName(Y_VALUE_KEY),
                        getComponentName(Y_ERROR_KEY), getComponentName(POINT_LENGTH_NAME)},
                x, count, countErr, t);
    }


    @Override
    public Optional<Value> optComponent(Values values, String component) {
        switch (component) {
            case "count":
                return super.optComponent(values, Y_VALUE_KEY);
            case Y_VALUE_KEY:
                return super.optComponent(values, Y_VALUE_KEY)
                        .map(it -> it.doubleValue() / getTime(values))
                        .map(Value::of);
            case Y_ERROR_KEY:
                Optional<Value> err = super.optComponent(values, Y_ERROR_KEY);
                if (err.isPresent()) {
                    return Optional.of(Value.of(err.get().doubleValue() / getTime(values)));
                } else {
                    double y = getComponent(values, Y_VALUE_KEY).doubleValue();
                    if (y < 0) {
                        return Optional.empty();
                    } else if (y == 0) {
                        //avoid infinite weights
                        return Optional.of(Value.of(1d / getTime(values)));
                    } else {
                        return Optional.of(Value.of(Math.sqrt(y) / getTime(values)));
                    }
                }

            default:
                return super.optComponent(values, component);
        }
    }

    @Override
    public Stream<String> listComponents() {
        return Stream.concat(super.listComponents(), Stream.of(X_VALUE_KEY, Y_VALUE_KEY, POINT_LENGTH_NAME)).distinct();
    }
}
