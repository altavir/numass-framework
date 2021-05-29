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
package inr.numass.models.misc

import hep.dataforge.stat.parametric.AbstractParametricFunction
import hep.dataforge.values.ValueProvider
import hep.dataforge.values.Values

import java.lang.Math.*

/**
 * @author Darksnake
 */
class ModGauss(private val cutoff: Double = 4.0) : AbstractParametricFunction("w", "shift", "tailAmp", "tailW"), FunctionSupport {

    private fun getShift(pars: ValueProvider): Double = pars.getDouble("shift", 0.0)

    private fun getTailAmp(pars: ValueProvider): Double = pars.getDouble("tailAmp", 0.0)

    private fun getTailW(pars: ValueProvider): Double = pars.getDouble("tailW", 100.0)

    private fun getW(pars: ValueProvider): Double = pars.getDouble("w")

    override fun providesDeriv(name: String): Boolean = true


    override fun value(d: Double, pars: Values): Double {
        val shift = getShift(pars)
        if (d - shift > cutoff * getW(pars)) {
            return 0.0
        }
        val aux = (d - shift) / getW(pars)
        val tail = if (d > getShift(pars)) {
            0.0
        } else {
            val tailW = getTailW(pars)
            getTailAmp(pars) / tailW * Math.exp((d - shift) / tailW)
        }
        return exp(-aux * aux / 2) / getW(pars) / sqrt(2 * Math.PI) + tail
    }

    override fun derivValue(parName: String, d: Double, pars: Values): Double {
        if (abs(d - getShift(pars)) > cutoff * getW(pars)) {
            return 0.0
        }
        val pos = getShift(pars)
        val w = getW(pars)
        val tailW = getTailW(pars)

        return when (parName) {
            "shift" -> this.value(d, pars) * (d - pos) / w / w
            "w" -> this.value(d, pars) * ((d - pos) * (d - pos) / w / w / w - 1 / w)
            "tailAmp" -> if (d > pos) {
                0.0
            } else {
                Math.exp((d - pos) / tailW) / tailW
            }
            else -> return 0.0;
        }
    }

    override fun getSupport(params: Values): Pair<Double, Double> {
        val shift = getShift(params)
        return Pair(shift - cutoff * getTailW(params), shift + cutoff * getW(params))
    }

    override fun getDerivSupport(parName: String, params: Values): Pair<Double, Double> {
        val shift = getShift(params)
        return Pair(shift - cutoff * getTailW(params), shift + cutoff * getW(params))
    }
}
