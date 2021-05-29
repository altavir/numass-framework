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
import org.apache.commons.math3.analysis.integration.gauss.GaussIntegrator;
import org.apache.commons.math3.analysis.integration.gauss.GaussIntegratorFactory;
import org.apache.commons.math3.util.Pair;

import java.util.function.Predicate;

/**
 * <p>GaussRuleIntegrator class.</p>
 *
 * @author Alexander Nozik
 * @version $Id: $Id
 */
public class GaussRuleIntegrator extends UnivariateIntegrator<CMIntegrand> {

    private static final GaussIntegratorFactory factory = new GaussIntegratorFactory();
    private final int numpoints;
    private IntegratorType type = IntegratorType.LEGANDRE;

    /**
     * <p>Constructor for GaussRuleIntegrator.</p>
     *
     * @param nodes a int.
     */
    public GaussRuleIntegrator(int nodes) {
        this.numpoints = nodes;
    }

    /**
     * <p>Constructor for GaussRuleIntegrator.</p>
     *
     * @param nodes a int.
     * @param type a {@link hep.dataforge.maths.integration.GaussRuleIntegrator.IntegratorType} object.
     */
    public GaussRuleIntegrator(int nodes, IntegratorType type) {
        this.numpoints = nodes;
        this.type = type;
    }

    /** {@inheritDoc}
     * @return  */
    @Override
    public Predicate<CMIntegrand> getDefaultStoppingCondition() {
        return (t) -> true;
    }

    /** {@inheritDoc}
     * @return  */
    @Override
    protected CMIntegrand init(Double lower, Double upper, UnivariateFunction function) {
        return new CMIntegrand(lower, upper, function);
    }

    /** {@inheritDoc}
     * @return  */
    @Override
    public CMIntegrand evaluate(CMIntegrand integrand, Predicate<CMIntegrand> condition) {
        GaussIntegrator integrator = getIntegrator(integrand.getLower(), integrand.getUpper());
        double res = integrator.integrate(integrand.getFunction());
        return new CMIntegrand(integrand.getAbsoluteAccuracy(), integrand.getRelativeAccuracy(), 1, numpoints, res, integrand);
    }

    private GaussIntegrator getIntegrator(double lower, double upper) {
        switch (type) {
            case LEGANDRE:
                return factory.legendre(numpoints, lower, upper);
            case LEGANDREHP:
                return factory.legendreHighPrecision(numpoints, lower, upper);
            case UNIFORM:
                return new GaussIntegrator(getUniformRule(lower, upper, numpoints));
            default:
                throw new Error();
        }
    }

    private Pair<double[], double[]> getUniformRule(double min, double max, int numPoints) {
        assert numPoints > 2;
        double[] points = new double[numPoints];
        double[] weights = new double[numPoints];

        final double step = (max - min) / (numPoints - 1);
        points[0] = min;

        for (int i = 1; i < numPoints; i++) {
            points[i] = points[i - 1] + step;
            weights[i] = step;

        }
        return new Pair<>(points, weights);

    }

    public enum IntegratorType {
        UNIFORM,
        LEGANDRE,
        LEGANDREHP
    }
}
