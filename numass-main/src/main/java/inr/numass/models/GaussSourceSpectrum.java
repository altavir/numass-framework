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
import hep.dataforge.stat.parametric.AbstractParametricFunction;
import hep.dataforge.values.ValueProvider;
import hep.dataforge.values.Values;

import static java.lang.Math.exp;
import static java.lang.Math.sqrt;

/**
 *
 * @author Darksnake
 */
public class GaussSourceSpectrum extends AbstractParametricFunction implements RangedNamedSetSpectrum {

    private static final String[] list = {"pos", "sigma"};
    private final double cutoff = 4d;

    public GaussSourceSpectrum() {
        super(list);
    }

    @Override
    public double derivValue(String parName, double E, Values set) {
        switch (parName) {
            case "pos":
                return getGaussPosDeriv(E, getPos(set), getSigma(set));
            case "sigma":
                return getGaussSigmaDeriv(E, getPos(set), getSigma(set));
            default:
                throw new NotDefinedException();
        }
    }

    double getGauss(double E, double pos, double sigma) {
        double aux = (E - pos) / sigma;
        return exp(-aux * aux / 2) / sigma / sqrt(2 * Math.PI);
    }

    double getGaussPosDeriv(double E, double pos, double sigma) {
        return getGauss(E, pos, sigma) * (E - pos) / sigma / sigma;
    }

    double getGaussSigmaDeriv(double E, double pos, double sigma) {
        return getGauss(E, pos, sigma) * ((E - pos) * (E - pos) / sigma / sigma / sigma - 1 / sigma);
    }

    @Override
    public Double max(Values set) {
        return getPos(set) + cutoff * getSigma(set);
    }

    @Override
    public Double min(Values set) {
        return getPos(set) - cutoff * getSigma(set);
    }

    private double getPos(ValueProvider set) {
        return set.getDouble("pos");
    }

    private double getSigma(ValueProvider set) {
        return set.getDouble("sigma");
    }

    @Override
    public boolean providesDeriv(String name) {
        return this.getNames().contains(name);
    }

    @Override
    public double value(final double E, Values set) {
        return getGauss(E, getPos(set), getSigma(set));
    }
}
