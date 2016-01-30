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

import hep.dataforge.functions.FunctionCaching;
import hep.dataforge.maths.NamedDoubleSet;
import hep.dataforge.maths.integration.GaussRuleIntegrator;
import hep.dataforge.maths.integration.UnivariateIntegrator;
import hep.dataforge.plots.XYPlotFrame;
import hep.dataforge.plots.data.PlottableFunction;
import static java.lang.Math.exp;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.commons.math3.analysis.BivariateFunction;
import org.apache.commons.math3.analysis.UnivariateFunction;
import org.apache.commons.math3.exception.OutOfRangeException;
import org.slf4j.LoggerFactory;

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
//    private static final UnivariateIntegrator tailIntegrator = new GaussRuleIntegrator(50);

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
        final double norm = integrator.integrate(func, 0d, cutoff) + tailNorm;
        return (e) -> func.value(e) / norm;
    }

    public static UnivariateFunction getSingleScatterFunction(NamedDoubleSet set) {

        final double exPos = set.getValue("exPos");
        final double ionPos = set.getValue("ionPos");
        final double exW = set.getValue("exW");
        final double ionW = set.getValue("ionW");
        final double exIonRatio = set.getValue("exIonRatio");

        return getSingleScatterFunction(exPos, ionPos, exW, ionW, exIonRatio);
    }

    static BivariateFunction getTrapFunction() {
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

    public static void plotScatter(XYPlotFrame frame, NamedDoubleSet set) {
        //"X", "shift", "exPos", "ionPos", "exW", "ionW", "exIonRatio"

//        JFreeChartFrame frame = JFreeChartFrame.drawFrame("Differential scattering crosssection", null);
        double X = set.getValue("X");

        final double exPos = set.getValue("exPos");

        final double ionPos = set.getValue("ionPos");

        final double exW = set.getValue("exW");

        final double ionW = set.getValue("ionW");

        final double exIonRatio = set.getValue("exIonRatio");

        UnivariateFunction scatterFunction = getSingleScatterFunction(exPos, ionPos, exW, ionW, exIonRatio);

        if (set.names().contains("X")) {
            final LossCalculator loss = LossCalculator.instance;
            final List<Double> probs = loss.getGunLossProbabilities(set.getValue("X"));
            UnivariateFunction single = (double e) -> probs.get(1) * scatterFunction.value(e);
            frame.add(new PlottableFunction("Single scattering", null, single, 0, 100, 1000));

            for (int i = 2; i < probs.size(); i++) {
                final int j = i;
                UnivariateFunction scatter = (double e) -> probs.get(j) * loss.getLossValue(j, e, 0d);
                frame.add(new PlottableFunction(j + " scattering", null, scatter, 0, 100, 1000));
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

            frame.add(new PlottableFunction("Total loss", null, total, 0, 100, 1000));

        } else {

            frame.add(new PlottableFunction("Differential crosssection", null, scatterFunction, 0, 100, 2000));
        }

    }

    private final Map<Integer, UnivariateFunction> cache = new HashMap<>();

    private LossCalculator() {
        cache.put(1, getSingleScatterFunction());
//        cache.put(2, getDoubleScatterFunction());
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
    private synchronized UnivariateFunction getLoss(int order) {
        if (order <= 0) {
            throw new IllegalArgumentException();
        }
        if (cache.containsKey(order)) {
            return cache.get(order);
        } else {
            LoggerFactory.getLogger(getClass())
                    .debug("Scatter cache of order {} not found. Updating", order);
            cache.put(order, getNextLoss(getMargin(order), getLoss(order - 1)));
            return cache.get(order);
        }
    }

    public BivariateFunction getLossFunction(int order) {
        assert order > 0;
        return (double Ei, double Ef) -> {
            return getLossValue(order, Ei, Ef);
        };
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
     *
     * дисер, стр.48
     *
     * @param X
     * @return
     */
    public List<Double> getLossProbabilities(double X) {
        List<Double> res = new ArrayList<>();
        double prob;
        if (X > 0) {
            prob = 1 / X * (1 - Math.exp(-X));
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
            double delta = Math.exp(-X);
            for (int i = 1; i < res.size() + 1; i++) {
                delta *= X / i;
            }
            prob -= delta / X;
            res.add(prob);
        }

        return res;
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

    public synchronized double getLossValue(int order, double Ei, double Ef) {
        if (Ei < Ef) {
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
            return integrator.integrate(integrand, 0d, margin);
        };

        return FunctionCaching.cacheUnivariateFunction(res, 0, margin, 200);

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
        List<Double> probs = getLossProbabilities(X);

        double sum = 0;
        for (int i = 1; i < probs.size(); i++) {
            sum += probs.get(i) * getLossValue(i, Ei, Ef);
        }
        return sum;
    }
}
