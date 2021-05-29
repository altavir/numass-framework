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

import java.util.function.Predicate;

/**
 * General ancestor for univariate integrators
 *
 * @author Alexander Nozik
 * @param <T>
 * @version $Id: $Id
 */
public abstract class UnivariateIntegrator<T extends UnivariateIntegrand> implements Integrator<T> {

    /**
     * Create initial Integrand for given function and borders. This method is
     * required to initialize any
     *
     * @param lower a {@link Double} object.
     * @param upper a {@link Double} object.
     * @param function a
     * {@link UnivariateFunction} object.
     * @return a T object.
     */
    protected abstract T init(Double lower, Double upper, UnivariateFunction function);

    public T evaluate(Double lower, Double upper, UnivariateFunction function) {
        return evaluate(UnivariateIntegrator.this.init(lower, upper, function));
    }

    public Double integrate(Double lower, Double upper, UnivariateFunction function) {
        return evaluate(lower, upper, function).getValue();
    }

    public T evaluate(Predicate<T> condition, Double lower, Double upper, UnivariateFunction function) {
        return evaluate(init(lower, upper, function), condition);
    }

    public T init(UnivariateIntegrand integrand) {
        return evaluate(integrand.getLower(), integrand.getUpper(), integrand.getFunction());
    }

    public T init(UnivariateIntegrand integrand, Predicate<T> condition) {
        return evaluate(condition, integrand.getLower(), integrand.getUpper(), integrand.getFunction());
    }
}
