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
 * Iterative integrator based on any UnivariateIntegrator
 *
 * @author Alexander Nozik
 * @param <T> Integrand type for supplied univariate integrator
 * @version $Id: $Id
 */
public class IterativeUnivariateIntegrator<T extends UnivariateIntegrand> extends UnivariateIntegrator<T> {

    private final UnivariateIntegrator<T> integrator;

    /**
     * <p>Constructor for IterativeUnivariateIntegrator.</p>
     *
     * @param integrator a {@link hep.dataforge.maths.integration.UnivariateIntegrator} object.
     */
    public IterativeUnivariateIntegrator(UnivariateIntegrator<T> integrator) {
        this.integrator = integrator;
    }

    /** {@inheritDoc}
     * @return  */
    @Override
    public Predicate<T> getDefaultStoppingCondition() {
        return integrator.getDefaultStoppingCondition();
    }

    /** {@inheritDoc}
     * @return  */
    @Override
    protected T init(Double lower, Double upper, UnivariateFunction function) {
        return integrator.init(lower, upper, function);
    }

//    /** {@inheritDoc}
//     * @return  */
//    @Override
//    public T evaluate(T integrand, Predicate<T> condition) {
//        T firstResult = integrator.init(integrand, condition);
//        T nextResult = integrator.evaluate(firstResult, condition);
//
//        double dif = Math.abs(nextResult.getValue() - firstResult.getValue());
//        double relDif = dif / Math.abs(firstResult.getValue());
//        
//        // No improvement. Returning last result
//        if(dif == 0){
//            return nextResult;
//        }
//        
//        UnivariateIntegrand res = new UnivariateIntegrand(nextResult, dif,
//                relDif, nextResult.getNumCalls(), nextResult.getValue());
//
//        while (!condition.test(res)) {
//            firstResult = nextResult;
//            nextResult = integrator.evaluate(firstResult, condition);
//            dif = Math.abs(nextResult.getValue() - firstResult.getValue());
//            relDif = dif / Math.abs(firstResult.getValue());
//
//            res = new UnivariateIntegrand(nextResult, dif,
//                    relDif, nextResult.getIterations(), nextResult.getNumCalls(), nextResult.getValue());
//        }
//
//        return res;
//    }

    @Override
    public T evaluate(T integrand, Predicate<T> condition) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
}
