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
package hep.dataforge.stat.fit

import hep.dataforge.io.history.Chronicle
import hep.dataforge.io.history.History
import hep.dataforge.maths.MathUtils
import hep.dataforge.maths.MatrixOperations.inverse
import hep.dataforge.maths.NamedMatrix
import hep.dataforge.maths.NamedVector
import hep.dataforge.meta.Meta
import hep.dataforge.names.AbstractNamedSet
import hep.dataforge.stat.fit.FitStage.*
import hep.dataforge.stat.fit.Fitter.Companion.getFitPars
import hep.dataforge.utils.Misc
import org.apache.commons.math3.linear.ArrayRealVector
import org.apache.commons.math3.linear.EigenDecomposition

/**
 * The state of QOW fitter
 * Created by darksnake on 17-Oct-16.
 */
internal class QOWeight(val source: FitState, val fitPars: Array<String>, theta: ParamSet) : AbstractNamedSet(fitPars) {

    /**
     * The set of parameters for which the weight is calculated
     * TODO make paramSet immutable
     */
    val theta = theta.copy()

    /**
     * Derivatives of the spectrum over parameters. First index in the point number, second one - index of parameter
     *
     * @return the derivs
     */
    val derivs: Array<DoubleArray>

    /**
     * Array of dispersions in each point
     *
     * @return the dispersion
     */
    val dispersion: DoubleArray

    init {
        if (source.dataSize <= 0) {
            throw IllegalStateException("The state does not contain data")
        }

        dispersion = DoubleArray(source.dataSize)
        derivs = Array(names.size()) { DoubleArray(source.dataSize) }

        (0 until source.dataSize).forEach { i ->
            this.dispersion[i] = source.getDispersion(i, theta)
            (0 until names.size()).forEach { k ->
                derivs[k][i] = source.getDisDeriv(this.names.get(k), i, theta)
            }
        }

    }
}


/**
 *
 *
 * QOWFitter class.
 *
 * @author Alexander Nozik
 * @version $Id: $Id
 */
object QOWFitter : Fitter {


    /**
     * Constant `QOW_ENGINE_NAME="QOW"`
     */
    const val QOW_ENGINE_NAME = "QOW"
    /**
     * Constant `QOW_METHOD_FAST="fast"`
     */
    const val QOW_METHOD_FAST = "fast"

    private fun newtonianRun(state: FitState, weight: QOWeight, log: History, meta: Meta) : ParamSet {
        val maxSteps = meta.getInt("iterations", 100)
        val tolerance = meta.getDouble("tolerance", 0.0)

        var dis: Double//норма невязки
        // Для удобства работаем всегда с полным набором параметров
        var par = state.parameters.copy()

        log.report("Starting newtonian iteration from: \n\t{}",
                MathUtils.toString(par, *weight.namesAsArray()))

        var eqvalues = QOWUtils.getEqValues(state, par, weight)//значения функций

        dis = eqvalues.vector.norm// невязка
        log.report("Starting discrepancy is {}", dis)
        var i = 0
        var flag = false
        while (!flag) {
            Misc.checkThread()
            i++
            log.report("Starting step number {}", i)

            val currentSolution = if (meta.getString(METHOD_NAME,"").equals(QOW_METHOD_FAST, ignoreCase = true)) {
                //Берет значения матрицы в той точке, где считается вес
                fastNewtonianStep(state, par, eqvalues, weight)
            } else {
                //Берет значения матрицы в точке par
                newtonianStep(state, par, eqvalues, weight)
            }
            // здесь должен стоять учет границ параметров

            log.report("Parameter values after step are: \n\t{}",
                    MathUtils.toString(currentSolution, *weight.namesAsArray()))

            eqvalues = QOWUtils.getEqValues(state, currentSolution, weight)
            val currentDis = eqvalues.vector.norm// невязка после шага

            log.report("The discrepancy after step is: {}", currentDis)
            if (currentDis >= dis && i > 1) {
                //дополнительно проверяем, чтобы был сделан хотя бы один шаг
                flag = true
                log.report("The discrepancy does not decrease. Stopping iteration.")
            } else {
                par = currentSolution
                dis = currentDis
            }
            if (i >= maxSteps) {
                flag = true
                log.report("Maximum number of iterations reached. Stopping iteration.")
            }
            if (dis <= tolerance) {
                flag = true
                log.report("Tolerance threshold is reached. Stopping iteration.")
            }
        }

        return par
    }

