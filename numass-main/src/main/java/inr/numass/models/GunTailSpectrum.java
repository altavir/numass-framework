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
import hep.dataforge.maths.NamedDoubleSet;
import hep.dataforge.names.Names;
import static java.lang.Math.abs;
import static java.lang.Math.exp;
import static java.lang.Math.sqrt;
import org.apache.commons.math3.analysis.UnivariateFunction;

public class GunTailSpectrum implements RangedNamedSetSpectrum {

    private final double cutoff = 4d;

    private final String[] list = {"pos", "tailShift", "tailAmp", "sigma"};

    @Override
    public double derivValue(String parName, double x, NamedDoubleSet set) {
        throw new NotDefinedException();
    }

    @Override
    public Double max(NamedDoubleSet set) {
        return set.getValue("pos") + cutoff * set.getValue("sigma");
    }

    @Override
    public Double min(NamedDoubleSet set) {
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
    public double value(double E, NamedDoubleSet set) {
        double pos = set.getValue("pos");
        double amp = set.getValue("tailAmp");
        double sigma = set.getValue("sigma");

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

    double tail(double E, double pos, NamedDoubleSet set) {

        double tailShift = set.getValue("tailShift");

        double delta = Math.max(pos - E - tailShift, 1d);
        UnivariateFunction func = (double d) -> 1d / d / d;
//        double tailNorm = NumassContext.defaultIntegrator.integrate(func, 0d, 300d);

        return func.value(delta);
    }

}
