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
package inr.numass.models;

import hep.dataforge.maths.integration.GaussRuleIntegrator;
import hep.dataforge.maths.integration.UnivariateIntegrator;
import hep.dataforge.plots.PlotFrame;
import hep.dataforge.plots.XYFunctionPlot;
import hep.dataforge.utils.Misc;
import hep.dataforge.values.Values;
import org.apache.commons.math3.analysis.BivariateFunction;
import org.apache.commons.math3.analysis.UnivariateFunction;
import org.apache.commons.math3.exception.OutOfRangeException;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static java.lang.Math.exp;

/**
 * Вычисление произвольного порядка функции рассеяния. Не учитывается
 * зависимость сечения от энергии электрона
 *
 * @author Darksnake
 */
public class LossCalculator {

    /**
     * порог по вероятности, до которого вычисляются компоненты функции потерь
     */
    public final static double SCATTERING_PROBABILITY_THRESHOLD = 1e-3;

    private static final LossCalculator instance = new LossCalculator();
    private static final UnivariateIntegrator integrator = new GaussRuleIntegrator(100);
    private static final Map<Double, List<Double>> lossProbCache = Misc.getLRUCache(100);
    private final Map<Integer, UnivariateFunction> cache = new HashMap<>();




    private LossCalculator() {
        cache.put(1, getSingleScatterFunction());
//        immutable.put(2, getDoubleScatterFunction());
    }

    public static UnivariateFunction getSingleScatterFunction() {
        final double A1 = 0.204;
        final double A2 = 0.0556;
        final double b = 14.0;
        final double pos1 = 12.6;
        final double pos2 = 14.3;
        final double w1 = 1.85;
        final double w2 = 12.5;

        return (double eps) -> {
            if (eps <= 0) {
                return 0;
            }
            if (eps <= b) {
                double z = eps - pos1;
                return A1 * exp(-2 * z * z / w1 / w1);
            } else {
                double z = 4 * (eps - pos2) * (eps - pos2);
                return A2 / (1 + z / w2 / w2);
            }
        };
    }

    /**
     * A generic loss function for numass experiment in "Lobashev"
     * parameterization
     *
     * @param exPos
     * @param ionPos
     * @param exW
     * @param ionW
     * @param exIonRatio
     * @return
     */
    public static UnivariateFunction getSingleScatterFunction(
            final double exPos,
            final double ionPos,
            final double exW,
            final double ionW,
            final double exIonRatio) {
        UnivariateFunction func = (double eps) -> {
            if (eps <= 0) {
                return 0;
            }
            double z1 = eps - exPos;
            double ex = exIonRatio * exp(-2 * z1 * z1 / exW / exW);

            double z = 4 * (eps - ionPos) * (eps - ionPos);
            double ion = 1 / (1 + z / ionW / ionW);

            double res;
            if (eps < exPos) {
                res = ex;
            } else {
                res = Math.max(ex, ion);
            }

            return res;
        };

        double cutoff = 25d;
        //caclulating lorentz integral analythically
        double tailNorm = (Math.atan((ionPos - cutoff) * 2d / ionW) + 0.5 * Math.PI) * ionW / 2d;
        final double norm = integrator.integrate(0d, cutoff, func) + tailNorm;
        return (e) -> func.value(e) / norm;
    }

    public static UnivariateFunction getSingleScatterFunction(Values set) {

        final double exPos = set.getDouble("exPos");
        final double ionPos = set.getDouble("ionPos");
        final double exW = set.getDouble("exW");
        final double ionW = set.getDouble("ionW");
        final double exIonRatio = set.getDouble("exIonRatio");

        return getSingleScatterFunction(exPos, ionPos, exW, ionW, exIonRatio);
    }

    public static BivariateFunction getTrapFunction() {
        return (double Ei, double Ef) -> {
            double eps = Ei - Ef;
            if (eps > 10) {
                return 1.86e-04 * exp(-eps / 25.0) + 5.5e-05;
            } else {
                return 0;
            }
        };
    }

