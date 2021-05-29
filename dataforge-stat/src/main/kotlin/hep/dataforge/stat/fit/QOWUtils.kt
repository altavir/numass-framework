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
package hep.dataforge.stat.fit

import hep.dataforge.maths.NamedVector
import org.apache.commons.math3.linear.Array2DRowRealMatrix
import org.apache.commons.math3.linear.RealMatrix
import java.util.logging.Logger

/**
 *
 * @author Alexander Nozik
 */
internal object QOWUtils {

    fun covarFExp(source: FitState, set: ParamSet, weight: QOWeight): RealMatrix {
        return covarFExp(source, set, weight.namesAsArray(), weight)
    }

    /**
     * Теоретическая ковариация весовых функций.
     *
     * D(\phi)=E(\phi_k(\theta_0) \phi_l(\theta_0))= disDeriv_k * disDeriv_l /sigma^2
     *
     *
     * @param source
     * @param weight
     * @return
     */
    fun covarF(source: FitState, weight: QOWeight): RealMatrix {

        val fitDim = weight.names.size()
        val res = Array(fitDim) { DoubleArray(fitDim) }

        var i: Int
        var k: Int
        var l: Int

        var summ: Double

        k = 0
        while (k < fitDim) {
            l = k
            while (l < fitDim) {
                summ = 0.0
                i = 0
                while (i < source.dataSize) {
                    summ += weight.derivs[k][i] * weight.derivs[l][i] / weight.dispersion[i]
                    i++
                }
                res[k][l] = summ
                if (l != k) {
                    res[l][k] = summ
                }
                l++
            }
            k++
        }
        return Array2DRowRealMatrix(res)
    }

    /**
     * Экспериментальная ковариация весов. Формула (22) из
     * http://arxiv.org/abs/physics/0604127
     *
     * @param source
     * @param set
     * @param fitPars
     * @param weight
     * @return
     */
    fun covarFExp(source: FitState, set: ParamSet, fitPars: Array<String>, weight: QOWeight): RealMatrix {

        val fitDim = fitPars.size
        val res = Array(fitDim) { DoubleArray(fitDim) }
        val eqvalues = Array(source.dataSize) { DoubleArray(fitDim) }
        /*
         * Важно! Если не делать предварителього вычисления этих производных, то
         * количество вызывов функции будет dim^2 вместо dim Первый индекс -
         * номер точки, второй - номер переменной, по которой берется производная
         */
        var i: Int
        var k: Int
        var l: Int
        l = 0
        while (l < fitDim) {
            i = 0
            while (i < source.dataSize) {
                eqvalues[i][l] = source.getDis(i, set) * weight.derivs[l][i] / weight.dispersion[i]
                i++
            }
            l++
        }
        var summ: Double

        k = 0
        while (k < fitDim) {
            l = 0
            while (l < fitDim) {
                summ = 0.0
                i = 0
                while (i < source.dataSize) {
                    summ += eqvalues[i][l] * eqvalues[i][k]
                    i++
                }
                res[k][l] = summ
                l++
            }
            k++
        }
        return Array2DRowRealMatrix(res)
    }

    /**
     * Берет производные уравнений по параметрам, указанным в весе
     *
     * @param source
     * @param set
     * @param weight
     * @return
     */
    fun getEqDerivValues(source: FitState, set: ParamSet, weight: QOWeight): RealMatrix {
        return getEqDerivValues(source, set, weight.namesAsArray(), weight)
    }

    fun getEqDerivValues(source: FitState, weight: QOWeight): RealMatrix {
        return getEqDerivValues(source, weight.namesAsArray(), weight)
    }

