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
class GaussFunction(private val cutoff: Double = 4.0) : AbstractParametricFunction(list), FunctionSupport {

    private fun getShift(pars: ValueProvider): Double {
        return pars.getDouble("shift", 0.0)
    }

    private fun getW(pars: ValueProvider): Double {
        return pars.getDouble("w")
    }

    override fun providesDeriv(name: String): Boolean {
        return true
    }


    override fun value(d: Double, pars: Values): Double {
        if (abs(d - getShift(pars)) > cutoff * getW(pars)) {
            return 0.0
        }
        val aux = (d - getShift(pars)) / getW(pars)
        return exp(-aux * aux / 2) / getW(pars) / sqrt(2 * Math.PI)
    }

    override fun derivValue(parName: String, d: Double, pars: Values): Double {
        if (abs(d - getShift(pars)) > cutoff * getW(pars)) {
            return 0.0
        }
        val pos = getShift(pars)
        val w = getW(pars)

        return when (parName) {
            "shift" -> this.value(d, pars) * (d - pos) / w / w
            "w" -> this.value(d, pars) * ((d - pos) * (d - pos) / w / w / w - 1 / w)
            else -> return 0.0;
        }
    }

    override fun getSupport(params: Values): Pair<Double, Double> {
        val shift = getShift(params)
        val w = getW(params)
        return Pair(shift - cutoff * w, shift + cutoff * w)
    }

    override fun getDerivSupport(parName: String, params: Values): Pair<Double, Double> {
        val shift = getShift(params)
        val w = getW(params)
        return Pair(shift - cutoff * w, shift + cutoff * w)
    }


    companion object {

        private val list = arrayOf("w", "shift")
    }
}
