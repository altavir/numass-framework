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

import hep.dataforge.context.Global;
import hep.dataforge.exceptions.NameNotFoundException;
import hep.dataforge.stat.parametric.AbstractParametricFunction;
import hep.dataforge.stat.parametric.ParametricFunction;
import hep.dataforge.values.NamedValueSet;
import hep.dataforge.values.ValueProvider;
import org.apache.commons.math3.analysis.UnivariateFunction;
import org.apache.commons.math3.analysis.integration.SimpsonIntegrator;
import org.apache.commons.math3.analysis.integration.UnivariateIntegrator;

import static hep.dataforge.names.NamedUtils.combineNamesWithEquals;
import static java.lang.Double.isNaN;
import static java.lang.Math.*;

/**
 *
 * @author Darksnake
 */
public class GaussResolution extends AbstractParametricFunction implements Transmission {

    private static final String[] list = {"w"};

    private double cutoff = 4d;
    private final UnivariateIntegrator integrator = new SimpsonIntegrator(1e-3, Double.MAX_VALUE, 3, 64);

    /**
     *
     * @param cutoff - расстояние в сигмах от среднего значения, на котором
     * функция считается обращающейся в ноль
     */
    public GaussResolution(double cutoff) {
        super(list);
        this.cutoff = cutoff;
    }

    @Override
    public double derivValue(String name, double X, NamedValueSet pars) {
        if (abs(X - getPos(pars)) > cutoff * getW(pars)) {
            return 0;
        }
        switch (name) {
            case "pos":
                return this.value(X, pars) * (X - getPos(pars)) / getW(pars) / getW(pars);
            case "w":
                return this.value(X, pars) * ((X - getPos(pars)) * (X - getPos(pars)) / getW(pars) / getW(pars) / getW(pars) - 1 / getW(pars));
            default:
                return 0;
        }
    }

    @Override
    public ParametricFunction getConvolutedSpectrum(final RangedNamedSetSpectrum bare) {
        return new AbstractParametricFunction(combineNamesWithEquals(this.namesAsArray(), bare.namesAsArray())) {
            int maxEval = Global.instance().getInt("INTEGR_POINTS", 500);

            @Override
            public double derivValue(String parName, double x, NamedValueSet set) {
                double a = getLowerBound(set);
                double b = getUpperBound(set);
                assert b > a;
                return integrator.integrate(maxEval, getDerivProduct(parName, bare, set, x), a, b);
            }

            @Override
            public boolean providesDeriv(String name) {
                if ("w".equals(name)) {
                    return true;
                }
                return bare.providesDeriv(name);
            }

            @Override
            public double value(double x, NamedValueSet set) {
                double a = getLowerBound(set);
                double b = getUpperBound(set);
                assert b > a;
                return integrator.integrate(maxEval, getProduct(bare, set, x), a, b);
            }
        };
    }

    @Override
    public double getDeriv(String name, NamedValueSet set, double input, double output) {
        return this.derivValue(name, output - input, set);
    }

    private UnivariateFunction getDerivProduct(final String name, final ParametricFunction bare, final NamedValueSet pars, final double x0) {
        return (double x) -> {
            double res1;
            double res2;
            try {
                res1 = bare.derivValue(name, x0 - x, pars) * GaussResolution.this.value(x, pars);
            } catch (NameNotFoundException ex1) {
                res1 = 0;
            }
            try {
                res2 = bare.value(x0 - x, pars) * GaussResolution.this.derivValue(name, x, pars);
            } catch (NameNotFoundException ex2) {
                res2 = 0;
            }
            return res1 + res2;
        };
    }

    private double getLowerBound(final NamedValueSet pars) {
        return getPos(pars) - cutoff * getW(pars);
    }

    private double getPos(ValueProvider pars) {
//        return pars.getDouble("pos");
        // вряд ли стоит ожидать, что разрешение будет сдвигать среднее, поэтому оставляем один параметр
        return 0;
    }

    private UnivariateFunction getProduct(final ParametricFunction bare, final NamedValueSet pars, final double x0) {
        return (double x) -> {
            double res = bare.value(x0 - x, pars) * GaussResolution.this.value(x, pars);
            assert !isNaN(res);
            return res;
        };
    }

    private double getUpperBound(final NamedValueSet pars) {
        return getPos(pars) + cutoff * getW(pars);
    }

    @Override
    public double getValue(NamedValueSet set, double input, double output) {
        return this.value(output - input, set);
    }

    private double getW(ValueProvider pars) {
        return pars.getDouble("w");
    }

    @Override
    public boolean providesDeriv(String name) {
        return true;
    }

    @Override
    public double value(double x, NamedValueSet pars) {
        if (abs(x - getPos(pars)) > cutoff * getW(pars)) {
            return 0;
        }
        double aux = (x - getPos(pars)) / getW(pars);
        return exp(-aux * aux / 2) / getW(pars) / sqrt(2 * Math.PI);
    }
}
