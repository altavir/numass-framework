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

import hep.dataforge.io.history.History;
import hep.dataforge.maths.NamedMatrix;
import hep.dataforge.maths.functions.UniFunction;
import hep.dataforge.maths.functions.UnivariateSplineWrapper;
import hep.dataforge.stat.fit.FitState;
import hep.dataforge.stat.fit.IntervalEstimate;
import hep.dataforge.stat.fit.Param;
import org.apache.commons.math3.analysis.UnivariateFunction;
import org.apache.commons.math3.random.JDKRandomGenerator;
import org.apache.commons.math3.random.RandomGenerator;
import org.apache.commons.math3.random.SynchronizedRandomGenerator;

import java.io.PrintWriter;

import static hep.dataforge.maths.GridCalculator.getUniformUnivariateGrid;
import static hep.dataforge.names.NamesUtils.exclude;
import static hep.dataforge.stat.parametric.ParametricUtils.getNamedProjection;
import static java.lang.Math.max;
import static java.lang.Math.min;
import static java.lang.String.format;

/**
 * TODO переделать freePars в varArgs
 *
 * @author Alexander Nozik
 * @version $Id: $Id
 */
public class BayesianConfidenceLimit {

    /**
     * Constant <code>DEFAULT_MAX_CALLS=10000</code>
     */
    public static final int DEFAULT_MAX_CALLS = 10000;
    private final History log;
    private ConfidenceLimitCalculator previousCalc;
    private String previousPar;
    private FitState previousResult;

    /**
     * <p>
     * Constructor for BayesianConfidenceLimit.</p>
     *
     * @param log
     */
    public BayesianConfidenceLimit(History log) {
        this.log = log;
    }

    private RandomGenerator generator = new SynchronizedRandomGenerator(new JDKRandomGenerator());

    /**
     * A marginalized likelihood function without caching. Recalculates on call.
     *
     * @param parname
     * @param state
     * @param freePars
     * @param numCalls
     * @return
     */
    private UniFunction calculateLikelihood(String parname, FitState state, String[] freePars, int numCalls) {
        if (state == null) {
            throw new IllegalStateException("Fit information is not propertly initialized.");
        }
        LogLikelihood loglike = state.getLogLike();
        String[] parNames = exclude(freePars, parname);

        NamedMatrix matrix = state.getCovariance();
        MarginalFunctionBuilder marginal = new MarginalFunctionBuilder()
                .setFunction(loglike)
                .setParameters(state.getParameters(), freePars)
                .setNormalSampler(generator, state.getParameters(), matrix);

        return getNamedProjection(marginal.build(), parname, state.getParameters());
    }

    private ConfidenceLimitCalculator getCalculator(String parname, FitState result, String[] freePars) {
        return getCalculator(parname, result, freePars, DEFAULT_MAX_CALLS);
    }

    private ConfidenceLimitCalculator getCalculator(String parname, FitState state, String[] freePars, int numCalls) {
        log.report(format(
                "Calculating marginal likelihood cache for parameter \'%s\'.", parname));
        if ((previousCalc != null) && parname.equals(previousPar) && state.equals(previousResult)) {
            log.report("Using previously stored marginal likelihood immutable.");
            return previousCalc;
        } else {
            UnivariateFunction function = this.calculateLikelihood(parname, state, freePars, numCalls);
            Param par = state.getParameters().getByName(parname);
            Double a = max(par.getValue() - 4 * par.getErr(), par.getLowerBound());
            Double b = min(par.getValue() + 4 * par.getErr(), par.getUpperBound());
            ConfidenceLimitCalculator calculator = new ConfidenceLimitCalculator(function, a, b);
            // На случай, если нам нужно сделать несколько действий и не пересчитывать все
            previousCalc = calculator;
            previousPar = parname;
            previousResult = state;
            log.report("Likelihood immutable calculation completed.");
            return calculator;
        }
    }

    public FitState getConfidenceInterval(String parname, FitState state, String[] freePars) {
        log.report(
                format("Starting combined confidence limits calculation for parameter \'%s\'.", parname));

        ConfidenceLimitCalculator calculator = this.getCalculator(parname, state, freePars);
        IntervalEstimate limit = calculator.getEstimate(parname);
//        limit.parName = parname;
//        limit.freePars = freePars;
        log.report("Confidence limit calculation completed.");
        return state.edit().setInterval(limit).build();
    }

    public UnivariateFunction getMarginalLikelihood(String parname, FitState state, String... freePars) {
        ConfidenceLimitCalculator calculator;
        if (freePars.length == 0) {
            calculator = this.getCalculator(parname, state, state.getModel().namesAsArray());
        } else {
            calculator = this.getCalculator(parname, state, freePars);
        }
        return new UnivariateSplineWrapper(calculator.getProbability());
    }

    /**
     * Prints spline smoothed marginal likelihood for the parameter TODO нужно
     * сделать возможность контролировать количество точек кэширования
     *
     * @param out      a {@link java.io.PrintWriter} object.
     * @param parname  a {@link java.lang.String} object.
     * @param result   a {@link hep.dataforge.stat.fit.FitState} object.
     * @param freePars an array of {@link java.lang.String} objects.
     * @param numCalls a int.
     */
    public void printMarginalLikelihood(PrintWriter out, String parname, FitState result, String[] freePars, int numCalls) {
        ConfidenceLimitCalculator calculator = this.getCalculator(parname, result, freePars, numCalls);
        UnivariateFunction prob = calculator.getProbability();
        UnivariateFunction integr = calculator.getIntegralProbability();
        double[] grid = getUniformUnivariateGrid(calculator.a, calculator.b, 50);
        out.printf("%n*** The marginalized likelihood function for parameter \'%s\' ***%n%n", parname);
        out.printf("%-10s\t%-8s\t%-8s%n", parname, "Like", "Integral");
        for (int i = 0; i < grid.length; i++) {
            out.printf("%-10.8g\t%-8.8g\t%-8.8g%n", grid[i], prob.value(grid[i]), integr.value(grid[i]));

        }
        out.println();
    }
}
