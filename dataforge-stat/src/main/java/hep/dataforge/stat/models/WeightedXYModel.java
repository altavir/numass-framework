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
package hep.dataforge.stat.models;

import hep.dataforge.meta.Meta;
import hep.dataforge.stat.parametric.ParametricFunction;
import hep.dataforge.tables.ValuesAdapter;
import hep.dataforge.values.Values;

import java.util.function.Function;

/**
 * The XYModel in which errors in some (or all) point are increased in comparison with errors provided by DataSet
 * The weightFunction describes the increase in dispersion (not errors!) for each point.
 *
 * @author darksnake
 * @version $Id: $Id
 */
public class WeightedXYModel extends XYModel {

    private final Function<Values, Double> weightFunction;

    public WeightedXYModel(Meta meta, ParametricFunction source, Function<Values, Double> weightFunction) {
        super(meta, source);
        this.weightFunction = weightFunction;
    }

    public WeightedXYModel(Meta meta, ValuesAdapter format, ParametricFunction source, Function<Values, Double> weightFunction) {
        super(meta, format, source);
        this.weightFunction = weightFunction;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public double dispersion(Values point, Values pars) {
        return super.dispersion(point, pars) * weightFunction.apply(point);
    }
}
