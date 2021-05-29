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
import hep.dataforge.maths.NamedVector
import hep.dataforge.meta.Meta
import hep.dataforge.stat.defaultGenerator
import hep.dataforge.stat.fit.FitStage.TASK_RUN
import hep.dataforge.stat.fit.FitStage.TASK_SINGLE
import hep.dataforge.stat.parametric.ParametricMultiFunctionWrapper
import hep.dataforge.stat.parametric.ParametricUtils
import org.apache.commons.math3.optim.*
import org.apache.commons.math3.optim.nonlinear.scalar.GoalType
import org.apache.commons.math3.optim.nonlinear.scalar.MultivariateFunctionMappingAdapter
import org.apache.commons.math3.optim.nonlinear.scalar.ObjectiveFunction
import org.apache.commons.math3.optim.nonlinear.scalar.noderiv.CMAESOptimizer
import org.apache.commons.math3.optim.nonlinear.scalar.noderiv.NelderMeadSimplex
import org.apache.commons.math3.optim.nonlinear.scalar.noderiv.SimplexOptimizer
import java.lang.Math.log

/**
 *
 *
 * CMFitter class.
 *
 * @author Alexander Nozik
 * @version $Id: $Id
 */
object CMFitter : Fitter {


    /**
     * Constant `CM_ENGINE_NAME="CM"`
     */
    const val CM_ENGINE_NAME = "CM"
    /**
     * Constant `CM_NELDERMEADSIMPLEX="neldermead"`
     */
    const val CM_NELDERMEADSIMPLEX = "neldermead"
    /**
     * Constant `CM_CMAESO="CMAESO"`
     */
    const val CM_CMAESO = "CMAESO"

    private const val DEFAULT_MAXITER = 1000
    private const val DEFAULT_TOLERANCE = 1e-5

    override val name: String = CM_ENGINE_NAME


    override fun run(state: FitState, parentLog: History?, meta: Meta): FitResult {
        val log = Chronicle("CM", parentLog)
        val action = meta.getString("action", TASK_RUN)
        when (action) {
            TASK_SINGLE, TASK_RUN -> return makeRun(state, log, meta)
            else -> throw IllegalArgumentException(String.format("Action '%s' is not supported by CMFitter", action))
        }
    }

    fun makeRun(state: FitState, log: History, meta: Meta): FitResult {

        log.report("Starting fit using provided Commons Math algorithms.")
        val maxSteps = meta.getInt("iterations", DEFAULT_MAXITER)
        val tolerance = meta.getDouble("tolerance", DEFAULT_TOLERANCE)
        val fitPars = Fitter.getFitPars(state, meta)
        val pars = state.parameters.copy()

        val subSet = pars.getParValues(*fitPars)
        val likeFunc = ParametricUtils.getNamedSubFunction(state.logLike, pars, *fitPars)

        val func = ParametricMultiFunctionWrapper(likeFunc)
        val oFunc: ObjectiveFunction
        val maxEval = MaxEval(maxSteps)
        val ig = InitialGuess(subSet.getArray())

        val upBounds = DoubleArray(fitPars.size)
        val loBounds = DoubleArray(fitPars.size)

        for (i in fitPars.indices) {
            val p = pars.getByName(fitPars[i])
            upBounds[i] = p.upperBound!!
            loBounds[i] = p.lowerBound!!
        }

        val res: PointValuePair
        val checker = SimpleValueChecker(tolerance, 0.0)

        when (meta.getString("method", "")) {
            CM_NELDERMEADSIMPLEX -> {
                log.report("Using Nelder Mead Simlex (no derivs).")
                val simplex = NelderMeadSimplex(pars.getParErrors(*fitPars).getArray())
                val nmOptimizer = SimplexOptimizer(checker)

                oFunc = ObjectiveFunction(MultivariateFunctionMappingAdapter(func, loBounds, upBounds))
                res = nmOptimizer.optimize(oFunc, maxEval, ig, GoalType.MAXIMIZE, simplex)
            }
            else -> {
                log.report("Using CMAESO optimizer (no derivs).")
                val sb = SimpleBounds(loBounds, upBounds)
                val cmaesOptimizer = CMAESOptimizer(100, java.lang.Double.NEGATIVE_INFINITY,
                        true, 4, 4, defaultGenerator, false, checker)

                val sigmas = CMAESOptimizer.Sigma(pars.getParErrors(*fitPars).getArray())
                val popSize = CMAESOptimizer.PopulationSize((4 + 3 * log(fitPars.size.toDouble())).toInt())

                oFunc = ObjectiveFunction(func)
                res = cmaesOptimizer.optimize(oFunc, maxEval, ig, sb, sigmas, popSize, GoalType.MAXIMIZE)
            }
        }

        val respars = NamedVector(fitPars, res.point)
        val allpars = pars.copy()
        allpars.setParValues(respars)

        return FitResult.build(state.edit().setPars(allpars).build(), *Fitter.getFitPars(state, meta))

    }


}
