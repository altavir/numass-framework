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

import hep.dataforge.maths.integration.IntegratorFactory;
import hep.dataforge.maths.integration.UnivariateIntegrator;
import hep.dataforge.stat.fit.BasicIntervalEstimate;
import hep.dataforge.stat.fit.IntervalEstimate;
import org.apache.commons.math3.analysis.UnivariateFunction;
import org.apache.commons.math3.analysis.differentiation.DerivativeStructure;
import org.apache.commons.math3.analysis.differentiation.UnivariateDifferentiableFunction;
import org.apache.commons.math3.analysis.polynomials.PolynomialSplineFunction;
import org.apache.commons.math3.analysis.solvers.PegasusSolver;
import org.apache.commons.math3.exception.DimensionMismatchException;
import org.apache.commons.math3.util.Pair;

import static hep.dataforge.maths.functions.FunctionCaching.cacheUnivariateFunction;

/**
 * <p>ConfidenceLimitCalculator class.</p>
 *
 * @author Alexander Nozik
 * @version $Id: $Id
 */
public class ConfidenceLimitCalculator {

    /** Constant <code>DEFAULT_CACHE_POINTS=30</code> */
    public static int DEFAULT_CACHE_POINTS = 30;
    public static int DEFAULT_INTEGRATION_NODES = 300;

    Double a;
    Double b;
    /*
     * Нормированный интеграл функции. На всякий случай используем кэшироввание.
     * В силу того, что функция кэшированная, она сразу же вычисляется вместе с производной.
     */
    private PolynomialSplineFunction integralProbability;
    private double norming;
    /*
     * Кэшированный вариант исходной функции/
     *
     */
    private PolynomialSplineFunction probability;

    /**
     * Конструируем интервалы по готовому одномерному пострериорному правдоподобию
     *
     * @param func a {@link org.apache.commons.math3.analysis.UnivariateFunction} object.
     * @param a a double.
     * @param b a double.
     * @param cachePoints a int.
     */
    public ConfidenceLimitCalculator(UnivariateFunction func, double a, double b, int cachePoints) {
        this.setupfunctions(func, a, b, cachePoints);
    }

    /**
     * <p>Constructor for ConfidenceLimitCalculator.</p>
     *
     * @param func a {@link org.apache.commons.math3.analysis.UnivariateFunction} object.
     * @param a a double.
     * @param b a double.
     */
    public ConfidenceLimitCalculator(UnivariateFunction func, double a, double b) {
        this.setupfunctions(func, a, b, DEFAULT_CACHE_POINTS);
    }

//    public ConfidenceLimitCalculator(NamedMatrix cov, LogValue like, NamedVector point,
//            String parName, double a, double b, String... freePars) {
//        Marginalizer marginalizer = new Marginalizer(cov, like, point);
//        UnivariateFunction marginalLike = marginalizer.getUnivariateMarginalFunction(parName, freePars);
//        this.setupfunctions(marginalLike, a, b, DEFAULT_CACHE_POINTS);
//    }

    /**
     * <p>get90CLCentralinterval.</p>
     *
     * @return a {@link org.apache.commons.math3.util.Pair} object.
     */
    public Pair<Double, Double> get90CLCentralinterval() {
        Double lower = this.solve(0.05);
        Double upper = this.solve(0.95);
        return new Pair<>(lower, upper);
    }

    /**
     * <p>get90CLLowerLimit.</p>
     *
     * @return a {@link org.apache.commons.math3.util.Pair} object.
     */
    public Pair<Double, Double> get90CLLowerLimit() {
        Double lower = this.solve(0.10);
        return new Pair<>(lower, Double.POSITIVE_INFINITY);
    }

    /**
     * <p>get90CLUpperLimit.</p>
     *
     * @return a {@link org.apache.commons.math3.util.Pair} object.
     */
    public Pair<Double, Double> get90CLUpperLimit() {
        Double upper = this.solve(0.90);
        return new Pair<>(Double.NEGATIVE_INFINITY, upper);
    }

    /**
     * <p>get95CLCentralinterval.</p>
     *
     * @return a {@link org.apache.commons.math3.util.Pair} object.
     */
    public Pair<Double, Double> get95CLCentralinterval() {
        Double lower = this.solve(0.025);
        Double upper = this.solve(0.975);
        return new Pair<>(lower, upper);
    }

    /**
     * <p>get95CLLowerLimit.</p>
     *
     * @return a {@link org.apache.commons.math3.util.Pair} object.
     */
    public Pair<Double, Double> get95CLLowerLimit() {
        Double lower = this.solve(0.05);
        return new Pair<>(lower, Double.POSITIVE_INFINITY);
    }

