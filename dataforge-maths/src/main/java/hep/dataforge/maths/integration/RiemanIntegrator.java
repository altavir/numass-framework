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

import hep.dataforge.maths.GridCalculator;
import org.apache.commons.math3.analysis.UnivariateFunction;

import java.util.function.Predicate;

/**
 * <p>
 * GaussRuleIntegrator class.</p>
 *
 * @author Alexander Nozik
 * @version $Id: $Id
 */
public class RiemanIntegrator extends UnivariateIntegrator<UnivariateIntegrand> {

    private final int numpoints;

    /**
     * <p>
     * Constructor for GaussRuleIntegrator.</p>
     *
     * @param nodes a int.
     */
    public RiemanIntegrator(int nodes) {
        if (nodes < 5) {
            throw new IllegalStateException("The number of integration nodes is to small");
        }
        this.numpoints = nodes;
    }

    /**
     * {@inheritDoc}
     *
     * @return
     */
    @Override
    public Predicate<UnivariateIntegrand> getDefaultStoppingCondition() {
        return (t) -> true;
    }

    /**
     * {@inheritDoc}
     *
     * @return
     */
    @Override
    protected UnivariateIntegrand init(Double lower, Double upper, UnivariateFunction function) {
        return new UnivariateIntegrand(lower, upper, function);
    }

    /**
     * {@inheritDoc}
     *
     * @return
     */
    @Override
    public UnivariateIntegrand evaluate(UnivariateIntegrand integrand, Predicate<UnivariateIntegrand> condition) {
        double[] grid = GridCalculator.getUniformUnivariateGrid(integrand.getLower(), integrand.getUpper(), numpoints);
        double res = 0;

        UnivariateFunction f = integrand.getFunction();

        double prevX;
        double nextX;

        for (int i = 0; i < grid.length - 1; i++) {
            prevX = grid[i];
            nextX = grid[i + 1];
            res += f.value(prevX) * (nextX - prevX);
        }

        return new UnivariateIntegrand(integrand, numpoints, res);
    }
}
