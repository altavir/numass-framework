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

import hep.dataforge.exceptions.NotDefinedException;
import hep.dataforge.names.Names;
import hep.dataforge.values.NamedValueSet;
import hep.dataforge.values.ValueProvider;
import static java.lang.Math.abs;
import static java.lang.Math.exp;
import static java.lang.Math.sqrt;
import org.apache.commons.math3.analysis.UnivariateFunction;

public class GunTailSpectrum implements RangedNamedSetSpectrum {

    private final double cutoff = 4d;

    private final String[] list = {"pos", "tailShift", "tailAmp", "sigma"};

    @Override
    public double derivValue(String parName, double x, NamedValueSet set) {
        throw new NotDefinedException();
    }

    @Override
    public Double max(NamedValueSet set) {
        return set.getDouble("pos") + cutoff * set.getDouble("sigma");
    }

    @Override
    public Double min(NamedValueSet set) {
        return 0d;
    }

    @Override
    public Names names() {
        return Names.of(list);
    }

    @Override
    public boolean providesDeriv(String name) {
        return false;
    }

    @Override
    public double value(double E, NamedValueSet set) {
        double pos = set.getDouble("pos");
        double amp = set.getDouble("tailAmp");
        double sigma = set.getDouble("sigma");

        if (E >= pos + cutoff * sigma) {
            return 0d;
        }

        return gauss(E, pos, sigma) * (1 - amp) + amp * tail(E, pos, set);
    }

    double gauss(double E, double pos, double sigma) {
        if (abs(E - pos) > cutoff * sigma) {
            return 0;
        }
        double aux = (E - pos) / sigma;
        return exp(-aux * aux / 2) / sigma / sqrt(2 * Math.PI);
    }

    double tail(double E, double pos, ValueProvider set) {

        double tailShift = set.getDouble("tailShift");

        double delta = Math.max(pos - E - tailShift, 1d);
        UnivariateFunction func = (double d) -> 1d / d / d;
//        double tailNorm = NumassContext.defaultIntegrator.integrate(func, 0d, 300d);

        return func.value(delta);
    }

}
