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
package hep.dataforge.maths.integration;

import org.apache.commons.math3.analysis.MultivariateFunction;

import java.util.function.Predicate;

/**
 * <p>
 * MonteCarloIntegrator class.</p>
 *
 * @author Alexander Nozik
 * @version $Id: $Id
 */
public class MonteCarloIntegrator implements Integrator<MonteCarloIntegrand> {

    private static final int DEFAULT_MAX_CALLS = 100000;
    private static final double DEFAULT_MIN_RELATIVE_ACCURACY = 1e-3;

    private int sampleSizeStep = 500;

    public MonteCarloIntegrator() {
    }

    public MonteCarloIntegrator(int sampleSizeStep) {
        this.sampleSizeStep = sampleSizeStep;
    }

    /**
     * {@inheritDoc}
     *
     * @return
     */
    @Override
    public Predicate<MonteCarloIntegrand> getDefaultStoppingCondition() {
        return (t) -> t.getNumCalls() > DEFAULT_MAX_CALLS || t.getRelativeAccuracy() < DEFAULT_MIN_RELATIVE_ACCURACY;
    }

    /**
     * Integration with fixed sample size
     *
     * @param function   a
     *                   {@link org.apache.commons.math3.analysis.MultivariateFunction} object.
     * @param sampler    a {@link hep.dataforge.maths.integration.Sampler} object.
     * @param sampleSize a int.
     * @return a {@link hep.dataforge.maths.integration.MonteCarloIntegrand}
     * object.
     */
    public MonteCarloIntegrand evaluate(MultivariateFunction function, Sampler sampler, int sampleSize) {
        return evaluate(new MonteCarloIntegrand(sampler, function), sampleSize);
    }

    private MonteCarloIntegrand makeStep(MonteCarloIntegrand integrand) {
        double res = 0;

        for (int i = 0; i < sampleSizeStep; i++) {
            Sample sample = integrand.getSampler().nextSample(null);
            res += integrand.getFunctionValue(sample.getArray()) / sample.getWeight();
        }

        double oldValue = integrand.getValue();
        int oldCalls = integrand.getNumCalls();
        double value;
        if (Double.isNaN(oldValue)) {
            value = res / sampleSizeStep;
        } else {
            value = (oldValue * oldCalls + res) / (sampleSizeStep + oldCalls);
        }

        int evaluations = integrand.getNumCalls() + sampleSizeStep;

        double accuracy = Double.POSITIVE_INFINITY;

        if (!Double.isNaN(oldValue)) {
            accuracy = Math.abs(value - oldValue);
        }

        return new MonteCarloIntegrand(integrand, evaluations, value, accuracy);
    }

    /**
     * {@inheritDoc}
     *
     * @return
     */
    @Override
    public MonteCarloIntegrand evaluate(MonteCarloIntegrand integrand, Predicate<MonteCarloIntegrand> condition) {
        MonteCarloIntegrand res = integrand;
        while (!condition.test(res)) {
            res = makeStep(res);
        }
        return res;
    }

    /**
     * Integration with fixed maximum sample size
     *
     * @param integrand  a
     *                   {@link hep.dataforge.maths.integration.MonteCarloIntegrand} object.
     * @param sampleSize a int.
     * @return a {@link hep.dataforge.maths.integration.MonteCarloIntegrand}
     * object.
     */
    public MonteCarloIntegrand evaluate(MonteCarloIntegrand integrand, int sampleSize) {
        return MonteCarloIntegrator.this.evaluate(integrand, ((t) -> t.getNumCalls() >= sampleSize));
    }

}