    private fun newtonianStep(source: FitState, par: ParamSet, eqvalues: NamedVector, weight: QOWeight): ParamSet {
        Misc.checkThread()// check if action is cacneled
        val start = par.getParValues(*weight.namesAsArray()).vector
        val invJacob = inverse(QOWUtils.getEqDerivValues(source, par, weight))

        val step = invJacob.operate(ArrayRealVector(eqvalues.getArray()))
        return par.copy().setParValues(NamedVector(weight.namesAsArray(), start.subtract(step)))
    }

    private fun fastNewtonianStep(source: FitState, par: ParamSet, eqvalues: NamedVector, weight: QOWeight): ParamSet {
        Misc.checkThread()// check if action is cacneled
        val start = par.getParValues(*weight.namesAsArray()).vector
        val invJacob = inverse(QOWUtils.getEqDerivValues(source, weight))

        val step = invJacob.operate(ArrayRealVector(eqvalues.getArray()))
        return par.copy().setParValues(NamedVector(weight.namesAsArray(), start.subtract(step)))
    }

    override fun run(state: FitState, parentLog: History?, meta: Meta): FitResult {
        val log = Chronicle("QOW", parentLog)
        val action = meta.getString(FIT_STAGE_TYPE, TASK_RUN)
        log.report("QOW fit engine started task '{}'", action)
        return when (action) {
            TASK_SINGLE -> makeRun(state, log, meta)
            TASK_COVARIANCE -> generateErrors(state, log, meta)
            TASK_RUN -> {
                var res = makeRun(state, log, meta)
                res = makeRun(res.optState().get(), log, meta)
                generateErrors(res.optState().get(), log, meta)
            }
            else -> throw IllegalArgumentException("Unknown task")
        }
    }

    override val name: String = QOW_ENGINE_NAME

    private fun makeRun(state: FitState, log: History, meta: Meta): FitResult {
        /*Инициализация объектов, задание исходных значений*/
        log.report("Starting fit using quasioptimal weights method.")

        val fitPars = getFitPars(state, meta)

        val curWeight = QOWeight(state, fitPars, state.parameters)

        // вычисляем вес в allPar. Потом можно будет попробовать ручное задание веса
        log.report("The starting weight is: \n\t{}",
                MathUtils.toString(curWeight.theta))

        //Стартовая точка такая же как и параметр веса
        /*Фитирование*/
        val res = this.newtonianRun(state, curWeight, log, meta)

        /*Генерация результата*/

        return FitResult.build(state.edit().setPars(res).build(), *fitPars)
    }

    /**
     *
     *
     * generateErrors.
     *
     * @param state a [hep.dataforge.stat.fit.FitState] object.
     * @param task  a [hep.dataforge.stat.fit.FitStage] object.
     * @param log   a [History] object.
     * @return a [FitResult] object.
     */
    private fun generateErrors(state: FitState, log: History, meta: Meta): FitResult {

        log.report("Starting errors estimation using quasioptimal weights method.")

        val fitPars = getFitPars(state, meta)

        val curWeight = QOWeight(state, fitPars, state.parameters)

        // вычисляем вес в allPar. Потом можно будет попробовать ручное задание веса
        log.report("The starting weight is: \n\t{}",
                MathUtils.toString(curWeight.theta))

        //        ParamSet pars = state.getParameters().copy();
        val covar = getCovariance(state, curWeight)

        val decomposition = EigenDecomposition(covar.matrix)
        var valid = true
        for (lambda in decomposition.realEigenvalues) {
            if (lambda <= 0) {
                log.report("The covariance matrix is not positive defined. Error estimation is not valid")
                valid = false
            }
        }


        return FitResult.build(
                state.edit().setCovariance(covar, true).build(),
                valid,
                *fitPars
        )

    }

    private fun getCovariance(source: FitState, weight: QOWeight): NamedMatrix {
        val invH = inverse(QOWUtils.getEqDerivValues(source, weight.namesAsArray(), weight))
        return NamedMatrix(weight.namesAsArray(), invH)
    }

}
