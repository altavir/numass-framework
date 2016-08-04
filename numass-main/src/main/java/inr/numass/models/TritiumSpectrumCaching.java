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
package inr.numass.models;

import hep.dataforge.stat.parametric.ParametricFunction;
import hep.dataforge.maths.NamedVector;
import hep.dataforge.values.NamedValueSet;
import static java.lang.Math.abs;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Darksnake
 */
public class TritiumSpectrumCaching extends NamedSpectrumCaching {

    private double delta = 50d;

    public TritiumSpectrumCaching(ParametricFunction spectrum, double a, double b) {
        super(spectrum, a, b);
    }

    public TritiumSpectrumCaching(ParametricFunction spectrum, double a, double b, double delta) {
        super(spectrum, a, b);
        this.delta = delta;
    }

    @Override
    protected double transformation(CacheElement cache, NamedValueSet newSet, double x) throws TransformationNotAvailable {
        double res;
        NamedVector curSet = new NamedVector(newSet);
        double E0new = newSet.getDouble("E0");
        double E0old = cache.getCachedParameters().getDouble("E0");
        double E0delta = E0new - E0old;
        if (abs(E0delta) > delta) {
            LoggerFactory.getLogger(getClass())
                    .debug("The difference in 'E0' is too large. Caching is not available.");
            throw new TransformationNotAvailable();
        } else {
            res = cache.value(x - E0delta);//проверить знак
            curSet.setValue("E0", E0old);
        }

        if (sameSet(curSet, cache.getCachedParameters())) {
            return res;
        } else {
            throw new TransformationNotAvailable();
        }

    }
}
