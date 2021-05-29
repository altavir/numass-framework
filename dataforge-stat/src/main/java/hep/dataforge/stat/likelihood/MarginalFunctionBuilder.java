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
package hep.dataforge.stat.likelihood;

import hep.dataforge.maths.NamedMatrix;
import hep.dataforge.maths.NamedVector;
import hep.dataforge.maths.integration.DistributionSampler;
import hep.dataforge.maths.integration.MonteCarloIntegrand;
import hep.dataforge.maths.integration.MonteCarloIntegrator;
import hep.dataforge.maths.integration.Sampler;
import hep.dataforge.names.NameList;
import hep.dataforge.stat.fit.FitResult;
import hep.dataforge.stat.parametric.AbstractParametricValue;
import hep.dataforge.stat.parametric.ParametricValue;
import hep.dataforge.utils.ArgumentChecker;
import hep.dataforge.utils.GenericBuilder;
import hep.dataforge.utils.Optionals;
import hep.dataforge.values.Value;
import hep.dataforge.values.Values;
import kotlin.Pair;
import org.apache.commons.math3.analysis.MultivariateFunction;
import org.apache.commons.math3.distribution.MultivariateNormalDistribution;
import org.apache.commons.math3.linear.RealMatrix;
import org.apache.commons.math3.linear.RealVector;
import org.apache.commons.math3.random.RandomGenerator;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Optional;

/**
 * @author Alexander Nozik
 * @version $Id: $Id
 */
public class MarginalFunctionBuilder implements GenericBuilder<ParametricValue, MarginalFunctionBuilder> {

    private MonteCarloIntegrator integrator = new MonteCarloIntegrator();
    private Sampler sampler;
    private Values startingPoint;
    /**
     * The set of parameters over which integration should be made
     */
    private String[] nuisancePars;
    private ParametricValue function;

    private NameList allNames() {
        return function.getNames();
    }

    private Sampler getSampler() {
        return sampler;
    }

    private int numCalls = -1;

    /**
     * Set a specific sampler for this function
     *
     * @param sampler
     * @return
     */
    public MarginalFunctionBuilder setSampler(Sampler sampler) {
        this.sampler = sampler;
        return self();
    }

    public MarginalFunctionBuilder setUniformSampler(RandomGenerator generator, List<Pair<Double, Double>> borders) {
        this.sampler = Sampler.uniform(generator, borders);
        return self();
    }

    public MarginalFunctionBuilder setNormalSampler(RandomGenerator generator, double[] means, double[][] covariance) {
        this.sampler = new DistributionSampler(new MultivariateNormalDistribution(generator, means, covariance));
        return self();
    }

    public MarginalFunctionBuilder setNormalSampler(RandomGenerator generator, RealVector means, RealMatrix covariance) {
        this.sampler = new DistributionSampler(new MultivariateNormalDistribution(generator, means.toArray(), covariance.getData()));
        return self();
    }

    public MarginalFunctionBuilder setMaxCalls(int numCalls) {
        this.numCalls = numCalls;
        return self();
    }

    public MarginalFunctionBuilder setNormalSampler(RandomGenerator generator, Values means, NamedMatrix covariance, String... parameters) {
        setNuisanceParameters(parameters);

        if (parameters.length == 0 && this.nuisancePars != null) {
            parameters = means.namesAsArray();
        }

        return setNormalSampler(
                generator,
                new NamedVector(means).subVector(parameters).getVector(),
                covariance.subMatrix(parameters).getMatrix()
        );
    }

    public MarginalFunctionBuilder setNormalSampler(RandomGenerator generator, FitResult fitResult, String... parameters) {
        if (fitResult.optCovariance().isPresent()) {
            return setNormalSampler(
                    generator,
                    fitResult.getParameters(),
                    fitResult.optCovariance().get(),
                    parameters
            );
        } else {
            throw new IllegalArgumentException("Need FitResult with valid covariance");
        }
    }

    public ParametricValue getFunction() {
        return function;
    }

    /**
     * Set the function
     *
     * @param function
     * @return
     */
    public MarginalFunctionBuilder setFunction(ParametricValue function) {
        this.function = function;
        return self();
    }

    /**
     * Define a subset of parameters over which one needs to marginalize
     *
     * @param startingPoint
     * @param nuisancePars
     * @return
     */
    public MarginalFunctionBuilder setParameters(Values startingPoint, String... nuisancePars) {
        this.startingPoint = startingPoint;
        if (nuisancePars.length == 0 && this.nuisancePars != null) {
            this.nuisancePars = startingPoint.namesAsArray();
        } else {
            this.nuisancePars = nuisancePars;
        }
        return self();
    }

    public MarginalFunctionBuilder setNuisanceParameters(String... nuisancePars) {
        this.nuisancePars = nuisancePars;
        return self();
    }

    @Override
    public MarginalFunctionBuilder self() {
        return this;
    }

