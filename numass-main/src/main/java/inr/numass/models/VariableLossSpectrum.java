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
import hep.dataforge.maths.integration.UnivariateIntegrator;
import hep.dataforge.stat.parametric.AbstractParametricFunction;
import hep.dataforge.stat.parametric.ParametricFunction;
import hep.dataforge.values.NamedValueSet;
import hep.dataforge.values.ValueProvider;
import inr.numass.utils.NumassIntegrator;
import org.apache.commons.math3.analysis.BivariateFunction;
import org.apache.commons.math3.analysis.UnivariateFunction;

import java.util.List;

/**
 *
 * @author Darksnake
 */
public class VariableLossSpectrum extends AbstractParametricFunction {

    public static String[] names = {"X", "shift", "exPos", "ionPos", "exW", "ionW", "exIonRatio"};

    public static VariableLossSpectrum withGun(double eMax) {
        return new VariableLossSpectrum(new GunSpectrum(), eMax);
    }

    public static VariableLossSpectrum withData(final UnivariateFunction transmission, double eMax) {
        return new VariableLossSpectrum(new AbstractParametricFunction(new String[0]) {

            @Override
            public double derivValue(String parName, double x, NamedValueSet set) {
                throw new NotDefinedException();
            }

            @Override
            public boolean providesDeriv(String name) {
                return false;
            }

            @Override
            public double value(double x, NamedValueSet set) {
                return transmission.value(x);
            }
        }, eMax);
    }

    private final ParametricFunction transmission;
    private UnivariateFunction backgroundFunction;
    private final double eMax;

    protected VariableLossSpectrum(ParametricFunction transmission, double eMax) {
        super(names);
        this.transmission = transmission;
        this.eMax = eMax;
    }

    @Override
    public double derivValue(String parName, double x, NamedValueSet set) {
        throw new NotDefinedException();
    }

    @Override
    public double value(double U, NamedValueSet set) {
        if (U >= eMax) {
            return 0;
        }
        double X = set.getDouble("X");
        final double shift = set.getDouble("shift");

        final LossCalculator loss = LossCalculator.instance();

        final List<Double> probs = loss.getGunLossProbabilities(X);
        final double noLossProb = probs.get(0);

        UnivariateFunction scatter = singleScatterFunction(set);

        final BivariateFunction lossFunction = (Ei, Ef) -> {
            if (probs.size() == 1) {
                return 0;
            }
            double sum = probs.get(1) * scatter.value(Ei - Ef);
            for (int i = 2; i < probs.size(); i++) {
                sum += probs.get(i) * loss.getLossValue(i, Ei, Ef);
            }
            return sum;
        };
        UnivariateFunction integrand = (double x) -> transmission.value(x, set) * lossFunction.value(x, U - shift);
        UnivariateIntegrator integrator;
        if (eMax - U > 150) {
            integrator = NumassIntegrator.getHighDensityIntegrator();
        } else {
            integrator = NumassIntegrator.getDefaultIntegrator();
        }
        return noLossProb * transmission.value(U - shift, set) + integrator.integrate(integrand, U, eMax);
    }

    public UnivariateFunction singleScatterFunction(ValueProvider set) {

        final double exPos = set.getDouble("exPos");
        final double ionPos = set.getDouble("ionPos");
        final double exW = set.getDouble("exW");
        final double ionW = set.getDouble("ionW");
        final double exIonRatio = set.getDouble("exIonRatio");

        return singleScatterFunction(exPos, ionPos, exW, ionW, exIonRatio);
    }

    public UnivariateFunction singleScatterFunction(
            final double exPos,
            final double ionPos,
            final double exW,
            final double ionW,
            final double exIonRatio) {
        return LossCalculator.getSingleScatterFunction(exPos, ionPos, exW, ionW, exIonRatio);
    }

    @Override
    public boolean providesDeriv(String name) {
        return false;
    }

    @Override
    protected double getDefaultParameter(String name) {
        switch (name) {
            case "shift":
                return 0;
            case "X":
                return 0;
            default:
                return super.getDefaultParameter(name);
        }
    }

}
