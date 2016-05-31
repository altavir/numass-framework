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

import hep.dataforge.exceptions.NamingException;
import hep.dataforge.exceptions.NotDefinedException;
import hep.dataforge.functions.AbstractParametricFunction;
import hep.dataforge.maths.integration.GaussRuleIntegrator;
import hep.dataforge.maths.integration.UnivariateIntegrator;
import hep.dataforge.values.NamedValueSet;
import java.util.List;
import org.apache.commons.math3.analysis.BivariateFunction;
import org.apache.commons.math3.analysis.UnivariateFunction;

/**
 *
 * @author Darksnake
 */
public class EmpiricalLossSpectrum extends AbstractParametricFunction {

    public static String[] names = {"X", "shift"};
    private final UnivariateFunction transmission;
    private final double eMax;

    private final UnivariateIntegrator integrator;

    public EmpiricalLossSpectrum(UnivariateFunction transmission, double eMax) throws NamingException {
        super(names);
        integrator = new GaussRuleIntegrator(300);
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

        //FIXME тут толщины усреднены по длине источника, а надо брать чистого Пуассона
        final List<Double> probs = LossCalculator.instance().getGunLossProbabilities(X);
        final double noLossProb = probs.get(0);
        final BivariateFunction lossFunction = (Ei, Ef) -> {
            return LossCalculator.instance().getLossValue(probs, Ei, Ef);
        };
        UnivariateFunction integrand = (double x) -> transmission.value(x) * lossFunction.value(x, U - shift);
        return noLossProb * transmission.value(U - shift) + integrator.integrate(integrand, U, eMax);
    }

    @Override
    public boolean providesDeriv(String name) {
        return false;
    }

}