    /**
     * Build a parametric marginalized function. If marginalization was performed over all parameters, than resulting
     * function is a fixed value and does not require additional parameters
     *
     * @return
     */
    @Override
    public ParametricValue build() {
        ArgumentChecker.checkNotNull(sampler, function);

        if (nuisancePars.length == 0) {
            this.nuisancePars = this.allNames().asArray();
        }

        ArgumentChecker.checkEqualDimensions(sampler.getDimension(), nuisancePars.length);
        NameList remaining = allNames().minus(nuisancePars);

        double divider;
        if (startingPoint != null) {
            divider = getFunction().value(startingPoint);
        } else {
            divider = 1;
        }

        return new AbstractParametricValue(remaining) {
            @Override
            public double value(Values pars) {
                MultivariateFunction func = new ExpLikelihood(divider);
                MonteCarloIntegrand integrand = new MonteCarloIntegrand(getSampler(), func);
                if (numCalls < 0) {
                    return divider * integrator.integrate(integrand);
                } else {
                    return divider * integrator.evaluate(integrand, numCalls).getValue();
                }
            }
        };
    }

    private class ExpLikelihood implements MultivariateFunction {

        private final double divider;

        public ExpLikelihood(double divider) {
            this.divider = divider;
        }

        public ExpLikelihood() {
            this.divider = 1;
        }

        public double getDivider() {
            return divider;
        }

        @Override
        public double value(double[] point) {
            Values actualVector;
            if (nuisancePars == null || nuisancePars.length == 0) {
                actualVector = new NamedVector(startingPoint.getNames(), point);
            } else {
                actualVector = new VectorWithDefault(point);
            }
            return getFunction().value(actualVector) / divider;
        }

        private class VectorWithDefault implements Values {

            NamedVector vector;

            private VectorWithDefault(double[] point) {
                this.vector = new NamedVector(nuisancePars, point);
            }

            @Override
            public NameList getNames() {
                return startingPoint.getNames();
            }

            @Override
            public Optional<Value> optValue(@NotNull String path) {
                return Optionals.either(vector.optValue(path)).or(startingPoint.optValue(path)).opt();
            }
        }
    }

    //    private static int DEFAULT_MAXIMUM_CALLS = 10000;
//
//    private ParametricValue like;
////    private NamedMatrix cov;
////    private RandomGenerator generator;
////    private NamedVector point;
//
//    /**
//     *
//     * @param like the function to be marginalized
//     * @param point the center of function Gaussian approximation
//     * @param cov covariance for function Gaussian approximation
//     * @param generator rundom number generator
//     * @throws NameNotFoundException
//     */
//    public MarginalFunctionBuilder(ParametricValue like, Values point, NamedMatrix cov,
//                                   RandomGenerator generator) throws NameNotFoundException {
//        //the parameter set is defined by function
//        this.cov = cov;
//        this.like = like;
//        this.generator = generator;
//        if (!point.names().contains(like.namesAsArray())) {
//            throw new NameNotFoundException();
//        }
//        this.point = new NamedVector(point);
//    }
//
//    public MarginalFunctionBuilder(ParametricValue like, Values point, NamedMatrix cov) {
//        this(like, point, cov, getDefaultRandomGenerator());
//    }
//
//    public double getMarginalValue(int maxCalls, final String... freePars) {
//        assert (like.names().contains(freePars));
//        if (!cov.names().contains(freePars)) {
//            throw new NameNotFoundException();
//        }
//        double[] vals = point.getArray(freePars);
//        double[][] mat = cov.subMatrix(freePars).getMatrix().getData();
//
//        Sampler sampler = new DistributionSampler(generator, vals, mat);
//        MonteCarloIntegrator integrator = new MonteCarloIntegrator();
//        MultivariateFunction expLike = new ExpLikelihood(freePars);
//        /*
//        * Используется нормировка, в которой максимум функции правдопадобия - единица
//        * при желании потом можно перенормировать обратно
//         */
//        Integrand res = integrator.evaluate(expLike, sampler, maxCalls);///exp(like.getScale());
//        LoggerFactory.getLogger(getClass()).info("Marginalization complete with {} calls",
//                res.getNumCalls());
//        return res.getValue();
//    }
//
//    public double getNorm(int maxCalls) {
//        return this.getMarginalValue(maxCalls, like.namesAsArray());
//    }
//
//    public UnivariateFunction getUnivariateMarginalFunction(
//            final int maxCalls, final String parName, String... freePars) {
//
//        if (!this.names().contains(parName)) {
//            throw new NameNotFoundException(parName);
//        }
//
//        if (!this.names().contains(freePars)) {
//            throw new NameNotFoundException();
//        }
//        final String[] variablePars;
//        if (freePars.length > 0) {
//            variablePars = freePars;
//        } else {
//            variablePars = NamesUtils.exclude(like.names(), parName);
//        }
//
//        return (double x) -> {
//            point.setValue(parName, x);
//            return getMarginalValue(maxCalls, variablePars);
//        };
//
//    }
//
//    public UnivariateFunction getUnivariateMarginalFunction(
//            final String parName, String... freePars) {
//        return this.getUnivariateMarginalFunction(DEFAULT_MAXIMUM_CALLS, parName, freePars);
//    }
//
}
