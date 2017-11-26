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

import hep.dataforge.stat.parametric.AbstractParametricBiFunction;
import hep.dataforge.values.ValueProvider;
import hep.dataforge.values.Values;

import static java.lang.Math.*;

/**
 * @author Darksnake
 */
public class GaussResolution extends AbstractParametricBiFunction {

    private static final String[] list = {"w", "shift"};

    private double cutoff = 4d;
//    private final UnivariateIntegrator integrator = new SimpsonIntegrator(1e-3, Double.MAX_VALUE, 3, 64);

    /**
     * @param cutoff - расстояние в сигмах от среднего значения, на котором
     *               функция считается обращающейся в ноль
     */
    public GaussResolution(double cutoff) {
        super(list);
        this.cutoff = cutoff;
    }

//    @Override
//    public ParametricFunction getConvolutedSpectrum(final RangedNamedSetSpectrum bare) {
//        return new AbstractParametricFunction(combineNamesWithEquals(this.namesAsArray(), bare.namesAsArray())) {
//            int maxEval = Global.instance().getInt("INTEGR_POINTS", 500);
//
//            @Override
//            public double derivValue(String parName, double x, Values set) {
//                double a = getLowerBound(set);
//                double b = getUpperBound(set);
//                assert b > a;
//                return integrator.integrate(maxEval, getDerivProduct(parName, bare, set, x), a, b);
//            }
//
//            @Override
//            public boolean providesDeriv(String name) {
//                return "w".equals(name) || bare.providesDeriv(name);
//            }
//
//            @Override
//            public double value(double x, Values set) {
//                double a = getLowerBound(set);
//                double b = getUpperBound(set);
//                assert b > a;
//                return integrator.integrate(maxEval, getProduct(bare, set, x), a, b);
//            }
//        };
//    }

//    @Override
//    public double getDeriv(String name, Values set, double input, double output) {
//        return this.derivValue(name, output - input, set);
//    }

//    private UnivariateFunction getDerivProduct(final String name, final ParametricFunction bare, final Values pars, final double x0) {
//        return (double x) -> {
//            double res1;
//            double res2;
//            try {
//                res1 = bare.derivValue(name, x0 - x, pars) * GaussResolution.this.value(x, pars);
//            } catch (NameNotFoundException ex1) {
//                res1 = 0;
//            }
//            try {
//                res2 = bare.value(x0 - x, pars) * GaussResolution.this.derivValue(name, x, pars);
//            } catch (NameNotFoundException ex2) {
//                res2 = 0;
//            }
//            return res1 + res2;
//        };
//    }

//    private double getLowerBound(final Values pars) {
//        return getPos(pars) - cutoff * getW(pars);
//    }



//    private UnivariateFunction getProduct(final ParametricFunction bare, final Values pars, final double x0) {
//        return (double x) -> {
//            double res = bare.value(x0 - x, pars) * GaussResolution.this.value(x, pars);
//            assert !isNaN(res);
//            return res;
//        };
//    }

//    private double getUpperBound(final Values pars) {
//        return getPos(pars) + cutoff * getW(pars);
//    }

//    @Override
//    public double getValue(Values set, double input, double output) {
//        return this.value(output - input, set);
//    }

    private double getPos(ValueProvider pars) {
        return pars.getDouble("shift", 0);
    }

    private double getW(ValueProvider pars) {
        return pars.getDouble("w");
    }

    @Override
    public boolean providesDeriv(String name) {
        return true;
    }


    @Override
    public double value(double x, double y, Values pars) {
        double d = x - y;
        if (abs(d - getPos(pars)) > cutoff * getW(pars)) {
            return 0;
        }
        double aux = (d - getPos(pars)) / getW(pars);
        return exp(-aux * aux / 2) / getW(pars) / sqrt(2 * Math.PI);
    }

    @Override
    public double derivValue(String parName, double x, double y, Values pars) {
        double d = x - y;
        if (abs(d - getPos(pars)) > cutoff * getW(pars)) {
            return 0;
        }
        double pos = getPos(pars);
        double w = getW(pars);

        switch (parName) {
            case "shift":
                return this.value(x, y, pars) * (d - pos) / w / w;
            case "w":
                return this.value(x, y, pars) * ((d - pos) * (d - pos) / w / w / w - 1 / w);
            default:
                return super.derivValue(parName, x, y, pars);
        }
    }
}