    /**
     * Синглетон, так как кэши функций потреь можно вычислять один раз
     *
     * @return
     */
    public static LossCalculator instance() {
        return instance;
    }

    public static void plotScatter(PlotFrame frame, Values set) {
        //"X", "shift", "exPos", "ionPos", "exW", "ionW", "exIonRatio"

//        JFreeChartFrame frame = JFreeChartFrame.drawFrame("Differential scattering crosssection", null);
        double X = set.getDouble("X");

        final double exPos = set.getDouble("exPos");

        final double ionPos = set.getDouble("ionPos");

        final double exW = set.getDouble("exW");

        final double ionW = set.getDouble("ionW");

        final double exIonRatio = set.getDouble("exIonRatio");

        UnivariateFunction scatterFunction = getSingleScatterFunction(exPos, ionPos, exW, ionW, exIonRatio);

        if (set.getNames().contains("X")) {
            final LossCalculator loss = LossCalculator.instance;
            final List<Double> probs = loss.getGunLossProbabilities(set.getDouble("X"));
            UnivariateFunction single = (double e) -> probs.get(1) * scatterFunction.value(e);
            frame.add(XYFunctionPlot.Companion.plot("Single scattering", 0, 100, 1000, single::value));

            for (int i = 2; i < probs.size(); i++) {
                final int j = i;
                UnivariateFunction scatter = (double e) -> probs.get(j) * loss.getLossValue(j, e, 0d);
                frame.add(XYFunctionPlot.Companion.plot(j + " scattering", 0, 100, 1000, scatter::value));
            }

            UnivariateFunction total = (eps) -> {
                if (probs.size() == 1) {
                    return 0;
                }
                double sum = probs.get(1) * scatterFunction.value(eps);
                for (int i = 2; i < probs.size(); i++) {
                    sum += probs.get(i) * loss.getLossValue(i, eps, 0);
                }
                return sum;
            };

            frame.add(XYFunctionPlot.Companion.plot("Total loss", 0, 100, 1000, total::value));

        } else {

            frame.add(XYFunctionPlot.Companion.plot("Differential crosssection", 0, 100, 2000, scatterFunction::value));
        }

    }

    public List<Double> getGunLossProbabilities(double X) {
        List<Double> res = new ArrayList<>();
        double prob;
        if (X > 0) {
            prob = Math.exp(-X);
        } else {
            // если x ==0, то выживает только нулевой член, первый равен 1
            res.add(1d);
            return res;
        }
        res.add(prob);

        int n = 0;
        while (prob > SCATTERING_PROBABILITY_THRESHOLD) {
            /*
            * prob(n) = prob(n-1)*X/n;
             */
            n++;
            prob *= X / n;
            res.add(prob);
        }

        return res;
    }

    public double getGunZeroLossProb(double X) {
        return Math.exp(-X);
    }

    /**
     * Ленивое рекурсивное вычисление функции потерь через предыдущие
     *
     * @param order
     * @return
     */
    private UnivariateFunction getLoss(int order) {
        if (order <= 0) {
            throw new IllegalArgumentException();
        }
        if (cache.containsKey(order)) {
            return cache.get(order);
        } else {
            synchronized (this) {
                cache.computeIfAbsent(order, (i) -> {
                    LoggerFactory.getLogger(getClass())
                            .debug("Scatter immutable of order {} not found. Updating", i);
                    return getNextLoss(getMargin(i), getLoss(i - 1));
                });
                return cache.get(order);
            }
        }
    }

    public BivariateFunction getLossFunction(int order) {
        assert order > 0;
        return (double Ei, double Ef) -> getLossValue(order, Ei, Ef);
    }

    public List<Double> getLossProbDerivs(double X) {
        List<Double> res = new ArrayList<>();
        List<Double> probs = getLossProbabilities(X);

        double delta = Math.exp(-X);
        res.add((delta - probs.get(0)) / X);
        for (int i = 1; i < probs.size(); i++) {
            delta *= X / i;
            res.add((delta - probs.get(i)) / X);
        }

        return res;
    }

