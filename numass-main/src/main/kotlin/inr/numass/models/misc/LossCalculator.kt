/*
 * Copyright  2018 Alexander Nozik.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package inr.numass.models.misc

import hep.dataforge.maths.functions.FunctionCaching
import hep.dataforge.maths.integration.GaussRuleIntegrator
import hep.dataforge.plots.PlotFrame
import hep.dataforge.plots.data.XYFunctionPlot
import hep.dataforge.utils.Misc
import hep.dataforge.values.Values
import kotlinx.coroutines.*
import org.apache.commons.math3.analysis.BivariateFunction
import org.apache.commons.math3.analysis.UnivariateFunction
import org.apache.commons.math3.exception.OutOfRangeException
import org.slf4j.LoggerFactory
import java.lang.Math.exp
import java.util.*

/**
 * Вычисление произвольного порядка функции рассеяния. Не учитывается
 * зависимость сечения от энергии электрона
 *
 * @author Darksnake
 */
object LossCalculator {
    private val cache = HashMap<Int, Deferred<UnivariateFunction>>()

    private const val ION_POTENTIAL = 15.4//eV

    val adjustX = true


    private fun getX(set: Values, eIn: Double): Double {
        return if (adjustX) {
            //From our article
            set.getDouble("X") * Math.log(eIn / ION_POTENTIAL) * eIn * ION_POTENTIAL / 1.9580741410115568e6
        } else {
            set.getDouble("X")
        }
    }

    fun p0(set: Values, eIn: Double): Double {
        return LossCalculator.getLossProbability(0, getX(set, eIn))
    }

    fun getGunLossProbabilities(X: Double): List<Double> {
        val res = ArrayList<Double>()
        var prob: Double
        if (X > 0) {
            prob = Math.exp(-X)
        } else {
            // если x ==0, то выживает только нулевой член, первый равен 1
            res.add(1.0)
            return res
        }
        res.add(prob)

        var n = 0
        while (prob > SCATTERING_PROBABILITY_THRESHOLD) {
            /*
            * prob(n) = prob(n-1)*X/n;
             */
            n++
            prob *= X / n
            res.add(prob)
        }

        return res
    }

    fun getGunZeroLossProb(x: Double): Double {
        return Math.exp(-x)
    }


    private fun CoroutineScope.getCachedSpectrum(order: Int): Deferred<UnivariateFunction> {
        return when {
            order <= 0 -> error("Non-positive loss cache order")
            order == 1 -> CompletableDeferred(singleScatterFunction)
            else -> cache.getOrPut(order) {
                async {
                    LoggerFactory.getLogger(javaClass)
                            .debug("Scatter cache of order {} not found. Updating", order)
                    getNextLoss(getMargin(order), getCachedSpectrum(order - 1).await())
                }
            }
        }
    }

    /**
     * Ленивое рекурсивное вычисление функции потерь через предыдущие
     *
     * @param order
     * @return
     */
    private fun getLoss(order: Int): UnivariateFunction {
        return runBlocking { getCachedSpectrum(order).await() }
    }

    fun getLossFunction(order: Int): BivariateFunction {
        assert(order > 0)
        return BivariateFunction { Ei: Double, Ef: Double -> getLossValue(order, Ei, Ef) }
    }

    fun getLossProbDerivs(x: Double): List<Double> {
        val res = ArrayList<Double>()
        val probs = getLossProbabilities(x)

        var delta = Math.exp(-x)
        res.add((delta - probs[0]) / x)
        for (i in 1 until probs.size) {
            delta *= x / i
            res.add((delta - probs[i]) / x)
        }

        return res
    }

    /**
     * рекурсивно вычисляем все вероятности, котрорые выше порога
     *
     *
     * дисер, стр.48
     *
     * @param X
     * @return
     */
    fun calculateLossProbabilities(x: Double): List<Double> {
        val res = ArrayList<Double>()
        var prob: Double
        if (x > 0) {
            prob = 1 / x * (1 - Math.exp(-x))
        } else {
            // если x ==0, то выживает только нулевой член, первый равен нулю
            res.add(1.0)
            return res
        }
        res.add(prob)

        while (prob > SCATTERING_PROBABILITY_THRESHOLD) {
            /*
        * prob(n) = prob(n-1)-1/n! * X^n * exp(-X);
         */
            var delta = Math.exp(-x)
            for (i in 1 until res.size + 1) {
                delta *= x / i
            }
            prob -= delta / x
            res.add(prob)
        }

        return res
    }