    /**
     * <p>get95CLUpperLimit.</p>
     *
     * @return a {@link org.apache.commons.math3.util.Pair} object.
     */
    public Pair<Double, Double> get95CLUpperLimit() {
        Double upper = this.solve(0.95);
        return new Pair<>(Double.NEGATIVE_INFINITY, upper);
    }

    /**
     * <p>Getter for the field <code>integralProbability</code>.</p>
     *
     * @return the integralProbability
     */
    public PolynomialSplineFunction getIntegralProbability() {
        return integralProbability;
    }

    /**
     * Intellectual confidence limit calculator. It reports central interval in
     * case the confidence interval is narrower then the one for flat
     * distribution.
     *
     * @return
     */
    IntervalEstimate getEstimate(String parName) {
        BasicIntervalEstimate result = new BasicIntervalEstimate(0.95);
        Pair<Double, Double> interval95 = this.get95CLCentralinterval();
        double delta = interval95.getSecond() - interval95.getFirst();
        if (interval95.getFirst() <= a + 0.025 / 0.95 * delta) {
            interval95 = this.get95CLUpperLimit();
        } else if (interval95.getFirst() >= b - 0.025 / 0.95 * delta) {
            interval95 = this.get95CLLowerLimit();
        }
        result.put(parName, 0.95, interval95.getFirst(), interval95.getSecond());

        Pair<Double, Double> interval90 = this.get90CLCentralinterval();
        delta = interval90.getSecond() - interval90.getFirst();
        if (interval90.getFirst() <= a + 0.05 / 0.9 * delta) {
            interval90 = this.get90CLUpperLimit();
        } else if (interval90.getFirst() >= b - 0.05 / 0.9 * delta) {
            interval90 = this.get90CLLowerLimit();
        }
        result.put(parName, 0.90, interval90.getFirst(), interval90.getSecond());
        return result;
    }

    /**
     * <p>Getter for the field <code>norming</code>.</p>
     *
     * @return the norming
     */
    public double getNorming() {
        return norming;
    }

    /**
     * <p>Getter for the field <code>probability</code>.</p>
     *
     * @return the probability
     */
    public PolynomialSplineFunction getProbability() {
        return probability;
    }

    private void setupfunctions(UnivariateFunction func, final Double a, Double b, int numCachePoints) {
        assert func != null;
        assert a > Double.NEGATIVE_INFINITY;
        assert b < Double.POSITIVE_INFINITY;
        //TODO в перспективе можно тут сделать интегрирование с одной жесткой границей
        this.a = a;
        this.b = b;
        final PolynomialSplineFunction prob = cacheUnivariateFunction(a, b, numCachePoints, func);
        final UnivariateIntegrator integrator = IntegratorFactory.getGaussRuleIntegrator(DEFAULT_INTEGRATION_NODES);
        final double norm = integrator.integrate(a, b, prob);
        UnivariateFunction integralFunc = new IntegralFunc(a, prob, norm);
        this.probability = prob;
        // На всякий случай удваиваем количество узлов
        this.integralProbability = cacheUnivariateFunction(a, b, numCachePoints * 2, integralFunc);
        this.norming = norm;
        assert getIntegralProbability().value(a) == 0;
        assert getIntegralProbability().value(b) == 1;
    }

    private double solve(final double value) {
        assert (value > 0) && (value < 1);
        UnivariateDifferentiableFunction solveFunc = new UnivariateDifferentiableFunction() {
            @Override
            public DerivativeStructure value(DerivativeStructure t) throws DimensionMismatchException {
                return getIntegralProbability().value(t).subtract(value);
            }
            
            @Override
            public double value(double x) {
                return getIntegralProbability().value(x) - value;
            }
        };
//        NewtonRaphsonSolver solver = new NewtonRaphsonSolver();
        //FIXME Имеется баг с залезанием за границу интервала в.
        PegasusSolver solver = new PegasusSolver();
        return solver.solve(1000, solveFunc, a, b);
    }

    private static class IntegralFunc implements UnivariateFunction {

        private final Double a;
        private final UnivariateIntegrator integrator = IntegratorFactory.getGaussRuleIntegrator(DEFAULT_INTEGRATION_NODES);
        private final PolynomialSplineFunction prob;
        private final double norm;

        public IntegralFunc(Double a, PolynomialSplineFunction prob, double norm) {
            this.a = a;
            this.prob = prob;
            this.norm = norm;
        }

        @Override
        public double value(double x) {
            if (x <= a) {
                return 0;
            }
            return integrator.integrate(a, x, prob) / norm;
        }
    }
}