    /**
     * рекурсивно вычисляем все вероятности, котрорые выше порога
     * <p>
     * дисер, стр.48
     * </p>
     * @param X
     * @return
     */
    public List<Double> getLossProbabilities(double X) {
        return lossProbCache.computeIfAbsent(X, x -> {
            List<Double> res = new ArrayList<>();
            double prob;
            if (x > 0) {
                prob = 1 / x * (1 - Math.exp(-x));
            } else {
                // если x ==0, то выживает только нулевой член, первый равен нулю
                res.add(1d);
                return res;
            }
            res.add(prob);

            while (prob > SCATTERING_PROBABILITY_THRESHOLD) {
            /*
            * prob(n) = prob(n-1)-1/n! * X^n * exp(-X);
             */
                double delta = Math.exp(-x);
                for (int i = 1; i < res.size() + 1; i++) {
                    delta *= x / i;
                }
                prob -= delta / x;
                res.add(prob);
            }

            return res;
        });
    }

    public double getLossProbability(int order, double X) {
        if (order == 0) {
            if (X > 0) {
                return 1 / X * (1 - Math.exp(-X));
            } else {
                return 1d;
            }
        }
        List<Double> probs = getLossProbabilities(X);
        if (order >= probs.size()) {
            return 0;
        } else {
            return probs.get(order);
        }
    }

    public double getLossValue(int order, double Ei, double Ef) {
        if (Ei - Ef < 5d) {
            return 0;
        } else if (Ei - Ef >= getMargin(order)) {
            return 0;
        } else {
            return getLoss(order).value(Ei - Ef);
        }
    }

    /**
     * функция потерь с произвольными вероятностями рассеяния
     *
     * @param probs
     * @param Ei
     * @param Ef
     * @return
     */
    public double getLossValue(List<Double> probs, double Ei, double Ef) {
        double sum = 0;
        for (int i = 1; i < probs.size(); i++) {
            sum += probs.get(i) * getLossValue(i, Ei, Ef);
        }
        return sum;
    }

    /**
     * граница интегрирования
     *
     * @param order
     * @return
     */
    private double getMargin(int order) {
        return 80 + order * 50d;
    }

    /**
     * генерирует кэшированную функцию свертки loss со спектром однократных
     * потерь
     *
     * @param loss
     * @return
     */
    private UnivariateFunction getNextLoss(double margin, UnivariateFunction loss) {
        UnivariateFunction res = (final double x) -> {
            UnivariateFunction integrand = (double y) -> {
                try {
                    return loss.value(x - y) * getSingleScatterFunction().value(y);
                } catch (OutOfRangeException ex) {
                    return 0;
                }
            };
            return integrator.integrate(5d, margin, integrand);
        };

        return FunctionCaching.INSTANCE.cacheUnivariateFunction(0, margin, 200, res);

    }

    public BivariateFunction getTotalLossBivariateFunction(final double X) {
        return (double Ei, double Ef) -> getTotalLossValue(X, Ei, Ef);
    }

    /**
     * Значение полной производной функции потерь с учетом всех неисчезающих
     * порядков
     *
     * @param X
     * @param Ei
     * @param Ef
     * @return
     */
    public double getTotalLossDeriv(double X, double Ei, double Ef) {
        List<Double> probs = getLossProbDerivs(X);

        double sum = 0;
        for (int i = 1; i < probs.size(); i++) {
            sum += probs.get(i) * getLossValue(i, Ei, Ef);
        }
        return sum;
    }

    public BivariateFunction getTotalLossDerivBivariateFunction(final double X) {
        return (double Ei, double Ef) -> getTotalLossDeriv(X, Ei, Ef);
    }

    /**
     * Значение полной функции потерь с учетом всех неисчезающих порядков
     *
     * @param X
     * @param Ei
     * @param Ef
     * @return
     */
    public double getTotalLossValue(double X, double Ei, double Ef) {
        if (X == 0) {
            return 0;
        }
        List<Double> probs = getLossProbabilities(X);

        double sum = 0;
        for (int i = 1; i < probs.size(); i++) {
            sum += probs.get(i) * getLossValue(i, Ei, Ef);
        }
        return sum;
    }
}
