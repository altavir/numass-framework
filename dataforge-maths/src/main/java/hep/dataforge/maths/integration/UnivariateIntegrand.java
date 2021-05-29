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

import org.apache.commons.math3.analysis.UnivariateFunction;

/**
 * <p>
 * UnivariateIntegrand class.</p>
 *
 * @author Alexander Nozik
 * @version $Id: $Id
 */
public class UnivariateIntegrand implements Integrand {

    private final UnivariateFunction function;
    /*
     In theory it is possible to make infinite bounds
     */
    private Double lower;
    private Double upper;
    private Double value;
    private int numCalls;

    public UnivariateIntegrand(Double lower, Double upper, UnivariateFunction function) {
        this.function = function;
        if (lower >= upper) {
            throw new IllegalArgumentException("Wrong bounds for integrand");
        }
        this.lower = lower;
        this.upper = upper;
    }

    public UnivariateIntegrand(UnivariateIntegrand integrand, int numCalls, Double value) {
        //TODO check value
        this.value = value;
        this.numCalls = numCalls + integrand.numCalls;
        this.function = integrand.getFunction();
        if (integrand.getLower() >= integrand.getUpper()) {
            throw new IllegalArgumentException("Wrong bounds for integrand");
        }
        this.lower = integrand.getLower();
        this.upper = integrand.getUpper();
    }

    /**
     * {@inheritDoc}
     *
     * @return
     */
    @Override
    public int getDimension() {
        return 1;
    }

    public double getFunctionValue(double x) {
        return function.value(x);
    }

    public UnivariateFunction getFunction() {
        return function;
    }

    public Double getLower() {
        return lower;
    }

    public Double getUpper() {
        return upper;
    }

    @Override
    public Double getValue() {
        return value;
    }

    @Override
    public int getNumCalls() {
        return numCalls;
    }

}
