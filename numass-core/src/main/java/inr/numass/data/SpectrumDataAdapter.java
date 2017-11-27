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

import hep.dataforge.exceptions.DataFormatException;
import hep.dataforge.meta.Meta;
import hep.dataforge.meta.MetaBuilder;
import hep.dataforge.tables.ValueMap;
import hep.dataforge.tables.ValuesAdapter;
import hep.dataforge.tables.XYAdapter;
import hep.dataforge.values.Value;
import hep.dataforge.values.Values;

import java.util.Objects;

/**
 * @author Darksnake
 */
public class SpectrumDataAdapter extends XYAdapter {

    private static final String POINT_LENGTH_NAME = "time";

    public SpectrumDataAdapter() {
    }

    public SpectrumDataAdapter(Meta meta) {
        super(meta);
    }

    public SpectrumDataAdapter(String xName, String yName, String yErrName, String measurementTime) {
        super(new MetaBuilder(ValuesAdapter.ADAPTER_KEY)
                .setValue(X_VALUE_KEY, xName)
                .setValue(Y_VALUE_KEY, yName)
                .setValue(Y_ERROR_KEY, yErrName)
                .setValue(POINT_LENGTH_NAME, measurementTime)
                .build()
        );
    }

    public SpectrumDataAdapter(String xName, String yName, String measurementTime) {
        super(new MetaBuilder(ValuesAdapter.ADAPTER_KEY)
                .setValue(X_VALUE_KEY, xName)
                .setValue(Y_VALUE_KEY, yName)
                .setValue(POINT_LENGTH_NAME, measurementTime)
                .build()
        );
    }

    public double getTime(Values point) {
        return this.getComponent(point, POINT_LENGTH_NAME, 1d).doubleValue();
    }

    public Values buildSpectrumDataPoint(double x, long count, double t) {
        return ValueMap.of(new String[]{nameFor(X_VALUE_KEY), nameFor(Y_VALUE_KEY),
                        nameFor(POINT_LENGTH_NAME)},
                x, count, t);
    }

    public Values buildSpectrumDataPoint(double x, long count, double countErr, double t) {
        return ValueMap.of(new String[]{nameFor(X_VALUE_KEY), nameFor(Y_VALUE_KEY),
                        nameFor(Y_ERROR_KEY), nameFor(POINT_LENGTH_NAME)},
                x, count, countErr, t);
    }

    @Override
    public boolean providesYError(Values point) {
        return true;
    }

    public long getCount(Values point) {
        return super.getY(point).numberValue().longValue();
    }

    @Override
    public Value getValue(Values point, String axis) {
        if (Objects.equals(axis, XYAdapter.Y_AXIS)) {
            return Value.of(getComponent(point, Y_VALUE_KEY).doubleValue() / getTime(point));
        } else {
            return super.getValue(point, axis);
        }
    }

    @Override
    public Value getError(Values point, String axis) {
        if (Objects.equals(axis, XYAdapter.Y_AXIS)) {
            if (super.providesYError(point)) {
                return Value.of(getComponent(point, Y_ERROR_KEY).doubleValue() / getTime(point));
            } else {
                double y = getComponent(point, Y_VALUE_KEY).doubleValue();
                if (y < 0) {
                    throw new DataFormatException();
                } else if (y == 0) {
                    //avoid infinite weights
                    return Value.of(1d / getTime(point));
                } else {
                    return Value.of(Math.sqrt(y) / getTime(point));
                }
            }
        } else {
            return super.getError(point, axis);
        }
    }
}