    fun getLossProbabilities(x: Double): List<Double> = lossProbCache.getOrPut(x) { calculateLossProbabilities(x) }

    fun getLossProbability(order: Int, X: Double): Double {
        if (order == 0) {
            return if (X > 0) {
                1 / X * (1 - Math.exp(-X))
            } else {
                1.0
            }
        }
        val probs = getLossProbabilities(X)
        return if (order >= probs.size) {
            0.0
        } else {
            probs[order]
        }
    }

    fun getLossValue(order: Int, Ei: Double, Ef: Double): Double {
        return when {
            Ei - Ef < 5.0 -> 0.0
            Ei - Ef >= getMargin(order) -> 0.0
            else -> getLoss(order).value(Ei - Ef)
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
    fun getLossValue(probs: List<Double>, Ei: Double, Ef: Double): Double {
        var sum = 0.0
        for (i in 1 until probs.size) {
            sum += probs[i] * getLossValue(i, Ei, Ef)
        }
        return sum
    }

    /**
     * граница интегрирования
     *
     * @param order
     * @return
     */
    private fun getMargin(order: Int): Double {
        return 80 + order * 50.0
    }

    /**
     * генерирует кэшированную функцию свертки loss со спектром однократных
     * потерь
     *
     * @param loss
     * @return
     */
    private fun getNextLoss(margin: Double, loss: UnivariateFunction?): UnivariateFunction {
        val res = { x: Double ->
            val integrand = UnivariateFunction { y: Double ->
                try {
                    loss!!.value(x - y) * singleScatterFunction.value(y)
                } catch (ex: OutOfRangeException) {
                    0.0
                }
            }
            integrator.integrate(5.0, margin, integrand)
        }

        return FunctionCaching.cacheUnivariateFunction(0.0, margin, 200, res)

    }

    fun getTotalLossBivariateFunction(X: Double): BivariateFunction {
        return BivariateFunction { Ei: Double, Ef: Double -> getTotalLossValue(X, Ei, Ef) }
    }

    /**
     * Значение полной производной функции потерь с учетом всех неисчезающих
     * порядков
     *
     * @param X
     * @param eIn
     * @param eOut
     * @return
     */
    fun getTotalLossDeriv(X: Double, eIn: Double, eOut: Double): Double {
        val probs = getLossProbDerivs(X)

        var sum = 0.0
        for (i in 1 until probs.size) {
            sum += probs[i] * getLossValue(i, eIn, eOut)
        }
        return sum
    }

    fun getTotalLossDeriv(pars: Values, eIn: Double, eOut: Double) = getTotalLossDeriv(getX(pars, eIn), eIn, eOut)

    fun getTotalLossDerivBivariateFunction(X: Double) = BivariateFunction { Ei: Double, Ef: Double -> getTotalLossDeriv(X, Ei, Ef) }


    /**
     * Значение полной функции потерь с учетом всех неисчезающих порядков
     *
     * @param x
     * @param Ei
     * @param Ef
     * @return
     */
    fun getTotalLossValue(x: Double, Ei: Double, Ef: Double): Double {
        return if (x == 0.0) {
            0.0
        } else {
            val probs = getLossProbabilities(x)
            (1 until probs.size).sumOf { i ->
                probs[i] * getLossValue(i, Ei, Ef)
            }
        }
    }

    fun getTotalLossValue(pars: Values, Ei: Double, Ef: Double): Double = getTotalLossValue(getX(pars, Ei), Ei, Ef)


    /**
     * порог по вероятности, до которого вычисляются компоненты функции потерь
     */
    private const val SCATTERING_PROBABILITY_THRESHOLD = 1e-3
    private val integrator = GaussRuleIntegrator(100)
    private val lossProbCache = Misc.getLRUCache<Double, List<Double>>(100)


    private val A1 = 0.204
    private val A2 = 0.0556
    private val b = 14.0
    private val pos1 = 12.6
    private val pos2 = 14.3
    private val w1 = 1.85
    private val w2 = 12.5

    val singleScatterFunction = UnivariateFunction { eps: Double ->
        when {
            eps <= 0 -> 0.0
            eps <= b -> {
                val z = eps - pos1
                A1 * exp(-2.0 * z * z / w1 / w1)
            }
            else -> {
                val z = 4.0 * (eps - pos2) * (eps - pos2)
                A2 / (1 + z / w2 / w2)
            }
        }
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
    fun getSingleScatterFunction(
            exPos: Double,
            ionPos: Double,
            exW: Double,
            ionW: Double,
            exIonRatio: Double): UnivariateFunction {
        val func = UnivariateFunction { eps: Double ->
            if (eps <= 0) {
                0.0
            } else {
                val z1 = eps - exPos
                val ex = exIonRatio * exp(-2.0 * z1 * z1 / exW / exW)

                val z = 4.0 * (eps - ionPos) * (eps - ionPos)
                val ion = 1 / (1 + z / ionW / ionW)

                if (eps < exPos) {
                    ex
                } else {
                    Math.max(ex, ion)
                }
            }
        }

        val cutoff = 25.0
        //caclulating lorentz integral analythically
        val tailNorm = (Math.atan((ionPos - cutoff) * 2.0 / ionW) + 0.5 * Math.PI) * ionW / 2.0
        val norm = integrator.integrate(0.0, cutoff, func)!! + tailNorm
        return UnivariateFunction { e -> func.value(e) / norm }
    }

    fun getSingleScatterFunction(set: Values): UnivariateFunction {

        val exPos = set.getDouble("exPos")
        val ionPos = set.getDouble("ionPos")
        val exW = set.getDouble("exW")
        val ionW = set.getDouble("ionW")
        val exIonRatio = set.getDouble("exIonRatio")

        return getSingleScatterFunction(exPos, ionPos, exW, ionW, exIonRatio)
    }

    val trapFunction: BivariateFunction
        get() = BivariateFunction { Ei: Double, Ef: Double ->
            val eps = Ei - Ef
            if (eps > 10) {
                1.86e-04 * exp(-eps / 25.0) + 5.5e-05
            } else {
                0.0
            }
        }

    fun plotScatter(frame: PlotFrame, set: Values) {
        //"X", "shift", "exPos", "ionPos", "exW", "ionW", "exIonRatio"

        //        JFreeChartFrame frame = JFreeChartFrame.drawFrame("Differential scattering crosssection", null);
        val X = set.getDouble("X")

        val exPos = set.getDouble("exPos")

        val ionPos = set.getDouble("ionPos")

        val exW = set.getDouble("exW")

        val ionW = set.getDouble("ionW")

        val exIonRatio = set.getDouble("exIonRatio")

        val scatterFunction = getSingleScatterFunction(exPos, ionPos, exW, ionW, exIonRatio)

        if (set.names.contains("X")) {
            val probs = LossCalculator.getGunLossProbabilities(set.getDouble("X"))
            val single = { e: Double -> probs[1] * scatterFunction.value(e) }
            frame.add(XYFunctionPlot.plot("Single scattering", 0.0, 100.0, 1000) { x: Double -> single(x) })

            for (i in 2 until probs.size) {
                val scatter = { e: Double -> probs[i] * LossCalculator.getLossValue(i, e, 0.0) }
                frame.add(XYFunctionPlot.plot(i.toString() + " scattering", 0.0, 100.0, 1000) { x: Double -> scatter(x) })
            }

            val total = UnivariateFunction { eps ->
                if (probs.size == 1) {
                    return@UnivariateFunction 0.0
                }
                var sum = probs[1] * scatterFunction.value(eps)
                for (i in 2 until probs.size) {
                    sum += probs[i] * LossCalculator.getLossValue(i, eps, 0.0)
                }
                return@UnivariateFunction sum
            }

            frame.add(XYFunctionPlot.plot("Total loss", 0.0, 100.0, 1000) { x: Double -> total.value(x) })

        } else {

            frame.add(XYFunctionPlot.plot("Differential cross-section", 0.0, 100.0, 2000) { x: Double -> scatterFunction.value(x) })
        }

    }

}
