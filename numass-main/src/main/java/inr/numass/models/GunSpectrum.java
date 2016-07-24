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
import hep.dataforge.fitting.parametric.AbstractParametricFunction;
import hep.dataforge.maths.integration.UnivariateIntegrator;
import hep.dataforge.values.NamedValueSet;
import inr.numass.NumassContext;
import static java.lang.Math.abs;
import static java.lang.Math.exp;
import static java.lang.Math.sqrt;
import org.apache.commons.math3.analysis.UnivariateFunction;
import static java.lang.Math.abs;
import static java.lang.Math.abs;
import static java.lang.Math.abs;

/**
 *
 * @author Darksnake
 */
public class GunSpectrum extends AbstractParametricFunction {

    private static final String[] list = {"pos", "resA", "sigma"};
    private final double cutoff = 4d;
    protected final UnivariateIntegrator integrator;
            

    public GunSpectrum() {
        super(list);
        integrator = NumassContext.defaultIntegrator;
    }

    @Override
    public double derivValue(String parName, final double U, NamedValueSet set) {
        final double pos = set.getDouble("pos");
        final double sigma = set.getDouble("sigma");
        final double resA = set.getDouble("resA");

        if(sigma == 0) throw new NotDefinedException();
        
        UnivariateFunction integrand;
        switch (parName) {
            case "pos":
                integrand = (double E) -> transmissionValueFast(U, E, resA) * getGaussPosDeriv(E, pos, sigma);
                break;
            case "sigma":
                integrand = (double E) -> transmissionValueFast(U, E, resA) * getGaussSigmaDeriv(E, pos, sigma);
                break;
            case "resA":
                integrand = (double E) -> transmissionValueFastDeriv(U, E, resA) * getGauss(E, pos, sigma);
                break;
            default:
                throw new NotDefinedException();

        }

        if (pos + cutoff * sigma < U) {
            return 0;
        } else if (pos - cutoff * sigma > U * (1 + resA)) {
            return 0;
        } else {
            return integrator.integrate(integrand, pos - cutoff * sigma, pos + cutoff * sigma);
        }
    }

    double getGauss(double E, double pos, double sigma) {
        if (abs(E - pos) > cutoff * sigma) {
            return 0;
        }
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
    public boolean providesDeriv(String name) {
//        return false;
        return this.names().contains(name);
    }

    double transmissionValue(double U, double E, double resA, double resB) {
        assert resA > 0;
        assert resB > 0;
        double delta = resA * E;
        if (E - U < 0) {
            return 0;
        } else if (E - U > delta) {
            return 1;
        } else {
            return (1 - sqrt(1 - (E - U) / E * resB)) / (1 - sqrt(1 - resA * resB));
        }
    }

    double transmissionValueFast(double U, double E, double resA) {
        double delta = resA * E;
        if (E - U < 0) {
            return 0;
        } else if (E - U > delta) {
            return 1;
        } else {
            return (E - U) / delta;
        }
    }

    double transmissionValueFastDeriv(double U, double E, double resA) {
        double delta = resA * E;
        if (E - U < 0) {
            return 0;
        } else if (E - U > delta) {
            return 1;
        } else {
            return -(E - U) / delta / resA;
        }
    }

    @Override
    public double value(final double U, NamedValueSet set) {
        final double pos = set.getDouble("pos");
        final double sigma = set.getDouble("sigma");
        final double resA = set.getDouble("resA");
        
        if (sigma <1e-5 ) {
            return transmissionValueFast(U, pos, resA);
        }
        
        UnivariateFunction integrand = (double E) -> transmissionValueFast(U, E, resA) * getGauss(E, pos, sigma);
        
        if (pos + cutoff * sigma < U) {
            return 0;
        } else if (pos - cutoff * sigma > U * (1 + resA)) {
            return 1;
        } else {
            return integrator.integrate(integrand, pos - cutoff * sigma, pos + cutoff * sigma);
        }

    }
}
