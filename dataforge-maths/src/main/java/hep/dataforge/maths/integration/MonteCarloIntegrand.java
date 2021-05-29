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

/**
 * <p>
 * MonteCarloIntegrand class.</p>
 *
 * @author Alexander Nozik
 * @version $Id: $Id
 */
public class MonteCarloIntegrand implements Integrand {

    private final MultivariateFunction function;
    private final Sampler sampler;
    private final Double value;
    private int numCalls;
    private double accuracy = Double.POSITIVE_INFINITY;

    public MonteCarloIntegrand(MonteCarloIntegrand integrand, int numCalls, Double value, double accuracy) {
        this.numCalls = numCalls;
        this.value = value;
        this.function = integrand.getFunction();
        this.sampler = integrand.getSampler();
        this.accuracy = accuracy;
    }

    public MonteCarloIntegrand(Sampler sampler, int numCalls, Double value, double accuracy, MultivariateFunction function) {
        this.numCalls = numCalls;
        this.value = value;
        this.function = function;
        this.sampler = sampler;
        this.accuracy = accuracy;
    }

    public MonteCarloIntegrand(Sampler sampler, MultivariateFunction function) {
        super();
        this.function = function;
        this.sampler = sampler;
        this.value = Double.NaN;
    }

    /**
     * {@inheritDoc}
     *
     * @return
     */
    @Override
    public int getDimension() {
        return getSampler().getDimension();
    }

    public double getFunctionValue(double[] x) {
        return function.value(x);
    }

//    public Sample getSample() {
//        return sampler.nextSample();
//    }

    public MultivariateFunction getFunction() {
        return function;
    }

    public Sampler getSampler() {
        return sampler;
    }

    @Override
    public Double getValue() {
        return value;
    }

    @Override
    public int getNumCalls() {
        return numCalls;
    }

    double getRelativeAccuracy() {
        return accuracy;
    }

}