    /**
     * производные уравнений для метода Ньютона
     *
     * @param source
     * @param set
     * @param fitPars
     * @param weight
     * @return
     */
    fun getEqDerivValues(source: FitState, set: ParamSet, fitPars: Array<String>, weight: QOWeight): RealMatrix {

        val fitDim = fitPars.size
        //Возвращает производную k-того Eq по l-тому параметру
        val res = Array(fitDim) { DoubleArray(fitDim) }
        val sderiv = Array(source.dataSize) { DoubleArray(fitDim) }
        /*
         * Важно! Если не делать предварителього вычисления этих производных, то
         * количество вызывов функции будет dim^2 вместо dim Первый индекс -
         * номер точки, второй - номер переменной, по которой берется производная
         */
        var i: Int// номер точки из набора данных
        var k: Int// номер уравнения
        var l: Int// номер параметра, по короторому берется производная
        l = 0
        while (l < fitDim) {
            i = 0
            while (i < source.dataSize) {
                sderiv[i][l] = source.getDisDeriv(fitPars[l], i, set)
                i++

            }
            l++
        }
        var summ: Double

        k = 0
        while (k < fitDim) {
            l = 0
            while (l < fitDim) {
                summ = 0.0
                i = 0
                while (i < source.dataSize) {
                    // Тут баг, при нулевой дисперсии скатываемся в сингулярность.!!!
                    assert(weight.dispersion[i] > 0)
                    summ += sderiv[i][l] * weight.derivs[k][i] / weight.dispersion[i]
                    i++
                }
                res[k][l] = summ
                //TODO Это правильно. Почему??
                if (source.prior != null
                        && source.prior.names.contains(fitPars[k])
                        && source.prior.names.contains(fitPars[l])) {
                    val prior = source.prior
                    Logger.getAnonymousLogger().warning("QOW does not interpret prior probability correctly")
                    val pi = prior.value(set)
                    val deriv1 = prior.derivValue(fitPars[k], set)
                    val deriv2 = prior.derivValue(fitPars[l], set)
                    //считаем априорную вероятность независимой для разных переменных
                    res[k][l] += deriv1 * deriv2 / pi / pi
                }
                l++
            }
            k++
        }
        return Array2DRowRealMatrix(res)
    }

    /**
     * Этот метод считает матрицу производных сразу в тета-0. Сильно экономит
     * вызовы функции
     *
     * @param source
     * @param fitPars
     * @param weight
     * @return
     */
    fun getEqDerivValues(source: FitState, fitPars: Array<String>, weight: QOWeight): RealMatrix {
        val fitDim = fitPars.size
        val res = Array(fitDim) { DoubleArray(fitDim) }
        var i: Int
        var k: Int
        var l: Int
        var summ: Double
        k = 0
        while (k < fitDim) {
            l = 0
            while (l < fitDim) {
                summ = 0.0
                i = 0
                while (i < source.dataSize) {
                    summ += weight.derivs[l][i] * weight.derivs[k][i] / weight.dispersion[i]
                    i++
                }
                res[k][l] = summ

                //TODO Это правильно. Почему??
                if (source.prior != null
                        && source.prior.names.contains(fitPars[k])
                        && source.prior.names.contains(fitPars[l])) {
                    Logger.getAnonymousLogger().warning("QOW does not interpret prior probability correctly")
                    val prior = source.prior
                    val pi = prior.value(weight.theta)
                    val deriv1 = prior.derivValue(fitPars[k], weight.theta)
                    val deriv2 = prior.derivValue(fitPars[l], weight.theta)
                    //считаем априорную вероятность независимой для разный переменных
                    res[k][l] += deriv1 * deriv2 / pi / pi
                }
                l++
            }
            k++
        }
        return Array2DRowRealMatrix(res)
    }

    fun getEqValues(source: FitState, set: ParamSet, weight: QOWeight): NamedVector {
        return getEqValues(source, set, weight.namesAsArray(), weight)
    }

    /**
     * Значения уравнений метода квазиоптимальных весов
     *
     * @param source
     * @param set
     * @param fitPars
     * @param weight
     * @return
     */
    fun getEqValues(source: FitState, set: ParamSet, fitPars: Array<String>, weight: QOWeight): NamedVector {

        val res = DoubleArray(fitPars.size)
        var i: Int
        var k: Int
        var summ: Double

        val diss = DoubleArray(source.dataSize)

        i = 0
        while (i < diss.size) {
            diss[i] = source.getDis(i, set)
            i++

        }

        k = 0
        while (k < fitPars.size) {
            summ = 0.0
            i = 0
            while (i < source.dataSize) {
                summ += diss[i] * weight.derivs[k][i] / weight.dispersion[i]
                i++
            }
            res[k] = summ
            //Поправка на априорную вероятность
            if (source.prior != null && source.prior.names.contains(fitPars[k])) {
                Logger.getAnonymousLogger().warning("QOW does not interpret prior probability correctly")
                val prior = source.prior
                res[k] -= prior.derivValue(fitPars[k], set) / prior.value(set)
            }
            k++
        }
        return NamedVector(fitPars, res)
    }
}

